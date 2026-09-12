package com.travelingtunes.app

import com.travelingtunes.app.core.media.ArtworkCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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

    private fun resolveArtist(rawArtist: String?, relativePath: String): String {
        val folderName = when {
            relativePath.isNotBlank() -> relativePath.substringAfterLast('/')
            else -> "Music"
        }
        return if (!rawArtist.isNullOrBlank() && !rawArtist.equals("Unknown", ignoreCase = true) && !rawArtist.equals("Unknown Artist", ignoreCase = true) && !rawArtist.equals("<unknown>", ignoreCase = true)) {
            rawArtist
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
    fun testFolderFallbackForArtistName() {
        val relativePath = "Music/Rock/Incubus"
        assertEquals("Incubus", resolveArtist(null, relativePath))
        assertEquals("Incubus", resolveArtist("Unknown Artist", relativePath))
        assertEquals("Incubus", resolveArtist("<unknown>", relativePath))
        assertEquals("Incubus", resolveArtist("Unknown", relativePath))
    }

    @Test
    fun testMetadataPreservedWhenValid() {
        val fileName = "01 - Drive.mp3"
        val relativePath = "Music/Rock/Make Yourself"
        assertEquals("Drive", resolveTitle("Drive", fileName))
        assertEquals("Make Yourself", resolveAlbum("Make Yourself", relativePath))
        assertEquals("Incubus", resolveArtist("Incubus", relativePath))
    }

    @Test
    fun testArtworkCandidateSelection() {
        val candidates = listOf(
            ArtworkCandidate("https://example.com/lowres.jpg", 300, 300),
            ArtworkCandidate("https://example.com/rectangular.jpg", 1200, 800),
            ArtworkCandidate("https://example.com/highres_square.jpg", 1000, 1000)
        )

        // Select best candidate (squarest & highest resolution)
        val best = candidates.maxWithOrNull(
            compareBy<ArtworkCandidate> { it.squareness }
                .thenBy { it.resolution }
        )

        assertNotNull(best)
        assertEquals("https://example.com/highres_square.jpg", best?.url)
        assertEquals(1000, best?.width)
        assertEquals(1000, best?.height)
        assertEquals(1.0, best?.squareness ?: 0.0, 0.001)
    }
}
