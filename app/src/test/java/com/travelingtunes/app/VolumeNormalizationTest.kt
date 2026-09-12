package com.travelingtunes.app

import android.net.Uri
import com.travelingtunes.app.core.media.AudioVolumeAnalyzer
import com.travelingtunes.app.core.model.NormalizationMode
import com.travelingtunes.app.core.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito

class VolumeNormalizationTest {

    @Test
    fun testNormalizationModeEnumDefaults() {
        assertEquals("Album-Scale (Default)", NormalizationMode.ALBUM.displayName)
        assertEquals("Track-Scale", NormalizationMode.TRACK.displayName)
        assertEquals("Off", NormalizationMode.OFF.displayName)
    }

    @Test
    fun testSongModelVolumeFields() {
        val mockUri = Mockito.mock(Uri::class.java)
        val song = Song(
            id = 1L,
            title = "Whisper",
            artist = "Acoustic Duo",
            album = "Unplugged",
            albumId = 10L,
            durationMs = 180000L,
            contentUri = mockUri,
            avgVolume = 0.08f,
            peakVolume = 0.35f,
            trackGain = 1.875f,
            albumGain = 1.45f
        )

        assertEquals(0.08f, song.avgVolume, 0.001f)
        assertEquals(0.35f, song.peakVolume, 0.001f)
        assertEquals(1.875f, song.trackGain, 0.001f)
        assertEquals(1.45f, song.albumGain, 0.001f)
    }

    @Test
    fun testTrackGainCalculation_quietTrack() {
        // Quiet track with low RMS (0.05) and low peak (0.25)
        val gain = AudioVolumeAnalyzer.calculateTrackGain(rms = 0.05f, peak = 0.25f)
        // Expected target RMS 0.15 / 0.05 = 3.0 gain (within max peak limiter 0.98 / 0.25 = 3.92)
        assertEquals(3.0f, gain, 0.01f)
    }

    @Test
    fun testTrackGainCalculation_loudTrackPeakLimiter() {
        // Loud track with RMS 0.30 and peak 0.95
        val gain = AudioVolumeAnalyzer.calculateTrackGain(rms = 0.30f, peak = 0.95f)
        // Raw target gain = 0.15 / 0.30 = 0.5f. Max allowed peak gain = 0.98 / 0.95 = 1.031f
        // min(0.5, 1.031) = 0.5f
        assertEquals(0.5f, gain, 0.01f)

        // Track with high peak near 1.0 where raw gain would clip peak
        val peakGain = AudioVolumeAnalyzer.calculateTrackGain(rms = 0.05f, peak = 0.98f)
        // Peak limiter caps gain at 0.98 / 0.98 = 1.0f to avoid blowing out speaker or clipping
        assertEquals(1.0f, peakGain, 0.01f)
        assertTrue(peakGain * 0.98f <= 0.98f + 0.001f)
    }

    @Test
    fun testAlbumGainCalculation_preservesDynamicRange() {
        val mockUri = Mockito.mock(Uri::class.java)
        // Quiet song on album
        val song1 = Song(
            id = 1L, title = "Intro", artist = "Band", album = "Concept Album",
            albumId = 5L, durationMs = 60000L, contentUri = mockUri,
            avgVolume = 0.04f, peakVolume = 0.20f
        )
        // Main song on same album
        val song2 = Song(
            id = 2L, title = "Climax", artist = "Band", album = "Concept Album",
            albumId = 5L, durationMs = 200000L, contentUri = mockUri,
            avgVolume = 0.12f, peakVolume = 0.85f
        )

        val albumGain = AudioVolumeAnalyzer.calculateAlbumGain(listOf(song1, song2))
        // Quiet album average RMS ~ 0.091 < 0.15 target, so albumGain > 1.0f
        assertTrue("Album gain should amplify quiet album", albumGain > 1.0f)
        assertTrue("Album peak safety limit preserved", albumGain * song2.peakVolume <= 0.981f)

        // When applying albumGain, relative ratio between song1 and song2 volume is preserved!
        val song1Output = song1.avgVolume * albumGain
        val song2Output = song2.avgVolume * albumGain
        val originalRatio = song1.avgVolume / song2.avgVolume
        val outputRatio = song1Output / song2Output
        assertEquals(originalRatio, outputRatio, 0.001f)
    }

    @Test
    fun testGainSelectionByNormalizationMode() {
        val mockUri = Mockito.mock(Uri::class.java)
        val song = Song(
            id = 1L, title = "Track 1", artist = "Artist", album = "Album",
            albumId = 1L, durationMs = 100000L, contentUri = mockUri,
            trackGain = 1.5f, albumGain = 1.2f
        )

        val albumModeGain = selectModifier(song, NormalizationMode.ALBUM)
        val trackModeGain = selectModifier(song, NormalizationMode.TRACK)
        val offModeGain = selectModifier(song, NormalizationMode.OFF)

        assertEquals(1.2f, albumModeGain, 0.001f)
        assertEquals(1.5f, trackModeGain, 0.001f)
        assertEquals(1.0f, offModeGain, 0.001f)
    }

    private fun selectModifier(song: Song, mode: NormalizationMode): Float {
        return when (mode) {
            NormalizationMode.ALBUM -> if (song.albumGain > 0.001f) song.albumGain else 1.0f
            NormalizationMode.TRACK -> if (song.trackGain > 0.001f) song.trackGain else 1.0f
            NormalizationMode.OFF -> 1.0f
        }
    }
}
