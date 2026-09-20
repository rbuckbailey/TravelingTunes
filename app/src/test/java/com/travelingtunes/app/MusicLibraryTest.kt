package com.travelingtunes.app

import android.net.Uri
import com.travelingtunes.app.core.database.AlbumInfo
import com.travelingtunes.app.core.database.LibraryStats
import com.travelingtunes.app.core.model.GestureAction
import com.travelingtunes.app.core.model.Song
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito

class MusicLibraryTest {

    @Test
    fun testSongModelCreation() {
        val mockUri = Mockito.mock(Uri::class.java)
        val song = Song(
            id = 101L,
            title = "Drive",
            artist = "Incubus",
            album = "Make Yourself",
            albumId = 501L,
            durationMs = 232000L,
            contentUri = mockUri,
            artworkUri = null,
            genre = "Alternative Rock",
            folderPath = "Rock/Incubus",
            fileName = "03 Drive.mp3",
            trackNumber = 3,
            year = 1999
        )

        assertEquals("Drive", song.title)
        assertEquals("Incubus", song.artist)
        assertEquals("Make Yourself", song.album)
        assertEquals("Alternative Rock", song.genre)
        assertEquals("Rock/Incubus", song.folderPath)
        assertEquals("03 Drive.mp3", song.fileName)
        assertEquals(3, song.trackNumber)
        assertEquals(1999, song.year)
    }

    @Test
    fun testSongPickerGestureResolution() {
        val actionFromKey = GestureAction.fromKey("SongPicker")
        assertEquals(GestureAction.SONG_PICKER, actionFromKey)
        assertEquals("Song Picker", GestureAction.SONG_PICKER.displayName)
    }

    @Test
    fun testLibraryStatsModel() {
        val stats = LibraryStats(
            totalSongs = 120,
            totalAlbums = 15,
            totalArtists = 10,
            totalGenres = 5
        )

        assertEquals(120, stats.totalSongs)
        assertEquals(15, stats.totalAlbums)
        assertEquals(10, stats.totalArtists)
        assertEquals(5, stats.totalGenres)
    }

    @Test
    fun testAlbumInfoModel() {
        val album = AlbumInfo(
            name = "Californication",
            artist = "Red Hot Chili Peppers",
            songCount = 15,
            artworkUri = null
        )

        assertEquals("Californication", album.name)
        assertEquals("Red Hot Chili Peppers", album.artist)
        assertEquals(15, album.songCount)
    }

    @Test
    fun testSavedPlaybackStateModel() {
        val state = com.travelingtunes.app.core.datastore.SavedPlaybackState(
            queueIds = listOf(101L, 102L, 103L),
            activeSongId = 102L,
            activeSongIndex = 1,
            positionMs = 45000L,
            isShuffle = true,
            isRepeat = false
        )

        assertEquals(3, state.queueIds.size)
        assertEquals(102L, state.activeSongId)
        assertEquals(1, state.activeSongIndex)
        assertEquals(45000L, state.positionMs)
        assertEquals(true, state.isShuffle)
        assertEquals(false, state.isRepeat)
    }

    @Test
    fun testSongFolderArtworkLinking() {
        val mockContentUri = Mockito.mock(Uri::class.java)
        val mockFolderArtUri = Mockito.mock(Uri::class.java)

        val song = Song(
            id = 201L,
            title = "Folder Art Track",
            artist = "Artist",
            album = "Folder Album",
            albumId = 601L,
            durationMs = 180000L,
            contentUri = mockContentUri,
            artworkUri = mockFolderArtUri,
            folderPath = "Music/FolderAlbum",
            fileName = "track.mp3"
        )

        assertEquals(mockFolderArtUri, song.artworkUri)
        assertEquals("Music/FolderAlbum", song.folderPath)
    }
}
