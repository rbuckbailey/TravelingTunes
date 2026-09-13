package com.travelingtunes.app

import android.net.Uri
import com.travelingtunes.app.core.media.DuplicateTrackFinder
import com.travelingtunes.app.core.media.DuplicateTrackInfo
import com.travelingtunes.app.core.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class DuplicateTrackFinderTest {

    @Test
    fun testCleanFileName() {
        val name1 = DuplicateTrackFinder.cleanFileName("01 - Stairway To Heaven.mp3")
        val name2 = DuplicateTrackFinder.cleanFileName("Stairway To Heaven (1).mp3")
        val name3 = DuplicateTrackFinder.cleanFileName("stairway_to_heaven.flac")

        assertEquals("stairwaytoheaven", name1)
        assertEquals("stairwaytoheaven", name2)
        assertEquals("stairwaytoheaven", name3)
    }

    @Test
    fun testCleanMetadata() {
        assertEquals("bohemianrhapsody", DuplicateTrackFinder.cleanMetadata("Bohemian Rhapsody"))
        assertEquals("", DuplicateTrackFinder.cleanMetadata("<Unknown>"))
        assertEquals("", DuplicateTrackFinder.cleanMetadata("Unknown Title"))
    }

    @Test
    fun testCalculateMatchExactDuplicate() {
        val dummyUri = mock(Uri::class.java)

        val songA = Song(
            id = 101L,
            title = "Stairway to Heaven",
            artist = "Led Zeppelin",
            album = "Led Zeppelin IV",
            albumId = 1L,
            durationMs = 482000L,
            contentUri = dummyUri,
            folderPath = "/sdcard/Music/Led Zeppelin",
            fileName = "01 - Stairway to Heaven.mp3"
        )

        val songB = Song(
            id = 102L,
            title = "Stairway to Heaven",
            artist = "Led Zeppelin",
            album = "Led Zeppelin IV",
            albumId = 1L,
            durationMs = 482000L,
            contentUri = dummyUri,
            folderPath = "/sdcard/Downloads",
            fileName = "Stairway to Heaven (1).mp3"
        )

        val infoA = DuplicateTrackInfo(songA, 12500000L, "/sdcard/Music/Led Zeppelin")
        val infoB = DuplicateTrackInfo(songB, 12500000L, "/sdcard/Downloads")

        val match = DuplicateTrackFinder.calculateMatch(infoA, infoB)
        assertNotNull(match)
        assertTrue(match!!.likelihoodPercentage >= 95)
        assertTrue(match.matchReasons.contains("Identical file size (11.92 MB)"))
        assertTrue(match.matchReasons.contains("Identical file name"))
        assertTrue(match.matchReasons.contains("Identical title"))
    }

    @Test
    fun testCalculateMatchPartialDuplicate() {
        val dummyUri = mock(Uri::class.java)

        val songA = Song(
            id = 101L,
            title = "Hotel California",
            artist = "Eagles",
            album = "Hotel California",
            albumId = 2L,
            durationMs = 390000L,
            contentUri = dummyUri,
            folderPath = "/sdcard/Music/Eagles",
            fileName = "01 Hotel California.mp3"
        )

        val songB = Song(
            id = 102L,
            title = "Hotel California (Live)",
            artist = "Eagles",
            album = "Hell Freezes Over",
            albumId = 3L,
            durationMs = 420000L,
            contentUri = dummyUri,
            folderPath = "/sdcard/Music/Live",
            fileName = "Hotel California Live.mp3"
        )

        val infoA = DuplicateTrackInfo(songA, 9000000L, "/sdcard/Music/Eagles")
        val infoB = DuplicateTrackInfo(songB, 11000000L, "/sdcard/Music/Live")

        val match = DuplicateTrackFinder.calculateMatch(infoA, infoB)
        assertNotNull(match)
        assertTrue(match!!.likelihoodPercentage in 40..85)
    }

    @Test
    fun testFormatFileSizeAndDuration() {
        assertEquals("1.00 MB", DuplicateTrackFinder.formatFileSize(1024L * 1024L))
        assertEquals("500.0 KB", DuplicateTrackFinder.formatFileSize(500L * 1024L))
        assertEquals("3:45", DuplicateTrackFinder.formatDuration(225000L))
    }
}
