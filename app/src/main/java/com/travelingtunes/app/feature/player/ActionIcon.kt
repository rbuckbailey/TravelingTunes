package com.travelingtunes.app.feature.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AlignHorizontalCenter
import androidx.compose.material.icons.filled.AlignHorizontalLeft
import androidx.compose.material.icons.filled.AlignHorizontalRight
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.BorderBottom
import androidx.compose.material.icons.filled.BorderOuter
import androidx.compose.material.icons.filled.CarRental
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShortText
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.TimeToLeave
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignCenter
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelingtunes.app.core.model.GestureAction
import com.travelingtunes.app.core.model.RepeatMode
import com.travelingtunes.app.core.model.ShuffleMode

@Composable
fun ActionIcon(
    action: GestureAction,
    optionKey: String? = null,
    repeatMode: RepeatMode = RepeatMode.OFF,
    shuffleMode: ShuffleMode = ShuffleMode.OFF,
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    iconSize: Dp = 28.dp
) {
    if (action == GestureAction.UNASSIGNED) return

    val alpha = getActionAlpha(action, repeatMode, shuffleMode, isPlaying)
    val effectiveTint = tint.copy(alpha = alpha)

    Box(
        modifier = modifier.size(iconSize),
        contentAlignment = Alignment.Center
    ) {
        when (action) {
            GestureAction.TOGGLE_REPEAT -> RepeatModeIcon(
                repeatMode = repeatMode,
                tint = effectiveTint,
                iconSize = iconSize
            )

            GestureAction.TOGGLE_SHUFFLE -> ShuffleModeIcon(
                shuffleMode = shuffleMode,
                tint = effectiveTint,
                iconSize = iconSize
            )

            GestureAction.PLAY -> Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = action.displayName,
                tint = effectiveTint,
                modifier = Modifier.size(iconSize)
            )

            GestureAction.PAUSE -> Icon(
                imageVector = Icons.Default.Pause,
                contentDescription = action.displayName,
                tint = effectiveTint,
                modifier = Modifier.size(iconSize)
            )

            GestureAction.PLAY_PAUSE -> Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = action.displayName,
                tint = effectiveTint,
                modifier = Modifier.size(iconSize)
            )

            GestureAction.FAST_FORWARD -> Icon(
                imageVector = Icons.Default.FastForward,
                contentDescription = action.displayName,
                tint = effectiveTint,
                modifier = Modifier.size(iconSize)
            )

            GestureAction.REWIND -> Icon(
                imageVector = Icons.Default.FastRewind,
                contentDescription = action.displayName,
                tint = effectiveTint,
                modifier = Modifier.size(iconSize)
            )

            GestureAction.NEXT -> Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = action.displayName,
                tint = effectiveTint,
                modifier = Modifier.size(iconSize)
            )

            GestureAction.PREVIOUS -> Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = action.displayName,
                tint = effectiveTint,
                modifier = Modifier.size(iconSize)
            )

            GestureAction.RESTART -> Icon(
                imageVector = Icons.Default.Replay,
                contentDescription = action.displayName,
                tint = effectiveTint,
                modifier = Modifier.size(iconSize)
            )

            GestureAction.RESTART_PREVIOUS -> Icon(
                imageVector = Icons.AutoMirrored.Filled.Undo,
                contentDescription = action.displayName,
                tint = effectiveTint,
                modifier = Modifier.size(iconSize)
            )

            GestureAction.SONG_PICKER -> Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = action.displayName,
                tint = effectiveTint,
                modifier = Modifier.size(iconSize)
            )

            GestureAction.SHOW_QUEUE -> Icon(
                imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                contentDescription = action.displayName,
                tint = effectiveTint,
                modifier = Modifier.size(iconSize)
            )

            GestureAction.MENU -> Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = action.displayName,
                tint = effectiveTint,
                modifier = Modifier.size(iconSize)
            )

            GestureAction.VOLUME_UP -> Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = action.displayName,
                tint = effectiveTint,
                modifier = Modifier.size(iconSize)
            )

            GestureAction.VOLUME_DOWN -> Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeDown,
                contentDescription = action.displayName,
                tint = effectiveTint,
                modifier = Modifier.size(iconSize)
            )

            GestureAction.SHUFFLE_ALL_SONGS -> Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = action.displayName,
                tint = effectiveTint,
                modifier = Modifier.size(iconSize)
            )

            GestureAction.PLAY_CURRENT_ARTIST -> Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = action.displayName,
                tint = effectiveTint,
                modifier = Modifier.size(iconSize)
            )

            GestureAction.PLAY_CURRENT_ALBUM -> Icon(
                imageVector = Icons.Default.LibraryMusic,
                contentDescription = action.displayName,
                tint = effectiveTint,
                modifier = Modifier.size(iconSize)
            )

            GestureAction.NEXT_ALBUM -> Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = action.displayName,
                tint = effectiveTint,
                modifier = Modifier.size(iconSize)
            )

            GestureAction.PREVIOUS_ALBUM -> Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = action.displayName,
                tint = effectiveTint,
                modifier = Modifier.size(iconSize)
            )

            GestureAction.SELECT_ALBUM_VIEW -> Icon(
                imageVector = Icons.Default.Album,
                contentDescription = action.displayName,
                tint = effectiveTint,
                modifier = Modifier.size(iconSize)
            )

            GestureAction.SELECT_ARTIST_VIEW -> Icon(
                imageVector = Icons.Default.Person,
                contentDescription = action.displayName,
                tint = effectiveTint,
                modifier = Modifier.size(iconSize)
            )

            GestureAction.INCREASE_RATING -> Icon(
                imageVector = Icons.Default.ThumbUp,
                contentDescription = action.displayName,
                tint = effectiveTint,
                modifier = Modifier.size(iconSize)
            )

            GestureAction.DECREASE_RATING -> Icon(
                imageVector = Icons.Default.ThumbDown,
                contentDescription = action.displayName,
                tint = effectiveTint,
                modifier = Modifier.size(iconSize)
            )

            GestureAction.SHOW_QUICK_START -> Icon(
                imageVector = Icons.AutoMirrored.Filled.Help,
                contentDescription = action.displayName,
                tint = effectiveTint,
                modifier = Modifier.size(iconSize)
            )

            GestureAction.DELETE_DOWNLOADED_ART -> Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = action.displayName,
                tint = effectiveTint,
                modifier = Modifier.size(iconSize)
            )

            GestureAction.TOGGLE_DRIVING_MODE -> Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = action.displayName,
                tint = effectiveTint,
                modifier = Modifier.size(iconSize)
            )

            GestureAction.TOGGLE_DOCKED_ART -> Icon(
                imageVector = Icons.Default.Album,
                contentDescription = action.displayName,
                tint = effectiveTint,
                modifier = Modifier.size(iconSize)
            )

            GestureAction.RADIAL_MENU -> Icon(
                imageVector = Icons.Default.DonutLarge,
                contentDescription = action.displayName,
                tint = effectiveTint,
                modifier = Modifier.size(iconSize)
            )

            GestureAction.OTHER_OPTION -> {
                if (optionKey != null) {
                    ConfigOptionIcon(
                        optionKey = optionKey,
                        tint = effectiveTint,
                        iconSize = iconSize
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = action.displayName,
                        tint = effectiveTint,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }

            GestureAction.UNASSIGNED -> {}
        }
    }
}

