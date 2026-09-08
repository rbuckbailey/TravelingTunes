package com.travelingtunes.app.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
            primary = activeTheme.textColor
        )
    } else {
        lightColorScheme(
            background = activeTheme.backgroundColor,
            surface = activeTheme.backgroundColor,
            onBackground = activeTheme.textColor,
            onSurface = activeTheme.textColor,
            primary = activeTheme.textColor
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

fun resolveActiveTheme(
    themeSettings: ThemeSettings,
    dynamicAlbumArtTheme: ColorTheme? = null,
    useAlbumArtColors: Boolean = true
): ColorTheme {
    if (useAlbumArtColors && dynamicAlbumArtTheme != null) {
        return dynamicAlbumArtTheme
    }

    val baseTheme = if (themeSettings.currentThemeName.equals("Custom", ignoreCase = true)) {
        ColorTheme(
            name = "Custom",
            backgroundColor = Color(
                red = (themeSettings.customBGRed / 255f).coerceIn(0f, 1f),
                green = (themeSettings.customBGGreen / 255f).coerceIn(0f, 1f),
                blue = (themeSettings.customBGBlue / 255f).coerceIn(0f, 1f)
            ),
            textColor = Color(
                red = (themeSettings.customTextRed / 255f).coerceIn(0f, 1f),
                green = (themeSettings.customTextGreen / 255f).coerceIn(0f, 1f),
                blue = (themeSettings.customTextBlue / 255f).coerceIn(0f, 1f)
            )
        )
    } else {
        ColorTheme.getByName(themeSettings.currentThemeName)
    }

    // Handle Night Dimming / Inversion
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val isNight = currentHour >= themeSettings.sunSetHour || currentHour < themeSettings.sunRiseHour

    return if (isNight && themeSettings.invertAtNight) {
        ColorTheme(
            name = baseTheme.name + " (Inverted)",
            backgroundColor = baseTheme.textColor,
            textColor = baseTheme.backgroundColor
        )
    } else {
        baseTheme
    }
}

fun Color.luminance(): Float {
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}
