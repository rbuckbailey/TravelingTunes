package com.travelingtunes.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.travelingtunes.app.core.model.ArtAlignmentLandscape
import com.travelingtunes.app.core.model.ArtAlignmentPortrait
import com.travelingtunes.app.core.model.ArtColorPriority
import com.travelingtunes.app.core.model.ArtLayoutOption
import com.travelingtunes.app.core.model.ArtScaleOption
import com.travelingtunes.app.core.model.AutoCategory
import com.travelingtunes.app.core.model.ColorTheme
import com.travelingtunes.app.core.model.ConfigOption
import com.travelingtunes.app.core.model.DisplaySettings
import com.travelingtunes.app.core.model.GestureAction
import com.travelingtunes.app.core.model.GestureBinding
import com.travelingtunes.app.core.model.GestureCategory
import com.travelingtunes.app.core.model.GestureTrigger
import com.travelingtunes.app.core.model.HudTypeOption
import com.travelingtunes.app.core.model.NormalizationMode
import com.travelingtunes.app.core.model.NormalizationSettings
import com.travelingtunes.app.core.model.Profile
import com.travelingtunes.app.core.model.ProfileSelectionMode
import com.travelingtunes.app.core.model.RepeatMode
import com.travelingtunes.app.core.model.ScrubHudTypeOption
import com.travelingtunes.app.core.model.ShuffleMode
import com.travelingtunes.app.core.model.TextAlignmentOption
import com.travelingtunes.app.core.model.ThemeSettings
import com.travelingtunes.app.core.model.TitleRowType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "traveling_tunes_settings")

data class SavedPlaybackState(
    val queueIds: List<Long> = emptyList(),
    val activeSongId: Long = -1L,
    val activeSongIndex: Int = 0,
    val positionMs: Long = 0L,
    val isShuffle: Boolean = false,
    val isRepeat: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleMode: ShuffleMode = ShuffleMode.OFF,
)

class SettingsDataStore(private val context: Context) {

    // Keys
    companion object {
        val KEY_MUSIC_FOLDER_URI = stringPreferencesKey("musicFolderUri")
        val KEY_MUSIC_FOLDER_NAME = stringPreferencesKey("musicFolderName")
        val KEY_LAST_SCAN_TIME = longPreferencesKey("lastScanTime")
        val KEY_FIRST_RUN_PROMPTED = booleanPreferencesKey("firstRunPrompted")
        val KEY_AUTO_RESCAN = booleanPreferencesKey("autoRescan")
        val KEY_NORMALIZATION_MODE = stringPreferencesKey("normalizationMode")
        val KEY_TARGET_RMS = floatPreferencesKey("targetRms")
        val KEY_MAX_PEAK = floatPreferencesKey("maxPeak")
        val KEY_MAX_GAIN_BOOST = floatPreferencesKey("maxGainBoost")
        val KEY_FULL_SCAN_ENABLED = booleanPreferencesKey("fullScanEnabled")

        val KEY_GPS_SENSITIVITY = floatPreferencesKey("gpsSensitivity")
        val KEY_GPS_VOLUME = booleanPreferencesKey("gpsVolume")
        val KEY_DISABLE_AUTOLOCK = booleanPreferencesKey("disableAutolock")

        // Display
        val KEY_ARTIST_FONT_SIZE = floatPreferencesKey("artistFontSize")
        val KEY_SONG_FONT_SIZE = floatPreferencesKey("songFontSize")
        val KEY_ALBUM_FONT_SIZE = floatPreferencesKey("albumFontSize")
        val KEY_ARTIST_ALIGNMENT = stringPreferencesKey("artistAlignment")
        val KEY_SONG_ALIGNMENT = stringPreferencesKey("songAlignment")
        val KEY_ALBUM_ALIGNMENT = stringPreferencesKey("albumAlignment")
        val KEY_MINIMUM_FONT_SIZE = floatPreferencesKey("minimumFontSize")
        val KEY_TITLE_SHRINK_PORTRAIT = booleanPreferencesKey("titleShrinkInPortrait")
        val KEY_TITLE_SHRINK_LONG = booleanPreferencesKey("titleShrinkLong")
        val KEY_TITLE_SCROLL_LONG = booleanPreferencesKey("titleScrollLong")
        val KEY_SHOW_ALBUM_ART = booleanPreferencesKey("showAlbumArt")
        val KEY_ALBUM_ART_COLORS = booleanPreferencesKey("albumArtColors")
        val KEY_ALBUM_ART_SCALE = intPreferencesKey("albumArtScale")
        val KEY_ART_ALIGNMENT_PORTRAIT = stringPreferencesKey("artAlignmentPortrait")
        val KEY_ART_ALIGNMENT_LANDSCAPE = stringPreferencesKey("artAlignmentLandscape")
        val KEY_ALBUM_ART_FADE = floatPreferencesKey("albumArtFade")
        val KEY_ART_DISPLAY_LAYOUT = intPreferencesKey("artDisplayLayout")
        val KEY_STRETCH_ART = booleanPreferencesKey("stretchArt")
        val KEY_MATCH_ART_COLOR_PRIORITY = intPreferencesKey("matchArtColorPriority")
        val KEY_ADAPTIVE_DOCKED_ART = booleanPreferencesKey("adaptiveDockedArt")
        val KEY_SEPARATE_TOUCH_ZONES = booleanPreferencesKey("separateTouchZones")
        val KEY_HUD_TYPE = intPreferencesKey("hudType")
        val KEY_SCRUB_HUD_TYPE = intPreferencesKey("scrubHudType")
        val KEY_VOLUME_ALWAYS_ON = booleanPreferencesKey("volumeAlwaysOn")
        val KEY_SHOW_STATUS_BAR = booleanPreferencesKey("showStatusBar")
        val KEY_SHOW_ACTIONS = booleanPreferencesKey("showActions")
        val KEY_HUD_LINE_THICKNESS = floatPreferencesKey("hudLineThickness")
        val KEY_ARTIST_FONT_KEY = stringPreferencesKey("artistFontKey")
        val KEY_SONG_FONT_KEY = stringPreferencesKey("songFontKey")
        val KEY_ALBUM_FONT_KEY = stringPreferencesKey("albumFontKey")
        val KEY_ARTIST_BOLD = booleanPreferencesKey("artistBold")
        val KEY_ARTIST_ITALIC = booleanPreferencesKey("artistItalic")
        val KEY_ARTIST_UNDERLINE = booleanPreferencesKey("artistUnderline")
        val KEY_SONG_BOLD = booleanPreferencesKey("songBold")
        val KEY_SONG_ITALIC = booleanPreferencesKey("songItalic")
        val KEY_SONG_UNDERLINE = booleanPreferencesKey("songUnderline")
        val KEY_ALBUM_BOLD = booleanPreferencesKey("albumBold")
        val KEY_ALBUM_ITALIC = booleanPreferencesKey("albumItalic")
        val KEY_ALBUM_UNDERLINE = booleanPreferencesKey("albumUnderline")
        val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keepScreenOn")
        val KEY_IMMERSIVE_MODE = booleanPreferencesKey("immersiveMode")
        val KEY_NUM_EDGE_REGIONS = intPreferencesKey("numEdgeRegions")
        val KEY_NUM_ART_EDGE_REGIONS = intPreferencesKey("numArtEdgeRegions")
        val KEY_TITLE_ORDER = stringPreferencesKey("titleOrder")

        // Android Auto Preferences
        val KEY_AUTO_CATEGORY_ORDER = stringPreferencesKey("autoCategoryOrder")
        val KEY_AUTO_SHOW_ALBUM_ART = booleanPreferencesKey("autoShowAlbumArt")
        val KEY_AUTO_ALBUM_STYLE_GRID = booleanPreferencesKey("autoAlbumStyleGrid")
        val KEY_AUTO_ARTIST_STYLE_GRID = booleanPreferencesKey("autoArtistStyleGrid")
        val KEY_AUTO_AUTOPLAY_ON_CONNECT = booleanPreferencesKey("autoAutoplayOnConnect")
        val KEY_AUTO_VOICE_SEARCH = booleanPreferencesKey("autoVoiceSearch")
        val KEY_AUTO_SPEED_VOLUME_ENABLED = booleanPreferencesKey("autoSpeedVolumeEnabled")
        val KEY_AUTO_AMBIENT_NOISE_ENABLED = booleanPreferencesKey("autoAmbientNoiseEnabled")
        val KEY_AUTO_DEFAULT_VOLUME = intPreferencesKey("autoDefaultVolume")
        val KEY_AUTO_MIN_SPEED_THRESHOLD = floatPreferencesKey("autoMinSpeedThreshold")
        val KEY_AUTO_SPEED_VOLUME_RATIO = floatPreferencesKey("autoSpeedVolumeRatio")
        val KEY_AUTO_SPEED_UNIT = stringPreferencesKey("autoSpeedUnit")
        val KEY_DRIVING_MODE_ENABLED = booleanPreferencesKey("drivingModeEnabled")
        val KEY_AUTO_ENABLE_DRIVING_MODE = booleanPreferencesKey("autoEnableDrivingMode")
        val KEY_AUTO_ACTION_BUTTON_ORDER = stringPreferencesKey("autoActionButtonOrder")

        // Navigation / Menu State Persistence
        val KEY_LAST_SETTINGS_SUBMENU = stringPreferencesKey("lastSettingsSubmenu")

        // Profiles Persistence
        val KEY_ACTIVE_PROFILE_ID = stringPreferencesKey("activeProfileId")
        val KEY_ACTIVE_PROFILE_STACK = stringPreferencesKey("activeProfileStack")
        val KEY_PROFILES_JSON = stringPreferencesKey("profilesJson")
        val KEY_PROFILE_SELECTION_MODE = stringPreferencesKey("profileSelectionMode")
        val KEY_PROFILE_SWITCH_TARGETS = stringPreferencesKey("profileSwitchTargets")

        // Theme
        val KEY_CURRENT_THEME = stringPreferencesKey("currentTheme")
        val KEY_PRIOR_THEME = stringPreferencesKey("priorTheme")
        val KEY_CUSTOM_TEXT_RED = floatPreferencesKey("customTextRed")
        val KEY_CUSTOM_TEXT_GREEN = floatPreferencesKey("customTextGreen")
        val KEY_CUSTOM_TEXT_BLUE = floatPreferencesKey("customTextBlue")
        val KEY_CUSTOM_SONG_TITLE_RED = floatPreferencesKey("customSongTitleRed")
        val KEY_CUSTOM_SONG_TITLE_GREEN = floatPreferencesKey("customSongTitleGreen")
        val KEY_CUSTOM_SONG_TITLE_BLUE = floatPreferencesKey("customSongTitleBlue")
        val KEY_CUSTOM_ARTIST_TITLE_RED = floatPreferencesKey("customArtistTitleRed")
        val KEY_CUSTOM_ARTIST_TITLE_GREEN = floatPreferencesKey("customArtistTitleGreen")
        val KEY_CUSTOM_ARTIST_TITLE_BLUE = floatPreferencesKey("customArtistTitleBlue")
        val KEY_CUSTOM_ALBUM_TITLE_RED = floatPreferencesKey("customAlbumTitleRed")
        val KEY_CUSTOM_ALBUM_TITLE_GREEN = floatPreferencesKey("customAlbumTitleGreen")
        val KEY_CUSTOM_ALBUM_TITLE_BLUE = floatPreferencesKey("customAlbumTitleBlue")
        val KEY_CUSTOM_BG_RED = floatPreferencesKey("customBGRed")
        val KEY_CUSTOM_BG_GREEN = floatPreferencesKey("customBGGreen")
        val KEY_CUSTOM_BG_BLUE = floatPreferencesKey("customBGBlue")
        val KEY_DIM_AT_NIGHT = booleanPreferencesKey("dimAtNight")
        val KEY_INVERT_AT_NIGHT = booleanPreferencesKey("invertAtNight")
        val KEY_SUNRISE_HOUR = intPreferencesKey("sunRiseHour")
        val KEY_SUNSET_HOUR = intPreferencesKey("sunSetHour")
        val KEY_THEME_ROUNDED = booleanPreferencesKey("themeRounded")
        val KEY_THEME_GLASS = booleanPreferencesKey("themeGlass")

        // Playback Persistence
        val KEY_SAVED_QUEUE_IDS = stringPreferencesKey("savedQueueIds")
        val KEY_ACTIVE_SONG_ID = longPreferencesKey("activeSongId")
        val KEY_ACTIVE_SONG_INDEX = intPreferencesKey("activeSongIndex")
        val KEY_PLAYBACK_POSITION_MS = longPreferencesKey("playbackPositionMs")
        val KEY_SAVED_SHUFFLE = booleanPreferencesKey("savedShuffle")
        val KEY_SAVED_REPEAT = booleanPreferencesKey("savedRepeat")
        val KEY_SAVED_REPEAT_MODE = stringPreferencesKey("savedRepeatMode")
        val KEY_SAVED_SHUFFLE_MODE = stringPreferencesKey("savedShuffleMode")

        val DEFAULT_RADIAL_ACTIONS = listOf(
            GestureAction.PLAY_PAUSE,
            GestureAction.NEXT,
            GestureAction.PREVIOUS,
            GestureAction.VOLUME_UP,
            GestureAction.VOLUME_DOWN,
            GestureAction.FAST_FORWARD,
            GestureAction.REWIND,
            GestureAction.SONG_PICKER,
        )
    }

