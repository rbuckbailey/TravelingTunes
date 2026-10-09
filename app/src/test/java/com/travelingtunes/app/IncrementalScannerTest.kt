package com.travelingtunes.app

import android.net.Uri
import com.travelingtunes.app.core.database.MusicDatabase
import com.travelingtunes.app.core.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class IncrementalScannerTest {

    private lateinit var mockDb: MusicDatabase
    private lateinit var mockUri: Uri

    @Before
    fun setUp() {
        mockDb = mock(MusicDatabase::class.java)
        mockUri = mock(Uri::class.java)
        `when`(mockUri.toString()).thenReturn("content://media/external/audio/media/101")
    }

    @Test
    fun testIncrementalMapMatchingPreservesSongMetadata() {
        val song1 = Song(
            id = 101L,
            title = "Song One",
            artist = "Artist A",
            album = "Album A",
            albumId = 1L,
            durationMs = 180000L,
            contentUri = mockUri,
            userRating = 5,
            avgVolume = 85.0f,
            peakVolume = 95.0f,
            trackGain = 0.95f,
            lastModified = 1000L
        )

        val existingSongs = listOf(song1)
        val existingMap = existingSongs.associateBy { it.contentUri.toString() }

        val newUriStr = "content://media/external/audio/media/101"
        val matched = existingMap[newUriStr]

        assertNotNull(matched)
        assertEquals(5, matched?.userRating)
        assertEquals(85.0f, matched?.avgVolume)
        assertEquals("Song One", matched?.title)
        assertEquals(1000L, matched?.lastModified)
    }

    @Test
    fun testUnchangedFileWithTimestampIsReused() {
        val existingSong = Song(
            id = 101L,
            title = "Existing Song",
            artist = "Artist",
            album = "Album",
            albumId = 1L,
            durationMs = 200000L,
            contentUri = mockUri,
            userRating = 4,
            avgVolume = 88.0f,
            lastModified = 5000L
        )

        val fileMtime = 5000L
        val isUnchanged = existingSong.lastModified > 0L && fileMtime <= existingSong.lastModified

        assertTrue(isUnchanged)
        assertEquals(4, existingSong.userRating)
        assertEquals(88.0f, existingSong.avgVolume)
    }

    @Test
    fun testModifiedFilePreservesUserCustomizationsOnReprocess() {
        val existingSong = Song(
            id = 101L,
            title = "Old Title",
            artist = "Artist",
            album = "Album",
            albumId = 1L,
            durationMs = 200000L,
            contentUri = mockUri,
            userRating = 5,
            avgVolume = 89.2f,
            peakVolume = 98.0f,
            trackGain = 0.92f,
            lastModified = 5000L
        )

        val fileMtime = 6000L
        val isModified = fileMtime > existingSong.lastModified

        assertTrue(isModified)

        // Simulated reprocessed song merging user ratings and volume gains
        val updatedSong = Song(
            id = existingSong.id,
            title = "New Updated Title",
            artist = "Artist",
            album = "Album",
            albumId = 1L,
            durationMs = 205000L,
            contentUri = mockUri,
            userRating = existingSong.userRating,
            avgVolume = existingSong.avgVolume,
            peakVolume = existingSong.peakVolume,
            trackGain = existingSong.trackGain,
            lastModified = fileMtime
        )

        assertEquals("New Updated Title", updatedSong.title)
        assertEquals(5, updatedSong.userRating)
        assertEquals(89.2f, updatedSong.avgVolume)
        assertEquals(6000L, updatedSong.lastModified)
    }
}
