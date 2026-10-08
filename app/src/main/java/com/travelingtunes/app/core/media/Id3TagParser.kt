package com.travelingtunes.app.core.media

import java.io.ByteArrayOutputStream

data class ParsedId3Frame(
    val id: String,
    val flags: ByteArray = byteArrayOf(0, 0),
    val payload: ByteArray
) {
    val frameBytes: ByteArray
        get() = toId3v23FrameBytes()

    fun toId3v23FrameBytes(): ByteArray {
        val bos = ByteArrayOutputStream()
        val normalizedId = id.padEnd(4, ' ').substring(0, 4)
        bos.write(normalizedId.toByteArray(Charsets.ISO_8859_1))
        val size = payload.size
        bos.write((size shr 24 and 0xFF))
        bos.write((size shr 16 and 0xFF))
        bos.write((size shr 8 and 0xFF))
        bos.write((size and 0xFF))
        if (flags.size == 2) {
            bos.write(flags)
        } else {
            bos.write(0x00)
            bos.write(0x00)
        }
        bos.write(payload)
        return bos.toByteArray()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ParsedId3Frame
        if (id != other.id) return false
        if (!flags.contentEquals(other.flags)) return false
        if (!payload.contentEquals(other.payload)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + flags.contentHashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }
}

typealias RawId3Frame = ParsedId3Frame

object Id3TagParser {

    private val v2ToV3Map = mapOf(
        "TT2" to "TIT2",
        "TP1" to "TPE1",
        "TAL" to "TALB",
        "TCO" to "TCON",
        "TRK" to "TRCK",
        "TPA" to "TPOS",
        "TYE" to "TYER",
        "PIC" to "APIC",
        "COM" to "COMM",
        "TEN" to "TENC",
        "TCR" to "TCOP",
        "TDA" to "TDAT",
        "TRD" to "TRDA"
    )

    fun parseAndExtractAudioPayload(audioBytes: ByteArray): Pair<List<ParsedId3Frame>, ByteArray> {
        if (audioBytes.size < 10 || audioBytes[0] != 'I'.code.toByte() || audioBytes[1] != 'D'.code.toByte() || audioBytes[2] != '3'.code.toByte()) {
            return Pair(emptyList(), audioBytes)
        }

        val version = audioBytes[3].toInt() and 0xFF
        val flags = audioBytes[5].toInt() and 0xFF
        val synchsafeSize = readSynchsafeInt(audioBytes, 6)
        val tagSize = (10 + synchsafeSize).coerceAtMost(audioBytes.size)

        val audioPayload = if (tagSize in 1 until audioBytes.size) {
            audioBytes.copyOfRange(tagSize, audioBytes.size)
        } else {
            audioBytes
        }

        var offset = 10

        // Extended Header check
        if ((flags and 0x40) != 0) {
            if (offset + 4 <= tagSize) {
                val extHeaderSize = if (version == 4) {
                    readSynchsafeInt(audioBytes, offset)
                } else {
                    4 + (((audioBytes[offset].toInt() and 0xFF) shl 24) or
                         ((audioBytes[offset + 1].toInt() and 0xFF) shl 16) or
                         ((audioBytes[offset + 2].toInt() and 0xFF) shl 8) or
                         (audioBytes[offset + 3].toInt() and 0xFF))
                }
                if (extHeaderSize in 1..(tagSize - offset)) {
                    offset += extHeaderSize
                }
            }
        }

        val frames = mutableListOf<ParsedId3Frame>()
        val headerSize = if (version == 2) 6 else 10
        val idLength = if (version == 2) 3 else 4

        while (offset + headerSize <= tagSize) {
            val firstByte = audioBytes[offset]
            if (firstByte == 0.toByte()) {
                break
            }

            var isValidId = true
            for (i in 0 until idLength) {
                val b = audioBytes[offset + i].toInt() and 0xFF
                if (b !in 0x30..0x39 && b !in 0x41..0x5A) {
                    isValidId = false
                    break
                }
            }
            if (!isValidId) {
                break
            }

            val rawFrameId = String(audioBytes, offset, idLength, Charsets.ISO_8859_1)
            val frameId = if (version == 2) {
                v2ToV3Map[rawFrameId] ?: rawFrameId.padEnd(4, ' ')
            } else {
                rawFrameId
            }

            val framePayloadSize = if (version == 2) {
                ((audioBytes[offset + 3].toInt() and 0xFF) shl 16) or
                ((audioBytes[offset + 4].toInt() and 0xFF) shl 8) or
                (audioBytes[offset + 5].toInt() and 0xFF)
            } else if (version == 4) {
                readSynchsafeInt(audioBytes, offset + 4)
            } else {
                ((audioBytes[offset + 4].toInt() and 0xFF) shl 24) or
                ((audioBytes[offset + 5].toInt() and 0xFF) shl 16) or
                ((audioBytes[offset + 6].toInt() and 0xFF) shl 8) or
                (audioBytes[offset + 7].toInt() and 0xFF)
            }

            if (framePayloadSize < 0 || offset + headerSize + framePayloadSize > tagSize) {
                break
            }

            val frameFlags = if (version >= 3 && offset + 10 <= tagSize) {
                audioBytes.copyOfRange(offset + 8, offset + 10)
            } else {
                byteArrayOf(0x00, 0x00)
            }

            val payload = audioBytes.copyOfRange(offset + headerSize, offset + headerSize + framePayloadSize)
            frames.add(ParsedId3Frame(frameId, frameFlags, payload))

            offset += headerSize + framePayloadSize
        }

        return Pair(frames, audioPayload)
    }

    private fun readSynchsafeInt(bytes: ByteArray, offset: Int): Int {
        val b1 = bytes[offset].toInt() and 0x7F
        val b2 = bytes[offset + 1].toInt() and 0x7F
        val b3 = bytes[offset + 2].toInt() and 0x7F
        val b4 = bytes[offset + 3].toInt() and 0x7F
        return (b1 shl 21) or (b2 shl 14) or (b3 shl 7) or b4
    }
}
