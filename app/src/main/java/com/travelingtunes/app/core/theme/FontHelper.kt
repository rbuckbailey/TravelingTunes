package com.travelingtunes.app.core.theme

import android.content.Context
import android.net.Uri
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import java.io.File
import java.io.FileOutputStream

data class FontOption(
    val key: String,
    val displayName: String,
    val isCustom: Boolean = false
)

object FontHelper {

    val BUILTIN_FONTS = listOf(
        FontOption("DEFAULT", "System Default"),
        FontOption("SANS_SERIF", "Sans Serif"),
        FontOption("SERIF", "Serif"),
        FontOption("MONOSPACE", "Monospace"),
        FontOption("CURSIVE", "Cursive")
    )

    fun getAvailableFonts(context: Context): List<FontOption> {
        val fontList = mutableListOf<FontOption>()
        fontList.addAll(BUILTIN_FONTS)

        // 1. Scan imported custom fonts in app files directory
        val customFontsDir = File(context.filesDir, "custom_fonts").apply { mkdirs() }
        val customFiles = customFontsDir.listFiles { _, name ->
            val ext = name.substringAfterLast('.', "").lowercase()
            ext == "ttf" || ext == "otf"
        }
        customFiles?.sortedBy { it.name }?.forEach { file ->
            fontList.add(
                FontOption(
                    key = file.absolutePath,
                    displayName = "✨ ${file.nameWithoutExtension.replace('_', ' ').replace('-', ' ')}",
                    isCustom = true
                )
            )
        }

        // 2. Scan system fonts in /system/fonts/
        val systemFontsDir = File("/system/fonts")
        if (systemFontsDir.exists() && systemFontsDir.isDirectory) {
            val systemFiles = systemFontsDir.listFiles { _, name ->
                val ext = name.substringAfterLast('.', "").lowercase()
                ext == "ttf" || ext == "otf"
            }
            systemFiles?.sortedBy { it.name }?.forEach { file ->
                val nameFormatted = file.nameWithoutExtension.replace('_', ' ').replace('-', ' ')
                fontList.add(
                    FontOption(
                        key = file.absolutePath,
                        displayName = nameFormatted,
                        isCustom = false
                    )
                )
            }
        }

        return fontList
    }

    fun getFontFamily(fontKey: String): FontFamily {
        return when (fontKey) {
            "SANS_SERIF" -> FontFamily.SansSerif
            "SERIF" -> FontFamily.Serif
            "MONOSPACE" -> FontFamily.Monospace
            "CURSIVE" -> FontFamily.Cursive
            "DEFAULT" -> FontFamily.Default
            else -> {
                val file = File(fontKey)
                if (file.exists()) {
                    try {
                        FontFamily(Font(file))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        FontFamily.Default
                    }
                } else {
                    FontFamily.Default
                }
            }
        }
    }

    fun importFontFile(context: Context, uri: Uri): FontOption? {
        return try {
            val customFontsDir = File(context.filesDir, "custom_fonts").apply { mkdirs() }
            val fileName = getFileNameFromUri(context, uri) ?: "imported_font_${System.currentTimeMillis()}.ttf"
            val targetFile = File(customFontsDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (targetFile.exists() && targetFile.length() > 0) {
                FontOption(
                    key = targetFile.absolutePath,
                    displayName = "✨ ${targetFile.nameWithoutExtension.replace('_', ' ').replace('-', ' ')}",
                    isCustom = true
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex)
                    }
                }
            }
        }
        if (name == null) {
            name = uri.path?.substringAfterLast('/')
        }
        return name
    }
}
