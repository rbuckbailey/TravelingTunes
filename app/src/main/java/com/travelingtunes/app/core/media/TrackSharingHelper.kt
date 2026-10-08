package com.travelingtunes.app.core.media

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.travelingtunes.app.core.model.Song
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object TrackSharingHelper {

    fun formatTrackText(song: Song): String {
        return if (song.album.isNotBlank()) {
            "\"${song.title}\" by ${song.artist} (${song.album})"
        } else {
            "\"${song.title}\" by ${song.artist}"
        }
    }

    fun formatAlbumText(albumName: String, artistName: String, albumSongs: List<Song>): String {
        val header = "Album: \"$albumName\" by $artistName"
        if (albumSongs.isEmpty()) return header
        val tracklist = albumSongs.mapIndexed { idx, song ->
            val num = if (song.trackNumber > 0) song.trackNumber else (idx + 1)
            "$num. ${song.title}"
        }.joinToString("\n")
        return "$header\n\nTracklist:\n$tracklist"
    }

    fun shareTrackText(context: Context, song: Song) {
        val text = formatTrackText(song)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val chooserIntent = Intent.createChooser(sendIntent, "Share Track Info").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
    }

    fun shareAlbumText(context: Context, albumName: String, artistName: String, albumSongs: List<Song>) {
        val text = formatAlbumText(albumName, artistName, albumSongs)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val chooserIntent = Intent.createChooser(sendIntent, "Share Album Info").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
    }

    fun shareTrackFile(context: Context, song: Song) {
        try {
            val audioFile = resolveAudioFile(context, song) ?: run {
                shareTrackText(context, song)
                return
            }
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, audioFile)

            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "audio/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooserIntent = Intent.createChooser(sendIntent, "Share Track File").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            shareTrackText(context, song)
        }
    }

    fun shareAlbumFilesZip(context: Context, albumName: String, artistName: String, albumSongs: List<Song>) {
        if (albumSongs.isEmpty()) return
        try {
            val zipFile = createAlbumZipArchive(context, albumName, artistName, albumSongs)
            if (zipFile == null || !zipFile.exists() || zipFile.length() == 0L) {
                shareAlbumText(context, albumName, artistName, albumSongs)
                return
            }

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, zipFile)

            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "application/zip"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooserIntent = Intent.createChooser(sendIntent, "Share Album ZIP").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            shareAlbumText(context, albumName, artistName, albumSongs)
        }
    }

    fun createAlbumZipArchive(
        context: Context,
        albumName: String,
        artistName: String,
        albumSongs: List<Song>
    ): File? {
        val sanitizedAlbum = albumName.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "Album" }
        val sanitizedArtist = artistName.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "Artist" }
        val zipDir = File(context.cacheDir, "shared_albums").apply { mkdirs() }
        val zipFile = File(zipDir, "${sanitizedArtist}_${sanitizedAlbum}.zip")

        try {
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                val usedNames = mutableSetOf<String>()

                for ((idx, song) in albumSongs.withIndex()) {
                    val audioFile = resolveAudioFile(context, song) ?: continue
                    val trackNumStr = if (song.trackNumber > 0) "%02d".format(song.trackNumber) else "%02d".format(idx + 1)
                    val ext = audioFile.extension.ifBlank { "mp3" }
                    val cleanTitle = song.title.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "Track_$idx" }
                    var entryName = "$trackNumStr - $cleanTitle.$ext"

                    var dupeCounter = 1
                    while (usedNames.contains(entryName)) {
                        entryName = "$trackNumStr - $cleanTitle ($dupeCounter).$ext"
                        dupeCounter++
                    }
                    usedNames.add(entryName)

                    zos.putNextEntry(ZipEntry(entryName))
                    audioFile.inputStream().use { input ->
                        input.copyTo(zos)
                    }
                    zos.closeEntry()
                }

                // Add downloaded artwork image if present
                val artFile = AlbumArtDownloader.getDownloadedArtworkFile(context, artistName, albumName)
                if (artFile != null && artFile.exists() && artFile.length() > 0) {
                    val artExt = artFile.extension.ifBlank { "jpg" }
                    zos.putNextEntry(ZipEntry("cover.$artExt"))
                    artFile.inputStream().use { input ->
                        input.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }
            return zipFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun resolveAudioFile(context: Context, song: Song): File? {
        val uri = song.contentUri
        if (uri.scheme == "file" && uri.path != null) {
            val f = File(uri.path!!)
            if (f.exists() && f.length() > 0) return f
        }

        val shareDir = File(context.cacheDir, "shared_tracks").apply { mkdirs() }
        val ext = song.fileName.substringAfterLast('.', "mp3")
        val tempFile = File(shareDir, "song_${song.id}.$ext")

        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (tempFile.exists() && tempFile.length() > 0) {
                return tempFile
            }
        } catch (_: Exception) {}

        return null
    }
}
