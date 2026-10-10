package com.travelingtunes.app.core.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.travelingtunes.app.core.model.ColorTheme
import com.travelingtunes.app.core.model.DisplaySettings
import com.travelingtunes.app.core.model.ThemeSettings
import java.util.Calendar

data class CalculatedThemeColors(
    val backgroundColor: Color,
    val textColor: Color,
    val primaryColor: Color
)

fun blendColors(base: Color, overlay: Color, ratio: Float): Color {
    val r = (base.red + (overlay.red - base.red) * ratio).coerceIn(0f, 1f)
    val g = (base.green + (overlay.green - base.green) * ratio).coerceIn(0f, 1f)
    val b = (base.blue + (overlay.blue - base.blue) * ratio).coerceIn(0f, 1f)
    return Color(red = r, green = g, blue = b, alpha = 1f)
}

fun buildColorSchemeFromTheme(
    activeTheme: ColorTheme,
    isMatchedTheme: Boolean = false
): ColorScheme {
    val bg = activeTheme.backgroundColor
    val isDark = bg.luminance() < 0.5f

    // Derive surface containers directly from the album art palette background
    val surfaceContainerLowest = blendColors(bg, activeTheme.textColor, if (isDark) 0.03f else 0.02f)
    val surfaceContainerLow = blendColors(bg, activeTheme.textColor, if (isDark) 0.06f else 0.04f)
    val surfaceContainer = blendColors(bg, activeTheme.textColor, if (isDark) 0.10f else 0.07f)
    val surfaceContainerHigh = blendColors(bg, activeTheme.textColor, if (isDark) 0.14f else 0.10f)
    val surfaceContainerHighest = blendColors(bg, activeTheme.textColor, if (isDark) 0.18f else 0.14f)
    val surfaceVariant = blendColors(bg, activeTheme.textColor, if (isDark) 0.15f else 0.12f)

    // Primary text / onSurface for menus, popups, and dialog containers
    val onSurface = adjustContrastForBackground(
        textColor = activeTheme.textColor,
        backgroundColor = surfaceContainerHighest,
        matchedSwatches = activeTheme.matchedSwatches,
        isMatchedTheme = isMatchedTheme,
        minContrastRatio = 4.5
    )

    // Secondary text / onSurfaceVariant for submenus, popups, descriptions, and option cards
    val onSurfaceVariant = adjustSecondaryContrastForBackground(
        secondaryColor = activeTheme.secondaryTextColor,
        primaryColor = onSurface,
        backgroundColor = surfaceContainerHighest,
        matchedSwatches = activeTheme.matchedSwatches,
        isMatchedTheme = isMatchedTheme,
        minContrastRatio = 4.5
    )

    // Primary Container for selected menu items, cards, and primary buttons
    val primaryContainer = blendColors(bg, activeTheme.textColor, if (isDark) 0.24f else 0.18f)
    val onPrimaryContainer = adjustContrastForBackground(
        textColor = activeTheme.textColor,
        backgroundColor = primaryContainer,
        matchedSwatches = activeTheme.matchedSwatches,
        isMatchedTheme = isMatchedTheme,
        minContrastRatio = 4.5
    )

    // Secondary Container for active controls, badges, and secondary highlights
    val secondaryContainer = blendColors(bg, activeTheme.artistColor, if (isDark) 0.22f else 0.16f)
    val onSecondaryContainer = adjustContrastForBackground(
        textColor = activeTheme.artistColor,
        backgroundColor = secondaryContainer,
        matchedSwatches = activeTheme.matchedSwatches,
        isMatchedTheme = isMatchedTheme,
        minContrastRatio = 4.5
    )

    // Tertiary Container
    val tertiaryContainer = blendColors(bg, activeTheme.albumColor, if (isDark) 0.22f else 0.16f)
    val onTertiaryContainer = adjustContrastForBackground(
        textColor = activeTheme.albumColor,
        backgroundColor = tertiaryContainer,
        matchedSwatches = activeTheme.matchedSwatches,
        isMatchedTheme = isMatchedTheme,
        minContrastRatio = 4.5
    )

    // Outlines / Borders for popups, dropdown menus, dialogs, cards
    val outline = adjustContrastForBackground(
        textColor = activeTheme.secondaryTextColor,
        backgroundColor = bg,
        matchedSwatches = activeTheme.matchedSwatches,
        isMatchedTheme = isMatchedTheme,
        minContrastRatio = 3.5
    )
    val outlineVariant = blendColors(bg, activeTheme.textColor, if (isDark) 0.22f else 0.16f)

    return if (isDark) {
        darkColorScheme(
            primary = onSurface,
            onPrimary = bg,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = activeTheme.artistColor,
            onSecondary = bg,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = activeTheme.albumColor,
            onTertiary = bg,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = bg,
            onBackground = onSurface,
            surface = bg,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            surfaceTint = activeTheme.textColor,
            inverseSurface = onSurface,
            inverseOnSurface = bg,
            inversePrimary = activeTheme.artistColor,
            outline = outline,
            outlineVariant = outlineVariant,
            scrim = Color.Black.copy(alpha = 0.6f),
            surfaceContainer = surfaceContainer,
            surfaceContainerLowest = surfaceContainerLowest,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest
        )
    } else {
        lightColorScheme(
            primary = onSurface,
            onPrimary = bg,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = activeTheme.artistColor,
            onSecondary = bg,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = activeTheme.albumColor,
            onTertiary = bg,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = bg,
            onBackground = onSurface,
            surface = bg,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            surfaceTint = activeTheme.textColor,
            inverseSurface = onSurface,
            inverseOnSurface = bg,
            inversePrimary = activeTheme.artistColor,
            outline = outline,
            outlineVariant = outlineVariant,
            scrim = Color.Black.copy(alpha = 0.6f),
            surfaceContainer = surfaceContainer,
            surfaceContainerLowest = surfaceContainerLowest,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest
        )
    }
}

