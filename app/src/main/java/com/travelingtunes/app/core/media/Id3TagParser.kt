package com.travelingtunes.app.core.media

data class RawId3Frame(
    val id: String,
    val frameBytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as RawId3Frame
        if (id != other.id) return false
        if (!frameBytes.contentEquals(other.frameBytes)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + frameBytes.contentHashCode()
        return result
    }
}

object Id3TagParser {

    fun parseAndExtractAudioPayload(audioBytes: ByteArray): Pair<List<RawId3Frame>, ByteArray> {
        if (audioBytes.size < 10 || audioBytes[0] != 'I'.code.toByte() || audioBytes[1] != 'D'.code.toByte() || audioBytes[2] != '3'.code.toByte()) {
            return Pair(emptyList(), audioBytes)
        }

        val version = audioBytes[3].toInt() and 0xFF
        val flags = audioBytes[5].toInt() and 0xFF
        val synchsafeSize = readSynchsafeInt(audioBytes, 6)
        val tagSize = (10 + synchsafeSize).coerceAtMost(audioBytes.size)

        val audioPayload = if (tagSize > 0 && tagSize < audioBytes.size) {
            audioBytes.copyOfRange(tagSize, audioBytes.size)
        } else {
            audioBytes
        }

        var offset = 10

        // Extended Header check
        if ((flags and 0x40) != 0) {
            if (version >= 3 && offset + 4 <= tagSize) {
                val extHeaderSize = if (version == 4) {
                    readSynchsafeInt(audioBytes, offset)
                } else {
                    ((audioBytes[offset].toInt() and 0xFF) shl 24) or
                    ((audioBytes[offset + 1].toInt() and 0xFF) shl 16) or
                    ((audioBytes[offset + 2].toInt() and 0xFF) shl 8) or
                    (audioBytes[offset + 3].toInt() and 0xFF)
                }
                if (extHeaderSize in 1..(tagSize - offset)) {
                    offset += extHeaderSize
                }
            }
        }

        val frames = mutableListOf<RawId3Frame>()
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

            val frameId = String(audioBytes, offset, idLength, Charsets.ISO_8859_1)

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

            val fullFrameLength = headerSize + framePayloadSize
            val frameBytes = audioBytes.copyOfRange(offset, offset + fullFrameLength)
            frames.add(RawId3Frame(frameId, frameBytes))

            offset += fullFrameLength
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
