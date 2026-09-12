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
import com.travelingtunes.app.core.model.ArtLayoutOption
import com.travelingtunes.app.core.model.ArtScaleOption
import com.travelingtunes.app.core.model.ColorTheme
import com.travelingtunes.app.core.model.ConfigOption
import com.travelingtunes.app.core.model.DisplaySettings
import com.travelingtunes.app.core.model.GestureAction
import com.travelingtunes.app.core.model.GestureBinding
import com.travelingtunes.app.core.model.GestureTrigger
import com.travelingtunes.app.core.model.HudTypeOption
import com.travelingtunes.app.core.model.RepeatMode
import com.travelingtunes.app.core.model.ScrubHudTypeOption
import com.travelingtunes.app.core.model.ShuffleMode
import com.travelingtunes.app.core.model.StreamingAccount
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
    val shuffleMode: ShuffleMode = ShuffleMode.OFF
)

class SettingsDataStore(private val context: Context) {

    // Keys
    companion object {
        val KEY_FIRST_RUN = booleanPreferencesKey("firstRun")
        val KEY_MUSIC_FOLDER_URI = stringPreferencesKey("musicFolderUri")
        val KEY_MUSIC_FOLDER_NAME = stringPreferencesKey("musicFolderName")
        val KEY_LAST_SCAN_TIME = longPreferencesKey("lastScanTime")
        val KEY_FIRST_RUN_PROMPTED = booleanPreferencesKey("firstRunPrompted")
        val KEY_AUTO_RESCAN = booleanPreferencesKey("autoRescan")

        val KEY_VOLUME_SENSITIVITY = floatPreferencesKey("volumeSensitivity")
        val KEY_SEEK_SENSITIVITY = floatPreferencesKey("seekSensitivity")
        val KEY_GPS_SENSITIVITY = floatPreferencesKey("gpsSensitivity")
        val KEY_GPS_VOLUME = booleanPreferencesKey("gpsVolume")
        val KEY_DISABLE_AUTOLOCK = booleanPreferencesKey("disableAutolock")
        val KEY_REPEAT = booleanPreferencesKey("repeat")
        val KEY_SHUFFLE = booleanPreferencesKey("shuffle")
        val KEY_PLAYLIST = stringPreferencesKey("playlist")

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
        val KEY_TITLE_ORDER = stringPreferencesKey("titleOrder")

        // Navigation / Menu State Persistence
        val KEY_LAST_SETTINGS_SUBMENU = stringPreferencesKey("lastSettingsSubmenu")

        // Theme
        val KEY_CURRENT_THEME = stringPreferencesKey("currentTheme")
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

        // Streaming Accounts
        val KEY_STREAMING_ACCOUNTS = stringPreferencesKey("streamingAccounts")

        val DEFAULT_RADIAL_ACTIONS = listOf(
            GestureAction.PLAY_PAUSE,
            GestureAction.NEXT,
            GestureAction.PREVIOUS,
            GestureAction.VOLUME_UP,
            GestureAction.VOLUME_DOWN,
            GestureAction.FAST_FORWARD,
            GestureAction.REWIND,
            GestureAction.SONG_PICKER
        )
    }

