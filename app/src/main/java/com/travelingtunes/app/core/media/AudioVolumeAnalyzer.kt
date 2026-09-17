package com.travelingtunes.app.core.media

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import com.travelingtunes.app.core.database.MusicDatabase
import com.travelingtunes.app.core.model.NormalizationSettings
import com.travelingtunes.app.core.model.NormalizationSummary
import com.travelingtunes.app.core.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

data class VolumeAnalysisResult(
    val avgVolume: Float,
    val peakVolume: Float,
    val trackGain: Float
)

object AudioVolumeAnalyzer {

    const val TARGET_RMS = 0.15f
    const val MAX_PEAK = 0.98f

    fun calculateTrackGain(
        rms: Float,
        peak: Float,
        targetRms: Float = TARGET_RMS,
        maxPeak: Float = MAX_PEAK,
        maxGainBoost: Float = 4.0f
    ): Float {
        if (rms <= 0.0001f) return 1.0f
        val rawGain = targetRms / rms
        val maxGain = if (peak > 0.0001f) (maxPeak / peak) else maxGainBoost
        return min(rawGain, maxGain).coerceIn(0.1f, maxGainBoost)
    }

    fun calculateAlbumGain(
        songs: List<Song>,
        targetRms: Float = TARGET_RMS,
        maxPeak: Float = MAX_PEAK,
        maxGainBoost: Float = 4.0f
    ): Float {
        val validSongs = songs.filter { it.avgVolume > 0.0001f }
        if (validSongs.isEmpty()) return 1.0f

        val sumRmsSq = validSongs.fold(0.0) { acc, song -> acc + (song.avgVolume * song.avgVolume) }
        val albumAvgRms = sqrt(sumRmsSq / validSongs.size).toFloat()
        val albumMaxPeak = validSongs.maxOfOrNull { it.peakVolume } ?: 0.9f

        if (albumAvgRms <= 0.0001f) return 1.0f
        val rawGain = targetRms / albumAvgRms
        val maxGain = if (albumMaxPeak > 0.0001f) (maxPeak / albumMaxPeak) else maxGainBoost
        return min(rawGain, maxGain).coerceIn(0.1f, maxGainBoost)
    }

    fun calculateNormalizationSummary(
        songs: List<Song>,
        settings: NormalizationSettings = NormalizationSettings()
    ): NormalizationSummary {
        val totalSongs = songs.size
        val validSongs = songs.filter { it.avgVolume > 0.0001f }
        val analyzedCount = validSongs.size

        if (validSongs.isEmpty()) {
            return NormalizationSummary(totalSongs = totalSongs, analyzedSongs = 0)
        }

        val sumRmsSq = validSongs.fold(0.0) { acc, song -> acc + (song.avgVolume * song.avgVolume) }
        val avgRms = sqrt(sumRmsSq / validSongs.size).toFloat()

        val peakLimitedCount = validSongs.count { song ->
            val rawGain = settings.targetRms / song.avgVolume
            val maxPeakGain = if (song.peakVolume > 0.0001f) settings.maxPeak / song.peakVolume else settings.maxGainBoost
            maxPeakGain < rawGain - 0.01f
        }

        val minGain = validSongs.minOfOrNull { it.trackGain } ?: 1.0f
        val maxGain = validSongs.maxOfOrNull { it.trackGain } ?: 1.0f

        return NormalizationSummary(
            totalSongs = totalSongs,
            analyzedSongs = analyzedCount,
            avgRms = avgRms,
            peakLimitedCount = peakLimitedCount,
            minGain = minGain,
            maxGain = maxGain
        )
    }

    suspend fun analyzeSong(
        context: Context,
        song: Song,
        settings: NormalizationSettings = NormalizationSettings()
    ): VolumeAnalysisResult = withContext(Dispatchers.IO) {
        val result = withTimeoutOrNull(3000L) {
            analyzeSongInternal(context, song, settings)
        }
        result ?: VolumeAnalysisResult(
            avgVolume = if (song.avgVolume > 0.0001f) song.avgVolume else 0.15f,
            peakVolume = if (song.peakVolume > 0.0001f) song.peakVolume else 0.85f,
            trackGain = if (song.trackGain > 0.001f) song.trackGain else 1.0f
        )
    }

    private fun analyzeSongInternal(
        context: Context,
        song: Song,
        settings: NormalizationSettings
    ): VolumeAnalysisResult {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        var pfd: android.os.ParcelFileDescriptor? = null

        var sumSquares = 0.0
        var totalSamples = 0L
        var maxPeakVal = 0.0f

        try {
            pfd = try {
                context.contentResolver.openFileDescriptor(song.contentUri, "r")
            } catch (_: Exception) {
                null
            }

            if (pfd != null) {
                extractor.setDataSource(pfd.fileDescriptor)
            } else {
                extractor.setDataSource(context, song.contentUri, null)
            }

            var audioTrackIndex = -1
            var format: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val trackFormat = extractor.getTrackFormat(i)
                val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = trackFormat
                    break
                }
            }

            if (audioTrackIndex >= 0 && format != null) {
                extractor.selectTrack(audioTrackIndex)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                codec = MediaCodec.createDecoderByType(mime)
                codec.configure(format, null, null, 0)
                codec.start()

                val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                    format.getLong(MediaFormat.KEY_DURATION)
                } else {
                    song.durationMs * 1000L
                }

