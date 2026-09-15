package com.travelingtunes.app.core.model

import androidx.compose.ui.graphics.Color

data class ColorTheme(
    val name: String,
    val backgroundColor: Color,
    val textColor: Color,
    val secondaryTextColor: Color = textColor,
    val artistColor: Color = secondaryTextColor,
    val albumColor: Color = secondaryTextColor,
    val matchedSwatches: List<Int> = emptyList()
) {
    companion object {
        val MATCH_ALBUM_ART = ColorTheme("Match Album Art", Color(0xFF1E1E2C), Color(0xFFE0E0E0), Color(0xFFB0B0B0))
        val AUTO_BY_ART = MATCH_ALBUM_ART
        val WHITE_ON_GREY = ColorTheme("White on Grey", Color(0xFFAAAAAA), Color(0xFFFFFFFF), Color(0xFFE0E0E0))
        val GREY_ON_BLACK = ColorTheme("Grey on Black", Color(0xFF000000), Color(0xFFBEBEBE), Color(0xFF888888))
        val LEAF = ColorTheme("Leaf", Color(0xFFCDF105), Color(0xFF62801D), Color(0xFF4A6214))
        val OLD_WEST = ColorTheme("Old West", Color(0xFFF1C392), Color(0xFF774427), Color(0xFF58321C))
        val PERIWINKLE_BLUE = ColorTheme("Periwinkle Blue", Color(0xFF625BFF), Color(0xFF9BB2FF), Color(0xFFC2D2FF))
        val LAVENDER = ColorTheme("Lavender", Color(0xFFC3C0FF), Color(0xFFFFFFFF), Color(0xFFE5E3FF))
        val BLUSH = ColorTheme("Blush", Color(0xFFFFBCC4), Color(0xFFFFEFF2), Color(0xFFF5C2C8))
        val HOT_DOG_STAND = ColorTheme("Hot Dog Stand", Color(0xFFFFFF00), Color(0xFFFF0000), Color(0xFFCC0000))
        val MONDRIAN = ColorTheme("Mondrian", Color(0xFFFFFFFF), Color(0xFF000000), Color(0xFF000000))
        val CUSTOM = ColorTheme("Custom", Color(0xFFC8C864), Color(0xFF1616B4), Color(0xFF1616B4))

        val PRESETS = listOf(
            MATCH_ALBUM_ART, WHITE_ON_GREY, GREY_ON_BLACK, LEAF, OLD_WEST,
            PERIWINKLE_BLUE, LAVENDER, BLUSH, HOT_DOG_STAND, MONDRIAN, CUSTOM
        )

        fun getByName(name: String): ColorTheme {
            if (name.equals("Auto By Art", ignoreCase = true) || name.equals("Match Album Art", ignoreCase = true)) {
                return MATCH_ALBUM_ART
            }
            return PRESETS.find { it.name.equals(name, ignoreCase = true) } ?: MATCH_ALBUM_ART
        }
    }
}

data class ThemeSettings(
    val currentThemeName: String = ColorTheme.MATCH_ALBUM_ART.name,
    val customTextRed: Float = 22f,
    val customTextGreen: Float = 22f,
    val customTextBlue: Float = 180f,
    val customSongTitleRed: Float = 22f,
    val customSongTitleGreen: Float = 22f,
    val customSongTitleBlue: Float = 180f,
    val customArtistTitleRed: Float = 22f,
    val customArtistTitleGreen: Float = 22f,
    val customArtistTitleBlue: Float = 180f,
    val customAlbumTitleRed: Float = 22f,
    val customAlbumTitleGreen: Float = 22f,
    val customAlbumTitleBlue: Float = 180f,
    val customBGRed: Float = 200f,
    val customBGGreen: Float = 200f,
    val customBGBlue: Float = 100f,
    val dimAtNight: Boolean = true,
    val invertAtNight: Boolean = false,
    val sunRiseHour: Int = 6,
    val sunSetHour: Int = 19,
    val isRounded: Boolean = false,
    val isGlass: Boolean = false
)
