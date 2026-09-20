package com.travelingtunes.app

import android.content.Context
import android.media.AudioManager
import com.travelingtunes.app.core.location.SpeedVolumeManager
import com.travelingtunes.app.core.model.GestureAction
import com.travelingtunes.app.core.model.GestureTrigger
import com.travelingtunes.app.core.model.SlideDirection
import com.travelingtunes.app.core.model.getSlideDirection
import com.travelingtunes.app.core.model.getReverseTrigger
import com.travelingtunes.app.core.model.ThemeSettings
import com.travelingtunes.app.core.theme.adjustContrastForBackground
import androidx.compose.ui.graphics.toArgb
import com.travelingtunes.app.core.datastore.SettingsBackupHelper
import com.travelingtunes.app.core.model.GestureBinding
import com.travelingtunes.app.core.model.Profile
import com.travelingtunes.app.core.model.ProfileSelectionMode
import com.travelingtunes.app.feature.settings.GestureSubmenu
import com.travelingtunes.app.feature.settings.getSubmenu
import com.travelingtunes.app.feature.settings.getTriggersForSubmenu
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito

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
        assertEquals("Match Album Art", themeSettings.currentThemeName)
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
    fun testMatchAlbumArtThemePreset() {
        val theme = com.travelingtunes.app.core.model.ColorTheme.getByName("Match Album Art")
        assertEquals("Match Album Art", theme.name)
        assertEquals(com.travelingtunes.app.core.model.ColorTheme.PRESETS.first(), theme)
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
    fun testTitleFontOptionsDefaultsAndUpdates() {
        val defaultSettings = com.travelingtunes.app.core.model.DisplaySettings()
        org.junit.Assert.assertTrue(defaultSettings.artistBold)
        org.junit.Assert.assertFalse(defaultSettings.artistItalic)
        org.junit.Assert.assertFalse(defaultSettings.artistUnderline)

        org.junit.Assert.assertTrue(defaultSettings.songBold)
        org.junit.Assert.assertFalse(defaultSettings.songItalic)
        org.junit.Assert.assertFalse(defaultSettings.songUnderline)

        org.junit.Assert.assertFalse(defaultSettings.albumBold)
        org.junit.Assert.assertFalse(defaultSettings.albumItalic)
        org.junit.Assert.assertFalse(defaultSettings.albumUnderline)

        val updatedSettings = defaultSettings.copy(
            artistItalic = true,
            songUnderline = true,
            albumBold = true
        )
        org.junit.Assert.assertTrue(updatedSettings.artistItalic)
        org.junit.Assert.assertTrue(updatedSettings.songUnderline)
        org.junit.Assert.assertTrue(updatedSettings.albumBold)
    }

    @Test
    fun testSettingsCategoriesNaming() {
        val submenus = com.travelingtunes.app.feature.settings.SettingsSubmenu.entries
        assertEquals(9, submenus.size)
        assertEquals(com.travelingtunes.app.feature.settings.SettingsSubmenu.PROFILES, submenus[0])
        assertEquals(com.travelingtunes.app.feature.settings.SettingsSubmenu.LIBRARY, submenus[1])
        assertEquals(com.travelingtunes.app.feature.settings.SettingsSubmenu.TITLES, submenus[2])
        assertEquals(com.travelingtunes.app.feature.settings.SettingsSubmenu.ART, submenus[3])
        assertEquals(com.travelingtunes.app.feature.settings.SettingsSubmenu.HUD, submenus[4])
        assertEquals(com.travelingtunes.app.feature.settings.SettingsSubmenu.THEMES, submenus[5])
        assertEquals(com.travelingtunes.app.feature.settings.SettingsSubmenu.GESTURES, submenus[6])
        assertEquals(com.travelingtunes.app.feature.settings.SettingsSubmenu.ANDROID_AUTO, submenus[7])
        assertEquals(com.travelingtunes.app.feature.settings.SettingsSubmenu.ABOUT, submenus[8])

        val standalone = submenus.filter { it.categoryGroup == null }
        assertEquals(2, standalone.size)
        assertTrue(standalone.contains(com.travelingtunes.app.feature.settings.SettingsSubmenu.PROFILES))
        assertTrue(standalone.contains(com.travelingtunes.app.feature.settings.SettingsSubmenu.LIBRARY))

        val appearanceGroup = submenus.filter { it.categoryGroup == "Appearance" }
        assertEquals(4, appearanceGroup.size)

        val controlsGroup = submenus.filter { it.categoryGroup == "Controls" }
        assertEquals(3, controlsGroup.size)
        assertEquals(com.travelingtunes.app.feature.settings.SettingsSubmenu.GESTURES, controlsGroup[0])
        assertEquals(com.travelingtunes.app.feature.settings.SettingsSubmenu.ANDROID_AUTO, controlsGroup[1])
        assertEquals(com.travelingtunes.app.feature.settings.SettingsSubmenu.ABOUT, controlsGroup[2])
    }

    @Test
    fun testAutoRescanKeyDefinition() {
        val key = com.travelingtunes.app.core.datastore.SettingsDataStore.KEY_AUTO_RESCAN
        assertEquals("autoRescan", key.name)
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
            customAlbumTitleRed = 0f, customAlbumTitleGreen = 255f, customAlbumTitleBlue = 255f
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
        assertEquals(androidx.compose.ui.graphics.Color.Cyan, resolved.albumColor)
    }

    @Test
    fun testAdjustContrastForBackgroundLowContrastStaticTheme() {
        val lightText = androidx.compose.ui.graphics.Color(0xFFE0E0E0)
        val lightBg = androidx.compose.ui.graphics.Color(0xFFF5F5F5)

        val adjusted = adjustContrastForBackground(
            textColor = lightText,
            backgroundColor = lightBg,
            isMatchedTheme = false
        )

        val contrastRatio = com.travelingtunes.app.core.theme.calculateWcagContrast(
            adjusted.toArgb(),
            lightBg.toArgb()
        )
        assertTrue("Contrast ratio should be >= 4.5, was $contrastRatio", contrastRatio >= 4.5)
    }

    @Test
    fun testAdjustContrastForBackgroundMatchedThemeCycling() {
        val lowContrastText = androidx.compose.ui.graphics.Color(0xFF303030)
        val darkBg = androidx.compose.ui.graphics.Color(0xFF121212)
        val highContrastMatchedSwatch = android.graphics.Color.YELLOW

        val adjusted = adjustContrastForBackground(
            textColor = lowContrastText,
            backgroundColor = darkBg,
            matchedSwatches = listOf(android.graphics.Color.BLACK, highContrastMatchedSwatch),
            isMatchedTheme = true
        )

        assertEquals(androidx.compose.ui.graphics.Color(highContrastMatchedSwatch), adjusted)
    }

    @Test
    fun testAdjustContrastForBackgroundMatchedThemeLowContrastFallbackToWhiteOrBlack() {
        val lowContrastText = androidx.compose.ui.graphics.Color(0xFF222222)
        val darkBg = androidx.compose.ui.graphics.Color(0xFF121212)
        // Swatches that all have low contrast (< 4.5) against darkBg
        val lowContrastSwatches = listOf(
            android.graphics.Color.rgb(0x20, 0x20, 0x20),
            android.graphics.Color.rgb(0x30, 0x30, 0x30)
        )

        val adjusted = adjustContrastForBackground(
            textColor = lowContrastText,
            backgroundColor = darkBg,
            matchedSwatches = lowContrastSwatches,
            isMatchedTheme = true
        )

        val contrastRatio = com.travelingtunes.app.core.theme.calculateWcagContrast(
            adjusted.toArgb(),
            darkBg.toArgb()
        )
        assertTrue("Contrast ratio should be >= 4.5, was $contrastRatio", contrastRatio >= 4.5)
        assertTrue("Fallback color should be White or Black", adjusted == androidx.compose.ui.graphics.Color.White || adjusted == androidx.compose.ui.graphics.Color.Black)
    }

    @Test
    fun testAdjustSecondaryContrastForBackgroundDistinctHueAndContrast() {
        val darkBg = androidx.compose.ui.graphics.Color(0xFF000020)
        val primaryYellowText = androidx.compose.ui.graphics.Color(0xFFFFD700)
        val sameHueLightYellow = 0xFFFFE033.toInt()
        val distinctHueCyan = 0xFF00E5FF.toInt()

        val adjustedSecondary = com.travelingtunes.app.core.theme.adjustSecondaryContrastForBackground(
            secondaryColor = primaryYellowText,
            primaryColor = primaryYellowText,
            backgroundColor = darkBg,
            matchedSwatches = listOf(sameHueLightYellow, distinctHueCyan),
            isMatchedTheme = true
        )

        assertEquals(androidx.compose.ui.graphics.Color(distinctHueCyan), adjustedSecondary)
    }

    @Test
    fun testDockedArtLayoutSettings() {
        val displaySettings = com.travelingtunes.app.core.model.DisplaySettings(
            artDisplayLayout = com.travelingtunes.app.core.model.ArtLayoutOption.DOCKED,
            showAlbumArt = true
        )
        assertEquals(com.travelingtunes.app.core.model.ArtLayoutOption.DOCKED, displaySettings.artDisplayLayout)
        assertTrue(displaySettings.showAlbumArt)

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

    @Test
    fun testUserFacingRegionMappingForVariousSlotCounts() {
        for (numRegions in 1..7) {
            val topTriggers = GestureTrigger.getActiveTopTriggers(numRegions)
            val bottomTriggers = GestureTrigger.getActiveBottomTriggers(numRegions)

            assertEquals(numRegions, topTriggers.size)
            assertEquals(numRegions, bottomTriggers.size)

            for (i in 0 until numRegions) {
                val userFacingNum = i + 1
                val topTrigger = topTriggers[i]
                val bottomTrigger = bottomTriggers[i]

                assertEquals(userFacingNum, topTrigger.getUserFacingRegionNumber(numRegions))
                assertEquals(userFacingNum, bottomTrigger.getUserFacingRegionNumber(numRegions))

                assertEquals("Top Region $userFacingNum", topTrigger.getDisplayName(numRegions))
                assertEquals("Bottom Region $userFacingNum", bottomTrigger.getDisplayName(numRegions))

                assertEquals(topTrigger, GestureTrigger.getTopTriggerForUserRegion(userFacingNum, numRegions))
                assertEquals(bottomTrigger, GestureTrigger.getBottomTriggerForUserRegion(userFacingNum, numRegions))
            }
        }
    }

    @Test
    fun testSlideDirectionForTriggers() {
        // Buttons: Top regions -> BOTTOM, Bottom regions -> TOP
        assertEquals(com.travelingtunes.app.core.model.SlideDirection.BOTTOM, GestureTrigger.CORNER_TOP_LEFT.getSlideDirection())
        assertEquals(com.travelingtunes.app.core.model.SlideDirection.BOTTOM, GestureTrigger.CORNER_TOP_CENTER.getSlideDirection())
        assertEquals(com.travelingtunes.app.core.model.SlideDirection.TOP, GestureTrigger.CORNER_BOTTOM_LEFT.getSlideDirection())
        assertEquals(com.travelingtunes.app.core.model.SlideDirection.TOP, GestureTrigger.CORNER_BOTTOM_RIGHT.getSlideDirection())

        // Taps: -> TOP
        assertEquals(com.travelingtunes.app.core.model.SlideDirection.TOP, GestureTrigger.TAP_1_1.getSlideDirection())
        assertEquals(com.travelingtunes.app.core.model.SlideDirection.TOP, GestureTrigger.TAP_2_1.getSlideDirection())
        assertEquals(com.travelingtunes.app.core.model.SlideDirection.TOP, GestureTrigger.LONG_PRESS_1.getSlideDirection())

        // Slide gestures (Swipes): Reversed directions (Swipe UP -> BOTTOM, Swipe DOWN -> TOP, Swipe LEFT -> RIGHT, Swipe RIGHT -> LEFT)
        assertEquals(com.travelingtunes.app.core.model.SlideDirection.BOTTOM, GestureTrigger.SWIPE_1_UP.getSlideDirection())
        assertEquals(com.travelingtunes.app.core.model.SlideDirection.TOP, GestureTrigger.SWIPE_1_DOWN.getSlideDirection())
        assertEquals(com.travelingtunes.app.core.model.SlideDirection.RIGHT, GestureTrigger.SWIPE_1_LEFT.getSlideDirection())
        assertEquals(com.travelingtunes.app.core.model.SlideDirection.LEFT, GestureTrigger.SWIPE_1_RIGHT.getSlideDirection())
    }

    @Test
    fun testSeparateTouchZonesSettingAndGestureBindings() {
        val displaySettings = com.travelingtunes.app.core.model.DisplaySettings(
            artDisplayLayout = com.travelingtunes.app.core.model.ArtLayoutOption.DOCKED,
            separateTouchZones = true
        )
        assertTrue(displaySettings.separateTouchZones)
        assertEquals(3, displaySettings.numArtEdgeRegions)

        val binding = com.travelingtunes.app.core.model.GestureBinding(
            trigger = GestureTrigger.SWIPE_1_LEFT,
            action = GestureAction.UNASSIGNED,
            artAction = GestureAction.NEXT,
            titleAction = GestureAction.PREVIOUS
        )

        assertEquals(GestureAction.UNASSIGNED, binding.action)
        assertEquals(GestureAction.NEXT, binding.artAction)
        assertEquals(GestureAction.PREVIOUS, binding.titleAction)
    }

    @Test
    fun testNumArtEdgeRegionsDefaultAndSlider() {
        val defaultDisplay = com.travelingtunes.app.core.model.DisplaySettings()
        assertEquals(3, defaultDisplay.numArtEdgeRegions)

        val updatedDisplay = defaultDisplay.copy(numArtEdgeRegions = 5)
        assertEquals(5, updatedDisplay.numArtEdgeRegions)
    }

    @Test
    fun testSubmenuTriggersForSeparateTouchZones() {
        val standardSubmenu = com.travelingtunes.app.feature.settings.getTriggersForSubmenu(
            submenu = com.travelingtunes.app.feature.settings.GestureSubmenu.BUTTON,
            numEdgeRegions = 3,
            numArtEdgeRegions = 3,
            isSeparateTouchZones = false
        )
        assertTrue(standardSubmenu.containsKey("Top Edge Regions"))
        assertTrue(standardSubmenu.containsKey("Bottom Edge Regions"))

        val separateSubmenu = com.travelingtunes.app.feature.settings.getTriggersForSubmenu(
            submenu = com.travelingtunes.app.feature.settings.GestureSubmenu.BUTTON,
            numEdgeRegions = 3,
            numArtEdgeRegions = 4,
            isSeparateTouchZones = true
        )
        assertTrue(separateSubmenu.containsKey("Top Title Edge Regions"))
        assertTrue(separateSubmenu.containsKey("Bottom Title Edge Regions"))
        assertTrue(separateSubmenu.containsKey("Top Art Edge Regions"))
        assertTrue(separateSubmenu.containsKey("Bottom Art Edge Regions"))

        assertEquals(3, separateSubmenu["Top Title Edge Regions"]?.size)
        assertEquals(4, separateSubmenu["Top Art Edge Regions"]?.size)
    }

    @Test
    fun testGestureTriggerDisplayNameForArtAndTitle() {
        val top1 = GestureTrigger.CORNER_TOP_LEFT
        assertEquals("Top Region 1", top1.getDisplayName(3))
        assertEquals("Top Art Region 1", top1.getDisplayName(3, isArt = true))
        assertEquals("Top Title Region 1", top1.getDisplayName(3, isTitle = true))
    }

    @Test
    fun testTouchRegionTargetEnum() {
        val targets = com.travelingtunes.app.core.model.TouchRegionTarget.entries
        assertEquals(3, targets.size)
        assertEquals("Both", com.travelingtunes.app.core.model.TouchRegionTarget.BOTH.displayName)
        assertEquals("Art", com.travelingtunes.app.core.model.TouchRegionTarget.ART.displayName)
        assertEquals("Title", com.travelingtunes.app.core.model.TouchRegionTarget.TITLE.displayName)
    }

    @Test
    fun testMondrianThemePreset() {
        val theme = com.travelingtunes.app.core.model.ColorTheme.getByName("Mondrian")
        assertEquals("Mondrian", theme.name)
        org.junit.Assert.assertTrue(com.travelingtunes.app.core.model.ColorTheme.PRESETS.contains(theme))
        assertEquals(androidx.compose.ui.graphics.Color.White, theme.backgroundColor)
        assertEquals(androidx.compose.ui.graphics.Color.Black, theme.textColor)
    }

    @Test
    fun testMondrianLayoutDeterministicPerAlbum() {
        val mockUri = org.mockito.Mockito.mock(android.net.Uri::class.java)
        val song1 = com.travelingtunes.app.core.model.Song(
            id = 1L, title = "Song A", artist = "Artist 1", album = "Album Red", albumId = 100L,
            durationMs = 180000L, contentUri = mockUri, artworkUri = null
        )
        val song2 = com.travelingtunes.app.core.model.Song(
            id = 1L, title = "Song A", artist = "Artist 1", album = "Album Red", albumId = 100L,
            durationMs = 200000L, contentUri = mockUri, artworkUri = null
        )
        val song3 = com.travelingtunes.app.core.model.Song(
            id = 3L, title = "Song C", artist = "Artist 2", album = "Album Blue", albumId = 200L,
            durationMs = 210000L, contentUri = mockUri, artworkUri = null
        )

        val layout1 = com.travelingtunes.app.core.theme.MondrianThemeHelper.generateLayoutForSong(song1)
        val layout2 = com.travelingtunes.app.core.theme.MondrianThemeHelper.generateLayoutForSong(song2)
        val layout3 = com.travelingtunes.app.core.theme.MondrianThemeHelper.generateLayoutForSong(song3)

        // Same album and track produces identical Mondrian layout
        assertEquals(layout1, layout2)
        // Different album produces different Mondrian layout
        org.junit.Assert.assertNotEquals(layout1, layout3)
    }

    @Test
    fun testMondrianAtMostOneSubdivisionPerTrack() {
        val mockUri = org.mockito.Mockito.mock(android.net.Uri::class.java)
        val primaryColors = com.travelingtunes.app.core.theme.MondrianThemeHelper.PRIMARY_COLORS
        val secondaryColors = com.travelingtunes.app.core.theme.MondrianThemeHelper.SECONDARY_HALF_COLORS

        for (i in 1..20) {
            val song = com.travelingtunes.app.core.model.Song(
                id = i.toLong(), title = "Track $i", artist = "Artist", album = "Album X", albumId = 10L,
                durationMs = 180000L, contentUri = mockUri, artworkUri = null
            )
            val layout = com.travelingtunes.app.core.theme.MondrianThemeHelper.generateLayoutForSong(song)
            val corners = listOf(layout.topLeft, layout.topRight, layout.bottomLeft, layout.bottomRight)

            val subdividedCount = corners.count { it.isSubdivided }
            org.junit.Assert.assertTrue("Subdivided count $subdividedCount should be <= 1", subdividedCount <= 1)

            corners.forEach { corner ->
                org.junit.Assert.assertTrue(primaryColors.contains(corner.color1))
                if (corner.isSubdivided) {
                    org.junit.Assert.assertNotNull(corner.color2)
                    org.junit.Assert.assertTrue(secondaryColors.contains(corner.color2))
                } else {
                    org.junit.Assert.assertNull(corner.color2)
                }
            }
        }
    }

    @Test
    fun testMondrianNoAdjacentNonWhiteIdenticalColors() {
        val mockUri = org.mockito.Mockito.mock(android.net.Uri::class.java)

        for (i in 1..50) {
            val song = com.travelingtunes.app.core.model.Song(
                id = i.toLong(), title = "Track $i", artist = "Artist $i", album = "Album $i", albumId = i.toLong() * 10L,
                durationMs = 180000L, contentUri = mockUri, artworkUri = null
            )
            val layout = com.travelingtunes.app.core.theme.MondrianThemeHelper.generateLayoutForSong(song)

            // Main 4 region base colors: non-white adjacent pair check
            if (layout.topLeft.color1 != com.travelingtunes.app.core.theme.MondrianThemeHelper.COLOR_WHITE) {
                org.junit.Assert.assertNotEquals("TL and TR non-white base colors must not match", layout.topLeft.color1, layout.topRight.color1)
                org.junit.Assert.assertNotEquals("TL and BL non-white base colors must not match", layout.topLeft.color1, layout.bottomLeft.color1)
            }
            if (layout.topRight.color1 != com.travelingtunes.app.core.theme.MondrianThemeHelper.COLOR_WHITE) {
                org.junit.Assert.assertNotEquals("TR and BR non-white base colors must not match", layout.topRight.color1, layout.bottomRight.color1)
            }
            if (layout.bottomLeft.color1 != com.travelingtunes.app.core.theme.MondrianThemeHelper.COLOR_WHITE) {
                org.junit.Assert.assertNotEquals("BL and BR non-white base colors must not match", layout.bottomLeft.color1, layout.bottomRight.color1)
            }

            // Subdivided corners
            val corners = listOf(layout.topLeft, layout.topRight, layout.bottomLeft, layout.bottomRight)
            corners.forEach { corner ->
                if (corner.isSubdivided && corner.color1 != com.travelingtunes.app.core.theme.MondrianThemeHelper.COLOR_WHITE) {
                    org.junit.Assert.assertNotEquals("Subdivided non-white half 1 and half 2 colors must not match", corner.color1, corner.color2)
                }
            }
        }
    }

    @Test
    fun testMondrianRoughlyFiftyPercentWhite() {
        val mockUri = org.mockito.Mockito.mock(android.net.Uri::class.java)
        var totalRegions = 0
        var whiteRegions = 0

        for (i in 1..100) {
            val song = com.travelingtunes.app.core.model.Song(
                id = i.toLong(), title = "Track $i", artist = "Artist $i", album = "Album $i", albumId = i.toLong() * 10L,
                durationMs = 180000L, contentUri = mockUri, artworkUri = null
            )
            val layout = com.travelingtunes.app.core.theme.MondrianThemeHelper.generateLayoutForSong(song)
            val corners = listOf(layout.topLeft, layout.topRight, layout.bottomLeft, layout.bottomRight)

            corners.forEach { corner ->
                totalRegions++
                if (corner.color1 == com.travelingtunes.app.core.theme.MondrianThemeHelper.COLOR_WHITE) {
                    whiteRegions++
                }
            }
        }

        val whitePercentage = (whiteRegions.toDouble() / totalRegions.toDouble()) * 100.0
        // Expecting white percentage to be roughly 40% - 60%
        org.junit.Assert.assertTrue("White percentage $whitePercentage% should be between 35% and 65%", whitePercentage in 35.0..65.0)
    }

    @Test
    fun testMondrianAtLeastTwoColoredSpacesAndOnlyWhiteRepeats() {
        val mockUri = org.mockito.Mockito.mock(android.net.Uri::class.java)
        val red = com.travelingtunes.app.core.theme.MondrianThemeHelper.COLOR_RED
        val yellow = com.travelingtunes.app.core.theme.MondrianThemeHelper.COLOR_YELLOW
        val blue = com.travelingtunes.app.core.theme.MondrianThemeHelper.COLOR_BLUE
        val white = com.travelingtunes.app.core.theme.MondrianThemeHelper.COLOR_WHITE

        for (i in 1..100) {
            val song = com.travelingtunes.app.core.model.Song(
                id = i.toLong(), title = "Track $i", artist = "Artist $i", album = "Album $i", albumId = i.toLong() * 10L,
                durationMs = 180000L, contentUri = mockUri, artworkUri = null
            )
            val layout = com.travelingtunes.app.core.theme.MondrianThemeHelper.generateLayoutForSong(song)
            val corners = listOf(layout.topLeft, layout.topRight, layout.bottomLeft, layout.bottomRight)

            val colors = mutableListOf<androidx.compose.ui.graphics.Color>()
            corners.forEach { corner ->
                colors.add(corner.color1)
                corner.color2?.let { colors.add(it) }
            }

            val nonWhiteColors = colors.filter { it != white && it != com.travelingtunes.app.core.theme.MondrianThemeHelper.COLOR_BLACK }
            org.junit.Assert.assertTrue("Track $i should have at least 2 colored spaces, found ${nonWhiteColors.size}", nonWhiteColors.size >= 2)

            val redCount = colors.count { it == red }
            val yellowCount = colors.count { it == yellow }
            val blueCount = colors.count { it == blue }

            org.junit.Assert.assertTrue("Red count $redCount should be <= 1", redCount <= 1)
            org.junit.Assert.assertTrue("Yellow count $yellowCount should be <= 1", yellowCount <= 1)
            org.junit.Assert.assertTrue("Blue count $blueCount should be <= 1", blueCount <= 1)
        }
    }

    @Test
    fun testReverseTriggerMapping() {
        assertEquals(GestureTrigger.SWIPE_1_DOWN, GestureTrigger.SWIPE_1_UP.getReverseTrigger())
        assertEquals(GestureTrigger.SWIPE_1_UP, GestureTrigger.SWIPE_1_DOWN.getReverseTrigger())
        assertEquals(GestureTrigger.SWIPE_1_RIGHT, GestureTrigger.SWIPE_1_LEFT.getReverseTrigger())
        assertEquals(GestureTrigger.SWIPE_1_LEFT, GestureTrigger.SWIPE_1_RIGHT.getReverseTrigger())

        assertEquals(GestureTrigger.SWIPE_2_DOWN, GestureTrigger.SWIPE_2_UP.getReverseTrigger())
        assertEquals(GestureTrigger.SWIPE_2_UP, GestureTrigger.SWIPE_2_DOWN.getReverseTrigger())
        assertEquals(GestureTrigger.SWIPE_2_RIGHT, GestureTrigger.SWIPE_2_LEFT.getReverseTrigger())
        assertEquals(GestureTrigger.SWIPE_2_LEFT, GestureTrigger.SWIPE_2_RIGHT.getReverseTrigger())

        assertEquals(GestureTrigger.SWIPE_3_DOWN, GestureTrigger.SWIPE_3_UP.getReverseTrigger())
        assertEquals(GestureTrigger.SWIPE_3_UP, GestureTrigger.SWIPE_3_DOWN.getReverseTrigger())

        // Taps, long presses, region buttons return themselves
        assertEquals(GestureTrigger.TAP_1_1, GestureTrigger.TAP_1_1.getReverseTrigger())
        assertEquals(GestureTrigger.LONG_PRESS_1, GestureTrigger.LONG_PRESS_1.getReverseTrigger())
        assertEquals(GestureTrigger.CORNER_TOP_LEFT, GestureTrigger.CORNER_TOP_LEFT.getReverseTrigger())
    }

    @Test
    fun testSettingsBackupJsonStructure() {
        val display = com.travelingtunes.app.core.model.DisplaySettings(
            artistFontSize = 50f,
            artDisplayLayout = com.travelingtunes.app.core.model.ArtLayoutOption.DOCKED
        )
        val theme = ThemeSettings()
        val bindings = mapOf(
            GestureTrigger.SWIPE_1_UP to com.travelingtunes.app.core.model.GestureBinding(
                GestureTrigger.SWIPE_1_UP,
                GestureAction.VOLUME_UP,
                true
            )
        )

        val jsonStr = com.travelingtunes.app.core.datastore.SettingsBackupHelper.exportToJson(
            display = display,
            theme = theme,
            bindings = bindings,
            gpsVolume = false,
            gpsSens = 0.5f,
            autoRescan = true
        )

        assertTrue(jsonStr.contains("\"version\": 1"))
        assertTrue(jsonStr.contains("\"artistFontSize\": 50.0") || jsonStr.contains("\"artistFontSize\": 50"))
        assertTrue(jsonStr.contains("\"artDisplayLayout\": \"DOCKED\""))
        assertTrue(jsonStr.contains("\"1SwipeUp\""))
        assertTrue(jsonStr.contains("\"autoRescan\": true"))
    }

    @Test
    fun testGestureSubmenuCategories() {
        val submenus = com.travelingtunes.app.feature.settings.GestureSubmenu.entries
        assertEquals(5, submenus.size)
        assertEquals(com.travelingtunes.app.feature.settings.GestureSubmenu.SWIPE, submenus[0])
        assertEquals(com.travelingtunes.app.feature.settings.GestureSubmenu.TAP, submenus[1])
        assertEquals(com.travelingtunes.app.feature.settings.GestureSubmenu.BUTTON, submenus[2])
        assertEquals(com.travelingtunes.app.feature.settings.GestureSubmenu.KEYBOARD, submenus[3])
        assertEquals(com.travelingtunes.app.feature.settings.GestureSubmenu.RADIAL_MENU, submenus[4])

        assertEquals("Swipe Actions", submenus[0].title)
        assertEquals("Tap Actions", submenus[1].title)
        assertEquals("Button Actions", submenus[2].title)
        assertEquals("Keyboard Controls", submenus[3].title)
        assertEquals("Radial Menu Actions", submenus[4].title)
    }

    @Test
    fun testGetSubmenuForTriggers() {
        // Swipes -> SWIPE
        assertEquals(com.travelingtunes.app.feature.settings.GestureSubmenu.SWIPE, GestureTrigger.SWIPE_1_UP.getSubmenu())
        assertEquals(com.travelingtunes.app.feature.settings.GestureSubmenu.SWIPE, GestureTrigger.SWIPE_2_LEFT.getSubmenu())
        assertEquals(com.travelingtunes.app.feature.settings.GestureSubmenu.SWIPE, GestureTrigger.SWIPE_3_DOWN.getSubmenu())

        // Taps & Long Presses -> TAP
        assertEquals(com.travelingtunes.app.feature.settings.GestureSubmenu.TAP, GestureTrigger.TAP_1_1.getSubmenu())
        assertEquals(com.travelingtunes.app.feature.settings.GestureSubmenu.TAP, GestureTrigger.TAP_2_3.getSubmenu())
        assertEquals(com.travelingtunes.app.feature.settings.GestureSubmenu.TAP, GestureTrigger.LONG_PRESS_1.getSubmenu())
        assertEquals(com.travelingtunes.app.feature.settings.GestureSubmenu.TAP, GestureTrigger.LONG_PRESS_3.getSubmenu())

        // Edge Regions -> BUTTON
        assertEquals(com.travelingtunes.app.feature.settings.GestureSubmenu.BUTTON, GestureTrigger.CORNER_TOP_LEFT.getSubmenu())
        assertEquals(com.travelingtunes.app.feature.settings.GestureSubmenu.BUTTON, GestureTrigger.CORNER_BOTTOM_RIGHT.getSubmenu())
    }

    @Test
    fun testGetTriggersForSubmenuSections() {
        val swipeMap = com.travelingtunes.app.feature.settings.getTriggersForSubmenu(
            com.travelingtunes.app.feature.settings.GestureSubmenu.SWIPE, 3
        )
        assertEquals(3, swipeMap.size)
        assertTrue(swipeMap.containsKey("1-Finger Swipes"))
        assertTrue(swipeMap.containsKey("2-Finger Swipes"))
        assertTrue(swipeMap.containsKey("3-Finger Swipes"))
        assertEquals(4, swipeMap["1-Finger Swipes"]?.size)

        val tapMap = com.travelingtunes.app.feature.settings.getTriggersForSubmenu(
            com.travelingtunes.app.feature.settings.GestureSubmenu.TAP, 3
        )
        assertEquals(4, tapMap.size)
        assertTrue(tapMap.containsKey("1-Finger Taps"))
        assertTrue(tapMap.containsKey("2-Finger Taps"))
        assertTrue(tapMap.containsKey("3-Finger Taps"))
        assertTrue(tapMap.containsKey("Long Presses"))
        assertEquals(3, tapMap["Long Presses"]?.size)

        val buttonMap = com.travelingtunes.app.feature.settings.getTriggersForSubmenu(
            com.travelingtunes.app.feature.settings.GestureSubmenu.BUTTON, 3
        )
        assertEquals(2, buttonMap.size)
        assertTrue(buttonMap.containsKey("Top Edge Regions"))
        assertTrue(buttonMap.containsKey("Bottom Edge Regions"))
        assertEquals(3, buttonMap["Top Edge Regions"]?.size)
        assertEquals(3, buttonMap["Bottom Edge Regions"]?.size)
    }

    @Test
    fun testRadialMenuActionResolution() {
        val action = GestureAction.fromKey("RadialMenu")
        assertEquals(GestureAction.RADIAL_MENU, action)
        assertEquals("Radial Menu", GestureAction.RADIAL_MENU.displayName)
    }

    @Test
    fun testShortestAngleDiff() {
        val diff0 = com.travelingtunes.app.feature.player.shortestAngleDiff(0f, 0f)
        assertEquals(0f, diff0, 0.001f)

        val diffPi = com.travelingtunes.app.feature.player.shortestAngleDiff(0f, Math.PI.toFloat())
        assertEquals(Math.PI.toFloat(), diffPi, 0.001f)

        val diffWrap = com.travelingtunes.app.feature.player.shortestAngleDiff(-Math.PI.toFloat() + 0.1f, Math.PI.toFloat() - 0.1f)
        assertEquals(0.2f, diffWrap, 0.01f)
    }

    @Test
    fun testOtherOptionIsLastInGestureActionEntries() {
        val entries = GestureAction.entries
        assertEquals("Other Option should always be the last entry in GestureAction", GestureAction.OTHER_OPTION, entries.last())
        assertEquals("Other Option", GestureAction.OTHER_OPTION.displayName)
        assertEquals(GestureAction.OTHER_OPTION, GestureAction.fromKey("Other Option"))
        assertEquals(GestureAction.OTHER_OPTION, GestureAction.fromKey("OTHER_OPTION"))
    }

    @Test
    fun testConfigOptionRegistry() {
        val allOptions = com.travelingtunes.app.core.model.ConfigOption.ALL_OPTIONS
        assertTrue("ConfigOption.ALL_OPTIONS should contain configuration options from the config hierarchy", allOptions.isNotEmpty())

        val mondrian = com.travelingtunes.app.core.model.ConfigOption.findByKey("THEME_MONDRIAN")
        org.junit.Assert.assertNotNull(mondrian)
        assertEquals("Theme: Mondrian", mondrian?.title)
        assertEquals("Mondrian", mondrian?.targetValue)
        assertEquals(false, mondrian?.isBooleanToggle)

        val showArt = com.travelingtunes.app.core.model.ConfigOption.findByKey("DISPLAY_showAlbumArt")
        org.junit.Assert.assertNotNull(showArt)
        assertEquals("Show Album Art", showArt?.title)
        assertEquals(true, showArt?.isBooleanToggle)
    }

    @Test
    fun testMondrianThemeAlwaysHasBlackText() {
        val mondrianTheme = ThemeSettings(currentThemeName = "Mondrian")
        val dynamicTheme = com.travelingtunes.app.core.model.ColorTheme("Dynamic", androidx.compose.ui.graphics.Color.Red, androidx.compose.ui.graphics.Color.Yellow)

        val resolved = com.travelingtunes.app.core.theme.resolveActiveTheme(
            themeSettings = mondrianTheme,
            dynamicAlbumArtTheme = dynamicTheme,
            useAlbumArtColors = true
        )

        assertEquals("Mondrian", resolved.name)
        assertEquals(androidx.compose.ui.graphics.Color.White, resolved.backgroundColor)
        assertEquals(androidx.compose.ui.graphics.Color.Black, resolved.textColor)
        assertEquals(androidx.compose.ui.graphics.Color.Black, resolved.secondaryTextColor)
        assertEquals(androidx.compose.ui.graphics.Color.Black, resolved.artistColor)
        assertEquals(androidx.compose.ui.graphics.Color.Black, resolved.albumColor)
    }

    @Test
    fun testDockedScreenLayoutAndTouchRegionBoundsToggle() {
        val dockedDisplay = com.travelingtunes.app.core.model.DisplaySettings(
            artDisplayLayout = com.travelingtunes.app.core.model.ArtLayoutOption.DOCKED,
            showAlbumArt = true
        )
        val matchedTheme = ThemeSettings(currentThemeName = "Match Album Art")
        val mondrianTheme = ThemeSettings(currentThemeName = "Mondrian")

        val isDockedMatched = dockedDisplay.artDisplayLayout == com.travelingtunes.app.core.model.ArtLayoutOption.DOCKED &&
                              dockedDisplay.showAlbumArt &&
                              !matchedTheme.currentThemeName.equals("Mondrian", ignoreCase = true)
        assertTrue("In Matched theme with Docked layout, isDockedScreen should be true", isDockedMatched)

        val isDockedMondrian = dockedDisplay.artDisplayLayout == com.travelingtunes.app.core.model.ArtLayoutOption.DOCKED &&
                               dockedDisplay.showAlbumArt &&
                               !mondrianTheme.currentThemeName.equals("Mondrian", ignoreCase = true)
        org.junit.Assert.assertFalse("In Mondrian theme, isDockedScreen should be false as artwork is hidden", isDockedMondrian)
    }

    @Test
    fun testAutoCategoryDefaultsAndReordering() {
        val defaultDisplay = com.travelingtunes.app.core.model.DisplaySettings()
        assertEquals(5, defaultDisplay.autoCategoryOrder.size)
        assertEquals(com.travelingtunes.app.core.model.AutoCategory.SONGS, defaultDisplay.autoCategoryOrder[0])
        assertEquals(com.travelingtunes.app.core.model.AutoCategory.ALBUMS, defaultDisplay.autoCategoryOrder[1])
        assertEquals(com.travelingtunes.app.core.model.AutoCategory.ARTISTS, defaultDisplay.autoCategoryOrder[2])
        assertEquals(com.travelingtunes.app.core.model.AutoCategory.GENRES, defaultDisplay.autoCategoryOrder[3])
        assertEquals(com.travelingtunes.app.core.model.AutoCategory.FOLDERS, defaultDisplay.autoCategoryOrder[4])

        assertTrue(defaultDisplay.autoShowAlbumArt)
        assertTrue(defaultDisplay.autoAlbumStyleGrid)
        org.junit.Assert.assertFalse(defaultDisplay.autoArtistStyleGrid)
        org.junit.Assert.assertFalse(defaultDisplay.autoAutoplayOnConnect)
        assertTrue(defaultDisplay.autoVoiceSearch)

        val reordered = listOf(
            com.travelingtunes.app.core.model.AutoCategory.ARTISTS,
            com.travelingtunes.app.core.model.AutoCategory.ALBUMS,
            com.travelingtunes.app.core.model.AutoCategory.SONGS
        )
        val updatedDisplay = defaultDisplay.copy(
            autoCategoryOrder = reordered,
            autoShowAlbumArt = false,
            autoArtistStyleGrid = true
        )

        assertEquals(3, updatedDisplay.autoCategoryOrder.size)
        assertEquals(com.travelingtunes.app.core.model.AutoCategory.ARTISTS, updatedDisplay.autoCategoryOrder[0])
        org.junit.Assert.assertFalse(updatedDisplay.autoShowAlbumArt)
        assertTrue(updatedDisplay.autoArtistStyleGrid)
    }

    @Test
    fun testSettingsBackupWithAndroidAutoPreferences() {
        val display = com.travelingtunes.app.core.model.DisplaySettings(
            autoCategoryOrder = listOf(
                com.travelingtunes.app.core.model.AutoCategory.FOLDERS,
                com.travelingtunes.app.core.model.AutoCategory.SONGS
            ),
            autoShowAlbumArt = false,
            autoVoiceSearch = true
        )
        val theme = ThemeSettings()
        val jsonStr = com.travelingtunes.app.core.datastore.SettingsBackupHelper.exportToJson(
            display = display,
            theme = theme,
            bindings = emptyMap(),
            gpsVolume = false,
            gpsSens = 0.5f,
            autoRescan = true
        )

        assertTrue(jsonStr.contains("\"autoCategoryOrder\": \"FOLDERS,SONGS\""))
        assertTrue(jsonStr.contains("\"autoShowAlbumArt\": false"))
        assertTrue(jsonStr.contains("\"autoVoiceSearch\": true"))
    }

    @Test
    fun testAutoActionButtonOrderDefaultsAndBackup() {
        val defaultDisplay = com.travelingtunes.app.core.model.DisplaySettings()
        assertTrue(defaultDisplay.autoActionButtonOrder.contains(com.travelingtunes.app.core.model.GestureAction.PLAY_CURRENT_ALBUM))
        assertTrue(defaultDisplay.autoActionButtonOrder.contains(com.travelingtunes.app.core.model.GestureAction.PLAY_CURRENT_ARTIST))
        assertEquals(com.travelingtunes.app.core.model.GestureAction.PLAY_CURRENT_ALBUM, defaultDisplay.autoActionButtonOrder[0])
        assertEquals(com.travelingtunes.app.core.model.GestureAction.PLAY_CURRENT_ARTIST, defaultDisplay.autoActionButtonOrder[1])

        val customButtons = listOf(
            com.travelingtunes.app.core.model.GestureAction.PLAY_CURRENT_ARTIST,
            com.travelingtunes.app.core.model.GestureAction.PLAY_CURRENT_ALBUM
        )
        val updated = defaultDisplay.copy(autoActionButtonOrder = customButtons)
        assertEquals(2, updated.autoActionButtonOrder.size)
        assertEquals(com.travelingtunes.app.core.model.GestureAction.PLAY_CURRENT_ARTIST, updated.autoActionButtonOrder[0])

        val jsonStr = com.travelingtunes.app.core.datastore.SettingsBackupHelper.exportToJson(
            display = updated,
            theme = ThemeSettings(),
            bindings = emptyMap(),
            gpsVolume = false,
            gpsSens = 0.5f,
            autoRescan = true
        )
        assertTrue(jsonStr.contains("\"autoActionButtonOrder\": \"PLAY_CURRENT_ARTIST,PLAY_CURRENT_ALBUM\""))
    }

    @Test
    fun testDrivingModeAndSpeedVolumeDefaults() {
        val display = com.travelingtunes.app.core.model.DisplaySettings()
        org.junit.Assert.assertFalse(display.drivingModeEnabled)
        org.junit.Assert.assertFalse(display.autoEnableDrivingMode)
        org.junit.Assert.assertFalse(display.autoAmbientNoiseEnabled)
        assertEquals(50, display.autoDefaultVolume)
        assertEquals(15f, display.autoMinSpeedThreshold, 0.01f)
        assertEquals(1.0f, display.autoSpeedVolumeRatio, 0.01f)
        assertEquals("MPH", display.autoSpeedUnit)

        val updated = display.copy(
            drivingModeEnabled = true,
            autoEnableDrivingMode = true,
            autoSpeedVolumeEnabled = true,
            autoAmbientNoiseEnabled = true,
            autoDefaultVolume = 70,
            autoMinSpeedThreshold = 20f,
            autoSpeedVolumeRatio = 1.5f,
            autoSpeedUnit = "KPH"
        )
        assertTrue(updated.drivingModeEnabled)
        assertTrue(updated.autoEnableDrivingMode)
        assertTrue(updated.autoSpeedVolumeEnabled)
        assertTrue(updated.autoAmbientNoiseEnabled)
        assertEquals(70, updated.autoDefaultVolume)
        assertEquals(20f, updated.autoMinSpeedThreshold, 0.01f)
        assertEquals(1.5f, updated.autoSpeedVolumeRatio, 0.01f)
        assertEquals("KPH", updated.autoSpeedUnit)
    }

    @Test
    fun testToggleDrivingModeAction() {
        val action = GestureAction.fromKey("TOGGLE_DRIVING_MODE")
        assertEquals(GestureAction.TOGGLE_DRIVING_MODE, action)
        assertEquals("Toggle Traveling Mode", GestureAction.TOGGLE_DRIVING_MODE.displayName)
    }

    @Test
    fun testAmbientNoiseManagerBehavior() {
        val mockContext = Mockito.mock(Context::class.java)
        val mockAudioManager = Mockito.mock(AudioManager::class.java)
        Mockito.`when`(mockContext.applicationContext).thenReturn(mockContext)
        Mockito.`when`(mockContext.getSystemService(Context.AUDIO_SERVICE)).thenReturn(mockAudioManager)
        Mockito.`when`(mockAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).thenReturn(15)
        Mockito.`when`(mockAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)).thenReturn(7)

        val manager = com.travelingtunes.app.core.location.AmbientNoiseManager(mockContext)
        manager.updateConfig(
            ambientNoiseEnabled = true,
            drivingModeEnabled = false,
            autoEnableDrivingMode = false,
            defaultVolumePercent = 50
        )

        assertFalse(manager.isAmbientNoiseActive)

        manager.updateConfig(
            ambientNoiseEnabled = true,
            drivingModeEnabled = true,
            autoEnableDrivingMode = false,
            defaultVolumePercent = 50
        )

        assertTrue(manager.isAmbientNoiseActive)
    }

    @Test
    fun testSpeedVolumeAdjustmentDisabledWhenDrivingModeOff() {
        val mockContext = Mockito.mock(Context::class.java)
        val mockAudioManager = Mockito.mock(AudioManager::class.java)
        Mockito.`when`(mockContext.applicationContext).thenReturn(mockContext)
        Mockito.`when`(mockContext.getSystemService(Context.AUDIO_SERVICE)).thenReturn(mockAudioManager)
        Mockito.`when`(mockAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).thenReturn(15)
        Mockito.`when`(mockAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)).thenReturn(7)

        val manager = SpeedVolumeManager(mockContext)
        manager.updateConfig(
            speedVolumeEnabled = true,
            defaultVolumePercent = 50,
            minSpeedThreshold = 15f,
            speedVolumeRatio = 1.0f,
            speedUnit = "MPH",
            drivingModeEnabled = false,
            autoEnableDrivingMode = false
        )

        assertFalse(manager.isSpeedVolumeActive)

        var defaultVolChanged = false
        manager.onDefaultVolumeChanged = { defaultVolChanged = true }

        manager.onManualVolumeChanged(10)
        assertEquals(50, manager.defaultVolumePercent)
        assertFalse(defaultVolChanged)
    }

    @Test
    fun testSpeedVolumeAdjustmentManualChangeProportionalUpdate() {
        val mockContext = Mockito.mock(Context::class.java)
        val mockAudioManager = Mockito.mock(AudioManager::class.java)
        Mockito.`when`(mockContext.applicationContext).thenReturn(mockContext)
        Mockito.`when`(mockContext.getSystemService(Context.AUDIO_SERVICE)).thenReturn(mockAudioManager)
        Mockito.`when`(mockAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).thenReturn(15)
        Mockito.`when`(mockAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)).thenReturn(7)

        val manager = SpeedVolumeManager(mockContext)
        var updatedDefaultVol = -1
        manager.updateConfig(
            speedVolumeEnabled = true,
            defaultVolumePercent = 50,
            minSpeedThreshold = 15f,
            speedVolumeRatio = 1.0f,
            speedUnit = "MPH",
            drivingModeEnabled = true,
            autoEnableDrivingMode = false,
            onDefaultVolumeChanged = { updatedDefaultVol = it }
        )

        assertTrue(manager.isSpeedVolumeActive)

        // Simulate driving at 35 MPH (20 MPH above threshold of 15 MPH)
        // excessSpeed = 20 -> boostIndex = (20 / 10) * 1.0 = 2
        manager.adjustVolumeForSpeed(35f)

        // System volume index set by speed boost was 7 + 2 = 9
        // User manually changes volume to 11 (bumping up volume by 2 steps)
        manager.onManualVolumeChanged(11)

        // newBaseVolIndex = 11 - 2 = 9 out of 15 max -> (9 / 15) * 100 = 60%
        assertEquals(60, manager.defaultVolumePercent)
        assertEquals(60, updatedDefaultVol)
    }

    @Test
    fun testArtColorPriorityEnumAndDisplaySettingsDefaults() {
        val display = com.travelingtunes.app.core.model.DisplaySettings()
        assertFalse(display.stretchArt)
        assertFalse(display.adaptiveDockedArt)
        assertEquals(com.travelingtunes.app.core.model.ArtColorPriority.CENTER, display.matchArtColorPriority)

        assertEquals("Center", com.travelingtunes.app.core.model.ArtColorPriority.CENTER.displayName)
        assertEquals("Outer Edge", com.travelingtunes.app.core.model.ArtColorPriority.OUTER_EDGE.displayName)
        assertEquals("Whole Edge", com.travelingtunes.app.core.model.ArtColorPriority.WHOLE.displayName)

        assertEquals(com.travelingtunes.app.core.model.ArtColorPriority.CENTER, com.travelingtunes.app.core.model.ArtColorPriority.fromOrdinal(0))
        assertEquals(com.travelingtunes.app.core.model.ArtColorPriority.OUTER_EDGE, com.travelingtunes.app.core.model.ArtColorPriority.fromOrdinal(1))
        assertEquals(com.travelingtunes.app.core.model.ArtColorPriority.WHOLE, com.travelingtunes.app.core.model.ArtColorPriority.fromOrdinal(2))
    }

    @Test
    fun testToggleDockedArtAction() {
        val action = GestureAction.fromKey("TOGGLE_DOCKED_ART")
        assertEquals(GestureAction.TOGGLE_DOCKED_ART, action)
        assertEquals("Toggle Docked Art", GestureAction.TOGGLE_DOCKED_ART.displayName)
    }

    @Test
    fun testSettingsBackupAndRestoreWithNewDockedAndEdgeOptions() {
        val display = com.travelingtunes.app.core.model.DisplaySettings(
            stretchArt = true,
            adaptiveDockedArt = true,
            matchArtColorPriority = com.travelingtunes.app.core.model.ArtColorPriority.WHOLE
        )
        val json = SettingsBackupHelper.exportToJson(
            display = display,
            theme = ThemeSettings(),
            bindings = emptyMap<GestureTrigger, GestureBinding>(),
            gpsVolume = false,
            gpsSens = 1.0f,
            autoRescan = false
        )

        assertTrue(json.contains("\"stretchArt\": true"))
        assertTrue(json.contains("\"adaptiveDockedArt\": true"))
        assertTrue(json.contains("\"matchArtColorPriority\": \"WHOLE\""))
    }

    @Test
    fun testAlbumArtColorExtractorPriorityModes() {
        kotlinx.coroutines.runBlocking {
            val mockBitmap = Mockito.mock(android.graphics.Bitmap::class.java)
            Mockito.`when`(mockBitmap.width).thenReturn(100)
            Mockito.`when`(mockBitmap.height).thenReturn(100)
            Mockito.`when`(mockBitmap.getPixel(Mockito.anyInt(), Mockito.anyInt())).thenReturn(android.graphics.Color.BLUE)

            val themeCenter = com.travelingtunes.app.core.theme.AlbumArtColorExtractor.extractThemeFromBitmap(
                bitmap = mockBitmap,
                innerEdge = com.travelingtunes.app.core.theme.InnerEdge.RIGHT,
                priority = com.travelingtunes.app.core.model.ArtColorPriority.CENTER
            )
            assertNotNull(themeCenter)

            val themeOuter = com.travelingtunes.app.core.theme.AlbumArtColorExtractor.extractThemeFromBitmap(
                bitmap = mockBitmap,
                innerEdge = com.travelingtunes.app.core.theme.InnerEdge.RIGHT,
                priority = com.travelingtunes.app.core.model.ArtColorPriority.OUTER_EDGE
            )
            assertNotNull(themeOuter)

            val themeWhole = com.travelingtunes.app.core.theme.AlbumArtColorExtractor.extractThemeFromBitmap(
                bitmap = mockBitmap,
                innerEdge = com.travelingtunes.app.core.theme.InnerEdge.RIGHT,
                priority = com.travelingtunes.app.core.model.ArtColorPriority.WHOLE
            )
            assertNotNull(themeWhole)
        }
    }

    @Test
    fun testAlbumArtColorExtractorPrefersBlackEdgeBackgroundOverLighterColor() {
        kotlinx.coroutines.runBlocking {
            val blackColor = 0xFF000000.toInt()
            val whiteColor = 0xFFFFFFFF.toInt()
            val mockBitmap = Mockito.mock(android.graphics.Bitmap::class.java)
            Mockito.`when`(mockBitmap.width).thenReturn(100)
            Mockito.`when`(mockBitmap.height).thenReturn(100)
            Mockito.`when`(mockBitmap.getPixel(Mockito.anyInt(), Mockito.anyInt())).thenAnswer { invocation ->
                val x = invocation.getArgument<Int>(0)
                val y = invocation.getArgument<Int>(1)
                if (x == 0 && y == 0) whiteColor else blackColor
            }

            val theme = com.travelingtunes.app.core.theme.AlbumArtColorExtractor.extractThemeFromBitmap(
                bitmap = mockBitmap,
                priority = com.travelingtunes.app.core.model.ArtColorPriority.WHOLE
            )
            assertEquals(androidx.compose.ui.graphics.Color(blackColor), theme.backgroundColor)
        }
    }

    @Test
    fun testAlbumArtColorExtractorPrefersRedEdgeBackgroundOverLighterColor() {
        kotlinx.coroutines.runBlocking {
            val darkRed = 0xFF800000.toInt()
            val lightPink = 0xFFFFE6E6.toInt()
            val mockBitmap = Mockito.mock(android.graphics.Bitmap::class.java)
            Mockito.`when`(mockBitmap.width).thenReturn(100)
            Mockito.`when`(mockBitmap.height).thenReturn(100)
            Mockito.`when`(mockBitmap.getPixel(Mockito.anyInt(), Mockito.anyInt())).thenAnswer { invocation ->
                val x = invocation.getArgument<Int>(0)
                val y = invocation.getArgument<Int>(1)
                if (x == 0 && y == 0) lightPink else darkRed
            }

            val theme = com.travelingtunes.app.core.theme.AlbumArtColorExtractor.extractThemeFromBitmap(
                bitmap = mockBitmap,
                priority = com.travelingtunes.app.core.model.ArtColorPriority.WHOLE
            )
            assertEquals(androidx.compose.ui.graphics.Color(darkRed), theme.backgroundColor)
        }
    }

    @Test
    fun testAlbumArtColorCacheStorageAndRetrieval() {
        val cache = com.travelingtunes.app.core.theme.AlbumArtColorCache.instance
        cache.clear()

        val sampleTheme = com.travelingtunes.app.core.model.ColorTheme(
            name = "Test Dynamic Theme",
            backgroundColor = androidx.compose.ui.graphics.Color.Red,
            textColor = androidx.compose.ui.graphics.Color.White,
            secondaryTextColor = androidx.compose.ui.graphics.Color.LightGray
        )

        val key = com.travelingtunes.app.core.theme.AlbumArtColorCache.makeKey(
            songId = 123L,
            innerEdge = com.travelingtunes.app.core.theme.InnerEdge.LEFT,
            priority = com.travelingtunes.app.core.model.ArtColorPriority.CENTER
        )

        org.junit.Assert.assertNull(cache.get(key))

        cache.put(key, sampleTheme)

        val retrieved = cache.get(key)
        assertNotNull(retrieved)
        assertEquals(androidx.compose.ui.graphics.Color.Red, retrieved!!.backgroundColor)
        assertEquals(sampleTheme, cache.get(123L, com.travelingtunes.app.core.theme.InnerEdge.LEFT, com.travelingtunes.app.core.model.ArtColorPriority.CENTER))
    }

    @Test
    fun testAllConfigOptionsAreRegistered() {
        val allOptions = com.travelingtunes.app.core.model.ConfigOption.ALL_OPTIONS
        assertTrue("Every ConfigOption must have a non-empty key and title", allOptions.all { it.key.isNotEmpty() && it.title.isNotEmpty() })

        val mondrian = com.travelingtunes.app.core.model.ConfigOption.findByKey("THEME_MONDRIAN")
        assertNotNull(mondrian)
        assertEquals("Mondrian", mondrian?.targetValue)
        assertFalse(mondrian!!.isBooleanToggle)
    }

    @Test
    fun testThemeToggleLogicAndMatchColorsMode() {
        val initialTheme = ThemeSettings(currentThemeName = "Match Album Art")
        val displaySettings = com.travelingtunes.app.core.model.DisplaySettings(albumArtColors = true)

        // 1. Initial state: Match Colors mode is active
        val resolvedInitial = com.travelingtunes.app.core.theme.resolveActiveTheme(
            themeSettings = initialTheme,
            dynamicAlbumArtTheme = com.travelingtunes.app.core.model.ColorTheme("Dynamic", androidx.compose.ui.graphics.Color.Red, androidx.compose.ui.graphics.Color.White),
            useAlbumArtColors = displaySettings.albumArtColors
        )
        assertEquals("Dynamic", resolvedInitial.name)

        // 2. Toggle to Mondrian theme
        val mondrianTheme = ThemeSettings(currentThemeName = "Mondrian")
        val resolvedMondrian = com.travelingtunes.app.core.theme.resolveActiveTheme(
            themeSettings = mondrianTheme,
            dynamicAlbumArtTheme = null,
            useAlbumArtColors = false
        )
        assertEquals("Mondrian", resolvedMondrian.name)

        // 3. Toggle back to Match Colors mode
        val resolvedRestored = com.travelingtunes.app.core.theme.resolveActiveTheme(
            themeSettings = initialTheme,
            dynamicAlbumArtTheme = com.travelingtunes.app.core.model.ColorTheme("Dynamic", androidx.compose.ui.graphics.Color.Red, androidx.compose.ui.graphics.Color.White),
            useAlbumArtColors = displaySettings.albumArtColors
        )
        assertEquals("Dynamic", resolvedRestored.name)
    }

    @Test
    fun testProfileDataModelAndSerialization() {
        // Built-in Profiles
        val defaultProfile = Profile.DEFAULT
        assertEquals("default", defaultProfile.id)
        assertEquals("Default", defaultProfile.name)
        assertTrue(defaultProfile.isBuiltIn)
        assertFalse(defaultProfile.isDeletable)
        assertTrue(defaultProfile.overrides.isEmpty())

        val travelingProfile = Profile.TRAVELING
        assertEquals("traveling", travelingProfile.id)
        assertEquals("Traveling", travelingProfile.name)
        assertTrue(travelingProfile.isBuiltIn)
        assertFalse(travelingProfile.isDeletable)
        assertTrue(travelingProfile.overrides.containsKey("autoEnableDrivingMode"))
        assertTrue(travelingProfile.overrides.containsKey("gpsVolume"))

        val drivingProfile = Profile.DRIVING
        assertEquals("driving", drivingProfile.id)
        assertEquals("Driving", drivingProfile.name)
        assertEquals("traveling", drivingProfile.parentId)
        assertTrue(drivingProfile.isBuiltIn)
        assertFalse(drivingProfile.isDeletable)
        assertEquals("true", drivingProfile.overrides["autoSpeedVolumeEnabled"])

        val transitProfile = Profile.TRANSIT
        assertEquals("transit", transitProfile.id)
        assertEquals("Transit", transitProfile.name)
        assertEquals("traveling", transitProfile.parentId)
        assertTrue(transitProfile.isBuiltIn)
        assertFalse(transitProfile.isDeletable)
        assertEquals("true", transitProfile.overrides["autoAmbientNoiseEnabled"])

        val dockedProfile = Profile.DOCKED
        assertEquals("docked", dockedProfile.id)
        assertEquals("Docked Art", dockedProfile.name)
        assertTrue(dockedProfile.isBuiltIn)
        assertFalse(dockedProfile.isDeletable)
        assertEquals("DOCKED", dockedProfile.overrides["artDisplayLayout"])

        val undockedProfile = Profile.UNDOCKED
        assertEquals("undocked", undockedProfile.id)
        assertEquals("Undocked Art", undockedProfile.name)
        assertTrue(undockedProfile.isBuiltIn)
        assertFalse(undockedProfile.isDeletable)
        assertEquals("OVERLAY", undockedProfile.overrides["artDisplayLayout"])

        // Custom Profile
        val customProfile = Profile(
            id = "custom_1",
            name = "Night Drive",
            isBuiltIn = false,
            isDeletable = true,
            overrides = mapOf("artistFontSize" to "40.0")
        )
        val jsonStr = Profile.listToJson(listOf(defaultProfile, travelingProfile, drivingProfile, transitProfile, dockedProfile, undockedProfile, customProfile))
        val parsedList = Profile.listFromJson(jsonStr)

        assertEquals(7, parsedList.size)
        val parsedCustom = parsedList.find { it.id == "custom_1" }
        assertNotNull(parsedCustom)
        assertEquals("Night Drive", parsedCustom?.name)
        assertFalse(parsedCustom!!.isBuiltIn)
        assertTrue(parsedCustom.isDeletable)
        assertEquals("40.0", parsedCustom.overrides["artistFontSize"])

        val parsedDocked = parsedList.find { it.id == "docked" }
        assertNotNull(parsedDocked)
        assertTrue(parsedDocked!!.isBuiltIn)
        assertFalse(parsedDocked.isDeletable)
        assertEquals("DOCKED", parsedDocked.overrides["artDisplayLayout"])

        val parsedUndocked = parsedList.find { it.id == "undocked" }
        assertNotNull(parsedUndocked)
        assertTrue(parsedUndocked!!.isBuiltIn)
        assertFalse(parsedUndocked.isDeletable)
        assertEquals("OVERLAY", parsedUndocked.overrides["artDisplayLayout"])
    }

    @Test
    fun testBuiltInInheritancesEnforcedAndNonEditable() {
        assertEquals(null, Profile.DEFAULT.parentId)
        assertEquals("default", Profile.TRAVELING.parentId)
        assertEquals("traveling", Profile.DRIVING.parentId)
        assertEquals("traveling", Profile.TRANSIT.parentId)
        assertEquals("default", Profile.DOCKED.parentId)
        assertEquals("default", Profile.UNDOCKED.parentId)

        val tamperedJson = """[{"id":"driving","name":"Driving","isBuiltIn":true,"parentId":"default"}]"""
        val parsed = Profile.listFromJson(tamperedJson)
        val driving = parsed.find { it.id == "driving" }
        assertEquals("traveling", driving?.parentId)
    }

    @Test
    fun testProfileEmojiDefaultAndSerialization() {
        assertEquals("🏷️", Profile.DEFAULT.emoji)
        assertEquals("🧳", Profile.TRAVELING.emoji)
        assertEquals("🚗", Profile.DRIVING.emoji)
        assertEquals("🚆", Profile.TRANSIT.emoji)

        val customProfile = Profile(
            id = "custom_2",
            name = "Gym",
            emoji = "💪"
        )
        assertEquals("💪", customProfile.emoji)

        val jsonStr = customProfile.toJson()
        assertTrue(jsonStr.contains("\"emoji\": \"💪\""))

        val parsed = Profile.listFromJson(Profile.listToJson(listOf(customProfile))).first { it.id == "custom_2" }
        assertEquals("💪", parsed.emoji)
    }

    @Test
    fun testSelectProfileGestureActionAndSelectionMode() {
        val selectProfileAction = GestureAction.fromKey("SelectProfile")
        assertEquals(GestureAction.SELECT_PROFILE, selectProfileAction)
        assertEquals("Select Profile", GestureAction.SELECT_PROFILE.displayName)

        val menuMode = ProfileSelectionMode.fromKey("MENU")
        assertEquals(ProfileSelectionMode.MENU, menuMode)

        val seqMode = ProfileSelectionMode.fromKey("SEQUENTIAL")
        assertEquals(ProfileSelectionMode.SEQUENTIAL, seqMode)
    }

    @Test
    fun testGestureBindingProfileOverrideResolution() {
        val trigger = com.travelingtunes.app.core.model.GestureTrigger.TAP_1_1
        val customProfile = Profile(
            id = "custom_gestures",
            name = "Gesture Custom",
            overrides = mapOf(trigger.key to "FastForward")
        )
        val stack = listOf(customProfile, Profile.DEFAULT)
        val override = Profile.resolveEffectiveOverride(trigger.key, stack, listOf(Profile.DEFAULT, customProfile))
        assertEquals("FastForward", override)
    }

    @Test
    fun testBackupHelperWithProfiles() {
        val display = com.travelingtunes.app.core.model.DisplaySettings()
        val theme = ThemeSettings()
        val json = SettingsBackupHelper.exportToJson(
            display = display,
            theme = theme,
            bindings = emptyMap(),
            gpsVolume = true,
            gpsSens = 1.0f,
            autoRescan = false,
            profiles = listOf(Profile.DEFAULT, Profile.TRAVELING),
            activeProfileId = Profile.TRAVELING_ID,
            rawPreferences = mapOf("customKey" to "customValue")
        )
        assertTrue(json.contains("\"activeProfileId\": \"traveling\""))
        assertTrue(json.contains("\"profiles\":"))
        assertTrue(json.contains("\"rawPreferences\":"))
    }

    @Test
    fun testProfileCopyAndInheritanceLinking() {
        val parent = Profile(
            id = "parent_1",
            name = "Commute",
            overrides = mapOf("gpsVolume" to "true", "artistFontSize" to "45.0")
        )
        val child = Profile(
            id = "child_1",
            name = "Commute Night",
            parentId = "parent_1",
            overrides = mapOf("artistFontSize" to "35.0")
        )
        val profiles = listOf(Profile.DEFAULT, Profile.TRAVELING, parent, child)

        assertEquals("35.0", child.getEffectiveOverride("artistFontSize", profiles))
        assertEquals("true", child.getEffectiveOverride("gpsVolume", profiles))
        assertEquals(null, child.getEffectiveOverride("titleShrinkLong", profiles))

        val ancestors = child.getAncestorChain(profiles)
        assertEquals(3, ancestors.size)
        assertEquals("child_1", ancestors[0].id)
        assertEquals("parent_1", ancestors[1].id)
        assertEquals("default", ancestors[2].id)
    }

    @Test
    fun testAutomaticDockAndUndockedProfileToggle() {
        val dockedProfile = Profile.DOCKED
        val undockedProfile = Profile.UNDOCKED

        assertEquals("docked", dockedProfile.id)
        assertFalse(dockedProfile.isDeletable)
        assertTrue(dockedProfile.isBuiltIn)

        assertEquals("undocked", undockedProfile.id)
        assertFalse(undockedProfile.isDeletable)
        assertTrue(undockedProfile.isBuiltIn)

        val profiles = listOf(Profile.DEFAULT, Profile.TRAVELING, Profile.DOCKED, Profile.UNDOCKED)
        assertEquals("DOCKED", dockedProfile.getEffectiveOverride("artDisplayLayout", profiles))
        assertEquals("OVERLAY", undockedProfile.getEffectiveOverride("artDisplayLayout", profiles))
    }

    @Test
    fun testProfileSettingChangesDoNotPropagateToOtherProfiles() {
        val defaultProfile = Profile.DEFAULT
        val dockedProfile = Profile.DOCKED
        var undockedProfile = Profile.UNDOCKED

        val initialProfiles = listOf(defaultProfile, Profile.TRAVELING, dockedProfile, undockedProfile)

        // Docked and Undocked inherit from Default, and override artDisplayLayout
        assertEquals("DOCKED", dockedProfile.getEffectiveOverride("artDisplayLayout", initialProfiles))
        assertEquals("OVERLAY", undockedProfile.getEffectiveOverride("artDisplayLayout", initialProfiles))

        // Undocked initially inherits Default font size (null local override)
        assertEquals(null, undockedProfile.overrides["artistFontSize"])
        assertEquals(null, dockedProfile.overrides["artistFontSize"])

        // Simulate updating a setting while Undocked profile is active
        val updatedUndockedMap = undockedProfile.overrides.toMutableMap().apply {
            put("artistFontSize", "65.0")
            put("DISPLAY_artistFontSize", "65.0")
            put("stretchArt", "true")
            put("DISPLAY_stretchArt", "true")
        }
        undockedProfile = undockedProfile.copy(overrides = updatedUndockedMap)

        val updatedProfiles = listOf(defaultProfile, Profile.TRAVELING, dockedProfile, undockedProfile)

        // Undocked profile now has local overrides
        assertEquals("65.0", undockedProfile.getEffectiveOverride("artistFontSize", updatedProfiles))
        assertEquals("true", undockedProfile.getEffectiveOverride("stretchArt", updatedProfiles))

        // Docked profile and Default profile remain untouched
        assertEquals(null, dockedProfile.overrides["artistFontSize"])
        assertEquals(null, dockedProfile.getEffectiveOverride("artistFontSize", updatedProfiles))
        assertEquals(null, dockedProfile.getEffectiveOverride("stretchArt", updatedProfiles))

        // Docked profile still has its own layout override ("DOCKED") and didn't get affected by Undocked's changes
        assertEquals("DOCKED", dockedProfile.getEffectiveOverride("artDisplayLayout", updatedProfiles))
        assertEquals("OVERLAY", undockedProfile.getEffectiveOverride("artDisplayLayout", updatedProfiles))
    }

    @Test
    fun testKeyboardTriggersAndDefaults() {
        assertEquals(com.travelingtunes.app.core.model.GestureCategory.KEYBOARD, GestureTrigger.KEY_SPACE.category)
        assertEquals("PlayPause", GestureTrigger.KEY_SPACE.defaultActionKey)

        assertEquals(com.travelingtunes.app.core.model.GestureCategory.KEYBOARD, GestureTrigger.KEY_F.category)
        assertEquals("ToggleDockedArt", GestureTrigger.KEY_F.defaultActionKey)

        assertEquals("Rewind", GestureTrigger.KEY_LEFT.defaultActionKey)
        assertEquals("FastForward", GestureTrigger.KEY_RIGHT.defaultActionKey)
        assertEquals("VolumeUp", GestureTrigger.KEY_UP.defaultActionKey)
        assertEquals("VolumeDown", GestureTrigger.KEY_DOWN.defaultActionKey)
        assertEquals("Menu", GestureTrigger.KEY_ESC.defaultActionKey)
        assertEquals("SongPicker", GestureTrigger.KEY_TAB.defaultActionKey)
        assertEquals("ShowQueue", GestureTrigger.KEY_Q.defaultActionKey)
        assertEquals("ShowQuickStart", GestureTrigger.KEY_QUESTION.defaultActionKey)

        assertEquals("ToggleRepeat", GestureTrigger.KEY_F1.defaultActionKey)
        assertEquals("Rewind", GestureTrigger.KEY_F2.defaultActionKey)
        assertEquals("PlayCurrentArtist", GestureTrigger.KEY_F3.defaultActionKey)
        assertEquals("PlayCurrentAlbum", GestureTrigger.KEY_F4.defaultActionKey)
        assertEquals("ShuffleAllSongs", GestureTrigger.KEY_F5.defaultActionKey)
        assertEquals("FastForward", GestureTrigger.KEY_F6.defaultActionKey)
        assertEquals("SongPicker", GestureTrigger.KEY_F7.defaultActionKey)
        assertEquals("Previous", GestureTrigger.KEY_F8.defaultActionKey)
        assertEquals("ShowQueue", GestureTrigger.KEY_F9.defaultActionKey)
        assertEquals("PlayPause", GestureTrigger.KEY_F10.defaultActionKey)
        assertEquals("ShowQuickStart", GestureTrigger.KEY_F11.defaultActionKey)
        assertEquals("Next", GestureTrigger.KEY_F12.defaultActionKey)

        assertEquals("PlayPause", GestureTrigger.KEY_MEDIA_PLAY_PAUSE.defaultActionKey)
        assertEquals("Next", GestureTrigger.KEY_MEDIA_NEXT.defaultActionKey)
        assertEquals("Previous", GestureTrigger.KEY_MEDIA_PREVIOUS.defaultActionKey)
        assertEquals("FastForward", GestureTrigger.KEY_MEDIA_FAST_FORWARD.defaultActionKey)
        assertEquals("Rewind", GestureTrigger.KEY_MEDIA_REWIND.defaultActionKey)
        assertEquals("Pause", GestureTrigger.KEY_MEDIA_STOP.defaultActionKey)
    }

    @Test
    fun testKeyCodeToKeyboardTriggerMapping() {
        assertEquals(GestureTrigger.KEY_SPACE, com.travelingtunes.app.feature.player.keyCodeToKeyboardTrigger(android.view.KeyEvent.KEYCODE_SPACE))
        assertEquals(GestureTrigger.KEY_F, com.travelingtunes.app.feature.player.keyCodeToKeyboardTrigger(android.view.KeyEvent.KEYCODE_F))
        assertEquals(GestureTrigger.KEY_LEFT, com.travelingtunes.app.feature.player.keyCodeToKeyboardTrigger(android.view.KeyEvent.KEYCODE_DPAD_LEFT))
        assertEquals(GestureTrigger.KEY_RIGHT, com.travelingtunes.app.feature.player.keyCodeToKeyboardTrigger(android.view.KeyEvent.KEYCODE_DPAD_RIGHT))
        assertEquals(GestureTrigger.KEY_UP, com.travelingtunes.app.feature.player.keyCodeToKeyboardTrigger(android.view.KeyEvent.KEYCODE_DPAD_UP))
        assertEquals(GestureTrigger.KEY_DOWN, com.travelingtunes.app.feature.player.keyCodeToKeyboardTrigger(android.view.KeyEvent.KEYCODE_DPAD_DOWN))
        assertEquals(GestureTrigger.KEY_ESC, com.travelingtunes.app.feature.player.keyCodeToKeyboardTrigger(android.view.KeyEvent.KEYCODE_ESCAPE))
        assertEquals(GestureTrigger.KEY_TAB, com.travelingtunes.app.feature.player.keyCodeToKeyboardTrigger(android.view.KeyEvent.KEYCODE_TAB))
        assertEquals(GestureTrigger.KEY_Q, com.travelingtunes.app.feature.player.keyCodeToKeyboardTrigger(android.view.KeyEvent.KEYCODE_Q))
        assertEquals(GestureTrigger.KEY_QUESTION, com.travelingtunes.app.feature.player.keyCodeToKeyboardTrigger(0, unicodeChar = '?'.code))
        assertEquals(GestureTrigger.KEY_F1, com.travelingtunes.app.feature.player.keyCodeToKeyboardTrigger(android.view.KeyEvent.KEYCODE_F1))
        assertEquals(GestureTrigger.KEY_F12, com.travelingtunes.app.feature.player.keyCodeToKeyboardTrigger(android.view.KeyEvent.KEYCODE_F12))
        assertEquals(GestureTrigger.KEY_MEDIA_PLAY_PAUSE, com.travelingtunes.app.feature.player.keyCodeToKeyboardTrigger(android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
    }

    @Test
    fun testKeyboardSubmenuTriggers() {
        val keyboardTriggersMap = com.travelingtunes.app.feature.settings.getTriggersForSubmenu(
            com.travelingtunes.app.feature.settings.GestureSubmenu.KEYBOARD,
            numEdgeRegions = 3
        )

        assertEquals(3, keyboardTriggersMap.size)
        assertTrue(keyboardTriggersMap.containsKey("Navigation & Control Keys"))
        assertTrue(keyboardTriggersMap.containsKey("F1-F12 Edge Buttons"))
        assertTrue(keyboardTriggersMap.containsKey("UI & Hardware Media Buttons"))

        val navKeys = keyboardTriggersMap["Navigation & Control Keys"]!!
        assertTrue(navKeys.contains(GestureTrigger.KEY_SPACE))
        assertTrue(navKeys.contains(GestureTrigger.KEY_F))
        assertTrue(navKeys.contains(GestureTrigger.KEY_QUESTION))

        val fKeys = keyboardTriggersMap["F1-F12 Edge Buttons"]!!
        assertEquals(12, fKeys.size)
        assertEquals(GestureTrigger.KEY_F1, fKeys[0])
        assertEquals(GestureTrigger.KEY_F12, fKeys[11])
    }
}