@Composable
fun TravelingTunesTheme(
    themeSettings: ThemeSettings,
    dynamicAlbumArtTheme: ColorTheme? = null,
    useAlbumArtColors: Boolean = true,
    displaySettings: DisplaySettings? = null,
    content: @Composable () -> Unit
) {
    val isMatchedTheme = useAlbumArtColors && dynamicAlbumArtTheme != null && (
        themeSettings.currentThemeName.equals("Match Album Art", ignoreCase = true) ||
        themeSettings.currentThemeName.equals("Auto By Art", ignoreCase = true)
    )
    val activeTheme = resolveActiveTheme(themeSettings, dynamicAlbumArtTheme, useAlbumArtColors, displaySettings)
    val colorScheme = buildColorSchemeFromTheme(activeTheme, isMatchedTheme)

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

fun rgbToHsl(colorInt: Int, outHsl: FloatArray) {
    val r = ((colorInt shr 16) and 0xFF) / 255.0f
    val g = ((colorInt shr 8) and 0xFF) / 255.0f
    val b = (colorInt and 0xFF) / 255.0f

    val max = maxOf(r, maxOf(g, b))
    val min = minOf(r, minOf(g, b))
    val delta = max - min

    val l = (max + min) / 2.0f
    var h = 0.0f
    var s = 0.0f

    if (delta != 0.0f) {
        s = if (l < 0.5f) delta / (max + min) else delta / (2.0f - max - min)
        h = when (max) {
            r -> (g - b) / delta + (if (g < b) 6.0f else 0.0f)
            g -> (b - r) / delta + 2.0f
            else -> (r - g) / delta + 4.0f
        }
        h *= 60.0f
    }

    outHsl[0] = h
    outHsl[1] = s
    outHsl[2] = l
}

fun colorDistanceLAB(c1: Int, c2: Int): Double {
    fun rgbToLab(colorInt: Int): DoubleArray {
        var r = ((colorInt shr 16) and 0xFF) / 255.0
        var g = ((colorInt shr 8) and 0xFF) / 255.0
        var b = (colorInt and 0xFF) / 255.0

        r = if (r > 0.04045) Math.pow((r + 0.055) / 1.055, 2.4) else r / 12.92
        g = if (g > 0.04045) Math.pow((g + 0.055) / 1.055, 2.4) else g / 12.92
        b = if (b > 0.04045) Math.pow((b + 0.055) / 1.055, 2.4) else b / 12.92

        var x = (r * 0.4124 + g * 0.3576 + b * 0.1805) / 0.95047
        var y = (r * 0.2126 + g * 0.7152 + b * 0.0722) / 1.00000
        var z = (r * 0.0193 + g * 0.1192 + b * 0.9505) / 1.08883

        x = if (x > 0.008856) Math.pow(x, 1.0 / 3.0) else (7.787 * x) + (16.0 / 116.0)
        y = if (y > 0.008856) Math.pow(y, 1.0 / 3.0) else (7.787 * y) + (16.0 / 116.0)
        z = if (z > 0.008856) Math.pow(z, 1.0 / 3.0) else (7.787 * z) + (16.0 / 116.0)

        val lVal = (116.0 * y) - 16.0
        val aVal = 500.0 * (x - y)
        val bVal = 200.0 * (y - z)
        return doubleArrayOf(lVal, aVal, bVal)
    }

    val lab1 = rgbToLab(c1)
    val lab2 = rgbToLab(c2)
    val dL = lab1[0] - lab2[0]
    val dA = lab1[1] - lab2[1]
    val dB = lab1[2] - lab2[2]
    return kotlin.math.sqrt(dL * dL + dA * dA + dB * dB)
}

private fun calculateHueDifferenceDegrees(h1: Float, h2: Float): Float {
    val diff = kotlin.math.abs(h1 - h2)
    return minOf(diff, 360f - diff)
}

fun adjustSecondaryContrastForBackground(
    secondaryColor: Color,
    primaryColor: Color,
    backgroundColor: Color,
    matchedSwatches: List<Int> = emptyList(),
    isMatchedTheme: Boolean = false,
    minContrastRatio: Double = 4.5
): Color {
    val bgInt = (backgroundColor.toArgb() and 0x00FFFFFF) or -0x1000000
    val primaryInt = (primaryColor.toArgb() and 0x00FFFFFF) or -0x1000000
    val secInt = (secondaryColor.toArgb() and 0x00FFFFFF) or -0x1000000

    val currentContrastToBg = calculateWcagContrast(secInt, bgInt)
    val currentDistToPrimary = colorDistanceLAB(secInt, primaryInt)

    val primaryHsl = FloatArray(3)
    val secHsl = FloatArray(3)
    rgbToHsl(primaryInt, primaryHsl)
    rgbToHsl(secInt, secHsl)

    val currentHueDiff = calculateHueDifferenceDegrees(primaryHsl[0], secHsl[0])
    val isSaturated = primaryHsl[1] >= 0.15f && secHsl[1] >= 0.15f

    // If current secondary color already has good contrast to bg, good distance to primary, and different hue (if saturated), keep it!
    if (currentContrastToBg >= minContrastRatio && currentDistToPrimary >= 25.0 && (!isSaturated || currentHueDiff >= 25f)) {
        return secondaryColor
    }

    if (isMatchedTheme && matchedSwatches.isNotEmpty()) {
        // Pass 1: Strict search through matchedSwatches (bg contrast >= minContrastRatio, distToPrimary >= 25.0, hueDiff >= 25)
        val bestDistinctSwatch = matchedSwatches.find { swatchInt ->
            val swatchOpaque = (swatchInt and 0x00FFFFFF) or -0x1000000
            if (swatchOpaque == primaryInt) return@find false

            val contrastToBg = calculateWcagContrast(swatchOpaque, bgInt)
            if (contrastToBg < minContrastRatio) return@find false

            val distToPrimary = colorDistanceLAB(swatchOpaque, primaryInt)
            if (distToPrimary < 25.0) return@find false

            val candHsl = FloatArray(3)
            rgbToHsl(swatchOpaque, candHsl)
            if (primaryHsl[1] >= 0.15f && candHsl[1] >= 0.15f) {
                val hueDiff = calculateHueDifferenceDegrees(primaryHsl[0], candHsl[0])
                if (hueDiff < 25f) return@find false
            }

            true
        }

        if (bestDistinctSwatch != null) {
            return Color((bestDistinctSwatch and 0x00FFFFFF) or -0x1000000)
        }

        // Pass 2: Relaxed search through matchedSwatches (bg contrast >= minContrastRatio, distToPrimary >= 18.0)
        val relaxedSwatch = matchedSwatches.find { swatchInt ->
            val swatchOpaque = (swatchInt and 0x00FFFFFF) or -0x1000000
            if (swatchOpaque == primaryInt) return@find false

            val contrastToBg = calculateWcagContrast(swatchOpaque, bgInt)
            if (contrastToBg < minContrastRatio) return@find false

            val distToPrimary = colorDistanceLAB(swatchOpaque, primaryInt)
            if (distToPrimary < 18.0) return@find false

            val candHsl = FloatArray(3)
            rgbToHsl(swatchOpaque, candHsl)
            if (primaryHsl[1] >= 0.15f && candHsl[1] >= 0.15f) {
                val hueDiff = calculateHueDifferenceDegrees(primaryHsl[0], candHsl[0])
                if (hueDiff < 18f) return@find false
            }

            true
        }

        if (relaxedSwatch != null) {
            return Color((relaxedSwatch and 0x00FFFFFF) or -0x1000000)
        }
    }

    // Pass 3: Synthesize a secondary color by rotating hue from primaryColor
    if (primaryHsl[1] >= 0.15f) {
        primaryHsl[0] = (primaryHsl[0] + 45f) % 360f
    } else {
        val bgLum = calculateLuminance(bgInt)
        if (bgLum < 0.5) {
            primaryHsl[2] = (primaryHsl[2] - 0.30f).coerceIn(0.60f, 0.90f)
        } else {
            primaryHsl[2] = (primaryHsl[2] + 0.30f).coerceIn(0.10f, 0.40f)
        }
    }
    val synthInt = ColorUtils.HSLToColor(primaryHsl) or -0x1000000
    if (calculateWcagContrast(synthInt, bgInt) >= minContrastRatio) {
        return Color(synthInt)
    }

    return adjustContrastForBackground(secondaryColor, backgroundColor, matchedSwatches, isMatchedTheme, minContrastRatio)
}

fun applySaturationToColor(colorInt: Int, saturation: Float): Int {
    if (saturation >= 0.999f) return colorInt
    val sat = saturation.coerceIn(0.0f, 1.0f)
    val r = (colorInt shr 16) and 0xFF
    val g = (colorInt shr 8) and 0xFF
    val b = colorInt and 0xFF

    val gray = (0.2126f * r + 0.7152f * g + 0.0722f * b)
    val newR = (gray + sat * (r - gray)).toInt().coerceIn(0, 255)
    val newG = (gray + sat * (g - gray)).toInt().coerceIn(0, 255)
    val newB = (gray + sat * (b - gray)).toInt().coerceIn(0, 255)
    return (colorInt and -0x1000000) or (newR shl 16) or (newG shl 8) or newB
}

fun blendOver(fgColorInt: Int, bgColorInt: Int, alpha: Float): Int {
    val a = alpha.coerceIn(0.0f, 1.0f)
    if (a <= 0.001f) return bgColorInt
    if (a >= 0.999f) return fgColorInt

    val fgR = (fgColorInt shr 16) and 0xFF
    val fgG = (fgColorInt shr 8) and 0xFF
    val fgB = fgColorInt and 0xFF

    val bgR = (bgColorInt shr 16) and 0xFF
    val bgG = (bgColorInt shr 8) and 0xFF
    val bgB = bgColorInt and 0xFF

    val r = (fgR * a + bgR * (1f - a)).toInt().coerceIn(0, 255)
    val g = (fgG * a + bgG * (1f - a)).toInt().coerceIn(0, 255)
    val b = (fgB * a + bgB * (1f - a)).toInt().coerceIn(0, 255)

    return -0x1000000 or (r shl 16) or (g shl 8) or b
}

fun calculateRenderedPageBackground(
    baseBgColor: Color,
    artBgColor: Color,
    displaySettings: DisplaySettings?
): Color {
    if (displaySettings == null || !displaySettings.showAlbumArt) {
        return baseBgColor
    }
    val isDocked = displaySettings.artDisplayLayout == com.travelingtunes.app.core.model.ArtLayoutOption.DOCKED && !displaySettings.adaptiveDockedArt
    if (isDocked) {
        return baseBgColor
    }

    val fadeAlpha = displaySettings.albumArtFade.coerceIn(0.0f, 1.0f)
    val saturation = displaySettings.albumArtSaturation.coerceIn(0.0f, 1.0f)

    val baseInt = (baseBgColor.toArgb() and 0x00FFFFFF) or -0x1000000
    val artInt = (artBgColor.toArgb() and 0x00FFFFFF) or -0x1000000

    val desaturatedArtInt = applySaturationToColor(artInt, saturation)
    val blendedInt = blendOver(desaturatedArtInt, baseInt, fadeAlpha)

    return Color(blendedInt)
}

fun resolveActiveTheme(
    themeSettings: ThemeSettings,
    dynamicAlbumArtTheme: ColorTheme? = null,
    useAlbumArtColors: Boolean = true,
    displaySettings: DisplaySettings? = null
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

    val baseBgColor = if (isMatchedTheme) {
        val staticTheme = ColorTheme.getByName(themeSettings.currentThemeName)
        if (staticTheme == ColorTheme.MATCH_ALBUM_ART && !themeSettings.currentThemeName.equals("Match Album Art", ignoreCase = true) && !themeSettings.currentThemeName.equals("Auto By Art", ignoreCase = true)) {
            baseTheme.backgroundColor
        } else {
            staticTheme.backgroundColor
        }
    } else {
        baseTheme.backgroundColor
    }

    val renderedBgColor = calculateRenderedPageBackground(
        baseBgColor = baseBgColor,
        artBgColor = baseTheme.backgroundColor,
        displaySettings = displaySettings
    )

    val highContrastText = adjustContrastForBackground(
        textColor = baseTheme.textColor,
        backgroundColor = renderedBgColor,
        matchedSwatches = baseTheme.matchedSwatches,
        isMatchedTheme = isMatchedTheme
    )
    val highContrastArtist = adjustSecondaryContrastForBackground(
        secondaryColor = baseTheme.artistColor,
        primaryColor = highContrastText,
        backgroundColor = renderedBgColor,
        matchedSwatches = baseTheme.matchedSwatches,
        isMatchedTheme = isMatchedTheme
    )
    val highContrastAlbum = adjustSecondaryContrastForBackground(
        secondaryColor = baseTheme.albumColor,
        primaryColor = highContrastText,
        backgroundColor = renderedBgColor,
        matchedSwatches = baseTheme.matchedSwatches,
        isMatchedTheme = isMatchedTheme
    )
    val highContrastSecondary = adjustSecondaryContrastForBackground(
        secondaryColor = baseTheme.secondaryTextColor,
        primaryColor = highContrastText,
        backgroundColor = renderedBgColor,
        matchedSwatches = baseTheme.matchedSwatches,
        isMatchedTheme = isMatchedTheme
    )

    return baseTheme.copy(
        textColor = highContrastText,
        secondaryTextColor = highContrastSecondary,
        artistColor = highContrastArtist,
        albumColor = highContrastAlbum
    )
}

fun Color.luminance(): Float {
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}