    val displaySettingsFlow: Flow<DisplaySettings> = context.dataStore.data.map { prefs ->
        val activeProfileId = prefs[KEY_ACTIVE_PROFILE_ID] ?: Profile.DEFAULT_ID
        val rawStackStr = prefs[KEY_ACTIVE_PROFILE_STACK] ?: activeProfileId
        val stackIds = rawStackStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val profiles = Profile.listFromJson(prefs[KEY_PROFILES_JSON])
        val activeStack = stackIds.mapNotNull { id -> profiles.find { it.id == id } }.ifEmpty {
            listOf(profiles.find { it.id == activeProfileId } ?: Profile.DEFAULT)
        }

        fun getBool(prefKey: Preferences.Key<Boolean>, overrideKey: String, defaultVal: Boolean): Boolean {
            val ov = Profile.resolveEffectiveOverride(overrideKey, activeStack, profiles)
                ?: Profile.resolveEffectiveOverride(prefKey.name, activeStack, profiles)
            if (ov != null) return ov.toBooleanStrictOrNull() ?: defaultVal
            return prefs[prefKey] ?: defaultVal
        }

        fun getFloat(prefKey: Preferences.Key<Float>, overrideKey: String, defaultVal: Float): Float {
            val ov = Profile.resolveEffectiveOverride(overrideKey, activeStack, profiles)
                ?: Profile.resolveEffectiveOverride(prefKey.name, activeStack, profiles)
            if (ov != null) return ov.toFloatOrNull() ?: defaultVal
            return prefs[prefKey] ?: defaultVal
        }

        fun getInt(prefKey: Preferences.Key<Int>, overrideKey: String, defaultVal: Int): Int {
            val ov = Profile.resolveEffectiveOverride(overrideKey, activeStack, profiles)
                ?: Profile.resolveEffectiveOverride(prefKey.name, activeStack, profiles)
            if (ov != null) return ov.toIntOrNull() ?: defaultVal
            return prefs[prefKey] ?: defaultVal
        }

        fun getString(prefKey: Preferences.Key<String>, overrideKey: String, defaultVal: String): String {
            return Profile.resolveEffectiveOverride(overrideKey, activeStack, profiles)
                ?: Profile.resolveEffectiveOverride(prefKey.name, activeStack, profiles)
                ?: prefs[prefKey]
                ?: defaultVal
        }

        val artScaleOv = Profile.resolveEffectiveOverride("albumArtScale", activeStack, profiles) ?: Profile.resolveEffectiveOverride("DISPLAY_albumArtScale", activeStack, profiles)
        val albumArtScale = if (artScaleOv != null) {
            ArtScaleOption.entries.find { it.name.equals(artScaleOv, ignoreCase = true) }
                ?: ArtScaleOption.entries.getOrNull(artScaleOv.toIntOrNull() ?: -1)
                ?: ArtScaleOption.FILL_SCREEN
        } else {
            ArtScaleOption.entries.getOrElse(prefs[KEY_ALBUM_ART_SCALE] ?: 0) { ArtScaleOption.FILL_SCREEN }
        }

        val portAlignOv = Profile.resolveEffectiveOverride("artAlignmentPortrait", activeStack, profiles) ?: Profile.resolveEffectiveOverride("DISPLAY_artAlignmentPortrait", activeStack, profiles)
        val artAlignmentPortrait = if (portAlignOv != null) {
            ArtAlignmentPortrait.entries.find { it.name.equals(portAlignOv, ignoreCase = true) } ?: ArtAlignmentPortrait.MIDDLE
        } else {
            ArtAlignmentPortrait.entries.find { it.name.equals(prefs[KEY_ART_ALIGNMENT_PORTRAIT], ignoreCase = true) } ?: ArtAlignmentPortrait.MIDDLE
        }

        val landAlignOv = Profile.resolveEffectiveOverride("artAlignmentLandscape", activeStack, profiles) ?: Profile.resolveEffectiveOverride("DISPLAY_artAlignmentLandscape", activeStack, profiles)
        val artAlignmentLandscape = if (landAlignOv != null) {
            ArtAlignmentLandscape.entries.find { it.name.equals(landAlignOv, ignoreCase = true) } ?: ArtAlignmentLandscape.CENTER
        } else {
            ArtAlignmentLandscape.entries.find { it.name.equals(prefs[KEY_ART_ALIGNMENT_LANDSCAPE], ignoreCase = true) } ?: ArtAlignmentLandscape.CENTER
        }

        val prioOv = Profile.resolveEffectiveOverride("matchArtColorPriority", activeStack, profiles) ?: Profile.resolveEffectiveOverride("DISPLAY_matchArtColorPriority", activeStack, profiles)
        val matchArtColorPriority = if (prioOv != null) {
            ArtColorPriority.entries.find { it.name.equals(prioOv, ignoreCase = true) }
                ?: ArtColorPriority.entries.getOrNull(prioOv.toIntOrNull() ?: -1)
                ?: ArtColorPriority.CENTER
        } else {
            ArtColorPriority.entries.getOrElse(prefs[KEY_MATCH_ART_COLOR_PRIORITY] ?: 0) { ArtColorPriority.CENTER }
        }

        val hudOv = Profile.resolveEffectiveOverride("hudType", activeStack, profiles) ?: Profile.resolveEffectiveOverride("DISPLAY_hudType", activeStack, profiles)
        val hudType = if (hudOv != null) {
            HudTypeOption.entries.find { it.name.equals(hudOv, ignoreCase = true) || it.value == hudOv.toIntOrNull() } ?: HudTypeOption.BAR_VOLUME
        } else {
            HudTypeOption.entries.find { it.value == (prefs[KEY_HUD_TYPE] ?: 1) } ?: HudTypeOption.BAR_VOLUME
        }

        val scrubOv = Profile.resolveEffectiveOverride("scrubHudType", activeStack, profiles) ?: Profile.resolveEffectiveOverride("DISPLAY_scrubHudType", activeStack, profiles)
        val scrubHudType = if (scrubOv != null) {
            ScrubHudTypeOption.entries.find { it.name.equals(scrubOv, ignoreCase = true) || it.value == scrubOv.toIntOrNull() } ?: ScrubHudTypeOption.EDGE_HUD
        } else {
            ScrubHudTypeOption.entries.find { it.value == (prefs[KEY_SCRUB_HUD_TYPE] ?: 2) } ?: ScrubHudTypeOption.EDGE_HUD
        }

        val titleOrderStr = getString(KEY_TITLE_ORDER, "titleOrder", prefs[KEY_TITLE_ORDER] ?: "ARTIST,SONG,ALBUM")
        val titleOrder = titleOrderStr.split(",")
            .mapNotNull { name -> runCatching { TitleRowType.valueOf(name.trim()) }.getOrNull() }
            .ifEmpty { listOf(TitleRowType.ARTIST, TitleRowType.SONG, TitleRowType.ALBUM) }

        val catOrderStr = getString(KEY_AUTO_CATEGORY_ORDER, "autoCategoryOrder", prefs[KEY_AUTO_CATEGORY_ORDER] ?: "QUEUE,SONGS,ALBUMS,ARTISTS,GENRES,FOLDERS")
        val parsedCatOrder = catOrderStr.split(",")
            .mapNotNull { name -> runCatching { AutoCategory.valueOf(name.trim()) }.getOrNull() }
            .ifEmpty { listOf(AutoCategory.QUEUE, AutoCategory.SONGS, AutoCategory.ALBUMS, AutoCategory.ARTISTS, AutoCategory.GENRES, AutoCategory.FOLDERS) }
        val autoCategoryOrder = (parsedCatOrder + AutoCategory.entries).distinct()

        val actionOrderStr = getString(KEY_AUTO_ACTION_BUTTON_ORDER, "autoActionButtonOrder", prefs[KEY_AUTO_ACTION_BUTTON_ORDER] ?: "PLAY_CURRENT_ALBUM,PLAY_CURRENT_ARTIST,PLAY_PAUSE,NEXT,PREVIOUS,TOGGLE_SHUFFLE,TOGGLE_REPEAT,SHUFFLE_ALL_SONGS")
        val autoActionButtonOrder = actionOrderStr.split(",")
            .asSequence()
            .mapNotNull { name -> runCatching { GestureAction.valueOf(name.trim()) }.getOrNull() ?: GestureAction.fromKey(name.trim()) }
            .filter { (it != GestureAction.UNASSIGNED) && (it != GestureAction.OTHER_OPTION) }
            .toList()
            .ifEmpty {
                listOf(
                    GestureAction.PLAY_CURRENT_ALBUM,
                    GestureAction.PLAY_CURRENT_ARTIST,
                    GestureAction.PLAY_PAUSE,
                    GestureAction.NEXT,
                    GestureAction.PREVIOUS,
                    GestureAction.TOGGLE_SHUFFLE,
                    GestureAction.TOGGLE_REPEAT,
                    GestureAction.SHUFFLE_ALL_SONGS,
                )
            }

        DisplaySettings(
            artistFontSize = getFloat(KEY_ARTIST_FONT_SIZE, "DISPLAY_artistFontSize", 50f),
            songFontSize = getFloat(KEY_SONG_FONT_SIZE, "DISPLAY_songFontSize", 70f),
            albumFontSize = getFloat(KEY_ALBUM_FONT_SIZE, "DISPLAY_albumFontSize", 55f),
            artistAlignment = TextAlignmentOption.entries.find {
                it.name.equals(getString(KEY_ARTIST_ALIGNMENT, "ALIGN_ARTIST", "LEFT"), ignoreCase = true)
            } ?: TextAlignmentOption.LEFT,
            songAlignment = TextAlignmentOption.entries.find {
                it.name.equals(getString(KEY_SONG_ALIGNMENT, "ALIGN_SONG", "CENTER"), ignoreCase = true)
            } ?: TextAlignmentOption.CENTER,
            albumAlignment = TextAlignmentOption.entries.find {
                it.name.equals(getString(KEY_ALBUM_ALIGNMENT, "ALIGN_ALBUM", "RIGHT"), ignoreCase = true)
            } ?: TextAlignmentOption.RIGHT,
            minimumFontSize = getFloat(KEY_MINIMUM_FONT_SIZE, "DISPLAY_minimumFontSize", 35f),
            titleShrinkInPortrait = getBool(KEY_TITLE_SHRINK_PORTRAIT, "DISPLAY_titleShrinkInPortrait", true),
            titleShrinkLong = getBool(KEY_TITLE_SHRINK_LONG, "DISPLAY_titleShrinkLong", true),
            titleScrollLong = getBool(KEY_TITLE_SCROLL_LONG, "DISPLAY_titleScrollLong", false),
            showAlbumArt = getBool(KEY_SHOW_ALBUM_ART, "DISPLAY_showAlbumArt", true),
            albumArtColors = getBool(KEY_ALBUM_ART_COLORS, "DISPLAY_albumArtColors", true),
            albumArtScale = albumArtScale,
            artAlignmentPortrait = artAlignmentPortrait,
            artAlignmentLandscape = artAlignmentLandscape,
            albumArtFade = getFloat(KEY_ALBUM_ART_FADE, "albumArtFade", 1.0f).takeIf { it >= 0.05f } ?: 1.0f,
            artDisplayLayout = run {
                val artLayoutOverride = Profile.resolveEffectiveOverride("DISPLAY_artDisplayLayout", activeStack, profiles)
                    ?: Profile.resolveEffectiveOverride("artDisplayLayout", activeStack, profiles)
                    ?: Profile.resolveEffectiveOverride("ART_LAYOUT_DOCKED", activeStack, profiles)?.let { if (it.toBooleanStrictOrNull() == true) ArtLayoutOption.DOCKED.name else null }
                    ?: Profile.resolveEffectiveOverride("ART_LAYOUT_OVERLAY", activeStack, profiles)?.let { if (it.toBooleanStrictOrNull() == true) ArtLayoutOption.OVERLAY.name else null }
                if (artLayoutOverride != null) {
                    ArtLayoutOption.entries.find { it.name.equals(artLayoutOverride, ignoreCase = true) }
                        ?: ArtLayoutOption.entries.getOrNull(artLayoutOverride.toIntOrNull() ?: -1)
                        ?: ArtLayoutOption.entries.getOrElse(prefs[KEY_ART_DISPLAY_LAYOUT] ?: 0) { ArtLayoutOption.OVERLAY }
                } else {
                    ArtLayoutOption.entries.getOrElse(prefs[KEY_ART_DISPLAY_LAYOUT] ?: 0) { ArtLayoutOption.OVERLAY }
                }
            },
            stretchArt = getBool(KEY_STRETCH_ART, "DISPLAY_stretchArt", false),
            matchArtColorPriority = matchArtColorPriority,
            adaptiveDockedArt = getBool(KEY_ADAPTIVE_DOCKED_ART, "DISPLAY_adaptiveDockedArt", false),
            separateTouchZones = getBool(KEY_SEPARATE_TOUCH_ZONES, "DISPLAY_separateTouchZones", false),
            hudType = hudType,
            scrubHudType = scrubHudType,
            volumeAlwaysOn = getBool(KEY_VOLUME_ALWAYS_ON, "DISPLAY_volumeAlwaysOn", true),
            showStatusBar = getBool(KEY_SHOW_STATUS_BAR, "DISPLAY_showStatusBar", false),
            showActions = getBool(KEY_SHOW_ACTIONS, "DISPLAY_showActions", true),
            hudLineThickness = getFloat(KEY_HUD_LINE_THICKNESS, "DISPLAY_hudLineThickness", 16f),
            artistFontKey = getString(KEY_ARTIST_FONT_KEY, "DISPLAY_artistFontKey", "DEFAULT"),
            songFontKey = getString(KEY_SONG_FONT_KEY, "DISPLAY_songFontKey", "DEFAULT"),
            albumFontKey = getString(KEY_ALBUM_FONT_KEY, "DISPLAY_albumFontKey", "DEFAULT"),
            artistBold = getBool(KEY_ARTIST_BOLD, "DISPLAY_artistBold", true),
            artistItalic = getBool(KEY_ARTIST_ITALIC, "DISPLAY_artistItalic", false),
            artistUnderline = getBool(KEY_ARTIST_UNDERLINE, "DISPLAY_artistUnderline", false),
            songBold = getBool(KEY_SONG_BOLD, "DISPLAY_songBold", true),
            songItalic = getBool(KEY_SONG_ITALIC, "DISPLAY_songItalic", false),
            songUnderline = getBool(KEY_SONG_UNDERLINE, "DISPLAY_songUnderline", false),
            albumBold = getBool(KEY_ALBUM_BOLD, "DISPLAY_albumBold", false),
            albumItalic = getBool(KEY_ALBUM_ITALIC, "DISPLAY_albumItalic", false),
            albumUnderline = getBool(KEY_ALBUM_UNDERLINE, "DISPLAY_albumUnderline", false),
            keepScreenOn = getBool(KEY_KEEP_SCREEN_ON, "DISPLAY_keepScreenOn", true),
            immersiveMode = getBool(KEY_IMMERSIVE_MODE, "DISPLAY_immersiveMode", true),
            numEdgeRegions = getInt(KEY_NUM_EDGE_REGIONS, "numEdgeRegions", 3),
            numArtEdgeRegions = getInt(KEY_NUM_ART_EDGE_REGIONS, "numArtEdgeRegions", 3),
            titleOrder = titleOrder,
            autoCategoryOrder = autoCategoryOrder,
            autoShowAlbumArt = getBool(KEY_AUTO_SHOW_ALBUM_ART, "AUTO_autoShowAlbumArt", true),
            autoAlbumStyleGrid = getBool(KEY_AUTO_ALBUM_STYLE_GRID, "AUTO_autoAlbumStyleGrid", true),
            autoArtistStyleGrid = getBool(KEY_AUTO_ARTIST_STYLE_GRID, "AUTO_autoArtistStyleGrid", false),
            autoAutoplayOnConnect = getBool(KEY_AUTO_AUTOPLAY_ON_CONNECT, "AUTO_autoAutoplayOnConnect", false),
            autoVoiceSearch = getBool(KEY_AUTO_VOICE_SEARCH, "AUTO_autoVoiceSearch", true),
            autoSpeedVolumeEnabled = getBool(KEY_AUTO_SPEED_VOLUME_ENABLED, "AUTO_speedVolume", false),
            autoAmbientNoiseEnabled = getBool(KEY_AUTO_AMBIENT_NOISE_ENABLED, "AUTO_ambientNoise", false),
            autoDefaultVolume = getInt(KEY_AUTO_DEFAULT_VOLUME, "autoDefaultVolume", 50),
            autoMinSpeedThreshold = getFloat(KEY_AUTO_MIN_SPEED_THRESHOLD, "autoMinSpeedThreshold", 15f),
            autoSpeedVolumeRatio = getFloat(KEY_AUTO_SPEED_VOLUME_RATIO, "autoSpeedVolumeRatio", 1.0f),
            autoSpeedUnit = getString(KEY_AUTO_SPEED_UNIT, "AUTO_autoSpeedUnit", "MPH"),
            drivingModeEnabled = getBool(KEY_DRIVING_MODE_ENABLED, "AUTO_drivingMode", false),
            autoEnableDrivingMode = getBool(KEY_AUTO_ENABLE_DRIVING_MODE, "AUTO_autoEnableDrivingMode", false),
            autoActionButtonOrder = autoActionButtonOrder
        )
    }