    val displaySettingsFlow: Flow<DisplaySettings> = context.dataStore.data.map { prefs ->
        DisplaySettings(
            artistFontSize = prefs[KEY_ARTIST_FONT_SIZE] ?: 50f,
            songFontSize = prefs[KEY_SONG_FONT_SIZE] ?: 70f,
            albumFontSize = prefs[KEY_ALBUM_FONT_SIZE] ?: 55f,
            artistAlignment = TextAlignmentOption.entries.find {
                it.name.equals(prefs[KEY_ARTIST_ALIGNMENT], true) || it.displayName.equals(prefs[KEY_ARTIST_ALIGNMENT], true)
            } ?: TextAlignmentOption.LEFT,
            songAlignment = TextAlignmentOption.entries.find {
                it.name.equals(prefs[KEY_SONG_ALIGNMENT], true) || it.displayName.equals(prefs[KEY_SONG_ALIGNMENT], true)
            } ?: TextAlignmentOption.CENTER,
            albumAlignment = TextAlignmentOption.entries.find {
                it.name.equals(prefs[KEY_ALBUM_ALIGNMENT], true) || it.displayName.equals(prefs[KEY_ALBUM_ALIGNMENT], true)
            } ?: TextAlignmentOption.RIGHT,
            minimumFontSize = prefs[KEY_MINIMUM_FONT_SIZE] ?: 35f,
            titleShrinkInPortrait = prefs[KEY_TITLE_SHRINK_PORTRAIT] ?: true,
            titleShrinkLong = prefs[KEY_TITLE_SHRINK_LONG] ?: true,
            titleScrollLong = prefs[KEY_TITLE_SCROLL_LONG] ?: false,
            showAlbumArt = prefs[KEY_SHOW_ALBUM_ART] ?: true,
            albumArtColors = prefs[KEY_ALBUM_ART_COLORS] ?: true,
            albumArtScale = ArtScaleOption.entries.getOrElse(prefs[KEY_ALBUM_ART_SCALE] ?: 0) { ArtScaleOption.FILL_SCREEN },
            artAlignmentPortrait = ArtAlignmentPortrait.entries.find {
                it.name.equals(prefs[KEY_ART_ALIGNMENT_PORTRAIT], true)
            } ?: ArtAlignmentPortrait.MIDDLE,
            artAlignmentLandscape = ArtAlignmentLandscape.entries.find {
                it.name.equals(prefs[KEY_ART_ALIGNMENT_LANDSCAPE], true)
            } ?: ArtAlignmentLandscape.CENTER,
            albumArtFade = (prefs[KEY_ALBUM_ART_FADE] ?: 1.0f).takeIf { it >= 0.05f } ?: 1.0f,
            artDisplayLayout = ArtLayoutOption.entries.getOrElse(prefs[KEY_ART_DISPLAY_LAYOUT] ?: 0) { ArtLayoutOption.OVERLAY },
            hudType = HudTypeOption.entries.find { it.value == (prefs[KEY_HUD_TYPE] ?: 1) } ?: HudTypeOption.BAR_VOLUME,
            scrubHudType = ScrubHudTypeOption.entries.find { it.value == (prefs[KEY_SCRUB_HUD_TYPE] ?: 2) } ?: ScrubHudTypeOption.EDGE_HUD,
            volumeAlwaysOn = prefs[KEY_VOLUME_ALWAYS_ON] ?: true,
            showStatusBar = prefs[KEY_SHOW_STATUS_BAR] ?: false,
            showActions = prefs[KEY_SHOW_ACTIONS] ?: true,
            hudLineThickness = prefs[KEY_HUD_LINE_THICKNESS] ?: 16f,
            artistFontKey = prefs[KEY_ARTIST_FONT_KEY] ?: "DEFAULT",
            songFontKey = prefs[KEY_SONG_FONT_KEY] ?: "DEFAULT",
            albumFontKey = prefs[KEY_ALBUM_FONT_KEY] ?: "DEFAULT",
            artistBold = prefs[KEY_ARTIST_BOLD] ?: true,
            artistItalic = prefs[KEY_ARTIST_ITALIC] ?: false,
            artistUnderline = prefs[KEY_ARTIST_UNDERLINE] ?: false,
            songBold = prefs[KEY_SONG_BOLD] ?: true,
            songItalic = prefs[KEY_SONG_ITALIC] ?: false,
            songUnderline = prefs[KEY_SONG_UNDERLINE] ?: false,
            albumBold = prefs[KEY_ALBUM_BOLD] ?: false,
            albumItalic = prefs[KEY_ALBUM_ITALIC] ?: false,
            albumUnderline = prefs[KEY_ALBUM_UNDERLINE] ?: false,
            keepScreenOn = prefs[KEY_KEEP_SCREEN_ON] ?: prefs[KEY_DISABLE_AUTOLOCK] ?: true,
            immersiveMode = prefs[KEY_IMMERSIVE_MODE] ?: true,
            numEdgeRegions = prefs[KEY_NUM_EDGE_REGIONS] ?: 3,
            titleOrder = (prefs[KEY_TITLE_ORDER] ?: "ARTIST,SONG,ALBUM")
                .split(",")
                .mapNotNull { name ->
                    runCatching { TitleRowType.valueOf(name.trim()) }.getOrNull()
                }
                .ifEmpty { listOf(TitleRowType.ARTIST, TitleRowType.SONG, TitleRowType.ALBUM) }
        )
    }

