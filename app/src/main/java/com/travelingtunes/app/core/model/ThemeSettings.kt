package com.travelingtunes.app.core.model

import androidx.compose.ui.graphics.Color

data class ColorTheme(
    val name: String,
    val backgroundColor: Color,
    val textColor: Color
) {
    companion object {
        val WHITE_ON_GREY = ColorTheme("White on Grey", Color(0xFFAAAAAA), Color(0xFFFFFFFF))
        val GREY_ON_BLACK = ColorTheme("Grey on Black", Color(0xFF000000), Color(0xFFBEBEBE))
        val LEAF = ColorTheme("Leaf", Color(0xFFCDF105), Color(0xFF62801D))
        val OLD_WEST = ColorTheme("Old West", Color(0xFFF1C392), Color(0xFF774427))
        val PERIWINKLE_BLUE = ColorTheme("Periwinkle Blue", Color(0xFF625BFF), Color(0xFF9BB2FF))
        val LAVENDER = ColorTheme("Lavender", Color(0xFFC3C0FF), Color(0xFFFFFFFF))
        val BLUSH = ColorTheme("Blush", Color(0xFFFFBCC4), Color(0xFFFFEFF2))
        val HOT_DOG_STAND = ColorTheme("Hot Dog Stand", Color(0xFFFFFF00), Color(0xFFFF0000))
        val AUTO_BY_ART = ColorTheme("Auto By Art", Color(0xFF1E1E2C), Color(0xFFE0E0E0))
        val CUSTOM = ColorTheme("Custom", Color(0xFFC8C864), Color(0xFF1616B4))

        val PRESETS = listOf(
            WHITE_ON_GREY, GREY_ON_BLACK, LEAF, OLD_WEST,
            PERIWINKLE_BLUE, LAVENDER, BLUSH, HOT_DOG_STAND, AUTO_BY_ART, CUSTOM
        )

        fun getByName(name: String): ColorTheme {
            return PRESETS.find { it.name.equals(name, ignoreCase = true) } ?: WHITE_ON_GREY
        }
    }
}

data class ThemeSettings(
    val currentThemeName: String = ColorTheme.WHITE_ON_GREY.name,
    val customTextRed: Float = 22f,
    val customTextGreen: Float = 22f,
    val customTextBlue: Float = 180f,
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