    val themeSettingsFlow: Flow<ThemeSettings> = context.dataStore.data.map { prefs ->
        val activeProfileId = prefs[KEY_ACTIVE_PROFILE_ID] ?: Profile.DEFAULT_ID
        val rawStackStr = prefs[KEY_ACTIVE_PROFILE_STACK] ?: activeProfileId
        val stackIds = rawStackStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val profiles = Profile.listFromJson(prefs[KEY_PROFILES_JSON])
        val activeStack = stackIds.mapNotNull { id -> profiles.find { it.id == id } }.ifEmpty {
            listOf(profiles.find { it.id == activeProfileId } ?: Profile.DEFAULT)
        }

        fun getBool(prefKey: Preferences.Key<Boolean>, overrideKey: String, defaultVal: Boolean): Boolean {
            val ov = Profile.resolveEffectiveOverride(overrideKey, activeStack, profiles)
                ?: Profile.resolveEffectiveOverride(prefKey.name, activeStack, profiles)
            if (ov != null) return ov.toBooleanStrictOrNull() ?: defaultVal
            return prefs[prefKey] ?: defaultVal
        }

        fun getFloat(prefKey: Preferences.Key<Float>, overrideKey: String, defaultVal: Float): Float {
            val ov = Profile.resolveEffectiveOverride(overrideKey, activeStack, profiles)
                ?: Profile.resolveEffectiveOverride(prefKey.name, activeStack, profiles)
            if (ov != null) return ov.toFloatOrNull() ?: defaultVal
            return prefs[prefKey] ?: defaultVal
        }

        fun getInt(prefKey: Preferences.Key<Int>, overrideKey: String, defaultVal: Int): Int {
            val ov = Profile.resolveEffectiveOverride(overrideKey, activeStack, profiles)
                ?: Profile.resolveEffectiveOverride(prefKey.name, activeStack, profiles)
            if (ov != null) return ov.toIntOrNull() ?: defaultVal
            return prefs[prefKey] ?: defaultVal
        }

        fun getString(prefKey: Preferences.Key<String>, overrideKey: String, defaultVal: String): String {
            return Profile.resolveEffectiveOverride(overrideKey, activeStack, profiles)
                ?: Profile.resolveEffectiveOverride(prefKey.name, activeStack, profiles)
                ?: prefs[prefKey]
                ?: defaultVal
        }

        val textRed = getFloat(KEY_CUSTOM_TEXT_RED, "customTextRed", 22f)
        val textGreen = getFloat(KEY_CUSTOM_TEXT_GREEN, "customTextGreen", 22f)
        val textBlue = getFloat(KEY_CUSTOM_TEXT_BLUE, "customTextBlue", 180f)

        ThemeSettings(
            currentThemeName = getString(KEY_CURRENT_THEME, "THEME_currentThemeName", getString(KEY_CURRENT_THEME, "currentThemeName", ColorTheme.MATCH_ALBUM_ART.name)),
            customTextRed = textRed,
            customTextGreen = textGreen,
            customTextBlue = textBlue,
            customSongTitleRed = getFloat(KEY_CUSTOM_SONG_TITLE_RED, "customSongTitleRed", textRed),
            customSongTitleGreen = getFloat(KEY_CUSTOM_SONG_TITLE_GREEN, "customSongTitleGreen", textGreen),
            customSongTitleBlue = getFloat(KEY_CUSTOM_SONG_TITLE_BLUE, "customSongTitleBlue", textBlue),
            customArtistTitleRed = getFloat(KEY_CUSTOM_ARTIST_TITLE_RED, "customArtistTitleRed", textRed),
            customArtistTitleGreen = getFloat(KEY_CUSTOM_ARTIST_TITLE_GREEN, "customArtistTitleGreen", textGreen),
            customArtistTitleBlue = getFloat(KEY_CUSTOM_ARTIST_TITLE_BLUE, "customArtistTitleBlue", textBlue),
            customAlbumTitleRed = getFloat(KEY_CUSTOM_ALBUM_TITLE_RED, "customAlbumTitleRed", textRed),
            customAlbumTitleGreen = getFloat(KEY_CUSTOM_ALBUM_TITLE_GREEN, "customAlbumTitleGreen", textGreen),
            customAlbumTitleBlue = getFloat(KEY_CUSTOM_ALBUM_TITLE_BLUE, "customAlbumTitleBlue", textBlue),
            customBGRed = getFloat(KEY_CUSTOM_BG_RED, "customBGRed", 200f),
            customBGGreen = getFloat(KEY_CUSTOM_BG_GREEN, "customBGGreen", 200f),
            customBGBlue = getFloat(KEY_CUSTOM_BG_BLUE, "customBGBlue", 100f),
            dimAtNight = getBool(KEY_DIM_AT_NIGHT, "THEME_dimAtNight", true),
            invertAtNight = getBool(KEY_INVERT_AT_NIGHT, "THEME_invertAtNight", false),
            sunRiseHour = getInt(KEY_SUNRISE_HOUR, "sunRiseHour", 6),
            sunSetHour = getInt(KEY_SUNSET_HOUR, "sunSetHour", 19),
            isRounded = getBool(KEY_THEME_ROUNDED, "THEME_isRounded", false),
            isGlass = getBool(KEY_THEME_GLASS, "THEME_isGlass", false)
        )
    }

    val gestureBindingsFlow: Flow<Map<GestureTrigger, GestureBinding>> = context.dataStore.data.map { prefs ->
        val activeProfileId = prefs[KEY_ACTIVE_PROFILE_ID] ?: Profile.DEFAULT_ID
        val rawStackStr = prefs[KEY_ACTIVE_PROFILE_STACK] ?: activeProfileId
        val stackIds = rawStackStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val profiles = Profile.listFromJson(prefs[KEY_PROFILES_JSON])
        val activeStack = stackIds.mapNotNull { id -> profiles.find { it.id == id } }.ifEmpty {
            listOf(profiles.find { it.id == activeProfileId } ?: Profile.DEFAULT)
        }

        fun getString(key: String, defaultVal: String): String {
            return Profile.resolveEffectiveOverride(key, activeStack, profiles)
                ?: prefs[stringPreferencesKey(key)]
                ?: defaultVal
        }

        fun getBool(key: String, defaultVal: Boolean): Boolean {
            val ov = Profile.resolveEffectiveOverride(key, activeStack, profiles)
            if (ov != null) return ov.toBooleanStrictOrNull() ?: defaultVal
            return prefs[booleanPreferencesKey(key)] ?: defaultVal
        }

        GestureTrigger.entries.associateWith { trigger ->
            val actionKey = getString(trigger.key, trigger.defaultActionKey)
            val isContinuous = getBool("${trigger.key}Continuous", trigger.isContinuousDefault)
            val otherOptionKey = getString("${trigger.key}_other_target", "")

            val artActionKey = getString("${trigger.key}_art", GestureAction.UNASSIGNED.name)
            val artOtherOptionKey = getString("${trigger.key}_art_other_target", "")

            val defaultTitleKey = if (trigger.category == GestureCategory.SCREEN_REGION) actionKey else GestureAction.UNASSIGNED.name
            val titleActionKey = getString("${trigger.key}_title", defaultTitleKey)
            val titleOtherOptionKey = getString("${trigger.key}_title_other_target", "")

            GestureBinding(
                trigger = trigger,
                action = GestureAction.fromKey(actionKey),
                isContinuous = isContinuous,
                otherOptionKey = otherOptionKey.ifEmpty { null },
                artAction = GestureAction.fromKey(artActionKey),
                artOtherOptionKey = artOtherOptionKey.ifEmpty { null },
                titleAction = GestureAction.fromKey(titleActionKey),
                titleOtherOptionKey = titleOtherOptionKey.ifEmpty { null }
            )
        }
    }

    fun getRadialMenuActionsFlow(triggerKey: String): Flow<List<GestureAction>> {
        return context.dataStore.data.map { prefs ->
            val activeProfileId = prefs[KEY_ACTIVE_PROFILE_ID] ?: Profile.DEFAULT_ID
            val rawStackStr = prefs[KEY_ACTIVE_PROFILE_STACK] ?: activeProfileId
            val stackIds = rawStackStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val profiles = Profile.listFromJson(prefs[KEY_PROFILES_JSON])
            val activeStack = stackIds.mapNotNull { id -> profiles.find { it.id == id } }.ifEmpty {
                listOf(profiles.find { it.id == activeProfileId } ?: Profile.DEFAULT)
            }

            val rawStr = Profile.resolveEffectiveOverride("radial_actions_$triggerKey", activeStack, profiles)
                ?: prefs[stringPreferencesKey("radial_actions_$triggerKey")]

            if (rawStr.isNullOrEmpty()) {
                DEFAULT_RADIAL_ACTIONS
            } else {
                rawStr.split(",").map { GestureAction.fromKey(it) }
            }
        }
    }

    val gpsVolumeEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        val activeProfileId = prefs[KEY_ACTIVE_PROFILE_ID] ?: Profile.DEFAULT_ID
        val rawStackStr = prefs[KEY_ACTIVE_PROFILE_STACK] ?: activeProfileId
        val stackIds = rawStackStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val profiles = Profile.listFromJson(prefs[KEY_PROFILES_JSON])
        val activeStack = stackIds.mapNotNull { id -> profiles.find { it.id == id } }.ifEmpty {
            listOf(profiles.find { it.id == activeProfileId } ?: Profile.DEFAULT)
        }