    val themeSettingsFlow: Flow<ThemeSettings> = context.dataStore.data.map { prefs ->
        val textRed = prefs[KEY_CUSTOM_TEXT_RED] ?: 22f
        val textGreen = prefs[KEY_CUSTOM_TEXT_GREEN] ?: 22f
        val textBlue = prefs[KEY_CUSTOM_TEXT_BLUE] ?: 180f
        ThemeSettings(
            currentThemeName = prefs[KEY_CURRENT_THEME] ?: ColorTheme.MATCH_ALBUM_ART.name,
            customTextRed = textRed,
            customTextGreen = textGreen,
            customTextBlue = textBlue,
            customSongTitleRed = prefs[KEY_CUSTOM_SONG_TITLE_RED] ?: textRed,
            customSongTitleGreen = prefs[KEY_CUSTOM_SONG_TITLE_GREEN] ?: textGreen,
            customSongTitleBlue = prefs[KEY_CUSTOM_SONG_TITLE_BLUE] ?: textBlue,
            customArtistTitleRed = prefs[KEY_CUSTOM_ARTIST_TITLE_RED] ?: textRed,
            customArtistTitleGreen = prefs[KEY_CUSTOM_ARTIST_TITLE_GREEN] ?: textGreen,
            customArtistTitleBlue = prefs[KEY_CUSTOM_ARTIST_TITLE_BLUE] ?: textBlue,
            customAlbumTitleRed = prefs[KEY_CUSTOM_ALBUM_TITLE_RED] ?: textRed,
            customAlbumTitleGreen = prefs[KEY_CUSTOM_ALBUM_TITLE_GREEN] ?: textGreen,
            customAlbumTitleBlue = prefs[KEY_CUSTOM_ALBUM_TITLE_BLUE] ?: textBlue,
            customBGRed = prefs[KEY_CUSTOM_BG_RED] ?: 200f,
            customBGGreen = prefs[KEY_CUSTOM_BG_GREEN] ?: 200f,
            customBGBlue = prefs[KEY_CUSTOM_BG_BLUE] ?: 100f,
            dimAtNight = prefs[KEY_DIM_AT_NIGHT] ?: true,
            invertAtNight = prefs[KEY_INVERT_AT_NIGHT] ?: false,
            sunRiseHour = prefs[KEY_SUNRISE_HOUR] ?: 6,
            sunSetHour = prefs[KEY_SUNSET_HOUR] ?: 19,
            isRounded = prefs[KEY_THEME_ROUNDED] ?: false,
            isGlass = prefs[KEY_THEME_GLASS] ?: false
        )
    }

    val gestureBindingsFlow: Flow<Map<GestureTrigger, GestureBinding>> = context.dataStore.data.map { prefs ->
        GestureTrigger.entries.associateWith { trigger ->
            val actionKey = prefs[stringPreferencesKey(trigger.key)] ?: trigger.defaultActionKey
            val isContinuous = prefs[booleanPreferencesKey("${trigger.key}Continuous")] ?: trigger.isContinuousDefault
            val otherOptionKey = prefs[stringPreferencesKey("${trigger.key}_other_target")]
            GestureBinding(trigger, GestureAction.fromKey(actionKey), isContinuous, otherOptionKey)
        }
    }

    fun getRadialMenuActionsFlow(triggerKey: String): Flow<List<GestureAction>> {
        return context.dataStore.data.map { prefs ->
            val rawStr = prefs[stringPreferencesKey("radial_actions_$triggerKey")]
            if (rawStr.isNullOrEmpty()) {
                DEFAULT_RADIAL_ACTIONS
            } else {
                rawStr.split(",").map { GestureAction.fromKey(it) }
            }
        }
    }

