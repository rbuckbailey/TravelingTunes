package com.travelingtunes.app

import com.travelingtunes.app.core.model.RepeatMode
import com.travelingtunes.app.core.model.ShuffleMode
import com.travelingtunes.app.core.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito

class PlaybackActionsTest {

    @Test
    fun testRepeatAndShuffleModeEnums() {
        assertEquals("Repeat Album", RepeatMode.ALBUM.displayName)
        assertEquals("Shuffle Songs", ShuffleMode.SONGS.displayName)
        assertEquals("Shuffle Off", ShuffleMode.OFF.displayName)
    }

    @Test
    fun testShuffleAllSongsActionDisplayName() {
        assertEquals("Shuffle All Songs", com.travelingtunes.app.core.model.GestureAction.SHUFFLE_ALL_SONGS.displayName)
    }

    @Test
    fun testPlayCurrentAlbumLogic() {
        val mockUri = Mockito.mock(android.net.Uri::class.java)
        val song1 = Song(1L, "Track 1", "Artist A", "Album A", 10L, 100000L, mockUri, trackNumber = 1)
        val song2 = Song(2L, "Track 2", "Artist A", "Album A", 10L, 120000L, mockUri, trackNumber = 2)
        val song3 = Song(3L, "Track 3", "Artist B", "Album B", 11L, 110000L, mockUri, trackNumber = 1)

        val albumSongs = listOf(song1, song2, song3)
            .filter { it.album.equals("Album A", ignoreCase = true) }
            .sortedBy { it.trackNumber }

        assertEquals(2, albumSongs.size)
        assertEquals("Track 1", albumSongs[0].title)
        assertEquals("Track 2", albumSongs[1].title)
    }
}
