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
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.travelingtunes.app.core.model.GestureTrigger
import com.travelingtunes.app.core.model.HudTypeOption
import com.travelingtunes.app.core.model.ScrubHudTypeOption
import com.travelingtunes.app.core.model.Song
import com.travelingtunes.app.core.model.TextAlignmentOption
import com.travelingtunes.app.core.model.ThemeSettings
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
    themeSettings: ThemeSettings = ThemeSettings(),
    showFirstRunPrompt: Boolean = false,
    onDismissFirstRunPrompt: () -> Unit = {},
    onPickMusicFolder: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenQuickStart: () -> Unit,
    onOpenContacts: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val currentSong by playbackManager.currentSong.collectAsState()
    val currentPlaylist by playbackManager.currentPlaylist.collectAsState()
    val currentPositionMs by playbackManager.currentPositionMs.collectAsState()
    val durationMs by playbackManager.durationMs.collectAsState()
    val currentVolumeRatio by playbackManager.currentVolumeRatio.collectAsState()
    val actionHudText by playbackManager.actionHudText.collectAsState()

    var showSongPicker by remember { mutableStateOf(false) }
    var dragOffsetPx by remember { mutableStateOf(0f) }

    val pageCount = currentPlaylist.size.coerceAtLeast(1)
    val songIndex = currentPlaylist.indexOfFirst { it.id == currentSong?.id }.coerceAtLeast(0)

    val pagerState = rememberPagerState(initialPage = songIndex) { pageCount }
    val coroutineScope = rememberCoroutineScope()

    // Sync pagerState -> PlaybackManager when user swipes pager
    LaunchedEffect(pagerState.currentPage) {
        if (currentPlaylist.isNotEmpty() && pagerState.currentPage in currentPlaylist.indices) {
            val selectedSong = currentPlaylist[pagerState.currentPage]
            if (selectedSong.id != currentSong?.id) {
                playbackManager.playSongAtIndex(pagerState.currentPage)
            }
        }
    }

    // Sync PlaybackManager -> pagerState when song changes externally
    LaunchedEffect(currentSong?.id) {
        if (songIndex in 0 until pageCount && pagerState.currentPage != songIndex) {
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

    val gestureListener = object : GestureEventListener {
        override fun onGestureTriggered(trigger: GestureTrigger) {
            val binding = gestureBindings[trigger] ?: return
            when (binding.action) {
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
                        action = binding.action,
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
                GestureAction.NEXT, GestureAction.PREVIOUS -> {
                    dragOffsetPx += deltaX
                }
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
            val threshold = screenWidthPx * 0.15f
            if (dragOffsetPx != 0f) {
                if (dragOffsetPx < -threshold) {
                    coroutineScope.launch {
                        dragOffsetPx = 0f
                        if (pagerState.currentPage < pageCount - 1) {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        } else {
                            playbackManager.next()
                        }
                    }
                } else if (dragOffsetPx > threshold) {
                    coroutineScope.launch {
                        dragOffsetPx = 0f
                        if (pagerState.currentPage > 0) {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        } else {
                            playbackManager.previous()
                        }
                    }
                } else {
                    dragOffsetPx = 0f
                }
            } else if (kotlin.math.abs(totalDy) > 20f) {
                val volPct = (playbackManager.currentVolumeRatio.value * 100).toInt()
                playbackManager.showHudAction("Volume: $volPct%")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Sliding Page Transition (Album Art + Song Titles & Labels)
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 3,
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(dragOffsetPx.roundToInt(), 0) }
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
                    currentSong = pageSong,
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
            themeSettings = themeSettings,
            modifier = Modifier.align(Alignment.BottomStart)
        )

        // 4. Geometric Progress / Playback Bar Overlay (Edge Bar / Line)
        ProgressHudOverlay(
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            displaySettings = displaySettings,
            themeSettings = themeSettings,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // 5. Top Navigation Buttons (Song Picker & Settings)
        val settingsButtonShape = if (themeSettings.isRounded) CircleShape else RoundedCornerShape(8.dp)
        val settingsButtonGlassModifier = if (themeSettings.isGlass) {
            Modifier.border(1.dp, Color.White.copy(alpha = 0.40f), settingsButtonShape)
        } else Modifier

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { showSongPicker = true },
                modifier = Modifier
                    .clip(settingsButtonShape)
                    .background(
                        if (themeSettings.isGlass) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                        else Color.Transparent
                    )
                    .then(settingsButtonGlassModifier)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = "Song Picker",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .clip(settingsButtonShape)
                    .background(
                        if (themeSettings.isGlass) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                        else Color.Transparent
                    )
                    .then(settingsButtonGlassModifier)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // 6. Action HUD Banner Overlay
        AnimatedVisibility(
            visible = actionHudText != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            actionHudText?.let { text ->
                val bannerShape = if (themeSettings.isRounded) RoundedCornerShape(24.dp) else RectangleShape
                val bannerGlassModifier = if (themeSettings.isGlass) {
                    Modifier.border(1.dp, Color.White.copy(alpha = 0.45f), bannerShape)
                } else Modifier

                Box(
                    modifier = Modifier
                        .clip(bannerShape)
                        .background(
                            if (themeSettings.isGlass) Color.Black.copy(alpha = 0.45f)
                            else Color.Black.copy(alpha = 0.80f)
                        )
                        .then(bannerGlassModifier)
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
    }
}

@Composable
fun SongLabelsLayout(
    currentSong: Song?,
    displaySettings: DisplaySettings,
    modifier: Modifier = Modifier
) {
    val artistFont = com.travelingtunes.app.core.theme.FontHelper.getFontFamily(displaySettings.artistFontKey)
    val songFont = com.travelingtunes.app.core.theme.FontHelper.getFontFamily(displaySettings.songFontKey)
    val albumFont = com.travelingtunes.app.core.theme.FontHelper.getFontFamily(displaySettings.albumFontKey)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 48.dp)
    ) {
        // Artist Name Label
        Text(
            text = currentSong?.artist ?: "Traveling Tunes",
            fontSize = displaySettings.artistFontSize.sp,
            fontFamily = artistFont,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            textAlign = displaySettings.artistAlignment.toComposeAlignment(),
            modifier = Modifier
                .fillMaxWidth()
                .then(if (displaySettings.titleScrollLong) Modifier.basicMarquee() else Modifier)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Song Title Label
        Text(
            text = currentSong?.title ?: "Swipe or Tap Screen to Play",
            fontSize = displaySettings.songFontSize.sp,
            fontFamily = songFont,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            textAlign = displaySettings.songAlignment.toComposeAlignment(),
            modifier = Modifier
                .fillMaxWidth()
                .then(if (displaySettings.titleScrollLong) Modifier.basicMarquee() else Modifier)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Album Name Label
        Text(
            text = currentSong?.album ?: "No Song Selected",
            fontSize = displaySettings.albumFontSize.sp,
            fontFamily = albumFont,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Normal,
            textAlign = displaySettings.albumAlignment.toComposeAlignment(),
            modifier = Modifier
                .fillMaxWidth()
                .then(if (displaySettings.titleScrollLong) Modifier.basicMarquee() else Modifier)
        )
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
    var bitmap by remember(song.id, song.artworkUri) {
        mutableStateOf(AlbumArtCache.instance.get(song.id))
    }

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

    val imgBitmap = bitmap ?: return

    val contentScale = when (displaySettings.albumArtScale) {
        ArtScaleOption.FILL_SCREEN -> ContentScale.Crop
        ArtScaleOption.ASPECT_FIT -> ContentScale.Fit
        ArtScaleOption.ASPECT_FILL -> ContentScale.Crop
    }

    val layoutModifier = when (displaySettings.artDisplayLayout) {
        ArtLayoutOption.OVERLAY -> Modifier.fillMaxSize()
        ArtLayoutOption.DOCKED -> Modifier.fillMaxWidth().fillMaxHeight(0.5f)
    }

    Box(modifier = modifier.then(layoutModifier)) {
        Image(
            bitmap = imgBitmap,
            contentDescription = "Album Art Background",
            contentScale = contentScale,
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
    themeSettings: ThemeSettings = ThemeSettings(),
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

    val shape = if (themeSettings.isRounded) RoundedCornerShape(lineThicknessDp / 2f) else RectangleShape
    val glassModifier = if (themeSettings.isGlass) {
        Modifier.border(1.dp, Color.White.copy(alpha = 0.45f), shape)
    } else Modifier

    when (displaySettings.hudType) {
        HudTypeOption.EDGE_HUD -> {
            // Geometric Vertical Strip along right edge
            BoxWithConstraints(modifier = modifier.fillMaxSize()) {
                val filledHeight = this.maxHeight * volumeRatio.coerceIn(0.01f, 1f)
                Box(
                    modifier = Modifier
                        .width(lineThicknessDp)
                        .height(filledHeight)
                        .align(Alignment.BottomEnd)
                        .clip(shape)
                        .background(
                            if (themeSettings.isGlass) primaryColor.copy(alpha = 0.35f)
                            else primaryColor.copy(alpha = 0.50f)
                        )
                        .then(glassModifier)
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
                        .then(if (themeSettings.isRounded) Modifier.padding(horizontal = 12.dp) else Modifier)
                        .fillMaxWidth()
                        .height(lineThicknessDp)
                        .clip(shape)
                        .background(
                            if (themeSettings.isGlass) primaryColor.copy(alpha = 0.35f)
                            else primaryColor.copy(alpha = 0.70f)
                        )
                        .then(glassModifier)
                )
            }
        }
        HudTypeOption.BAR_VOLUME -> {
            // Full width rectangular block filling from bottom to current volume level
            val barShape = if (themeSettings.isRounded) RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp) else RectangleShape
            val barGlassModifier = if (themeSettings.isGlass) {
                Modifier.border(1.dp, Color.White.copy(alpha = 0.35f), barShape)
            } else Modifier
            val barPadding = if (themeSettings.isRounded) Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp) else Modifier

            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .fillMaxHeight(volumeRatio.coerceIn(0.01f, 1f))
                    .then(barPadding)
                    .clip(barShape)
                    .background(
                        if (themeSettings.isGlass) primaryColor.copy(alpha = 0.25f)
                        else primaryColor.copy(alpha = 0.20f)
                    )
                    .then(barGlassModifier)
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
    themeSettings: ThemeSettings = ThemeSettings(),
    modifier: Modifier = Modifier
) {
    if (displaySettings.scrubHudType == ScrubHudTypeOption.NONE || durationMs <= 0L) return

    val progressRatio = (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    val primaryColor = MaterialTheme.colorScheme.primary
    val lineThicknessDp = displaySettings.hudLineThickness.dp

    val shape = if (themeSettings.isRounded) RoundedCornerShape(lineThicknessDp / 2f) else RectangleShape
    val glassModifier = if (themeSettings.isGlass) {
        Modifier.border(1.dp, Color.White.copy(alpha = 0.45f), shape)
    } else Modifier
    val roundedPadding = if (themeSettings.isRounded) Modifier.padding(horizontal = 12.dp, vertical = 8.dp) else Modifier

    when (displaySettings.scrubHudType) {
        ScrubHudTypeOption.EDGE_HUD -> {
            // Geometric Edge/Bottom Progress Bar
            Box(
                modifier = modifier
                    .then(roundedPadding)
                    .fillMaxWidth()
                    .height(lineThicknessDp)
                    .clip(shape)
                    .background(
                        if (themeSettings.isGlass) primaryColor.copy(alpha = 0.15f)
                        else primaryColor.copy(alpha = 0.15f)
                    )
                    .then(glassModifier)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressRatio)
                        .clip(shape)
                        .background(
                            if (themeSettings.isGlass) primaryColor.copy(alpha = 0.50f)
                            else primaryColor.copy(alpha = 0.85f)
                        )
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
                        .then(if (themeSettings.isRounded) Modifier.padding(vertical = 12.dp) else Modifier)
                        .fillMaxHeight()
                        .width(lineThicknessDp)
                        .clip(shape)
                        .background(
                            if (themeSettings.isGlass) primaryColor.copy(alpha = 0.50f)
                            else primaryColor.copy(alpha = 0.85f)
                        )
                        .then(glassModifier)
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
