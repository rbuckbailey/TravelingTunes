import com.travelingtunes.app.core.model.GestureAction
import com.travelingtunes.app.core.model.GestureTrigger
import com.travelingtunes.app.core.model.RepeatMode
import com.travelingtunes.app.core.model.ShuffleMode
import com.travelingtunes.app.core.model.ThemeSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class GestureAndSettingsTest {

    @Test
    fun testDefaultGestureActionResolution() {
        val action = GestureAction.fromKey("PlayPause")
        assertEquals(GestureAction.PLAY_PAUSE, action)
    }

    @Test
    fun testTriggerLookup() {
        val trigger = GestureTrigger.fromKey("1SwipeUp")
        assertEquals(GestureTrigger.SWIPE_1_UP, trigger)
        assertEquals("VolumeUp", trigger?.defaultActionKey)
    }

    @Test
    fun testDefaultThemeSettings() {
        val themeSettings = ThemeSettings()
        assertEquals("White on Grey", themeSettings.currentThemeName)
        assertEquals(6, themeSettings.sunRiseHour)
        assertEquals(19, themeSettings.sunSetHour)
    }

    @Test
    fun testRepeatModes() {
        assertEquals("Repeat Off", RepeatMode.OFF.displayName)
        assertEquals("Repeat Song", RepeatMode.SONG.displayName)
        assertEquals("Repeat Album", RepeatMode.ALBUM.displayName)
        assertEquals("Repeat Artist", RepeatMode.ARTIST.displayName)
        assertEquals("Repeat Genre", RepeatMode.GENRE.displayName)
    }

    @Test
    fun testShuffleModes() {
        assertEquals("Shuffle Off", ShuffleMode.OFF.displayName)
        assertEquals("Shuffle All", ShuffleMode.ALL.displayName)
        assertEquals("Shuffle Genre", ShuffleMode.GENRE.displayName)
        assertEquals("Shuffle Artist", ShuffleMode.ARTIST.displayName)
        assertEquals("Shuffle Album", ShuffleMode.ALBUM.displayName)
    }

    @Test
    fun testScreenRegionTriggers() {
        val topLeft = GestureTrigger.fromKey("TopLeft")
        assertEquals(GestureTrigger.CORNER_TOP_LEFT, topLeft)
        assertEquals("ToggleRepeat", topLeft?.defaultActionKey)

        val topRight = GestureTrigger.fromKey("TopRight")
        assertEquals(GestureTrigger.CORNER_TOP_RIGHT, topRight)
        assertEquals("ToggleShuffle", topRight?.defaultActionKey)
    }
}
