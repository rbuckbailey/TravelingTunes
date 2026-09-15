package com.travelingtunes.app.core.media

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.travelingtunes.app.core.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object Id3ArtworkEmbedder {

    suspend fun embedArtworkIntoSong(
        context: Context,
        song: Song,
        artworkBytes: ByteArray
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val tempInFile = File(context.cacheDir, "temp_embed_in_${song.id}.tmp")
            val tempOutFile = File(context.cacheDir, "temp_embed_out_${song.id}.tmp")

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
                    embedMp3Id3v2Apic(fileBytes, artworkBytes, tempOutFile)
                }
                fileName.endsWith(".flac") || isFlacHeader(fileBytes) -> {
                    embedFlacPicture(fileBytes, artworkBytes, tempOutFile)
                }
                else -> {
                    embedMp3Id3v2Apic(fileBytes, artworkBytes, tempOutFile)
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

    suspend fun embedArtworkIntoAlbum(
        context: Context,
        songs: List<Song>,
        artworkUri: Uri
    ): Pair<Int, Int> = BackgroundTaskGate.runAsBackgroundTask {
        if (songs.isEmpty()) return@runAsBackgroundTask Pair(0, 0)

        val imageBytes = try {
            if (artworkUri.scheme == "file") {
                File(artworkUri.path ?: "").readBytes()
            } else {
                context.contentResolver.openInputStream(artworkUri)?.use { it.readBytes() }
            }
        } catch (_: Exception) {
            null
        }

        if (imageBytes == null || imageBytes.isEmpty()) {
            return@runAsBackgroundTask Pair(0, songs.size)
        }

        var successCount = 0
        var failedCount = 0

        for (song in songs) {
            BackgroundTaskGate.checkYieldAndPause()
            val ok = embedArtworkIntoSong(context, song, imageBytes)
            if (ok) successCount++ else failedCount++
        }

        Pair(successCount, failedCount)
    }

    private fun isMp3Header(bytes: ByteArray): Boolean {
        if (bytes.size >= 3 && bytes[0] == 'I'.code.toByte() && bytes[1] == 'D'.code.toByte() && bytes[2] == '3'.code.toByte()) {
            return true
        }
        return bytes.size >= 2 && (bytes[0].toInt() and 0xFF) == 0xFF && (bytes[1].toInt() and 0xE0) == 0xE0
    }

    private fun isFlacHeader(bytes: ByteArray): Boolean {
        return bytes.size >= 4 && bytes[0] == 'f'.code.toByte() && bytes[1] == 'L'.code.toByte() && bytes[2] == 'a'.code.toByte() && bytes[3] == 'C'.code.toByte()
    }

    private fun embedMp3Id3v2Apic(
        audioBytes: ByteArray,
        imageBytes: ByteArray,
        outputFile: File
    ): Boolean {
        return try {
            val apicFrame = buildApicFrame(imageBytes)

            val audioStartOffset: Int

            if (audioBytes.size >= 10 && audioBytes[0] == 'I'.code.toByte() && audioBytes[1] == 'D'.code.toByte() && audioBytes[2] == '3'.code.toByte()) {
                val synchsafeSize = readSynchsafeInt(audioBytes, 6)
                val existingTagSize = 10 + synchsafeSize
                audioStartOffset = existingTagSize.coerceAtMost(audioBytes.size)
            } else {
                audioStartOffset = 0
            }

            val audioPayload = if (audioStartOffset > 0 && audioStartOffset < audioBytes.size) {
                audioBytes.copyOfRange(audioStartOffset, audioBytes.size)
            } else {
                audioBytes
            }

            val tagBody = ByteArrayOutputStream()
            tagBody.write(apicFrame)

            val tagBodyBytes = tagBody.toByteArray()
            val synchsafeSize = encodeSynchsafeInt(tagBodyBytes.size)

            val id3Header = byteArrayOf(
                'I'.toByte(), 'D'.toByte(), '3'.toByte(),
                0x03, 0x00,
                0x00,
                synchsafeSize[0], synchsafeSize[1], synchsafeSize[2], synchsafeSize[3]
            )

            FileOutputStream(outputFile).use { fos ->
                fos.write(id3Header)
                fos.write(tagBodyBytes)
                fos.write(audioPayload)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun buildApicFrame(imageBytes: ByteArray): ByteArray {
        val mimeType = if (isPng(imageBytes)) "image/png" else "image/jpeg"
        val mimeBytes = mimeType.toByteArray(Charsets.ISO_8859_1)

        val frameContent = ByteArrayOutputStream()
        frameContent.write(0x00)
        frameContent.write(mimeBytes)
        frameContent.write(0x00)
        frameContent.write(0x03)
        frameContent.write(0x00)

        frameContent.write(imageBytes)

        val frameBytes = frameContent.toByteArray()
        val frameHeader = ByteArrayOutputStream()
        frameHeader.write("APIC".toByteArray(Charsets.ISO_8859_1))

        val size = frameBytes.size
        frameHeader.write((size shr 24 and 0xFF))
        frameHeader.write((size shr 16 and 0xFF))
        frameHeader.write((size shr 8 and 0xFF))
        frameHeader.write((size and 0xFF))

        frameHeader.write(0x00)
        frameHeader.write(0x00)

        frameHeader.write(frameBytes)
        return frameHeader.toByteArray()
    }

    private fun embedFlacPicture(
        audioBytes: ByteArray,
        imageBytes: ByteArray,
        outputFile: File
    ): Boolean {
        return try {
            val mimeType = if (isPng(imageBytes)) "image/png" else "image/jpeg"
            val mimeBytes = mimeType.toByteArray(Charsets.UTF_8)

            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, opts)
            val w = if (opts.outWidth > 0) opts.outWidth else 500
            val h = if (opts.outHeight > 0) opts.outHeight else 500

            val picBlock = ByteArrayOutputStream()
            picBlock.write(intToFourBytes(3))
            picBlock.write(intToFourBytes(mimeBytes.size))
            picBlock.write(mimeBytes)
            picBlock.write(intToFourBytes(0))
            picBlock.write(intToFourBytes(w))
            picBlock.write(intToFourBytes(h))
            picBlock.write(intToFourBytes(24))
            picBlock.write(intToFourBytes(0))
            picBlock.write(intToFourBytes(imageBytes.size))
            picBlock.write(imageBytes)

            val picBlockData = picBlock.toByteArray()

            val blockHeader = ByteArrayOutputStream()
            blockHeader.write(0x06)
            val len = picBlockData.size
            blockHeader.write((len shr 16 and 0xFF))
            blockHeader.write((len shr 8 and 0xFF))
            blockHeader.write((len and 0xFF))

            FileOutputStream(outputFile).use { fos ->
                fos.write(audioBytes, 0, 4)
                fos.write(blockHeader.toByteArray())
                fos.write(picBlockData)
                fos.write(audioBytes, 4, audioBytes.size - 4)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun isPng(bytes: ByteArray): Boolean {
        return bytes.size >= 8 &&
                bytes[0] == 0x89.toByte() &&
                bytes[1] == 0x50.toByte() &&
                bytes[2] == 0x4E.toByte() &&
                bytes[3] == 0x47.toByte()
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

    private fun intToFourBytes(value: Int): ByteArray {
        return byteArrayOf(
            (value shr 24 and 0xFF).toByte(),
            (value shr 16 and 0xFF).toByte(),
            (value shr 8 and 0xFF).toByte(),
            (value and 0xFF).toByte()
        )
    }
}
