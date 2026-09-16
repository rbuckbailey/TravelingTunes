package com.travelingtunes.app.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.asImageBitmap
import com.travelingtunes.app.core.database.MusicDatabase
import com.travelingtunes.app.core.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ArtworkCandidate(
    val url: String,
    val width: Int,
    val height: Int,
    val source: String = "Online"
) {
    val squareness: Double
        get() {
            if (width <= 0 || height <= 0) return 0.0
            return Math.min(width, height).toDouble() / Math.max(width, height).toDouble()
        }

    val resolution: Int
        get() = width * height
}

data class AlbumArtAuditItem(
    val artist: String,
    val album: String,
    val songCount: Int,
    val isSuccess: Boolean,
    val sourceEngine: String? = null,
    val failureReason: String? = null
)

data class AlbumArtAuditReport(
    val timestamp: Long = System.currentTimeMillis(),
    val totalProcessed: Int = 0,
    val successCount: Int = 0,
    val failedCount: Int = 0,
    val items: List<AlbumArtAuditItem> = emptyList()
) {
    fun toFormattedSummaryText(): String {
        val sb = StringBuilder()
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
        sb.appendLine("=== Traveling Tunes Album Art Audit Report ===")
        sb.appendLine("Date: $dateStr")
        sb.appendLine("Total Processed: $totalProcessed")
        sb.appendLine("Successfully Downloaded: $successCount")
        sb.appendLine("Failed / Missing: $failedCount")
        sb.appendLine()

        if (failedCount > 0) {
            sb.appendLine("--- FAILED / MISSING ALBUM ART ($failedCount) ---")
            sb.appendLine("Use these details to manually supply album art (e.g., placing folder.jpg or updating ID3 tags):")
            sb.appendLine()
            items.filter { !it.isSuccess }.forEachIndexed { idx, item ->
                sb.appendLine("${idx + 1}. Artist: \"${item.artist}\" | Album: \"${item.album}\" (${item.songCount} songs)")
                sb.appendLine("   Reason: ${item.failureReason ?: "No artwork found"}")
            }
            sb.appendLine()
        }

        if (successCount > 0) {
            sb.appendLine("--- SUCCESSFUL DOWNLOADS ($successCount) ---")
            items.filter { it.isSuccess }.forEachIndexed { idx, item ->
                sb.appendLine("${idx + 1}. \"${item.artist}\" - \"${item.album}\" [Source: ${item.sourceEngine ?: "Online"}]")
            }
        }

        return sb.toString()
    }
}

