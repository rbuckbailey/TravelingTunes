package com.travelingtunes.app.core.model

enum class TextAlignmentOption(val displayName: String) {
    LEFT("Left"),
    CENTER("Center"),
    RIGHT("Right")
}

enum class TitleRowType(val displayName: String) {
    ARTIST("Artist"),
    SONG("Song Title"),
    ALBUM("Album")
}

enum class AutoCategory(val displayName: String) {
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    GENRES("Genres"),
    FOLDERS("Folders")
}

enum class ArtScaleOption(val displayName: String) {
    FILL_SCREEN("Fill Screen"),
    ASPECT_FIT("Fit Screen")
}

enum class ArtAlignmentPortrait(val displayName: String) {
    TOP("Top"),
    MIDDLE("Middle"),
    BOTTOM("Bottom"),
    LEFT("Left"),
    CENTER("Center"),
    RIGHT("Right")
}

enum class ArtAlignmentLandscape(val displayName: String) {
    LEFT("Left"),
    CENTER("Center"),
    RIGHT("Right"),
    TOP("Top"),
    MIDDLE("Middle"),
    BOTTOM("Bottom")
}

enum class ArtLayoutOption(val displayName: String) {
    OVERLAY("Behind Titles"),
    DOCKED("Docked (Displace Titles)");

    companion object {
        fun fromOrdinal(ordinal: Int): ArtLayoutOption {
            return entries.getOrNull(ordinal) ?: OVERLAY
        }
    }
}

enum class ArtColorPriority(val displayName: String) {
    CENTER("Center"),
    OUTER_EDGE("Outer Edge"),
    WHOLE("Whole Edge");

    companion object {
        fun fromOrdinal(ordinal: Int): ArtColorPriority {
            return entries.getOrNull(ordinal) ?: CENTER
        }
    }
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
    val artAlignmentPortrait: ArtAlignmentPortrait = ArtAlignmentPortrait.MIDDLE,
    val artAlignmentLandscape: ArtAlignmentLandscape = ArtAlignmentLandscape.CENTER,
    val albumArtFade: Float = 1.0f,
    val artDisplayLayout: ArtLayoutOption = ArtLayoutOption.OVERLAY,
    val stretchArt: Boolean = false,
    val matchArtColorPriority: ArtColorPriority = ArtColorPriority.CENTER,
    val adaptiveDockedArt: Boolean = false,
    val separateTouchZones: Boolean = false,
    val hudType: HudTypeOption = HudTypeOption.BAR_VOLUME,
    val scrubHudType: ScrubHudTypeOption = ScrubHudTypeOption.EDGE_HUD,
    val volumeAlwaysOn: Boolean = true,
    val showStatusBar: Boolean = false,
    val showActions: Boolean = true,
    val hudLineThickness: Float = 16f,
    val artistFontKey: String = "DEFAULT",
    val songFontKey: String = "DEFAULT",
    val albumFontKey: String = "DEFAULT",
    val artistBold: Boolean = true,
    val artistItalic: Boolean = false,
    val artistUnderline: Boolean = false,
    val songBold: Boolean = true,
    val songItalic: Boolean = false,
    val songUnderline: Boolean = false,
    val albumBold: Boolean = false,
    val albumItalic: Boolean = false,
    val albumUnderline: Boolean = false,
    val keepScreenOn: Boolean = true,
    val immersiveMode: Boolean = true,
    val numEdgeRegions: Int = 3,
    val titleOrder: List<TitleRowType> = listOf(TitleRowType.ARTIST, TitleRowType.SONG, TitleRowType.ALBUM),
    val autoCategoryOrder: List<AutoCategory> = listOf(AutoCategory.SONGS, AutoCategory.ALBUMS, AutoCategory.ARTISTS, AutoCategory.GENRES, AutoCategory.FOLDERS),
    val autoShowAlbumArt: Boolean = true,
    val autoAlbumStyleGrid: Boolean = true,
    val autoArtistStyleGrid: Boolean = false,
    val autoAutoplayOnConnect: Boolean = false,
    val autoVoiceSearch: Boolean = true,
    val autoSpeedVolumeEnabled: Boolean = false,
    val autoDefaultVolume: Int = 50,
    val autoMinSpeedThreshold: Float = 15f,
    val autoSpeedVolumeRatio: Float = 1.0f,
    val autoSpeedUnit: String = "MPH",
    val drivingModeEnabled: Boolean = false,
    val autoEnableDrivingMode: Boolean = false,
    val autoActionButtonOrder: List<GestureAction> = listOf(
        GestureAction.PLAY_CURRENT_ALBUM,
        GestureAction.PLAY_CURRENT_ARTIST,
        GestureAction.PLAY_PAUSE,
        GestureAction.NEXT,
        GestureAction.PREVIOUS,
        GestureAction.TOGGLE_SHUFFLE,
        GestureAction.TOGGLE_REPEAT,
        GestureAction.SHUFFLE_ALL_SONGS
    )
)
