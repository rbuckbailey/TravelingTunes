package com.travelingtunes.app.core.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.travelingtunes.app.core.database.MusicDatabase
import com.travelingtunes.app.core.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val isDownloadingArt: StateFlow<Boolean> = albumArtDownloader.isDownloading
    val artDownloadStatusMessage: StateFlow<String?> = albumArtDownloader.statusMessage
    val artDownloadDownloadedCount: StateFlow<Int> = albumArtDownloader.downloadedCount
    val artDownloadFailedCount: StateFlow<Int> = albumArtDownloader.failedCount
    val artDownloadTotalCount: StateFlow<Int> = albumArtDownloader.totalToDownload
    val lastAuditReport: StateFlow<AlbumArtAuditReport?> = albumArtDownloader.lastAuditReport

    suspend fun downloadMissingArtwork(): Int = albumArtDownloader.downloadMissingArtwork()
    fun cancelDownloadArt() = albumArtDownloader.cancelDownload()

    private val supportedExtensions = setOf("mp3", "m4a", "flac", "wav", "aac", "ogg", "opus", "wma")

    suspend fun scanFolder(treeUri: Uri) = withContext(Dispatchers.IO) {
        if (_isScanning.value) return@withContext
        _isScanning.value = true
        _scannedCount.value = 0
        _statusMessage.value = "Scanning library folder..."

        val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
        if (rootDoc == null || !rootDoc.canRead()) {
            _statusMessage.value = "Cannot read selected folder"
            _isScanning.value = false
            return@withContext
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
        if (foundSongs.isNotEmpty()) {
            musicDatabase.insertOrReplaceSongs(foundSongs)
            _statusMessage.value = "Scanned ${foundSongs.size} songs successfully"
        } else {
            _statusMessage.value = "No audio files found in selected folder"
        }

        _isScanning.value = false
    }

    private fun traverseDocumentTree(
        rootDir: DocumentFile,
        currentDir: DocumentFile,
        relativePath: String,
        foundSongs: MutableList<Song>,
        artworkCacheDir: File
    ) {
        val files = currentDir.listFiles()
        for (file in files) {
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
            context.contentResolver.openFileDescriptor(contentUri, "r")?.use { pfd ->
                mmr.setDataSource(pfd.fileDescriptor)
            } ?: run {
                mmr.setDataSource(context, contentUri)
            }

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
