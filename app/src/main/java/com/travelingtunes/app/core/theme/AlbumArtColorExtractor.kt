package com.travelingtunes.app.core.theme

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.travelingtunes.app.core.model.ColorTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AlbumArtColorExtractor {

    suspend fun extractThemeFromBitmap(bitmap: Bitmap): ColorTheme = withContext(Dispatchers.Default) {
        val safeBmp = if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }
        val targetBmp = safeBmp ?: bitmap
        val palette = Palette.from(targetBmp).generate()

        val allSwatches = palette.swatches.sortedByDescending { it.population }
        if (allSwatches.isEmpty()) {
            return@withContext ColorTheme.WHITE_ON_GREY
        }

        // Favor edge colors for letterboxing / background extraction
        val edgeSwatches = extractEdgeSwatches(targetBmp)

        // Combine edge swatches first to favor edge colors, followed by full image swatches
        val bgCandidates = (edgeSwatches + allSwatches).distinctBy { it.rgb }

        // Ordered text candidates from palette swatches
        val preferredTextSwatches = listOfNotNull(
            palette.vibrantSwatch,
            palette.lightVibrantSwatch,
            palette.darkVibrantSwatch,
            palette.mutedSwatch,
            palette.lightMutedSwatch,
            palette.darkMutedSwatch
        )

        val targetMinContrast = 3.5 // Minimum contrast ratio threshold for visibility

        // Try background swatches down the line (favoring edge colors) in order of prominence
        for (bgSwatch in bgCandidates) {
            val bgInt = bgSwatch.rgb

            val textCandidates = (preferredTextSwatches + allSwatches)
                .distinctBy { it.rgb }
                .filter { it.rgb != bgInt }

            for (primaryTextSwatch in textCandidates) {
                val primaryInt = primaryTextSwatch.rgb
                val contrast = ColorUtils.calculateContrast(primaryInt, bgInt)
                if (contrast >= targetMinContrast) {
                    val secondaryInt = findSecondaryTextColor(bgInt, primaryInt, textCandidates)
                    return@withContext ColorTheme(
                        name = "Album Art Dynamic",
                        backgroundColor = Color(bgInt),
                        textColor = Color(primaryInt),
                        secondaryTextColor = Color(secondaryInt)
                    )
                }
            }
        }

        // Fallback pass: try threshold 3.0 down the line
        for (bgSwatch in bgCandidates) {
            val bgInt = bgSwatch.rgb
            val textCandidates = (preferredTextSwatches + allSwatches)
                .distinctBy { it.rgb }
                .filter { it.rgb != bgInt }

            for (primaryTextSwatch in textCandidates) {
                val primaryInt = primaryTextSwatch.rgb
                val contrast = ColorUtils.calculateContrast(primaryInt, bgInt)
                if (contrast >= 3.0) {
                    val secondaryInt = findSecondaryTextColor(bgInt, primaryInt, textCandidates)
                    return@withContext ColorTheme(
                        name = "Album Art Dynamic",
                        backgroundColor = Color(bgInt),
                        textColor = Color(primaryInt),
                        secondaryTextColor = Color(secondaryInt)
                    )
                }
            }
        }

        // Ultimate fallback: Use dominant background (favoring edge) and White/Black for text
        val primaryBgInt = (edgeSwatches.firstOrNull() ?: palette.dominantSwatch ?: allSwatches.first()).rgb
        val whiteContrast = ColorUtils.calculateContrast(android.graphics.Color.WHITE, primaryBgInt)
        val blackContrast = ColorUtils.calculateContrast(android.graphics.Color.BLACK, primaryBgInt)
        val fallbackTextInt = if (whiteContrast >= blackContrast) android.graphics.Color.WHITE else android.graphics.Color.BLACK
        val fallbackSecondaryInt = if (whiteContrast >= blackContrast) {
            android.graphics.Color.argb(255, 220, 220, 220)
        } else {
            android.graphics.Color.argb(255, 50, 50, 50)
        }

        ColorTheme(
            name = "Album Art Dynamic",
            backgroundColor = Color(primaryBgInt),
            textColor = Color(fallbackTextInt),
            secondaryTextColor = Color(fallbackSecondaryInt)
        )
    }

    private fun extractEdgeSwatches(bitmap: Bitmap): List<Palette.Swatch> {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            if (width <= 0 || height <= 0) return emptyList()

            val scaledBmp = if (width > 100 || height > 100) {
                Bitmap.createScaledBitmap(bitmap, 100, 100, false)
            } else {
                bitmap
            }

            val sw = scaledBmp.width
            val sh = scaledBmp.height
            val border = (sw * 0.05f).toInt().coerceAtLeast(1)

            val edgePixels = IntArray(sw * border * 2 + (sh - border * 2) * border * 2)
            var index = 0

            // Top border
            for (y in 0 until border) {
                for (x in 0 until sw) {
                    edgePixels[index++] = scaledBmp.getPixel(x, y)
                }
            }
            // Bottom border
            for (y in (sh - border) until sh) {
                for (x in 0 until sw) {
                    edgePixels[index++] = scaledBmp.getPixel(x, y)
                }
            }
            // Left & Right borders (middle)
            for (y in border until (sh - border)) {
                for (x in 0 until border) {
                    edgePixels[index++] = scaledBmp.getPixel(x, y)
                }
                for (x in (sw - border) until sw) {
                    edgePixels[index++] = scaledBmp.getPixel(x, y)
                }
            }

            if (index <= 0) return emptyList()

            val edgeBmp = Bitmap.createBitmap(edgePixels, index, 1, Bitmap.Config.ARGB_8888)
            val edgePalette = Palette.from(edgeBmp).generate()
            edgePalette.swatches.sortedByDescending { it.population }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun findSecondaryTextColor(
        bgInt: Int,
        primaryTextInt: Int,
        candidates: List<Palette.Swatch>
    ): Int {
        for (swatch in candidates) {
            val candInt = swatch.rgb
            if (candInt == primaryTextInt) continue

            val contrastToBg = ColorUtils.calculateContrast(candInt, bgInt)
            if (contrastToBg < 2.5) continue

            val distance = colorDistance(candInt, primaryTextInt)
            if (distance > 30) {
                return candInt
            }
        }

        return blendSecondaryColor(bgInt, primaryTextInt)
    }

    private fun colorDistance(c1: Int, c2: Int): Double {
        val r1 = android.graphics.Color.red(c1)
        val g1 = android.graphics.Color.green(c1)
        val b1 = android.graphics.Color.blue(c1)
        val r2 = android.graphics.Color.red(c2)
        val g2 = android.graphics.Color.green(c2)
        val b2 = android.graphics.Color.blue(c2)
        val dr = r1 - r2
        val dg = g1 - g2
        val db = b1 - b2
        return Math.sqrt((dr * dr + dg * dg + db * db).toDouble())
    }

    private fun blendSecondaryColor(bgInt: Int, primaryTextInt: Int): Int {
        val bgLuminance = ColorUtils.calculateLuminance(bgInt)
        val primaryLuminance = ColorUtils.calculateLuminance(primaryTextInt)

        return if (primaryLuminance > bgLuminance) {
            ColorUtils.blendARGB(primaryTextInt, bgInt, 0.30f)
        } else {
            ColorUtils.blendARGB(primaryTextInt, bgInt, 0.30f)
        }
    }
}
