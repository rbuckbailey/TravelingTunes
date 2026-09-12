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

    val SECONDARY_HALF_COLORS = listOf(COLOR_WHITE, COLOR_BLACK)

    fun canBeAdjacent(c1: Color, c2: Color): Boolean {
        if (c1 == COLOR_WHITE && c2 == COLOR_WHITE) return true
        return c1 != c2
    }

    fun generateLayoutForSong(song: Song?): MondrianAlbumLayout {
        // Seed 1: Base 4 region colors per album
        val albumKey = if (song != null) {
            val albumStr = song.album.lowercase().trim()
            if (albumStr.isNotEmpty()) "album_${song.albumId}_$albumStr"
            else "song_${song.id}"
        } else {
            "default_mondrian_album"
        }
        val albumRandom = Random(albumKey.hashCode().toLong())

        val colorTL = WEIGHTED_PRIMARY_COLORS[albumRandom.nextInt(WEIGHTED_PRIMARY_COLORS.size)]

        val validTR = WEIGHTED_PRIMARY_COLORS.filter { canBeAdjacent(colorTL, it) }
        val colorTR = validTR[albumRandom.nextInt(validTR.size)]

        val validBL = WEIGHTED_PRIMARY_COLORS.filter { canBeAdjacent(colorTL, it) }
        val colorBL = validBL[albumRandom.nextInt(validBL.size)]

        val validBR = WEIGHTED_PRIMARY_COLORS.filter { canBeAdjacent(colorTR, it) && canBeAdjacent(colorBL, it) }
        val colorBR = validBR[albumRandom.nextInt(validBR.size)]

        // Seed 2: Subdivision selection per track (no more than 1 corner subdivided per track)
        val trackKey = if (song != null) {
            val titleStr = song.title.lowercase().trim()
            "track_${song.id}_$titleStr"
        } else {
            "default_mondrian_track"
        }
        val trackRandom = Random(trackKey.hashCode().toLong())

        val shouldSubdivide = trackRandom.nextBoolean()
        val cornerToSubdivide = if (shouldSubdivide) trackRandom.nextInt(4) else -1

        val isVert = trackRandom.nextBoolean()

        fun pickColor2(color1: Color, neighborColor: Color?): Color {
            val valid = SECONDARY_HALF_COLORS.filter {
                canBeAdjacent(color1, it) && (neighborColor == null || canBeAdjacent(neighborColor, it))
            }
            return if (valid.isNotEmpty()) {
                valid[trackRandom.nextInt(valid.size)]
            } else {
                SECONDARY_HALF_COLORS.first { canBeAdjacent(color1, it) }
            }
        }

        // Region 0: TL
        val isSubTL = (cornerToSubdivide == 0)
        val color2TL = if (isSubTL) {
            val neighbor = if (isVert) colorTR else colorBL
            pickColor2(colorTL, neighbor)
        } else null

        // Region 1: TR
        val isSubTR = (cornerToSubdivide == 1)
        val color2TR = if (isSubTR) {
            val neighbor = if (isVert) colorTL else colorBR
            pickColor2(colorTR, neighbor)
        } else null

        // Region 2: BL
        val isSubBL = (cornerToSubdivide == 2)
        val color2BL = if (isSubBL) {
            val neighbor = if (isVert) colorBR else colorTL
            pickColor2(colorBL, neighbor)
        } else null

        // Region 3: BR
        val isSubBR = (cornerToSubdivide == 3)
        val color2BR = if (isSubBR) {
            val neighbor = if (isVert) colorBL else colorTR
            pickColor2(colorBR, neighbor)
        } else null

        return MondrianAlbumLayout(
            topLeft = MondrianRegionSpec(colorTL, isSubTL, if (isSubTL) isVert else false, color2TL),
            topRight = MondrianRegionSpec(colorTR, isSubTR, if (isSubTR) isVert else false, color2TR),
            bottomLeft = MondrianRegionSpec(colorBL, isSubBL, if (isSubBL) isVert else false, color2BL),
            bottomRight = MondrianRegionSpec(colorBR, isSubBR, if (isSubBR) isVert else false, color2BR)
        )
    }
}

@Composable
fun MondrianBackground(
    song: Song?,
    modifier: Modifier = Modifier,
    currentPositionMs: Long = 0L,
    durationMs: Long = 0L,
    volumeRatio: Float = 0.5f
) {
    val layout = remember(song?.id, song?.albumId, song?.album, song?.title) {
        MondrianThemeHelper.generateLayoutForSong(song)
    }

    val progressRatio = if (durationMs > 0L) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0.02f, 0.98f)
    } else {
        0.5f
    }
    val clampedVolumeRatio = volumeRatio.coerceIn(0.02f, 0.98f)

    Canvas(modifier = modifier.fillMaxSize()) {
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
    content: @Composable () -> Unit
) {
    val isMondrian = themeSettings.currentThemeName.equals("Mondrian", ignoreCase = true)
    if (!isMondrian) {
        content()
        return
    }

    val layout = remember(song?.id, song?.albumId, song?.album, song?.title) {
        MondrianThemeHelper.generateLayoutForSong(song)
    }

    val progressRatio = if (durationMs > 0L) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0.02f, 0.98f)
    } else {
        0.5f
    }
    val clampedVolumeRatio = volumeRatio.coerceIn(0.02f, 0.98f)

    Box(modifier = modifier) {
        // Layer 1: Base Black content
        content()

        // Layer 2: White content masked ONLY to black segments and grid lines
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    val w = size.width
                    val h = size.height
                    val splitX = w * progressRatio
                    val splitY = h * (1f - clampedVolumeRatio)

                    drawBlackSegmentsMask(
                        w = w,
                        h = h,
                        splitX = splitX,
                        splitY = splitY,
                        layout = layout
                    )

                    drawIntoCanvas { canvas ->
                        val paint = androidx.compose.ui.graphics.Paint().apply {
                            blendMode = BlendMode.SrcIn
                        }
                        canvas.saveLayer(androidx.compose.ui.geometry.Rect(0f, 0f, w, h), paint)
                        drawContent()
                        canvas.restore()
                    }
                }
        ) {
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(
                    primary = Color.White,
                    secondary = Color.White,
                    tertiary = Color.White,
                    onBackground = Color.White,
                    onSurface = Color.White,
                    onSurfaceVariant = Color.White
                )
            ) {
                content()
            }
        }
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
