package com.travelingtunes.app.core.model

/**
 * =========================================================================================
 * CRUCIAL ARCHITECTURAL FEATURE NOTICE:
 * GestureAction.OTHER_OPTION ("Other Option") is a CORE feature of TravelingTunes.
 * It allows binding ANY application preference, setting, or profile override (ConfigOption)
 * directly to swipe gestures, taps, edge region buttons, radial menus, and keyboard shortcuts.
 *
 * DO NOT EVER REMOVE OR FILTER OUT GestureAction.OTHER_OPTION from action selection popups,
 * dropdown menus, or gesture assignment pickers.
 * It MUST ALWAYS remain available to the user at the top level at the end of the action list.
 * =========================================================================================
 */
enum class GestureAction(val displayName: String) {
    UNASSIGNED("Unassigned"),
    PLAY("Play"),
    PAUSE("Pause"),
    PLAY_PAUSE("Play / Pause"),
    FAST_FORWARD("Fast Forward"),
    REWIND("Rewind"),
    NEXT("Next Song"),
    PREVIOUS("Previous Song"),
    RESTART("Restart Song"),
    RESTART_PREVIOUS("Restart / Previous"),
    SONG_PICKER("Song Picker"),
    SELECT_ALBUM_VIEW("Select Album View"),
    SELECT_ARTIST_VIEW("Select Artist View"),
    SHOW_QUEUE("Current Queue"),
    MENU("Menu"),
    VOLUME_UP("Volume Up"),
    VOLUME_DOWN("Volume Down"),
    SHUFFLE_ALL_SONGS("Shuffle All Songs"),
    PLAY_CURRENT_ARTIST("Play Current Artist"),
    PLAY_CURRENT_ALBUM("Play Current Album"),
    NEXT_ALBUM("Next Album"),
    PREVIOUS_ALBUM("Previous Album"),
    TOGGLE_REPEAT("Toggle Repeat"),
    TOGGLE_SHUFFLE("Toggle Shuffle"),
    INCREASE_RATING("Increase Rating"),
    DECREASE_RATING("Decrease Rating"),
    SHOW_QUICK_START("Show Quick Start"),
    DELETE_DOWNLOADED_ART("Delete Downloaded Art"),
    TOGGLE_DRIVING_MODE("Toggle Traveling Mode"),
    TOGGLE_DOCKED_ART("Toggle Docked Art"),
    SELECT_PROFILE("Select Profile"),
    EDIT_TAGS("Edit Tags"),
    SHARE_TUNES("Share Tunes"),
    RADIAL_MENU("Radial Menu"),
    OTHER_OPTION("Other Option");

    companion object {
        fun fromKey(key: String): GestureAction {
            val sanitizedKey = key.filter { it.isLetterOrDigit() }
            if (sanitizedKey.equals("StartDefaultPlaylist", ignoreCase = true)) {
                return SHUFFLE_ALL_SONGS
            }
            if (sanitizedKey.startsWith("EDIT", ignoreCase = true) && sanitizedKey.contains("TAG", ignoreCase = true)) {
                return EDIT_TAGS
            }
            if (sanitizedKey.startsWith("SHARE", ignoreCase = true) &&
                (sanitizedKey.contains("TRACK", ignoreCase = true) || sanitizedKey.contains("ALBUM", ignoreCase = true) || sanitizedKey.contains("TUNE", ignoreCase = true) || sanitizedKey.contains("SONG", ignoreCase = true))) {
                return SHARE_TUNES
            }
            return entries.find {
                it.name.equals(key, ignoreCase = true) ||
                it.displayName.equals(key, ignoreCase = true) ||
                it.name.filter { c -> c.isLetterOrDigit() }.equals(sanitizedKey, ignoreCase = true) ||
                it.displayName.filter { c -> c.isLetterOrDigit() }.equals(sanitizedKey, ignoreCase = true)
            } ?: UNASSIGNED
        }

        /**
         * Returns GestureActions organized logically into categories for selection popups and dropdowns.
         * CRUCIAL: GestureAction.OTHER_OPTION ("Other Option") is ALWAYS placed at the very end of the
         * list as a top-level item.
         */
        fun getGroupedCategories(
            excludeRadialMenu: Boolean = false,
            excludeUnassigned: Boolean = false
        ): List<ActionCategoryGroup> {
            val playbackSet = setOf(
                PLAY_PAUSE, PLAY, PAUSE, NEXT, PREVIOUS,
                FAST_FORWARD, REWIND, RESTART, RESTART_PREVIOUS
            )

            val librarySet = setOf(
                SONG_PICKER, SHOW_QUEUE, SHUFFLE_ALL_SONGS,
                PLAY_CURRENT_ARTIST, PLAY_CURRENT_ALBUM,
                NEXT_ALBUM, PREVIOUS_ALBUM,
                SELECT_ALBUM_VIEW, SELECT_ARTIST_VIEW,
                EDIT_TAGS, SHARE_TUNES
            )

            val volumeAndModesSet = setOf(
                VOLUME_UP, VOLUME_DOWN, TOGGLE_REPEAT, TOGGLE_SHUFFLE,
                INCREASE_RATING, DECREASE_RATING
            )

            val appAndDisplaySet = setOf(
                MENU, SHOW_QUICK_START, SELECT_PROFILE,
                TOGGLE_DRIVING_MODE, TOGGLE_DOCKED_ART,
                DELETE_DOWNLOADED_ART
            )

            val playbackList = mutableListOf<GestureAction>()
            val libraryList = mutableListOf<GestureAction>()
            val volumeAndModesList = mutableListOf<GestureAction>()
            val appAndDisplayList = mutableListOf<GestureAction>()

            if (!excludeUnassigned) {
                appAndDisplayList.add(UNASSIGNED)
            }

            for (action in entries) {
                if (action == OTHER_OPTION) continue
                if (action == UNASSIGNED) continue
                if (action == RADIAL_MENU) {
                    if (!excludeRadialMenu) {
                        appAndDisplayList.add(RADIAL_MENU)
                    }
                    continue
                }

                when {
                    action in playbackSet -> playbackList.add(action)
                    action in librarySet -> libraryList.add(action)
                    action in volumeAndModesSet -> volumeAndModesList.add(action)
                    action in appAndDisplaySet -> appAndDisplayList.add(action)
                    else -> libraryList.add(action)
                }
            }

            // CRUCIAL: Custom Action OTHER_OPTION ("Other Option") is always at the top level at the end.
            val custom = listOf(OTHER_OPTION)

            return listOf(
                ActionCategoryGroup(ActionCategory.PLAYBACK, playbackList),
                ActionCategoryGroup(ActionCategory.LIBRARY, libraryList),
                ActionCategoryGroup(ActionCategory.VOLUME_AND_MODES, volumeAndModesList),
                ActionCategoryGroup(ActionCategory.APP_AND_DISPLAY, appAndDisplayList),
                ActionCategoryGroup(ActionCategory.CUSTOM, custom)
            )
        }
    }
}

enum class ActionCategory(val displayName: String) {
    PLAYBACK("Playback Controls"),
    LIBRARY("Library & Navigation"),
    VOLUME_AND_MODES("Volume & Playback Modes"),
    APP_AND_DISPLAY("App & Display"),
    CUSTOM("Custom Action")
}

data class ActionCategoryGroup(
    val category: ActionCategory,
    val actions: List<GestureAction>
)
