package com.travelingtunes.app

import com.travelingtunes.app.core.model.GestureAction
import com.travelingtunes.app.core.model.GestureTrigger
import com.travelingtunes.app.core.model.SlideDirection
import com.travelingtunes.app.core.model.getSlideDirection
import com.travelingtunes.app.core.model.getReverseTrigger
import com.travelingtunes.app.core.model.ThemeSettings
import com.travelingtunes.app.feature.settings.GestureSubmenu
import com.travelingtunes.app.feature.settings.getSubmenu
import com.travelingtunes.app.feature.settings.getTriggersForSubmenu
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        assertEquals(8, submenus.size)
        assertEquals(com.travelingtunes.app.feature.settings.SettingsSubmenu.LIBRARY, submenus[0])
        assertEquals(com.travelingtunes.app.feature.settings.SettingsSubmenu.TITLES, submenus[1])
        assertEquals(com.travelingtunes.app.feature.settings.SettingsSubmenu.ART, submenus[2])
        assertEquals(com.travelingtunes.app.feature.settings.SettingsSubmenu.HUD, submenus[3])
        assertEquals(com.travelingtunes.app.feature.settings.SettingsSubmenu.THEMES, submenus[4])
        assertEquals(com.travelingtunes.app.feature.settings.SettingsSubmenu.GESTURES, submenus[5])
        assertEquals(com.travelingtunes.app.feature.settings.SettingsSubmenu.ANDROID_AUTO, submenus[6])
        assertEquals(com.travelingtunes.app.feature.settings.SettingsSubmenu.ABOUT, submenus[7])

        val standalone = submenus.filter { it.categoryGroup == null }
        assertEquals(1, standalone.size)
        assertEquals(com.travelingtunes.app.feature.settings.SettingsSubmenu.LIBRARY, standalone[0])

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
        assertEquals(4, submenus.size)
        assertEquals(com.travelingtunes.app.feature.settings.GestureSubmenu.SWIPE, submenus[0])
        assertEquals(com.travelingtunes.app.feature.settings.GestureSubmenu.TAP, submenus[1])
        assertEquals(com.travelingtunes.app.feature.settings.GestureSubmenu.BUTTON, submenus[2])
        assertEquals(com.travelingtunes.app.feature.settings.GestureSubmenu.RADIAL_MENU, submenus[3])

        assertEquals("Swipe Actions", submenus[0].title)
        assertEquals("Tap Actions", submenus[1].title)
        assertEquals("Button Actions", submenus[2].title)
        assertEquals("Radial Menu Actions", submenus[3].title)
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
}