    val allRadialMenuActionsFlow: Flow<Map<String, List<GestureAction>>> = context.dataStore.data.map { prefs ->
        val result = mutableMapOf<String, List<GestureAction>>()
        GestureTrigger.entries.forEach { trigger ->
            val rawStr = prefs[stringPreferencesKey("radial_actions_${trigger.key}")]
            if (!rawStr.isNullOrEmpty()) {
                result[trigger.key] = rawStr.split(",").map { GestureAction.fromKey(it) }
            }
        }
        result
    }

    val gpsVolumeEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_GPS_VOLUME] ?: false
    }

    val gpsSensitivityFlow: Flow<Float> = context.dataStore.data.map { prefs ->
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
        prefs[KEY_AUTO_RESCAN] ?: false
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
            RepeatMode.entries.find { it.name.equals(repeatModeName, true) } ?: RepeatMode.OFF
        } else {
            if (isRepeat) RepeatMode.SONG else RepeatMode.OFF
        }

        val shuffleModeName = prefs[KEY_SAVED_SHUFFLE_MODE]
        val shuffleMode = if (shuffleModeName != null) {
            ShuffleMode.entries.find { it.name.equals(shuffleModeName, true) } ?: ShuffleMode.OFF
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

    val streamingAccountsFlow: Flow<Map<String, StreamingAccount>> = context.dataStore.data.map { prefs ->
        parseStreamingAccounts(prefs[KEY_STREAMING_ACCOUNTS] ?: "")
    }

    suspend fun signInStreamingService(serviceId: String, username: String, accountType: String = "Premium") {
        context.dataStore.edit { prefs ->
            val currentMap = parseStreamingAccounts(prefs[KEY_STREAMING_ACCOUNTS] ?: "").toMutableMap()
            currentMap[serviceId] = StreamingAccount(serviceId, username, accountType, System.currentTimeMillis())
            prefs[KEY_STREAMING_ACCOUNTS] = serializeStreamingAccounts(currentMap)
        }
    }

    suspend fun signOutStreamingService(serviceId: String) {
        context.dataStore.edit { prefs ->
            val currentMap = parseStreamingAccounts(prefs[KEY_STREAMING_ACCOUNTS] ?: "").toMutableMap()
            currentMap.remove(serviceId)
            prefs[KEY_STREAMING_ACCOUNTS] = serializeStreamingAccounts(currentMap)
        }
    }

    private fun parseStreamingAccounts(raw: String): Map<String, StreamingAccount> {
        if (raw.isBlank()) return emptyMap()
        return raw.split(";").mapNotNull { entry ->
            val parts = entry.split("::")
            if (parts.size >= 3) {
                val serviceId = parts[0]
                val username = parts[1]
                val accountType = parts[2]
                val signedInAt = parts.getOrNull(3)?.toLongOrNull() ?: System.currentTimeMillis()
                serviceId to StreamingAccount(serviceId, username, accountType, signedInAt)
            } else null
        }.toMap()
    }

    private fun serializeStreamingAccounts(map: Map<String, StreamingAccount>): String {
        return map.values.joinToString(";") { account ->
            "${account.serviceId}::${account.username}::${account.accountType}::${account.signedInAt}"
        }
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
            prefs[KEY_AUTO_RESCAN] = enabled
        }
    }

    suspend fun updateGestureBinding(
        trigger: GestureTrigger,
        action: GestureAction,
        isContinuous: Boolean = false,
        otherOptionKey: String? = null
    ) {
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey(trigger.key)] = action.name
            prefs[booleanPreferencesKey("${trigger.key}Continuous")] = isContinuous
            if (otherOptionKey != null) {
                prefs[stringPreferencesKey("${trigger.key}_other_target")] = otherOptionKey
            } else {
                prefs.remove(stringPreferencesKey("${trigger.key}_other_target"))
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
                }
            } else {
                val priorKey = stringPreferencesKey("${triggerKey}_prior_value")
                val priorArtColorsKey = booleanPreferencesKey("${triggerKey}_prior_art_colors")
                val targetVal = option.targetValue ?: return@edit

                when {
                    option.key.startsWith("THEME_") -> {
                        val currentVal = prefs[KEY_CURRENT_THEME] ?: ColorTheme.MATCH_ALBUM_ART.name
                        val currentArtColors = prefs[KEY_ALBUM_ART_COLORS] ?: true
                        if (currentVal.equals(targetVal, ignoreCase = true)) {
                            val priorVal = prefs[priorKey] ?: ColorTheme.MATCH_ALBUM_ART.name
                            val priorArtColors = prefs[priorArtColorsKey] ?: true
                            prefs[KEY_CURRENT_THEME] = priorVal
                            prefs[KEY_ALBUM_ART_COLORS] = priorArtColors
                            prefs[priorKey] = currentVal
                            prefs[priorArtColorsKey] = currentArtColors
                        } else {
                            prefs[priorKey] = currentVal
                            prefs[priorArtColorsKey] = currentArtColors
                            prefs[KEY_CURRENT_THEME] = targetVal
                            prefs[KEY_ALBUM_ART_COLORS] = (targetVal.equals("Match Album Art", true) || targetVal.equals("Auto By Art", true))
                        }
                    }
                    option.key.startsWith("ALIGN_ARTIST_") -> {
                        val currentVal = prefs[KEY_ARTIST_ALIGNMENT] ?: TextAlignmentOption.LEFT.name
                        if (currentVal.equals(targetVal, ignoreCase = true)) {
                            val priorVal = prefs[priorKey] ?: TextAlignmentOption.LEFT.name
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
                            val priorVal = prefs[priorKey] ?: TextAlignmentOption.CENTER.name
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
                            val priorVal = prefs[priorKey] ?: TextAlignmentOption.RIGHT.name
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
                            val priorVal = prefs[priorKey] ?: ArtScaleOption.FILL_SCREEN.name
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
                        if (currentName.equals(targetVal, ignoreCase = true)) {
                            val priorVal = prefs[priorKey] ?: ArtLayoutOption.OVERLAY.name
                            val newOrdinal = ArtLayoutOption.entries.find { it.name.equals(priorVal, true) }?.ordinal ?: 0
                            prefs[KEY_ART_DISPLAY_LAYOUT] = newOrdinal
                            prefs[priorKey] = currentName
                        } else {
                            prefs[priorKey] = currentName
                            val newOrdinal = ArtLayoutOption.entries.find { it.name.equals(targetVal, true) }?.ordinal ?: 0
                            prefs[KEY_ART_DISPLAY_LAYOUT] = newOrdinal
                        }
                    }
                    option.key.startsWith("ART_ALIGN_PORT_") -> {
                        val currentVal = prefs[KEY_ART_ALIGNMENT_PORTRAIT] ?: ArtAlignmentPortrait.MIDDLE.name
                        if (currentVal.equals(targetVal, ignoreCase = true)) {
                            val priorVal = prefs[priorKey] ?: ArtAlignmentPortrait.MIDDLE.name
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
                            val priorVal = prefs[priorKey] ?: ArtAlignmentLandscape.CENTER.name
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
                            val priorVal = prefs[priorKey] ?: HudTypeOption.BAR_VOLUME.name
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
                            val priorVal = prefs[priorKey] ?: ScrubHudTypeOption.EDGE_HUD.name
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
            prefs[KEY_TITLE_ORDER] = update.titleOrder.joinToString(",") { it.name }
        }
    }

    suspend fun updateThemeSettings(update: ThemeSettings) {
        context.dataStore.edit { prefs ->
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
                    titleOrder = dJson.optString("titleOrder", "")
                        .split(",")
                        .mapNotNull { name -> runCatching { TitleRowType.valueOf(name.trim()) }.getOrNull() }
                        .ifEmpty { currentDisplay.titleOrder }
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
                        updateGestureBinding(trigger, action, isContinuous)
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
}

