package com.travelingtunes.app.core.media

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import com.travelingtunes.app.core.model.Song
import com.travelingtunes.app.feature.player.decodeSampledBitmapFromByteArray
import java.io.ByteArrayOutputStream
import java.io.File

object AutoArtworkCache {

    fun getAutoArtwork(
        context: Context,
        song: Song,
        safeZoneRatio: Float,
        paddingModeStr: String
    ): Uri? {
        val ratio = safeZoneRatio.coerceIn(0.15f, 1.00f)
        val ratioPercent = (ratio * 100).toInt()
        val mode = when (paddingModeStr.uppercase()) {
            "STRETCH" -> ArtCropFillMode.STRETCH
            "BLUR" -> ArtCropFillMode.BLUR
            "ECHO" -> ArtCropFillMode.ECHO
            else -> ArtCropFillMode.FILL
        }

        val cacheKey = "song_${song.id}_${ratioPercent}_${mode.name}"
        val cacheDir = File(context.cacheDir, "auto_art").apply { mkdirs() }
        val cacheFile = File(cacheDir, "$cacheKey.jpg")

        if (cacheFile.exists() && cacheFile.length() > 0L) {
            val uri = getFileProviderUri(context, cacheFile)
            grantAutoUriPermissions(context, uri)
            return uri
        }

        val originalBytes = loadRawArtworkBytes(context, song) ?: return null
        val originalBitmap = decodeSampledBitmapFromByteArray(originalBytes, 800, 800) ?: return null

        val processedBitmap = ArtCropFillHelper.processAutoDashboardCanvas(
            original = originalBitmap,
            mode = mode,
            canvasDim = 800,
            safeZoneRatio = safeZoneRatio
        )

        val bos = ByteArrayOutputStream()
        processedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, bos)
        val resultBytes = bos.toByteArray()

        try {
            cacheFile.writeBytes(resultBytes)
        } catch (_: Exception) {}

        val uri = getFileProviderUri(context, cacheFile)
        grantAutoUriPermissions(context, uri)
        return uri
    }

    fun clearCache(context: Context) {
        try {
            val cacheDir = File(context.cacheDir, "auto_art")
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { it.delete() }
            }
        } catch (_: Exception) {}
    }

    private fun getFileProviderUri(context: Context, file: File): Uri {
        return try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Exception) {
            Uri.fromFile(file)
        }
    }

    private fun loadRawArtworkBytes(context: Context, song: Song): ByteArray? {
        try {
            // 1. Check downloaded artwork file
            val downloadedFile = AlbumArtDownloader.getDownloadedArtworkFile(context, song.artist, song.album)
            if (downloadedFile != null && downloadedFile.exists() && downloadedFile.length() > 0L) {
                return downloadedFile.readBytes()
            }

            // 2. Check song.artworkUri with isolated exception handling
            val uri = song.artworkUri
            if (uri != null) {
                val bytes = try {
                    if (uri.scheme == "file" && uri.path != null) {
                        val file = File(uri.path!!)
                        if (file.exists() && file.length() > 0L) file.readBytes() else null
                    } else {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    }
                } catch (_: Exception) {
                    null
                }
                if (bytes != null && bytes.isNotEmpty()) return bytes
            }

            // 3. Check cached artwork files by artist-album hash
            try {
                val md = java.security.MessageDigest.getInstance("MD5")
                val digest = md.digest("${song.artist}-${song.album}".toByteArray())
                val hashKey = digest.joinToString("") { "%02x".format(it) }

                val cacheArtFile = File(context.cacheDir, "album_art/art_$hashKey.jpg")
                val embeddedArtFile = File(context.cacheDir, "embedded_art/art_embedded_$hashKey.jpg")
                val downloadedArtFile = File(context.filesDir, "downloaded_art/art_downloaded_$hashKey.jpg")

                val targetFile = when {
                    downloadedArtFile.exists() && downloadedArtFile.length() > 0L -> downloadedArtFile
                    embeddedArtFile.exists() && embeddedArtFile.length() > 0L -> embeddedArtFile
                    cacheArtFile.exists() && cacheArtFile.length() > 0L -> cacheArtFile
                    else -> null
                }

                if (targetFile != null) {
                    val bytes = targetFile.readBytes()
                    if (bytes.isNotEmpty()) return bytes
                }
            } catch (_: Exception) {}

            // 4. Extract embedded picture using MediaMetadataRetriever
            val mmr = android.media.MediaMetadataRetriever()
            try {
                var rawBytes: ByteArray? = null
                try {
                    mmr.setDataSource(context, song.contentUri)
                    rawBytes = mmr.embeddedPicture
                } catch (_: Exception) {
                    try {
                        context.contentResolver.openFileDescriptor(song.contentUri, "r")?.use { pfd ->
                            mmr.setDataSource(pfd.fileDescriptor)
                            rawBytes = mmr.embeddedPicture
                        }
                    } catch (_: Exception) {}
                }
                if (rawBytes != null && rawBytes.isNotEmpty()) return rawBytes
            } finally {
                try { mmr.release() } catch (_: Exception) {}
            }

            // 5. Fallback: ContentResolver.loadThumbnail on Android 10+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                try {
                    val bitmap = context.contentResolver.loadThumbnail(song.contentUri, android.util.Size(800, 800), null)
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                    val bytes = stream.toByteArray()
                    if (bytes.isNotEmpty()) return bytes
                } catch (_: Exception) {}
            }

            return null
        } catch (_: Exception) {
            return null
        }
    }
}
