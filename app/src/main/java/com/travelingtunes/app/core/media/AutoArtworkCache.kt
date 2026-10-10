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
            val downloadedFile = AlbumArtDownloader.getDownloadedArtworkFile(context, song.artist, song.album)
            if (downloadedFile != null && downloadedFile.exists() && downloadedFile.length() > 0L) {
                return downloadedFile.readBytes()
            }

            val uri = song.artworkUri
            if (uri != null) {
                val bytes = if (uri.scheme == "file" && uri.path != null) {
                    val file = File(uri.path!!)
                    if (file.exists() && file.length() > 0L) file.readBytes() else null
                } else {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }
                if (bytes != null && bytes.isNotEmpty()) return bytes
            }

            val mmr = android.media.MediaMetadataRetriever()
            return try {
                mmr.setDataSource(context, song.contentUri)
                mmr.embeddedPicture
            } catch (_: Exception) {
                null
            } finally {
                try { mmr.release() } catch (_: Exception) {}
            }
        } catch (_: Exception) {
            return null
        }
    }
}
