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
import kotlinx.coroutines.flow.Flow
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
        val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keepScreenOn")
        val KEY_IMMERSIVE_MODE = booleanPreferencesKey("immersiveMode")

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
            albumArtFade = prefs[KEY_ALBUM_ART_FADE] ?: 0.35f,
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
            keepScreenOn = prefs[KEY_KEEP_SCREEN_ON] ?: prefs[KEY_DISABLE_AUTOLOCK] ?: true,
            immersiveMode = prefs[KEY_IMMERSIVE_MODE] ?: true
        )
    }

    val themeSettingsFlow: Flow<ThemeSettings> = context.dataStore.data.map { prefs ->
        val textRed = prefs[KEY_CUSTOM_TEXT_RED] ?: 22f
        val textGreen = prefs[KEY_CUSTOM_TEXT_GREEN] ?: 22f
        val textBlue = prefs[KEY_CUSTOM_TEXT_BLUE] ?: 180f
        ThemeSettings(
            currentThemeName = prefs[KEY_CURRENT_THEME] ?: "White on Grey",
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
            GestureBinding(trigger, GestureAction.fromKey(actionKey), isContinuous)
        }
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

    suspend fun updateGestureBinding(trigger: GestureTrigger, action: GestureAction, isContinuous: Boolean = false) {
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey(trigger.key)] = action.name
            prefs[booleanPreferencesKey("${trigger.key}Continuous")] = isContinuous
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
            prefs[KEY_KEEP_SCREEN_ON] = update.keepScreenOn
            prefs[KEY_IMMERSIVE_MODE] = update.immersiveMode
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
            }
        }
    }
}
