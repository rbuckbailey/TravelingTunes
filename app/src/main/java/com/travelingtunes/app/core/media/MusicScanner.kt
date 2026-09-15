package com.travelingtunes.app.core.media

import android.content.Context
import android.database.ContentObserver
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.graphics.asImageBitmap
import androidx.documentfile.provider.DocumentFile
import com.travelingtunes.app.core.database.MusicDatabase
import com.travelingtunes.app.core.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

class MusicScanner(
    private val context: Context,
    private val musicDatabase: MusicDatabase
) {

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scannedCount = MutableStateFlow(0)
    val scannedCount: StateFlow<Int> = _scannedCount.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val albumArtDownloader = AlbumArtDownloader(context, musicDatabase)
    val cddbManager = CddbManager(context, musicDatabase)

    val isDownloadingArt: StateFlow<Boolean> = albumArtDownloader.isDownloading
    val artDownloadStatusMessage: StateFlow<String?> = albumArtDownloader.statusMessage
    val artDownloadDownloadedCount: StateFlow<Int> = albumArtDownloader.downloadedCount
    val artDownloadFailedCount: StateFlow<Int> = albumArtDownloader.failedCount
    val artDownloadTotalCount: StateFlow<Int> = albumArtDownloader.totalToDownload
    val lastAuditReport: StateFlow<AlbumArtAuditReport?> = albumArtDownloader.lastAuditReport

    val isEmbeddingCddb: StateFlow<Boolean> = cddbManager.isEmbeddingCddb
    val cddbStatusMessage: StateFlow<String?> = cddbManager.embeddingCddbStatusMessage

    private val _isAnalyzingVolume = MutableStateFlow(false)
    val isAnalyzingVolume: StateFlow<Boolean> = _isAnalyzingVolume.asStateFlow()

    private val _volumeAnalysisStatusMessage = MutableStateFlow<String?>(null)
    val volumeAnalysisStatusMessage: StateFlow<String?> = _volumeAnalysisStatusMessage.asStateFlow()

    private val _volumeAnalysisProgressCurrent = MutableStateFlow(0)
    val volumeAnalysisProgressCurrent: StateFlow<Int> = _volumeAnalysisProgressCurrent.asStateFlow()

    private val _volumeAnalysisProgressTotal = MutableStateFlow(0)
    val volumeAnalysisProgressTotal: StateFlow<Int> = _volumeAnalysisProgressTotal.asStateFlow()

    private val _cachedDuplicatePairs = MutableStateFlow<List<DuplicateMatchPair>?>(null)
    val cachedDuplicatePairs: StateFlow<List<DuplicateMatchPair>?> = _cachedDuplicatePairs.asStateFlow()

    private val _isAnalyzingDuplicates = MutableStateFlow(false)
    val isAnalyzingDuplicates: StateFlow<Boolean> = _isAnalyzingDuplicates.asStateFlow()

    val duplicateScanProgressCurrent = MutableStateFlow(0)
    val duplicateScanProgressTotal = MutableStateFlow(0)

    suspend fun getOrScanDuplicates(
        musicFolderName: String? = null,
        forceRescan: Boolean = false
    ): List<DuplicateMatchPair> = BackgroundTaskGate.runAsBackgroundTask {
        if (!forceRescan && _cachedDuplicatePairs.value != null) {
            return@runAsBackgroundTask _cachedDuplicatePairs.value!!
        }
        if (_isAnalyzingDuplicates.value) {
            return@runAsBackgroundTask _cachedDuplicatePairs.value ?: emptyList()
        }

        _isAnalyzingDuplicates.value = true
        val songs = musicDatabase.getAllSongs()
        val pairs = DuplicateTrackFinder.findDuplicates(
            context = context,
            songs = songs,
            musicFolderName = musicFolderName,
            onProgress = { current, total ->
                duplicateScanProgressCurrent.value = current
                duplicateScanProgressTotal.value = total
            }
        )
        _cachedDuplicatePairs.value = pairs
        _isAnalyzingDuplicates.value = false
        pairs
    }

    fun removeSongFromDuplicateCache(songId: Long) {
        val current = _cachedDuplicatePairs.value ?: return
        val updated = current.filter { pair ->
            pair.trackA.song.id != songId && pair.trackB.song.id != songId
        }
        _cachedDuplicatePairs.value = updated
    }

    fun clearDuplicateCache() {
        _cachedDuplicatePairs.value = null
    }

    suspend fun getCddbOverridesCount(): Int = cddbManager.getCddbOverridesCount()
    suspend fun embedCddbOverrides(): Pair<Int, Int> = cddbManager.embedAllCddbOverrides()

    suspend fun downloadMissingArtwork(): Int = albumArtDownloader.downloadMissingArtwork()
    fun cancelDownloadArt() = albumArtDownloader.cancelDownload()

    suspend fun analyzeLibraryVolumeLevels(): Pair<Int, Int> = withContext(Dispatchers.IO) {
        if (_isAnalyzingVolume.value) return@withContext Pair(0, 0)
        _isAnalyzingVolume.value = true
        _volumeAnalysisStatusMessage.value = "Starting volume level analysis..."
        _volumeAnalysisProgressCurrent.value = 0
        _volumeAnalysisProgressTotal.value = 0

        val (succ, fail) = AudioVolumeAnalyzer.analyzeAllSongsInDatabase(
            context = context,
            database = musicDatabase,
            onProgress = { current, total, status ->
                _volumeAnalysisProgressCurrent.value = current
                _volumeAnalysisProgressTotal.value = total
                _volumeAnalysisStatusMessage.value = status
            }
        )

        val summary = "Analyzed volume for $succ songs ($fail failed)."
        _volumeAnalysisStatusMessage.value = summary
        _isAnalyzingVolume.value = false
        Pair(succ, fail)
    }

    private val _isAutoRescanWaiting = MutableStateFlow(false)
    val isAutoRescanWaiting: StateFlow<Boolean> = _isAutoRescanWaiting.asStateFlow()

    private val _autoRescanStatusMessage = MutableStateFlow<String?>(null)
    val autoRescanStatusMessage: StateFlow<String?> = _autoRescanStatusMessage.asStateFlow()

    @Volatile
    private var contentObserver: ContentObserver? = null
    private var autoRescanJob: Job? = null
    private var lastChangeTimestamp = 0L

    fun startAutoRescanWatcher(
        treeUri: Uri,
        coroutineScope: CoroutineScope,
        debounceMs: Long = 3000L,
        onScanComplete: suspend () -> Unit = {}
    ) {
        stopAutoRescanWatcher()

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                onFolderChangeDetected(treeUri, coroutineScope, debounceMs, onScanComplete)
            }
        }

        try {
            context.contentResolver.registerContentObserver(treeUri, true, observer)
            context.contentResolver.registerContentObserver(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )
            contentObserver = observer
            _autoRescanStatusMessage.value = "Monitoring folder for changes..."
        } catch (e: Exception) {
            e.printStackTrace()
            _autoRescanStatusMessage.value = "Auto-rescan setup failed: ${e.localizedMessage}"
        }
    }

    fun stopAutoRescanWatcher() {
        contentObserver?.let {
            try {
                context.contentResolver.unregisterContentObserver(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        contentObserver = null
        autoRescanJob?.cancel()
        autoRescanJob = null
        _isAutoRescanWaiting.value = false
        _autoRescanStatusMessage.value = null
    }

    private fun onFolderChangeDetected(
        treeUri: Uri,
        coroutineScope: CoroutineScope,
        debounceMs: Long,
        onScanComplete: suspend () -> Unit
    ) {
        lastChangeTimestamp = System.currentTimeMillis()
        _isAutoRescanWaiting.value = true
        _autoRescanStatusMessage.value = "File changes detected. Waiting for folder to stabilize..."

        autoRescanJob?.cancel()
        autoRescanJob = coroutineScope.launch(Dispatchers.IO) {
            while (isActive) {
                val timeSinceLastChange = System.currentTimeMillis() - lastChangeTimestamp
                if (timeSinceLastChange >= debounceMs) {
                    break
                }
                delay(debounceMs - timeSinceLastChange)
            }

            if (!isActive) return@launch

            _autoRescanStatusMessage.value = "Checking folder stability..."
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
            val initialCount = rootDoc?.listFiles()?.size ?: 0
            delay(500L)
            val secondCount = rootDoc?.listFiles()?.size ?: 0

            if (initialCount != secondCount) {
                onFolderChangeDetected(treeUri, coroutineScope, debounceMs, onScanComplete)
                return@launch
            }

            while (_isScanning.value && isActive) {
                delay(500L)
            }

            if (!isActive) return@launch

            _autoRescanStatusMessage.value = "Folder stable. Auto-rescanning library..."
            scanFolder(treeUri)
            _isAutoRescanWaiting.value = false
            _autoRescanStatusMessage.value = "Auto-scan complete"
            onScanComplete()
        }
    }

    private val _isEmbeddingArt = MutableStateFlow(false)
    val isEmbeddingArt: StateFlow<Boolean> = _isEmbeddingArt.asStateFlow()

    private val _embeddingStatusMessage = MutableStateFlow<String?>(null)
    val embeddingStatusMessage: StateFlow<String?> = _embeddingStatusMessage.asStateFlow()

    private val _embeddingProgressCurrent = MutableStateFlow(0)
    val embeddingProgressCurrent: StateFlow<Int> = _embeddingProgressCurrent.asStateFlow()

    private val _embeddingProgressTotal = MutableStateFlow(0)
    val embeddingProgressTotal: StateFlow<Int> = _embeddingProgressTotal.asStateFlow()

    private val _embeddingResultSummary = MutableStateFlow<String?>(null)
    val embeddingResultSummary: StateFlow<String?> = _embeddingResultSummary.asStateFlow()

    @Volatile
    private var isEmbeddingCancelled = false
    private var activeEmbeddingJob: kotlinx.coroutines.Job? = null

    fun cancelEmbedding() {
        isEmbeddingCancelled = true
        activeEmbeddingJob?.cancel()
        _isEmbeddingArt.value = false
        _embeddingStatusMessage.value = "ID3 artwork embedding cancelled"
    }

    suspend fun embedArtworkInBackground(
        targets: List<Pair<String, String>>,
        artworkUris: Map<Pair<String, String>, Uri?> = emptyMap(),
        playbackManager: PlaybackManager? = null
    ): Pair<Int, Int> = BackgroundTaskGate.runAsBackgroundTask {
        if (_isEmbeddingArt.value || targets.isEmpty()) return@runAsBackgroundTask Pair(0, 0)
        _isEmbeddingArt.value = true
        isEmbeddingCancelled = false

        _embeddingProgressCurrent.value = 0
        _embeddingProgressTotal.value = targets.size
        _embeddingStatusMessage.value = "Starting ID3 artwork embedding..."
        _embeddingResultSummary.value = null

        var totalEmbedded = 0
        var totalFailed = 0

        for ((index, pair) in targets.withIndex()) {
            BackgroundTaskGate.checkYieldAndPause()
            if (isEmbeddingCancelled) {
                _embeddingStatusMessage.value = "Embedding stopped ($totalEmbedded songs updated)"
                break
            }

            val (album, artist) = pair
            _embeddingProgressCurrent.value = index + 1
            _embeddingStatusMessage.value = "Embedding artwork: \"$album\" (${index + 1}/${targets.size})"

            val songs = musicDatabase.getSongsByAlbumAndArtist(album, artist)
            val uri = artworkUris[pair] ?: songs.firstOrNull()?.artworkUri
            if (uri != null) {
                val (succ, fail) = Id3ArtworkEmbedder.embedArtworkIntoAlbum(context, songs, uri)
                totalEmbedded += succ
                totalFailed += fail

                if (succ > 0) {
                    try {
                        val hashKey = hashString("$artist-$album")
                        val embeddedDir = File(context.cacheDir, "embedded_art").apply { mkdirs() }
                        val embeddedFile = File(embeddedDir, "art_embedded_$hashKey.jpg")

                        val bytes = try {
                            if (uri.scheme == "file") {
                                File(uri.path ?: "").readBytes()
                            } else {
                                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            }
                        } catch (_: Exception) {
                            null
                        }

                        if (bytes != null && bytes.isNotEmpty()) {
                            FileOutputStream(embeddedFile).use { fos -> fos.write(bytes) }
                            if (embeddedFile.exists() && embeddedFile.length() > 0) {
                                val embeddedUri = Uri.fromFile(embeddedFile)
                                musicDatabase.updateAlbumArtwork(album, artist, embeddedUri)

                                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                if (bitmap != null) {
                                    val imgBmp = bitmap.asImageBitmap()
                                    for (song in songs) {
                                        AlbumArtCache.instance.put(song.id, imgBmp)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                totalFailed += songs.size
            }
        }

        val summary = "Embedded artwork into $totalEmbedded tracks across ${targets.size} albums ($totalFailed failed)."
        _embeddingResultSummary.value = summary
        _embeddingStatusMessage.value = summary
        _isEmbeddingArt.value = false
        playbackManager?.refreshCurrentSongArtwork()
        Pair(totalEmbedded, totalFailed)
    }

    fun clearEmbeddingResultSummary() {
        _embeddingResultSummary.value = null
    }

    private val supportedExtensions = setOf("mp3", "m4a", "flac", "wav", "aac", "ogg", "opus", "wma")

    suspend fun scanFolder(treeUri: Uri) = BackgroundTaskGate.runAsBackgroundTask {
        if (_isScanning.value) return@runAsBackgroundTask
        _isScanning.value = true
        _scannedCount.value = 0
        _statusMessage.value = "Scanning library folder..."

        val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
        if (rootDoc == null || !rootDoc.canRead()) {
            _statusMessage.value = "Cannot read selected folder"
            _isScanning.value = false
            return@runAsBackgroundTask
        }

        val foundSongs = mutableListOf<Song>()
        val artworkCacheDir = File(context.cacheDir, "album_art").apply { mkdirs() }

        traverseDocumentTree(
            rootDir = rootDoc,
            currentDir = rootDoc,
            relativePath = "",
            foundSongs = foundSongs,
            artworkCacheDir = artworkCacheDir
        )

        musicDatabase.clearDatabase()
        clearDuplicateCache()
        if (foundSongs.isNotEmpty()) {
            musicDatabase.insertOrReplaceSongs(foundSongs)
            _statusMessage.value = "Scanned ${foundSongs.size} songs successfully. Analyzing volume levels..."
            analyzeLibraryVolumeLevels()
            _statusMessage.value = "Scanned ${foundSongs.size} songs successfully"
        } else {
            _statusMessage.value = "No audio files found in selected folder"
        }

        _isScanning.value = false
    }

    private suspend fun traverseDocumentTree(
        rootDir: DocumentFile,
        currentDir: DocumentFile,
        relativePath: String,
        foundSongs: MutableList<Song>,
        artworkCacheDir: File
    ) {
        val files = currentDir.listFiles()
        for (file in files) {
            BackgroundTaskGate.checkYieldAndPause()
            if (file.isDirectory) {
                val subFolder = if (relativePath.isEmpty()) file.name.orEmpty() else "$relativePath/${file.name}"
                traverseDocumentTree(rootDir, file, subFolder, foundSongs, artworkCacheDir)
            } else if (file.isFile) {
                val name = file.name.orEmpty()
                val ext = name.substringAfterLast('.', "").lowercase()
                if (supportedExtensions.contains(ext)) {
                    val song = processAudioFile(
                        file = file,
                        currentDir = currentDir,
                        relativePath = relativePath,
                        artworkCacheDir = artworkCacheDir,
                        idSeed = foundSongs.size + 1L
                    )
                    if (song != null) {
                        foundSongs.add(song)
                        _scannedCount.value = foundSongs.size
                        _statusMessage.value = "Indexed: ${song.title}"
                    }
                }
            }
        }
    }

    private fun processAudioFile(
        file: DocumentFile,
        currentDir: DocumentFile,
        relativePath: String,
        artworkCacheDir: File,
        idSeed: Long
    ): Song? {
        val contentUri = file.uri
        val fileName = file.name ?: "Unknown"
        val cleanFileName = fileName.substringBeforeLast('.').ifBlank { fileName }
        val folderName = when {
            relativePath.isNotBlank() -> relativePath.substringAfterLast('/')
            !currentDir.name.isNullOrBlank() -> currentDir.name!!
            else -> "Music"
        }
        val mmr = MediaMetadataRetriever()

        return try {
            mmr.setDataSource(context, contentUri)

            val rawTitle = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.trim()
            val rawArtist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.trim()
            val rawAlbum = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.trim()
            val rawGenre = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)?.trim()

            val title = if (!rawTitle.isNullOrBlank() && !rawTitle.equals("Unknown", ignoreCase = true) && !rawTitle.equals("Unknown Title", ignoreCase = true) && !rawTitle.equals("<unknown>", ignoreCase = true)) {
                rawTitle
            } else {
                cleanFileName
            }

            val album = if (!rawAlbum.isNullOrBlank() && !rawAlbum.equals("Unknown", ignoreCase = true) && !rawAlbum.equals("Unknown Album", ignoreCase = true) && !rawAlbum.equals("<unknown>", ignoreCase = true)) {
                rawAlbum
            } else {
                folderName
            }

            val artist = if (!rawArtist.isNullOrBlank() && !rawArtist.equals("Unknown", ignoreCase = true) && !rawArtist.equals("Unknown Artist", ignoreCase = true) && !rawArtist.equals("<unknown>", ignoreCase = true)) {
                rawArtist
            } else {
                folderName
            }

            val genre = if (!rawGenre.isNullOrBlank() && !rawGenre.equals("Unknown", ignoreCase = true) && !rawGenre.equals("Unknown Genre", ignoreCase = true)) rawGenre else "Unknown Genre"
            val durationMs = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val trackStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
            val trackNumber = trackStr?.substringBefore('/')?.trim()?.toIntOrNull() ?: 0
            val discStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)
            val discNumber = discStr?.substringBefore('/')?.trim()?.toIntOrNull() ?: 0
            val yearStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
            val year = yearStr?.trim()?.toIntOrNull() ?: 0

            val artworkUri = extractAndSaveArtwork(mmr, album, artist, artworkCacheDir)

            // Generate stable numeric ID from Uri string
            val songId = kotlin.math.abs(contentUri.toString().hashCode().toLong())

            Song(
                id = if (songId != 0L) songId else idSeed,
                title = title,
                artist = artist,
                album = album,
                albumId = album.hashCode().toLong(),
                durationMs = durationMs,
                contentUri = contentUri,
                artworkUri = artworkUri,
                userRating = 0,
                genre = genre,
                folderPath = relativePath,
                fileName = fileName,
                trackNumber = trackNumber,
                discNumber = discNumber,
                year = year
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback Song
            Song(
                id = kotlin.math.abs(contentUri.toString().hashCode().toLong()),
                title = cleanFileName,
                artist = folderName,
                album = folderName,
                albumId = folderName.hashCode().toLong(),
                durationMs = 0L,
                contentUri = contentUri,
                artworkUri = null,
                genre = "Unknown Genre",
                folderPath = relativePath,
                fileName = fileName
            )
        } finally {
            try {
                mmr.release()
            } catch (ignored: Exception) {}
        }
    }

    private fun extractAndSaveArtwork(
        mmr: MediaMetadataRetriever,
        album: String,
        artist: String,
        artworkCacheDir: File
    ): Uri? {
        val bytes = mmr.embeddedPicture ?: return null
        return try {
            val hashKey = hashString("$artist-$album")
            val artFile = File(artworkCacheDir, "art_$hashKey.jpg")
            if (!artFile.exists()) {
                FileOutputStream(artFile).use { fos ->
                    fos.write(bytes)
                }
            }
            Uri.fromFile(artFile)
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
