package com.travelingtunes.app

import android.net.Uri
import com.travelingtunes.app.core.model.Song
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito

class AlbumOrderingAndShuffleTest {

    private val mockUri = Mockito.mock(Uri::class.java)

    private fun compareAlbumSongs(s1: Song, s2: Song): Int {
        val d1 = if (s1.discNumber > 0) s1.discNumber else 1
        val d2 = if (s2.discNumber > 0) s2.discNumber else 1
        if (d1 != d2) return d1.compareTo(d2)

        val t1 = if (s1.trackNumber > 0) s1.trackNumber else Int.MAX_VALUE
        val t2 = if (s2.trackNumber > 0) s2.trackNumber else Int.MAX_VALUE
        if (t1 != t2) return t1.compareTo(t2)

        return String.CASE_INSENSITIVE_ORDER.compare(s1.title, s2.title)
    }

    private fun getAlbumKey(song: Song): String {
        return if (song.albumId > 0) {
            "id_${song.albumId}"
        } else {
            "${song.artist.trim().lowercase()}_${song.album.trim().lowercase()}"
        }
    }

    @Test
    fun testAlbumOrdering_DiscFirstThenTrack() {
        val s1 = Song(1L, "Track B", "Artist", "Album", 1L, 1000L, mockUri, discNumber = 2, trackNumber = 1)
        val s2 = Song(2L, "Track A", "Artist", "Album", 1L, 1000L, mockUri, discNumber = 1, trackNumber = 2)
        val s3 = Song(3L, "Track C", "Artist", "Album", 1L, 1000L, mockUri, discNumber = 1, trackNumber = 1)

        val sorted = listOf(s1, s2, s3).sortedWith(::compareAlbumSongs)

        assertEquals("Track C", sorted[0].title) // Disc 1, Track 1
        assertEquals("Track A", sorted[1].title) // Disc 1, Track 2
        assertEquals("Track B", sorted[2].title) // Disc 2, Track 1
    }

    @Test
    fun testAlbumOrdering_DiscZeroAndDiscOneTreatedSame() {
        val s1 = Song(1L, "Track B", "Artist", "Album", 1L, 1000L, mockUri, discNumber = 0, trackNumber = 2)
        val s2 = Song(2L, "Track A", "Artist", "Album", 1L, 1000L, mockUri, discNumber = 1, trackNumber = 1)

        val sorted = listOf(s1, s2).sortedWith(::compareAlbumSongs)

        assertEquals("Track A", sorted[0].title) // Track 1
        assertEquals("Track B", sorted[1].title) // Track 2
    }

    @Test
    fun testAlbumOrdering_NoDiscData_TrackOnly() {
        val s1 = Song(1L, "Song 3", "Artist", "Album", 1L, 1000L, mockUri, discNumber = 0, trackNumber = 3)
        val s2 = Song(2L, "Song 1", "Artist", "Album", 1L, 1000L, mockUri, discNumber = 0, trackNumber = 1)
        val s3 = Song(3L, "Song 2", "Artist", "Album", 1L, 1000L, mockUri, discNumber = 0, trackNumber = 2)

        val sorted = listOf(s1, s2, s3).sortedWith(::compareAlbumSongs)

        assertEquals("Song 1", sorted[0].title)
        assertEquals("Song 2", sorted[1].title)
        assertEquals("Song 3", sorted[2].title)
    }

    @Test
    fun testAlbumOrdering_NoDiscNoTrack_AlphaFallback() {
        val s1 = Song(1L, "Charlie", "Artist", "Album", 1L, 1000L, mockUri, discNumber = 0, trackNumber = 0)
        val s2 = Song(2L, "Alpha", "Artist", "Album", 1L, 1000L, mockUri, discNumber = 0, trackNumber = 0)
        val s3 = Song(3L, "Bravo", "Artist", "Album", 1L, 1000L, mockUri, discNumber = 0, trackNumber = 0)

        val sorted = listOf(s1, s2, s3).sortedWith(::compareAlbumSongs)

        assertEquals("Alpha", sorted[0].title)
        assertEquals("Bravo", sorted[1].title)
        assertEquals("Charlie", sorted[2].title)
    }

    @Test
    fun testSameAlbumNameDifferentArtist_DifferentAlbumKeys() {
        val s1 = Song(1L, "Song 1", "Artist A", "Greatest Hits", 0L, 1000L, mockUri, trackNumber = 1)
        val s2 = Song(2L, "Song 1", "Artist B", "Greatest Hits", 0L, 1000L, mockUri, trackNumber = 1)

        org.junit.Assert.assertNotEquals(getAlbumKey(s1), getAlbumKey(s2))
    }

    @Test
    fun testShuffleAlbums_CurrentAlbumInTrackOrderThenRandomAlbumsInOrder() {
        val a1_1 = Song(1L, "A1", "Artist A", "Album A", 1L, 1000L, mockUri, discNumber = 1, trackNumber = 1)
        val a1_2 = Song(2L, "A2", "Artist A", "Album A", 1L, 1000L, mockUri, discNumber = 1, trackNumber = 2)
        val a1_3 = Song(3L, "A3", "Artist A", "Album A", 1L, 1000L, mockUri, discNumber = 1, trackNumber = 3)
        val b1_1 = Song(4L, "B1", "Artist B", "Album B", 2L, 1000L, mockUri, discNumber = 1, trackNumber = 1)
        val b1_2 = Song(5L, "B2", "Artist B", "Album B", 2L, 1000L, mockUri, discNumber = 1, trackNumber = 2)

        val allSongs = listOf(a1_1, a1_2, a1_3, b1_1, b1_2)

        val chosenSong = a1_2 // Selected Track 2 of Album A
        val chosenAlbumKey = getAlbumKey(chosenSong)

        val albumsMap = allSongs.groupBy { getAlbumKey(it) }
        val activeAlbumSongs = (albumsMap[chosenAlbumKey] ?: emptyList()).sortedWith(::compareAlbumSongs)
        val chosenIdx = activeAlbumSongs.indexOfFirst { it.id == chosenSong.id }

        val otherAlbumKeys = (albumsMap.keys - chosenAlbumKey).shuffled()

        val activeQueue = mutableListOf<Song>()
        activeQueue.addAll(activeAlbumSongs)

        for (albumKey in otherAlbumKeys) {
            val albumSongs = albumsMap[albumKey] ?: emptyList()
            activeQueue.addAll(albumSongs.sortedWith(::compareAlbumSongs))
        }

        assertEquals(5, activeQueue.size)
        // Full Album A must be in exact track order: A1, A2, A3
        assertEquals("A1", activeQueue[0].title)
        assertEquals("A2", activeQueue[1].title)
        assertEquals("A3", activeQueue[2].title)
        // Playback starts at index 1 (A2)
        assertEquals(1, chosenIdx)
        // Next album is Album B in track order: B1, B2
        assertEquals("B1", activeQueue[3].title)
        assertEquals("B2", activeQueue[4].title)
    }
}
