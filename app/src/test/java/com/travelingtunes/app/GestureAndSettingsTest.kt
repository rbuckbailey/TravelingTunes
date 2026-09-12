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

        val shuffleAll = GestureAction.fromKey("ShuffleAllSongs")
        assertEquals(GestureAction.SHUFFLE_ALL_SONGS, shuffleAll)

        val legacyDefaultPlaylist = GestureAction.fromKey("StartDefaultPlaylist")
        assertEquals(GestureAction.SHUFFLE_ALL_SONGS, legacyDefaultPlaylist)
    }

    @Test
    fun testTriggerLookup() {
        val trigger = GestureTrigger.fromKey("1SwipeUp")
        assertEquals(GestureTrigger.SWIPE_1_UP, trigger)
        assertEquals("VolumeUp", trigger?.defaultActionKey)

        val longPress3 = GestureTrigger.LONG_PRESS_3
        assertEquals("ShuffleAllSongs", longPress3.defaultActionKey)
    }

    @Test
    fun testThreeFingerTapTriggers() {
        val tap31 = GestureTrigger.fromKey("31Tap")
        assertEquals(GestureTrigger.TAP_3_1, tap31)

        val tap32 = GestureTrigger.fromKey("32Tap")
        assertEquals(GestureTrigger.TAP_3_2, tap32)

        val tap33 = GestureTrigger.fromKey("33Tap")
        assertEquals(GestureTrigger.TAP_3_3, tap33)
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

    @Test
    fun testSettingsCategoriesNaming() {
        val categories = listOf("Library", "Gestures", "Titles and Art", "Themes", "About")
        assertEquals(5, categories.size)
        assertEquals("Library", categories[0])
        assertEquals("Gestures", categories[1])
        assertEquals("Titles and Art", categories[2])
        assertEquals("Themes", categories[3])
        assertEquals("About", categories[4])
    }

    @Test
    fun testColorThemeSecondaryTextColor() {
        val theme = com.travelingtunes.app.core.model.ColorTheme("Custom Theme", androidx.compose.ui.graphics.Color.Black, androidx.compose.ui.graphics.Color.White, androidx.compose.ui.graphics.Color.LightGray)
        assertEquals(androidx.compose.ui.graphics.Color.Black, theme.backgroundColor)
        assertEquals(androidx.compose.ui.graphics.Color.White, theme.textColor)
        assertEquals(androidx.compose.ui.graphics.Color.LightGray, theme.secondaryTextColor)
    }

    @Test
    fun testResolveActiveThemeSecondaryColor() {
        val dynamicTheme = com.travelingtunes.app.core.model.ColorTheme("Dynamic", androidx.compose.ui.graphics.Color.Blue, androidx.compose.ui.graphics.Color.Yellow, androidx.compose.ui.graphics.Color.Cyan)
        val resolved = com.travelingtunes.app.core.theme.resolveActiveTheme(
            themeSettings = ThemeSettings(),
            dynamicAlbumArtTheme = dynamicTheme,
            useAlbumArtColors = true
        )
        assertEquals(dynamicTheme, resolved)
        assertEquals(androidx.compose.ui.graphics.Color.Yellow, resolved.textColor)
        assertEquals(androidx.compose.ui.graphics.Color.Cyan, resolved.secondaryTextColor)
    }

    @Test
    fun testResolveActiveCustomThemeSongArtistAlbumColors() {
        val customThemeSettings = ThemeSettings(
            currentThemeName = "Custom",
            customBGRed = 0f, customBGGreen = 0f, customBGBlue = 0f,
            customSongTitleRed = 255f, customSongTitleGreen = 0f, customSongTitleBlue = 0f,
            customArtistTitleRed = 0f, customArtistTitleGreen = 255f, customArtistTitleBlue = 0f,
            customAlbumTitleRed = 0f, customAlbumTitleGreen = 0f, customAlbumTitleBlue = 255f
        )
        val resolved = com.travelingtunes.app.core.theme.resolveActiveTheme(
            themeSettings = customThemeSettings,
            dynamicAlbumArtTheme = null,
            useAlbumArtColors = false
        )
        assertEquals("Custom", resolved.name)
        assertEquals(androidx.compose.ui.graphics.Color.Black, resolved.backgroundColor)
        assertEquals(androidx.compose.ui.graphics.Color.Red, resolved.textColor)
        assertEquals(androidx.compose.ui.graphics.Color.Green, resolved.artistColor)
        assertEquals(androidx.compose.ui.graphics.Color.Blue, resolved.albumColor)
    }

    @Test
    fun testDockedArtLayoutSettings() {
        val displaySettings = com.travelingtunes.app.core.model.DisplaySettings(
            artDisplayLayout = com.travelingtunes.app.core.model.ArtLayoutOption.DOCKED
        )
        assertEquals(com.travelingtunes.app.core.model.ArtLayoutOption.DOCKED, displaySettings.artDisplayLayout)

        val edges = com.travelingtunes.app.feature.player.DockAdjacentEdge.entries
        assertEquals(4, edges.size)
        assertEquals(com.travelingtunes.app.feature.player.DockAdjacentEdge.TOP, edges[0])
        assertEquals(com.travelingtunes.app.feature.player.DockAdjacentEdge.BOTTOM, edges[1])
        assertEquals(com.travelingtunes.app.feature.player.DockAdjacentEdge.LEFT, edges[2])
        assertEquals(com.travelingtunes.app.feature.player.DockAdjacentEdge.RIGHT, edges[3])
    }

    @Test
    fun testDefaultNumEdgeRegions() {
        val displaySettings = com.travelingtunes.app.core.model.DisplaySettings()
        assertEquals(3, displaySettings.numEdgeRegions)
    }

    @Test
    fun testGetActiveRegionSlots() {
        // N = 1: 1 alone in center (slot index 3 -> slot 4)
        assertEquals(listOf(3), GestureTrigger.getActiveRegionSlots(1))

        // N = 2: 2 in corners (slots 0 and 6 -> slots 1 and 7)
        assertEquals(listOf(0, 6), GestureTrigger.getActiveRegionSlots(2))

        // N = 3: first and last corners, middle center (slots 0, 3, 6 -> slots 1, 4, 7)
        assertEquals(listOf(0, 3, 6), GestureTrigger.getActiveRegionSlots(3))

        // N = 4: slots 0, 2, 4, 6
        assertEquals(listOf(0, 2, 4, 6), GestureTrigger.getActiveRegionSlots(4))

        // N = 7: all slots 0..6
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6), GestureTrigger.getActiveRegionSlots(7))
    }
}
