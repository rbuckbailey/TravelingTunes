package com.travelingtunes.app.core.model

data class NormalizationSettings(
    val targetRms: Float = 0.15f,
    val maxPeak: Float = 0.98f,
    val maxGainBoost: Float = 4.0f,
    val fullScanEnabled: Boolean = false
)

data class NormalizationSummary(
    val totalSongs: Int = 0,
    val analyzedSongs: Int = 0,
    val avgRms: Float = 0f,
    val peakLimitedCount: Int = 0,
    val minGain: Float = 1.0f,
    val maxGain: Float = 1.0f
) {
    val isFullyAnalyzed: Boolean
        get() = totalSongs > 0 && analyzedSongs >= totalSongs
}
