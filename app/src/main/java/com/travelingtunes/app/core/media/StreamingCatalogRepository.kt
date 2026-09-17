package com.travelingtunes.app.core.media

import com.travelingtunes.app.core.model.StreamingTrack

object StreamingCatalogRepository {

    private val spotifyTracks = listOf(
        StreamingTrack("sp_1", "spotify", "Blinding Lights", "The Weeknd", "After Hours", 200000L, null, "https://stream.spotify.com/track/sp_1", genre = "Pop"),
        StreamingTrack("sp_2", "spotify", "As It Was", "Harry Styles", "Harry's House", 167000L, null, "https://stream.spotify.com/track/sp_2", genre = "Pop"),
        StreamingTrack("sp_3", "spotify", "Starboy", "The Weeknd ft. Daft Punk", "Starboy", 230000L, null, "https://stream.spotify.com/track/sp_3", genre = "R&B"),
        StreamingTrack("sp_4", "spotify", "Shape of You", "Ed Sheeran", "÷ (Divide)", 233000L, null, "https://stream.spotify.com/track/sp_4", genre = "Pop"),
        StreamingTrack("sp_5", "spotify", "Levitating", "Dua Lipa", "Future Nostalgia", 203000L, null, "https://stream.spotify.com/track/sp_5", genre = "Disco Pop"),
    )

    private val ytMusicTracks = listOf(
        StreamingTrack("yt_1", "ytmusic", "Cruel Summer", "Taylor Swift", "Lover", 178000L, null, "https://music.youtube.com/watch?v=yt_1", genre = "Pop"),
        StreamingTrack("yt_2", "ytmusic", "Bad Guy", "Billie Eilish", "WHEN WE ALL FALL ASLEEP", 194000L, null, "https://music.youtube.com/watch?v=yt_2", genre = "Alt Pop"),
        StreamingTrack("yt_3", "ytmusic", "Peaches", "Justin Bieber ft. Daniel Caesar", "Justice", 198000L, null, "https://music.youtube.com/watch?v=yt_3", genre = "Pop/R&B"),
        StreamingTrack("yt_4", "ytmusic", "Flowers", "Miley Cyrus", "Endless Summer Vacation", 200000L, null, "https://music.youtube.com/watch?v=yt_4", genre = "Pop"),
        StreamingTrack("yt_5", "ytmusic", "Stay", "The Kid LAROI & Justin Bieber", "F*CK LOVE 3", 141000L, null, "https://music.youtube.com/watch?v=yt_5", genre = "Pop"),
    )

    private val amazonMusicTracks = listOf(
        StreamingTrack("am_1", "amazon_music", "Good 4 U", "Olivia Rodrigo", "SOUR", 178000L, null, "https://music.amazon.com/tracks/am_1", genre = "Pop Punk"),
        StreamingTrack("am_2", "amazon_music", "Fast Car", "Luke Combs", "Gettin' Old", 265000L, null, "https://music.amazon.com/tracks/am_2", genre = "Country"),
        StreamingTrack("am_3", "amazon_music", "Sunflower", "Post Malone & Swae Lee", "Spider-Man: Into the Spider-Verse", 158000L, null, "https://music.amazon.com/tracks/am_3", genre = "Hip-Hop"),
        StreamingTrack("am_4", "amazon_music", "Heat Waves", "Glass Animals", "Dreamland", 238000L, null, "https://music.amazon.com/tracks/am_4", genre = "Indie Pop"),
        StreamingTrack("am_5", "amazon_music", "Shallow", "Lady Gaga & Bradley Cooper", "A Star Is Born", 215000L, null, "https://music.amazon.com/tracks/am_5", genre = "Soundtrack")
    )

    private val pandoraTracks = listOf(
        StreamingTrack("pan_1", "pandora", "Radioactive", "Imagine Dragons", "Night Visions", 186000L, null, "https://pandora.com/station/pan_1", genre = "Alternative"),
        StreamingTrack("pan_2", "pandora", "Counting Stars", "OneRepublic", "Native", 257000L, null, "https://pandora.com/station/pan_2", genre = "Pop Rock"),
        StreamingTrack("pan_3", "pandora", "Believer", "Imagine Dragons", "Evolve", 204000L, null, "https://pandora.com/station/pan_3", genre = "Alternative"),
        StreamingTrack("pan_4", "pandora", "Havana", "Camila Cabello", "Camila", 217000L, null, "https://pandora.com/station/pan_4", genre = "Latin Pop"),
        StreamingTrack("pan_5", "pandora", "Take Me to Church", "Hozier", "Hozier", 241000L, null, "https://pandora.com/station/pan_5", genre = "Indie Rock")
    )