        val ov = Profile.resolveEffectiveOverride("LIBRARY_gpsVolume", activeStack, profiles)
            ?: Profile.resolveEffectiveOverride("gpsVolume", activeStack, profiles)
        if (ov != null) return@map ov.toBooleanStrictOrNull() ?: false
        prefs[KEY_GPS_VOLUME] ?: false
    }

    val gpsSensitivityFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        val activeProfileId = prefs[KEY_ACTIVE_PROFILE_ID] ?: Profile.DEFAULT_ID
        val rawStackStr = prefs[KEY_ACTIVE_PROFILE_STACK] ?: activeProfileId
        val stackIds = rawStackStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val profiles = Profile.listFromJson(prefs[KEY_PROFILES_JSON])
        val activeStack = stackIds.mapNotNull { id -> profiles.find { it.id == id } }.ifEmpty {
            listOf(profiles.find { it.id == activeProfileId } ?: Profile.DEFAULT)
        }

        val ov = Profile.resolveEffectiveOverride("LIBRARY_gpsSensitivity", activeStack, profiles)
            ?: Profile.resolveEffectiveOverride("gpsSensitivity", activeStack, profiles)
        if (ov != null) return@map ov.toFloatOrNull() ?: 0.5f
        prefs[KEY_GPS_SENSITIVITY] ?: 0.5f
    }

    val musicFolderUriFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_MUSIC_FOLDER_URI]
    }

    val musicFolderNameFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_MUSIC_FOLDER_NAME]
    }

    val lastScanTimeFlow: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_SCAN_TIME] ?: 0L
    }

    val firstRunPromptedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_FIRST_RUN_PROMPTED] ?: false
    }

    val autoRescanFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        val activeProfileId = prefs[KEY_ACTIVE_PROFILE_ID] ?: Profile.DEFAULT_ID
        val rawStackStr = prefs[KEY_ACTIVE_PROFILE_STACK] ?: activeProfileId
        val stackIds = rawStackStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val profiles = Profile.listFromJson(prefs[KEY_PROFILES_JSON])
        val activeStack = stackIds.mapNotNull { id -> profiles.find { it.id == id } }.ifEmpty {
            listOf(profiles.find { it.id == activeProfileId } ?: Profile.DEFAULT)
        }

        val ov = Profile.resolveEffectiveOverride("LIBRARY_autoRescan", activeStack, profiles)
            ?: Profile.resolveEffectiveOverride("autoRescan", activeStack, profiles)
        if (ov != null) return@map ov.toBooleanStrictOrNull() ?: false
        prefs[KEY_AUTO_RESCAN] ?: false
    }

    val normalizationModeFlow: Flow<NormalizationMode> = context.dataStore.data.map { prefs ->
        val activeProfileId = prefs[KEY_ACTIVE_PROFILE_ID] ?: Profile.DEFAULT_ID
        val rawStackStr = prefs[KEY_ACTIVE_PROFILE_STACK] ?: activeProfileId
        val stackIds = rawStackStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val profiles = Profile.listFromJson(prefs[KEY_PROFILES_JSON])
        val activeStack = stackIds.mapNotNull { id -> profiles.find { it.id == id } }.ifEmpty {
            listOf(profiles.find { it.id == activeProfileId } ?: Profile.DEFAULT)
        }

        val modeName = Profile.resolveEffectiveOverride("normalizationMode", activeStack, profiles)
            ?: prefs[KEY_NORMALIZATION_MODE]
        if (modeName != null) {
            NormalizationMode.entries.find { it.name.equals(modeName, ignoreCase = true) } ?: NormalizationMode.ALBUM
        } else {
            NormalizationMode.ALBUM
        }
    }

    suspend fun setNormalizationMode(mode: NormalizationMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_NORMALIZATION_MODE] = mode.name
        }
    }

    val normalizationSettingsFlow: Flow<NormalizationSettings> = context.dataStore.data.map { prefs ->
        NormalizationSettings(
            targetRms = prefs[KEY_TARGET_RMS] ?: 0.15f,
            maxPeak = prefs[KEY_MAX_PEAK] ?: 0.98f,
            maxGainBoost = prefs[KEY_MAX_GAIN_BOOST] ?: 4.0f,
            fullScanEnabled = prefs[KEY_FULL_SCAN_ENABLED] ?: false
        )
    }

    suspend fun setNormalizationSettings(settings: NormalizationSettings) {
        context.dataStore.edit { prefs ->
            val activeId = prefs[KEY_ACTIVE_PROFILE_ID] ?: Profile.DEFAULT_ID
            if (activeId == Profile.DEFAULT_ID) {
                prefs[KEY_TARGET_RMS] = settings.targetRms.coerceIn(0.05f, 0.30f)
                prefs[KEY_MAX_PEAK] = settings.maxPeak.coerceIn(0.80f, 0.999f)
                prefs[KEY_MAX_GAIN_BOOST] = settings.maxGainBoost.coerceIn(1.0f, 10.0f)
                prefs[KEY_FULL_SCAN_ENABLED] = settings.fullScanEnabled
            } else {
                val profiles = Profile.listFromJson(prefs[KEY_PROFILES_JSON]).map { profile ->
                    if (profile.id == activeId) {
                        val updatedMap = profile.overrides.toMutableMap()
                        updatedMap["targetRms"] = settings.targetRms.coerceIn(0.05f, 0.30f).toString()
                        updatedMap["maxPeak"] = settings.maxPeak.coerceIn(0.80f, 0.999f).toString()
                        updatedMap["maxGainBoost"] = settings.maxGainBoost.coerceIn(1.0f, 10.0f).toString()
                        updatedMap["fullScanEnabled"] = settings.fullScanEnabled.toString()
                        profile.copy(overrides = updatedMap)
                    } else profile
                }
                prefs[KEY_PROFILES_JSON] = Profile.listToJson(profiles)
            }
        }
    }

    val lastSettingsSubmenuFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_SETTINGS_SUBMENU]
    }

    suspend fun setLastSettingsSubmenu(submenuName: String?) {
        context.dataStore.edit { prefs ->
            if (submenuName == null) {
                prefs.remove(KEY_LAST_SETTINGS_SUBMENU)
            } else {
                prefs[KEY_LAST_SETTINGS_SUBMENU] = submenuName
            }
        }
    }

    val savedPlaybackStateFlow: Flow<SavedPlaybackState> = context.dataStore.data.map { prefs ->
        val queueStr = prefs[KEY_SAVED_QUEUE_IDS] ?: ""
        val queueIds = queueStr.split(",").mapNotNull { it.trim().toLongOrNull() }
        val isShuffle = prefs[KEY_SAVED_SHUFFLE] ?: false
        val isRepeat = prefs[KEY_SAVED_REPEAT] ?: false

        val repeatModeName = prefs[KEY_SAVED_REPEAT_MODE]
        val repeatMode = if (repeatModeName != null) {
            RepeatMode.entries.find { it.name.equals(repeatModeName, ignoreCase = true) } ?: RepeatMode.OFF
        } else {
            if (isRepeat) RepeatMode.SONG else RepeatMode.OFF
        }

        val shuffleModeName = prefs[KEY_SAVED_SHUFFLE_MODE]
        val shuffleMode = if (shuffleModeName != null) {
            ShuffleMode.entries.find { it.name.equals(shuffleModeName, ignoreCase = true) } ?: ShuffleMode.OFF
        } else {
            if (isShuffle) ShuffleMode.SONGS else ShuffleMode.OFF
        }

        SavedPlaybackState(
            queueIds = queueIds,
            activeSongId = prefs[KEY_ACTIVE_SONG_ID] ?: -1L,
            activeSongIndex = prefs[KEY_ACTIVE_SONG_INDEX] ?: 0,
            positionMs = prefs[KEY_PLAYBACK_POSITION_MS] ?: 0L,
            isShuffle = isShuffle,
            isRepeat = isRepeat,
            repeatMode = repeatMode,
            shuffleMode = shuffleMode
        )
    }

    suspend fun savePlaybackState(
        queueIds: List<Long>,
        activeSongId: Long,
        activeSongIndex: Int,
        positionMs: Long,
        isShuffle: Boolean,
        isRepeat: Boolean,
        repeatMode: RepeatMode = if (isRepeat) RepeatMode.SONG else RepeatMode.OFF,
        shuffleMode: ShuffleMode = if (isShuffle) ShuffleMode.SONGS else ShuffleMode.OFF
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SAVED_QUEUE_IDS] = queueIds.joinToString(",")
            prefs[KEY_ACTIVE_SONG_ID] = activeSongId
            prefs[KEY_ACTIVE_SONG_INDEX] = activeSongIndex
            prefs[KEY_PLAYBACK_POSITION_MS] = positionMs
            prefs[KEY_SAVED_SHUFFLE] = isShuffle
            prefs[KEY_SAVED_REPEAT] = isRepeat
            prefs[KEY_SAVED_REPEAT_MODE] = repeatMode.name
            prefs[KEY_SAVED_SHUFFLE_MODE] = shuffleMode.name
        }
    }

    suspend fun setMusicFolder(uri: String?, name: String?) {
        context.dataStore.edit { prefs ->
            if (uri != null) prefs[KEY_MUSIC_FOLDER_URI] = uri else prefs.remove(KEY_MUSIC_FOLDER_URI)
            if (name != null) prefs[KEY_MUSIC_FOLDER_NAME] = name else prefs.remove(KEY_MUSIC_FOLDER_NAME)
        }
    }

    suspend fun setLastScanTime(time: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_SCAN_TIME] = time
        }
    }

    suspend fun setFirstRunPrompted(prompted: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FIRST_RUN_PROMPTED] = prompted
        }
    }

    suspend fun setAutoRescan(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val activeId = prefs[KEY_ACTIVE_PROFILE_ID] ?: Profile.DEFAULT_ID
            if (activeId == Profile.DEFAULT_ID) {
                prefs[KEY_AUTO_RESCAN] = enabled
            } else {
                val profiles = Profile.listFromJson(prefs[KEY_PROFILES_JSON]).map { profile ->
                    if (profile.id == activeId) {
                        val updatedMap = profile.overrides.toMutableMap()
                        updatedMap["LIBRARY_autoRescan"] = enabled.toString()
                        updatedMap["autoRescan"] = enabled.toString()
                        profile.copy(overrides = updatedMap)
                    } else profile
                }
                prefs[KEY_PROFILES_JSON] = Profile.listToJson(profiles)
            }
        }
    }

    suspend fun setDrivingModeEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DRIVING_MODE_ENABLED] = enabled
            if (!enabled) {
                prefs[KEY_AUTO_SPEED_VOLUME_ENABLED] = false
                prefs[KEY_AUTO_AMBIENT_NOISE_ENABLED] = false
            }
            val activeId = prefs[KEY_ACTIVE_PROFILE_ID] ?: Profile.DEFAULT_ID
            if (activeId != Profile.DEFAULT_ID) {
                val profiles = Profile.listFromJson(prefs[KEY_PROFILES_JSON]).map { profile ->
                    if (profile.id == activeId) {
                        val updatedMap = profile.overrides.toMutableMap()
                        updatedMap["AUTO_drivingMode"] = enabled.toString()
                        updatedMap["drivingModeEnabled"] = enabled.toString()
                        if (!enabled) {
                            updatedMap["AUTO_speedVolume"] = "false"
                            updatedMap["autoSpeedVolumeEnabled"] = "false"
                            updatedMap["AUTO_ambientNoise"] = "false"
                            updatedMap["autoAmbientNoiseEnabled"] = "false"
                        } else {
                            updatedMap["AUTO_speedVolume"] = (prefs[KEY_AUTO_SPEED_VOLUME_ENABLED] ?: false).toString()
                            updatedMap["autoSpeedVolumeEnabled"] = (prefs[KEY_AUTO_SPEED_VOLUME_ENABLED] ?: false).toString()
                            updatedMap["AUTO_ambientNoise"] = (prefs[KEY_AUTO_AMBIENT_NOISE_ENABLED] ?: false).toString()
                            updatedMap["autoAmbientNoiseEnabled"] = (prefs[KEY_AUTO_AMBIENT_NOISE_ENABLED] ?: false).toString()
                        }
                        profile.copy(overrides = updatedMap)
                    } else profile
                }
                prefs[KEY_PROFILES_JSON] = Profile.listToJson(profiles)
            }
        }
    }

    suspend fun setAutoDefaultVolume(volumePercent: Int) {
        context.dataStore.edit { prefs ->
            val activeId = prefs[KEY_ACTIVE_PROFILE_ID] ?: Profile.DEFAULT_ID
            if (activeId == Profile.DEFAULT_ID) {
                prefs[KEY_AUTO_DEFAULT_VOLUME] = volumePercent.coerceIn(0, 100)
            } else {
                val profiles = Profile.listFromJson(prefs[KEY_PROFILES_JSON]).map { profile ->
                    if (profile.id == activeId) {
                        val updatedMap = profile.overrides.toMutableMap()
                        updatedMap["autoDefaultVolume"] = volumePercent.coerceIn(0, 100).toString()
                        profile.copy(overrides = updatedMap)
                    } else profile
                }
                prefs[KEY_PROFILES_JSON] = Profile.listToJson(profiles)
            }
        }
    }

    suspend fun toggleDrivingMode() {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_DRIVING_MODE_ENABLED] ?: false
            val next = !current
            prefs[KEY_DRIVING_MODE_ENABLED] = next
            if (!next) {
                prefs[KEY_AUTO_SPEED_VOLUME_ENABLED] = false
                prefs[KEY_AUTO_AMBIENT_NOISE_ENABLED] = false
            }
            val activeId = prefs[KEY_ACTIVE_PROFILE_ID] ?: Profile.DEFAULT_ID
            if (activeId != Profile.DEFAULT_ID) {
                val profiles = Profile.listFromJson(prefs[KEY_PROFILES_JSON]).map { profile ->
                    if (profile.id == activeId) {
                        val updatedMap = profile.overrides.toMutableMap()
                        updatedMap["AUTO_drivingMode"] = next.toString()
                        updatedMap["drivingModeEnabled"] = next.toString()
                        if (!next) {
                            updatedMap["AUTO_speedVolume"] = "false"
                            updatedMap["autoSpeedVolumeEnabled"] = "false"
                            updatedMap["AUTO_ambientNoise"] = "false"
                            updatedMap["autoAmbientNoiseEnabled"] = "false"
                        } else {
                            updatedMap["AUTO_speedVolume"] = (prefs[KEY_AUTO_SPEED_VOLUME_ENABLED] ?: false).toString()
                            updatedMap["autoSpeedVolumeEnabled"] = (prefs[KEY_AUTO_SPEED_VOLUME_ENABLED] ?: false).toString()
                            updatedMap["AUTO_ambientNoise"] = (prefs[KEY_AUTO_AMBIENT_NOISE_ENABLED] ?: false).toString()
                            updatedMap["autoAmbientNoiseEnabled"] = (prefs[KEY_AUTO_AMBIENT_NOISE_ENABLED] ?: false).toString()
                        }
                        profile.copy(overrides = updatedMap)
                    } else profile
                }
                prefs[KEY_PROFILES_JSON] = Profile.listToJson(profiles)
            }
        }
    }

    suspend fun updateGestureBinding(
        trigger: GestureTrigger,
        action: GestureAction = GestureAction.UNASSIGNED,
        isContinuous: Boolean = false,
        otherOptionKey: String? = null,
        artAction: GestureAction = GestureAction.UNASSIGNED,
        artOtherOptionKey: String? = null,
        titleAction: GestureAction = GestureAction.UNASSIGNED,
        titleOtherOptionKey: String? = null
    ) {
        context.dataStore.edit { prefs ->
            val activeId = prefs[KEY_ACTIVE_PROFILE_ID] ?: Profile.DEFAULT_ID
            if (activeId == Profile.DEFAULT_ID) {
                prefs[stringPreferencesKey(trigger.key)] = action.name
                prefs[booleanPreferencesKey("${trigger.key}Continuous")] = isContinuous
                if (otherOptionKey != null) {
                    prefs[stringPreferencesKey("${trigger.key}_other_target")] = otherOptionKey
                } else {
                    prefs.remove(stringPreferencesKey("${trigger.key}_other_target"))
                }

                prefs[stringPreferencesKey("${trigger.key}_art")] = artAction.name
                if (artOtherOptionKey != null) {
                    prefs[stringPreferencesKey("${trigger.key}_art_other_target")] = artOtherOptionKey
                } else {
                    prefs.remove(stringPreferencesKey("${trigger.key}_art_other_target"))
                }

                prefs[stringPreferencesKey("${trigger.key}_title")] = titleAction.name
                if (titleOtherOptionKey != null) {
                    prefs[stringPreferencesKey("${trigger.key}_title_other_target")] = titleOtherOptionKey
                } else {
                    prefs.remove(stringPreferencesKey("${trigger.key}_title_other_target"))
                }
            } else {
                val profiles = Profile.listFromJson(prefs[KEY_PROFILES_JSON]).map { profile ->
                    if (profile.id == activeId) {
                        val updatedMap = profile.overrides.toMutableMap()
                        updatedMap[trigger.key] = action.name
                        updatedMap["${trigger.key}Continuous"] = isContinuous.toString()
                        if (otherOptionKey != null) {
                            updatedMap["${trigger.key}_other_target"] = otherOptionKey
                        } else {
                            updatedMap.remove("${trigger.key}_other_target")
                        }

                        updatedMap["${trigger.key}_art"] = artAction.name
                        if (artOtherOptionKey != null) {
                            updatedMap["${trigger.key}_art_other_target"] = artOtherOptionKey
                        } else {
                            updatedMap.remove("${trigger.key}_art_other_target")
                        }

                        updatedMap["${trigger.key}_title"] = titleAction.name
                        if (titleOtherOptionKey != null) {
                            updatedMap["${trigger.key}_title_other_target"] = titleOtherOptionKey
                        } else {
                            updatedMap.remove("${trigger.key}_title_other_target")
                        }
                        profile.copy(overrides = updatedMap)
                    } else profile
                }
                prefs[KEY_PROFILES_JSON] = Profile.listToJson(profiles)
            }
        }
    }

    fun getRadialOtherOptionFlow(triggerKey: String, slotIndex: Int): Flow<String?> {
        return context.dataStore.data.map { prefs ->
            prefs[stringPreferencesKey("radial_target_${triggerKey}_$slotIndex")]
        }
    }

    suspend fun updateRadialOtherOption(triggerKey: String, slotIndex: Int, targetKey: String?) {
        context.dataStore.edit { prefs ->
            if (targetKey != null) {
                prefs[stringPreferencesKey("radial_target_${triggerKey}_$slotIndex")] = targetKey
            } else {
                prefs.remove(stringPreferencesKey("radial_target_${triggerKey}_$slotIndex"))
            }
        }
    }

    suspend fun toggleOtherOption(triggerKey: String, optionKey: String) {
        val option = ConfigOption.findByKey(optionKey) ?: return
        context.dataStore.edit { prefs ->
            val activeId = prefs[KEY_ACTIVE_PROFILE_ID] ?: Profile.DEFAULT_ID
            if (activeId != Profile.DEFAULT_ID) {
                val profiles = Profile.listFromJson(prefs[KEY_PROFILES_JSON]).map { profile ->
                    if (profile.id == activeId) {
                        val updatedMap = profile.overrides.toMutableMap()
                        if (option.isBooleanToggle) {
                            val currentVal = profile.overrides[option.key]?.toBooleanStrictOrNull()
                                ?: profile.overrides[option.key.removePrefix("DISPLAY_").removePrefix("THEME_").removePrefix("LIBRARY_").removePrefix("AUTO_")]?.toBooleanStrictOrNull()
                                ?: false
                            updatedMap[option.key] = (!currentVal).toString()
                            updatedMap[option.key.removePrefix("DISPLAY_").removePrefix("THEME_").removePrefix("LIBRARY_").removePrefix("AUTO_")] = (!currentVal).toString()
                        } else if (option.targetValue != null) {
                            updatedMap[option.key] = option.targetValue
                            when {
                                option.key.startsWith("THEME_") -> {
                                    updatedMap["THEME_currentThemeName"] = option.targetValue
                                    updatedMap["currentThemeName"] = option.targetValue
                                }
                                option.key.startsWith("ALIGN_ARTIST_") -> {
                                    updatedMap["ALIGN_ARTIST"] = option.targetValue
                                    updatedMap["artistAlignment"] = option.targetValue
                                }
                                option.key.startsWith("ALIGN_SONG_") -> {
                                    updatedMap["ALIGN_SONG"] = option.targetValue
                                    updatedMap["songAlignment"] = option.targetValue
                                }
                                option.key.startsWith("ALIGN_ALBUM_") -> {
                                    updatedMap["ALIGN_ALBUM"] = option.targetValue
                                    updatedMap["albumAlignment"] = option.targetValue
                                }
                                option.key.startsWith("ART_SCALE_") -> {
                                    updatedMap["albumArtScale"] = option.targetValue
                                }
                                option.key.startsWith("ART_LAYOUT_") -> {
                                    updatedMap["DISPLAY_artDisplayLayout"] = option.targetValue
                                    updatedMap["artDisplayLayout"] = option.targetValue
                                }
                                option.key.startsWith("ART_ALIGN_PORT_") -> {
                                    updatedMap["artAlignmentPortrait"] = option.targetValue
                                }
                                option.key.startsWith("ART_ALIGN_LAND_") -> {
                                    updatedMap["artAlignmentLandscape"] = option.targetValue
                                }
                                option.key.startsWith("HUD_TYPE_") -> {
                                    updatedMap["hudType"] = option.targetValue
                                }
                                option.key.startsWith("SCRUB_HUD_TYPE_") -> {
                                    updatedMap["scrubHudType"] = option.targetValue
                                }
                            }
                        }
                        profile.copy(overrides = updatedMap)
                    } else profile
                }
                prefs[KEY_PROFILES_JSON] = Profile.listToJson(profiles)
                return@edit
            }

            if (option.isBooleanToggle) {
                when (option.key) {
                    "DISPLAY_showAlbumArt" -> prefs[KEY_SHOW_ALBUM_ART] = !(prefs[KEY_SHOW_ALBUM_ART] ?: true)
                    "DISPLAY_albumArtColors" -> prefs[KEY_ALBUM_ART_COLORS] = !(prefs[KEY_ALBUM_ART_COLORS] ?: true)
                    "DISPLAY_titleShrinkInPortrait" -> prefs[KEY_TITLE_SHRINK_PORTRAIT] = !(prefs[KEY_TITLE_SHRINK_PORTRAIT] ?: true)
                    "DISPLAY_titleShrinkLong" -> prefs[KEY_TITLE_SHRINK_LONG] = !(prefs[KEY_TITLE_SHRINK_LONG] ?: true)
                    "DISPLAY_titleScrollLong" -> prefs[KEY_TITLE_SCROLL_LONG] = !(prefs[KEY_TITLE_SCROLL_LONG] ?: false)
                    "DISPLAY_volumeAlwaysOn" -> prefs[KEY_VOLUME_ALWAYS_ON] = !(prefs[KEY_VOLUME_ALWAYS_ON] ?: true)
                    "DISPLAY_showStatusBar" -> prefs[KEY_SHOW_STATUS_BAR] = !(prefs[KEY_SHOW_STATUS_BAR] ?: false)
                    "DISPLAY_showActions" -> prefs[KEY_SHOW_ACTIONS] = !(prefs[KEY_SHOW_ACTIONS] ?: true)
                    "DISPLAY_keepScreenOn" -> prefs[KEY_KEEP_SCREEN_ON] = !(prefs[KEY_KEEP_SCREEN_ON] ?: true)
                    "DISPLAY_immersiveMode" -> prefs[KEY_IMMERSIVE_MODE] = !(prefs[KEY_IMMERSIVE_MODE] ?: true)
                    "DISPLAY_artistBold" -> prefs[KEY_ARTIST_BOLD] = !(prefs[KEY_ARTIST_BOLD] ?: true)
                    "DISPLAY_artistItalic" -> prefs[KEY_ARTIST_ITALIC] = !(prefs[KEY_ARTIST_ITALIC] ?: false)
                    "DISPLAY_artistUnderline" -> prefs[KEY_ARTIST_UNDERLINE] = !(prefs[KEY_ARTIST_UNDERLINE] ?: false)
                    "DISPLAY_songBold" -> prefs[KEY_SONG_BOLD] = !(prefs[KEY_SONG_BOLD] ?: true)
                    "DISPLAY_songItalic" -> prefs[KEY_SONG_ITALIC] = !(prefs[KEY_SONG_ITALIC] ?: false)
                    "DISPLAY_songUnderline" -> prefs[KEY_SONG_UNDERLINE] = !(prefs[KEY_SONG_UNDERLINE] ?: false)
                    "DISPLAY_albumBold" -> prefs[KEY_ALBUM_BOLD] = !(prefs[KEY_ALBUM_BOLD] ?: false)
                    "DISPLAY_albumItalic" -> prefs[KEY_ALBUM_ITALIC] = !(prefs[KEY_ALBUM_ITALIC] ?: false)
                    "DISPLAY_albumUnderline" -> prefs[KEY_ALBUM_UNDERLINE] = !(prefs[KEY_ALBUM_UNDERLINE] ?: false)
                    "THEME_dimAtNight" -> prefs[KEY_DIM_AT_NIGHT] = !(prefs[KEY_DIM_AT_NIGHT] ?: true)
                    "THEME_invertAtNight" -> prefs[KEY_INVERT_AT_NIGHT] = !(prefs[KEY_INVERT_AT_NIGHT] ?: false)
                    "THEME_isRounded" -> prefs[KEY_THEME_ROUNDED] = !(prefs[KEY_THEME_ROUNDED] ?: false)
                    "THEME_isGlass" -> prefs[KEY_THEME_GLASS] = !(prefs[KEY_THEME_GLASS] ?: false)
                    "LIBRARY_autoRescan" -> prefs[KEY_AUTO_RESCAN] = !(prefs[KEY_AUTO_RESCAN] ?: false)
                    "LIBRARY_gpsVolume" -> prefs[KEY_GPS_VOLUME] = !(prefs[KEY_GPS_VOLUME] ?: false)
                    "AUTO_drivingMode" -> {
                        val current = prefs[KEY_DRIVING_MODE_ENABLED] ?: false
                        val next = !current
                        prefs[KEY_DRIVING_MODE_ENABLED] = next
                        if (!next) {
                            prefs[KEY_AUTO_SPEED_VOLUME_ENABLED] = false
                            prefs[KEY_AUTO_AMBIENT_NOISE_ENABLED] = false
                        }
                    }
                    "AUTO_autoEnableDrivingMode" -> prefs[KEY_AUTO_ENABLE_DRIVING_MODE] = !(prefs[KEY_AUTO_ENABLE_DRIVING_MODE] ?: false)
                    "AUTO_speedVolume" -> {
                        val current = prefs[KEY_AUTO_SPEED_VOLUME_ENABLED] ?: false
                        val next = !current
                        prefs[KEY_AUTO_SPEED_VOLUME_ENABLED] = next
                        if (next) {
                            prefs[KEY_DRIVING_MODE_ENABLED] = true
                        }
                    }
                    "AUTO_ambientNoise" -> {
                        val current = prefs[KEY_AUTO_AMBIENT_NOISE_ENABLED] ?: false
                        val next = !current
                        prefs[KEY_AUTO_AMBIENT_NOISE_ENABLED] = next
                        if (next) {
                            prefs[KEY_DRIVING_MODE_ENABLED] = true
                        }
                    }
                }
            } else {
                val priorKey = stringPreferencesKey("${triggerKey}_prior_value")
                val priorArtColorsKey = booleanPreferencesKey("${triggerKey}_prior_art_colors")
                val targetVal = option.targetValue ?: return@edit

                fun isSameThemeName(a: String?, b: String?): Boolean {
                    if ((a == null) || (b == null)) return false
                    if (a.equals(b, ignoreCase = true)) return true
                    val isAMatch = a.equals("Match Album Art", ignoreCase = true) || a.equals("Auto By Art", ignoreCase = true)
                    val isBMatch = b.equals("Match Album Art", ignoreCase = true) || b.equals("Auto By Art", ignoreCase = true)
                    return isAMatch && isBMatch
                }

                when {
                    option.key.startsWith("THEME_") -> {
                        val currentVal = prefs[KEY_CURRENT_THEME] ?: ColorTheme.MATCH_ALBUM_ART.name
                        val currentArtColors = prefs[KEY_ALBUM_ART_COLORS] ?: true
                        val isCurrentTargetMatch = isSameThemeName(currentVal, targetVal)
                        if (isCurrentTargetMatch) {
                            var priorVal = prefs[priorKey]
                            if ((priorVal == null) || isSameThemeName(priorVal, targetVal)) {
                                val globalPrior = prefs[KEY_PRIOR_THEME]
                                priorVal = if ((globalPrior != null) && !isSameThemeName(globalPrior, targetVal)) {
                                    globalPrior
                                } else {
                                    if (targetVal.equals("Match Album Art", ignoreCase = true) || targetVal.equals("Auto By Art", ignoreCase = true)) {
                                        ColorTheme.WHITE_ON_GREY.name
                                    } else {
                                        ColorTheme.MATCH_ALBUM_ART.name
                                    }
                                }
                            }
                            val priorArtColors = prefs[priorArtColorsKey] ?: (priorVal.equals("Match Album Art", true) || priorVal.equals("Auto By Art", true))
                            prefs[KEY_CURRENT_THEME] = priorVal
                            prefs[KEY_ALBUM_ART_COLORS] = priorArtColors
                            prefs[priorKey] = currentVal
                            prefs[priorArtColorsKey] = currentArtColors
                            prefs[KEY_PRIOR_THEME] = currentVal
                        } else {
                            prefs[priorKey] = currentVal
                            prefs[priorArtColorsKey] = currentArtColors
                            prefs[KEY_PRIOR_THEME] = currentVal
                            prefs[KEY_CURRENT_THEME] = targetVal
                            prefs[KEY_ALBUM_ART_COLORS] = (targetVal.equals("Match Album Art", true) || targetVal.equals("Auto By Art", true))
                            if (targetVal.equals("Mondrian", ignoreCase = true)) {
                                prefs[KEY_ART_DISPLAY_LAYOUT] = ArtLayoutOption.OVERLAY.ordinal
                            }
                        }
                    }
                    option.key.startsWith("ALIGN_ARTIST_") -> {
                        val currentVal = prefs[KEY_ARTIST_ALIGNMENT] ?: TextAlignmentOption.LEFT.name
                        if (currentVal.equals(targetVal, ignoreCase = true)) {
                            var priorVal = prefs[priorKey]
                            if ((priorVal == null) || priorVal.equals(targetVal, ignoreCase = true)) {
                                priorVal = if (targetVal.equals("LEFT", true)) "CENTER" else "LEFT"
                            }
                            prefs[KEY_ARTIST_ALIGNMENT] = priorVal
                            prefs[priorKey] = currentVal
                        } else {
                            prefs[priorKey] = currentVal
                            prefs[KEY_ARTIST_ALIGNMENT] = targetVal
                        }
                    }
                    option.key.startsWith("ALIGN_SONG_") -> {
                        val currentVal = prefs[KEY_SONG_ALIGNMENT] ?: TextAlignmentOption.CENTER.name
                        if (currentVal.equals(targetVal, ignoreCase = true)) {
                            var priorVal = prefs[priorKey]
                            if (priorVal == null || priorVal.equals(targetVal, ignoreCase = true)) {
                                priorVal = if (targetVal.equals("CENTER", true)) "LEFT" else "CENTER"
                            }
                            prefs[KEY_SONG_ALIGNMENT] = priorVal
                            prefs[priorKey] = currentVal
                        } else {
                            prefs[priorKey] = currentVal
                            prefs[KEY_SONG_ALIGNMENT] = targetVal
                        }
                    }
                    option.key.startsWith("ALIGN_ALBUM_") -> {
                        val currentVal = prefs[KEY_ALBUM_ALIGNMENT] ?: TextAlignmentOption.RIGHT.name
                        if (currentVal.equals(targetVal, ignoreCase = true)) {
                            var priorVal = prefs[priorKey]
                            if (priorVal == null || priorVal.equals(targetVal, ignoreCase = true)) {
                                priorVal = if (targetVal.equals("RIGHT", true)) "LEFT" else "RIGHT"
                            }
                            prefs[KEY_ALBUM_ALIGNMENT] = priorVal
                            prefs[priorKey] = currentVal
                        } else {
                            prefs[priorKey] = currentVal
                            prefs[KEY_ALBUM_ALIGNMENT] = targetVal
                        }
                    }
                    option.key.startsWith("ART_SCALE_") -> {
                        val currentOrdinal = prefs[KEY_ALBUM_ART_SCALE] ?: 0
                        val currentName = ArtScaleOption.entries.getOrNull(currentOrdinal)?.name ?: ArtScaleOption.FILL_SCREEN.name
                        if (currentName.equals(targetVal, ignoreCase = true)) {
                            var priorVal = prefs[priorKey]
                            if (priorVal == null || priorVal.equals(targetVal, ignoreCase = true)) {
                                priorVal = if (targetVal.equals("FILL_SCREEN", true)) ArtScaleOption.ASPECT_FIT.name else ArtScaleOption.FILL_SCREEN.name
                            }
                            val newOrdinal = ArtScaleOption.entries.find { it.name.equals(priorVal, true) }?.ordinal ?: 0
                            prefs[KEY_ALBUM_ART_SCALE] = newOrdinal
                            prefs[priorKey] = currentName
                        } else {
                            prefs[priorKey] = currentName
                            val newOrdinal = ArtScaleOption.entries.find { it.name.equals(targetVal, true) }?.ordinal ?: 0
                            prefs[KEY_ALBUM_ART_SCALE] = newOrdinal
                        }
                    }
                    option.key.startsWith("ART_LAYOUT_") -> {
                        val currentOrdinal = prefs[KEY_ART_DISPLAY_LAYOUT] ?: 0
                        val currentName = ArtLayoutOption.entries.getOrNull(currentOrdinal)?.name ?: ArtLayoutOption.OVERLAY.name
                        val targetOrdinal = if (currentName.equals(targetVal, ignoreCase = true)) {
                            var priorVal = prefs[priorKey]
                            if (priorVal == null || priorVal.equals(targetVal, ignoreCase = true)) {
                                priorVal = if (targetVal.equals("OVERLAY", true)) ArtLayoutOption.DOCKED.name else ArtLayoutOption.OVERLAY.name
                            }
                            prefs[priorKey] = currentName
                            ArtLayoutOption.entries.find { it.name.equals(priorVal, true) }?.ordinal ?: 0
                        } else {
                            prefs[priorKey] = currentName
                            ArtLayoutOption.entries.find { it.name.equals(targetVal, true) }?.ordinal ?: 0
                        }
                        prefs[KEY_ART_DISPLAY_LAYOUT] = targetOrdinal
                    }
                    option.key.startsWith("ART_ALIGN_PORT_") -> {
                        val currentVal = prefs[KEY_ART_ALIGNMENT_PORTRAIT] ?: ArtAlignmentPortrait.MIDDLE.name
                        if (currentVal.equals(targetVal, ignoreCase = true)) {
                            var priorVal = prefs[priorKey]
                            if (priorVal == null || priorVal.equals(targetVal, ignoreCase = true)) {
                                priorVal = if (targetVal.equals("MIDDLE", true)) ArtAlignmentPortrait.TOP.name else ArtAlignmentPortrait.MIDDLE.name
                            }
                            prefs[KEY_ART_ALIGNMENT_PORTRAIT] = priorVal
                            prefs[priorKey] = currentVal
                        } else {
                            prefs[priorKey] = currentVal
                            prefs[KEY_ART_ALIGNMENT_PORTRAIT] = targetVal
                        }
                    }
                    option.key.startsWith("ART_ALIGN_LAND_") -> {
                        val currentVal = prefs[KEY_ART_ALIGNMENT_LANDSCAPE] ?: ArtAlignmentLandscape.CENTER.name
                        if (currentVal.equals(targetVal, ignoreCase = true)) {
                            var priorVal = prefs[priorKey]
                            if (priorVal == null || priorVal.equals(targetVal, ignoreCase = true)) {
                                priorVal = if (targetVal.equals("CENTER", true)) ArtAlignmentLandscape.LEFT.name else ArtAlignmentLandscape.CENTER.name
                            }
                            prefs[KEY_ART_ALIGNMENT_LANDSCAPE] = priorVal
                            prefs[priorKey] = currentVal
                        } else {
                            prefs[priorKey] = currentVal
                            prefs[KEY_ART_ALIGNMENT_LANDSCAPE] = targetVal
                        }
                    }
                    option.key.startsWith("HUD_TYPE_") -> {
                        val currentValInt = prefs[KEY_HUD_TYPE] ?: 1
                        val currentOption = HudTypeOption.entries.find { it.value == currentValInt } ?: HudTypeOption.BAR_VOLUME
                        if (currentOption.name.equals(targetVal, ignoreCase = true)) {
                            var priorVal = prefs[priorKey]
                            if (priorVal == null || priorVal.equals(targetVal, ignoreCase = true)) {
                                priorVal = if (targetVal.equals("BAR_VOLUME", true)) HudTypeOption.NONE.name else HudTypeOption.BAR_VOLUME.name
                            }
                            val targetHud = HudTypeOption.entries.find { it.name.equals(priorVal, true) } ?: HudTypeOption.BAR_VOLUME
                            prefs[KEY_HUD_TYPE] = targetHud.value
                            prefs[priorKey] = currentOption.name
                        } else {
                            prefs[priorKey] = currentOption.name
                            val targetHud = HudTypeOption.entries.find { it.name.equals(targetVal, true) } ?: HudTypeOption.BAR_VOLUME
                            prefs[KEY_HUD_TYPE] = targetHud.value
                        }
                    }
                    option.key.startsWith("SCRUB_HUD_TYPE_") -> {
                        val currentValInt = prefs[KEY_SCRUB_HUD_TYPE] ?: 2
                        val currentOption = ScrubHudTypeOption.entries.find { it.value == currentValInt } ?: ScrubHudTypeOption.EDGE_HUD
                        if (currentOption.name.equals(targetVal, ignoreCase = true)) {
                            var priorVal = prefs[priorKey]
                            if (priorVal == null || priorVal.equals(targetVal, ignoreCase = true)) {
                                priorVal = if (targetVal.equals("EDGE_HUD", true)) ScrubHudTypeOption.NONE.name else ScrubHudTypeOption.EDGE_HUD.name
                            }
                            val targetHud = ScrubHudTypeOption.entries.find { it.name.equals(priorVal, true) } ?: ScrubHudTypeOption.EDGE_HUD
                            prefs[KEY_SCRUB_HUD_TYPE] = targetHud.value
                            prefs[priorKey] = currentOption.name
                        } else {
                            prefs[priorKey] = currentOption.name
                            val targetHud = ScrubHudTypeOption.entries.find { it.name.equals(targetVal, true) } ?: ScrubHudTypeOption.EDGE_HUD
                            prefs[KEY_SCRUB_HUD_TYPE] = targetHud.value
                        }
                    }
                }
            }
        }
    }

    suspend fun updateDisplaySettings(update: DisplaySettings) {
        context.dataStore.edit { prefs ->
            val activeId = prefs[KEY_ACTIVE_PROFILE_ID] ?: Profile.DEFAULT_ID
            if (activeId != Profile.DEFAULT_ID) {
                val profiles = Profile.listFromJson(prefs[KEY_PROFILES_JSON]).map { profile ->
                    if (profile.id == activeId) {
                        val updatedMap = profile.overrides.toMutableMap()
                        updatedMap["DISPLAY_artDisplayLayout"] = update.artDisplayLayout.name
                        updatedMap["artDisplayLayout"] = update.artDisplayLayout.name
                        updatedMap["AUTO_speedVolume"] = update.autoSpeedVolumeEnabled.toString()
                        updatedMap["autoSpeedVolumeEnabled"] = update.autoSpeedVolumeEnabled.toString()
                        updatedMap["AUTO_ambientNoise"] = update.autoAmbientNoiseEnabled.toString()
                        updatedMap["autoAmbientNoiseEnabled"] = update.autoAmbientNoiseEnabled.toString()
                        updatedMap["AUTO_drivingMode"] = update.drivingModeEnabled.toString()
                        updatedMap["drivingModeEnabled"] = update.drivingModeEnabled.toString()
                        profile.copy(overrides = updatedMap)
                    } else profile
                }
                prefs[KEY_PROFILES_JSON] = Profile.listToJson(profiles)
            }
            prefs[KEY_ARTIST_FONT_SIZE] = update.artistFontSize
            prefs[KEY_SONG_FONT_SIZE] = update.songFontSize
            prefs[KEY_ALBUM_FONT_SIZE] = update.albumFontSize
            prefs[KEY_ARTIST_ALIGNMENT] = update.artistAlignment.name
            prefs[KEY_SONG_ALIGNMENT] = update.songAlignment.name
            prefs[KEY_ALBUM_ALIGNMENT] = update.albumAlignment.name
            prefs[KEY_MINIMUM_FONT_SIZE] = update.minimumFontSize
            prefs[KEY_TITLE_SHRINK_PORTRAIT] = update.titleShrinkInPortrait
            prefs[KEY_TITLE_SHRINK_LONG] = update.titleShrinkLong
            prefs[KEY_TITLE_SCROLL_LONG] = update.titleScrollLong
            prefs[KEY_SHOW_ALBUM_ART] = update.showAlbumArt
            prefs[KEY_ALBUM_ART_COLORS] = update.albumArtColors
            prefs[KEY_ALBUM_ART_SCALE] = update.albumArtScale.ordinal
            prefs[KEY_ART_ALIGNMENT_PORTRAIT] = update.artAlignmentPortrait.name
            prefs[KEY_ART_ALIGNMENT_LANDSCAPE] = update.artAlignmentLandscape.name
            prefs[KEY_ALBUM_ART_FADE] = update.albumArtFade
            prefs[KEY_ART_DISPLAY_LAYOUT] = update.artDisplayLayout.ordinal
            prefs[KEY_STRETCH_ART] = update.stretchArt
            prefs[KEY_MATCH_ART_COLOR_PRIORITY] = update.matchArtColorPriority.ordinal
            prefs[KEY_ADAPTIVE_DOCKED_ART] = update.adaptiveDockedArt
            prefs[KEY_SEPARATE_TOUCH_ZONES] = update.separateTouchZones
            prefs[KEY_HUD_TYPE] = update.hudType.value
            prefs[KEY_SCRUB_HUD_TYPE] = update.scrubHudType.value
            prefs[KEY_VOLUME_ALWAYS_ON] = update.volumeAlwaysOn
            prefs[KEY_SHOW_STATUS_BAR] = update.showStatusBar
            prefs[KEY_SHOW_ACTIONS] = update.showActions
            prefs[KEY_HUD_LINE_THICKNESS] = update.hudLineThickness
            prefs[KEY_ARTIST_FONT_KEY] = update.artistFontKey
            prefs[KEY_SONG_FONT_KEY] = update.songFontKey
            prefs[KEY_ALBUM_FONT_KEY] = update.albumFontKey
            prefs[KEY_ARTIST_BOLD] = update.artistBold
            prefs[KEY_ARTIST_ITALIC] = update.artistItalic
            prefs[KEY_ARTIST_UNDERLINE] = update.artistUnderline
            prefs[KEY_SONG_BOLD] = update.songBold
            prefs[KEY_SONG_ITALIC] = update.songItalic
            prefs[KEY_SONG_UNDERLINE] = update.songUnderline
            prefs[KEY_ALBUM_BOLD] = update.albumBold
            prefs[KEY_ALBUM_ITALIC] = update.albumItalic
            prefs[KEY_ALBUM_UNDERLINE] = update.albumUnderline
            prefs[KEY_KEEP_SCREEN_ON] = update.keepScreenOn
            prefs[KEY_IMMERSIVE_MODE] = update.immersiveMode
            prefs[KEY_NUM_EDGE_REGIONS] = update.numEdgeRegions
            prefs[KEY_NUM_ART_EDGE_REGIONS] = update.numArtEdgeRegions
            prefs[KEY_TITLE_ORDER] = update.titleOrder.joinToString(",") { it.name }
            prefs[KEY_AUTO_CATEGORY_ORDER] = update.autoCategoryOrder.joinToString(",") { it.name }
            prefs[KEY_AUTO_SHOW_ALBUM_ART] = update.autoShowAlbumArt
            prefs[KEY_AUTO_ALBUM_STYLE_GRID] = update.autoAlbumStyleGrid
            prefs[KEY_AUTO_ARTIST_STYLE_GRID] = update.autoArtistStyleGrid
            prefs[KEY_AUTO_AUTOPLAY_ON_CONNECT] = update.autoAutoplayOnConnect
            prefs[KEY_AUTO_VOICE_SEARCH] = update.autoVoiceSearch
            prefs[KEY_AUTO_SPEED_VOLUME_ENABLED] = update.autoSpeedVolumeEnabled
            prefs[KEY_AUTO_AMBIENT_NOISE_ENABLED] = update.autoAmbientNoiseEnabled
            prefs[KEY_AUTO_DEFAULT_VOLUME] = update.autoDefaultVolume
            prefs[KEY_AUTO_MIN_SPEED_THRESHOLD] = update.autoMinSpeedThreshold
            prefs[KEY_AUTO_SPEED_VOLUME_RATIO] = update.autoSpeedVolumeRatio
            prefs[KEY_AUTO_SPEED_UNIT] = update.autoSpeedUnit
            prefs[KEY_DRIVING_MODE_ENABLED] = update.drivingModeEnabled
            prefs[KEY_AUTO_ENABLE_DRIVING_MODE] = update.autoEnableDrivingMode
            prefs[KEY_AUTO_ACTION_BUTTON_ORDER] = update.autoActionButtonOrder.joinToString(",") { it.name }
        }
    }

    suspend fun updateThemeSettings(update: ThemeSettings) {
        context.dataStore.edit { prefs ->
            val oldTheme = prefs[KEY_CURRENT_THEME] ?: ColorTheme.MATCH_ALBUM_ART.name
            if (!oldTheme.equals(update.currentThemeName, ignoreCase = true)) {
                prefs[KEY_PRIOR_THEME] = oldTheme
            }
            if (update.currentThemeName.equals("Mondrian", ignoreCase = true)) {
                prefs[KEY_ART_DISPLAY_LAYOUT] = ArtLayoutOption.OVERLAY.ordinal
            }
            prefs[KEY_CURRENT_THEME] = update.currentThemeName
            prefs[KEY_CUSTOM_TEXT_RED] = update.customTextRed
            prefs[KEY_CUSTOM_TEXT_GREEN] = update.customTextGreen
            prefs[KEY_CUSTOM_TEXT_BLUE] = update.customTextBlue
            prefs[KEY_CUSTOM_SONG_TITLE_RED] = update.customSongTitleRed
            prefs[KEY_CUSTOM_SONG_TITLE_GREEN] = update.customSongTitleGreen
            prefs[KEY_CUSTOM_SONG_TITLE_BLUE] = update.customSongTitleBlue
            prefs[KEY_CUSTOM_ARTIST_TITLE_RED] = update.customArtistTitleRed
            prefs[KEY_CUSTOM_ARTIST_TITLE_GREEN] = update.customArtistTitleGreen
            prefs[KEY_CUSTOM_ARTIST_TITLE_BLUE] = update.customArtistTitleBlue
            prefs[KEY_CUSTOM_ALBUM_TITLE_RED] = update.customAlbumTitleRed
            prefs[KEY_CUSTOM_ALBUM_TITLE_GREEN] = update.customAlbumTitleGreen
            prefs[KEY_CUSTOM_ALBUM_TITLE_BLUE] = update.customAlbumTitleBlue
            prefs[KEY_CUSTOM_BG_RED] = update.customBGRed
            prefs[KEY_CUSTOM_BG_GREEN] = update.customBGGreen
            prefs[KEY_CUSTOM_BG_BLUE] = update.customBGBlue
            prefs[KEY_DIM_AT_NIGHT] = update.dimAtNight
            prefs[KEY_INVERT_AT_NIGHT] = update.invertAtNight
            prefs[KEY_SUNRISE_HOUR] = update.sunRiseHour
            prefs[KEY_SUNSET_HOUR] = update.sunSetHour
            prefs[KEY_THEME_ROUNDED] = update.isRounded
            prefs[KEY_THEME_GLASS] = update.isGlass
        }
    }

    suspend fun resetAllGestureBindings() {
        context.dataStore.edit { prefs ->
            GestureTrigger.entries.forEach { trigger ->
                prefs[stringPreferencesKey(trigger.key)] = trigger.defaultActionKey
                prefs[booleanPreferencesKey("${trigger.key}Continuous")] = trigger.isContinuousDefault
                prefs.remove(stringPreferencesKey("${trigger.key}_other_target"))
                prefs.remove(stringPreferencesKey("${trigger.key}_art"))
                prefs.remove(stringPreferencesKey("${trigger.key}_art_other_target"))
                prefs.remove(stringPreferencesKey("${trigger.key}_title"))
                prefs.remove(stringPreferencesKey("${trigger.key}_title_other_target"))
                prefs.remove(stringPreferencesKey("radial_actions_${trigger.key}"))
            }
        }
    }

    suspend fun updateRadialMenuActions(triggerKey: String, actions: List<GestureAction>) {
        val validActions = actions.take(12)
        val strValue = validActions.joinToString(",") { it.name }
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey("radial_actions_$triggerKey")] = strValue
        }
    }

    suspend fun resetRadialMenuActions(triggerKey: String) {
        context.dataStore.edit { prefs ->
            prefs.remove(stringPreferencesKey("radial_actions_$triggerKey"))
        }
    }

    suspend fun exportSettingsToJson(): String {
        val display = displaySettingsFlow.first()
        val theme = themeSettingsFlow.first()
        val bindings = gestureBindingsFlow.first()
        val gpsVolume = gpsVolumeEnabledFlow.first()
        val gpsSens = gpsSensitivityFlow.first()
        val autoRescan = autoRescanFlow.first()

        return SettingsBackupHelper.exportToJson(
            display = display,
            theme = theme,
            bindings = bindings,
            gpsVolume = gpsVolume,
            gpsSens = gpsSens,
            autoRescan = autoRescan
        )
    }

    suspend fun importSettingsFromJson(jsonString: String): Boolean {
        return try {
            val json = org.json.JSONObject(jsonString)

            if (json.has("profiles")) {
                val profilesArray = json.getJSONArray("profiles")
                val importedProfiles = Profile.listFromJson(profilesArray.toString())
                context.dataStore.edit { prefs ->
                    prefs[KEY_PROFILES_JSON] = Profile.listToJson(importedProfiles)
                }
            }
            if (json.has("activeProfileId")) {
                val activeId = json.getString("activeProfileId")
                context.dataStore.edit { prefs ->
                    prefs[KEY_ACTIVE_PROFILE_ID] = activeId
                }
            }

            if (json.has("displaySettings")) {
                val dJson = json.getJSONObject("displaySettings")
                val currentDisplay = displaySettingsFlow.first()
                val newDisplay = currentDisplay.copy(
                    artistFontSize = dJson.optDouble("artistFontSize", currentDisplay.artistFontSize.toDouble()).toFloat(),
                    songFontSize = dJson.optDouble("songFontSize", currentDisplay.songFontSize.toDouble()).toFloat(),
                    albumFontSize = dJson.optDouble("albumFontSize", currentDisplay.albumFontSize.toDouble()).toFloat(),
                    artistAlignment = runCatching { TextAlignmentOption.valueOf(dJson.getString("artistAlignment")) }.getOrDefault(currentDisplay.artistAlignment),
                    songAlignment = runCatching { TextAlignmentOption.valueOf(dJson.getString("songAlignment")) }.getOrDefault(currentDisplay.songAlignment),
                    albumAlignment = runCatching { TextAlignmentOption.valueOf(dJson.getString("albumAlignment")) }.getOrDefault(currentDisplay.albumAlignment),
                    minimumFontSize = dJson.optDouble("minimumFontSize", currentDisplay.minimumFontSize.toDouble()).toFloat(),
                    titleShrinkInPortrait = dJson.optBoolean("titleShrinkInPortrait", currentDisplay.titleShrinkInPortrait),
                    titleShrinkLong = dJson.optBoolean("titleShrinkLong", currentDisplay.titleShrinkLong),
                    titleScrollLong = dJson.optBoolean("titleScrollLong", currentDisplay.titleScrollLong),
                    showAlbumArt = dJson.optBoolean("showAlbumArt", currentDisplay.showAlbumArt),
                    albumArtColors = dJson.optBoolean("albumArtColors", currentDisplay.albumArtColors),
                    albumArtScale = runCatching { ArtScaleOption.valueOf(dJson.getString("albumArtScale")) }.getOrDefault(currentDisplay.albumArtScale),
                    artAlignmentPortrait = runCatching { ArtAlignmentPortrait.valueOf(dJson.getString("artAlignmentPortrait")) }.getOrDefault(currentDisplay.artAlignmentPortrait),
                    artAlignmentLandscape = runCatching { ArtAlignmentLandscape.valueOf(dJson.getString("artAlignmentLandscape")) }.getOrDefault(currentDisplay.artAlignmentLandscape),
                    albumArtFade = dJson.optDouble("albumArtFade", currentDisplay.albumArtFade.toDouble()).toFloat(),
                    artDisplayLayout = runCatching { ArtLayoutOption.valueOf(dJson.getString("artDisplayLayout")) }.getOrDefault(currentDisplay.artDisplayLayout),
                    stretchArt = dJson.optBoolean("stretchArt", currentDisplay.stretchArt),
                    matchArtColorPriority = runCatching { ArtColorPriority.valueOf(dJson.getString("matchArtColorPriority")) }.getOrDefault(currentDisplay.matchArtColorPriority),
                    adaptiveDockedArt = dJson.optBoolean("adaptiveDockedArt", currentDisplay.adaptiveDockedArt),
                    separateTouchZones = dJson.optBoolean("separateTouchZones", currentDisplay.separateTouchZones),
                    hudType = runCatching { HudTypeOption.valueOf(dJson.getString("hudType")) }.getOrDefault(currentDisplay.hudType),
                    scrubHudType = runCatching { ScrubHudTypeOption.valueOf(dJson.getString("scrubHudType")) }.getOrDefault(currentDisplay.scrubHudType),
                    volumeAlwaysOn = dJson.optBoolean("volumeAlwaysOn", currentDisplay.volumeAlwaysOn),
                    showStatusBar = dJson.optBoolean("showStatusBar", currentDisplay.showStatusBar),
                    showActions = dJson.optBoolean("showActions", currentDisplay.showActions),
                    hudLineThickness = dJson.optDouble("hudLineThickness", currentDisplay.hudLineThickness.toDouble()).toFloat(),
                    artistFontKey = dJson.optString("artistFontKey", currentDisplay.artistFontKey),
                    songFontKey = dJson.optString("songFontKey", currentDisplay.songFontKey),
                    albumFontKey = dJson.optString("albumFontKey", currentDisplay.albumFontKey),
                    artistBold = dJson.optBoolean("artistBold", currentDisplay.artistBold),
                    artistItalic = dJson.optBoolean("artistItalic", currentDisplay.artistItalic),
                    artistUnderline = dJson.optBoolean("artistUnderline", currentDisplay.artistUnderline),
                    songBold = dJson.optBoolean("songBold", currentDisplay.songBold),
                    songItalic = dJson.optBoolean("songItalic", currentDisplay.songItalic),
                    songUnderline = dJson.optBoolean("songUnderline", currentDisplay.songUnderline),
                    albumBold = dJson.optBoolean("albumBold", currentDisplay.albumBold),
                    albumItalic = dJson.optBoolean("albumItalic", currentDisplay.albumItalic),
                    albumUnderline = dJson.optBoolean("albumUnderline", currentDisplay.albumUnderline),
                    keepScreenOn = dJson.optBoolean("keepScreenOn", currentDisplay.keepScreenOn),
                    immersiveMode = dJson.optBoolean("immersiveMode", currentDisplay.immersiveMode),
                    numEdgeRegions = dJson.optInt("numEdgeRegions", currentDisplay.numEdgeRegions),
                    numArtEdgeRegions = dJson.optInt("numArtEdgeRegions", currentDisplay.numArtEdgeRegions),
                    titleOrder = dJson.optString("titleOrder", "")
                        .split(",")
                        .mapNotNull { name -> runCatching { TitleRowType.valueOf(name.trim()) }.getOrNull() }
                        .ifEmpty { currentDisplay.titleOrder },
                    autoCategoryOrder = dJson.optString("autoCategoryOrder", "")
                        .split(",")
                        .mapNotNull { name -> runCatching { AutoCategory.valueOf(name.trim()) }.getOrNull() }
                        .ifEmpty { currentDisplay.autoCategoryOrder },
                    autoShowAlbumArt = dJson.optBoolean("autoShowAlbumArt", currentDisplay.autoShowAlbumArt),
                    autoAlbumStyleGrid = dJson.optBoolean("autoAlbumStyleGrid", currentDisplay.autoAlbumStyleGrid),
                    autoArtistStyleGrid = dJson.optBoolean("autoArtistStyleGrid", currentDisplay.autoArtistStyleGrid),
                    autoAutoplayOnConnect = dJson.optBoolean("autoAutoplayOnConnect", currentDisplay.autoAutoplayOnConnect),
                    autoVoiceSearch = dJson.optBoolean("autoVoiceSearch", currentDisplay.autoVoiceSearch),
                    autoSpeedVolumeEnabled = dJson.optBoolean("autoSpeedVolumeEnabled", currentDisplay.autoSpeedVolumeEnabled),
                    autoAmbientNoiseEnabled = dJson.optBoolean("autoAmbientNoiseEnabled", currentDisplay.autoAmbientNoiseEnabled),
                    autoDefaultVolume = dJson.optInt("autoDefaultVolume", currentDisplay.autoDefaultVolume),
                    autoMinSpeedThreshold = dJson.optDouble("autoMinSpeedThreshold", currentDisplay.autoMinSpeedThreshold.toDouble()).toFloat(),
                    autoSpeedVolumeRatio = dJson.optDouble("autoSpeedVolumeRatio", currentDisplay.autoSpeedVolumeRatio.toDouble()).toFloat(),
                    autoSpeedUnit = dJson.optString("autoSpeedUnit", currentDisplay.autoSpeedUnit),
                    drivingModeEnabled = dJson.optBoolean("drivingModeEnabled", currentDisplay.drivingModeEnabled),
                    autoEnableDrivingMode = dJson.optBoolean("autoEnableDrivingMode", currentDisplay.autoEnableDrivingMode),
                    autoActionButtonOrder = dJson.optString("autoActionButtonOrder", "")
                        .split(",")
                        .asSequence()
                        .mapNotNull { name -> runCatching { GestureAction.valueOf(name.trim()) }.getOrNull() ?: GestureAction.fromKey(name.trim()) }
                        .filter { (it != GestureAction.UNASSIGNED) && (it != GestureAction.OTHER_OPTION) }
                        .toList()
                        .ifEmpty { currentDisplay.autoActionButtonOrder }
                )
                updateDisplaySettings(newDisplay)
            }

            if (json.has("themeSettings")) {
                val tJson = json.getJSONObject("themeSettings")
                val currentTheme = themeSettingsFlow.first()
                val newTheme = currentTheme.copy(
                    currentThemeName = tJson.optString("currentThemeName", currentTheme.currentThemeName),
                    customTextRed = tJson.optDouble("customTextRed", currentTheme.customTextRed.toDouble()).toFloat(),
                    customTextGreen = tJson.optDouble("customTextGreen", currentTheme.customTextGreen.toDouble()).toFloat(),
                    customTextBlue = tJson.optDouble("customTextBlue", currentTheme.customTextBlue.toDouble()).toFloat(),
                    customSongTitleRed = tJson.optDouble("customSongTitleRed", currentTheme.customSongTitleRed.toDouble()).toFloat(),
                    customSongTitleGreen = tJson.optDouble("customSongTitleGreen", currentTheme.customSongTitleGreen.toDouble()).toFloat(),
                    customSongTitleBlue = tJson.optDouble("customSongTitleBlue", currentTheme.customSongTitleBlue.toDouble()).toFloat(),
                    customArtistTitleRed = tJson.optDouble("customArtistTitleRed", currentTheme.customArtistTitleRed.toDouble()).toFloat(),
                    customArtistTitleGreen = tJson.optDouble("customArtistTitleGreen", currentTheme.customArtistTitleGreen.toDouble()).toFloat(),
                    customArtistTitleBlue = tJson.optDouble("customArtistTitleBlue", currentTheme.customArtistTitleBlue.toDouble()).toFloat(),
                    customAlbumTitleRed = tJson.optDouble("customAlbumTitleRed", currentTheme.customAlbumTitleRed.toDouble()).toFloat(),
                    customAlbumTitleGreen = tJson.optDouble("customAlbumTitleGreen", currentTheme.customAlbumTitleGreen.toDouble()).toFloat(),
                    customAlbumTitleBlue = tJson.optDouble("customAlbumTitleBlue", currentTheme.customAlbumTitleBlue.toDouble()).toFloat(),
                    customBGRed = tJson.optDouble("customBGRed", currentTheme.customBGRed.toDouble()).toFloat(),
                    customBGGreen = tJson.optDouble("customBGGreen", currentTheme.customBGGreen.toDouble()).toFloat(),
                    customBGBlue = tJson.optDouble("customBGBlue", currentTheme.customBGBlue.toDouble()).toFloat(),
                    dimAtNight = tJson.optBoolean("dimAtNight", currentTheme.dimAtNight),
                    invertAtNight = tJson.optBoolean("invertAtNight", currentTheme.invertAtNight),
                    sunRiseHour = tJson.optInt("sunRiseHour", currentTheme.sunRiseHour),
                    sunSetHour = tJson.optInt("sunSetHour", currentTheme.sunSetHour),
                    isRounded = tJson.optBoolean("isRounded", currentTheme.isRounded),
                    isGlass = tJson.optBoolean("isGlass", currentTheme.isGlass)
                )
                updateThemeSettings(newTheme)
            }

            if (json.has("gestureBindings")) {
                val gJson = json.getJSONObject("gestureBindings")
                GestureTrigger.entries.forEach { trigger ->
                    if (gJson.has(trigger.key)) {
                        val bJson = gJson.getJSONObject(trigger.key)
                        val actionName = bJson.optString("action", trigger.defaultActionKey)
                        val action = GestureAction.fromKey(actionName)
                        val isContinuous = bJson.optBoolean("isContinuous", trigger.isContinuousDefault)
                        val artActionName = bJson.optString("artAction", GestureAction.UNASSIGNED.name)
                        val artAction = GestureAction.fromKey(artActionName)
                        val titleActionName = bJson.optString("titleAction", GestureAction.UNASSIGNED.name)
                        val titleAction = GestureAction.fromKey(titleActionName)
                        updateGestureBinding(
                            trigger = trigger,
                            action = action,
                            isContinuous = isContinuous,
                            artAction = artAction,
                            titleAction = titleAction
                        )
                    }
                }
            }

            if (json.has("gpsVolumeEnabled")) {
                val gpsVolume = json.getBoolean("gpsVolumeEnabled")
                context.dataStore.edit { prefs -> prefs[KEY_GPS_VOLUME] = gpsVolume }
            }
            if (json.has("gpsSensitivity")) {
                val gpsSens = json.getDouble("gpsSensitivity").toFloat()
                context.dataStore.edit { prefs -> prefs[KEY_GPS_SENSITIVITY] = gpsSens }
            }
            if (json.has("autoRescan")) {
                setAutoRescan(json.getBoolean("autoRescan"))
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Profiles Flow & Helpers
    val profilesFlow: Flow<List<Profile>> = context.dataStore.data.map { prefs ->
        Profile.listFromJson(prefs[KEY_PROFILES_JSON])
    }

    val activeProfileIdFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_PROFILE_ID] ?: Profile.DEFAULT_ID
    }

    val activeProfileFlow: Flow<Profile> = context.dataStore.data.map { prefs ->
        val profiles = Profile.listFromJson(prefs[KEY_PROFILES_JSON])
        val activeId = prefs[KEY_ACTIVE_PROFILE_ID] ?: Profile.DEFAULT_ID
        profiles.find { it.id == activeId } ?: profiles.find { it.id == Profile.DEFAULT_ID } ?: Profile.DEFAULT
    }

    val profileSelectionModeFlow: Flow<ProfileSelectionMode> = context.dataStore.data.map { prefs ->
        ProfileSelectionMode.fromKey(prefs[KEY_PROFILE_SELECTION_MODE])
    }

    val profileSwitchTargetsFlow: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_PROFILE_SWITCH_TARGETS] ?: "${Profile.DEFAULT_ID},${Profile.TRAVELING_ID},${Profile.DRIVING_ID},${Profile.TRANSIT_ID},${Profile.DOCKED_ID},${Profile.UNDOCKED_ID}"
        raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    suspend fun setActiveProfile(profileId: String) {
        context.dataStore.edit { prefs ->
            val profiles = Profile.listFromJson(prefs[KEY_PROFILES_JSON])
            if (profiles.any { it.id == profileId }) {
                prefs[KEY_ACTIVE_PROFILE_ID] = profileId
                prefs[KEY_ACTIVE_PROFILE_STACK] = profileId
            }
        }
    }

    val activeProfileStackFlow: Flow<List<Profile>> = context.dataStore.data.map { prefs ->
        val activeProfileId = prefs[KEY_ACTIVE_PROFILE_ID] ?: Profile.DEFAULT_ID
        val rawStackStr = prefs[KEY_ACTIVE_PROFILE_STACK] ?: activeProfileId
        val stackIds = rawStackStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val profiles = Profile.listFromJson(prefs[KEY_PROFILES_JSON])
        val activeStack = stackIds.mapNotNull { id -> profiles.find { it.id == id } }.ifEmpty {
            listOf(profiles.find { it.id == activeProfileId } ?: Profile.DEFAULT)
        }
        activeStack
    }

    suspend fun setActiveProfileStack(profileIds: List<String>) {
        context.dataStore.edit { prefs ->
            val profiles = Profile.listFromJson(prefs[KEY_PROFILES_JSON])
            val validIds = profileIds.filter { id -> profiles.any { it.id == id } }
            if (validIds.isNotEmpty()) {
                prefs[KEY_ACTIVE_PROFILE_STACK] = validIds.joinToString(",")
            }
        }
    }

    suspend fun createProfile(name: String, emoji: String = "🏷️", parentId: String? = Profile.DEFAULT_ID): String {
        val newId = "profile_" + System.currentTimeMillis()
        context.dataStore.edit { prefs ->
            val profiles = Profile.listFromJson(prefs[KEY_PROFILES_JSON]).toMutableList()
            val newProfile = Profile(
                id = newId,
                name = name.ifBlank { "New Profile" },
                emoji = emoji.ifBlank { "🏷️" },
                isBuiltIn = false,
                isDeletable = true,
                overrides = emptyMap(),
                parentId = if (parentId == newId) Profile.DEFAULT_ID else (parentId ?: Profile.DEFAULT_ID)
            )
            profiles.add(newProfile)
            prefs[KEY_PROFILES_JSON] = Profile.listToJson(profiles)
            prefs[KEY_ACTIVE_PROFILE_ID] = newId
        }
        return newId
    }

    suspend fun renameProfile(profileId: String, newName: String, newEmoji: String? = null, newParentId: String? = null) {
        val builtInIds = setOf(Profile.DEFAULT_ID, Profile.TRAVELING_ID, Profile.DRIVING_ID, Profile.TRANSIT_ID, Profile.DOCKED_ID, Profile.UNDOCKED_ID, "dock")
        context.dataStore.edit { prefs ->
            val profiles = Profile.listFromJson(prefs[KEY_PROFILES_JSON])
            val profile = profiles.find { it.id == profileId }
            if (profile == null) return@edit
            val updated = profiles.map {
                if (it.id == profileId) {
                    it.copy(
                        name = if (newName.isNotBlank()) newName else it.name,
                        emoji = if (!newEmoji.isNullOrBlank()) newEmoji else it.emoji,
                        parentId = if (!it.isBuiltIn && profileId !in builtInIds && newParentId != null && newParentId != profileId) newParentId else it.parentId
                    )
                } else it
            }
            prefs[KEY_PROFILES_JSON] = Profile.listToJson(updated)
        }
    }

    suspend fun deleteProfile(profileId: String) {
        context.dataStore.edit { prefs ->
            val profiles = Profile.listFromJson(prefs[KEY_PROFILES_JSON])
            val profile = profiles.find { it.id == profileId }
            if (profile == null || profile.isBuiltIn || !profile.isDeletable) return@edit
            val updated = profiles.filterNot { it.id == profileId }
            prefs[KEY_PROFILES_JSON] = Profile.listToJson(updated)
            val currentActive = prefs[KEY_ACTIVE_PROFILE_ID] ?: Profile.DEFAULT_ID
            if (currentActive == profileId) {
                prefs[KEY_ACTIVE_PROFILE_ID] = Profile.DEFAULT_ID
            }
        }
    }

    suspend fun overrideSettingForActiveProfile(key: String, value: String) {
        context.dataStore.edit { prefs ->
            val activeId = prefs[KEY_ACTIVE_PROFILE_ID] ?: Profile.DEFAULT_ID
            if (activeId == Profile.DEFAULT_ID) return@edit
            val profiles = Profile.listFromJson(prefs[KEY_PROFILES_JSON]).map { profile ->
                if (profile.id == activeId) {
                    val updatedMap = profile.overrides.toMutableMap()
                    updatedMap[key] = value
                    profile.copy(overrides = updatedMap)
                } else profile
            }
            prefs[KEY_PROFILES_JSON] = Profile.listToJson(profiles)
        }
    }

    suspend fun revertSettingForActiveProfile(key: String) {
        context.dataStore.edit { prefs ->
            val activeId = prefs[KEY_ACTIVE_PROFILE_ID] ?: Profile.DEFAULT_ID
            if (activeId == Profile.DEFAULT_ID) return@edit
            val profiles = Profile.listFromJson(prefs[KEY_PROFILES_JSON]).map { profile ->
                if (profile.id == activeId) {
                    val updatedMap = profile.overrides.toMutableMap()
                    updatedMap.remove(key)
                    profile.copy(overrides = updatedMap)
                } else profile
            }
            prefs[KEY_PROFILES_JSON] = Profile.listToJson(profiles)
        }
    }

    suspend fun revertSettingForProfile(profileId: String, key: String) {
        context.dataStore.edit { prefs ->
            val profiles = Profile.listFromJson(prefs[KEY_PROFILES_JSON]).map { profile ->
                if (profile.id == profileId) {
                    val updatedMap = profile.overrides.toMutableMap()
                    updatedMap.remove(key)
                    profile.copy(overrides = updatedMap)
                } else profile
            }
            prefs[KEY_PROFILES_JSON] = Profile.listToJson(profiles)
        }
    }

    suspend fun revertAllSettingsForActiveProfile() {
        context.dataStore.edit { prefs ->
            val activeId = prefs[KEY_ACTIVE_PROFILE_ID] ?: Profile.DEFAULT_ID
            if (activeId == Profile.DEFAULT_ID) return@edit
            val profiles = Profile.listFromJson(prefs[KEY_PROFILES_JSON]).map { profile ->
                if (profile.id == activeId) {
                    profile.copy(overrides = emptyMap())
                } else profile
            }
            prefs[KEY_PROFILES_JSON] = Profile.listToJson(profiles)
        }
    }

    suspend fun setProfileSelectionMode(mode: ProfileSelectionMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PROFILE_SELECTION_MODE] = mode.name
        }
    }

    suspend fun setProfileSwitchTargets(targetIds: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PROFILE_SWITCH_TARGETS] = targetIds.joinToString(",")
        }
    }

    suspend fun cycleToNextProfile(): Profile {
        var nextProfile = Profile.DEFAULT
        context.dataStore.edit { prefs ->
            val profiles = Profile.listFromJson(prefs[KEY_PROFILES_JSON])
            val activeId = prefs[KEY_ACTIVE_PROFILE_ID] ?: Profile.DEFAULT_ID
            val targetIdsRaw = prefs[KEY_PROFILE_SWITCH_TARGETS] ?: "${Profile.DEFAULT_ID},${Profile.TRAVELING_ID},${Profile.DRIVING_ID},${Profile.TRANSIT_ID},${Profile.DOCKED_ID},${Profile.UNDOCKED_ID}"
            val targetIds = targetIdsRaw.split(",").map { it.trim() }.filter { id -> profiles.any { it.id == id } }
                .ifEmpty { profiles.map { it.id } }

            val currentIndex = targetIds.indexOf(activeId)
            val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % targetIds.size else 0
            val nextId = targetIds.getOrElse(nextIndex) { Profile.DEFAULT_ID }

            prefs[KEY_ACTIVE_PROFILE_ID] = nextId
            nextProfile = profiles.find { it.id == nextId } ?: Profile.DEFAULT
        }
        return nextProfile
    }

    suspend fun copyProfile(
        sourceProfileId: String,
        newName: String,
        linkForInheritance: Boolean,
        newEmoji: String? = null
    ): String {
        val newId = "profile_" + System.currentTimeMillis()
        context.dataStore.edit { prefs ->
            val profiles = Profile.listFromJson(prefs[KEY_PROFILES_JSON]).toMutableList()
            val sourceProfile = profiles.find { it.id == sourceProfileId } ?: Profile.DEFAULT
            val targetEmoji = if (!newEmoji.isNullOrBlank()) newEmoji else sourceProfile.emoji

            val newProfile = if (linkForInheritance) {
                Profile(
                    id = newId,
                    name = newName.ifBlank { "${sourceProfile.name} Copy" },
                    emoji = targetEmoji,
                    isBuiltIn = false,
                    isDeletable = true,
                    overrides = emptyMap(),
                    parentId = sourceProfile.id
                )
            } else {
                Profile(
                    id = newId,
                    name = newName.ifBlank { "${sourceProfile.name} Copy" },
                    emoji = targetEmoji,
                    isBuiltIn = false,
                    isDeletable = true,
                    overrides = sourceProfile.overrides.toMap(),
                    parentId = sourceProfile.parentId ?: Profile.DEFAULT_ID
                )
            }
            profiles.add(newProfile)
            prefs[KEY_PROFILES_JSON] = Profile.listToJson(profiles)
            prefs[KEY_ACTIVE_PROFILE_ID] = newId
        }
        return newId
    }

    suspend fun setProfileParent(profileId: String, newParentId: String?) {
        val builtInIds = setOf(Profile.DEFAULT_ID, Profile.TRAVELING_ID, Profile.DRIVING_ID, Profile.TRANSIT_ID, Profile.DOCKED_ID, Profile.UNDOCKED_ID, "dock")
        if (profileId in builtInIds) return
        context.dataStore.edit { prefs ->
            val profiles = Profile.listFromJson(prefs[KEY_PROFILES_JSON]).toMutableList()
            val index = profiles.indexOfFirst { it.id == profileId }
            if (index >= 0) {
                val targetProfile = profiles[index]
                if (targetProfile.isBuiltIn) return@edit
                val updatedProfile = targetProfile.copy(parentId = newParentId)
                val testList = profiles.toMutableList().apply { set(index, updatedProfile) }

                var current: Profile? = updatedProfile
                val visited = mutableSetOf<String>()
                var hasCycle = false
                while (current != null) {
                    if (!visited.add(current.id)) {
                        hasCycle = true
                        break
                    }
                    val nextParentId = current.parentId
                    current = if (nextParentId != null) testList.find { it.id == nextParentId } else null
                }

                if (!hasCycle) {
                    profiles[index] = updatedProfile
                    prefs[KEY_PROFILES_JSON] = Profile.listToJson(profiles)
                }
            }
        }
    }
}

