package com.travelingtunes.app

import org.junit.Assert.assertEquals
import org.junit.Test

class MetadataFallbackTest {

    private fun resolveTitle(rawTitle: String?, fileName: String): String {
        val cleanFileName = fileName.substringBeforeLast('.').ifBlank { fileName }
        return if (!rawTitle.isNullOrBlank() && !rawTitle.equals("Unknown", ignoreCase = true) && !rawTitle.equals("Unknown Title", ignoreCase = true) && !rawTitle.equals("<unknown>", ignoreCase = true)) {
            rawTitle
        } else {
            cleanFileName
        }
    }

    private fun resolveAlbum(rawAlbum: String?, relativePath: String): String {
        val folderName = when {
            relativePath.isNotBlank() -> relativePath.substringAfterLast('/')
            else -> "Music"
        }
        return if (!rawAlbum.isNullOrBlank() && !rawAlbum.equals("Unknown", ignoreCase = true) && !rawAlbum.equals("Unknown Album", ignoreCase = true) && !rawAlbum.equals("<unknown>", ignoreCase = true)) {
            rawAlbum
        } else {
            folderName
        }
    }

    @Test
    fun testFilenameFallbackForSongTitle() {
        val fileName = "01 - Drive.mp3"
        assertEquals("01 - Drive", resolveTitle(null, fileName))
        assertEquals("01 - Drive", resolveTitle("Unknown Title", fileName))
        assertEquals("01 - Drive", resolveTitle("<unknown>", fileName))
    }

    @Test
    fun testFolderFallbackForAlbumName() {
        val relativePath = "Music/Rock/Make Yourself"
        assertEquals("Make Yourself", resolveAlbum(null, relativePath))
        assertEquals("Make Yourself", resolveAlbum("Unknown Album", relativePath))
        assertEquals("Make Yourself", resolveAlbum("<unknown>", relativePath))
    }

    @Test
    fun testMetadataPreservedWhenValid() {
        val fileName = "01 - Drive.mp3"
        val relativePath = "Music/Rock/Make Yourself"
        assertEquals("Drive", resolveTitle("Drive", fileName))
        assertEquals("Make Yourself", resolveAlbum("Make Yourself", relativePath))
    }
}
