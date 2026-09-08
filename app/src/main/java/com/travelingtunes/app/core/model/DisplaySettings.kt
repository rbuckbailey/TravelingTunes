package com.travelingtunes.app.core.model

enum class TextAlignmentOption(val displayName: String) {
    LEFT("Left"),
    CENTER("Center"),
    RIGHT("Right")
}

enum class ArtScaleOption(val displayName: String) {
    FILL_SCREEN("Fill Screen"),
    ASPECT_FIT("Aspect Fit"),
    ASPECT_FILL("Aspect Fill")
}

enum class ArtLayoutOption(val displayName: String) {
    OVERLAY("Overlay"),
    BACKGROUND("Background"),
    SPLIT("Split")
}

enum class HudTypeOption(val value: Int, val displayName: String) {
    NONE(0, "None"),
    EDGE_HUD(1, "Edge Bar"),
    NUMBER(2, "Line"),
    BAR_VOLUME(3, "Bar Fill")
}

enum class ScrubHudTypeOption(val value: Int, val displayName: String) {
    NONE(0, "None"),
    EDGE_HUD(1, "Edge Bar"),
    POPUP(2, "Line"),
    BAR_PROGRESS(3, "Bar Fill")
}

data class DisplaySettings(
    val artistFontSize: Float = 50f,
    val songFontSize: Float = 70f,
    val albumFontSize: Float = 55f,
    val artistAlignment: TextAlignmentOption = TextAlignmentOption.LEFT,
    val songAlignment: TextAlignmentOption = TextAlignmentOption.CENTER,
    val albumAlignment: TextAlignmentOption = TextAlignmentOption.RIGHT,
    val minimumFontSize: Float = 35f,
    val titleShrinkInPortrait: Boolean = true,
    val titleShrinkLong: Boolean = true,
    val titleScrollLong: Boolean = false,
    val showAlbumArt: Boolean = true,
    val albumArtColors: Boolean = true,
    val albumArtScale: ArtScaleOption = ArtScaleOption.FILL_SCREEN,
    val albumArtFade: Float = 0.35f,
    val artDisplayLayout: ArtLayoutOption = ArtLayoutOption.OVERLAY,
    val mapOn: Int = 1, // 0 = off, 1 = show map when navigating, 2 = show map always
    val hudType: HudTypeOption = HudTypeOption.BAR_VOLUME,
    val scrubHudType: ScrubHudTypeOption = ScrubHudTypeOption.EDGE_HUD,
    val volumeAlwaysOn: Boolean = true,
    val showStatusBar: Boolean = false,
    val showActions: Boolean = true,
    val hudLineThickness: Float = 16f
)
