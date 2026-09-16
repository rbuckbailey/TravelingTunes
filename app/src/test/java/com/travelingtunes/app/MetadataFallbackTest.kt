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
        val resolved = com.travelingtunes.app.core.media.TrackNumberExtractor.resolveTrackAndTitle(0, "01 - Drive", fileName)
        assertEquals("Drive", resolved.cleanTitle)
        assertEquals(1, resolved.trackNumber)
    }

    @Test
    fun testTrackNumberExtractionFromFilenameWhenMissing() {
        val res1 = com.travelingtunes.app.core.media.TrackNumberExtractor.resolveTrackAndTitle(
            currentTrackNumber = 0,
            title = "01 - Stairway to Heaven",
            fileName = "01 - Stairway to Heaven.mp3"
        )
        assertEquals(1, res1.trackNumber)
        assertEquals("Stairway to Heaven", res1.cleanTitle)

        val res2 = com.travelingtunes.app.core.media.TrackNumberExtractor.resolveTrackAndTitle(
            currentTrackNumber = 0,
            title = "03 Drive",
            fileName = "03 Drive.flac"
        )
        assertEquals(3, res2.trackNumber)
        assertEquals("Drive", res2.cleanTitle)

        val res3 = com.travelingtunes.app.core.media.TrackNumberExtractor.resolveTrackAndTitle(
            currentTrackNumber = 0,
            title = "Hotel California",
            fileName = "12. Hotel California.m4a"
        )
        assertEquals(12, res3.trackNumber)
        assertEquals("Hotel California", res3.cleanTitle)
    }

    @Test
    fun testTrackNumberPreservedWhenAlreadyPresent() {
        val res = com.travelingtunes.app.core.media.TrackNumberExtractor.resolveTrackAndTitle(
            currentTrackNumber = 5,
            title = "05 - Drive",
            fileName = "05 - Drive.mp3"
        )
        assertEquals(5, res.trackNumber)
        assertEquals("Drive", res.cleanTitle)
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

    @Test
    fun testColorDistinctnessSelection_MovesDownTheLineIfTooSimilar() {
        val bgInt = (0xFF000000 or (20 shl 16) or (20 shl 8) or 30).toInt() // Dark blue/black

        // Candidate 1 (dominant): Very similar dark blue
        val cand1 = (0xFF000000 or (25 shl 16) or (25 shl 8) or 38).toInt()
        // Candidate 2 (less common): Bright yellow
        val cand2 = (0xFF000000 or (255 shl 16) or (220 shl 8) or 0).toInt()

        fun colorDistance(c1: Int, c2: Int): Double {
            val r1 = (c1 shr 16) and 0xFF
            val g1 = (c1 shr 8) and 0xFF
            val b1 = c1 and 0xFF
            val r2 = (c2 shr 16) and 0xFF
            val g2 = (c2 shr 8) and 0xFF
            val b2 = c2 and 0xFF
            val dr = r1 - r2
            val dg = g1 - g2
            val db = b1 - b2
            return Math.sqrt((dr * dr + dg * dg + db * db).toDouble())
        }

        fun relativeLuminance(c: Int): Double {
            val r = ((c shr 16) and 0xFF) / 255.0
            val g = ((c shr 8) and 0xFF) / 255.0
            val b = (c and 0xFF) / 255.0
            val rr = if (r <= 0.03928) r / 12.92 else Math.pow((r + 0.055) / 1.055, 2.4)
            val gg = if (g <= 0.03928) g / 12.92 else Math.pow((g + 0.055) / 1.055, 2.4)
            val bb = if (b <= 0.03928) b / 12.92 else Math.pow((b + 0.055) / 1.055, 2.4)
            return 0.2126 * rr + 0.7152 * gg + 0.0722 * bb
        }

        fun calculateContrast(c1: Int, c2: Int): Double {
            val l1 = relativeLuminance(c1)
            val l2 = relativeLuminance(c2)
            val maxL = Math.max(l1, l2)
            val minL = Math.min(l1, l2)
            return (maxL + 0.05) / (minL + 0.05)
        }

        val candidates = listOf(cand1, cand2)

        val selected = candidates.firstOrNull { cand ->
            val contrast = calculateContrast(cand, bgInt)
            val dist = colorDistance(cand, bgInt)
            contrast >= 4.5 && dist >= 80.0
        }

        assertNotNull(selected)
        assertEquals(cand2, selected)
    }
}
