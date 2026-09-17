package com.travelingtunes.app.core.theme

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * A title text composable that enforces two rules:
 * 1. When a title splits across multiple rows (e.g. 2 lines), lines are balanced to be even in length,
 *    without breaking up words across lines (using [LineBreak.Heading]).
 * 2. Never break up a word; if a single word won't fit without breaking it up on a line, font size
 *    is reduced just enough so that every single word (and the full title) fits cleanly without breaking up words.
 * 3. 5% single-row reduction rule: If reducing font size by up to 5% allows a title to fit on 1 row,
 *    do so to preserve vertical space for adjacent title rows.
 */
private data class SizeAndLines(
    val fontSize: TextUnit,
    val maxLines: Int
)

@Composable
fun BalancedTitleText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontFamily: FontFamily? = null,
    fontWeight: FontWeight? = null,
    fontStyle: FontStyle? = null,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign = TextAlign.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = 2,
    minFontSize: TextUnit = 10.sp,
    enableMarquee: Boolean = false,
    allowFivePercentOneRowFit: Boolean = true,
    style: TextStyle = LocalTextStyle.current,
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()

        val availableWidthPx = if (this.constraints.hasBoundedWidth) this.constraints.maxWidth.toFloat() else with(density) { this@BoxWithConstraints.maxWidth.toPx() }
        val availableHeightPx = if (this.constraints.hasBoundedHeight) this.constraints.maxHeight.toFloat() else with(density) { this@BoxWithConstraints.maxHeight.toPx() }

        // Determine initial base font size
        val baseFontSize = if (fontSize.isSp && (fontSize.value > 0f)) {
            fontSize
        } else if (style.fontSize.isSp && (style.fontSize.value > 0f)) {
            style.fontSize
        } else {
            18.sp
        }

        val minFontSp = if (minFontSize.isSp && (minFontSize.value > 0f)) minFontSize.value else 8f

        val effectiveTextStyle = style.merge(
            TextStyle(
                color = color,
                fontFamily = fontFamily,
                fontWeight = fontWeight,
                fontStyle = fontStyle,
                textDecoration = textDecoration,
                textAlign = textAlign,
                lineBreak = LineBreak.Heading,
            )
        )

        val actualMaxLines = if (enableMarquee) 1 else maxLines

        val availableWidthInt = (availableWidthPx).toInt()
        val availableHeightInt = (availableHeightPx).toInt()

        // Calculate auto-scaled font size and line count using low-latency binary search fitting
        val sizeAndLines = remember(
            text,
            availableWidthInt,
            availableHeightInt,
            baseFontSize,
            minFontSp,
            actualMaxLines,
            allowFivePercentOneRowFit,
            effectiveTextStyle,
        ) {
            if ((availableWidthPx <= 0f) || text.isBlank()) {
                SizeAndLines(baseFontSize, actualMaxLines)
            } else {
                var currentSp = baseFontSize.value
                val words = text.split(' ').filter { it.isNotEmpty() }

                // Step 1: Ensure no individual word is wider than available line width.
                if (words.isNotEmpty()) {
                    var maxWordWidthPx = 0f
                    for (word in words) {
                        val wordStyle = effectiveTextStyle.copy(fontSize = currentSp.sp)
                        val measuredWord = textMeasurer.measure(
                            text = word,
                            style = wordStyle,
                            maxLines = 1,
                            softWrap = false,
                        )
                        if (measuredWord.size.width > maxWordWidthPx) {
                            maxWordWidthPx = measuredWord.size.width.toFloat()
                        }
                    }

                    if (maxWordWidthPx > availableWidthPx) {
                        val scale = availableWidthPx / maxWordWidthPx
                        currentSp = (currentSp * scale).coerceAtLeast(minFontSp)
                    }
                }

                // Step 2: Check 5% single-row reduction fit rule.
                // If text can fit on 1 row by reducing font size by up to 5% (>= 95% of requested font size), do that.
                var solvedOneRow = false
                var finalSp = currentSp
                var finalLines = actualMaxLines

                if (actualMaxLines > 1 && allowFivePercentOneRowFit) {
                    val minFivePercentSp = (currentSp * 0.95f).coerceAtLeast(minFontSp)
                    // Test 1-row fit at minFivePercentSp
                    val testStyleOneRow = effectiveTextStyle.copy(
                        fontSize = minFivePercentSp.sp,
                        lineHeight = if (lineHeight.isSp && (lineHeight.value > 0f)) {
                            (lineHeight.value * (minFivePercentSp / baseFontSize.value)).sp
                        } else {
                            (minFivePercentSp * 1.35f).sp
                        },
                    )
                    val measuredOneRow = textMeasurer.measure(
                        text = text,
                        style = testStyleOneRow,
                        constraints = Constraints(maxWidth = availableWidthPx.toInt().coerceAtLeast(1)),
                        maxLines = 1,
                        softWrap = false,
                    )
                    if (measuredOneRow.lineCount == 1 && !measuredOneRow.hasVisualOverflow && measuredOneRow.size.width <= availableWidthPx) {
                        // Binary search between 0.95*currentSp and currentSp to find largest font size that fits 1 row
                        var low = minFivePercentSp
                        var high = currentSp
                        var bestOneRowSp = minFivePercentSp
                        for (i in 0..4) {
                            val mid = (low + high) / 2f
                            val midStyle = effectiveTextStyle.copy(
                                fontSize = mid.sp,
                                lineHeight = if (lineHeight.isSp && (lineHeight.value > 0f)) {
                                    (lineHeight.value * (mid / baseFontSize.value)).sp
                                } else {
                                    (mid * 1.35f).sp
                                },
                            )
                            val m = textMeasurer.measure(
                                text = text,
                                style = midStyle,
                                constraints = Constraints(maxWidth = availableWidthPx.toInt().coerceAtLeast(1)),
                                maxLines = 1,
                                softWrap = false,
                            )
                            if (m.lineCount == 1 && !m.hasVisualOverflow && m.size.width <= availableWidthPx) {
                                bestOneRowSp = mid
                                low = mid + 0.1f
                            } else {
                                high = mid - 0.1f
                            }
                        }
                        finalSp = bestOneRowSp
                        finalLines = 1
                        solvedOneRow = true
                    }
                }

                // Step 3: Fast binary search fitting for multiline / height constraints if 1-row fit was not used.
                if (!solvedOneRow) {
                    fun checkFits(sp: Float): Boolean {
                        val testStyle = effectiveTextStyle.copy(
                            fontSize = sp.sp,
                            lineHeight = if (lineHeight.isSp && (lineHeight.value > 0f)) {
                                (lineHeight.value * (sp / baseFontSize.value)).sp
                            } else {
                                (sp * 1.35f).sp
                            },
                        )
                        val measuredText = textMeasurer.measure(
                            text = text,
                            style = testStyle,
                            constraints = Constraints(maxWidth = availableWidthPx.toInt().coerceAtLeast(1)),
                            maxLines = actualMaxLines,
                            softWrap = true,
                        )
                        return !measuredText.hasVisualOverflow &&
                                (measuredText.lineCount <= actualMaxLines) &&
                                ((availableHeightPx >= Float.MAX_VALUE) || (availableHeightPx <= 0f) || (measuredText.size.height <= availableHeightPx))
                    }

                    if (checkFits(currentSp)) {
                        finalSp = currentSp
                    } else {
                        var low = minFontSp
                        var high = currentSp
                        var bestSp = minFontSp
                        for (i in 0..5) {
                            val mid = (low + high) / 2f
                            if (checkFits(mid)) {
                                bestSp = mid
                                low = mid + 0.1f
                            } else {
                                high = mid - 0.1f
                            }
                        }
                        finalSp = bestSp
                    }
                }

                SizeAndLines(finalSp.sp, finalLines)
            }
        }

        val calculatedFontSize = sizeAndLines.fontSize
        val targetMaxLines = sizeAndLines.maxLines

        val finalLineHeight = if (lineHeight.isSp && (lineHeight.value > 0f)) {
            (lineHeight.value * (calculatedFontSize.value / baseFontSize.value)).sp
        } else {
            (calculatedFontSize.value * 1.35f).sp
        }

        val marqueeModifier = if (enableMarquee) Modifier.basicMarquee() else Modifier

        Text(
            text = text,
            fontSize = calculatedFontSize,
            lineHeight = finalLineHeight,
            fontFamily = fontFamily,
            color = color,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            textDecoration = textDecoration,
            textAlign = textAlign,
            maxLines = targetMaxLines,
            overflow = TextOverflow.Clip,
            style = effectiveTextStyle,
            modifier = Modifier
                .fillMaxWidth()
                .then(marqueeModifier),
        )
    }
}
