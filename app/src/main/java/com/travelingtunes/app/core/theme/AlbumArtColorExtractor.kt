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
        val palette = Palette.from(safeBmp ?: bitmap).generate()

        val allSwatches = palette.swatches.sortedByDescending { it.population }
        if (allSwatches.isEmpty()) {
            return@withContext ColorTheme.WHITE_ON_GREY
        }

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

        // Try background swatches down the line in order of prominence
        for (bgSwatch in allSwatches) {
            val bgInt = bgSwatch.rgb

            // Build unique list of candidate text swatches in order of prominence
            val textCandidates = (preferredTextSwatches + allSwatches)
                .distinctBy { it.rgb }
                .filter { it.rgb != bgInt }

            for (textSwatch in textCandidates) {
                val textInt = textSwatch.rgb
                val contrast = ColorUtils.calculateContrast(textInt, bgInt)
                if (contrast >= targetMinContrast) {
                    return@withContext ColorTheme(
                        name = "Album Art Dynamic",
                        backgroundColor = Color(bgInt),
                        textColor = Color(textInt)
                    )
                }
            }
        }

        // Fallback pass: try threshold 3.0 down the line
        for (bgSwatch in allSwatches) {
            val bgInt = bgSwatch.rgb
            val textCandidates = (preferredTextSwatches + allSwatches)
                .distinctBy { it.rgb }
                .filter { it.rgb != bgInt }

            for (textSwatch in textCandidates) {
                val textInt = textSwatch.rgb
                val contrast = ColorUtils.calculateContrast(textInt, bgInt)
                if (contrast >= 3.0) {
                    return@withContext ColorTheme(
                        name = "Album Art Dynamic",
                        backgroundColor = Color(bgInt),
                        textColor = Color(textInt)
                    )
                }
            }
        }

        // Ultimate fallback: Use dominant background and White or Black text for maximum contrast
        val primaryBgInt = (palette.dominantSwatch ?: allSwatches.first()).rgb
        val whiteContrast = ColorUtils.calculateContrast(android.graphics.Color.WHITE, primaryBgInt)
        val blackContrast = ColorUtils.calculateContrast(android.graphics.Color.BLACK, primaryBgInt)
        val fallbackTextInt = if (whiteContrast >= blackContrast) android.graphics.Color.WHITE else android.graphics.Color.BLACK

        ColorTheme(
            name = "Album Art Dynamic",
            backgroundColor = Color(primaryBgInt),
            textColor = Color(fallbackTextInt)
        )
    }
}
