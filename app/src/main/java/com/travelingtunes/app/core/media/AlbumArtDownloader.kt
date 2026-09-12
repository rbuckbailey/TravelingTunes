package com.travelingtunes.app.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.asImageBitmap
import com.travelingtunes.app.core.database.MusicDatabase
import com.travelingtunes.app.core.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

data class ArtworkCandidate(
    val url: String,
    val width: Int,
    val height: Int
) {
    val squareness: Double
        get() {
            if (width <= 0 || height <= 0) return 0.0
            return Math.min(width, height).toDouble() / Math.max(width, height).toDouble()
        }

    val resolution: Int
        get() = width * height
}

class AlbumArtDownloader(
    private val context: Context,
    private val musicDatabase: MusicDatabase
) {

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadedCount = MutableStateFlow(0)
    val downloadedCount: StateFlow<Int> = _downloadedCount.asStateFlow()

    private val _totalToDownload = MutableStateFlow(0)
    val totalToDownload: StateFlow<Int> = _totalToDownload.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    suspend fun downloadMissingArtwork(): Int = withContext(Dispatchers.IO) {
        if (_isDownloading.value) return@withContext 0

        _isDownloading.value = true
        _downloadedCount.value = 0
        _totalToDownload.value = 0
        _statusMessage.value = "Scanning library for missing album art..."

        val allSongs = musicDatabase.getAllSongs()
        if (allSongs.isEmpty()) {
            _statusMessage.value = "Library is empty"
            _isDownloading.value = false
            return@withContext 0
        }

        // Group songs by (artist, album)
        val albumGroups = allSongs.groupBy { Pair(it.artist, it.album) }
        val missingGroups = mutableListOf<Pair<Pair<String, String>, List<Song>>>()

        for ((key, songs) in albumGroups) {
            val (artist, album) = key
            if (artist.isBlank() || album.isBlank()) continue

            // Check if any song in this group has valid artwork
            val hasArt = songs.any { song ->
                !isArtworkMissing(context, song)
            }

            if (!hasArt) {
                missingGroups.add(Pair(key, songs))
            }
        }

        if (missingGroups.isEmpty()) {
            _statusMessage.value = "All albums already have album art"
            _isDownloading.value = false
            return@withContext 0
        }

        _totalToDownload.value = missingGroups.size
        _statusMessage.value = "Found ${missingGroups.size} albums with missing artwork"

        val artworkCacheDir = File(context.cacheDir, "album_art").apply { mkdirs() }
        var successCount = 0

        for ((index, group) in missingGroups.withIndex()) {
            val (key, songs) = group
            val (artist, album) = key

            _statusMessage.value = "Searching artwork for: $artist - $album (${index + 1}/${missingGroups.size})"

            try {
                val artworkUri = searchAndDownloadArtwork(artist, album, artworkCacheDir)
                if (artworkUri != null) {
                    musicDatabase.updateAlbumArtwork(album, artist, artworkUri)
                    // Update cache for these songs
                    val bitmap = loadSongArtworkFromUri(context, artworkUri)
                    if (bitmap != null) {
                        for (song in songs) {
                            AlbumArtCache.instance.put(song.id, bitmap.asImageBitmap())
                        }
                    }
                    successCount++
                    _downloadedCount.value = successCount
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        _statusMessage.value = if (successCount > 0) {
            "Downloaded album art for $successCount of ${missingGroups.size} albums"
        } else {
            "Could not download artwork for missing albums"
        }

        _isDownloading.value = false
        return@withContext successCount
    }

    private fun isArtworkMissing(context: Context, song: Song): Boolean {
        val uri = song.artworkUri ?: return true
        val uriStr = uri.toString()
        if (uriStr.isBlank()) return true

        if (uri.scheme == "file") {
            val file = File(uri.path ?: "")
            return !file.exists() || file.length() == 0L
        }

        return try {
            val stream = context.contentResolver.openInputStream(uri)
            if (stream != null) {
                val hasBytes = stream.available() > 0
                stream.close()
                !hasBytes
            } else {
                true
            }
        } catch (e: Exception) {
            true
        }
    }

    suspend fun searchAndDownloadArtwork(
        artist: String,
        album: String,
        artworkCacheDir: File
    ): Uri? = withContext(Dispatchers.IO) {
        val queryStr = "$artist $album album art"
        val candidates = mutableListOf<ArtworkCandidate>()

        // 1. Search via iTunes Search API (returns high resolution square album art)
        try {
            val itunesCandidates = queryItunesAlbumArt(artist, album)
            candidates.addAll(itunesCandidates)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Search via free image search engine (DuckDuckGo / Web search parsing for "$artist $album album art")
        try {
            val webCandidates = queryFreeImageSearchEngine(queryStr)
            candidates.addAll(webCandidates)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (candidates.isEmpty()) return@withContext null

        // Select the squarest, most high-resolution search result
        val bestCandidate = selectBestCandidate(candidates) ?: return@withContext null

        // Download image bytes and save locally
        val downloadedFile = downloadImageToFile(bestCandidate.url, artist, album, artworkCacheDir)
        return@withContext downloadedFile?.let { Uri.fromFile(it) }
    }

    fun selectBestCandidate(candidates: List<ArtworkCandidate>): ArtworkCandidate? {
        if (candidates.isEmpty()) return null
        return candidates.maxWithOrNull(
            compareBy<ArtworkCandidate> { it.squareness }
                .thenBy { it.resolution }
        )
    }

    private fun queryItunesAlbumArt(artist: String, album: String): List<ArtworkCandidate> {
        val list = mutableListOf<ArtworkCandidate>()
        val encodedTerm = URLEncoder.encode("$artist $album", "UTF-8")
        val urlStr = "https://itunes.apple.com/search?term=$encodedTerm&entity=album&limit=5"

        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 5000
            setRequestProperty("User-Agent", "Mozilla/5.0")
        }

        if (conn.responseCode == 200) {
            val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
            val jsonObj = JSONObject(jsonStr)
            val results = jsonObj.optJSONArray("results") ?: return list

            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue
                val artUrl = item.optString("artworkUrl100", "")
                if (artUrl.isNotBlank()) {
                    val highResUrl = artUrl.replace("100x100bb", "1000x1000bb")
                    list.add(ArtworkCandidate(highResUrl, 1000, 1000))
                }
            }
        }
        conn.disconnect()
        return list
    }

    private fun queryFreeImageSearchEngine(queryStr: String): List<ArtworkCandidate> {
        val list = mutableListOf<ArtworkCandidate>()
        val encodedQuery = URLEncoder.encode(queryStr, "UTF-8")

        val urlStr = "https://html.duckduckgo.com/html/?q=$encodedQuery"
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 5000
            setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
        }

        if (conn.responseCode == 200) {
            val html = conn.inputStream.bufferedReader().use { it.readText() }
            val imgRegex = Regex("""(?:src|href)=["'](https?://[^"']+\.(?:jpg|jpeg|png))["']""", RegexOption.IGNORE_CASE)
            val matches = imgRegex.findAll(html)

            for (match in matches.take(10)) {
                val imgUrl = match.groupValues[1]
                if (imgUrl.contains("duckduckgo.com") && !imgUrl.contains("external-content")) continue

                val bounds = probeImageDimensions(imgUrl)
                if (bounds != null && bounds.first > 0 && bounds.second > 0) {
                    list.add(ArtworkCandidate(imgUrl, bounds.first, bounds.second))
                }
            }
        }
        conn.disconnect()
        return list
    }

    private fun probeImageDimensions(imgUrl: String): Pair<Int, Int>? {
        return try {
            val conn = (URL(imgUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3000
                readTimeout = 3000
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            if (conn.responseCode == 200) {
                conn.inputStream.use { stream ->
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(stream, null, options)
                    if (options.outWidth > 0 && options.outHeight > 0) {
                        Pair(options.outWidth, options.outHeight)
                    } else null
                }
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun downloadImageToFile(imgUrl: String, artist: String, album: String, artworkCacheDir: File): File? {
        return try {
            val hashKey = hashString("$artist-$album")
            val artFile = File(artworkCacheDir, "art_$hashKey.jpg")

            val conn = (URL(imgUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }

            if (conn.responseCode == 200) {
                conn.inputStream.use { input ->
                    FileOutputStream(artFile).use { output ->
                        input.copyTo(output)
                    }
                }
                conn.disconnect()
                if (artFile.exists() && artFile.length() > 0) artFile else null
            } else {
                conn.disconnect()
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun loadSongArtworkFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            if (uri.scheme == "file") {
                BitmapFactory.decodeFile(uri.path)
            } else {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun hashString(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
