package com.travelingtunes.app

import com.travelingtunes.app.core.model.GestureAction
import com.travelingtunes.app.core.model.GestureTrigger
import com.travelingtunes.app.core.model.SlideDirection
import com.travelingtunes.app.core.model.getSlideDirection
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

    @Test
    fun testSlideDirectionForTriggers() {
        // Buttons: Top regions -> TOP, Bottom regions -> BOTTOM
        assertEquals(com.travelingtunes.app.core.model.SlideDirection.TOP, GestureTrigger.CORNER_TOP_LEFT.getSlideDirection())
        assertEquals(com.travelingtunes.app.core.model.SlideDirection.TOP, GestureTrigger.CORNER_TOP_CENTER.getSlideDirection())
        assertEquals(com.travelingtunes.app.core.model.SlideDirection.BOTTOM, GestureTrigger.CORNER_BOTTOM_LEFT.getSlideDirection())
        assertEquals(com.travelingtunes.app.core.model.SlideDirection.BOTTOM, GestureTrigger.CORNER_BOTTOM_RIGHT.getSlideDirection())

        // Taps: -> BOTTOM
        assertEquals(com.travelingtunes.app.core.model.SlideDirection.BOTTOM, GestureTrigger.TAP_1_1.getSlideDirection())
        assertEquals(com.travelingtunes.app.core.model.SlideDirection.BOTTOM, GestureTrigger.TAP_2_1.getSlideDirection())
        assertEquals(com.travelingtunes.app.core.model.SlideDirection.BOTTOM, GestureTrigger.LONG_PRESS_1.getSlideDirection())

        // Slide gestures (Swipes): Gesture direction
        assertEquals(com.travelingtunes.app.core.model.SlideDirection.TOP, GestureTrigger.SWIPE_1_UP.getSlideDirection())
        assertEquals(com.travelingtunes.app.core.model.SlideDirection.BOTTOM, GestureTrigger.SWIPE_1_DOWN.getSlideDirection())
        assertEquals(com.travelingtunes.app.core.model.SlideDirection.LEFT, GestureTrigger.SWIPE_1_LEFT.getSlideDirection())
        assertEquals(com.travelingtunes.app.core.model.SlideDirection.RIGHT, GestureTrigger.SWIPE_1_RIGHT.getSlideDirection())
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
}
