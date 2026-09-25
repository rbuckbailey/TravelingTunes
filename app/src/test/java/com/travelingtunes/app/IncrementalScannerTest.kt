package com.travelingtunes.app

import android.net.Uri
import com.travelingtunes.app.core.database.MusicDatabase
import com.travelingtunes.app.core.model.Song
import org.junit.Assert.assertEquals
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
            trackGain = 0.95f
        )

        val existingSongs = listOf(song1)
        val existingMap = existingSongs.associateBy { it.contentUri.toString() }

        val newUriStr = "content://media/external/audio/media/101"
        val matched = existingMap[newUriStr]

        assertTrue(matched != null)
        assertEquals(5, matched?.userRating)
        assertEquals(85.0f, matched?.avgVolume)
        assertEquals("Song One", matched?.title)
    }
}
