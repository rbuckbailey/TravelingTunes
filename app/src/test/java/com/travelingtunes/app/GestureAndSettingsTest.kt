package com.travelingtunes.app

import com.travelingtunes.app.core.model.GestureAction
import com.travelingtunes.app.core.model.GestureTrigger
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
        assertEquals(false, themeSettings.isRounded)
        assertEquals(false, themeSettings.isGlass)
    }

    @Test
    fun testThemeSettingsRoundedAndGlass() {
        val themeSettings = ThemeSettings(isRounded = true, isGlass = true)
        assertEquals(true, themeSettings.isRounded)
        assertEquals(true, themeSettings.isGlass)
    }

    @Test
    fun testArtLayoutOptions() {
        assertEquals("Behind Titles", com.travelingtunes.app.core.model.ArtLayoutOption.OVERLAY.displayName)
        assertEquals("Docked (Displace Titles)", com.travelingtunes.app.core.model.ArtLayoutOption.DOCKED.displayName)
    }

    @Test
    fun testRepeatModes() {
        assertEquals("Repeat Off", com.travelingtunes.app.core.model.RepeatMode.OFF.displayName)
        assertEquals("Repeat Song", com.travelingtunes.app.core.model.RepeatMode.SONG.displayName)
        assertEquals("Repeat Album", com.travelingtunes.app.core.model.RepeatMode.ALBUM.displayName)
        assertEquals("Repeat Artist", com.travelingtunes.app.core.model.RepeatMode.ARTIST.displayName)
        assertEquals("Repeat Genre", com.travelingtunes.app.core.model.RepeatMode.GENRE.displayName)
        assertEquals("Repeat Folder", com.travelingtunes.app.core.model.RepeatMode.FOLDER.displayName)
    }

    @Test
    fun testShuffleModes() {
        assertEquals("Shuffle Off", com.travelingtunes.app.core.model.ShuffleMode.OFF.displayName)
        assertEquals("Shuffle Songs", com.travelingtunes.app.core.model.ShuffleMode.SONGS.displayName)
        assertEquals("Shuffle Albums", com.travelingtunes.app.core.model.ShuffleMode.ALBUMS.displayName)
    }

    @Test
    fun testAutoByArtThemePreset() {
        val theme = com.travelingtunes.app.core.model.ColorTheme.getByName("Auto By Art")
        assertEquals("Auto By Art", theme.name)
        org.junit.Assert.assertTrue(com.travelingtunes.app.core.model.ColorTheme.PRESETS.contains(theme))
    }

    @Test
    fun testArtScaleOptions() {
        assertEquals("Fill Screen", com.travelingtunes.app.core.model.ArtScaleOption.FILL_SCREEN.displayName)
        assertEquals("Fit Screen", com.travelingtunes.app.core.model.ArtScaleOption.ASPECT_FIT.displayName)
    }

    @Test
    fun testArtAlignmentOptions() {
        assertEquals("Top", com.travelingtunes.app.core.model.ArtAlignmentPortrait.TOP.displayName)
        assertEquals("Middle", com.travelingtunes.app.core.model.ArtAlignmentPortrait.MIDDLE.displayName)
        assertEquals("Bottom", com.travelingtunes.app.core.model.ArtAlignmentPortrait.BOTTOM.displayName)
        assertEquals("Left", com.travelingtunes.app.core.model.ArtAlignmentPortrait.LEFT.displayName)
        assertEquals("Center", com.travelingtunes.app.core.model.ArtAlignmentPortrait.CENTER.displayName)
        assertEquals("Right", com.travelingtunes.app.core.model.ArtAlignmentPortrait.RIGHT.displayName)

        assertEquals("Left", com.travelingtunes.app.core.model.ArtAlignmentLandscape.LEFT.displayName)
        assertEquals("Center", com.travelingtunes.app.core.model.ArtAlignmentLandscape.CENTER.displayName)
        assertEquals("Right", com.travelingtunes.app.core.model.ArtAlignmentLandscape.RIGHT.displayName)
        assertEquals("Top", com.travelingtunes.app.core.model.ArtAlignmentLandscape.TOP.displayName)
        assertEquals("Middle", com.travelingtunes.app.core.model.ArtAlignmentLandscape.MIDDLE.displayName)
        assertEquals("Bottom", com.travelingtunes.app.core.model.ArtAlignmentLandscape.BOTTOM.displayName)
    }

    @Test
    fun testDefaultDisplaySettingsKeepScreenOnAndImmersiveMode() {
        val displaySettings = com.travelingtunes.app.core.model.DisplaySettings()
        org.junit.Assert.assertTrue(displaySettings.keepScreenOn)
        org.junit.Assert.assertTrue(displaySettings.immersiveMode)
    }
}
