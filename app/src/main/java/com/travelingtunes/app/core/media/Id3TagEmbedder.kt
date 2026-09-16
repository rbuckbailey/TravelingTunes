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
                    false
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
            val fileName = song.fileName.lowercase()

            val success = when {
                fileName.endsWith(".mp3") || isMp3Header(fileBytes) -> {
                    embedMp3Id3v2FullTextFrames(
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
                }
                else -> {
                    false
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
            val updatedFrames = mutableMapOf<String, ByteArray>()
            val updatedFrameIds = mutableSetOf<String>()

            if (title.isNotBlank()) {
                updatedFrames["TIT2"] = buildTextFrame("TIT2", title)
                updatedFrameIds.add("TIT2")
            }
            if (artist.isNotBlank()) {
                updatedFrames["TPE1"] = buildTextFrame("TPE1", artist)
                updatedFrameIds.add("TPE1")
            }
            if (album.isNotBlank()) {
                updatedFrames["TALB"] = buildTextFrame("TALB", album)
                updatedFrameIds.add("TALB")
            }
            if (genre.isNotBlank() && genre != "Unknown Genre") {
                updatedFrames["TCON"] = buildTextFrame("TCON", genre)
                updatedFrameIds.add("TCON")
            }
            if (year > 0) {
                updatedFrames["TYER"] = buildTextFrame("TYER", year.toString())
                updatedFrameIds.add("TYER")
            }
            if (trackNumber > 0) {
                updatedFrames["TRCK"] = buildTextFrame("TRCK", trackNumber.toString())
                updatedFrameIds.add("TRCK")
            }
            if (discNumber > 0) {
                updatedFrames["TPOS"] = buildTextFrame("TPOS", discNumber.toString())
                updatedFrameIds.add("TPOS")
            }

            if (updatedFrames.isEmpty()) return false

            val (existingFrames, audioPayload) = Id3TagParser.parseAndExtractAudioPayload(audioBytes)

            val tagBodyStream = ByteArrayOutputStream()
            // Retain existing frames except the text frame IDs being updated
            for (frame in existingFrames) {
                if (frame.id !in updatedFrameIds) {
                    tagBodyStream.write(frame.frameBytes)
                }
            }

            // Append updated text frames
            for ((_, frameBytes) in updatedFrames) {
                tagBodyStream.write(frameBytes)
            }

            val newFramesBytes = tagBodyStream.toByteArray()
            if (newFramesBytes.isEmpty()) return false

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

    private fun embedMp3Id3v2TextFrames(
        audioBytes: ByteArray,
        trackNumber: Int,
        discNumber: Int,
        title: String,
        outputFile: File
    ): Boolean {
        return try {
            val updatedFrames = mutableMapOf<String, ByteArray>()
            val updatedFrameIds = mutableSetOf<String>()

            if (trackNumber > 0) {
                updatedFrames["TRCK"] = buildTextFrame("TRCK", trackNumber.toString())
                updatedFrameIds.add("TRCK")
            }
            if (discNumber > 0) {
                updatedFrames["TPOS"] = buildTextFrame("TPOS", discNumber.toString())
                updatedFrameIds.add("TPOS")
            }
            if (title.isNotBlank()) {
                updatedFrames["TIT2"] = buildTextFrame("TIT2", title)
                updatedFrameIds.add("TIT2")
            }

            if (updatedFrames.isEmpty()) return false

            val (existingFrames, audioPayload) = Id3TagParser.parseAndExtractAudioPayload(audioBytes)

            val tagBodyStream = ByteArrayOutputStream()
            for (frame in existingFrames) {
                if (frame.id !in updatedFrameIds) {
                    tagBodyStream.write(frame.frameBytes)
                }
            }

            for ((_, frameBytes) in updatedFrames) {
                tagBodyStream.write(frameBytes)
            }

            val newFramesBytes = tagBodyStream.toByteArray()
            if (newFramesBytes.isEmpty()) return false

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

    fun buildTextFrame(frameId: String, text: String): ByteArray {
        val isAscii = text.all { it.code in 1..127 }
        val body = ByteArrayOutputStream()
        if (isAscii) {
            body.write(0x00) // ISO-8859-1
            body.write(text.toByteArray(Charsets.ISO_8859_1))
            body.write(0x00) // null terminator
        } else {
            body.write(0x01) // UTF-16 with BOM
            body.write(text.toByteArray(Charsets.UTF_16))
            body.write(0x00)
            body.write(0x00) // double null terminator
        }

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

    private fun isMp3Header(bytes: ByteArray): Boolean {
        if (bytes.size >= 3 && bytes[0] == 'I'.code.toByte() && bytes[1] == 'D'.code.toByte() && bytes[2] == '3'.code.toByte()) {
            return true
        }
        return bytes.size >= 2 && (bytes[0].toInt() and 0xFF) == 0xFF && (bytes[1].toInt() and 0xE0) == 0xE0
    }

    private fun encodeSynchsafeInt(value: Int): ByteArray {
        val b1 = (value shr 21 and 0x7F).toByte()
        val b2 = (value shr 14 and 0x7F).toByte()
        val b3 = (value shr 7 and 0x7F).toByte()
        val b4 = (value and 0x7F).toByte()
        return byteArrayOf(b1, b2, b3, b4)
    }
}
