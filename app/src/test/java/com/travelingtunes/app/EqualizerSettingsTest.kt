package com.travelingtunes.app

import com.travelingtunes.app.core.model.EqualizerPreset
import com.travelingtunes.app.core.model.EqualizerSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualizerSettingsTest {

    @Test
    fun testAllPresetsExistAndHaveFiveBands() {
        val presets = EqualizerPreset.ALL_PRESETS
        assertTrue(presets.size >= 12)

        presets.forEach { preset ->
            assertEquals("Preset ${preset.name} should have 5 band gains", 5, preset.gainsDb.size)
        }
    }

    @Test
    fun testDefaultEqualizerSettings() {
        val settings = EqualizerSettings()
        assertFalse(settings.enabled)
        assertEquals("FLAT", settings.presetId)
        assertEquals(listOf(0f, 0f, 0f, 0f, 0f), settings.effectiveGainsDb)
    }

    @Test
    fun testGenrePresetEffectiveGains() {
        val popSettings = EqualizerSettings(enabled = true, presetId = "POP")
        assertEquals(listOf(0f, 3f, 4f, 2f, 3f), popSettings.effectiveGainsDb)

        val rockSettings = EqualizerSettings(enabled = true, presetId = "ROCK")
        assertEquals(listOf(5f, 3f, -1f, 2f, 4f), rockSettings.effectiveGainsDb)
    }

    @Test
    fun testCustomMakeYourOwnGains() {
        val customGains = listOf(5.0f, -2.5f, 3.0f, 1.5f, -4.0f)
        val customSettings = EqualizerSettings(
            enabled = true,
            presetId = "CUSTOM",
            customGainsDb = customGains
        )
        assertEquals("CUSTOM", customSettings.presetId)
        assertEquals(customGains, customSettings.effectiveGainsDb)
    }
}
