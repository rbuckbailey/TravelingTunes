package com.travelingtunes.app.core.model

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
    MENU("Menu"),
    VOLUME_UP("Volume Up"),
    VOLUME_DOWN("Volume Down"),
    START_DEFAULT_PLAYLIST("Start Default Playlist"),
    PLAY_CURRENT_ARTIST("Play Current Artist"),
    PLAY_CURRENT_ALBUM("Play Current Album"),
    TOGGLE_REPEAT("Toggle Repeat"),
    TOGGLE_SHUFFLE("Toggle Shuffle"),
    INCREASE_RATING("Increase Rating"),
    DECREASE_RATING("Decrease Rating"),
    SHOW_QUICK_START("Show Quick Start");
    // Navigation actions disabled/commented out:
    // NAVIGATE_TO_CONTACT("Navigate to Contact"),
    // NAVIGATE_HOME("Navigate Home"),
    // NAVIGATE_WORK("Navigate to Work"),
    // SHOW_DIRECTIONS("Show Directions"),
    // RECENTER_MAP("Recenter Map"),
    // REPEAT_INSTRUCTIONS("Repeat Navigation Instructions");

    companion object {
        fun fromKey(key: String): GestureAction {
            val sanitizedKey = key.filter { it.isLetterOrDigit() }
            return entries.find {
                it.name.equals(key, ignoreCase = true) ||
                it.displayName.equals(key, ignoreCase = true) ||
                it.name.filter { c -> c.isLetterOrDigit() }.equals(sanitizedKey, ignoreCase = true) ||
                it.displayName.filter { c -> c.isLetterOrDigit() }.equals(sanitizedKey, ignoreCase = true)
            } ?: UNASSIGNED
        }
    }
}
