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

            val (existingFrames, audioPayload) = Id3TagParser.parseAndExtractAudioPayload(audioBytes)

            val tagBodyStream = ByteArrayOutputStream()
            // Retain all existing frames EXCEPT old artwork frames ("APIC", "PIC")
            for (frame in existingFrames) {
                if (frame.id != "APIC" && frame.id != "PIC") {
                    tagBodyStream.write(frame.frameBytes)
                }
            }

            // Append new APIC frame
            tagBodyStream.write(apicFrame)

            val tagBodyBytes = tagBodyStream.toByteArray()
            val synchsafeSize = encodeSynchsafeInt(tagBodyBytes.size)

            val id3Header = byteArrayOf(
                'I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(),
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
        frameContent.write(0x00) // text encoding ISO-8859-1
        frameContent.write(mimeBytes)
        frameContent.write(0x00) // null terminator for MIME
        frameContent.write(0x03) // picture type: 0x03 Cover (front)
        frameContent.write(0x00) // description string null terminator

        frameContent.write(imageBytes)

        val frameBytes = frameContent.toByteArray()
        val frameHeader = ByteArrayOutputStream()
        headerWriteApic(frameHeader, frameBytes.size)

        frameHeader.write(frameBytes)
        return frameHeader.toByteArray()
    }

    private fun headerWriteApic(header: ByteArrayOutputStream, size: Int) {
        header.write("APIC".toByteArray(Charsets.ISO_8859_1))
        header.write((size shr 24 and 0xFF))
        header.write((size shr 16 and 0xFF))
        header.write((size shr 8 and 0xFF))
        header.write((size and 0xFF))
        header.write(0x00)
        header.write(0x00)
    }

    private fun embedFlacPicture(
        audioBytes: ByteArray,
        imageBytes: ByteArray,
        outputFile: File
    ): Boolean {
        return try {
            if (audioBytes.size < 4 || !isFlacHeader(audioBytes)) return false

            val mimeType = if (isPng(imageBytes)) "image/png" else "image/jpeg"
            val mimeBytes = mimeType.toByteArray(Charsets.UTF_8)

            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, opts)
            val w = if (opts.outWidth > 0) opts.outWidth else 500
            val h = if (opts.outHeight > 0) opts.outHeight else 500

            val picBlockDataStream = ByteArrayOutputStream()
            picBlockDataStream.write(intToFourBytes(3)) // 3 = Front Cover
            picBlockDataStream.write(intToFourBytes(mimeBytes.size))
            picBlockDataStream.write(mimeBytes)
            picBlockDataStream.write(intToFourBytes(0)) // Description length 0
            picBlockDataStream.write(intToFourBytes(w))
            picBlockDataStream.write(intToFourBytes(h))
            picBlockDataStream.write(intToFourBytes(24)) // Color depth
            picBlockDataStream.write(intToFourBytes(0)) // Indexed color count
            picBlockDataStream.write(intToFourBytes(imageBytes.size))
            picBlockDataStream.write(imageBytes)

            val picBlockData = picBlockDataStream.toByteArray()

            class FlacBlock(
                val blockType: Int,
                val blockData: ByteArray
            )

            var offset = 4
            val existingBlocks = mutableListOf<FlacBlock>()

            while (offset + 4 <= audioBytes.size) {
                val headerByte0 = audioBytes[offset].toInt() and 0xFF
                val isLast = (headerByte0 and 0x80) != 0
                val blockType = headerByte0 and 0x7F
                val len = ((audioBytes[offset + 1].toInt() and 0xFF) shl 16) or
                          ((audioBytes[offset + 2].toInt() and 0xFF) shl 8) or
                          (audioBytes[offset + 3].toInt() and 0xFF)

                offset += 4
                if (offset + len > audioBytes.size) break

                val blockData = audioBytes.copyOfRange(offset, offset + len)
                existingBlocks.add(FlacBlock(blockType, blockData))
                offset += len

                if (isLast) break
            }

            val audioPayload = if (offset <= audioBytes.size) {
                audioBytes.copyOfRange(offset, audioBytes.size)
            } else {
                ByteArray(0)
            }

            // Remove existing PICTURE blocks (type 6)
            val retainedBlocks = existingBlocks.filter { it.blockType != 6 }.toMutableList()
            retainedBlocks.add(FlacBlock(6, picBlockData))

            FileOutputStream(outputFile).use { fos ->
                fos.write("fLaC".toByteArray(Charsets.ISO_8859_1))

                for (i in retainedBlocks.indices) {
                    val block = retainedBlocks[i]
                    val isLastBlock = (i == retainedBlocks.size - 1) && (audioPayload.isNotEmpty())
                    val headerByte0 = (if (isLastBlock) 0x80 else 0x00) or (block.blockType and 0x7F)
                    val len = block.blockData.size

                    fos.write(headerByte0)
                    fos.write((len shr 16 and 0xFF))
                    fos.write((len shr 8 and 0xFF))
                    fos.write((len and 0xFF))
                    fos.write(block.blockData)
                }

                if (audioPayload.isNotEmpty()) {
                    fos.write(audioPayload)
                }
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
