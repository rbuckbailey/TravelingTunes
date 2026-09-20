package com.travelingtunes.app.core.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.travelingtunes.app.core.model.Song
import com.travelingtunes.app.core.model.ThemeSettings
import kotlin.random.Random

data class MondrianRegionSpec(
    val color1: Color,
    val isSubdivided: Boolean,
    val isVerticalSplit: Boolean,
    val color2: Color?
)

data class MondrianAlbumLayout(
    val topLeft: MondrianRegionSpec,
    val topRight: MondrianRegionSpec,
    val bottomLeft: MondrianRegionSpec,
    val bottomRight: MondrianRegionSpec
)

object MondrianThemeHelper {

    val COLOR_WHITE = Color(0xFFFFFFFF)
    val COLOR_RED = Color(0xFFE50000)
    val COLOR_YELLOW = Color(0xFFFAC901)
    val COLOR_BLUE = Color(0xFF3F80EA)
    val COLOR_BLACK = Color(0xFF000000)

    val PRIMARY_COLORS = listOf(COLOR_WHITE, COLOR_RED, COLOR_YELLOW, COLOR_BLUE)

    // Weighted selection: ~50% White, ~50% Color
    val WEIGHTED_PRIMARY_COLORS = listOf(
        COLOR_WHITE, COLOR_WHITE, COLOR_WHITE,
        COLOR_RED, COLOR_YELLOW, COLOR_BLUE
    )

    val SECONDARY_HALF_COLORS = listOf(COLOR_WHITE, COLOR_RED, COLOR_YELLOW, COLOR_BLUE, COLOR_BLACK)

    fun canBeAdjacent(c1: Color, c2: Color): Boolean {
        if (c1 == COLOR_WHITE && c2 == COLOR_WHITE) return true
        return c1 != c2
    }

    fun generateLayoutForSong(song: Song?): MondrianAlbumLayout {
        // Deterministic seed per track
        val seedKey = if (song != null) {
            "mondrian_track_${song.id}_${song.artist.lowercase().trim()}_${song.album.lowercase().trim()}_${song.title.lowercase().trim()}"
        } else {
            "default_mondrian_track"
        }
        val random = Random(seedKey.hashCode().toLong())

        // 1. Determine number of non-white colored spaces (at least 2, up to 3)
        val nonWhitePalette = listOf(COLOR_RED, COLOR_YELLOW, COLOR_BLUE)
        val numColoredSpaces = if (random.nextBoolean()) 2 else 3

        // Pick distinct non-white colors so NO non-white color ever repeats
        val chosenColored = nonWhitePalette.shuffled(random).take(numColoredSpaces)

        // 2. Decide if 1 corner should be subdivided
        val shouldSubdivide = random.nextBoolean()
        val cornerToSubdivide = if (shouldSubdivide) random.nextInt(4) else -1
        val isVert = random.nextBoolean()

        // Total spaces: 4 corners, or 3 full corners + 2 half-spaces if 1 corner is subdivided
        val spacesCount = if (shouldSubdivide) 5 else 4

        // Randomly pick which spaces receive the chosen non-white colors
        val spaceIndices = (0 until spacesCount).shuffled(random)
        val coloredSpaceIndices = spaceIndices.take(numColoredSpaces).toSet()

        val spaceColors = mutableMapOf<Int, Color>()
        for ((idx, spaceIdx) in coloredSpaceIndices.withIndex()) {
            spaceColors[spaceIdx] = chosenColored[idx]
        }

        // Fill remaining spaces with COLOR_WHITE
        for (i in 0 until spacesCount) {
            if (!spaceColors.containsKey(i)) {
                spaceColors[i] = COLOR_WHITE
            }
        }

        var spaceIdx = 0

        fun createRegionSpec(cornerIndex: Int): MondrianRegionSpec {
            val isSub = (cornerToSubdivide == cornerIndex)
            if (!isSub) {
                val c1 = spaceColors[spaceIdx++] ?: COLOR_WHITE
                return MondrianRegionSpec(
                    color1 = c1,
                    isSubdivided = false,
                    isVerticalSplit = false,
                    color2 = null
                )
            } else {
                val c1 = spaceColors[spaceIdx++] ?: COLOR_WHITE
                var c2 = spaceColors[spaceIdx++] ?: COLOR_WHITE
                if (c1 != COLOR_WHITE && c2 == c1) {
                    c2 = COLOR_WHITE
                }
                return MondrianRegionSpec(
                    color1 = c1,
                    isSubdivided = true,
                    isVerticalSplit = isVert,
                    color2 = c2
                )
            }
        }

        val topLeftSpec = createRegionSpec(0)
        val topRightSpec = createRegionSpec(1)
        val bottomLeftSpec = createRegionSpec(2)
        val bottomRightSpec = createRegionSpec(3)

        return MondrianAlbumLayout(
            topLeft = topLeftSpec,
            topRight = topRightSpec,
            bottomLeft = bottomLeftSpec,
            bottomRight = bottomRightSpec
        )
    }
}

