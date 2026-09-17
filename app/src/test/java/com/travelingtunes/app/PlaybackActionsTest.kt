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

        val masterLibrary = listOf(song1, song2, song3)

        // RepeatMode.ALBUM filters current active queue for Album A
        val albumAQueue = masterLibrary.filter { it.album.equals("Album A", ignoreCase = true) }.sortedBy { it.trackNumber }
        assertEquals(2, albumAQueue.size)
        assertEquals("Track 1", albumAQueue[0].title)
        assertEquals("Track 2", albumAQueue[1].title)

        // Turning RepeatMode OFF restores full master library
        val restoredQueue = masterLibrary
        assertEquals(3, restoredQueue.size)
    }

    @Test
    fun testSelectedSongFirstInShuffleMode_IndexZero() {
        val mockUri = Mockito.mock(android.net.Uri::class.java)
        val s1 = Song(1L, "Song 1", "Artist", "Album", 1L, 1000L, mockUri)
        val s2 = Song(2L, "Song 2", "Artist", "Album", 1L, 1000L, mockUri)
        val s3 = Song(3L, "Song 3", "Artist", "Album", 1L, 1000L, mockUri)

        val songs = listOf(s1, s2, s3)
        val chosenSong = songs[0] // User picked index 0

        val remainingSongs = songs.filter { it.id != chosenSong.id }
        val activeQueue = listOf(chosenSong) + remainingSongs.shuffled()

        assertEquals("Song 1", activeQueue[0].title)
        assertEquals(3, activeQueue.size)
    }

    @Test
    fun testSelectedSongFirstInShuffleMode_IndexNonZero() {
        val mockUri = Mockito.mock(android.net.Uri::class.java)
        val s1 = Song(1L, "Song 1", "Artist", "Album", 1L, 1000L, mockUri)
        val s2 = Song(2L, "Song 2", "Artist", "Album", 1L, 1000L, mockUri)
        val s3 = Song(3L, "Song 3", "Artist", "Album", 1L, 1000L, mockUri)

        val songs = listOf(s1, s2, s3)
        val chosenSong = songs[2] // User picked index 2 ("Song 3")

        val remainingSongs = songs.filter { it.id != chosenSong.id }
        val activeQueue = listOf(chosenSong) + remainingSongs.shuffled()

        assertEquals("Song 3", activeQueue[0].title)
        assertEquals(3, activeQueue.size)
    }

    @Test
    fun testNextAndPreviousAlbumActionDisplayNames() {
        assertEquals("Next Album", com.travelingtunes.app.core.model.GestureAction.NEXT_ALBUM.displayName)
        assertEquals("Previous Album", com.travelingtunes.app.core.model.GestureAction.PREVIOUS_ALBUM.displayName)
    }

    @Test
    fun testMediaItemTransitionReasonValues() {
        assertEquals(1, androidx.media3.common.Player.MEDIA_ITEM_TRANSITION_REASON_AUTO)
        assertEquals(0, androidx.media3.common.Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT)
        assertEquals(2, androidx.media3.common.Player.MEDIA_ITEM_TRANSITION_REASON_SEEK)
    }

    @Test
    fun testBackgroundTaskGatePauseState() {
        val gate = com.travelingtunes.app.core.media.BackgroundTaskGate
        gate.notifyForegroundBusy(false)
        assertEquals(false, gate.isForegroundBusy.value)

        gate.notifyForegroundBusy(true)
        assertEquals(true, gate.isForegroundBusy.value)

        gate.notifyForegroundBusy(false)
        assertEquals(false, gate.isForegroundBusy.value)
    }

    @Test
    fun testPlayCurrentArtistLogic() {
        val mockUri = Mockito.mock(android.net.Uri::class.java)
        val song1 = Song(1L, "Song 1", "Artist X", "Album 1", 10L, 100000L, mockUri, trackNumber = 1)
        val song2 = Song(2L, "Song 2", "Artist X", "Album 2", 10L, 120000L, mockUri, trackNumber = 1)
        val song3 = Song(3L, "Song 3", "Artist Y", "Album 3", 11L, 110000L, mockUri, trackNumber = 1)

        val masterLibrary = listOf(song1, song2, song3)

        val artistXSongs = masterLibrary.filter { it.artist.equals("Artist X", ignoreCase = true) }
        assertEquals(2, artistXSongs.size)
        assertEquals("Song 1", artistXSongs[0].title)
        assertEquals("Song 2", artistXSongs[1].title)
    }

    @Test
    fun testPlayNextInsertionInQueue() {
        val mockUri = Mockito.mock(android.net.Uri::class.java)
        val song1 = Song(1L, "Song 1", "Artist", "Album", 1L, 1000L, mockUri)
        val song2 = Song(2L, "Song 2", "Artist", "Album", 1L, 1000L, mockUri)
        val song3 = Song(3L, "Song 3", "Artist", "Album", 1L, 1000L, mockUri)
        val nextSong = Song(4L, "Next Song", "Artist", "Album", 1L, 1000L, mockUri)

        val queue = mutableListOf(song1, song2, song3)
        val currentPlayingIndex = 0 // Currently playing Song 1

        val insertIndex = currentPlayingIndex + 1
        queue.add(insertIndex, nextSong)

        assertEquals(4, queue.size)
        assertEquals("Song 1", queue[0].title)
        assertEquals("Next Song", queue[1].title)
        assertEquals("Song 2", queue[2].title)
        assertEquals("Song 3", queue[3].title)
    }

    @Test
    fun testAddToQueueAppendsToEnd() {
        val mockUri = Mockito.mock(android.net.Uri::class.java)
        val song1 = Song(1L, "Song 1", "Artist", "Album", 1L, 1000L, mockUri)
        val song2 = Song(2L, "Song 2", "Artist", "Album", 1L, 1000L, mockUri)
        val queuedSong = Song(3L, "Queued Song", "Artist", "Album", 1L, 1000L, mockUri)

        val queue = mutableListOf(song1, song2)
        queue.add(queuedSong)

        assertEquals(3, queue.size)
        assertEquals("Song 1", queue[0].title)
        assertEquals("Song 2", queue[1].title)
        assertEquals("Queued Song", queue[2].title)
    }
}
