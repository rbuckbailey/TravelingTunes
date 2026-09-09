package com.travelingtunes.app.core.model

import android.net.Uri

enum class StreamingServiceId(
    val id: String,
    val displayName: String,
    val brandColorHex: Long,
    val openApiDescription: String,
    val sampleUser: String
) {
    SPOTIFY("spotify", "Spotify", 0xFF1DB954, "Spotify Web API (v1)", "user@spotify.com"),
    YOUTUBE_MUSIC("ytmusic", "YouTube Music", 0xFFFF0000, "YouTube Music Data API v3", "user@youtube.com"),
    AMAZON_MUSIC("amazon_music", "Amazon Prime Music", 0xFF00A8E1, "Amazon Music Catalog API", "user@amazon.com"),
    PANDORA("pandora", "Pandora", 0xFF224099, "Pandora Developer API", "user@pandora.com"),
    SOUNDCLOUD("soundcloud", "SoundCloud", 0xFFFF5500, "SoundCloud Open API v2", "user@soundcloud.com"),
    DEEZER("deezer", "Deezer", 0xFFA220DF, "Deezer Open API", "user@deezer.com"),
    TIDAL("tidal", "Tidal", 0xFF000000, "Tidal Developer API", "user@tidal.com");

    companion object {
        fun fromId(id: String): StreamingServiceId? = entries.find { it.id.equals(id, ignoreCase = true) }
    }
}

data class StreamingAccount(
    val serviceId: String,
    val username: String,
    val accountType: String = "Premium",
    val signedInAt: Long = System.currentTimeMillis()
) {
    val serviceEnum: StreamingServiceId?
        get() = StreamingServiceId.fromId(serviceId)
}

data class StreamingTrack(
    val id: String,
    val serviceId: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val artworkUrl: String? = null,
    val streamUrl: String,
    val explicit: Boolean = false,
    val genre: String = "Popular"
) {
    fun toSong(overrideUri: Uri? = null): Song {
        val uniqueNumericId = (serviceId.hashCode().toLong() shl 32) or (id.hashCode().toLong() and 0xFFFFFFFFL)
        val contentUri = overrideUri ?: try { Uri.parse(streamUrl) } catch (e: Throwable) { null } ?: Uri.EMPTY

        return Song(
            id = uniqueNumericId,
            title = title,
            artist = artist,
            album = album,
            albumId = serviceId.hashCode().toLong(),
            durationMs = durationMs,
            contentUri = contentUri,
            artworkUri = artworkUrl?.let { try { Uri.parse(it) } catch (e: Throwable) { null } },
            userRating = 5,
            genre = genre,
            folderPath = "Streaming/$serviceId",
            fileName = "$title - $artist",
            trackNumber = 1,
            year = 2026
        )
    }
}
