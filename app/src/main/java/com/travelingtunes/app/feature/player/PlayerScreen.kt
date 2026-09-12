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
import androidx.compose.ui.graphics.asAndroidBitmap
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
import com.travelingtunes.app.core.datastore.SettingsDataStore
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
import com.travelingtunes.app.core.model.ThemeSettings
import com.travelingtunes.app.feature.songpicker.SongPickerBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private fun resolveGestureBinding(
    trigger: GestureTrigger,
    gestureBindings: Map<GestureTrigger, GestureBinding>
): GestureBinding {
    return gestureBindings[trigger] ?: GestureBinding(
        trigger = trigger,
        action = GestureAction.fromKey(trigger.defaultActionKey),
        isContinuous = trigger.isContinuousDefault
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerScreen(
    playbackManager: PlaybackManager,
    musicDatabase: MusicDatabase,
    displaySettings: DisplaySettings,
    gestureBindings: Map<GestureTrigger, GestureBinding>,
    musicScanner: MusicScanner? = null,
    settingsDataStore: SettingsDataStore? = null,
    themeSettings: ThemeSettings = ThemeSettings(),
    showFirstRunPrompt: Boolean = false,
    onDismissFirstRunPrompt: () -> Unit = {},
    onPickMusicFolder: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenQuickStart: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val window = (context as? android.app.Activity)?.window
    if (window != null) {
        androidx.compose.runtime.DisposableEffect(displaySettings.immersiveMode) {
            val insetsController = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
            if (displaySettings.immersiveMode) {
                insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            }
            onDispose {
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            }
        }
    }

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

    val gestureListener = object : GestureEventListener {
        override fun onGestureTriggered(trigger: GestureTrigger) {
            var binding = resolveGestureBinding(trigger, gestureBindings)
            var action = binding.action

            // When a touch region is unassigned, pass the tap through to the standard tap action
            if (action == GestureAction.UNASSIGNED && trigger.category == GestureCategory.SCREEN_REGION) {
                val fallbackTrigger = GestureTrigger.TAP_1_1
                binding = resolveGestureBinding(fallbackTrigger, gestureBindings)
                action = binding.action
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
                    playbackManager.next()
                }
                GestureAction.PREVIOUS -> {
                    playbackManager.previous()
                }
                else -> {
                    handleGestureAction(
                        action = action,
                        playbackManager = playbackManager,
                        onOpenSongPicker = { showSongPicker = true },
                        onOpenSettings = onOpenSettings,
                        onOpenQuickStart = onOpenQuickStart
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
            val binding = resolveGestureBinding(trigger, gestureBindings)
            val action = binding.action

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Sliding Page Transition (Album Art + Song Titles & Labels)
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val pageSong = currentPlaylist.getOrNull(page) ?: currentSong

            val hasTopButtons = listOf(
                GestureTrigger.CORNER_TOP_LEFT,
                GestureTrigger.CORNER_TOP_CENTER,
                GestureTrigger.CORNER_TOP_RIGHT
            ).any { resolveGestureBinding(it, gestureBindings).action != GestureAction.UNASSIGNED }

            val hasBottomButtons = listOf(
                GestureTrigger.CORNER_BOTTOM_LEFT,
                GestureTrigger.CORNER_BOTTOM_CENTER,
                GestureTrigger.CORNER_BOTTOM_RIGHT
            ).any { resolveGestureBinding(it, gestureBindings).action != GestureAction.UNASSIGNED }

            PlayerPageContent(
                pageSong = pageSong,
                displaySettings = displaySettings,
                hasTopButtons = hasTopButtons,
                hasBottomButtons = hasBottomButtons
            )
        }

        // 2. Gesture Detector Touch Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .travelingTunesGestures(gestureListener, gestureBindings)
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
            onShowRepeatOptions = { showRepeatOptionsDialog = true },
            onShowShuffleOptions = { showShuffleOptionsDialog = true }
        )

        /* Action HUD Banner Overlay disabled per user requirement (no pop-up announcements needed)
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
        */

        // 7. Song Picker Sheet
        if (showSongPicker) {
            SongPickerBottomSheet(
                musicDatabase = musicDatabase,
                playbackManager = playbackManager,
                musicScanner = musicScanner,
                settingsDataStore = settingsDataStore,
                onDismiss = { showSongPicker = false }
            )
        }

        // 8. Repeat & Shuffle Options Dialogs
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
fun PlayerPageContent(
    pageSong: Song?,
    displaySettings: DisplaySettings,
    hasTopButtons: Boolean,
    hasBottomButtons: Boolean
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val isDocked = displaySettings.artDisplayLayout == ArtLayoutOption.DOCKED

    if (isDocked && displaySettings.showAlbumArt && pageSong != null) {
        if (isLandscape) {
            val isDockedRight = displaySettings.artAlignmentLandscape == com.travelingtunes.app.core.model.ArtAlignmentLandscape.RIGHT
            Row(modifier = Modifier.fillMaxSize()) {
                if (isDockedRight) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        SongLabelsLayout(
                            currentSong = pageSong,
                            displaySettings = displaySettings,
                            hasTopButtons = hasTopButtons,
                            hasBottomButtons = hasBottomButtons
                        )
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        PlayerAlbumArtBackground(
                            song = pageSong,
                            displaySettings = displaySettings
                        )
                    }
                } else {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        PlayerAlbumArtBackground(
                            song = pageSong,
                            displaySettings = displaySettings
                        )
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        SongLabelsLayout(
                            currentSong = pageSong,
                            displaySettings = displaySettings,
                            hasTopButtons = hasTopButtons,
                            hasBottomButtons = hasBottomButtons
                        )
                    }
                }
            }
        } else {
            val isDockedBottom = displaySettings.artAlignmentPortrait == com.travelingtunes.app.core.model.ArtAlignmentPortrait.BOTTOM
            Column(modifier = Modifier.fillMaxSize()) {
                if (isDockedBottom) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        SongLabelsLayout(
                            currentSong = pageSong,
                            displaySettings = displaySettings,
                            hasTopButtons = hasTopButtons,
                            hasBottomButtons = hasBottomButtons
                        )
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        PlayerAlbumArtBackground(
                            song = pageSong,
                            displaySettings = displaySettings
                        )
                    }
                } else {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        PlayerAlbumArtBackground(
                            song = pageSong,
                            displaySettings = displaySettings
                        )
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        SongLabelsLayout(
                            currentSong = pageSong,
                            displaySettings = displaySettings,
                            hasTopButtons = hasTopButtons,
                            hasBottomButtons = hasBottomButtons
                        )
                    }
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            PlayerAlbumArtBackground(
                song = pageSong,
                displaySettings = displaySettings
            )

            SongLabelsLayout(
                currentSong = pageSong,
                displaySettings = displaySettings,
                hasTopButtons = hasTopButtons,
                hasBottomButtons = hasBottomButtons
            )
        }
    }
}

@Composable
fun SongLabelsLayout(
    currentSong: Song?,
    displaySettings: DisplaySettings,
    hasTopButtons: Boolean = true,
    hasBottomButtons: Boolean = true,
    modifier: Modifier = Modifier
) {
    val artistFont = com.travelingtunes.app.core.theme.FontHelper.getFontFamily(displaySettings.artistFontKey)
    val songFont = com.travelingtunes.app.core.theme.FontHelper.getFontFamily(displaySettings.songFontKey)
    val albumFont = com.travelingtunes.app.core.theme.FontHelper.getFontFamily(displaySettings.albumFontKey)

    val topPadding = if (hasTopButtons) 84.dp else 32.dp
    val bottomPadding = if (hasBottomButtons) 88.dp else 32.dp

    val minFontSize = displaySettings.minimumFontSize.coerceAtLeast(12f)
    var scaleFactor by remember(currentSong?.id, displaySettings) {
        mutableStateOf(1.0f)
    }

    val artistFontSize = (displaySettings.artistFontSize * scaleFactor).coerceAtLeast(minFontSize).sp
    val songFontSize = (displaySettings.songFontSize * scaleFactor).coerceAtLeast(minFontSize).sp
    val albumFontSize = (displaySettings.albumFontSize * scaleFactor).coerceAtLeast(minFontSize).sp

    val artistLineHeight = (artistFontSize.value * 1.35f).sp
    val songLineHeight = (songFontSize.value * 1.35f).sp
    val albumLineHeight = (albumFontSize.value * 1.35f).sp

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = 24.dp,
                end = 24.dp,
                top = topPadding,
                bottom = bottomPadding
            ),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Artist Name Label
        Text(
            text = currentSong?.artist ?: "Traveling Tunes",
            fontSize = artistFontSize,
            lineHeight = artistLineHeight,
            fontFamily = artistFont,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Medium,
            textAlign = displaySettings.artistAlignment.toComposeAlignment(),
            onTextLayout = { result ->
                if (result.didOverflowHeight && scaleFactor > (minFontSize / displaySettings.artistFontSize.coerceAtLeast(1f))) {
                    scaleFactor = (scaleFactor * 0.88f).coerceAtLeast(minFontSize / displaySettings.artistFontSize.coerceAtLeast(1f))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .then(if (displaySettings.titleScrollLong) Modifier.basicMarquee() else Modifier)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Song Title Label
        Text(
            text = currentSong?.title ?: "Swipe or Tap Screen to Play",
            fontSize = songFontSize,
            lineHeight = songLineHeight,
            fontFamily = songFont,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            textAlign = displaySettings.songAlignment.toComposeAlignment(),
            onTextLayout = { result ->
                if (result.didOverflowHeight && scaleFactor > (minFontSize / displaySettings.songFontSize.coerceAtLeast(1f))) {
                    scaleFactor = (scaleFactor * 0.88f).coerceAtLeast(minFontSize / displaySettings.songFontSize.coerceAtLeast(1f))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .then(if (displaySettings.titleScrollLong) Modifier.basicMarquee() else Modifier)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Album Name Label
        Text(
            text = currentSong?.album ?: "No Song Selected",
            fontSize = albumFontSize,
            lineHeight = albumLineHeight,
            fontFamily = albumFont,
            color = MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.Normal,
            textAlign = displaySettings.albumAlignment.toComposeAlignment(),
            onTextLayout = { result ->
                if (result.didOverflowHeight && scaleFactor > (minFontSize / displaySettings.albumFontSize.coerceAtLeast(1f))) {
                    scaleFactor = (scaleFactor * 0.88f).coerceAtLeast(minFontSize / displaySettings.albumFontSize.coerceAtLeast(1f))
                }
            },
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
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

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
    }

    val imageAlignment = if (isLandscape) {
        when (displaySettings.albumArtScale) {
            com.travelingtunes.app.core.model.ArtScaleOption.FILL_SCREEN -> {
                when (displaySettings.artAlignmentLandscape) {
                    com.travelingtunes.app.core.model.ArtAlignmentLandscape.TOP -> Alignment.TopCenter
                    com.travelingtunes.app.core.model.ArtAlignmentLandscape.MIDDLE -> Alignment.Center
                    com.travelingtunes.app.core.model.ArtAlignmentLandscape.BOTTOM -> Alignment.BottomCenter
                    com.travelingtunes.app.core.model.ArtAlignmentLandscape.LEFT -> Alignment.CenterStart
                    com.travelingtunes.app.core.model.ArtAlignmentLandscape.CENTER -> Alignment.Center
                    com.travelingtunes.app.core.model.ArtAlignmentLandscape.RIGHT -> Alignment.CenterEnd
                }
            }
            com.travelingtunes.app.core.model.ArtScaleOption.ASPECT_FIT -> {
                when (displaySettings.artAlignmentLandscape) {
                    com.travelingtunes.app.core.model.ArtAlignmentLandscape.LEFT -> Alignment.CenterStart
                    com.travelingtunes.app.core.model.ArtAlignmentLandscape.CENTER -> Alignment.Center
                    com.travelingtunes.app.core.model.ArtAlignmentLandscape.RIGHT -> Alignment.CenterEnd
                    com.travelingtunes.app.core.model.ArtAlignmentLandscape.TOP -> Alignment.TopCenter
                    com.travelingtunes.app.core.model.ArtAlignmentLandscape.MIDDLE -> Alignment.Center
                    com.travelingtunes.app.core.model.ArtAlignmentLandscape.BOTTOM -> Alignment.BottomCenter
                }
            }
        }
    } else {
        when (displaySettings.albumArtScale) {
            com.travelingtunes.app.core.model.ArtScaleOption.FILL_SCREEN -> {
                when (displaySettings.artAlignmentPortrait) {
                    com.travelingtunes.app.core.model.ArtAlignmentPortrait.LEFT -> Alignment.CenterStart
                    com.travelingtunes.app.core.model.ArtAlignmentPortrait.CENTER -> Alignment.Center
                    com.travelingtunes.app.core.model.ArtAlignmentPortrait.RIGHT -> Alignment.CenterEnd
                    com.travelingtunes.app.core.model.ArtAlignmentPortrait.TOP -> Alignment.TopCenter
                    com.travelingtunes.app.core.model.ArtAlignmentPortrait.MIDDLE -> Alignment.Center
                    com.travelingtunes.app.core.model.ArtAlignmentPortrait.BOTTOM -> Alignment.BottomCenter
                }
            }
            com.travelingtunes.app.core.model.ArtScaleOption.ASPECT_FIT -> {
                when (displaySettings.artAlignmentPortrait) {
                    com.travelingtunes.app.core.model.ArtAlignmentPortrait.TOP -> Alignment.TopCenter
                    com.travelingtunes.app.core.model.ArtAlignmentPortrait.MIDDLE -> Alignment.Center
                    com.travelingtunes.app.core.model.ArtAlignmentPortrait.BOTTOM -> Alignment.BottomCenter
                    com.travelingtunes.app.core.model.ArtAlignmentPortrait.LEFT -> Alignment.CenterStart
                    com.travelingtunes.app.core.model.ArtAlignmentPortrait.CENTER -> Alignment.Center
                    com.travelingtunes.app.core.model.ArtAlignmentPortrait.RIGHT -> Alignment.CenterEnd
                }
            }
        }
    }

    val letterboxBgColor = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (displaySettings.albumArtScale == ArtScaleOption.ASPECT_FIT) {
                    Modifier.background(letterboxBgColor.copy(alpha = displaySettings.albumArtFade.coerceIn(0.1f, 1.0f)))
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
                val topOffsetDp = (this.maxHeight - lineThicknessDp) * (1f - volumeRatio.coerceIn(0f, 1f))
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
    onOpenQuickStart: () -> Unit
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
        GestureAction.SHUFFLE_ALL_SONGS -> playbackManager.shuffleAllSongs()
        GestureAction.PLAY_CURRENT_ALBUM -> playbackManager.playCurrentAlbum()
        GestureAction.PLAY_CURRENT_ARTIST -> playbackManager.playCurrentArtist()
        GestureAction.INCREASE_RATING -> playbackManager.increaseRating()
        GestureAction.DECREASE_RATING -> playbackManager.decreaseRating()
        GestureAction.SONG_PICKER -> onOpenSongPicker()
        GestureAction.MENU -> onOpenSettings()
        GestureAction.SHOW_QUICK_START -> onOpenQuickStart()
        GestureAction.UNASSIGNED -> {}
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
            val binding = resolveGestureBinding(trigger, gestureBindings)
            val action = binding.action
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
                                    onOpenQuickStart = onOpenQuickStart
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
                                        onOpenQuickStart = onOpenQuickStart
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