@Composable
fun MondrianBackground(
    song: Song?,
    modifier: Modifier = Modifier,
    currentPositionMs: Long = 0L,
    durationMs: Long = 0L,
    volumeRatio: Float = 0.5f,
    currentPositionMsProvider: (() -> Long)? = null,
    durationMsProvider: (() -> Long)? = null,
    volumeRatioProvider: (() -> Float)? = null
) {
    val layout = remember(song?.id, song?.albumId, song?.album, song?.title) {
        MondrianThemeHelper.generateLayoutForSong(song)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val curPos = currentPositionMsProvider?.invoke() ?: currentPositionMs
        val dur = durationMsProvider?.invoke() ?: durationMs
        val vol = volumeRatioProvider?.invoke() ?: volumeRatio

        val progressRatio = if (dur > 0L) {
            (curPos.toFloat() / dur.toFloat()).coerceIn(0.02f, 0.98f)
        } else {
            0.5f
        }
        val clampedVolumeRatio = vol.coerceIn(0.02f, 0.98f)

        val w = size.width
        val h = size.height

        val splitX = w * progressRatio
        val splitY = h * (1f - clampedVolumeRatio)

        val gridLinePx = 8.dp.toPx()
        val subLinePx = 6.dp.toPx()

        // 1. TOP-LEFT REGION [0..splitX, 0..splitY]
        drawRegionBlock(
            left = 0f,
            top = 0f,
            blockWidth = splitX,
            blockHeight = splitY,
            spec = layout.topLeft,
            subLinePx = subLinePx
        )

        // 2. TOP-RIGHT REGION [splitX..w, 0..splitY]
        drawRegionBlock(
            left = splitX,
            top = 0f,
            blockWidth = w - splitX,
            blockHeight = splitY,
            spec = layout.topRight,
            subLinePx = subLinePx
        )

        // 3. BOTTOM-LEFT REGION [0..splitX, splitY..h]
        drawRegionBlock(
            left = 0f,
            top = splitY,
            blockWidth = splitX,
            blockHeight = h - splitY,
            spec = layout.bottomLeft,
            subLinePx = subLinePx
        )

        // 4. BOTTOM-RIGHT REGION [splitX..w, splitY..h]
        drawRegionBlock(
            left = splitX,
            top = splitY,
            blockWidth = w - splitX,
            blockHeight = h - splitY,
            spec = layout.bottomRight,
            subLinePx = subLinePx
        )

        // Vertical solid black progress bar line
        drawLine(
            color = Color.Black,
            start = Offset(splitX, 0f),
            end = Offset(splitX, h),
            strokeWidth = gridLinePx
        )

        // Horizontal solid black volume bar line
        drawLine(
            color = Color.Black,
            start = Offset(0f, splitY),
            end = Offset(w, splitY),
            strokeWidth = gridLinePx
        )
    }
}

@Composable
fun MondrianMaskedLayout(
    song: Song?,
    themeSettings: ThemeSettings,
    modifier: Modifier = Modifier,
    currentPositionMs: Long = 0L,
    durationMs: Long = 0L,
    volumeRatio: Float = 0.5f,
    currentPositionMsProvider: (() -> Long)? = null,
    durationMsProvider: (() -> Long)? = null,
    volumeRatioProvider: (() -> Float)? = null,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()
    }
}

private fun DrawScope.drawBlackSegmentsMask(
    w: Float,
    h: Float,
    splitX: Float,
    splitY: Float,
    layout: MondrianAlbumLayout
) {
    drawRegionBlackMask(0f, 0f, splitX, splitY, layout.topLeft)
    drawRegionBlackMask(splitX, 0f, w - splitX, splitY, layout.topRight)
    drawRegionBlackMask(0f, splitY, splitX, h - splitY, layout.bottomLeft)
    drawRegionBlackMask(splitX, splitY, w - splitX, h - splitY, layout.bottomRight)
}

private fun DrawScope.drawRegionBlackMask(
    left: Float,
    top: Float,
    blockWidth: Float,
    blockHeight: Float,
    spec: MondrianRegionSpec
) {
    if (blockWidth <= 0f || blockHeight <= 0f) return

    if (spec.isSubdivided && spec.color2 == MondrianThemeHelper.COLOR_BLACK) {
        if (spec.isVerticalSplit) {
            val halfW = blockWidth / 2f
            drawRect(
                color = Color.White,
                topLeft = Offset(left + halfW, top),
                size = Size(blockWidth - halfW, blockHeight)
            )
        } else {
            val halfH = blockHeight / 2f
            drawRect(
                color = Color.White,
                topLeft = Offset(left, top + halfH),
                size = Size(blockWidth, blockHeight - halfH)
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRegionBlock(
    left: Float,
    top: Float,
    blockWidth: Float,
    blockHeight: Float,
    spec: MondrianRegionSpec,
    subLinePx: Float
) {
    if (blockWidth <= 0f || blockHeight <= 0f) return

    if (!spec.isSubdivided || spec.color2 == null) {
        drawRect(
            color = spec.color1,
            topLeft = Offset(left, top),
            size = Size(blockWidth, blockHeight)
        )
    } else {
        if (spec.isVerticalSplit) {
            val halfW = blockWidth / 2f
            drawRect(
                color = spec.color1,
                topLeft = Offset(left, top),
                size = Size(halfW, blockHeight)
            )
            drawRect(
                color = spec.color2,
                topLeft = Offset(left + halfW, top),
                size = Size(blockWidth - halfW, blockHeight)
            )
            drawLine(
                color = Color.Black,
                start = Offset(left + halfW, top),
                end = Offset(left + halfW, top + blockHeight),
                strokeWidth = subLinePx
            )
        } else {
            val halfH = blockHeight / 2f
            drawRect(
                color = spec.color1,
                topLeft = Offset(left, top),
                size = Size(blockWidth, halfH)
            )
            drawRect(
                color = spec.color2,
                topLeft = Offset(left, top + halfH),
                size = Size(blockWidth, blockHeight - halfH)
            )
            drawLine(
                color = Color.Black,
                start = Offset(left, top + halfH),
                end = Offset(left + blockWidth, top + halfH),
                strokeWidth = subLinePx
            )
        }
    }
}
