package com.travelingtunes.app.core.media

import android.content.Context
import android.net.Uri
import com.travelingtunes.app.core.database.MusicDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class MetadataTrackRecord(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val genre: String = "",
    val year: Int = 0,
    val trackNumber: Int = 0,
    val discNumber: Int = 0,
    val userRating: Int = 0,
    val folderPath: String = "",
    val fileName: String = ""
)

data class MetadataBackupPayload(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val tracks: List<MetadataTrackRecord> = emptyList(),
    val albumArtworks: Map<String, String> = emptyMap()
)

data class RestoreFieldOptions(
    val genre: Boolean = true,
    val albumArt: Boolean = true,
    val title: Boolean = true,
    val artist: Boolean = true,
    val album: Boolean = true,
    val yearAndTrack: Boolean = true,
    val userRating: Boolean = true
) {
    val isAllSelected: Boolean
        get() = genre && albumArt && title && artist && album && yearAndTrack && userRating

    val hasAnySelected: Boolean
        get() = genre || albumArt || title || artist || album || yearAndTrack || userRating
}

object MusicMetadataBackupHelper {

    suspend fun exportMetadataToJson(
        context: Context,
        musicDatabase: MusicDatabase
    ): String = withContext(Dispatchers.IO) {
        val songs = musicDatabase.getAllSongs()
        val trackRecords = songs.map { song ->
            MetadataTrackRecord(
                title = song.title,
                artist = song.artist,
                album = song.album,
                genre = song.genre,
                year = song.year,
                trackNumber = song.trackNumber,
                discNumber = song.discNumber,
                userRating = song.userRating,
                folderPath = song.folderPath,
                fileName = song.fileName
            )
        }

        val albumArtMap = mutableMapOf<String, String>()
        val processedAlbums = mutableSetOf<String>()

        for (song in songs) {
            val key = "${song.artist.lowercase().trim()}|${song.album.lowercase().trim()}"
            if (processedAlbums.add(key) && song.artworkUri != null) {
                try {
                    val bytes = if (song.artworkUri.scheme == "file") {
                        val file = File(song.artworkUri.path ?: "")
                        if (file.exists()) file.readBytes() else null
                    } else {
                        context.contentResolver.openInputStream(song.artworkUri)?.use { it.readBytes() }
                    }

                    if (bytes != null && bytes.isNotEmpty()) {
                        val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        albumArtMap[key] = base64
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        val payload = MetadataBackupPayload(
            version = 1,
            timestamp = System.currentTimeMillis(),
            tracks = trackRecords,
            albumArtworks = albumArtMap
        )

        serializePayloadToJson(payload)
    }

    fun serializePayloadToJson(payload: MetadataBackupPayload): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"version\": ${payload.version},\n")
        sb.append("  \"timestamp\": ${payload.timestamp},\n")

        sb.append("  \"tracks\": [\n")
        payload.tracks.forEachIndexed { i, track ->
            sb.append("    {\n")
            sb.append("      \"title\": \"${escapeJson(track.title)}\",\n")
            sb.append("      \"artist\": \"${escapeJson(track.artist)}\",\n")
            sb.append("      \"album\": \"${escapeJson(track.album)}\",\n")
            sb.append("      \"genre\": \"${escapeJson(track.genre)}\",\n")
            sb.append("      \"year\": ${track.year},\n")
            sb.append("      \"trackNumber\": ${track.trackNumber},\n")
            sb.append("      \"discNumber\": ${track.discNumber},\n")
            sb.append("      \"userRating\": ${track.userRating},\n")
            sb.append("      \"folderPath\": \"${escapeJson(track.folderPath)}\",\n")
            sb.append("      \"fileName\": \"${escapeJson(track.fileName)}\"\n")
            sb.append("    }${if (i < payload.tracks.size - 1) "," else ""}\n")
        }
        sb.append("  ],\n")

        sb.append("  \"albumArtworks\": {\n")
        val artEntries = payload.albumArtworks.entries.toList()
        artEntries.forEachIndexed { i, (key, value) ->
            sb.append("    \"${escapeJson(key)}\": \"${escapeJson(value)}\"${if (i < artEntries.size - 1) "," else ""}\n")
        }
        sb.append("  }\n")

        sb.append("}")
        return sb.toString()
    }

    fun parsePayloadFromJson(jsonString: String): MetadataBackupPayload? {
        return try {
            val tracks = mutableListOf<MetadataTrackRecord>()
            val tracksPart = jsonString.substringAfter("\"tracks\": [", "").substringBefore("],")
            val rawTrackObjects = tracksPart.split("}")

            for (rawObj in rawTrackObjects) {
                if (rawObj.contains("\"title\"")) {
                    val title = extractJsonValue(rawObj, "title")
                    val artist = extractJsonValue(rawObj, "artist")
                    val album = extractJsonValue(rawObj, "album")
                    val genre = extractJsonValue(rawObj, "genre")
                    val year = extractJsonValue(rawObj, "year").toIntOrNull() ?: 0
                    val trackNumber = extractJsonValue(rawObj, "trackNumber").toIntOrNull() ?: 0
                    val discNumber = extractJsonValue(rawObj, "discNumber").toIntOrNull() ?: 0
                    val userRating = extractJsonValue(rawObj, "userRating").toIntOrNull() ?: 0
                    val folderPath = extractJsonValue(rawObj, "folderPath")
                    val fileName = extractJsonValue(rawObj, "fileName")

                    tracks.add(
                        MetadataTrackRecord(
                            title = title,
                            artist = artist,
                            album = album,
                            genre = genre,
                            year = year,
                            trackNumber = trackNumber,
                            discNumber = discNumber,
                            userRating = userRating,
                            folderPath = folderPath,
                            fileName = fileName
                        )
                    )
                }
            }

            val artMap = mutableMapOf<String, String>()
            val artPart = jsonString.substringAfter("\"albumArtworks\": {", "").substringBefore("}")
            val artLines = artPart.split("\n", ",")
            for (line in artLines) {
                if (line.contains(":")) {
                    val parts = line.split(":", limit = 2)
                    if (parts.size == 2) {
                        val k = parts[0].trim().trim('"', ' ', '\t', '\r', '\n')
                        val v = parts[1].trim().trim('"', ' ', '\t', '\r', '\n')
                        if (k.isNotBlank() && v.isNotBlank()) {
                            artMap[unescapeJson(k)] = unescapeJson(v)
                        }
                    }
                }
            }

            MetadataBackupPayload(1, System.currentTimeMillis(), tracks, artMap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun extractJsonValue(jsonBlock: String, keyName: String): String {
        val pattern = "\"$keyName\":\\s*\"([^\"]*)\"".toRegex()
        val match = pattern.find(jsonBlock)
        if (match != null) {
            return unescapeJson(match.groupValues[1])
        }
        val numPattern = "\"$keyName\":\\s*([0-9]+)".toRegex()
        val numMatch = numPattern.find(jsonBlock)
        if (numMatch != null) {
            return numMatch.groupValues[1]
        }
        return ""
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun unescapeJson(str: String): String {
        return str.replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
    }

    suspend fun restoreMetadataFromJson(
        context: Context,
        musicDatabase: MusicDatabase,
        jsonString: String,
        options: RestoreFieldOptions,
        onProgress: (current: Int, total: Int, status: String) -> Unit = { _, _, _ -> }
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val payload = parsePayloadFromJson(jsonString) ?: return@withContext Pair(0, 0)
        val dbSongs = musicDatabase.getAllSongs()
        if (dbSongs.isEmpty() || payload.tracks.isEmpty()) return@withContext Pair(0, 0)

        val songByFileName = dbSongs.associateBy { it.fileName.lowercase().trim() }
        val songByMeta = dbSongs.associateBy {
            "${it.artist.lowercase().trim()}|${it.album.lowercase().trim()}|${it.title.lowercase().trim()}"
        }

        var restoredCount = 0
        var failedCount = 0
        val total = payload.tracks.size

        for ((index, record) in payload.tracks.withIndex()) {
            if (index % 5 == 0 || index == total - 1) {
                withContext(Dispatchers.Main) {
                    onProgress(index + 1, total, "Restoring track ${index + 1} of $total...")
                }
            }

            val matchedSong = songByFileName[record.fileName.lowercase().trim()]
                ?: songByMeta["${record.artist.lowercase().trim()}|${record.album.lowercase().trim()}|${record.title.lowercase().trim()}"]

            if (matchedSong != null) {
                val updatedGenre = if (options.genre && record.genre.isNotBlank() && record.genre != "Unknown Genre") record.genre else null
                val updatedTitle = if (options.title && record.title.isNotBlank()) record.title else null
                val updatedArtist = if (options.artist && record.artist.isNotBlank()) record.artist else null
                val updatedAlbum = if (options.album && record.album.isNotBlank()) record.album else null
                val updatedYear = if (options.yearAndTrack && record.year > 0) record.year else null
                val updatedTrack = if (options.yearAndTrack && record.trackNumber > 0) record.trackNumber else null
                val updatedDisc = if (options.yearAndTrack && record.discNumber > 0) record.discNumber else null
                val updatedRating = if (options.userRating) record.userRating else null

                // 1. Update database fields
                musicDatabase.updateSongMetadata(
                    songId = matchedSong.id,
                    title = updatedTitle,
                    artist = updatedArtist,
                    album = updatedAlbum,
                    genre = updatedGenre,
                    year = updatedYear,
                    trackNumber = updatedTrack,
                    discNumber = updatedDisc,
                    userRating = updatedRating
                )

                // 2. Embed ID3 text tags into audio file on disk
                Id3TagEmbedder.embedMetadataTagsIntoSong(
                    context = context,
                    song = matchedSong,
                    genre = updatedGenre,
                    title = updatedTitle,
                    artist = updatedArtist,
                    album = updatedAlbum,
                    year = updatedYear,
                    trackNumber = updatedTrack,
                    discNumber = updatedDisc
                )

                // 3. Restore Album Artwork if selected
                if (options.albumArt) {
                    val albumKey = "${record.artist.lowercase().trim()}|${record.album.lowercase().trim()}"
                    val artBase64 = payload.albumArtworks[albumKey]
                    if (!artBase64.isNullOrBlank()) {
                        try {
                            val imageBytes = android.util.Base64.decode(artBase64, android.util.Base64.NO_WRAP)
                            if (imageBytes != null && imageBytes.isNotEmpty()) {
                                val artDir = File(context.cacheDir, "restored_art").apply { mkdirs() }
                                val hashKey = "${record.artist.hashCode()}_${record.album.hashCode()}"
                                val artFile = File(artDir, "art_restored_$hashKey.jpg")
                                FileOutputStream(artFile).use { fos -> fos.write(imageBytes) }

                                val artUri = Uri.fromFile(artFile)
                                musicDatabase.updateSongArtwork(matchedSong.id, artUri)

                                Id3ArtworkEmbedder.embedArtworkIntoSong(context, matchedSong, imageBytes)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                restoredCount++
            } else {
                failedCount++
            }
        }

        withContext(Dispatchers.Main) {
            onProgress(total, total, "Restored metadata for $restoredCount tracks.")
        }

        Pair(restoredCount, failedCount)
    }
}
