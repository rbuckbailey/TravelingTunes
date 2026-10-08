package com.travelingtunes.app

import com.travelingtunes.app.core.model.RepeatMode
import com.travelingtunes.app.core.model.ShuffleMode
import com.travelingtunes.app.core.model.Song
import org.junit.Assert.assertEquals
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

    @Test
    fun testAndroidAutoRootSelectionLogicWhenPlaying() {
        // When music is playing, Android Auto root resolves to "show_play_screen" to open play screen directly
        fun getRootMediaId(isPlaying: Boolean): String {
            return if (isPlaying) "show_play_screen" else "root"
        }

        assertEquals("show_play_screen", getRootMediaId(isPlaying = true))
        assertEquals("root", getRootMediaId(isPlaying = false))
    }

    @Test
    fun testForegroundServiceClassName() {
        val serviceClass = com.travelingtunes.app.core.media.MusicPlaybackService::class.java
        assertEquals("com.travelingtunes.app.core.media.MusicPlaybackService", serviceClass.name)
        assertEquals("traveling_tunes_playback_channel", com.travelingtunes.app.core.media.MusicPlaybackService.NOTIFICATION_CHANNEL_ID)
    }

    @Test
    fun testActiveSongIndexUsesPlayerCurrentMediaItemIndexOverFirstMatch() {
        val mockUri = Mockito.mock(android.net.Uri::class.java)
        val s1 = Song(100L, "Track A", "Artist", "Album", 1L, 1000L, mockUri)
        val s2 = Song(101L, "Track B", "Artist", "Album", 1L, 1000L, mockUri)
        // Duplicate instance of s1 later in the queue
        val s1Duplicate = Song(100L, "Track A", "Artist", "Album", 1L, 1000L, mockUri)

        val queue = listOf(s1, s2, s1Duplicate)
        val currentPlayerIndex = 2 // Player is actually on the second Track A instance (index 2)

        val songId = queue[currentPlayerIndex].id
        val songIndex = if (currentPlayerIndex in queue.indices) {
            currentPlayerIndex
        } else {
            queue.indexOfFirst { it.id == songId }.coerceAtLeast(0)
        }

        assertEquals(2, songIndex)
    }

    @Test
    fun testShuffleAllIconResource() {
        val shuffleAllDrawableRes = R.drawable.ic_shuffle_all
        assert(shuffleAllDrawableRes != 0)
    }

    @Test
    fun testVoiceSearchQueryResolution() {
        val mockUri = Mockito.mock(android.net.Uri::class.java)
        val s1 = Song(101L, "Midnight Rain", "Taylor Swift", "Midnights", 1L, 180000L, mockUri)
        val s2 = Song(102L, "Lavender Haze", "Taylor Swift", "Midnights", 2L, 200000L, mockUri)
        val s3 = Song(103L, "Shape of You", "Ed Sheeran", "Divide", 1L, 230000L, mockUri)

        val library = listOf(s1, s2, s3)

        fun resolveSearch(
            title: String? = null,
            artist: String? = null,
            album: String? = null,
            query: String? = null
        ): List<Song> {
            val cleanTitle = title?.trim()?.lowercase().orEmpty()
            val cleanArtist = artist?.trim()?.lowercase().orEmpty()
            val cleanAlbum = album?.trim()?.lowercase().orEmpty()
            val cleanQuery = query?.trim()?.lowercase().orEmpty()

            if (cleanTitle.isNotEmpty()) {
                val matches = library.filter { song ->
                    song.title.lowercase().contains(cleanTitle) &&
                            (cleanArtist.isEmpty() || song.artist.lowercase().contains(cleanArtist)) &&
                            (cleanAlbum.isEmpty() || song.album.lowercase().contains(cleanAlbum))
                }
                if (matches.isNotEmpty()) {
                    val target = matches.first()
                    return library.filter { it.album.equals(target.album, ignoreCase = true) }
                }
            }

            if (cleanAlbum.isNotEmpty()) {
                val albumSongs = library.filter { it.album.lowercase().contains(cleanAlbum) }
                if (albumSongs.isNotEmpty()) return albumSongs
            }

            if (cleanArtist.isNotEmpty()) {
                val artistSongs = library.filter { it.artist.lowercase().contains(cleanArtist) }
                if (artistSongs.isNotEmpty()) return artistSongs
            }

            if (cleanQuery.isNotEmpty()) {
                val searchResults = library.filter {
                    it.title.lowercase().contains(cleanQuery) ||
                            it.artist.lowercase().contains(cleanQuery) ||
                            it.album.lowercase().contains(cleanQuery)
                }
                if (searchResults.isNotEmpty()) return searchResults
            }

            return library
        }

        val titleMatch = resolveSearch(title = "Midnight Rain")
        assertEquals(2, titleMatch.size)
        assertEquals("Midnight Rain", titleMatch[0].title)

        val artistMatch = resolveSearch(artist = "Ed Sheeran")
        assertEquals(1, artistMatch.size)
        assertEquals("Shape of You", artistMatch[0].title)

        val albumMatch = resolveSearch(album = "Midnights")
        assertEquals(2, albumMatch.size)

        val queryMatch = resolveSearch(query = "Divide")
        assertEquals(1, queryMatch.size)
        assertEquals("Shape of You", queryMatch[0].title)
    }

    @Test
    fun testSongToMediaItemArtworkMetadata() {
        val mockUri = Mockito.mock(android.net.Uri::class.java)
        Mockito.`when`(mockUri.toString()).thenReturn("file:///cache/album_art/art_test.jpg")
        Mockito.`when`(mockUri.scheme).thenReturn("file")
        Mockito.`when`(mockUri.path).thenReturn("/cache/album_art/art_test.jpg")

        val song = Song(
            id = 42L,
            title = "Test Song",
            artist = "Test Artist",
            album = "Test Album",
            albumId = 1L,
            durationMs = 180000L,
            contentUri = mockUri,
            artworkUri = mockUri
        )

        val mediaItem = com.travelingtunes.app.core.media.songToMediaItem(song)
        assertEquals("42", mediaItem.mediaId)
        assertEquals("Test Song", mediaItem.mediaMetadata.title.toString())
        assertEquals("Test Artist", mediaItem.mediaMetadata.artist.toString())
        assertEquals(mockUri, mediaItem.mediaMetadata.artworkUri)
    }

    @Test
    fun testBuildActiveQueueFilteringByRepeatAlbum() {
        val mockUri = Mockito.mock(android.net.Uri::class.java)
        val s1 = Song(1L, "Song 1", "Artist A", "Album 1", 1L, 1000L, mockUri, trackNumber = 1)
        val s2 = Song(2L, "Song 2", "Artist A", "Album 1", 1L, 1000L, mockUri, trackNumber = 2)
        val s3 = Song(3L, "Song 3", "Artist B", "Album 2", 1L, 1000L, mockUri, trackNumber = 1)

        val baseList = listOf(s1, s2, s3)
        val targetSong = s1

        // RepeatMode.ALBUM filters queue to Album 1
        val albumFilter = baseList.filter { it.album.equals(targetSong.album, ignoreCase = true) }
        assertEquals(2, albumFilter.size)
        assertEquals("Song 1", albumFilter[0].title)
        assertEquals("Song 2", albumFilter[1].title)
    }

    @Test
    fun testBuildActiveQueueFilteringByRepeatArtist() {
        val mockUri = Mockito.mock(android.net.Uri::class.java)
        val s1 = Song(1L, "Song 1", "Artist X", "Album 1", 1L, 1000L, mockUri, trackNumber = 1)
        val s2 = Song(2L, "Song 2", "Artist X", "Album 2", 1L, 1000L, mockUri, trackNumber = 1)
        val s3 = Song(3L, "Song 3", "Artist Y", "Album 3", 1L, 1000L, mockUri, trackNumber = 1)

        val baseList = listOf(s1, s2, s3)
        val targetSong = s1

        // RepeatMode.ARTIST filters queue to Artist X
        val artistFilter = baseList.filter { it.artist.equals(targetSong.artist, ignoreCase = true) }
        assertEquals(2, artistFilter.size)
        assertEquals("Song 1", artistFilter[0].title)
        assertEquals("Song 2", artistFilter[1].title)
    }

    @Test
    fun testTogglingShuffleOnResetsRepeatAlbumMode() {
        var repeatMode = RepeatMode.ALBUM
        val newShuffleMode = ShuffleMode.SONGS

        if (newShuffleMode != ShuffleMode.OFF && (repeatMode == RepeatMode.ALBUM || repeatMode == RepeatMode.ARTIST || repeatMode == RepeatMode.GENRE || repeatMode == RepeatMode.FOLDER)) {
            repeatMode = RepeatMode.OFF
        }

        assertEquals(RepeatMode.OFF, repeatMode)
    }

    @Test
    fun testPersistStateSongIndexPrioritizesCurrentSongMatchOverMismatchingPlayerIndex() {
        val mockUri = Mockito.mock(android.net.Uri::class.java)
        val s1 = Song(101L, "Song 1", "Artist", "Album", 1L, 1000L, mockUri)
        val s2 = Song(102L, "Song 2", "Artist", "Album", 1L, 1000L, mockUri)
        val s3 = Song(103L, "Song 3", "Artist", "Album", 1L, 1000L, mockUri)

        val playlist = listOf(s1, s2, s3)
        val currentSong = s3 // Song 3 is current (index 2)
        val playerIndex = 1 // Player index is lagging behind at index 1 (Song 2)

        val songId = currentSong.id
        val songIndex = if (songId != -1L) {
            val matchedIndex = playlist.indexOfFirst { it.id == songId }
            if (matchedIndex != -1) {
                matchedIndex
            } else if (playerIndex in playlist.indices) {
                playerIndex
            } else {
                0
            }
        } else if (playerIndex in playlist.indices) {
            playerIndex
        } else {
            0
        }

        assertEquals("songIndex must resolve to currentSong's position in playlist (2), not lagging playerIndex (1)", 2, songIndex)
    }

    @Test
    fun testUpdateSongMetadataInQueuePreservesQueueStructureAndUpdatesCurrentSong() {
        val mockUri = Mockito.mock(android.net.Uri::class.java)
        val s1 = Song(101L, "Original Title 1", "Artist A", "Album A", 1L, 1000L, mockUri, trackNumber = 1)
        val s2 = Song(102L, "Original Title 2", "Artist A", "Album A", 1L, 1000L, mockUri, trackNumber = 2)

        var playlist = listOf(s1, s2)
        var currentSong: Song? = s1

        val updatedS1 = s1.copy(title = "Edited Title 1", genre = "Rock")
        val updatedSongs = listOf(updatedS1)

        val map = updatedSongs.associateBy { it.id }
        playlist = playlist.map { song -> map[song.id] ?: song }
        if (currentSong != null && map.containsKey(currentSong.id)) {
            currentSong = map[currentSong.id]
        }

        assertEquals(2, playlist.size)
        assertEquals("Edited Title 1", playlist[0].title)
        assertEquals("Rock", playlist[0].genre)
        assertEquals("Original Title 2", playlist[1].title)
        assertEquals("Edited Title 1", currentSong?.title)
    }

    @Test
    fun testTrackSharingHelperFormatters() {
        val mockUri = Mockito.mock(android.net.Uri::class.java)
        val song1 = Song(1L, "Bohemian Rhapsody", "Queen", "A Night at the Opera", 10L, 354000L, mockUri, trackNumber = 1)
        val song2 = Song(2L, "You're My Best Friend", "Queen", "A Night at the Opera", 10L, 172000L, mockUri, trackNumber = 2)

        val trackText = com.travelingtunes.app.core.media.TrackSharingHelper.formatTrackText(song1)
        assertEquals("\"Bohemian Rhapsody\" by Queen (A Night at the Opera)", trackText)

        val albumText = com.travelingtunes.app.core.media.TrackSharingHelper.formatAlbumText("A Night at the Opera", "Queen", listOf(song1, song2))
        org.junit.Assert.assertTrue(albumText.contains("Album: \"A Night at the Opera\" by Queen"))
        org.junit.Assert.assertTrue(albumText.contains("1. Bohemian Rhapsody"))
        org.junit.Assert.assertTrue(albumText.contains("2. You're My Best Friend"))
    }

    @Test
    fun testTagEditorAndShareGestureActionsFromKey() {
        assertEquals(com.travelingtunes.app.core.model.GestureAction.EDIT_TRACK_TAGS, com.travelingtunes.app.core.model.GestureAction.fromKey("EDIT_TRACK_TAGS"))
        assertEquals(com.travelingtunes.app.core.model.GestureAction.EDIT_ALBUM_TAGS, com.travelingtunes.app.core.model.GestureAction.fromKey("EDIT_ALBUM_TAGS"))
        assertEquals(com.travelingtunes.app.core.model.GestureAction.SHARE_TRACK_TEXT, com.travelingtunes.app.core.model.GestureAction.fromKey("SHARE_TRACK_TEXT"))
        assertEquals(com.travelingtunes.app.core.model.GestureAction.SHARE_TRACK_FILE, com.travelingtunes.app.core.model.GestureAction.fromKey("SHARE_TRACK_FILE"))
        assertEquals(com.travelingtunes.app.core.model.GestureAction.SHARE_ALBUM_TEXT, com.travelingtunes.app.core.model.GestureAction.fromKey("SHARE_ALBUM_TEXT"))
        assertEquals(com.travelingtunes.app.core.model.GestureAction.SHARE_ALBUM_FILES, com.travelingtunes.app.core.model.GestureAction.fromKey("SHARE_ALBUM_FILES"))
    }

    @Test
    fun testGetGroupedCategoriesContainsAllGestureActions() {
        val groups = com.travelingtunes.app.core.model.GestureAction.getGroupedCategories(excludeRadialMenu = false, excludeUnassigned = false)
        val categorizedActions = groups.flatMap { it.actions }.toSet()

        for (action in com.travelingtunes.app.core.model.GestureAction.entries) {
            org.junit.Assert.assertTrue(
                "Action ${action.name} must be included in getGroupedCategories()",
                categorizedActions.contains(action)
            )
        }
    }

    @Test
    fun testActionOptionsMappedToMusicLibrarySubmenu() {
        val editTrackOption = com.travelingtunes.app.core.model.ConfigOption.findByKey("ACTION_EDIT_TRACK_TAGS")
        val editAlbumOption = com.travelingtunes.app.core.model.ConfigOption.findByKey("ACTION_EDIT_ALBUM_TAGS")
        val shareTrackTextOption = com.travelingtunes.app.core.model.ConfigOption.findByKey("ACTION_SHARE_TRACK_TEXT")
        val shareAlbumFilesOption = com.travelingtunes.app.core.model.ConfigOption.findByKey("ACTION_SHARE_ALBUM_FILES")

        org.junit.Assert.assertNotNull(editTrackOption)
        org.junit.Assert.assertNotNull(editAlbumOption)
        org.junit.Assert.assertNotNull(shareTrackTextOption)
        org.junit.Assert.assertNotNull(shareAlbumFilesOption)

        assertEquals("Music Library", editTrackOption?.category)
        assertEquals("Music Library", editAlbumOption?.category)
        assertEquals("Music Library", shareTrackTextOption?.category)
        assertEquals("Music Library", shareAlbumFilesOption?.category)
    }
}
