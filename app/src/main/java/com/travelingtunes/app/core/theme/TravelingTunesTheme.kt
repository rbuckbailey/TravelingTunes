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

fun resolveActiveTheme(
    themeSettings: ThemeSettings,
    dynamicAlbumArtTheme: ColorTheme? = null,
    useAlbumArtColors: Boolean = true
): ColorTheme {
    if (useAlbumArtColors && dynamicAlbumArtTheme != null) {
        return dynamicAlbumArtTheme
    }

    val baseTheme = if (themeSettings.currentThemeName.equals("Custom", ignoreCase = true)) {
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

    return if (isNight && themeSettings.invertAtNight) {
        ColorTheme(
            name = baseTheme.name + " (Inverted)",
            backgroundColor = baseTheme.textColor,
            textColor = baseTheme.backgroundColor,
            secondaryTextColor = baseTheme.backgroundColor,
            artistColor = baseTheme.backgroundColor,
            albumColor = baseTheme.backgroundColor
        )
    } else {
        baseTheme
    }
}

fun Color.luminance(): Float {
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}
