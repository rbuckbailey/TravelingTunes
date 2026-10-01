package com.travelingtunes.app.core.model

data class EqualizerBand(
    val index: Int,
    val centerFreqHz: Int,
    val name: String,
    val minGainDb: Float = -12f,
    val maxGainDb: Float = +12f
) {
    val formattedFreq: String
        get() = if (centerFreqHz >= 1000) "${centerFreqHz / 1000}kHz" else "${centerFreqHz}Hz"
}

data class EqualizerPreset(
    val id: String,
    val name: String,
    val genre: String,
    val description: String,
    val gainsDb: List<Float> // 5 band values in dB: [60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz]
) {
    companion object {
        val BANDS = listOf(
            EqualizerBand(0, 60, "Sub-Bass"),
            EqualizerBand(1, 230, "Bass / Low-Mids"),
            EqualizerBand(2, 910, "Midrange"),
            EqualizerBand(3, 3600, "Upper-Mids"),
            EqualizerBand(4, 14000, "Treble / Air")
        )

        val FLAT = EqualizerPreset(
            id = "FLAT",
            name = "Flat / Off",
            genre = "Neutral",
            description = "Uncolored, flat reference frequency response.",
            gainsDb = listOf(0f, 0f, 0f, 0f, 0f)
        )

        val ACOUSTIC = EqualizerPreset(
            id = "ACOUSTIC",
            name = "Acoustic",
            genre = "Acoustic / Folk",
            description = "Enhances acoustic guitar string resonance, vocal clarity, and smooth highs.",
            gainsDb = listOf(4f, 2f, 1f, 3f, 2f)
        )

        val BASS_BOOST = EqualizerPreset(
            id = "BASS_BOOST",
            name = "Bass Boost",
            genre = "General",
            description = "Heavy sub-bass and low-frequency boost for punchy impact.",
            gainsDb = listOf(7f, 4f, 0f, 0f, 0f)
        )

        val CLASSICAL = EqualizerPreset(
            id = "CLASSICAL",
            name = "Classical",
            genre = "Classical / Orchestral",
            description = "Deep orchestral resonance, neutral midrange, and delicate high-end shimmer.",
            gainsDb = listOf(5f, 3f, -1f, 2f, 4f)
        )

        val EDM = EqualizerPreset(
            id = "EDM",
            name = "EDM / Electronic",
            genre = "Electronic / Dance",
            description = "Punchy sub-bass, clean scooped low-mids to prevent mud, and energetic highs.",
            gainsDb = listOf(6f, 2f, -2f, 2f, 5f)
        )

        val HIP_HOP = EqualizerPreset(
            id = "HIP_HOP",
            name = "Hip Hop",
            genre = "Hip Hop / Rap",
            description = "Deep kick drum and 808 sub-bass, snappy snares, and polished highs.",
            gainsDb = listOf(7f, 3f, 0f, 0f, 3f)
        )

        val JAZZ = EqualizerPreset(
            id = "JAZZ",
            name = "Jazz",
            genre = "Jazz / Blues",
            description = "Warm acoustic bass, dipped harsh mid frequencies, and crisp cymbals and brass.",
            gainsDb = listOf(4f, 2f, -2f, 2f, 4f)
        )

        val METAL = EqualizerPreset(
            id = "METAL",
            name = "Metal",
            genre = "Metal / Hard Rock",
            description = "Scooped midrange for heavy distorted guitar crunch, tight bass, and biting highs.",
            gainsDb = listOf(5f, 2f, -3f, 3f, 3f)
        )

        val POP = EqualizerPreset(
            id = "POP",
            name = "Pop",
            genre = "Pop",
            description = "Vocal presence boost in midrange, clean body, and smooth elevated highs.",
            gainsDb = listOf(0f, 3f, 4f, 2f, 3f)
        )

        val ROCK = EqualizerPreset(
            id = "ROCK",
            name = "Rock",
            genre = "Rock / Alternative",
            description = "Strong bass punch, slightly recessed mids for electric guitar drive, and bright treble.",
            gainsDb = listOf(5f, 3f, -1f, 2f, 4f)
        )

        val TREBLE_BOOST = EqualizerPreset(
            id = "TREBLE_BOOST",
            name = "Treble Boost",
            genre = "General",
            description = "Enhances high-frequency clarity, cymbals, acoustic shimmer, and vocal air.",
            gainsDb = listOf(0f, 0f, 1f, 4f, 7f)
        )

        val VOCAL = EqualizerPreset(
            id = "VOCAL",
            name = "Vocal Booster",
            genre = "Podcast / Vocal",
            description = "Emphasizes human vocal frequencies (300Hz–3kHz) while cutting low-end rumble.",
            gainsDb = listOf(-2f, 1f, 5f, 4f, -1f)
        )

        val CUSTOM = EqualizerPreset(
            id = "CUSTOM",
            name = "Custom",
            genre = "User Defined",
            description = "User-customized equalizer curve.",
            gainsDb = listOf(0f, 0f, 0f, 0f, 0f)
        )

        val ALL_PRESETS = listOf(
            FLAT,
            ACOUSTIC,
            BASS_BOOST,
            CLASSICAL,
            EDM,
            HIP_HOP,
            JAZZ,
            METAL,
            POP,
            ROCK,
            TREBLE_BOOST,
            VOCAL,
            CUSTOM
        )

        fun findById(id: String?): EqualizerPreset {
            if (id.isNullOrBlank()) return FLAT
            return ALL_PRESETS.find { it.id.equals(id, ignoreCase = true) } ?: FLAT
        }
    }
}

data class EqualizerSettings(
    val enabled: Boolean = false,
    val presetId: String = "FLAT",
    val customGainsDb: List<Float> = listOf(0f, 0f, 0f, 0f, 0f)
) {
    val activePreset: EqualizerPreset
        get() = EqualizerPreset.findById(presetId)

    val effectiveGainsDb: List<Float>
        get() = if (presetId.equals("CUSTOM", ignoreCase = true)) {
            if (customGainsDb.size == 5) customGainsDb else listOf(0f, 0f, 0f, 0f, 0f)
        } else {
            activePreset.gainsDb
        }
}
