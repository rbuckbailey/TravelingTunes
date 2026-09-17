package com.travelingtunes.app.core.media

import android.content.Context
import com.travelingtunes.app.core.database.MusicDatabase
import com.travelingtunes.app.core.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

data class CddbTrack(
    val discNumber: Int = 1,
    val trackNumber: Int,
    val title: String,
)

data class CddbAlbum(
    val artist: String,
    val album: String,
    val cddbId: String = "",
    val tracks: List<CddbTrack>,
)

class CddbManager(
    private val context: Context,
    private val musicDatabase: MusicDatabase,
) {
    private val cddbDir = File(context.filesDir, "cddb_data").apply { mkdirs() }

    private val _isEmbeddingCddb = MutableStateFlow(value = false)
    val isEmbeddingCddb: StateFlow<Boolean> = _isEmbeddingCddb.asStateFlow()

    private val _embeddingCddbStatusMessage = MutableStateFlow<String?>(null)
    val embeddingCddbStatusMessage: StateFlow<String?> = _embeddingCddbStatusMessage.asStateFlow()

    suspend fun getCddbOverridesCount(): Int = withContext(Dispatchers.IO) {
        musicDatabase.getCddbOverridesCount()
    }

    suspend fun ensureCddbMatchForAlbum(
        albumName: String,
        artistName: String,
        songs: List<Song>
    ): List<Song> = withContext(Dispatchers.IO) {
        if (songs.isEmpty()) return@withContext songs

        // If metadata already contains Disc or Track data, no CDDB lookup needed
        val hasDiscOrTrackData = songs.any { (it.discNumber > 0) || (it.trackNumber > 0) }
        if (hasDiscOrTrackData) {
            return@withContext songs.sortedWith(::compareAlbumSongs)
        }

        val hashKey = hashString("$artistName-$albumName")
        val cddbFile = File(cddbDir, "cddb_$hashKey.json")

        var cddbAlbum: CddbAlbum? = if (cddbFile.exists()) {
            loadCddbFile(cddbFile)
        } else {
            null
        }

        if (cddbAlbum == null) {
            cddbAlbum = fetchCddbTrackListOnline(artistName, albumName)
            cddbAlbum?.let { saveCddbFile(cddbFile, it) }
        }

        if ((cddbAlbum != null) && cddbAlbum.tracks.isNotEmpty()) {
            val updatedSongs = mutableListOf<Song>()
            val tracks = cddbAlbum.tracks.sortedWith(compareBy({ it.discNumber }, { it.trackNumber }))

            for ((index, song) in songs.withIndex()) {
                val matchedTrack = findBestMatchingTrack(song, tracks, index)
                val discNum = matchedTrack?.discNumber ?: 1
                val trackNum = matchedTrack?.trackNumber ?: (index + 1)

                musicDatabase.insertCddbOverride(
                    album = albumName,
                    artist = artistName,
                    songId = song.id,
                    discNumber = discNum,
                    trackNumber = trackNum,
                    title = matchedTrack?.title ?: song.title,
                    cddbId = cddbAlbum.cddbId.ifBlank { hashKey }
                )

                updatedSongs.add(
                    song.copy(
                        discNumber = discNum,
                        trackNumber = trackNum,
                        title = matchedTrack?.title.takeIf { !it.isNullOrBlank() } ?: song.title
                    )
                )
            }
            return@withContext updatedSongs.sortedWith(::compareAlbumSongs)
        }

        // Failing CDDB lookup, sort by alpha
        songs.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
    }

    private fun findBestMatchingTrack(song: Song, tracks: List<CddbTrack>, fallbackIndex: Int): CddbTrack? {
        val songTitleNorm = normalizeTitle(song.title)
        val songFileNorm = normalizeTitle(song.fileName.substringBeforeLast('.'))

        for (tr in tracks) {
            val trNorm = normalizeTitle(tr.title)
            if (trNorm.isNotBlank() && ((trNorm == songTitleNorm) || (trNorm == songFileNorm) || songTitleNorm.contains(trNorm) || trNorm.contains(songTitleNorm))) {
                return tr
            }
        }

        if (fallbackIndex in tracks.indices) {
            return tracks[fallbackIndex]
        }
        return null
    }

    private fun normalizeTitle(str: String): String {
        return str.lowercase().replace(Regex("[^a-z0-9]"), "")
    }

    private fun fetchCddbTrackListOnline(artist: String, album: String): CddbAlbum? {
        val term = when {
            artist.isBlank() -> album
            album.isBlank() -> artist
            else -> "$artist $album"
        }
        if (term.isBlank()) return null

        try {
            val encodedTerm = URLEncoder.encode(term, "UTF-8")
            val urlStr = "https://itunes.apple.com/search?term=$encodedTerm&entity=song&limit=50"
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }

            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()

                val jsonObj = JSONObject(jsonStr)
                val results = jsonObj.optJSONArray("results") ?: JSONArray()

                val tracks = mutableListOf<CddbTrack>()
                for (i in 0 until results.length()) {
                    val item = results.optJSONObject(i) ?: continue
                    val collectionName = item.optString("collectionName", "")
                    val trackName = item.optString("trackName", "")
                    val discNum = item.optInt("discNumber", 1)
                    val trackNum = item.optInt("trackNumber", 0)

                    if (trackName.isNotBlank() && trackNum > 0) {
                        if (album.isBlank() || collectionName.contains(album, ignoreCase = true) || album.contains(collectionName, ignoreCase = true)) {
                            tracks.add(CddbTrack(discNumber = discNum, trackNumber = trackNum, title = trackName))
                        }
                    }
                }

                if (tracks.isNotEmpty()) {
                    val sortedTracks = tracks.asSequence()
                        .distinctBy { Pair(it.discNumber, it.trackNumber) }
                        .sortedWith(compareBy({ it.discNumber }, { it.trackNumber }))
                        .toList()
                    return CddbAlbum(artist = artist, album = album, cddbId = "itunes", tracks = sortedTracks)
                }
            } else {
                conn.disconnect()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback: MusicBrainz release search API
        try {
            val query = "artist:\"$artist\" AND release:\"$album\""
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val mbUrlStr = "https://musicbrainz.org/ws/2/release/?query=$encodedQuery&fmt=json&limit=1"

            val conn = (URL(mbUrlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "TravelingTunes/1.0 (android-app)")
            }

            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()

                val jsonObj = JSONObject(jsonStr)
                val releases = jsonObj.optJSONArray("releases") ?: JSONArray()
                if (releases.length() > 0) {
                    val rel = releases.getJSONObject(0)
                    val releaseId = rel.optString("id", "")
                    val media = rel.optJSONArray("media") ?: JSONArray()

                    val tracks = mutableListOf<CddbTrack>()
                    for (m in 0 until media.length()) {
                        val med = media.optJSONObject(m) ?: continue
                        val discNum = med.optInt("position", m + 1)
                        val trackList = med.optJSONArray("tracks") ?: JSONArray()

                        for (t in 0 until trackList.length()) {
                            val tr = trackList.optJSONObject(t) ?: continue
                            val tNum = tr.optInt("position", t + 1)
                            val title = tr.optString("title", "")
                            if (title.isNotBlank()) {
                                tracks.add(CddbTrack(discNumber = discNum, trackNumber = tNum, title = title))
                            }
                        }
                    }

                    if (tracks.isNotEmpty()) {
                        return CddbAlbum(artist = artist, album = album, cddbId = releaseId, tracks = tracks)
                    }
                }
            } else {
                conn.disconnect()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    private fun loadCddbFile(file: File): CddbAlbum? {
        return try {
            val jsonStr = file.readText()
            val obj = JSONObject(jsonStr)
            val artist = obj.optString("artist", "")
            val album = obj.optString("album", "")
            val cddbId = obj.optString("cddbId", "")
            val tracksArray = obj.optJSONArray("tracks") ?: JSONArray()

            val tracks = mutableListOf<CddbTrack>()
            for (i in 0 until tracksArray.length()) {
                val tObj = tracksArray.optJSONObject(i) ?: continue
                tracks.add(
                    CddbTrack(
                        discNumber = tObj.optInt("discNumber", 1),
                        trackNumber = tObj.optInt("trackNumber", 1),
                        title = tObj.optString("title", "")
                    )
                )
            }
            CddbAlbum(artist, album, cddbId, tracks)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveCddbFile(file: File, album: CddbAlbum) {
        try {
            val obj = JSONObject()
            obj.put("artist", album.artist)
            obj.put("album", album.album)
            obj.put("cddbId", album.cddbId)

            val tracksArr = JSONArray()
            for (tr in album.tracks) {
                val tObj = JSONObject()
                tObj.put("discNumber", tr.discNumber)
                tObj.put("trackNumber", tr.trackNumber)
                tObj.put("title", tr.title)
                tracksArr.put(tObj)
            }
            obj.put("tracks", tracksArr)

            FileOutputStream(file).use { fos ->
                fos.write(obj.toString(2).toByteArray(Charsets.UTF_8))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun embedAllCddbOverrides(): Pair<Int, Int> = BackgroundTaskGate.runAsBackgroundTask {
        if (_isEmbeddingCddb.value) return@runAsBackgroundTask Pair(0, 0)
        _isEmbeddingCddb.value = true
        _embeddingCddbStatusMessage.value = "Starting CDDB ID3 tag embedding..."

        val overrides = musicDatabase.getCddbOverrides()
        val allSongsMap = musicDatabase.getAllSongs().associateBy { it.id }

        var succ = 0
        var fail = 0

        for ((idx, rec) in overrides.withIndex()) {
            BackgroundTaskGate.checkYieldAndPause()
            _embeddingCddbStatusMessage.value = "Embedding CDDB tags: ${rec.title} (${idx + 1}/${overrides.size})"
            val song = allSongsMap[rec.songId]
            if (song != null) {
                val ok = Id3TagEmbedder.embedCddbTagsIntoSong(context, song, rec)
                if (ok) succ++ else fail++
            } else {
                fail++
            }
        }

        val msg = "Embedded CDDB tags into $succ songs ($fail failed)."
        _embeddingCddbStatusMessage.value = msg
        _isEmbeddingCddb.value = false
        Pair(succ, fail)
    }

    private fun hashString(input: String): String {
        return MessageDigest.getInstance("MD5")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private fun compareAlbumSongs(s1: Song, s2: Song): Int {
        val d1 = if (s1.discNumber > 0) s1.discNumber else Int.MAX_VALUE
        val d2 = if (s2.discNumber > 0) s2.discNumber else Int.MAX_VALUE
        if (d1 != d2) return d1.compareTo(d2)

        val t1 = if (s1.trackNumber > 0) s1.trackNumber else Int.MAX_VALUE
        val t2 = if (s2.trackNumber > 0) s2.trackNumber else Int.MAX_VALUE
        if (t1 != t2) return t1.compareTo(t2)

        return String.CASE_INSENSITIVE_ORDER.compare(s1.title, s2.title)
    }
}
