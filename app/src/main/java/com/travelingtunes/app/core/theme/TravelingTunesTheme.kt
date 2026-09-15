package com.travelingtunes.app.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.travelingtunes.app.core.model.ColorTheme
import com.travelingtunes.app.core.model.ThemeSettings
import java.util.Calendar

data class CalculatedThemeColors(
    val backgroundColor: Color,
    val textColor: Color,
    val primaryColor: Color
)

@Composable
fun TravelingTunesTheme(
    themeSettings: ThemeSettings,
    dynamicAlbumArtTheme: ColorTheme? = null,
    useAlbumArtColors: Boolean = true,
    content: @Composable () -> Unit
) {
    val activeTheme = resolveActiveTheme(themeSettings, dynamicAlbumArtTheme, useAlbumArtColors)

    val colorScheme = if (activeTheme.backgroundColor.luminance() < 0.5f) {
        darkColorScheme(
            background = activeTheme.backgroundColor,
            surface = activeTheme.backgroundColor,
            onBackground = activeTheme.textColor,
            onSurface = activeTheme.textColor,
            primary = activeTheme.textColor,
            secondary = activeTheme.artistColor,
            tertiary = activeTheme.albumColor,
            onSurfaceVariant = activeTheme.secondaryTextColor
        )
    } else {
        lightColorScheme(
            background = activeTheme.backgroundColor,
            surface = activeTheme.backgroundColor,
            onBackground = activeTheme.textColor,
            onSurface = activeTheme.textColor,
            primary = activeTheme.textColor,
            secondary = activeTheme.artistColor,
            tertiary = activeTheme.albumColor,
            onSurfaceVariant = activeTheme.secondaryTextColor
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

fun calculateWcagContrast(foregroundInt: Int, backgroundInt: Int): Double {
    val l1 = calculateLuminance(foregroundInt)
    val l2 = calculateLuminance(backgroundInt)
    val lighter = maxOf(l1, l2)
    val darker = minOf(l1, l2)
    return (lighter + 0.05) / (darker + 0.05)
}

fun calculateLuminance(colorInt: Int): Double {
    val r = ((colorInt shr 16) and 0xFF) / 255.0
    val g = ((colorInt shr 8) and 0xFF) / 255.0
    val b = (colorInt and 0xFF) / 255.0

    val rL = if (r <= 0.03928) r / 12.92 else Math.pow((r + 0.055) / 1.055, 2.4)
    val gL = if (g <= 0.03928) g / 12.92 else Math.pow((g + 0.055) / 1.055, 2.4)
    val bL = if (b <= 0.03928) b / 12.92 else Math.pow((b + 0.055) / 1.055, 2.4)

    return 0.2126 * rL + 0.7152 * gL + 0.0722 * bL
}

fun adjustContrastForBackground(
    textColor: Color,
    backgroundColor: Color,
    matchedSwatches: List<Int> = emptyList(),
    isMatchedTheme: Boolean = false,
    minContrastRatio: Double = 4.5
): Color {
    val bgInt = (backgroundColor.toArgb() and 0x00FFFFFF) or -0x1000000
    val textInt = (textColor.toArgb() and 0x00FFFFFF) or -0x1000000

    val currentContrast = calculateWcagContrast(textInt, bgInt)
    if (currentContrast >= minContrastRatio) {
        return textColor
    }

    // Pass 1: If in Matched Theme, cycle through matched swatches to select high-contrast title color
    if (isMatchedTheme && matchedSwatches.isNotEmpty()) {
        val bestMatched = matchedSwatches.find { swatchInt ->
            val swatchOpaque = (swatchInt and 0x00FFFFFF) or -0x1000000
            calculateWcagContrast(swatchOpaque, bgInt) >= minContrastRatio
        }
        if (bestMatched != null) {
            return Color((bestMatched and 0x00FFFFFF) or -0x1000000)
        }

        val maxContrastSwatch = matchedSwatches.maxByOrNull { swatchInt ->
            val swatchOpaque = (swatchInt and 0x00FFFFFF) or -0x1000000
            calculateWcagContrast(swatchOpaque, bgInt)
        }
        if (maxContrastSwatch != null) {
            val maxOpaque = (maxContrastSwatch and 0x00FFFFFF) or -0x1000000
            if (calculateWcagContrast(maxOpaque, bgInt) >= 3.0) {
                return Color(maxOpaque)
            }
        }
    }

    // Pass 2: Static / Custom theme pass: Alter Brightness & Saturation via HSL to make text stand out
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(textInt, hsl)

    val bgLum = calculateLuminance(bgInt)
    val isBgDark = bgLum < 0.5

    var bestAdjustedColorInt = textInt
    var bestContrast = currentContrast

    for (step in 1..10) {
        if (isBgDark) {
            hsl[2] = (hsl[2] + 0.08f * step).coerceIn(0.60f, 1.0f)
            hsl[1] = (hsl[1] * 1.15f).coerceIn(0.20f, 1.0f)
        } else {
            hsl[2] = (hsl[2] - 0.08f * step).coerceIn(0.0f, 0.35f)
        }

        val rawAdjustedInt = ColorUtils.HSLToColor(hsl)
        val adjustedColorInt = (rawAdjustedInt and 0x00FFFFFF) or -0x1000000
        val contrast = calculateWcagContrast(adjustedColorInt, bgInt)
        if (contrast > bestContrast) {
            bestContrast = contrast
            bestAdjustedColorInt = adjustedColorInt
        }

        if (contrast >= minContrastRatio) {
            return Color(adjustedColorInt)
        }
    }

    if (bestContrast < minContrastRatio) {
        val whiteContrast = calculateWcagContrast(android.graphics.Color.WHITE, bgInt)
        val blackContrast = calculateWcagContrast(android.graphics.Color.BLACK, bgInt)
        return if (whiteContrast >= blackContrast) Color.White else Color.Black
    }

    return Color(bestAdjustedColorInt)
}

fun resolveActiveTheme(
    themeSettings: ThemeSettings,
    dynamicAlbumArtTheme: ColorTheme? = null,
    useAlbumArtColors: Boolean = true
): ColorTheme {
    val isMatchedTheme = useAlbumArtColors && dynamicAlbumArtTheme != null && (
        themeSettings.currentThemeName.equals("Match Album Art", ignoreCase = true) ||
        themeSettings.currentThemeName.equals("Auto By Art", ignoreCase = true)
    )

    if (themeSettings.currentThemeName.equals("Mondrian", ignoreCase = true)) {
        return ColorTheme(
            name = "Mondrian",
            backgroundColor = Color.White,
            textColor = Color.Black,
            secondaryTextColor = Color.Black,
            artistColor = Color.Black,
            albumColor = Color.Black
        )
    }

    val rawTheme = if (isMatchedTheme) {
        dynamicAlbumArtTheme
    } else if (themeSettings.currentThemeName.equals("Custom", ignoreCase = true)) {
        val songColor = Color(
            red = (themeSettings.customSongTitleRed / 255f).coerceIn(0f, 1f),
            green = (themeSettings.customSongTitleGreen / 255f).coerceIn(0f, 1f),
            blue = (themeSettings.customSongTitleBlue / 255f).coerceIn(0f, 1f)
        )
        val artistColor = Color(
            red = (themeSettings.customArtistTitleRed / 255f).coerceIn(0f, 1f),
            green = (themeSettings.customArtistTitleGreen / 255f).coerceIn(0f, 1f),
            blue = (themeSettings.customArtistTitleBlue / 255f).coerceIn(0f, 1f)
        )
        val albumColor = Color(
            red = (themeSettings.customAlbumTitleRed / 255f).coerceIn(0f, 1f),
            green = (themeSettings.customAlbumTitleGreen / 255f).coerceIn(0f, 1f),
            blue = (themeSettings.customAlbumTitleBlue / 255f).coerceIn(0f, 1f)
        )
        ColorTheme(
            name = "Custom",
            backgroundColor = Color(
                red = (themeSettings.customBGRed / 255f).coerceIn(0f, 1f),
                green = (themeSettings.customBGGreen / 255f).coerceIn(0f, 1f),
                blue = (themeSettings.customBGBlue / 255f).coerceIn(0f, 1f)
            ),
            textColor = songColor,
            secondaryTextColor = artistColor,
            artistColor = artistColor,
            albumColor = albumColor
        )
    } else {
        ColorTheme.getByName(themeSettings.currentThemeName)
    }

    // Handle Night Dimming / Inversion
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val isNight = currentHour >= themeSettings.sunSetHour || currentHour < themeSettings.sunRiseHour

    val baseTheme = if (isNight && themeSettings.invertAtNight) {
        ColorTheme(
            name = rawTheme.name + " (Inverted)",
            backgroundColor = rawTheme.textColor,
            textColor = rawTheme.backgroundColor,
            secondaryTextColor = rawTheme.backgroundColor,
            artistColor = rawTheme.backgroundColor,
            albumColor = rawTheme.backgroundColor,
            matchedSwatches = rawTheme.matchedSwatches
        )
    } else {
        rawTheme
    }

    val highContrastText = adjustContrastForBackground(
        textColor = baseTheme.textColor,
        backgroundColor = baseTheme.backgroundColor,
        matchedSwatches = baseTheme.matchedSwatches,
        isMatchedTheme = isMatchedTheme
    )
    val highContrastArtist = adjustContrastForBackground(
        textColor = baseTheme.artistColor,
        backgroundColor = baseTheme.backgroundColor,
        matchedSwatches = baseTheme.matchedSwatches,
        isMatchedTheme = isMatchedTheme
    )
    val highContrastAlbum = adjustContrastForBackground(
        textColor = baseTheme.albumColor,
        backgroundColor = baseTheme.backgroundColor,
        matchedSwatches = baseTheme.matchedSwatches,
        isMatchedTheme = isMatchedTheme
    )

    return baseTheme.copy(
        textColor = highContrastText,
        artistColor = highContrastArtist,
        albumColor = highContrastAlbum
    )
}

fun Color.luminance(): Float {
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}
