package com.travelingtunes.app.feature.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
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

            // Navigation actions disabled/commented out:
            /*
            GestureAction.NAVIGATE_TO_CONTACT -> Icon(...)
            GestureAction.NAVIGATE_HOME -> Icon(...)
            GestureAction.NAVIGATE_WORK -> Icon(...)
            GestureAction.SHOW_DIRECTIONS -> Icon(...)
            GestureAction.RECENTER_MAP -> Icon(...)
            GestureAction.REPEAT_INSTRUCTIONS -> Icon(...)
            */

            GestureAction.UNASSIGNED -> {}
        }
    }
}

@Composable
fun RepeatModeIcon(
    repeatMode: RepeatMode,
    tint: Color,
    iconSize: Dp
) {
    val overlaySize = iconSize * 0.5f

    Box(
        modifier = Modifier.size(iconSize),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Repeat,
            contentDescription = repeatMode.displayName,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )

        when (repeatMode) {
            RepeatMode.SONG -> {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Repeat Song",
                    tint = tint,
                    modifier = Modifier.size(overlaySize)
                )
            }

            RepeatMode.ALBUM -> {
                Icon(
                    imageVector = Icons.Default.Album,
                    contentDescription = "Repeat Album",
                    tint = tint,
                    modifier = Modifier.size(overlaySize)
                )
            }

            RepeatMode.ARTIST -> {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Repeat Artist",
                    tint = tint,
                    modifier = Modifier.size(overlaySize)
                )
            }

            RepeatMode.GENRE -> {
                Text(
                    text = "*",
                    color = tint,
                    fontSize = (iconSize.value * 0.6f).sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (2).dp, y = (-4).dp)
                )
            }

            RepeatMode.FOLDER -> {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Repeat Folder",
                    tint = tint,
                    modifier = Modifier.size(overlaySize)
                )
            }

            RepeatMode.OFF -> {}
        }
    }
}

@Composable
fun ShuffleModeIcon(
    shuffleMode: ShuffleMode,
    tint: Color,
    iconSize: Dp
) {
    val overlaySize = iconSize * 0.5f

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

        when (shuffleMode) {
            ShuffleMode.SONGS -> {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Shuffle Songs",
                    tint = tint,
                    modifier = Modifier.size(overlaySize)
                )
            }

            ShuffleMode.ALBUMS -> {
                Icon(
                    imageVector = Icons.Default.Album,
                    contentDescription = "Shuffle Albums",
                    tint = tint,
                    modifier = Modifier.size(overlaySize)
                )
            }

            ShuffleMode.OFF -> {}
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
        GestureAction.TOGGLE_REPEAT -> if (repeatMode != RepeatMode.OFF) 1.0f else 0.25f
        GestureAction.TOGGLE_SHUFFLE -> if (shuffleMode != ShuffleMode.OFF) 1.0f else 0.25f
        GestureAction.PLAY -> if (isPlaying) 1.0f else 0.25f
        GestureAction.PAUSE -> if (!isPlaying) 1.0f else 0.25f
        GestureAction.PLAY_PAUSE -> if (isPlaying) 1.0f else 0.25f
        else -> 1.0f
    }
}
