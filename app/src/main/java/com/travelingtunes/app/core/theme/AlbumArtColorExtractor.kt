package com.travelingtunes.app.core.theme

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import com.travelingtunes.app.core.model.ColorTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AlbumArtColorExtractor {

    suspend fun extractThemeFromBitmap(bitmap: Bitmap): ColorTheme = withContext(Dispatchers.Default) {
        val palette = Palette.from(bitmap).generate()

        val dominantSwatch = palette.dominantSwatch
        val vibrantSwatch = palette.vibrantSwatch ?: palette.lightVibrantSwatch ?: palette.darkVibrantSwatch

        val bgColorInt = dominantSwatch?.rgb ?: Color(0xFFAAAAAA).toArgb()
        val textColorInt = vibrantSwatch?.rgb ?: dominantSwatch?.bodyTextColor ?: Color.White.toArgb()

        ColorTheme(
            name = "Album Art Dynamic",
            backgroundColor = Color(bgColorInt),
            textColor = Color(textColorInt)
        )
    }
}
