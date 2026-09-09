package com.travelingtunes.app

import com.travelingtunes.app.core.media.StreamingCatalogRepository
import com.travelingtunes.app.core.model.StreamingAccount
import com.travelingtunes.app.core.model.StreamingServiceId
import com.travelingtunes.app.core.model.StreamingTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamingServicesTest {

    @Test
    fun testStreamingServiceEnumLookups() {
        val spotify = StreamingServiceId.fromId("spotify")
        assertNotNull(spotify)
        assertEquals("Spotify", spotify?.displayName)
        assertEquals(0xFF1DB954, spotify?.brandColorHex)

        val ytMusic = StreamingServiceId.fromId("ytmusic")
        assertNotNull(ytMusic)
        assertEquals("YouTube Music", ytMusic?.displayName)

        val amazon = StreamingServiceId.fromId("amazon_music")
        assertNotNull(amazon)
        assertEquals("Amazon Prime Music", amazon?.displayName)

        val pandora = StreamingServiceId.fromId("pandora")
        assertNotNull(pandora)
        assertEquals("Pandora", pandora?.displayName)

        val soundCloud = StreamingServiceId.fromId("soundcloud")
        assertNotNull(soundCloud)
        assertEquals("SoundCloud", soundCloud?.displayName)

        val deezer = StreamingServiceId.fromId("deezer")
        assertNotNull(deezer)
        assertEquals("Deezer", deezer?.displayName)

        val tidal = StreamingServiceId.fromId("tidal")
        assertNotNull(tidal)
        assertEquals("Tidal", tidal?.displayName)
    }

    @Test
    fun testStreamingAccountModel() {
        val account = StreamingAccount(
            serviceId = "spotify",
            username = "test_user@spotify.com",
            accountType = "Premium"
        )

        assertEquals("spotify", account.serviceId)
        assertEquals("test_user@spotify.com", account.username)
        assertEquals("Premium", account.accountType)
        assertEquals(StreamingServiceId.SPOTIFY, account.serviceEnum)
    }

    @Test
    fun testStreamingTrackToSongConversion() {
        val mockUri = org.mockito.Mockito.mock(android.net.Uri::class.java)
        val track = StreamingTrack(
            id = "sp_101",
            serviceId = "spotify",
            title = "Blinding Lights",
            artist = "The Weeknd",
            album = "After Hours",
            durationMs = 200000L,
            artworkUrl = null,
            streamUrl = "https://stream.spotify.com/track/sp_101",
            genre = "Pop"
        )

        val song = track.toSong(overrideUri = mockUri)

        assertEquals("Blinding Lights", song.title)
        assertEquals("The Weeknd", song.artist)
        assertEquals("After Hours", song.album)
        assertEquals(200000L, song.durationMs)
        assertEquals("Streaming/spotify", song.folderPath)
    }

    @Test
    fun testStreamingCatalogRepositoryServiceTracks() {
        val spotifyTracks = StreamingCatalogRepository.getTracksForService("spotify")
        assertTrue(spotifyTracks.isNotEmpty())
        assertTrue(spotifyTracks.any { it.title == "Blinding Lights" })

        val ytTracks = StreamingCatalogRepository.getTracksForService("ytmusic")
        assertTrue(ytTracks.isNotEmpty())
        assertTrue(ytTracks.any { it.title == "Cruel Summer" })

        val amazonTracks = StreamingCatalogRepository.getTracksForService("amazon_music")
        assertTrue(amazonTracks.isNotEmpty())
        assertTrue(amazonTracks.any { it.title == "Fast Car" })

        val pandoraTracks = StreamingCatalogRepository.getTracksForService("pandora")
        assertTrue(pandoraTracks.isNotEmpty())

        val soundCloudTracks = StreamingCatalogRepository.getTracksForService("soundcloud")
        assertTrue(soundCloudTracks.isNotEmpty())

        val deezerTracks = StreamingCatalogRepository.getTracksForService("deezer")
        assertTrue(deezerTracks.isNotEmpty())

        val tidalTracks = StreamingCatalogRepository.getTracksForService("tidal")
        assertTrue(tidalTracks.isNotEmpty())
    }

    @Test
    fun testStreamingCatalogSearchFiltering() {
        val searchResults = StreamingCatalogRepository.getTracksForService("spotify", "Style")
        assertTrue(searchResults.any { it.artist.contains("Styles", ignoreCase = true) })
    }

    @Test
    fun testStreamingCatalogPlaylists() {
        val playlists = StreamingCatalogRepository.getPlaylistsForService("spotify")
        assertTrue(playlists.isNotEmpty())
        assertTrue(playlists.contains("Top Hits USA"))
    }
}
