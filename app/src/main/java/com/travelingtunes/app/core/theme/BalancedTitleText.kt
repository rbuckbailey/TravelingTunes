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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
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
 */
@Composable
fun BalancedTitleText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontFamily: FontFamily? = null,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign = TextAlign.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = 2,
    minFontSize: TextUnit = 10.sp,
    enableMarquee: Boolean = false,
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
                textAlign = textAlign,
                lineBreak = LineBreak.Heading,
            )
        )

        val actualMaxLines = if (enableMarquee) 1 else maxLines

        // Calculate auto-scaled font size guaranteed not to break words
        val calculatedFontSize = remember(
            text,
            availableWidthPx,
            availableHeightPx,
            baseFontSize,
            minFontSp,
            actualMaxLines,
            effectiveTextStyle,
        ) {
            if ((availableWidthPx <= 0f) || text.isBlank()) {
                baseFontSize
            } else {
                var currentSp = baseFontSize.value
                val words = text.split("\\s+".toRegex()).filter { it.isNotEmpty() }

                // Step 1: Ensure no individual word is wider than available line width.
                // If a word exceeds available width, reduce font size so the longest word fits on 1 line.
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

                // Step 2: Ensure full text fits within actualMaxLines and height constraints.
                // Reduce font size iteratively if visual overflow occurs.
                var fits = false
                var attempts = 0
                while (!fits && (attempts < 15) && (currentSp > minFontSp)) {
                    val testStyle = effectiveTextStyle.copy(
                        fontSize = currentSp.sp,
                        lineHeight = if (lineHeight.isSp && (lineHeight.value > 0f)) {
                            (lineHeight.value * (currentSp / baseFontSize.value)).sp
                        } else {
                            (currentSp * 1.35f).sp
                        },
                    )
                    val measuredText = textMeasurer.measure(
                        text = text,
                        style = testStyle,
                        constraints = Constraints(
                            maxWidth = availableWidthPx.toInt().coerceAtLeast(1),
                        ),
                        maxLines = actualMaxLines,
                        softWrap = true,
                    )

                    val hasOverflow = measuredText.hasVisualOverflow ||
                            (measuredText.lineCount > actualMaxLines) ||
                            ((availableHeightPx < Float.MAX_VALUE) && (availableHeightPx > 0f) && (measuredText.size.height > availableHeightPx))

                    if (!hasOverflow) {
                        fits = true
                    } else {
                        val nextSp = currentSp * 0.92f
                        if (nextSp < minFontSp) {
                            currentSp = minFontSp
                            break
                        } else {
                            currentSp = nextSp
                        }
                        attempts++
                    }
                }

                currentSp.sp
            }
        }

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
            textAlign = textAlign,
            maxLines = actualMaxLines,
            overflow = TextOverflow.Clip,
            style = effectiveTextStyle,
            modifier = Modifier
                .fillMaxWidth()
                .then(marqueeModifier),
        )
    }
}