                val seekPoints = if (settings.fullScanEnabled && durationUs > 5000000L) {
                    listOf((durationUs * 0.15).toLong(), (durationUs * 0.50).toLong(), (durationUs * 0.80).toLong())
                } else if (durationUs > 2000000L) {
                    listOf((durationUs * 0.30).toLong())
                } else {
                    listOf(0L)
                }

                val framesPerWindow = if (settings.fullScanEnabled) 800 else 1200
                val bufferInfo = MediaCodec.BufferInfo()

                for (targetUs in seekPoints) {
                    if (targetUs > 0L) {
                        try {
                            extractor.seekTo(targetUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                            codec.flush()
                        } catch (_: Exception) {}
                    }

                    var isEOS = false
                    var decodedFrames = 0
                    var emptyAttempts = 0
                    val maxEmptyAttempts = 50

                    while (!isEOS && decodedFrames < framesPerWindow && emptyAttempts < maxEmptyAttempts) {
                        var processedSomething = false

                        val inputIdx = codec.dequeueInputBuffer(0L)
                        if (inputIdx >= 0) {
                            val inputBuf = codec.getInputBuffer(inputIdx)
                            if (inputBuf != null) {
                                val sampleSize = extractor.readSampleData(inputBuf, 0)
                                if (sampleSize < 0) {
                                    codec.queueInputBuffer(inputIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                    isEOS = true
                                } else {
                                    val sampleTime = extractor.sampleTime
                                    codec.queueInputBuffer(inputIdx, 0, sampleSize, sampleTime, 0)
                                    extractor.advance()
                                }
                                processedSomething = true
                            }
                        }

                        val outputIdx = codec.dequeueOutputBuffer(bufferInfo, 0L)
                        if (outputIdx >= 0) {
                            val outputBuf = codec.getOutputBuffer(outputIdx)
                            if (outputBuf != null && bufferInfo.size > 0) {
                                outputBuf.position(bufferInfo.offset)
                                outputBuf.limit(bufferInfo.offset + bufferInfo.size)
                                outputBuf.order(ByteOrder.LITTLE_ENDIAN)

                                val shortBuf = outputBuf.asShortBuffer()
                                while (shortBuf.hasRemaining()) {
                                    val sample = shortBuf.get()
                                    val norm = sample.toFloat() / 32768.0f
                                    val absNorm = abs(norm)
                                    sumSquares += (norm * norm)
                                    totalSamples++
                                    if (absNorm > maxPeakVal) {
                                        maxPeakVal = absNorm
                                    }
                                }
                                decodedFrames++
                                processedSomething = true
                            }
                            codec.releaseOutputBuffer(outputIdx, false)
                        }

                        if (!processedSomething) {
                            emptyAttempts++
                        } else {
                            emptyAttempts = 0
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                codec?.stop()
            } catch (_: Exception) {}
            try {
                codec?.release()
            } catch (_: Exception) {}
            try {
                extractor.release()
            } catch (_: Exception) {}
            try {
                pfd?.close()
            } catch (_: Exception) {}
        }

        val avgVolume = if (totalSamples > 0) sqrt(sumSquares / totalSamples).toFloat() else 0.15f
        val peakVolume = if (maxPeakVal > 0.001f) maxPeakVal else 0.85f
        val trackGain = calculateTrackGain(
            rms = avgVolume,
            peak = peakVolume,
            targetRms = settings.targetRms,
            maxPeak = settings.maxPeak,
            maxGainBoost = settings.maxGainBoost
        )

        return VolumeAnalysisResult(
            avgVolume = avgVolume,
            peakVolume = peakVolume,
            trackGain = trackGain
        )
    }

    suspend fun analyzeAllSongsInDatabase(
        context: Context,
        database: MusicDatabase,
        settings: NormalizationSettings = NormalizationSettings(),
        onProgress: (current: Int, total: Int, status: String) -> Unit = { _, _, _ -> }
    ): Pair<Int, Int> = BackgroundTaskGate.runAsBackgroundTask {
        val allSongs = database.getAllSongs()
        if (allSongs.isEmpty()) return@runAsBackgroundTask Pair(0, 0)

        var analyzedCount = 0
        var failedCount = 0

        val total = allSongs.size
        for ((index, song) in allSongs.withIndex()) {
            BackgroundTaskGate.checkYieldAndPause()
            onProgress(index + 1, total, "Analyzing volume: ${song.title}")
            try {
                val result = analyzeSong(context, song, settings)
                database.updateSongVolumeAnalysis(
                    songId = song.id,
                    avgVolume = result.avgVolume,
                    peakVolume = result.peakVolume,
                    trackGain = result.trackGain,
                    albumGain = 1.0f
                )
                analyzedCount++
            } catch (_: Exception) {
                failedCount++
            }
        }

        // Calculate album gain per album
        val updatedSongs = database.getAllSongs()
        val albumGroups = updatedSongs.groupBy { Pair(it.album, it.effectiveArtist) }
        for ((albumPair, albumSongs) in albumGroups) {
            BackgroundTaskGate.checkYieldAndPause()
            val (albumName, artistName) = albumPair
            val albumGain = calculateAlbumGain(
                songs = albumSongs,
                targetRms = settings.targetRms,
                maxPeak = settings.maxPeak,
                maxGainBoost = settings.maxGainBoost
            )
            database.updateAlbumGain(albumName, artistName, albumGain)
        }

        Pair(analyzedCount, failedCount)
    }
}