    private val soundCloudTracks = listOf(
        StreamingTrack("sc_1", "soundcloud", "Sunset Drive", "Lofi Beats", "Chill Lounge Vol. 1", 160000L, null, "https://soundcloud.com/stream/sc_1", genre = "Lofi / Chill"),
        StreamingTrack("sc_2", "soundcloud", "Midnight City Synth", "RetroWave", "Neon Highways", 210000L, null, "https://soundcloud.com/stream/sc_2", genre = "Synthwave"),
        StreamingTrack("sc_3", "soundcloud", "Electric Pulse", "EDM Collective", "Festival Anthems", 225000L, null, "https://soundcloud.com/stream/sc_3", genre = "Electronic"),
        StreamingTrack("sc_4", "soundcloud", "Acoustic Sunset", "Indie Busker", "Unplugged Sessions", 190000L, null, "https://soundcloud.com/stream/sc_4", genre = "Acoustic"),
        StreamingTrack("sc_5", "soundcloud", "Urban Drift", "SubBass Producer", "Underground Beats", 175000L, null, "https://soundcloud.com/stream/sc_5", genre = "Trap")
    )

    private val deezerTracks = listOf(
        StreamingTrack("dz_1", "deezer", "Dance Monkey", "Tones and I", "The Kids Are Coming", 209000L, null, "https://deezer.com/track/dz_1", genre = "Dance Pop"),
        StreamingTrack("dz_2", "deezer", "Riptide", "Vance Joy", "Dream Your Life Away", 204000L, null, "https://deezer.com/track/dz_2", genre = "Indie Folk"),
        StreamingTrack("dz_3", "deezer", "Closer", "The Chainsmokers ft. Halsey", "Collage", 244000L, null, "https://deezer.com/track/dz_3", genre = "EDM Pop"),
        StreamingTrack("dz_4", "deezer", "Drivers License", "Olivia Rodrigo", "SOUR", 242000L, null, "https://deezer.com/track/dz_4", genre = "Pop"),
        StreamingTrack("dz_5", "deezer", "Stay With Me", "Sam Smith", "In the Lonely Hour", 172000L, null, "https://deezer.com/track/dz_5", genre = "Soul Pop")
    )

    private val tidalTracks = listOf(
        StreamingTrack("td_1", "tidal", "Empire State of Mind", "Jay-Z ft. Alicia Keys", "The Blueprint 3", 276000L, null, "https://tidal.com/track/td_1", genre = "Hip-Hop"),
        StreamingTrack("td_2", "tidal", "Lemonade (Master Edition)", "Beyoncé", "Lemonade", 235000L, null, "https://tidal.com/track/td_2", genre = "R&B"),
        StreamingTrack("td_3", "tidal", "Redemption", "Drake", "Views", 330000L, null, "https://tidal.com/track/td_3", genre = "Hip-Hop / R&B"),
        StreamingTrack("td_4", "tidal", "Highest in the Room", "Travis Scott", "JACKBOYS", 175000L, null, "https://tidal.com/track/td_4", genre = "Trap"),
        StreamingTrack("td_5", "tidal", "Love on the Brain", "Rihanna", "ANTI", 224000L, null, "https://tidal.com/track/td_5", genre = "Soul")
    )

    fun getTracksForService(serviceId: String, query: String = ""): List<StreamingTrack> {
        val tracks = when (serviceId.lowercase()) {
            "spotify" -> spotifyTracks
            "ytmusic" -> ytMusicTracks
            "amazon_music" -> amazonMusicTracks
            "pandora" -> pandoraTracks
            "soundcloud" -> soundCloudTracks
            "deezer" -> deezerTracks
            "tidal" -> tidalTracks
            else -> spotifyTracks
        }

        if (query.isBlank()) return tracks

        return tracks.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true) ||
            it.album.contains(query, ignoreCase = true) ||
            it.genre.contains(query, ignoreCase = true)
        }
    }

    fun getPlaylistsForService(serviceId: String): List<String> {
        return when (serviceId.lowercase()) {
            "spotify" -> listOf("Top Hits USA", "Discover Weekly", "Release Radar", "Chill Hits", "Driving Anthems")
            "ytmusic" -> listOf("My Supermix", "Trending Charts", "Workout Energy", "Focus Instrumental", "Acoustic Pop")
            "amazon_music" -> listOf("All Hits", "Country Heat", "Fresh Indie", "Prime Music Rewind", "Roadtrip Hits")
            "pandora" -> listOf("Today's Hits Station", "Thumbprint Radio", "Pop Collector", "Rock Classics Station", "90s Alternative")
            "soundcloud" -> listOf("Daily Drops", "Hot Underground", "Electronic Pulse", "Lofi Beats to Study", "Independent Artists")
            "deezer" -> listOf("Flow Mix", "Top 50 Global", "Dance Floor", "Pop Hits", "Acoustic Chill")
            "tidal" -> listOf("Master Hi-Fi Mix", "Rising Hip-Hop", "R&B Anthems", "Jazz Essentials", "Curated Top 40")
            else -> listOf("Popular Playlist 1", "Popular Playlist 2", "Trending Tracks")
        }
    }
}