@Composable
fun ConfigOptionIcon(
    optionKey: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    iconSize: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    if (optionKey.equals("THEME_MONDRIAN", ignoreCase = true)) {
        MondrianIcon(modifier = modifier, iconSize = iconSize)
        return
    }

    val (baseVector, overlayBadge) = getConfigOptionIconVector(optionKey)

    Box(
        modifier = modifier.size(iconSize),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = baseVector,
            contentDescription = optionKey,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )

        if (overlayBadge != null) {
            Icon(
                imageVector = overlayBadge,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(iconSize * 0.45f)
                    .align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
fun MondrianIcon(
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp
) {
    Canvas(
        modifier = modifier.size(iconSize)
    ) {
        val w = size.width
        val h = size.height

        // Background canvas: crisp white
        drawRect(color = Color.White, size = size)

        // Mondrian Red block top-left (0..0.6w, 0..0.6h)
        drawRect(
            color = Color(0xFFDD2C00),
            topLeft = Offset(0f, 0f),
            size = Size(w * 0.6f, h * 0.6f)
        )

        // Mondrian Blue block bottom-left (0..0.3w, 0.75h..h)
        drawRect(
            color = Color(0xFF1976D2),
            topLeft = Offset(0f, h * 0.75f),
            size = Size(w * 0.3f, h * 0.25f)
        )

        // Mondrian Yellow block right (0.8w..w, 0.6h..0.85h)
        drawRect(
            color = Color(0xFFFFD600),
            topLeft = Offset(w * 0.8f, h * 0.6f),
            size = Size(w * 0.2f, h * 0.25f)
        )

        // Black grid lines
        val strokeW = (w * 0.08f).coerceAtLeast(1.5f)
        val black = Color.Black

        // Horizontal grid lines
        drawLine(black, start = Offset(0f, h * 0.6f), end = Offset(w, h * 0.6f), strokeWidth = strokeW)
        drawLine(black, start = Offset(0f, h * 0.75f), end = Offset(w * 0.8f, h * 0.75f), strokeWidth = strokeW)
        drawLine(black, start = Offset(w * 0.8f, h * 0.85f), end = Offset(w, h * 0.85f), strokeWidth = strokeW)

        // Vertical grid lines
        drawLine(black, start = Offset(w * 0.6f, 0f), end = Offset(w * 0.6f, h * 0.6f), strokeWidth = strokeW)
        drawLine(black, start = Offset(w * 0.3f, h * 0.6f), end = Offset(w * 0.3f, h), strokeWidth = strokeW)
        drawLine(black, start = Offset(w * 0.8f, h * 0.6f), end = Offset(w * 0.8f, h), strokeWidth = strokeW)

        // Outer border stroke
        drawRect(color = black, style = Stroke(width = strokeW))
    }
}

private fun getConfigOptionIconVector(optionKey: String): Pair<ImageVector, ImageVector?> {
    return when (optionKey) {
        // Colors & Themes
        "THEME_MATCH_ALBUM_ART" -> Icons.Default.Palette to null
        "THEME_WHITE_ON_GREY" -> Icons.Default.Contrast to null
        "THEME_GREY_ON_BLACK" -> Icons.Default.DarkMode to null
        "THEME_LEAF" -> Icons.Default.Spa to null
        "THEME_OLD_WEST" -> Icons.Default.Landscape to null
        "THEME_PERIWINKLE_BLUE" -> Icons.Default.WaterDrop to null
        "THEME_LAVENDER" -> Icons.Default.LocalFlorist to null
        "THEME_BLUSH" -> Icons.Default.Favorite to null
        "THEME_HOT_DOG_STAND" -> Icons.Default.Fastfood to null
        "THEME_CUSTOM" -> Icons.Default.ColorLens to null
        "THEME_dimAtNight" -> Icons.Default.NightsStay to null
        "THEME_invertAtNight" -> Icons.Default.InvertColors to null
        "THEME_isRounded" -> Icons.Default.RoundedCorner to null
        "THEME_isGlass" -> Icons.Default.BlurOn to null

        // Alignments
        "ALIGN_ARTIST_LEFT" -> Icons.AutoMirrored.Filled.FormatAlignLeft to Icons.Default.Person
        "ALIGN_ARTIST_CENTER" -> Icons.Default.FormatAlignCenter to Icons.Default.Person
        "ALIGN_ARTIST_RIGHT" -> Icons.AutoMirrored.Filled.FormatAlignRight to Icons.Default.Person
        "ALIGN_SONG_LEFT" -> Icons.AutoMirrored.Filled.FormatAlignLeft to Icons.Default.MusicNote
        "ALIGN_SONG_CENTER" -> Icons.Default.FormatAlignCenter to Icons.Default.MusicNote
        "ALIGN_SONG_RIGHT" -> Icons.AutoMirrored.Filled.FormatAlignRight to Icons.Default.MusicNote
        "ALIGN_ALBUM_LEFT" -> Icons.AutoMirrored.Filled.FormatAlignLeft to Icons.Default.Album
        "ALIGN_ALBUM_CENTER" -> Icons.Default.FormatAlignCenter to Icons.Default.Album
        "ALIGN_ALBUM_RIGHT" -> Icons.AutoMirrored.Filled.FormatAlignRight to Icons.Default.Album

        // Font Styles
        "DISPLAY_artistBold" -> Icons.Default.FormatBold to Icons.Default.Person
        "DISPLAY_artistItalic" -> Icons.Default.FormatItalic to Icons.Default.Person
        "DISPLAY_artistUnderline" -> Icons.Default.FormatUnderlined to Icons.Default.Person
        "DISPLAY_songBold" -> Icons.Default.FormatBold to Icons.Default.MusicNote
        "DISPLAY_songItalic" -> Icons.Default.FormatItalic to Icons.Default.MusicNote
        "DISPLAY_songUnderline" -> Icons.Default.FormatUnderlined to Icons.Default.MusicNote
        "DISPLAY_albumBold" -> Icons.Default.FormatBold to Icons.Default.Album
        "DISPLAY_albumItalic" -> Icons.Default.FormatItalic to Icons.Default.Album
        "DISPLAY_albumUnderline" -> Icons.Default.FormatUnderlined to Icons.Default.Album

        // Title Shrink/Scroll
        "DISPLAY_titleShrinkInPortrait" -> Icons.Default.Compress to null
        "DISPLAY_titleShrinkLong" -> Icons.Default.FitScreen to null
        "DISPLAY_titleScrollLong" -> Icons.Default.SwapHoriz to null

        // Art, HUD & Display
        "DISPLAY_showAlbumArt" -> Icons.Default.Image to null
        "DISPLAY_albumArtColors" -> Icons.Default.AutoFixHigh to null
        "ART_SCALE_FILL_SCREEN" -> Icons.Default.Fullscreen to null
        "ART_SCALE_ASPECT_FIT" -> Icons.Default.FullscreenExit to null
        "ART_LAYOUT_OVERLAY" -> Icons.Default.FlipToBack to null
        "ART_LAYOUT_DOCKED" -> Icons.Default.VerticalSplit to null
        "ART_ALIGN_PORT_TOP" -> Icons.Default.VerticalAlignTop to null
        "ART_ALIGN_PORT_MIDDLE" -> Icons.Default.VerticalAlignCenter to null
        "ART_ALIGN_PORT_BOTTOM" -> Icons.Default.VerticalAlignBottom to null
        "ART_ALIGN_LAND_LEFT" -> Icons.Default.AlignHorizontalLeft to null
        "ART_ALIGN_LAND_CENTER" -> Icons.Default.AlignHorizontalCenter to null
        "ART_ALIGN_LAND_RIGHT" -> Icons.Default.AlignHorizontalRight to null

        "HUD_TYPE_NONE" -> Icons.Default.VisibilityOff to null
        "HUD_TYPE_EDGE_HUD" -> Icons.Default.BorderOuter to null
        "HUD_TYPE_NUMBER" -> Icons.Default.LinearScale to null
        "HUD_TYPE_BAR_VOLUME" -> Icons.Default.BarChart to null
        "SCRUB_HUD_TYPE_NONE" -> Icons.Default.TimerOff to null
        "SCRUB_HUD_TYPE_EDGE_HUD" -> Icons.Default.BorderBottom to null
        "SCRUB_HUD_TYPE_POPUP" -> Icons.Default.ShortText to null
        "SCRUB_HUD_TYPE_BAR_PROGRESS" -> Icons.Default.HorizontalRule to null

        "DISPLAY_volumeAlwaysOn" -> Icons.Default.LockClock to null
        "DISPLAY_showStatusBar" -> Icons.Default.SignalCellular4Bar to null
        "DISPLAY_showActions" -> Icons.Default.Widgets to null
        "DISPLAY_keepScreenOn" -> Icons.Default.WbSunny to null
        "DISPLAY_immersiveMode" -> Icons.Default.Tv to null

        // Library & Auto
        "LIBRARY_autoRescan" -> Icons.Default.Sync to null
        "LIBRARY_gpsVolume" -> Icons.Default.Speed to null
        "AUTO_drivingMode" -> Icons.Default.DirectionsCar to null
        "AUTO_autoEnableDrivingMode" -> Icons.Default.TimeToLeave to null
        "AUTO_speedVolume" -> Icons.Default.CarRental to null

        else -> Icons.Default.Tune to null
    }
}

@Composable
fun RepeatModeIcon(
    repeatMode: RepeatMode,
    tint: Color,
    iconSize: Dp
) {
    val badgeLabel = when (repeatMode) {
        RepeatMode.SONG -> "1"
        RepeatMode.ALBUM -> "ALB"
        RepeatMode.ARTIST -> "ART"
        RepeatMode.GENRE -> "GNR"
        RepeatMode.FOLDER -> "FLD"
        RepeatMode.OFF -> null
    }

    Box(
        modifier = Modifier.size(iconSize),
        contentAlignment = Alignment.Center
    ) {
        if (repeatMode == RepeatMode.SONG) {
            Icon(
                imageVector = Icons.Default.RepeatOne,
                contentDescription = repeatMode.displayName,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Repeat,
                contentDescription = repeatMode.displayName,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }

        if (badgeLabel != null && repeatMode != RepeatMode.SONG) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(3.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
            ) {
                Text(
                    text = badgeLabel,
                    fontSize = (iconSize.value * 0.28f).sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 0.dp)
                )
            }
        }
    }
}

@Composable
fun ShuffleModeIcon(
    shuffleMode: ShuffleMode,
    tint: Color,
    iconSize: Dp
) {
    val badgeLabel = when (shuffleMode) {
        ShuffleMode.SONGS -> "ALL"
        ShuffleMode.ALBUMS -> "ALB"
        ShuffleMode.OFF -> null
    }

    Box(
        modifier = Modifier.size(iconSize),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Shuffle,
            contentDescription = shuffleMode.displayName,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )

        if (badgeLabel != null) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(3.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
            ) {
                Text(
                    text = badgeLabel,
                    fontSize = (iconSize.value * 0.28f).sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 0.dp)
                )
            }
        }
    }
}

private fun getActionAlpha(
    action: GestureAction,
    repeatMode: RepeatMode,
    shuffleMode: ShuffleMode,
    isPlaying: Boolean
): Float {
    return when (action) {
        GestureAction.TOGGLE_REPEAT -> if (repeatMode != RepeatMode.OFF) 1.0f else 0.45f
        GestureAction.TOGGLE_SHUFFLE -> if (shuffleMode != ShuffleMode.OFF) 1.0f else 0.45f
        GestureAction.PLAY -> if (isPlaying) 1.0f else 0.55f
        GestureAction.PAUSE -> if (!isPlaying) 1.0f else 0.55f
        GestureAction.PLAY_PAUSE -> if (isPlaying) 1.0f else 0.55f
        else -> 1.0f
    }
}
