package com.travelingtunes.app.core.model

enum class GestureCategory {
    ONE_FINGER_SWIPE,
    TWO_FINGER_SWIPE,
    THREE_FINGER_SWIPE,
    ONE_FINGER_TAP,
    TWO_FINGER_TAP,
    THREE_FINGER_TAP,
    LONG_PRESS,
    SCREEN_REGION
}

enum class GestureTrigger(
    val key: String,
    val displayName: String,
    val category: GestureCategory,
    val defaultActionKey: String,
    val isContinuousDefault: Boolean = false
) {
    // 1-Finger Swipes
    SWIPE_1_LEFT("1SwipeLeft", "1-Finger Swipe Left", GestureCategory.ONE_FINGER_SWIPE, "Rewind"),
    SWIPE_1_RIGHT("1SwipeRight", "1-Finger Swipe Right", GestureCategory.ONE_FINGER_SWIPE, "FastForward"),
    SWIPE_1_UP("1SwipeUp", "1-Finger Swipe Up", GestureCategory.ONE_FINGER_SWIPE, "VolumeUp", true),
    SWIPE_1_DOWN("1SwipeDown", "1-Finger Swipe Down", GestureCategory.ONE_FINGER_SWIPE, "VolumeDown", true),

    // 2-Finger Swipes
    SWIPE_2_LEFT("2SwipeLeft", "2-Finger Swipe Left", GestureCategory.TWO_FINGER_SWIPE, "Next"),
    SWIPE_2_RIGHT("2SwipeRight", "2-Finger Swipe Right", GestureCategory.TWO_FINGER_SWIPE, "RestartPrevious"),
    SWIPE_2_UP("2SwipeUp", "2-Finger Swipe Up", GestureCategory.TWO_FINGER_SWIPE, "IncreaseRating"),
    SWIPE_2_DOWN("2SwipeDown", "2-Finger Swipe Down", GestureCategory.TWO_FINGER_SWIPE, "DecreaseRating"),

    // 3-Finger Swipes
    SWIPE_3_LEFT("3SwipeLeft", "3-Finger Swipe Left", GestureCategory.THREE_FINGER_SWIPE, "Unassigned"),
    SWIPE_3_RIGHT("3SwipeRight", "3-Finger Swipe Right", GestureCategory.THREE_FINGER_SWIPE, "Unassigned"),
    SWIPE_3_UP("3SwipeUp", "3-Finger Swipe Up", GestureCategory.THREE_FINGER_SWIPE, "PlayCurrentArtist"),
    SWIPE_3_DOWN("3SwipeDown", "3-Finger Swipe Down", GestureCategory.THREE_FINGER_SWIPE, "PlayCurrentAlbum"),

    // Taps
    TAP_1_1("11Tap", "1-Finger Single Tap", GestureCategory.ONE_FINGER_TAP, "PlayPause"),
    TAP_1_2("12Tap", "1-Finger Double Tap", GestureCategory.ONE_FINGER_TAP, "Next"),
    TAP_1_3("13Tap", "1-Finger Triple Tap", GestureCategory.ONE_FINGER_TAP, "Previous"),
    TAP_2_1("21Tap", "2-Finger Single Tap", GestureCategory.TWO_FINGER_TAP, "SongPicker"),
    TAP_2_2("22Tap", "2-Finger Double Tap", GestureCategory.TWO_FINGER_TAP, "Next"),
    TAP_2_3("23Tap", "2-Finger Triple Tap", GestureCategory.TWO_FINGER_TAP, "Previous"),
    TAP_3_1("31Tap", "3-Finger Single Tap", GestureCategory.THREE_FINGER_TAP, "Unassigned"),
    TAP_3_2("32Tap", "3-Finger Double Tap", GestureCategory.THREE_FINGER_TAP, "Next"),
    TAP_3_3("33Tap", "3-Finger Triple Tap", GestureCategory.THREE_FINGER_TAP, "Previous"),

    // Long Presses
    LONG_PRESS_1("1LongPress", "1-Finger Long Press", GestureCategory.LONG_PRESS, "Menu"),
    LONG_PRESS_2("2LongPress", "2-Finger Long Press", GestureCategory.LONG_PRESS, "Unassigned"),
    LONG_PRESS_3("3LongPress", "3-Finger Long Press", GestureCategory.LONG_PRESS, "ShuffleAllSongs"),

    // Screen Regions / Edge Region Slots (1 to 7)
    CORNER_TOP_LEFT("TopLeft", "Top Region 1", GestureCategory.SCREEN_REGION, "ToggleRepeat"),
    CORNER_TOP_2("TopRegion2", "Top Region 2", GestureCategory.SCREEN_REGION, "Unassigned"),
    CORNER_TOP_3("TopRegion3", "Top Region 3", GestureCategory.SCREEN_REGION, "Unassigned"),
    CORNER_TOP_CENTER("TopCenter", "Top Region 4", GestureCategory.SCREEN_REGION, "PlayCurrentAlbum"),
    CORNER_TOP_5("TopRegion5", "Top Region 5", GestureCategory.SCREEN_REGION, "Unassigned"),
    CORNER_TOP_6("TopRegion6", "Top Region 6", GestureCategory.SCREEN_REGION, "Unassigned"),
    CORNER_TOP_RIGHT("TopRight", "Top Region 7", GestureCategory.SCREEN_REGION, "ToggleShuffle"),

    CORNER_BOTTOM_LEFT("BottomLeft", "Bottom Region 1", GestureCategory.SCREEN_REGION, "SongPicker"),
    CORNER_BOTTOM_2("BottomRegion2", "Bottom Region 2", GestureCategory.SCREEN_REGION, "Unassigned"),
    CORNER_BOTTOM_3("BottomRegion3", "Bottom Region 3", GestureCategory.SCREEN_REGION, "Unassigned"),
    CORNER_BOTTOM_CENTER("BottomCenter", "Bottom Region 4", GestureCategory.SCREEN_REGION, "Unassigned"),
    CORNER_BOTTOM_5("BottomRegion5", "Bottom Region 5", GestureCategory.SCREEN_REGION, "Unassigned"),
    CORNER_BOTTOM_6("BottomRegion6", "Bottom Region 6", GestureCategory.SCREEN_REGION, "Unassigned"),
    CORNER_BOTTOM_RIGHT("BottomRight", "Bottom Region 7", GestureCategory.SCREEN_REGION, "Menu");

    companion object {
        val TOP_REGION_SLOTS = listOf(
            CORNER_TOP_LEFT,
            CORNER_TOP_2,
            CORNER_TOP_3,
            CORNER_TOP_CENTER,
            CORNER_TOP_5,
            CORNER_TOP_6,
            CORNER_TOP_RIGHT
        )

        val BOTTOM_REGION_SLOTS = listOf(
            CORNER_BOTTOM_LEFT,
            CORNER_BOTTOM_2,
            CORNER_BOTTOM_3,
            CORNER_BOTTOM_CENTER,
            CORNER_BOTTOM_5,
            CORNER_BOTTOM_6,
            CORNER_BOTTOM_RIGHT
        )

        fun getActiveRegionSlots(numRegions: Int): List<Int> {
            val n = numRegions.coerceIn(1, 7)
            if (n == 1) return listOf(3)
            if (n == 2) return listOf(0, 6)
            val slots = mutableListOf<Int>()
            for (i in 0 until n) {
                val slotIndex = kotlin.math.round(i * 6.0 / (n - 1)).toInt()
                if (!slots.contains(slotIndex)) {
                    slots.add(slotIndex)
                }
            }
            return slots
        }

        fun getActiveTopTriggers(numRegions: Int): List<GestureTrigger> {
            return getActiveRegionSlots(numRegions).map { TOP_REGION_SLOTS[it] }
        }

        fun getActiveBottomTriggers(numRegions: Int): List<GestureTrigger> {
            return getActiveRegionSlots(numRegions).map { BOTTOM_REGION_SLOTS[it] }
        }

        fun fromKey(key: String): GestureTrigger? = entries.find { it.key.equals(key, ignoreCase = true) }
    }
}