class AlbumArtDownloader(
    private val context: Context,
    private val musicDatabase: MusicDatabase
) {

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadedCount = MutableStateFlow(0)
    val downloadedCount: StateFlow<Int> = _downloadedCount.asStateFlow()

    private val _failedCount = MutableStateFlow(0)
    val failedCount: StateFlow<Int> = _failedCount.asStateFlow()

    private val _totalToDownload = MutableStateFlow(0)
    val totalToDownload: StateFlow<Int> = _totalToDownload.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _lastAuditReport = MutableStateFlow<AlbumArtAuditReport?>(null)
    val lastAuditReport: StateFlow<AlbumArtAuditReport?> = _lastAuditReport.asStateFlow()

    @Volatile
    private var isCancelled = false
    private var activeJob: kotlinx.coroutines.Job? = null

    fun cancelDownload() {
        isCancelled = true
        activeJob?.cancel()
        _isDownloading.value = false
        _statusMessage.value = "Album art download stopped"
    }

    suspend fun downloadMissingArtwork(): Int = BackgroundTaskGate.runAsBackgroundTask {
        if (_isDownloading.value) return@runAsBackgroundTask 0

        _isDownloading.value = true
        isCancelled = false
        _downloadedCount.value = 0
        _failedCount.value = 0
        _totalToDownload.value = 0
        _statusMessage.value = "Scanning library for missing album art..."

        val allSongs = musicDatabase.getAllSongs()
        if (allSongs.isEmpty()) {
            _statusMessage.value = "Library is empty"
            _isDownloading.value = false
            return@runAsBackgroundTask 0
        }

        // Group songs by (artist, album)
        val albumGroups = allSongs.groupBy { Pair(it.effectiveArtist, it.album) }
        val missingGroups = mutableListOf<Pair<Pair<String, String>, List<Song>>>()

        for ((key, songs) in albumGroups) {
            BackgroundTaskGate.checkYieldAndPause()
            val (artist, album) = key
            if (artist.isBlank() || album.isBlank()) continue

            // Ignore generic unknown tags
            if (isGenericName(artist) && isGenericName(album)) continue

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
            return@runAsBackgroundTask 0
        }

        _totalToDownload.value = missingGroups.size
        _statusMessage.value = "Found ${missingGroups.size} albums with missing artwork"

        val artworkCacheDir = File(context.filesDir, "downloaded_art").apply { mkdirs() }
        var successCount = 0
        val auditItems = mutableListOf<AlbumArtAuditItem>()

        for ((index, group) in missingGroups.withIndex()) {
            BackgroundTaskGate.checkYieldAndPause()
            if (isCancelled) {
                _statusMessage.value = "Album art download stopped ($successCount of ${missingGroups.size} downloaded)"
                break
            }

            val (key, songs) = group
            val (artist, album) = key

            _statusMessage.value = "Searching artwork for: \"$artist\" - \"$album\" (${index + 1}/${missingGroups.size})"

            try {
                val result = searchAndDownloadArtwork(artist, album, songs, artworkCacheDir)
                if (result != null) {
                    val (artworkUri, sourceEngine) = result
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
                    auditItems.add(
                        AlbumArtAuditItem(
                            artist = artist,
                            album = album,
                            songCount = songs.size,
                            isSuccess = true,
                            sourceEngine = sourceEngine
                        )
                    )
                } else {
                    _failedCount.value = (index + 1) - successCount
                    auditItems.add(
                        AlbumArtAuditItem(
                            artist = artist,
                            album = album,
                            songCount = songs.size,
                            isSuccess = false,
                            failureReason = "No matching artwork found across iTunes, Deezer, Cover Art Archive, and Web search"
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _failedCount.value = (index + 1) - successCount
                auditItems.add(
                    AlbumArtAuditItem(
                        artist = artist,
                        album = album,
                        songCount = songs.size,
                        isSuccess = false,
                        failureReason = "Error during search: ${e.message}"
                    )
                )
            }

            // Small delay between searches to prevent network rate limits
            delay(150)
        }

        val report = AlbumArtAuditReport(
            timestamp = System.currentTimeMillis(),
            totalProcessed = missingGroups.size,
            successCount = successCount,
            failedCount = missingGroups.size - successCount,
            items = auditItems
        )
        _lastAuditReport.value = report

        _statusMessage.value = if (successCount > 0) {
            "Downloaded album art for $successCount of ${missingGroups.size} albums"
        } else {
            "Could not download artwork for missing albums"
        }

        _isDownloading.value = false
        return@runAsBackgroundTask successCount
    }

    private fun isGenericName(name: String): Boolean {
        val lower = name.trim().lowercase()
        return lower == "unknown" || lower == "unknown artist" || lower == "unknown album" || lower == "<unknown>" || lower == "music"
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
        songs: List<Song>,
        artworkCacheDir: File
    ): Pair<Uri, String>? = coroutineScope {
        // 0. Check local folder artwork first
        val localUri = checkLocalFolderArtwork(songs)
        if (localUri != null) {
            return@coroutineScope Pair(localUri, "Local Folder")
        }

        val cleanArtist = sanitizeMetadata(artist)
        val cleanAlbum = sanitizeMetadata(album)

        // 1. Quoted Artist + Album ("Artist" "Album")
        val tier1Candidates = fetchCandidatesInParallel(cleanArtist, cleanAlbum, useQuotes = true, searchMode = SearchMode.BOTH)
        if (tier1Candidates.isNotEmpty()) {
            val best = selectBestCandidate(tier1Candidates)
            if (best != null) {
                val file = downloadImageToFile(best.url, artist, album, artworkCacheDir)
                if (file != null) return@coroutineScope Pair(Uri.fromFile(file), best.source)
            }
        }

        // 2. Unquoted Artist + Album (Artist Album)
        val tier2Candidates = fetchCandidatesInParallel(cleanArtist, cleanAlbum, useQuotes = false, searchMode = SearchMode.BOTH)
        if (tier2Candidates.isNotEmpty()) {
            val best = selectBestCandidate(tier2Candidates)
            if (best != null) {
                val file = downloadImageToFile(best.url, artist, album, artworkCacheDir)
                if (file != null) return@coroutineScope Pair(Uri.fromFile(file), best.source)
            }
        }

        // 3. Quoted Album Only ("Album")
        val tier3Candidates = fetchCandidatesInParallel(cleanArtist = "", cleanAlbum = cleanAlbum, useQuotes = true, searchMode = SearchMode.ALBUM_ONLY)
        if (tier3Candidates.isNotEmpty()) {
            val best = selectBestCandidate(tier3Candidates)
            if (best != null) {
                val file = downloadImageToFile(best.url, artist, album, artworkCacheDir)
                if (file != null) return@coroutineScope Pair(Uri.fromFile(file), best.source)
            }
        }

        // 4. Unquoted Album Only (Album)
        val tier4Candidates = fetchCandidatesInParallel(cleanArtist = "", cleanAlbum = cleanAlbum, useQuotes = false, searchMode = SearchMode.ALBUM_ONLY)
        if (tier4Candidates.isNotEmpty()) {
            val best = selectBestCandidate(tier4Candidates)
            if (best != null) {
                val file = downloadImageToFile(best.url, artist, album, artworkCacheDir)
                if (file != null) return@coroutineScope Pair(Uri.fromFile(file), best.source)
            }
        }

        // 5. Quoted Artist Only ("Artist") — Artist portrait/filler search
        if (cleanArtist.isNotBlank()) {
            val tier5Candidates = fetchCandidatesInParallel(cleanArtist = cleanArtist, cleanAlbum = "", useQuotes = true, searchMode = SearchMode.ARTIST_ONLY)
            if (tier5Candidates.isNotEmpty()) {
                val best = selectBestCandidate(tier5Candidates)
                if (best != null) {
                    val file = downloadImageToFile(best.url, artist, album, artworkCacheDir)
                    if (file != null) return@coroutineScope Pair(Uri.fromFile(file), "${best.source} (Artist Portrait)")
                }
            }

            // 6. Unquoted Artist Only (Artist)
            val tier6Candidates = fetchCandidatesInParallel(cleanArtist = cleanArtist, cleanAlbum = "", useQuotes = false, searchMode = SearchMode.ARTIST_ONLY)
            if (tier6Candidates.isNotEmpty()) {
                val best = selectBestCandidate(tier6Candidates)
                if (best != null) {
                    val file = downloadImageToFile(best.url, artist, album, artworkCacheDir)
                    if (file != null) return@coroutineScope Pair(Uri.fromFile(file), "${best.source} (Artist Portrait)")
                }
            }
        }

        // 7. Artist + First Song Title
        val firstTrackTitle = songs.firstOrNull()?.title?.let { sanitizeMetadata(it) }
        if (!firstTrackTitle.isNullOrBlank() && !firstTrackTitle.equals(cleanAlbum, ignoreCase = true)) {
            val tier7Candidates = fetchCandidatesInParallel(cleanArtist = cleanArtist, cleanAlbum = firstTrackTitle, useQuotes = false, searchMode = SearchMode.BOTH)
            if (tier7Candidates.isNotEmpty()) {
                val best = selectBestCandidate(tier7Candidates)
                if (best != null) {
                    val file = downloadImageToFile(best.url, artist, album, artworkCacheDir)
                    if (file != null) return@coroutineScope Pair(Uri.fromFile(file), best.source)
                }
            }
        }

        return@coroutineScope null
    }

    private fun checkLocalFolderArtwork(songs: List<Song>): Uri? {
        val commonNames = listOf("cover.jpg", "cover.png", "folder.jpg", "folder.png", "album.jpg", "album.png", "front.jpg", "front.png")
        for (song in songs) {
            val uri = song.contentUri
            if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                val parent = file.parentFile ?: continue
                for (name in commonNames) {
                    val imgFile = File(parent, name)
                    if (imgFile.exists() && imgFile.length() > 0) {
                        return Uri.fromFile(imgFile)
                    }
                }
            }
        }
        return null
    }

    enum class SearchMode { BOTH, ALBUM_ONLY, ARTIST_ONLY }

    enum class SearchEngine(val displayName: String) {
        DEEZER("Deezer"),
        ITUNES("iTunes"),
        COVER_ART_ARCHIVE("Cover Art Archive"),
        WEB_SEARCH("Web Search")
    }

    private suspend fun fetchCandidatesInParallel(
        cleanArtist: String,
        cleanAlbum: String,
        useQuotes: Boolean,
        searchMode: SearchMode,
        enabledEngines: Set<SearchEngine> = SearchEngine.entries.toSet()
    ): List<ArtworkCandidate> = coroutineScope {
        val tasks = mutableListOf<kotlinx.coroutines.Deferred<List<ArtworkCandidate>>>()

        if (enabledEngines.contains(SearchEngine.DEEZER)) {
            tasks.add(async(Dispatchers.IO) {
                try {
                    when (searchMode) {
                        SearchMode.ALBUM_ONLY -> queryDeezerAlbumArt("", cleanAlbum, useQuotes)
                        SearchMode.ARTIST_ONLY -> queryDeezerAlbumArt(cleanArtist, "", useQuotes)
                        SearchMode.BOTH -> queryDeezerAlbumArt(cleanArtist, cleanAlbum, useQuotes)
                    }
                } catch (e: Exception) { emptyList() }
            })
        }

        if (enabledEngines.contains(SearchEngine.ITUNES)) {
            tasks.add(async(Dispatchers.IO) {
                try {
                    when (searchMode) {
                        SearchMode.ALBUM_ONLY -> queryItunesAlbumArt("", cleanAlbum, useQuotes, entityFilter = true)
                        SearchMode.ARTIST_ONLY -> queryItunesAlbumArt(cleanArtist, "", useQuotes, entityFilter = false)
                        SearchMode.BOTH -> queryItunesAlbumArt(cleanArtist, cleanAlbum, useQuotes, entityFilter = true)
                    }
                } catch (e: Exception) { emptyList() }
            })
        }

        if (enabledEngines.contains(SearchEngine.COVER_ART_ARCHIVE)) {
            tasks.add(async(Dispatchers.IO) {
                try {
                    if (searchMode == SearchMode.BOTH && cleanArtist.isNotBlank()) {
                        queryCoverArtArchive(cleanArtist, cleanAlbum)
                    } else emptyList()
                } catch (e: Exception) { emptyList() }
            })
        }

        if (enabledEngines.contains(SearchEngine.WEB_SEARCH)) {
            tasks.add(async(Dispatchers.IO) {
                try {
                    when (searchMode) {
                        SearchMode.ALBUM_ONLY -> queryFreeImageSearchEngine("", cleanAlbum)
                        SearchMode.ARTIST_ONLY -> queryFreeImageSearchEngine(cleanArtist, "")
                        SearchMode.BOTH -> queryFreeImageSearchEngine(cleanArtist, cleanAlbum)
                    }
                } catch (e: Exception) { emptyList() }
            })
        }

        val results = tasks.awaitAll()
        results.flatten()
    }

    fun sanitizeMetadata(input: String): String {
        if (input.isBlank()) return ""
        var cleaned = input
        // Remove brackets/parentheticals like (Deluxe Version), [2021 Remaster], (feat. ...), - Single
        cleaned = cleaned.replace(Regex("""(?i)[\(\[\{](?:deluxe|remaster|re-master|bonus|expanded|anniversary|special|edition|version|feat\.|live|explicit|mono|stereo|single|ep).*?[\)\]\}]"""), "")
        cleaned = cleaned.replace(Regex("""(?i)\s*-\s*Single$"""), "")
        cleaned = cleaned.replace(Regex("""(?i)\s*-\s*EP$"""), "")
        cleaned = cleaned.replace(Regex("""(?i)\s*feat\..*?$"""), "")
        cleaned = cleaned.replace(Regex("""\s+"""), " ").trim()
        return cleaned.ifBlank { input }
    }

    fun selectBestCandidate(candidates: List<ArtworkCandidate>): ArtworkCandidate? {
        if (candidates.isEmpty()) return null
        return candidates.maxWithOrNull(
            compareBy<ArtworkCandidate> { it.squareness }
                .thenBy { it.resolution }
        )
    }

    private fun queryDeezerAlbumArt(artist: String, album: String, useQuotes: Boolean): List<ArtworkCandidate> {
        val list = mutableListOf<ArtworkCandidate>()
        val term = when {
            artist.isBlank() -> if (useQuotes) "\"$album\"" else album
            album.isBlank() -> if (useQuotes) "\"$artist\"" else artist
            useQuotes -> "\"$artist\" \"$album\""
            else -> "$artist $album"
        }
        val encodedTerm = URLEncoder.encode(term, "UTF-8")

        // 1. Search Albums
        val urlStr = "https://api.deezer.com/search/album?q=$encodedTerm&limit=5"
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 4000
            readTimeout = 4000
            setRequestProperty("User-Agent", "Mozilla/5.0")
        }

        if (conn.responseCode == 200) {
            val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
            val jsonObj = JSONObject(jsonStr)
            val data = jsonObj.optJSONArray("data") ?: org.json.JSONArray()

            for (i in 0 until data.length()) {
                val item = data.optJSONObject(i) ?: continue
                val xl = item.optString("cover_xl", "")
                val big = item.optString("cover_big", "")
                val artUrl = xl.ifBlank { big }
                if (artUrl.isNotBlank()) {
                    list.add(ArtworkCandidate(artUrl, 1000, 1000, source = "Deezer"))
                }
            }
        }
        conn.disconnect()

        // 2. If album is blank, query Artist search API for portrait
        if (album.isBlank() && artist.isNotBlank()) {
            try {
                val artistUrlStr = "https://api.deezer.com/search/artist?q=$encodedTerm&limit=3"
                val artistConn = (URL(artistUrlStr).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 4000
                    readTimeout = 4000
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                }
                if (artistConn.responseCode == 200) {
                    val jsonStr = artistConn.inputStream.bufferedReader().use { it.readText() }
                    val jsonObj = JSONObject(jsonStr)
                    val data = jsonObj.optJSONArray("data") ?: org.json.JSONArray()
                    for (i in 0 until data.length()) {
                        val item = data.optJSONObject(i) ?: continue
                        val xl = item.optString("picture_xl", "")
                        val big = item.optString("picture_big", "")
                        val portraitUrl = xl.ifBlank { big }
                        if (portraitUrl.isNotBlank()) {
                            list.add(ArtworkCandidate(portraitUrl, 1000, 1000, source = "Deezer"))
                        }
                    }
                }
                artistConn.disconnect()
            } catch (ignored: Exception) {}
        }

        return list
    }

    private fun queryItunesAlbumArt(
        artist: String,
        album: String,
        useQuotes: Boolean,
        entityFilter: Boolean
    ): List<ArtworkCandidate> {
        val list = mutableListOf<ArtworkCandidate>()
        val term = when {
            artist.isBlank() -> if (useQuotes) "\"$album\"" else album
            album.isBlank() -> if (useQuotes) "\"$artist\"" else artist
            useQuotes -> "\"$artist\" \"$album\""
            else -> "$artist $album"
        }
        val encodedTerm = URLEncoder.encode(term, "UTF-8")
        val entityParam = when {
            album.isBlank() -> ""
            entityFilter -> "&entity=album"
            else -> ""
        }
        val urlStr = "https://itunes.apple.com/search?term=$encodedTerm$entityParam&limit=5"

        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 4000
            readTimeout = 4000
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
                    list.add(ArtworkCandidate(highResUrl, 1000, 1000, source = "iTunes"))
                }
            }
        }
        conn.disconnect()
        return list
    }

    private fun queryCoverArtArchive(artist: String, album: String): List<ArtworkCandidate> {
        val list = mutableListOf<ArtworkCandidate>()
        val query = "artist:\"$artist\" AND release:\"$album\""
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val mbUrlStr = "https://musicbrainz.org/ws/2/release-group/?query=$encodedQuery&fmt=json&limit=3"

        val conn = (URL(mbUrlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 4000
            readTimeout = 4000
            setRequestProperty("User-Agent", "TravelingTunes/1.0 (android-app)")
        }

        if (conn.responseCode == 200) {
            val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
            val jsonObj = JSONObject(jsonStr)
            val releaseGroups = jsonObj.optJSONArray("release-groups") ?: return list

            for (i in 0 until releaseGroups.length()) {
                val group = releaseGroups.optJSONObject(i) ?: continue
                val mbid = group.optString("id", "")
                if (mbid.isNotBlank()) {
                    val caaUrl = "https://coverartarchive.org/release-group/$mbid"
                    val caaConn = (URL(caaUrl).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 3000
                        readTimeout = 3000
                        setRequestProperty("User-Agent", "TravelingTunes/1.0 (android-app)")
                    }
                    if (caaConn.responseCode == 200) {
                        val caaJsonStr = caaConn.inputStream.bufferedReader().use { it.readText() }
                        val caaJson = JSONObject(caaJsonStr)
                        val images = caaJson.optJSONArray("images") ?: continue
                        for (j in 0 until images.length()) {
                            val img = images.optJSONObject(j) ?: continue
                            val imgUrl = img.optString("image", "")
                            if (imgUrl.isNotBlank()) {
                                list.add(ArtworkCandidate(imgUrl, 1000, 1000, source = "Cover Art Archive"))
                            }
                        }
                    }
                    caaConn.disconnect()
                }
            }
        }
        conn.disconnect()
        return list
    }

    private fun queryFreeImageSearchEngine(artist: String, album: String): List<ArtworkCandidate> {
        val list = mutableListOf<ArtworkCandidate>()
        val queryStr = when {
            album.isBlank() -> if (artist.isNotBlank()) "\"$artist\" artist portrait photo" else return list
            artist.isNotBlank() -> "\"$artist\" \"$album\" album cover art"
            else -> "\"$album\" album cover art"
        }
        val encodedQuery = URLEncoder.encode(queryStr, "UTF-8")

        val urlStr = "https://html.duckduckgo.com/html/?q=$encodedQuery"
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 4000
            readTimeout = 4000
            setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
        }

        if (conn.responseCode == 200) {
            val html = conn.inputStream.bufferedReader().use { it.readText() }
            val imgRegex = Regex("""(?:src|href)=["'](https?://[^"']+\.(?:jpg|jpeg|png))["']""", RegexOption.IGNORE_CASE)
            val matches = imgRegex.findAll(html)

            for (match in matches.take(5)) {
                val imgUrl = match.groupValues[1]
                if (imgUrl.contains("duckduckgo.com") && !imgUrl.contains("external-content")) continue

                val bounds = probeImageDimensions(imgUrl)
                if (bounds != null && bounds.first > 0 && bounds.second > 0) {
                    list.add(ArtworkCandidate(imgUrl, bounds.first, bounds.second, source = "Web Search"))
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
            val downloadedDir = File(context.filesDir, "downloaded_art").apply { mkdirs() }
            val hashKey = hashString("$artist-$album")
            val artFile = File(downloadedDir, "art_downloaded_$hashKey.jpg")

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

    suspend fun searchCandidatesWithQuery(
        query: String,
        enabledEngines: Set<SearchEngine> = SearchEngine.entries.toSet()
    ): List<ArtworkCandidate> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val sanitized = sanitizeMetadata(query)
        val candidates = fetchCandidatesInParallel(
            cleanArtist = "",
            cleanAlbum = sanitized,
            useQuotes = false,
            searchMode = SearchMode.ALBUM_ONLY,
            enabledEngines = enabledEngines
        )
        candidates.sortedWith(
            compareByDescending<ArtworkCandidate> { it.squareness }
                .thenByDescending { it.resolution }
        )
    }

    suspend fun copyArtworkToAlbums(
        sourceUri: Uri,
        targetAlbums: List<Pair<String, String>>
    ): Int = withContext(Dispatchers.IO) {
        if (targetAlbums.isEmpty()) return@withContext 0
        val imageBytes = try {
            if (sourceUri.scheme == "file") {
                File(sourceUri.path ?: "").readBytes()
            } else {
                context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
            }
        } catch (e: Exception) {
            null
        }
        if (imageBytes == null || imageBytes.isEmpty()) return@withContext 0

        var count = 0
        for ((album, artist) in targetAlbums) {
            val songs = musicDatabase.getSongsByAlbumAndArtist(album, artist)
            val uri = saveCustomArtworkForAlbum(album, artist, imageBytes, songs)
            if (uri != null) count++
        }
        count
    }

    suspend fun applyCandidateToAlbum(
        candidate: ArtworkCandidate,
        artist: String,
        album: String,
        songs: List<Song>
    ): Uri? = withContext(Dispatchers.IO) {
        val downloadedDir = File(context.filesDir, "downloaded_art").apply { mkdirs() }
        val downloadedFile = downloadImageToFile(candidate.url, artist, album, downloadedDir) ?: return@withContext null
        val artworkUri = Uri.fromFile(downloadedFile)

        musicDatabase.updateAlbumArtwork(album, artist, artworkUri)
        val bitmap = loadSongArtworkFromUri(context, artworkUri)
        if (bitmap != null) {
            val imageBitmap = bitmap.asImageBitmap()
            for (song in songs) {
                AlbumArtCache.instance.put(song.id, imageBitmap)
            }
        }
        artworkUri
    }

    suspend fun saveCustomArtworkForAlbum(
        album: String,
        artist: String,
        imageBytes: ByteArray,
        songs: List<Song>
    ): Uri? = withContext(Dispatchers.IO) {
        if (imageBytes.isEmpty()) return@withContext null
        val downloadedDir = File(context.filesDir, "downloaded_art").apply { mkdirs() }
        val hashKey = hashString("$artist-$album")
        val artFile = File(downloadedDir, "art_downloaded_$hashKey.jpg")

        try {
            FileOutputStream(artFile).use { fos ->
                fos.write(imageBytes)
            }
            if (!artFile.exists() || artFile.length() == 0L) return@withContext null

            val artworkUri = Uri.fromFile(artFile)
            musicDatabase.updateAlbumArtwork(album, artist, artworkUri)

            val bitmap = loadSongArtworkFromUri(context, artworkUri)
            if (bitmap != null) {
                val imageBitmap = bitmap.asImageBitmap()
                for (song in songs) {
                    AlbumArtCache.instance.put(song.id, imageBitmap)
                }
            }
            artworkUri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun deleteDownloadedArtworkForAlbum(
        album: String,
        artist: String,
        songs: List<Song>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Find current downloaded art file and delete if present
            val downloadedDir = File(context.filesDir, "downloaded_art")
            val hashKey = hashString("$artist-$album")
            val artFile = File(downloadedDir, "art_downloaded_$hashKey.jpg")
            if (artFile.exists()) {
                artFile.delete()
            }

            // Clear DB artwork URI for album
            musicDatabase.clearAlbumArtwork(album, artist)

            // Check if songs in album have embedded ID3 picture
            var embeddedUri: Uri? = null
            val embeddedDir = File(context.cacheDir, "embedded_art").apply { mkdirs() }
            val embeddedFile = File(embeddedDir, "art_embedded_$hashKey.jpg")

            for (song in songs) {
                val mmr = android.media.MediaMetadataRetriever()
                try {
                    mmr.setDataSource(context, song.contentUri)
                    val bytes = mmr.embeddedPicture
                    if (bytes != null) {
                        if (!embeddedFile.exists()) {
                            FileOutputStream(embeddedFile).use { fos -> fos.write(bytes) }
                        }
                        if (embeddedFile.exists() && embeddedFile.length() > 0) {
                            embeddedUri = Uri.fromFile(embeddedFile)
                            break
                        }
                    }
                } catch (_: Exception) {
                } finally {
                    try { mmr.release() } catch (_: Exception) {}
                }
            }

            if (embeddedUri != null) {
                musicDatabase.updateAlbumArtwork(album, artist, embeddedUri)
            }

            // Evict songs from AlbumArtCache
            for (song in songs) {
                if (embeddedUri != null) {
                    val bitmap = loadSongArtworkFromUri(context, embeddedUri)
                    if (bitmap != null) {
                        AlbumArtCache.instance.put(song.id, bitmap.asImageBitmap())
                    }
                } else {
                    // Force refresh or remove from cache
                    val dummy = loadSongArtworkFromUri(context, Uri.EMPTY)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun isDownloadedArtwork(song: Song?): Boolean {
        if (song == null) return false
        val uri = song.artworkUri ?: return false
        val uriStr = uri.toString()
        if (uriStr.isBlank()) return false

        if (uriStr.contains("downloaded_art") || uriStr.contains("art_downloaded") || uriStr.contains("art_custom")) {
            return true
        }
        if (uriStr.contains("art_embedded")) {
            return false
        }

        // Fallback for file URI: if file exists and song has NO embedded picture, it was downloaded online
        if (uri.scheme == "file") {
            val file = File(uri.path ?: "")
            if (file.exists() && file.length() > 0) {
                val mmr = android.media.MediaMetadataRetriever()
                return try {
                    mmr.setDataSource(context, song.contentUri)
                    val bytes = mmr.embeddedPicture
                    bytes == null
                } catch (_: Exception) {
                    true
                } finally {
                    try { mmr.release() } catch (_: Exception) {}
                }
            }
        }
        return false
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
