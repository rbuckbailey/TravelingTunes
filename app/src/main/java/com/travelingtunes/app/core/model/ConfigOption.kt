package com.travelingtunes.app.core.model

data class ConfigOption(
    val key: String,
    val category: String,
    val title: String,
    val isBooleanToggle: Boolean = true,
    val targetValue: String? = null
) {
    companion object {
        val ALL_OPTIONS = listOf(
            // Colors & Themes
            ConfigOption("THEME_MATCH_ALBUM_ART", "Colors & Themes", "Theme: Match Album Art", isBooleanToggle = false, targetValue = "Match Album Art"),
            ConfigOption("THEME_WHITE_ON_GREY", "Colors & Themes", "Theme: White on Grey", isBooleanToggle = false, targetValue = "White on Grey"),
            ConfigOption("THEME_GREY_ON_BLACK", "Colors & Themes", "Theme: Grey on Black", isBooleanToggle = false, targetValue = "Grey on Black"),
            ConfigOption("THEME_LEAF", "Colors & Themes", "Theme: Leaf", isBooleanToggle = false, targetValue = "Leaf"),
            ConfigOption("THEME_OLD_WEST", "Colors & Themes", "Theme: Old West", isBooleanToggle = false, targetValue = "Old West"),
            ConfigOption("THEME_PERIWINKLE_BLUE", "Colors & Themes", "Theme: Periwinkle Blue", isBooleanToggle = false, targetValue = "Periwinkle Blue"),
            ConfigOption("THEME_LAVENDER", "Colors & Themes", "Theme: Lavender", isBooleanToggle = false, targetValue = "Lavender"),
            ConfigOption("THEME_BLUSH", "Colors & Themes", "Theme: Blush", isBooleanToggle = false, targetValue = "Blush"),
            ConfigOption("THEME_HOT_DOG_STAND", "Colors & Themes", "Theme: Hot Dog Stand", isBooleanToggle = false, targetValue = "Hot Dog Stand"),
            ConfigOption("THEME_MONDRIAN", "Colors & Themes", "Theme: Mondrian", isBooleanToggle = false, targetValue = "Mondrian"),
            ConfigOption("THEME_CUSTOM", "Colors & Themes", "Theme: Custom RGB", isBooleanToggle = false, targetValue = "Custom"),
            ConfigOption("THEME_dimAtNight", "Colors & Themes", "Dim at Night", isBooleanToggle = true),
            ConfigOption("THEME_invertAtNight", "Colors & Themes", "Invert at Night", isBooleanToggle = true),
            ConfigOption("THEME_isRounded", "Colors & Themes", "Rounded Corners", isBooleanToggle = true),
            ConfigOption("THEME_isGlass", "Colors & Themes", "Glass Effect", isBooleanToggle = true),

            // Font & Layout
            ConfigOption("ALIGN_ARTIST_LEFT", "Font & Layout", "Artist Alignment: Left", isBooleanToggle = false, targetValue = "LEFT"),
            ConfigOption("ALIGN_ARTIST_CENTER", "Font & Layout", "Artist Alignment: Center", isBooleanToggle = false, targetValue = "CENTER"),
            ConfigOption("ALIGN_ARTIST_RIGHT", "Font & Layout", "Artist Alignment: Right", isBooleanToggle = false, targetValue = "RIGHT"),
            ConfigOption("ALIGN_SONG_LEFT", "Font & Layout", "Song Alignment: Left", isBooleanToggle = false, targetValue = "LEFT"),
            ConfigOption("ALIGN_SONG_CENTER", "Font & Layout", "Song Alignment: Center", isBooleanToggle = false, targetValue = "CENTER"),
            ConfigOption("ALIGN_SONG_RIGHT", "Font & Layout", "Song Alignment: Right", isBooleanToggle = false, targetValue = "RIGHT"),
            ConfigOption("ALIGN_ALBUM_LEFT", "Font & Layout", "Album Alignment: Left", isBooleanToggle = false, targetValue = "LEFT"),
            ConfigOption("ALIGN_ALBUM_CENTER", "Font & Layout", "Album Alignment: Center", isBooleanToggle = false, targetValue = "CENTER"),
            ConfigOption("ALIGN_ALBUM_RIGHT", "Font & Layout", "Album Alignment: Right", isBooleanToggle = false, targetValue = "RIGHT"),
            ConfigOption("DISPLAY_artistBold", "Font & Layout", "Artist Bold", isBooleanToggle = true),
            ConfigOption("DISPLAY_artistItalic", "Font & Layout", "Artist Italic", isBooleanToggle = true),
            ConfigOption("DISPLAY_artistUnderline", "Font & Layout", "Artist Underline", isBooleanToggle = true),
            ConfigOption("DISPLAY_songBold", "Font & Layout", "Song Bold", isBooleanToggle = true),
            ConfigOption("DISPLAY_songItalic", "Font & Layout", "Song Italic", isBooleanToggle = true),
            ConfigOption("DISPLAY_songUnderline", "Font & Layout", "Song Underline", isBooleanToggle = true),
            ConfigOption("DISPLAY_albumBold", "Font & Layout", "Album Bold", isBooleanToggle = true),
            ConfigOption("DISPLAY_albumItalic", "Font & Layout", "Album Italic", isBooleanToggle = true),
            ConfigOption("DISPLAY_albumUnderline", "Font & Layout", "Album Underline", isBooleanToggle = true),
            ConfigOption("DISPLAY_titleShrinkInPortrait", "Font & Layout", "Shrink Title in Portrait", isBooleanToggle = true),
            ConfigOption("DISPLAY_titleShrinkLong", "Font & Layout", "Shrink Long Titles", isBooleanToggle = true),
            ConfigOption("DISPLAY_titleScrollLong", "Font & Layout", "Scroll Long Titles", isBooleanToggle = true),

            // Art, HUD & Display
            ConfigOption("DISPLAY_showAlbumArt", "Art, HUD & Display", "Show Album Art", isBooleanToggle = true),
            ConfigOption("DISPLAY_albumArtColors", "Art, HUD & Display", "Dynamic Art Colors", isBooleanToggle = true),
            ConfigOption("ART_SCALE_FILL_SCREEN", "Art, HUD & Display", "Art Scale: Fill Screen", isBooleanToggle = false, targetValue = "FILL_SCREEN"),
            ConfigOption("ART_SCALE_ASPECT_FIT", "Art, HUD & Display", "Art Scale: Fit Screen", isBooleanToggle = false, targetValue = "ASPECT_FIT"),
            ConfigOption("ART_LAYOUT_OVERLAY", "Art, HUD & Display", "Art Layout: Behind Titles", isBooleanToggle = false, targetValue = "OVERLAY"),
            ConfigOption("ART_LAYOUT_DOCKED", "Art, HUD & Display", "Art Layout: Docked", isBooleanToggle = false, targetValue = "DOCKED"),
            ConfigOption("ART_ALIGN_PORT_TOP", "Art, HUD & Display", "Portrait Art Align: Top", isBooleanToggle = false, targetValue = "TOP"),
            ConfigOption("ART_ALIGN_PORT_MIDDLE", "Art, HUD & Display", "Portrait Art Align: Middle", isBooleanToggle = false, targetValue = "MIDDLE"),
            ConfigOption("ART_ALIGN_PORT_BOTTOM", "Art, HUD & Display", "Portrait Art Align: Bottom", isBooleanToggle = false, targetValue = "BOTTOM"),
            ConfigOption("ART_ALIGN_LAND_LEFT", "Art, HUD & Display", "Landscape Art Align: Left", isBooleanToggle = false, targetValue = "LEFT"),
            ConfigOption("ART_ALIGN_LAND_CENTER", "Art, HUD & Display", "Landscape Art Align: Center", isBooleanToggle = false, targetValue = "CENTER"),
            ConfigOption("ART_ALIGN_LAND_RIGHT", "Art, HUD & Display", "Landscape Art Align: Right", isBooleanToggle = false, targetValue = "RIGHT"),
            ConfigOption("HUD_TYPE_NONE", "Art, HUD & Display", "HUD Type: None", isBooleanToggle = false, targetValue = "NONE"),
            ConfigOption("HUD_TYPE_EDGE_HUD", "Art, HUD & Display", "HUD Type: Edge Bar", isBooleanToggle = false, targetValue = "EDGE_HUD"),
            ConfigOption("HUD_TYPE_NUMBER", "Art, HUD & Display", "HUD Type: Line", isBooleanToggle = false, targetValue = "NUMBER"),
            ConfigOption("HUD_TYPE_BAR_VOLUME", "Art, HUD & Display", "HUD Type: Bar Fill", isBooleanToggle = false, targetValue = "BAR_VOLUME"),
            ConfigOption("SCRUB_HUD_TYPE_NONE", "Art, HUD & Display", "Scrub HUD: None", isBooleanToggle = false, targetValue = "NONE"),
            ConfigOption("SCRUB_HUD_TYPE_EDGE_HUD", "Art, HUD & Display", "Scrub HUD: Edge Bar", isBooleanToggle = false, targetValue = "EDGE_HUD"),
            ConfigOption("SCRUB_HUD_TYPE_POPUP", "Art, HUD & Display", "Scrub HUD: Line", isBooleanToggle = false, targetValue = "POPUP"),
            ConfigOption("SCRUB_HUD_TYPE_BAR_PROGRESS", "Art, HUD & Display", "Scrub HUD: Bar Fill", isBooleanToggle = false, targetValue = "BAR_PROGRESS"),
            ConfigOption("DISPLAY_stretchArt", "Art, HUD & Display", "Stretch Art", isBooleanToggle = true),
            ConfigOption("DISPLAY_adaptiveDockedArt", "Art, HUD & Display", "Adaptive Docked Art", isBooleanToggle = true),
            ConfigOption("DISPLAY_separateTouchZones", "Art, HUD & Display", "Separate Touch Zones", isBooleanToggle = true),
            ConfigOption("ACTION_OPEN_ART_TAGS_EDITOR", "Art, HUD & Display", "Art & Tags Editor / Replace Art", isBooleanToggle = false),
            ConfigOption("ACTION_REPLACE_ART", "Art, HUD & Display", "Replace Artwork", isBooleanToggle = false),
            ConfigOption("ACTION_DELETE_DOWNLOADED_ART", "Art, HUD & Display", "Delete Downloaded Art", isBooleanToggle = false),
            ConfigOption("DISPLAY_volumeAlwaysOn", "Art, HUD & Display", "Volume Always On", isBooleanToggle = true),
            ConfigOption("DISPLAY_showStatusBar", "Art, HUD & Display", "Show Status Bar", isBooleanToggle = true),
            ConfigOption("DISPLAY_showActions", "Art, HUD & Display", "Show Action Buttons", isBooleanToggle = true),
            ConfigOption("DISPLAY_keepScreenOn", "Art, HUD & Display", "Keep Screen On", isBooleanToggle = true),
            ConfigOption("DISPLAY_immersiveMode", "Art, HUD & Display", "Immersive Mode", isBooleanToggle = true),

            // Music Library
            ConfigOption("ACTION_SONG_PICKER", "Music Library", "Song Picker", isBooleanToggle = false),
            ConfigOption("ACTION_SELECT_ALBUM_VIEW", "Music Library", "Select Album View", isBooleanToggle = false),
            ConfigOption("ACTION_SELECT_ARTIST_VIEW", "Music Library", "Select Artist View", isBooleanToggle = false),
            ConfigOption("ACTION_SHOW_QUEUE", "Music Library", "Current Queue", isBooleanToggle = false),
            ConfigOption("ACTION_DUPLICATE_TRACK_IDENTIFIER", "Music Library", "Duplicate Track Finder", isBooleanToggle = false),
            ConfigOption("LIBRARY_autoRescan", "Music Library", "Auto Rescan Library", isBooleanToggle = true),
            ConfigOption("LIBRARY_gpsVolume", "Music Library", "Speed-Dependent Volume", isBooleanToggle = true),

            // Playback
            ConfigOption("ACTION_TOGGLE_REPEAT", "Playback", "Toggle Repeat Mode", isBooleanToggle = true),
            ConfigOption("ACTION_TOGGLE_SHUFFLE", "Playback", "Toggle Shuffle Mode", isBooleanToggle = true),
            ConfigOption("ACTION_SHUFFLE_ALL_SONGS", "Playback", "Shuffle All Songs", isBooleanToggle = false),
            ConfigOption("ACTION_PLAY_CURRENT_ARTIST", "Playback", "Play Current Artist", isBooleanToggle = false),
            ConfigOption("ACTION_PLAY_CURRENT_ALBUM", "Playback", "Play Current Album", isBooleanToggle = false),

            // App & Settings
            ConfigOption("ACTION_MENU", "App & Settings", "Open Settings Menu", isBooleanToggle = false),
            ConfigOption("ACTION_SHOW_QUICK_START", "App & Settings", "Open Quick Start Help", isBooleanToggle = false),
            ConfigOption("ACTION_SELECT_PROFILE", "App & Settings", "Open Profile Selector", isBooleanToggle = false),

            // Traveling Mode & Volume
            ConfigOption("AUTO_drivingMode", "Traveling Mode", "Traveling Mode", isBooleanToggle = true),
            ConfigOption("AUTO_autoEnableDrivingMode", "Traveling Mode", "Auto-Enable Traveling Mode", isBooleanToggle = true),
            ConfigOption("AUTO_speedVolume", "Traveling Mode", "Speed-Based Volume Adjustment", isBooleanToggle = true),
            ConfigOption("AUTO_ambientNoise", "Traveling Mode", "Ambient Noise Volume Adjustment", isBooleanToggle = true),

            // Profiles
            ConfigOption("PROFILE_DEFAULT", "Profiles", "Profile: Default", isBooleanToggle = false, targetValue = "default"),
            ConfigOption("PROFILE_TRAVELING", "Profiles", "Profile: Traveling", isBooleanToggle = false, targetValue = "traveling"),
            ConfigOption("PROFILE_DRIVING", "Profiles", "Profile: Driving", isBooleanToggle = false, targetValue = "driving"),
            ConfigOption("PROFILE_TRANSIT", "Profiles", "Profile: Transit", isBooleanToggle = false, targetValue = "transit"),
            ConfigOption("PROFILE_DOCKED", "Profiles", "Profile: Docked Art", isBooleanToggle = false, targetValue = "docked"),
            ConfigOption("PROFILE_UNDOCKED", "Profiles", "Profile: Undocked Art", isBooleanToggle = false, targetValue = "undocked")
        )


        private val dynamicOptions = mutableListOf<ConfigOption>()

        fun registerOption(option: ConfigOption) {
            if (ALL_OPTIONS.none { it.key.equals(option.key, ignoreCase = true) } &&
                dynamicOptions.none { it.key.equals(option.key, ignoreCase = true) }) {
                dynamicOptions.add(option)
            }
        }

        fun getAllOptions(): List<ConfigOption> {
            return ALL_OPTIONS + dynamicOptions
        }

        fun findByKey(key: String?): ConfigOption? {
            if (key.isNullOrEmpty()) return null
            val match = getAllOptions().find { it.key.equals(key, ignoreCase = true) }
            if (match != null) return match

            val baseKey = key.removeSuffix("Continuous")
                .removeSuffix("_art")
                .removeSuffix("_title")
                .removeSuffix("_other_target")
                .removeSuffix("_art_other_target")
                .removeSuffix("_title_other_target")
            val trigger = GestureTrigger.entries.find { it.key.equals(baseKey, ignoreCase = true) || it.name.equals(baseKey, ignoreCase = true) }
            if (trigger != null) {
                val suffixDesc = when {
                    key.endsWith("_art") -> " (Art Zone)"
                    key.endsWith("_title") -> " (Title Zone)"
                    key.endsWith("_other_target") || key.endsWith("_art_other_target") || key.endsWith("_title_other_target") -> " Target"
                    key.endsWith("Continuous") -> " Continuous"
                    else -> ""
                }
                return ConfigOption(key, "Gestures", "Gesture: ${trigger.displayName}$suffixDesc", isBooleanToggle = false)
            }

            return null
        }
    }
}
