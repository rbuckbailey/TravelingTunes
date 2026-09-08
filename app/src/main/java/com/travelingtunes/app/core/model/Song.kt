package com.travelingtunes.app.core.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val contentUri: Uri,
    val artworkUri: Uri? = null,
    val userRating: Int = 0, // 0 to 5 stars
    val genre: String = "Unknown Genre",
    val folderPath: String = "",
    val fileName: String = "",
    val trackNumber: Int = 0,
    val year: Int = 0
)
