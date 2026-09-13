package com.travelingtunes.app.core.media

import android.content.Context
import com.travelingtunes.app.core.database.CddbOverrideRecord
import com.travelingtunes.app.core.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object Id3TagEmbedder {

    suspend fun embedCddbTagsIntoSong(
        context: Context,
        song: Song,
        override: CddbOverrideRecord
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val tempInFile = File(context.cacheDir, "temp_cddb_in_${song.id}.tmp")
            val tempOutFile = File(context.cacheDir, "temp_cddb_out_${song.id}.tmp")

            contentResolver.openInputStream(song.contentUri)?.use { input ->
                FileOutputStream(tempInFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext false

            if (!tempInFile.exists() || tempInFile.length() == 0L) {
                return@withContext false
            }

            val fileBytes = tempInFile.readBytes()
            val fileName = song.fileName.lowercase()

            val success = when {
                fileName.endsWith(".mp3") || isMp3Header(fileBytes) -> {
                    embedMp3Id3v2TextFrames(
                        audioBytes = fileBytes,
                        trackNumber = override.trackNumber,
                        discNumber = override.discNumber,
                        title = override.title.ifBlank { song.title },
                        outputFile = tempOutFile
                    )
                }
                else -> {
                    embedMp3Id3v2TextFrames(
                        audioBytes = fileBytes,
                        trackNumber = override.trackNumber,
                        discNumber = override.discNumber,
                        title = override.title.ifBlank { song.title },
                        outputFile = tempOutFile
                    )
                }
            }

            if (success && tempOutFile.exists() && tempOutFile.length() > 0) {
                try {
                    contentResolver.openOutputStream(song.contentUri, "rwt")?.use { out ->
                        tempOutFile.inputStream().use { inStream ->
                            inStream.copyTo(out)
                        }
                    }
                } catch (_: Exception) {
                    contentResolver.openOutputStream(song.contentUri, "w")?.use { out ->
                        tempOutFile.inputStream().use { inStream ->
                            inStream.copyTo(out)
                        }
                    }
                }
                tempInFile.delete()
                tempOutFile.delete()
                true
            } else {
                tempInFile.delete()
                tempOutFile.delete()
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun embedMetadataTagsIntoSong(
        context: Context,
        song: Song,
        genre: String? = null,
        title: String? = null,
        artist: String? = null,
        album: String? = null,
        year: Int? = null,
        trackNumber: Int? = null,
        discNumber: Int? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val tempInFile = File(context.cacheDir, "temp_meta_in_${song.id}.tmp")
            val tempOutFile = File(context.cacheDir, "temp_meta_out_${song.id}.tmp")

            contentResolver.openInputStream(song.contentUri)?.use { input ->
                FileOutputStream(tempInFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext false

            if (!tempInFile.exists() || tempInFile.length() == 0L) {
                return@withContext false
            }

            val fileBytes = tempInFile.readBytes()

            val success = embedMp3Id3v2FullTextFrames(
                audioBytes = fileBytes,
                genre = genre ?: song.genre,
                title = title ?: song.title,
                artist = artist ?: song.artist,
                album = album ?: song.album,
                year = year ?: song.year,
                trackNumber = trackNumber ?: song.trackNumber,
                discNumber = discNumber ?: song.discNumber,
                outputFile = tempOutFile
            )

            if (success && tempOutFile.exists() && tempOutFile.length() > 0) {
                try {
                    contentResolver.openOutputStream(song.contentUri, "rwt")?.use { out ->
                        tempOutFile.inputStream().use { inStream ->
                            inStream.copyTo(out)
                        }
                    }
                } catch (_: Exception) {
                    contentResolver.openOutputStream(song.contentUri, "w")?.use { out ->
                        tempOutFile.inputStream().use { inStream ->
                            inStream.copyTo(out)
                        }
                    }
                }
                tempInFile.delete()
                tempOutFile.delete()
                true
            } else {
                tempInFile.delete()
                tempOutFile.delete()
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun embedMp3Id3v2FullTextFrames(
        audioBytes: ByteArray,
        genre: String,
        title: String,
        artist: String,
        album: String,
        year: Int,
        trackNumber: Int,
        discNumber: Int,
        outputFile: File
    ): Boolean {
        return try {
            val framesStream = ByteArrayOutputStream()

            if (title.isNotBlank()) {
                framesStream.write(buildTextFrame("TIT2", title))
            }
            if (artist.isNotBlank()) {
                framesStream.write(buildTextFrame("TPE1", artist))
            }
            if (album.isNotBlank()) {
                framesStream.write(buildTextFrame("TALB", album))
            }
            if (genre.isNotBlank() && genre != "Unknown Genre") {
                framesStream.write(buildTextFrame("TCON", genre))
            }
            if (year > 0) {
                framesStream.write(buildTextFrame("TYER", year.toString()))
            }
            if (trackNumber > 0) {
                framesStream.write(buildTextFrame("TRCK", trackNumber.toString()))
            }
            if (discNumber > 0) {
                framesStream.write(buildTextFrame("TPOS", discNumber.toString()))
            }

            val newFramesBytes = framesStream.toByteArray()
            if (newFramesBytes.isEmpty()) return false

            val audioStartOffset: Int
            if (audioBytes.size >= 10 && audioBytes[0] == 'I'.code.toByte() && audioBytes[1] == 'D'.code.toByte() && audioBytes[2] == '3'.code.toByte()) {
                val synchsafeSize = readSynchsafeInt(audioBytes, 6)
                audioStartOffset = (10 + synchsafeSize).coerceAtMost(audioBytes.size)
            } else {
                audioStartOffset = 0
            }

            val audioPayload = if (audioStartOffset > 0 && audioStartOffset < audioBytes.size) {
                audioBytes.copyOfRange(audioStartOffset, audioBytes.size)
            } else {
                audioBytes
            }

            val synchsafeSize = encodeSynchsafeInt(newFramesBytes.size)
            val id3Header = byteArrayOf(
                'I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(),
                0x03, 0x00,
                0x00,
                synchsafeSize[0], synchsafeSize[1], synchsafeSize[2], synchsafeSize[3]
            )

            FileOutputStream(outputFile).use { fos ->
                fos.write(id3Header)
                fos.write(newFramesBytes)
                fos.write(audioPayload)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun isMp3Header(bytes: ByteArray): Boolean {
        if (bytes.size >= 3 && bytes[0] == 'I'.code.toByte() && bytes[1] == 'D'.code.toByte() && bytes[2] == '3'.code.toByte()) {
            return true
        }
        return bytes.size >= 2 && (bytes[0].toInt() and 0xFF) == 0xFF && (bytes[1].toInt() and 0xE0) == 0xE0
    }

    private fun embedMp3Id3v2TextFrames(
        audioBytes: ByteArray,
        trackNumber: Int,
        discNumber: Int,
        title: String,
        outputFile: File
    ): Boolean {
        return try {
            val framesStream = ByteArrayOutputStream()

            if (trackNumber > 0) {
                framesStream.write(buildTextFrame("TRCK", trackNumber.toString()))
            }
            if (discNumber > 0) {
                framesStream.write(buildTextFrame("TPOS", discNumber.toString()))
            }
            if (title.isNotBlank()) {
                framesStream.write(buildTextFrame("TIT2", title))
            }

            val newFramesBytes = framesStream.toByteArray()
            if (newFramesBytes.isEmpty()) return false

            val audioStartOffset: Int
            if (audioBytes.size >= 10 && audioBytes[0] == 'I'.code.toByte() && audioBytes[1] == 'D'.code.toByte() && audioBytes[2] == '3'.code.toByte()) {
                val synchsafeSize = readSynchsafeInt(audioBytes, 6)
                audioStartOffset = (10 + synchsafeSize).coerceAtMost(audioBytes.size)
            } else {
                audioStartOffset = 0
            }

            val audioPayload = if (audioStartOffset > 0 && audioStartOffset < audioBytes.size) {
                audioBytes.copyOfRange(audioStartOffset, audioBytes.size)
            } else {
                audioBytes
            }

            val synchsafeSize = encodeSynchsafeInt(newFramesBytes.size)
            val id3Header = byteArrayOf(
                'I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(),
                0x03, 0x00,
                0x00,
                synchsafeSize[0], synchsafeSize[1], synchsafeSize[2], synchsafeSize[3]
            )

            FileOutputStream(outputFile).use { fos ->
                fos.write(id3Header)
                fos.write(newFramesBytes)
                fos.write(audioPayload)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun buildTextFrame(frameId: String, text: String): ByteArray {
        val textBytes = text.toByteArray(Charsets.UTF_8)
        val body = ByteArrayOutputStream()
        body.write(0x03) // UTF-8 encoding
        body.write(textBytes)
        body.write(0x00) // null terminator

        val bodyBytes = body.toByteArray()
        val header = ByteArrayOutputStream()
        header.write(frameId.toByteArray(Charsets.ISO_8859_1))
        val size = bodyBytes.size
        header.write((size shr 24 and 0xFF))
        header.write((size shr 16 and 0xFF))
        header.write((size shr 8 and 0xFF))
        header.write((size and 0xFF))
        header.write(0x00)
        header.write(0x00)
        header.write(bodyBytes)
        return header.toByteArray()
    }

    private fun readSynchsafeInt(bytes: ByteArray, offset: Int): Int {
        val b1 = bytes[offset].toInt() and 0x7F
        val b2 = bytes[offset + 1].toInt() and 0x7F
        val b3 = bytes[offset + 2].toInt() and 0x7F
        val b4 = bytes[offset + 3].toInt() and 0x7F
        return (b1 shl 21) or (b2 shl 14) or (b3 shl 7) or b4
    }

    private fun encodeSynchsafeInt(value: Int): ByteArray {
        val b1 = (value shr 21 and 0x7F).toByte()
        val b2 = (value shr 14 and 0x7F).toByte()
        val b3 = (value shr 7 and 0x7F).toByte()
        val b4 = (value and 0x7F).toByte()
        return byteArrayOf(b1, b2, b3, b4)
    }
}
