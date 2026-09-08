package com.travelingtunes.app.feature.player

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Size
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelingtunes.app.core.database.MusicDatabase
import com.travelingtunes.app.core.gestures.GestureEventListener
import com.travelingtunes.app.core.gestures.travelingTunesGestures
import com.travelingtunes.app.core.media.AlbumArtCache
import com.travelingtunes.app.core.media.MusicScanner
import com.travelingtunes.app.core.media.PlaybackManager
import com.travelingtunes.app.core.model.ArtLayoutOption
import com.travelingtunes.app.core.model.ArtScaleOption
import com.travelingtunes.app.core.model.DisplaySettings
import com.travelingtunes.app.core.model.GestureAction
import com.travelingtunes.app.core.model.GestureBinding
import com.travelingtunes.app.core.model.GestureCategory
import com.travelingtunes.app.core.model.GestureTrigger
import com.travelingtunes.app.core.model.HudTypeOption
import com.travelingtunes.app.core.model.RepeatMode
import com.travelingtunes.app.core.model.ScrubHudTypeOption
import com.travelingtunes.app.core.model.ShuffleMode
import com.travelingtunes.app.core.model.Song
import com.travelingtunes.app.core.model.TextAlignmentOption
import com.travelingtunes.app.feature.songpicker.SongPickerBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerScreen(
    playbackManager: PlaybackManager,
    musicDatabase: MusicDatabase,
    displaySettings: DisplaySettings,
    gestureBindings: Map<GestureTrigger, GestureBinding>,
    musicScanner: MusicScanner? = null,
    showFirstRunPrompt: Boolean = false,
    onDismissFirstRunPrompt: () -> Unit = {},
    onPickMusicFolder: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenQuickStart: () -> Unit,
    onOpenContacts: () -> Unit
) {
    val context = LocalContext.current
    val currentSong by playbackManager.currentSong.collectAsState()
    val currentPlaylist by playbackManager.currentPlaylist.collectAsState()
    val currentPositionMs by playbackManager.currentPositionMs.collectAsState()
    val durationMs by playbackManager.durationMs.collectAsState()
    val currentVolumeRatio by playbackManager.currentVolumeRatio.collectAsState()
    val actionHudText by playbackManager.actionHudText.collectAsState()
    val isPlaying by playbackManager.isPlaying.collectAsState()
    val repeatMode by playbackManager.repeatMode.collectAsState()
    val shuffleMode by playbackManager.shuffleMode.collectAsState()

    var showSongPicker by remember { mutableStateOf(false) }
    var showRepeatOptionsDialog by remember { mutableStateOf(false) }
    var showShuffleOptionsDialog by remember { mutableStateOf(false) }
    val pageCount = currentPlaylist.size.coerceAtLeast(1)
    val songIndex = currentPlaylist.indexOfFirst { it.id == currentSong?.id }.coerceAtLeast(0)

    val pagerState = rememberPagerState(initialPage = songIndex) { pageCount }
    val coroutineScope = rememberCoroutineScope()

    // Sync pagerState -> PlaybackManager when user swipes pager to a settled page
    LaunchedEffect(pagerState.settledPage) {
        if (currentPlaylist.isNotEmpty() && pagerState.settledPage in currentPlaylist.indices) {
            val selectedSong = currentPlaylist[pagerState.settledPage]
            if (selectedSong.id != currentSong?.id) {
                playbackManager.playSongAtIndex(pagerState.settledPage)
            }
        }
    }

    // Sync PlaybackManager -> pagerState when song changes externally
    LaunchedEffect(currentSong?.id) {
        if (songIndex in 0 until pageCount && pagerState.settledPage != songIndex) {
            pagerState.animateScrollToPage(songIndex)
        }
    }

    // Pre-cache surrounding album art
    LaunchedEffect(songIndex, currentPlaylist) {
        AlbumArtCache.instance.preCacheSurroundingSongs(context, currentPlaylist, songIndex, radius = 4)
    }

    // Auto-clear action HUD text after 2 seconds
    LaunchedEffect(actionHudText) {
        if (actionHudText != null) {
            delay(2000L)
            playbackManager.clearHudAction()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val screenHeightPx = this.constraints.maxHeight.toFloat()

        val gestureListener = object : GestureEventListener {
            override fun onGestureTriggered(trigger: GestureTrigger) {
                var binding = gestureBindings[trigger]
                var action = binding?.action ?: GestureAction.UNASSIGNED

                // When a touch region is unassigned, pass the tap through to the standard tap action
                if (action == GestureAction.UNASSIGNED && trigger.category == GestureCategory.SCREEN_REGION) {
                    val fallbackTrigger = GestureTrigger.TAP_1_1
                    binding = gestureBindings[fallbackTrigger]
                    action = binding?.action ?: GestureAction.UNASSIGNED
                }

                if (action == GestureAction.UNASSIGNED) return

                if (trigger.category == GestureCategory.LONG_PRESS) {
                    if (action == GestureAction.TOGGLE_REPEAT) {
                        showRepeatOptionsDialog = true
                        return
                    } else if (action == GestureAction.TOGGLE_SHUFFLE) {
                        showShuffleOptionsDialog = true
                        return
                    }
                }

                when (action) {
                    GestureAction.NEXT -> {
                        coroutineScope.launch {
                            if (pagerState.currentPage < pageCount - 1) {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            } else {
                                playbackManager.next()
                            }
                        }
                    }
                    GestureAction.PREVIOUS -> {
                        coroutineScope.launch {
                            if (pagerState.currentPage > 0) {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            } else {
                                playbackManager.previous()
                            }
                        }
                    }
                    else -> {
                        handleGestureAction(
                            action = action,
                            playbackManager = playbackManager,
                            onOpenSongPicker = { showSongPicker = true },
                            onOpenSettings = onOpenSettings,
                            onOpenQuickStart = onOpenQuickStart,
                            onOpenContacts = onOpenContacts
                        )
                    }
                }
            }

            override fun onContinuousGesture(
                trigger: GestureTrigger,
                delta: Float,
                deltaX: Float,
                deltaY: Float,
                totalDx: Float,
                totalDy: Float
            ) {
                val binding = gestureBindings[trigger]
                val action = binding?.action ?: GestureAction.UNASSIGNED

                when (action) {
                    GestureAction.VOLUME_UP, GestureAction.VOLUME_DOWN -> {
                        playbackManager.adjustVolumeByDelta(deltaY, screenHeightPx)
                    }
                    GestureAction.FAST_FORWARD -> {
                        playbackManager.fastForward(2000L)
                    }
                    GestureAction.REWIND -> {
                        playbackManager.rewind(2000L)
                    }
                    else -> {
                        if (trigger.name.contains("SWIPE") && (trigger.name.contains("UP") || trigger.name.contains("DOWN"))) {
                            playbackManager.adjustVolumeByDelta(deltaY, screenHeightPx)
                        }
                    }
                }
            }

            override fun onGestureEnd(totalDx: Float, totalDy: Float, fingers: Int) {
                if (kotlin.math.abs(totalDy) > 20f && kotlin.math.abs(totalDy) > kotlin.math.abs(totalDx)) {
                    val volPct = (playbackManager.currentVolumeRatio.value * 100).toInt()
                    playbackManager.showHudAction("Volume: $volPct%")
                }
            }
        }

        // 1. Sliding Page Transition (Album Art + Song Titles & Labels)
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val pageSong = currentPlaylist.getOrNull(page) ?: currentSong

            Box(modifier = Modifier.fillMaxSize()) {
                // Album Art Background for this page
                PlayerAlbumArtBackground(
                    song = pageSong,
                    displaySettings = displaySettings
                )

                // Main Song Labels Container for this page
                SongLabelsLayout(
                    artist = pageSong?.artist ?: "Traveling Tunes",
                    title = pageSong?.title ?: "Swipe or Tap Screen to Play",
                    album = pageSong?.album ?: "No Song Selected",
                    displaySettings = displaySettings
                )
            }
        }

        // 2. Gesture Detector Touch Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .travelingTunesGestures(gestureListener)
        )

        // 3. Geometric Volume HUD Overlay (Bar / Line / Edge)
        VolumeHudOverlay(
            volumeRatio = currentVolumeRatio,
            displaySettings = displaySettings,
            modifier = Modifier.align(Alignment.BottomStart)
        )

        // 4. Geometric Progress / Playback Bar Overlay (Edge Bar / Line)
        ProgressHudOverlay(
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            displaySettings = displaySettings,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // 5. Screen Region Icons Overlay
        ScreenRegionIconsOverlay(
            gestureBindings = gestureBindings,
            playbackManager = playbackManager,
            repeatMode = repeatMode,
            shuffleMode = shuffleMode,
            isPlaying = isPlaying,
            onOpenSongPicker = { showSongPicker = true },
            onOpenSettings = onOpenSettings,
            onOpenQuickStart = onOpenQuickStart,
            onOpenContacts = onOpenContacts,
            onShowRepeatOptions = { showRepeatOptionsDialog = true },
            onShowShuffleOptions = { showShuffleOptionsDialog = true }
        )

        // 7. Action HUD Banner Overlay
        AnimatedVisibility(
            visible = actionHudText != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            actionHudText?.let { text ->
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(horizontal = 32.dp, vertical = 20.dp)
                ) {
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 7. Song Picker Sheet
        if (showSongPicker) {
            SongPickerBottomSheet(
                musicDatabase = musicDatabase,
                playbackManager = playbackManager,
                musicScanner = musicScanner,
                onDismiss = { showSongPicker = false }
            )
        }

        // 8. First Run Prompt Dialog
        if (showFirstRunPrompt) {
            AlertDialog(
                onDismissRequest = { onDismissFirstRunPrompt() },
                title = { Text("Select Music Library Folder") },
                text = { Text("To play music, please select the folder on your device where your music files are stored. Traveling Tunes will scan and build a database of your music.") },
                confirmButton = {
                    TextButton(onClick = {
                        onDismissFirstRunPrompt()
                        onPickMusicFolder()
                    }) {
                        Text("Select Folder")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onDismissFirstRunPrompt() }) {
                        Text("Later")
                    }
                }
            )
        }

        // 9. Repeat & Shuffle Options Dialogs
        if (showRepeatOptionsDialog) {
            RepeatOptionsDialog(
                currentRepeatMode = repeatMode,
                onSelectRepeatMode = { mode ->
                    playbackManager.setRepeatMode(mode)
                },
                onDismiss = { showRepeatOptionsDialog = false }
            )
        }

        if (showShuffleOptionsDialog) {
            ShuffleOptionsDialog(
                currentShuffleMode = shuffleMode,
                onSelectShuffleMode = { mode ->
                    playbackManager.setShuffleMode(mode)
                },
                onDismiss = { showShuffleOptionsDialog = false }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScreenRegionIconsOverlay(
    gestureBindings: Map<GestureTrigger, GestureBinding>,
    playbackManager: PlaybackManager,
    repeatMode: RepeatMode,
    shuffleMode: ShuffleMode,
    isPlaying: Boolean,
    onOpenSongPicker: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenQuickStart: () -> Unit,
    onOpenContacts: () -> Unit,
    onShowRepeatOptions: () -> Unit,
    onShowShuffleOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val regionTriggers = listOf(
        GestureTrigger.CORNER_TOP_LEFT to Alignment.TopStart,
        GestureTrigger.CORNER_TOP_CENTER to Alignment.TopCenter,
        GestureTrigger.CORNER_TOP_RIGHT to Alignment.TopEnd,
        GestureTrigger.CORNER_BOTTOM_LEFT to Alignment.BottomStart,
        GestureTrigger.CORNER_BOTTOM_CENTER to Alignment.BottomCenter,
        GestureTrigger.CORNER_BOTTOM_RIGHT to Alignment.BottomEnd
    )

    Box(modifier = modifier.fillMaxSize()) {
        for ((trigger, alignment) in regionTriggers) {
            val binding = gestureBindings[trigger]
            val action = binding?.action ?: GestureAction.UNASSIGNED
            if (action != GestureAction.UNASSIGNED) {
                val paddingModifier = when (alignment) {
                    Alignment.TopStart -> Modifier.padding(top = 8.dp, start = 8.dp)
                    Alignment.TopCenter -> Modifier.padding(top = 8.dp)
                    Alignment.TopEnd -> Modifier.padding(top = 8.dp, end = 8.dp)
                    Alignment.BottomStart -> Modifier.padding(bottom = 16.dp, start = 8.dp)
                    Alignment.BottomCenter -> Modifier.padding(bottom = 16.dp)
                    Alignment.BottomEnd -> Modifier.padding(bottom = 16.dp, end = 8.dp)
                    else -> Modifier.padding(8.dp)
                }

                Box(
                    modifier = Modifier
                        .align(alignment)
                        .then(paddingModifier)
                        .size(72.dp)
                        .clip(CircleShape)
                        .combinedClickable(
                            onClick = {
                                handleGestureAction(
                                    action = action,
                                    playbackManager = playbackManager,
                                    onOpenSongPicker = onOpenSongPicker,
                                    onOpenSettings = onOpenSettings,
                                    onOpenQuickStart = onOpenQuickStart,
                                    onOpenContacts = onOpenContacts
                                )
                            },
                            onLongClick = {
                                if (action == GestureAction.TOGGLE_REPEAT) {
                                    onShowRepeatOptions()
                                } else if (action == GestureAction.TOGGLE_SHUFFLE) {
                                    onShowShuffleOptions()
                                } else {
                                    handleGestureAction(
                                        action = action,
                                        playbackManager = playbackManager,
                                        onOpenSongPicker = onOpenSongPicker,
                                        onOpenSettings = onOpenSettings,
                                        onOpenQuickStart = onOpenQuickStart,
                                        onOpenContacts = onOpenContacts
                                    )
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    ActionIcon(
                        action = action,
                        repeatMode = repeatMode,
                        shuffleMode = shuffleMode,
                        isPlaying = isPlaying,
                        iconSize = 36.dp
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerAlbumArtBackground(
    song: Song?,
    displaySettings: DisplaySettings,
    modifier: Modifier = Modifier
) {
    if (!displaySettings.showAlbumArt || song == null) return

    val context = LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    var bitmap by remember(song.id, song.artworkUri) {
        mutableStateOf(AlbumArtCache.instance.get(song.id))
    }
    var dominantBgColor by remember(song.id) { mutableStateOf<Color?>(null) }

    LaunchedEffect(song.id, song.artworkUri, song.contentUri) {
        val cached = AlbumArtCache.instance.get(song.id)
        if (cached != null) {
            bitmap = cached
        } else {
            withContext(Dispatchers.IO) {
                val loadedBitmap = loadSongArtwork(context, song)
                if (loadedBitmap != null) {
                    val imgBmp = loadedBitmap.asImageBitmap()
                    AlbumArtCache.instance.put(song.id, imgBmp)
                    bitmap = imgBmp
                }
            }
        }
    }

    LaunchedEffect(bitmap) {
        val imgBmp = bitmap
        if (imgBmp != null) {
            withContext(Dispatchers.Default) {
                try {
                    val palette = androidx.palette.graphics.Palette.from(imgBmp.asAndroidBitmap()).generate()
                    val domSwatch = palette.dominantSwatch
                    if (domSwatch != null) {
                        dominantBgColor = Color(domSwatch.rgb)
                    }
                } catch (ignored: Exception) {}
            }
        }
    }

    val imgBitmap = bitmap ?: return

    val contentScale = when (displaySettings.albumArtScale) {
        ArtScaleOption.FILL_SCREEN -> ContentScale.Crop
        ArtScaleOption.ASPECT_FIT -> ContentScale.Fit
    }

    val imageAlignment = if (isLandscape) {
        when (displaySettings.artAlignmentLandscape) {
            com.travelingtunes.app.core.model.ArtAlignmentLandscape.LEFT -> Alignment.CenterStart
            com.travelingtunes.app.core.model.ArtAlignmentLandscape.MIDDLE -> Alignment.Center
            com.travelingtunes.app.core.model.ArtAlignmentLandscape.RIGHT -> Alignment.CenterEnd
        }
    } else {
        when (displaySettings.artAlignmentPortrait) {
            com.travelingtunes.app.core.model.ArtAlignmentPortrait.TOP -> Alignment.TopCenter
            com.travelingtunes.app.core.model.ArtAlignmentPortrait.MIDDLE -> Alignment.Center
            com.travelingtunes.app.core.model.ArtAlignmentPortrait.BOTTOM -> Alignment.BottomCenter
        }
    }

    val layoutModifier = when (displaySettings.artDisplayLayout) {
        ArtLayoutOption.OVERLAY, ArtLayoutOption.BACKGROUND -> Modifier.fillMaxSize()
        ArtLayoutOption.SPLIT -> Modifier.fillMaxWidth().fillMaxHeight(0.5f)
    }

    Box(
        modifier = modifier
            .then(layoutModifier)
            .then(
                if (displaySettings.albumArtScale == ArtScaleOption.ASPECT_FIT && dominantBgColor != null) {
                    Modifier.background(dominantBgColor!!.copy(alpha = displaySettings.albumArtFade.coerceIn(0.1f, 1.0f)))
                } else Modifier
            )
    ) {
        Image(
            bitmap = imgBitmap,
            contentDescription = "Album Art Background",
            contentScale = contentScale,
            alignment = imageAlignment,
            modifier = Modifier
                .fillMaxSize()
                .alpha(displaySettings.albumArtFade.coerceIn(0.1f, 1.0f))
        )
    }
}

@Composable
fun VolumeHudOverlay(
    volumeRatio: Float,
    displaySettings: DisplaySettings,
    modifier: Modifier = Modifier
) {
    if (displaySettings.hudType == HudTypeOption.NONE) return

    var isVisible by remember { mutableStateOf(displaySettings.volumeAlwaysOn) }

    LaunchedEffect(volumeRatio, displaySettings.volumeAlwaysOn) {
        if (displaySettings.volumeAlwaysOn) {
            isVisible = true
        } else {
            isVisible = true
            delay(2000L)
            isVisible = false
        }
    }

    if (!isVisible) return

    val primaryColor = MaterialTheme.colorScheme.primary
    val lineThicknessDp = displaySettings.hudLineThickness.dp

    when (displaySettings.hudType) {
        HudTypeOption.EDGE_HUD -> {
            // Geometric Vertical Strip along right edge
            BoxWithConstraints(modifier = modifier.fillMaxSize()) {
                val filledHeight = maxHeight * volumeRatio.coerceIn(0.01f, 1f)
                Box(
                    modifier = Modifier
                        .width(lineThicknessDp)
                        .height(filledHeight)
                        .align(Alignment.BottomEnd)
                        .background(primaryColor.copy(alpha = 0.50f))
                )
            }
        }
        HudTypeOption.NUMBER -> {
            // Horizontal geometric line indicator at height corresponding to volume level
            BoxWithConstraints(modifier = modifier.fillMaxSize()) {
                val topOffsetDp = (maxHeight - lineThicknessDp) * (1f - volumeRatio.coerceIn(0f, 1f))
                Box(
                    modifier = Modifier
                        .offset(y = topOffsetDp)
                        .fillMaxWidth()
                        .height(lineThicknessDp)
                        .background(primaryColor.copy(alpha = 0.70f))
                )
            }
        }
        HudTypeOption.BAR_VOLUME -> {
            // Full width rectangular block filling from bottom to current volume level
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .fillMaxHeight(volumeRatio.coerceIn(0.01f, 1f))
                    .background(primaryColor.copy(alpha = 0.20f))
            )
        }
        HudTypeOption.NONE -> {}
    }
}

@Composable
fun ProgressHudOverlay(
    currentPositionMs: Long,
    durationMs: Long,
    displaySettings: DisplaySettings,
    modifier: Modifier = Modifier
) {
    if (displaySettings.scrubHudType == ScrubHudTypeOption.NONE || durationMs <= 0L) return

    val progressRatio = (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    val primaryColor = MaterialTheme.colorScheme.primary
    val lineThicknessDp = displaySettings.hudLineThickness.dp

    when (displaySettings.scrubHudType) {
        ScrubHudTypeOption.EDGE_HUD -> {
            // Geometric Edge/Bottom Progress Bar
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(lineThicknessDp)
                    .background(primaryColor.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressRatio)
                        .background(primaryColor.copy(alpha = 0.85f))
                )
            }
        }
        ScrubHudTypeOption.POPUP -> {
            // Moving Geometric Vertical Line Indicator
            BoxWithConstraints(modifier = modifier.fillMaxSize()) {
                val leftOffsetDp = (maxWidth - lineThicknessDp) * progressRatio
                Box(
                    modifier = Modifier
                        .offset(x = leftOffsetDp)
                        .fillMaxHeight()
                        .width(lineThicknessDp)
                        .background(primaryColor.copy(alpha = 0.85f))
                )
            }
        }
        ScrubHudTypeOption.BAR_PROGRESS -> {
            // Translucent Rectangular Fill Block
            Box(
                modifier = modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progressRatio)
                    .background(primaryColor.copy(alpha = 0.15f))
            )
        }
        ScrubHudTypeOption.NONE -> {}
    }
}

private fun handleGestureAction(
    action: GestureAction,
    playbackManager: PlaybackManager,
    onOpenSongPicker: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenQuickStart: () -> Unit,
    onOpenContacts: () -> Unit
) {
    when (action) {
        GestureAction.PLAY_PAUSE -> playbackManager.togglePlayPause()
        GestureAction.PLAY -> playbackManager.play()
        GestureAction.PAUSE -> playbackManager.pause()
        GestureAction.NEXT -> playbackManager.next()
        GestureAction.PREVIOUS -> playbackManager.previous()
        GestureAction.RESTART -> playbackManager.restart()
        GestureAction.RESTART_PREVIOUS -> playbackManager.restartOrPrevious()
        GestureAction.FAST_FORWARD -> playbackManager.fastForward()
        GestureAction.REWIND -> playbackManager.rewind()
        GestureAction.VOLUME_UP -> playbackManager.increaseVolume()
        GestureAction.VOLUME_DOWN -> playbackManager.decreaseVolume()
        GestureAction.TOGGLE_REPEAT -> playbackManager.toggleRepeat()
        GestureAction.TOGGLE_SHUFFLE -> playbackManager.toggleShuffle()
        GestureAction.INCREASE_RATING -> playbackManager.increaseRating()
        GestureAction.DECREASE_RATING -> playbackManager.decreaseRating()
        GestureAction.SONG_PICKER -> onOpenSongPicker()
        GestureAction.MENU -> onOpenSettings()
        GestureAction.SHOW_QUICK_START -> onOpenQuickStart()
        GestureAction.PLAY_CURRENT_ALBUM -> playbackManager.playCurrentAlbum()
        GestureAction.PLAY_CURRENT_ARTIST -> playbackManager.playCurrentArtist()
        GestureAction.UNASSIGNED -> {}
        else -> playbackManager.showHudAction(action.displayName)
    }
}

private fun TextAlignmentOption.toComposeAlignment(): TextAlign {
    return when (this) {
        TextAlignmentOption.LEFT -> TextAlign.Left
        TextAlignmentOption.CENTER -> TextAlign.Center
        TextAlignmentOption.RIGHT -> TextAlign.Right
    }
}

@Composable
fun SongLabelsLayout(
    artist: String,
    title: String,
    album: String,
    displaySettings: DisplaySettings,
    modifier: Modifier = Modifier
) {
    val minFontSize = displaySettings.minimumFontSize.coerceAtLeast(12f)
    var scaleFactor by remember(artist, title, album, displaySettings) {
        mutableStateOf(1.0f)
    }

    val artistFontSize = (displaySettings.artistFontSize * scaleFactor).coerceAtLeast(minFontSize).sp
    val songFontSize = (displaySettings.songFontSize * scaleFactor).coerceAtLeast(minFontSize).sp
    val albumFontSize = (displaySettings.albumFontSize * scaleFactor).coerceAtLeast(minFontSize).sp

    val artistLineHeight = (artistFontSize.value * 1.25f).sp
    val songLineHeight = (songFontSize.value * 1.25f).sp
    val albumLineHeight = (albumFontSize.value * 1.25f).sp

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Artist Name Label
        Text(
            text = artist,
            fontSize = artistFontSize,
            lineHeight = artistLineHeight,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            textAlign = displaySettings.artistAlignment.toComposeAlignment(),
            onTextLayout = { result ->
                if (result.didOverflowHeight && scaleFactor > (minFontSize / displaySettings.artistFontSize.coerceAtLeast(1f))) {
                    scaleFactor = (scaleFactor * 0.9f).coerceAtLeast(minFontSize / displaySettings.artistFontSize.coerceAtLeast(1f))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .then(if (displaySettings.titleScrollLong) Modifier.basicMarquee() else Modifier)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Song Title Label
        Text(
            text = title,
            fontSize = songFontSize,
            lineHeight = songLineHeight,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            textAlign = displaySettings.songAlignment.toComposeAlignment(),
            onTextLayout = { result ->
                if (result.didOverflowHeight && scaleFactor > (minFontSize / displaySettings.songFontSize.coerceAtLeast(1f))) {
                    scaleFactor = (scaleFactor * 0.9f).coerceAtLeast(minFontSize / displaySettings.songFontSize.coerceAtLeast(1f))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .then(if (displaySettings.titleScrollLong) Modifier.basicMarquee() else Modifier)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Album Name Label
        Text(
            text = album,
            fontSize = albumFontSize,
            lineHeight = albumLineHeight,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Normal,
            textAlign = displaySettings.albumAlignment.toComposeAlignment(),
            onTextLayout = { result ->
                if (result.didOverflowHeight && scaleFactor > (minFontSize / displaySettings.albumFontSize.coerceAtLeast(1f))) {
                    scaleFactor = (scaleFactor * 0.9f).coerceAtLeast(minFontSize / displaySettings.albumFontSize.coerceAtLeast(1f))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .then(if (displaySettings.titleScrollLong) Modifier.basicMarquee() else Modifier)
        )
    }
}

@Composable
fun RepeatOptionsDialog(
    currentRepeatMode: RepeatMode,
    onSelectRepeatMode: (RepeatMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Repeat Options", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                RepeatMode.entries.forEach { mode ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectRepeatMode(mode)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                    ) {
                        ActionIcon(
                            action = GestureAction.TOGGLE_REPEAT,
                            repeatMode = mode,
                            iconSize = 28.dp,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = mode.displayName,
                            fontWeight = if (mode == currentRepeatMode) FontWeight.Bold else FontWeight.Normal,
                            color = if (mode == currentRepeatMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (mode == currentRepeatMode) {
                            Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ShuffleOptionsDialog(
    currentShuffleMode: ShuffleMode,
    onSelectShuffleMode: (ShuffleMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Shuffle Options", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                ShuffleMode.entries.forEach { mode ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectShuffleMode(mode)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                    ) {
                        ActionIcon(
                            action = GestureAction.TOGGLE_SHUFFLE,
                            shuffleMode = mode,
                            iconSize = 28.dp,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = mode.displayName,
                            fontWeight = if (mode == currentShuffleMode) FontWeight.Bold else FontWeight.Normal,
                            color = if (mode == currentShuffleMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (mode == currentShuffleMode) {
                            Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

suspend fun loadSongArtwork(context: android.content.Context, song: Song): android.graphics.Bitmap? = withContext(Dispatchers.IO) {
    // 1. Try file scheme if song.artworkUri is a file:// URI
    if (song.artworkUri != null && song.artworkUri.scheme == "file") {
        try {
            val bmp = BitmapFactory.decodeFile(song.artworkUri.path)
            if (bmp != null) return@withContext bmp
        } catch (ignored: Exception) {}
    }

    // 2. Try ContentResolver.loadThumbnail (Android 10+ / API 29+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        try {
            val bmp = context.contentResolver.loadThumbnail(song.contentUri, Size(1024, 1024), null)
            if (bmp != null) return@withContext bmp
        } catch (ignored: Exception) {}
    }

    // 3. Try MediaMetadataRetriever on song.contentUri
    val mmr = MediaMetadataRetriever()
    try {
        context.contentResolver.openFileDescriptor(song.contentUri, "r")?.use { pfd ->
            mmr.setDataSource(pfd.fileDescriptor)
        } ?: mmr.setDataSource(context, song.contentUri)

        val bytes = mmr.embeddedPicture
        if (bytes != null) {
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bmp != null) return@withContext bmp
        }
    } catch (ignored: Exception) {
    } finally {
        try { mmr.release() } catch (ignored: Exception) {}
    }

    // 4. Try ContentResolver openInputStream on artworkUri
    if (song.artworkUri != null) {
        try {
            context.contentResolver.openInputStream(song.artworkUri)?.use { stream ->
                val bmp = BitmapFactory.decodeStream(stream)
                if (bmp != null) return@withContext bmp
            }
        } catch (ignored: Exception) {}
    }

    null
}
