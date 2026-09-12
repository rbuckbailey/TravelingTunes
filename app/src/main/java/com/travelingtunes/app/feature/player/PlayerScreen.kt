package com.travelingtunes.app.feature.player

import android.content.ContentUris
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelingtunes.app.core.database.MusicDatabase
import androidx.core.net.toUri
import com.travelingtunes.app.core.database.LibraryStats
import com.travelingtunes.app.core.media.AlbumArtAuditReport
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
import com.travelingtunes.app.core.model.TitleRowType
import com.travelingtunes.app.core.theme.BalancedTitleText
import com.travelingtunes.app.core.theme.MondrianBackground
import com.travelingtunes.app.core.theme.MondrianMaskedLayout
import com.travelingtunes.app.core.model.SlideDirection
import com.travelingtunes.app.core.model.getSlideDirection
import com.travelingtunes.app.core.model.getReverseTrigger
import com.travelingtunes.app.core.ui.SlidingOverlay
import com.travelingtunes.app.feature.queue.QueueBottomSheet
import com.travelingtunes.app.feature.settings.DownloadedArtBrowserScreen
import com.travelingtunes.app.feature.settings.GestureAssignmentScreen
import com.travelingtunes.app.feature.settings.SettingsScreen
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
    musicFolderName: String? = null,
    lastScanTime: Long = 0L,
    libraryStats: LibraryStats = LibraryStats(),
    isScanning: Boolean = false,
    scanStatusMessage: String? = null,
    isDownloadingArt: Boolean = false,
    artDownloadStatusMessage: String? = null,
    artDownloadDownloadedCount: Int = 0,
    artDownloadFailedCount: Int = 0,
    artDownloadTotalCount: Int = 0,
    lastAuditReport: AlbumArtAuditReport? = null,
    onDismissFirstRunPrompt: () -> Unit = {},
    onPickMusicFolder: () -> Unit = {},
    onRescanMusicFolder: () -> Unit = {},
    onDownloadMissingArt: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenQuickStart: () -> Unit,
    onOpenGestureAssignments: () -> Unit = {},
    onOpenDownloadedArtBrowser: () -> Unit = {}
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
            onDispose { }
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
    var songPickerSlideDirection by remember { mutableStateOf(SlideDirection.BOTTOM) }
    var pickerOpeningTrigger by remember { mutableStateOf<GestureTrigger?>(null) }

    var showQueue by remember { mutableStateOf(false) }
    var queueSlideDirection by remember { mutableStateOf(SlideDirection.BOTTOM) }
    var queueOpeningTrigger by remember { mutableStateOf<GestureTrigger?>(null) }

    var showMenu by remember { mutableStateOf(false) }
    var menuSlideDirection by remember { mutableStateOf(SlideDirection.BOTTOM) }
    var menuOpeningTrigger by remember { mutableStateOf<GestureTrigger?>(null) }

    var showDownloadedArtBrowser by remember { mutableStateOf(false) }
    var showGestureAssignments by remember { mutableStateOf(false) }

    var showRepeatOptionsDialog by remember { mutableStateOf(false) }
    var showShuffleOptionsDialog by remember { mutableStateOf(false) }

    val activeIsScanning by musicScanner?.isScanning?.collectAsState()
        ?: remember(isScanning) { mutableStateOf(isScanning) }
    val activeScanStatusMessage by musicScanner?.statusMessage?.collectAsState()
        ?: remember(scanStatusMessage) { mutableStateOf(scanStatusMessage) }

    val activeIsDownloadingArt by musicScanner?.isDownloadingArt?.collectAsState()
        ?: remember(isDownloadingArt) { mutableStateOf(isDownloadingArt) }
    val activeArtDownloadStatusMessage by musicScanner?.artDownloadStatusMessage?.collectAsState()
        ?: remember(artDownloadStatusMessage) { mutableStateOf(artDownloadStatusMessage) }
    val activeArtDownloadDownloadedCount by musicScanner?.artDownloadDownloadedCount?.collectAsState()
        ?: remember(artDownloadDownloadedCount) { mutableStateOf(artDownloadDownloadedCount) }
    val activeArtDownloadFailedCount by musicScanner?.artDownloadFailedCount?.collectAsState()
        ?: remember(artDownloadFailedCount) { mutableStateOf(artDownloadFailedCount) }
    val activeArtDownloadTotalCount by musicScanner?.artDownloadTotalCount?.collectAsState()
        ?: remember(artDownloadTotalCount) { mutableStateOf(artDownloadTotalCount) }
    val activeLastAuditReport by musicScanner?.lastAuditReport?.collectAsState()
        ?: remember(lastAuditReport) { mutableStateOf(lastAuditReport) }

    val activeMusicFolderName by settingsDataStore?.musicFolderNameFlow?.collectAsState(initial = musicFolderName)
        ?: remember(musicFolderName) { mutableStateOf(musicFolderName) }
    val activeLastScanTime by settingsDataStore?.lastScanTimeFlow?.collectAsState(initial = lastScanTime)
        ?: remember(lastScanTime) { mutableStateOf(lastScanTime) }
    val activeMusicFolderUri by settingsDataStore?.musicFolderUriFlow?.collectAsState(initial = null)
        ?: remember { mutableStateOf(null) }

    var activeLibraryStats by remember { mutableStateOf(libraryStats) }
    LaunchedEffect(activeLastScanTime, activeIsScanning, currentPlaylist) {
        activeLibraryStats = musicDatabase.getLibraryStats()
    }

    fun openSongPicker(direction: SlideDirection = SlideDirection.BOTTOM, trigger: GestureTrigger? = null) {
        songPickerSlideDirection = direction
        pickerOpeningTrigger = trigger
        showSongPicker = true
    }

    fun openQueue(direction: SlideDirection = SlideDirection.BOTTOM, trigger: GestureTrigger? = null) {
        queueSlideDirection = direction
        queueOpeningTrigger = trigger
        showQueue = true
    }

    fun openMenu(direction: SlideDirection = SlideDirection.BOTTOM, trigger: GestureTrigger? = null) {
        menuSlideDirection = direction
        menuOpeningTrigger = trigger
        showMenu = true
    }

    val pageCount = currentPlaylist.size.coerceAtLeast(1)
    val songIndex = currentPlaylist.indexOfFirst { it.id == currentSong?.id }.coerceAtLeast(0)

    val pagerState = rememberPagerState(initialPage = songIndex) { pageCount }
    val coroutineScope = rememberCoroutineScope()

    val effectiveOnRescanMusicFolder: () -> Unit = remember(onRescanMusicFolder, activeMusicFolderUri, musicScanner, settingsDataStore, playbackManager, musicDatabase) {
        {
            if (!activeMusicFolderUri.isNullOrEmpty() && musicScanner != null && settingsDataStore != null) {
                coroutineScope.launch {
                    musicScanner.scanFolder(activeMusicFolderUri!!.toUri())
                    settingsDataStore.setLastScanTime(System.currentTimeMillis())
                    val scannedSongs = musicDatabase.getAllSongs()
                    if (scannedSongs.isNotEmpty()) {
                        playbackManager.setPlaylistAndPlay(scannedSongs, 0, shuffle = true)
                    }
                }
            } else {
                onRescanMusicFolder()
            }
            Unit
        }
    }

    val effectiveOnDownloadMissingArt: () -> Unit = remember(onDownloadMissingArt, musicScanner) {
        {
            if (musicScanner != null) {
                coroutineScope.launch {
                    musicScanner.downloadMissingArtwork()
                }
            } else {
                onDownloadMissingArt()
            }
            Unit
        }
    }

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
            pagerState.animateScrollToPage(
                page = songIndex,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            )
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
        override fun onGestureTriggered(trigger: GestureTrigger, isLongPress: Boolean): Boolean {
            var binding = resolveGestureBinding(trigger, gestureBindings)
            var action = binding.action

            val isAnyOverlayOpen = showSongPicker || showQueue || showMenu || showDownloadedArtBrowser || showGestureAssignments

            if (isAnyOverlayOpen) {
                val activeSlideDirection = when {
                    showSongPicker -> songPickerSlideDirection
                    showQueue -> queueSlideDirection
                    else -> menuSlideDirection
                }
                val activeOpeningTrigger = when {
                    showSongPicker -> pickerOpeningTrigger
                    showQueue -> queueOpeningTrigger
                    else -> menuOpeningTrigger
                }
                val activeAction = when {
                    showSongPicker -> GestureAction.SONG_PICKER
                    showQueue -> GestureAction.SHOW_QUEUE
                    else -> GestureAction.MENU
                }

                val isReverseAction = isReverseActionForOpenOverlay(
                    trigger = trigger,
                    action = action,
                    activeOpeningTrigger = activeOpeningTrigger,
                    activeSlideDirection = activeSlideDirection,
                    activeAction = activeAction
                )

                if (isReverseAction) {
                    if (showSongPicker) showSongPicker = false
                    if (showQueue) showQueue = false
                    if (showMenu) showMenu = false
                    if (showDownloadedArtBrowser) showDownloadedArtBrowser = false
                    if (showGestureAssignments) showGestureAssignments = false
                    return true
                } else {
                    return false
                }
            }

            // When a touch region is unassigned, pass the tap through to the standard tap action
            if (action == GestureAction.UNASSIGNED && trigger.category == GestureCategory.SCREEN_REGION) {
                val fallbackTrigger = GestureTrigger.TAP_1_1
                binding = resolveGestureBinding(fallbackTrigger, gestureBindings)
                action = binding.action
            }

            if (action == GestureAction.UNASSIGNED) return true

            val isCurrentSongDownloadedArt = musicScanner?.albumArtDownloader?.isDownloadedArtwork(currentSong) == true
            if (action == GestureAction.DELETE_DOWNLOADED_ART && !isCurrentSongDownloadedArt) {
                return true
            }

            val slideDir = trigger.getSlideDirection()

            if (isLongPress || trigger.category == GestureCategory.LONG_PRESS) {
                if (action == GestureAction.TOGGLE_REPEAT) {
                    showRepeatOptionsDialog = true
                } else if (action == GestureAction.TOGGLE_SHUFFLE) {
                    showShuffleOptionsDialog = true
                } else if (action == GestureAction.MENU) {
                    openMenu(slideDir, trigger)
                } else if (action == GestureAction.SHOW_QUICK_START) {
                    onOpenQuickStart()
                } else if (action == GestureAction.SONG_PICKER) {
                    openSongPicker(slideDir, trigger)
                } else if (action == GestureAction.SHOW_QUEUE) {
                    openQueue(slideDir, trigger)
                }
                return true
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
                        trigger = trigger,
                        playbackManager = playbackManager,
                        musicScanner = musicScanner,
                        musicDatabase = musicDatabase,
                        coroutineScope = coroutineScope,
                        onOpenSongPicker = { dir -> openSongPicker(dir, trigger) },
                        onOpenQueue = { dir -> openQueue(dir, trigger) },
                        onOpenSettings = { dir -> openMenu(dir, trigger) },
                        onOpenQuickStart = onOpenQuickStart
                    )
                }
            }
            return true
        }

        override fun onContinuousGesture(
            trigger: GestureTrigger,
            delta: Float,
            deltaX: Float,
            deltaY: Float,
            totalDx: Float,
            totalDy: Float
        ) {
            val isAnyOverlayOpen = showSongPicker || showQueue || showMenu || showDownloadedArtBrowser || showGestureAssignments
            if (isAnyOverlayOpen) return

            val binding = resolveGestureBinding(trigger, gestureBindings)
            val action = binding.action

            val msPerPixel = ((playbackManager.durationMs.value.coerceAtLeast(30000L)).toFloat() / screenWidthPx.coerceAtLeast(1f) * 0.5f).coerceIn(20f, 250f)

            when (action) {
                GestureAction.VOLUME_UP, GestureAction.VOLUME_DOWN -> {
                    playbackManager.adjustVolumeByDelta(deltaY, screenHeightPx)
                }
                GestureAction.FAST_FORWARD -> {
                    val deltaMs = (deltaX * msPerPixel).toLong()
                    playbackManager.seekByDelta(deltaMs)
                }
                GestureAction.REWIND -> {
                    val deltaMs = (deltaX * msPerPixel).toLong()
                    playbackManager.seekByDelta(deltaMs)
                }
                else -> {
                    // Actions not explicitly bound to Volume or Seek should not trigger continuous Volume or Seeking
                }
            }
        }

        override fun onGestureEnd(totalDx: Float, totalDy: Float, fingers: Int) {
            val isAnyOverlayOpen = showSongPicker || showQueue || showMenu || showDownloadedArtBrowser || showGestureAssignments
            if (!isAnyOverlayOpen) {
                playbackManager.persistCurrentPlaybackState()
            }
        }
    }

    val isMondrian = themeSettings.currentThemeName.equals("Mondrian", ignoreCase = true)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isMondrian) Modifier
                else Modifier.background(MaterialTheme.colorScheme.background)
            )
    ) {
        if (isMondrian) {
            MondrianBackground(
                song = currentSong,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                volumeRatio = currentVolumeRatio,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 1. Sliding Page Transition (Album Art + Song Titles & Labels)
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val pageSong = currentPlaylist.getOrNull(page) ?: currentSong

            val activeTopTriggers = GestureTrigger.getActiveTopTriggers(displaySettings.numEdgeRegions)
            val activeBottomTriggers = GestureTrigger.getActiveBottomTriggers(displaySettings.numEdgeRegions)

            val isPageSongDownloadedArt = musicScanner?.albumArtDownloader?.isDownloadedArtwork(pageSong) == true
            val hasTopButtons = activeTopTriggers.any {
                val act = resolveGestureBinding(it, gestureBindings).action
                act != GestureAction.UNASSIGNED && !(act == GestureAction.DELETE_DOWNLOADED_ART && !isPageSongDownloadedArt)
            }
            val hasBottomButtons = activeBottomTriggers.any {
                val act = resolveGestureBinding(it, gestureBindings).action
                act != GestureAction.UNASSIGNED && !(act == GestureAction.DELETE_DOWNLOADED_ART && !isPageSongDownloadedArt)
            }

            PlayerPageContent(
                pageSong = pageSong,
                displaySettings = displaySettings,
                themeSettings = themeSettings,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                volumeRatio = currentVolumeRatio,
                hasTopButtons = hasTopButtons,
                hasBottomButtons = hasBottomButtons,
                gestureBindings = gestureBindings,
                playbackManager = playbackManager,
                musicScanner = musicScanner,
                musicDatabase = musicDatabase,
                repeatMode = repeatMode,
                shuffleMode = shuffleMode,
                isPlaying = isPlaying,
                onOpenSongPicker = { dir -> openSongPicker(dir) },
                onOpenQueue = { dir -> openQueue(dir) },
                onOpenSettings = { dir -> openMenu(dir) },
                onOpenQuickStart = onOpenQuickStart,
                onShowRepeatOptions = { showRepeatOptionsDialog = true },
                onShowShuffleOptions = { showShuffleOptionsDialog = true }
            )
        }

        // 2. Gesture Detector Configuration
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val isDockedScreen = displaySettings.artDisplayLayout == ArtLayoutOption.DOCKED && displaySettings.showAlbumArt && !isMondrian

        val screenWidth = configuration.screenWidthDp.toFloat()
        val screenHeight = configuration.screenHeightDp.toFloat()
        val artFractionX = if (isLandscape) {
            if (screenWidth > 0f) (screenHeight / screenWidth).coerceIn(0.2f, 0.45f) else 0.45f
        } else {
            0.45f
        }
        val artFractionY = if (!isLandscape) {
            if (screenHeight > 0f) (screenWidth / screenHeight).coerceIn(0.2f, 0.45f) else 0.45f
        } else {
            0.45f
        }

        val regionBounds = if (isDockedScreen) {
            if (isLandscape) {
                when (displaySettings.artAlignmentLandscape) {
                    com.travelingtunes.app.core.model.ArtAlignmentLandscape.RIGHT -> Rect(0f, 0f, 1f - artFractionX, 1f)
                    com.travelingtunes.app.core.model.ArtAlignmentLandscape.TOP -> Rect(0f, artFractionY, 1f, 1f)
                    com.travelingtunes.app.core.model.ArtAlignmentLandscape.BOTTOM -> Rect(0f, 0f, 1f, 1f - artFractionY)
                    else -> Rect(artFractionX, 0f, 1f, 1f)
                }
            } else {
                when (displaySettings.artAlignmentPortrait) {
                    com.travelingtunes.app.core.model.ArtAlignmentPortrait.BOTTOM -> Rect(0f, 0f, 1f, 1f - artFractionY)
                    com.travelingtunes.app.core.model.ArtAlignmentPortrait.LEFT -> Rect(artFractionX, 0f, 1f, 1f)
                    com.travelingtunes.app.core.model.ArtAlignmentPortrait.RIGHT -> Rect(0f, 0f, 1f - artFractionX, 1f)
                    else -> Rect(0f, artFractionY, 1f, 1f)
                }
            }
        } else {
            Rect(0f, 0f, 1f, 1f)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .travelingTunesGestures(
                    listener = gestureListener,
                    gestureBindings = gestureBindings,
                    numEdgeRegions = displaySettings.numEdgeRegions,
                    regionBounds = regionBounds,
                    isOverlayOpen = false
                )
        )

        val hudBoundsModifier = if (isDockedScreen) {
            val offsetX = (screenWidth * regionBounds.left).dp
            val offsetY = (screenHeight * regionBounds.top).dp
            val widthDp = (screenWidth * regionBounds.width).dp
            val heightDp = (screenHeight * regionBounds.height).dp
            Modifier
                .offset(x = offsetX, y = offsetY)
                .size(width = widthDp, height = heightDp)
        } else {
            Modifier.fillMaxSize()
        }

        Box(modifier = hudBoundsModifier) {
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
        }

        // 5. Screen Region Icons Overlay (only if not docked)
        if (!isDockedScreen) {
            MondrianMaskedLayout(
                song = currentSong,
                themeSettings = themeSettings,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                volumeRatio = currentVolumeRatio,
                modifier = Modifier.fillMaxSize()
            ) {
                ScreenRegionIconsOverlay(
                    gestureBindings = gestureBindings,
                    playbackManager = playbackManager,
                    musicScanner = musicScanner,
                    musicDatabase = musicDatabase,
                    repeatMode = repeatMode,
                    shuffleMode = shuffleMode,
                    isPlaying = isPlaying,
                    onOpenSongPicker = { dir -> openSongPicker(dir) },
                    onOpenQueue = { dir -> openQueue(dir) },
                    onOpenSettings = { dir -> openMenu(dir) },
                    onOpenQuickStart = onOpenQuickStart,
                    onShowRepeatOptions = { showRepeatOptionsDialog = true },
                    onShowShuffleOptions = { showShuffleOptionsDialog = true },
                    numEdgeRegions = displaySettings.numEdgeRegions
                )
            }
        }

        // 7. Song Picker Sheet
        SongPickerBottomSheet(
            visible = showSongPicker,
            slideDirection = songPickerSlideDirection,
            openingTrigger = pickerOpeningTrigger,
            musicDatabase = musicDatabase,
            playbackManager = playbackManager,
            musicScanner = musicScanner,
            settingsDataStore = settingsDataStore,
            onDismiss = { showSongPicker = false }
        )

        // 8. Queue Sheet
        QueueBottomSheet(
            visible = showQueue,
            slideDirection = queueSlideDirection,
            openingTrigger = queueOpeningTrigger,
            playbackManager = playbackManager,
            onOpenSongPicker = { dir -> openSongPicker(dir) },
            onDismiss = { showQueue = false }
        )

        // 9. Menu / Settings Overlay
        SlidingOverlay(
            visible = showMenu,
            slideDirection = menuSlideDirection,
            openingTrigger = menuOpeningTrigger,
            onDismiss = { showMenu = false }
        ) {
            SettingsScreen(
                settingsDataStore = settingsDataStore ?: SettingsDataStore(context),
                displaySettings = displaySettings,
                themeSettings = themeSettings,
                musicFolderName = activeMusicFolderName,
                lastScanTime = activeLastScanTime,
                libraryStats = activeLibraryStats,
                isScanning = activeIsScanning,
                scanStatusMessage = activeScanStatusMessage,
                isDownloadingArt = activeIsDownloadingArt,
                artDownloadStatusMessage = activeArtDownloadStatusMessage,
                artDownloadDownloadedCount = activeArtDownloadDownloadedCount,
                artDownloadFailedCount = activeArtDownloadFailedCount,
                artDownloadTotalCount = activeArtDownloadTotalCount,
                lastAuditReport = activeLastAuditReport,
                onPickMusicFolder = onPickMusicFolder,
                onRescanMusicFolder = effectiveOnRescanMusicFolder,
                onDownloadMissingArt = effectiveOnDownloadMissingArt,
                onCancelDownloadArt = { musicScanner?.cancelDownloadArt() },
                onNavigateBack = { showMenu = false },
                onOpenGestureAssignments = {
                    showGestureAssignments = true
                    onOpenGestureAssignments()
                },
                onOpenQuickStart = onOpenQuickStart,
                onOpenDownloadedArtBrowser = {
                    showDownloadedArtBrowser = true
                    onOpenDownloadedArtBrowser()
                }
            )
        }

        // 10. Downloaded Art Browser Overlay
        SlidingOverlay(
            visible = showDownloadedArtBrowser,
            slideDirection = menuSlideDirection,
            openingTrigger = menuOpeningTrigger,
            onDismiss = { showDownloadedArtBrowser = false }
        ) {
            DownloadedArtBrowserScreen(
                musicDatabase = musicDatabase,
                albumArtDownloader = musicScanner?.albumArtDownloader ?: com.travelingtunes.app.core.media.AlbumArtDownloader(context, musicDatabase),
                playbackManager = playbackManager,
                onNavigateBack = { showDownloadedArtBrowser = false }
            )
        }

        // 11. Gesture Assignments Overlay
        SlidingOverlay(
            visible = showGestureAssignments,
            slideDirection = menuSlideDirection,
            openingTrigger = menuOpeningTrigger,
            onDismiss = { showGestureAssignments = false }
        ) {
            GestureAssignmentScreen(
                settingsDataStore = settingsDataStore ?: SettingsDataStore(context),
                gestureBindings = gestureBindings,
                onNavigateBack = { showGestureAssignments = false }
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

private fun isReverseActionForOpenOverlay(
    trigger: GestureTrigger,
    action: GestureAction,
    activeOpeningTrigger: GestureTrigger?,
    activeSlideDirection: SlideDirection,
    activeAction: GestureAction
): Boolean {
    if (activeOpeningTrigger != null) {
        val reverseTrigger = activeOpeningTrigger.getReverseTrigger()
        if (trigger == reverseTrigger || trigger == activeOpeningTrigger) {
            return true
        }
    }

    if (action == activeAction) {
        return true
    }

    val isMultiFingerSwipe = trigger.category == GestureCategory.TWO_FINGER_SWIPE || trigger.category == GestureCategory.THREE_FINGER_SWIPE
    if (isMultiFingerSwipe && trigger.getSlideDirection() != activeSlideDirection) {
        return true
    }

    return false
}

enum class DockAdjacentEdge {
    TOP, BOTTOM, LEFT, RIGHT
}

@Composable
private fun TitleAndButtonsContainer(
    pageSong: Song?,
    displaySettings: DisplaySettings,
    themeSettings: ThemeSettings = ThemeSettings(),
    currentPositionMs: Long = 0L,
    durationMs: Long = 0L,
    volumeRatio: Float = 0.5f,
    hasTopButtons: Boolean,
    hasBottomButtons: Boolean,
    gestureBindings: Map<GestureTrigger, GestureBinding>,
    playbackManager: PlaybackManager,
    musicScanner: MusicScanner? = null,
    musicDatabase: MusicDatabase? = null,
    repeatMode: RepeatMode,
    shuffleMode: ShuffleMode,
    isPlaying: Boolean,
    onOpenSongPicker: (SlideDirection) -> Unit,
    onOpenQueue: (SlideDirection) -> Unit = {},
    onOpenSettings: (SlideDirection) -> Unit,
    onOpenQuickStart: () -> Unit,
    onShowRepeatOptions: () -> Unit,
    onShowShuffleOptions: () -> Unit,
    dockAdjacentEdge: DockAdjacentEdge? = null,
    modifier: Modifier = Modifier
) {
    MondrianMaskedLayout(
        song = pageSong,
        themeSettings = themeSettings,
        currentPositionMs = currentPositionMs,
        durationMs = durationMs,
        volumeRatio = volumeRatio,
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SongLabelsLayout(
                currentSong = pageSong,
                displaySettings = displaySettings,
                hasTopButtons = hasTopButtons,
                hasBottomButtons = hasBottomButtons,
                dockAdjacentEdge = dockAdjacentEdge
            )
            ScreenRegionIconsOverlay(
                gestureBindings = gestureBindings,
                playbackManager = playbackManager,
                musicScanner = musicScanner,
                musicDatabase = musicDatabase,
                repeatMode = repeatMode,
                shuffleMode = shuffleMode,
                isPlaying = isPlaying,
                onOpenSongPicker = onOpenSongPicker,
                onOpenQueue = onOpenQueue,
                onOpenSettings = onOpenSettings,
                onOpenQuickStart = onOpenQuickStart,
                onShowRepeatOptions = onShowRepeatOptions,
                onShowShuffleOptions = onShowShuffleOptions,
                numEdgeRegions = displaySettings.numEdgeRegions,
                dockAdjacentEdge = dockAdjacentEdge
            )
        }
    }
}

@Composable
fun PlayerPageContent(
    pageSong: Song?,
    displaySettings: DisplaySettings,
    themeSettings: ThemeSettings = ThemeSettings(),
    currentPositionMs: Long = 0L,
    durationMs: Long = 0L,
    volumeRatio: Float = 0.5f,
    hasTopButtons: Boolean,
    hasBottomButtons: Boolean,
    gestureBindings: Map<GestureTrigger, GestureBinding>,
    playbackManager: PlaybackManager,
    musicScanner: MusicScanner? = null,
    musicDatabase: MusicDatabase? = null,
    repeatMode: RepeatMode,
    shuffleMode: ShuffleMode,
    isPlaying: Boolean,
    onOpenSongPicker: (SlideDirection) -> Unit,
    onOpenQueue: (SlideDirection) -> Unit = {},
    onOpenSettings: (SlideDirection) -> Unit,
    onOpenQuickStart: () -> Unit,
    onShowRepeatOptions: () -> Unit,
    onShowShuffleOptions: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isMondrian = themeSettings.currentThemeName.equals("Mondrian", ignoreCase = true)

    Box(modifier = Modifier.fillMaxSize()) {
        if (isMondrian) {
            MondrianBackground(
                song = pageSong,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                volumeRatio = volumeRatio,
                modifier = Modifier.fillMaxSize()
            )
        }

    val isDocked = displaySettings.artDisplayLayout == ArtLayoutOption.DOCKED && displaySettings.showAlbumArt && !isMondrian

    if (isDocked) {
        val (dockEdge, isRow) = if (isLandscape) {
            when (displaySettings.artAlignmentLandscape) {
                com.travelingtunes.app.core.model.ArtAlignmentLandscape.RIGHT -> Pair(DockAdjacentEdge.RIGHT, true)
                com.travelingtunes.app.core.model.ArtAlignmentLandscape.TOP -> Pair(DockAdjacentEdge.TOP, false)
                com.travelingtunes.app.core.model.ArtAlignmentLandscape.BOTTOM -> Pair(DockAdjacentEdge.BOTTOM, false)
                else -> Pair(DockAdjacentEdge.LEFT, true)
            }
        } else {
            when (displaySettings.artAlignmentPortrait) {
                com.travelingtunes.app.core.model.ArtAlignmentPortrait.BOTTOM -> Pair(DockAdjacentEdge.BOTTOM, false)
                com.travelingtunes.app.core.model.ArtAlignmentPortrait.LEFT -> Pair(DockAdjacentEdge.LEFT, true)
                com.travelingtunes.app.core.model.ArtAlignmentPortrait.RIGHT -> Pair(DockAdjacentEdge.RIGHT, true)
                else -> Pair(DockAdjacentEdge.TOP, false)
            }
        }

        val titlesContainer: @Composable (Modifier) -> Unit = { mod ->
            TitleAndButtonsContainer(
                pageSong = pageSong,
                displaySettings = displaySettings,
                themeSettings = themeSettings,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                volumeRatio = volumeRatio,
                hasTopButtons = hasTopButtons,
                hasBottomButtons = hasBottomButtons,
                gestureBindings = gestureBindings,
                playbackManager = playbackManager,
                musicScanner = musicScanner,
                musicDatabase = musicDatabase,
                repeatMode = repeatMode,
                shuffleMode = shuffleMode,
                isPlaying = isPlaying,
                onOpenSongPicker = onOpenSongPicker,
                onOpenQueue = onOpenQueue,
                onOpenSettings = onOpenSettings,
                onOpenQuickStart = onOpenQuickStart,
                onShowRepeatOptions = onShowRepeatOptions,
                onShowShuffleOptions = onShowShuffleOptions,
                dockAdjacentEdge = dockEdge,
                modifier = mod
            )
        }

        val albumArtContainer: @Composable (Modifier) -> Unit = { mod ->
            Box(modifier = mod) {
                PlayerAlbumArtBackground(
                    song = pageSong,
                    displaySettings = displaySettings,
                    themeSettings = themeSettings,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        val screenWidthDp = configuration.screenWidthDp.toFloat()
        val screenHeightDp = configuration.screenHeightDp.toFloat()

        val artFractionX = if (isLandscape) {
            if (screenWidthDp > 0f) (screenHeightDp / screenWidthDp).coerceIn(0.2f, 0.45f) else 0.45f
        } else {
            0.45f
        }
        val artFractionY = if (!isLandscape) {
            if (screenHeightDp > 0f) (screenWidthDp / screenHeightDp).coerceIn(0.2f, 0.45f) else 0.45f
        } else {
            0.45f
        }

        if (isRow) {
            Row(modifier = Modifier.fillMaxSize()) {
                val artMod = Modifier
                    .fillMaxHeight()
                    .widthIn(max = (screenWidthDp * artFractionX).dp)
                    .aspectRatio(1f)
                if (dockEdge == DockAdjacentEdge.LEFT) {
                    albumArtContainer(artMod)
                    titlesContainer(Modifier.weight(1f).fillMaxHeight())
                } else {
                    titlesContainer(Modifier.weight(1f).fillMaxHeight())
                    albumArtContainer(artMod)
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                val artMod = Modifier
                    .fillMaxWidth()
                    .heightIn(max = (screenHeightDp * artFractionY).dp)
                    .aspectRatio(1f)
                if (dockEdge == DockAdjacentEdge.TOP) {
                    albumArtContainer(artMod)
                    titlesContainer(Modifier.weight(1f).fillMaxWidth())
                } else {
                    titlesContainer(Modifier.weight(1f).fillMaxWidth())
                    albumArtContainer(artMod)
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            PlayerAlbumArtBackground(
                song = pageSong,
                displaySettings = displaySettings,
                themeSettings = themeSettings
            )

            TitleAndButtonsContainer(
                pageSong = pageSong,
                displaySettings = displaySettings,
                themeSettings = themeSettings,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                volumeRatio = volumeRatio,
                hasTopButtons = hasTopButtons,
                hasBottomButtons = hasBottomButtons,
                gestureBindings = gestureBindings,
                playbackManager = playbackManager,
                musicScanner = musicScanner,
                musicDatabase = musicDatabase,
                repeatMode = repeatMode,
                shuffleMode = shuffleMode,
                isPlaying = isPlaying,
                onOpenSongPicker = onOpenSongPicker,
                onOpenQueue = onOpenQueue,
                onOpenSettings = onOpenSettings,
                onOpenQuickStart = onOpenQuickStart,
                onShowRepeatOptions = onShowRepeatOptions,
                onShowShuffleOptions = onShowShuffleOptions,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
}

@Composable
fun SongLabelsLayout(
    currentSong: Song?,
    displaySettings: DisplaySettings,
    hasTopButtons: Boolean = true,
    hasBottomButtons: Boolean = true,
    dockAdjacentEdge: DockAdjacentEdge? = null,
    modifier: Modifier = Modifier
) {
    val artistFont = com.travelingtunes.app.core.theme.FontHelper.getFontFamily(displaySettings.artistFontKey)
    val songFont = com.travelingtunes.app.core.theme.FontHelper.getFontFamily(displaySettings.songFontKey)
    val albumFont = com.travelingtunes.app.core.theme.FontHelper.getFontFamily(displaySettings.albumFontKey)

    val baseTopPadding = if (hasTopButtons) 96.dp else 24.dp
    val baseBottomPadding = if (hasBottomButtons) 96.dp else 24.dp

    val topPadding = if (dockAdjacentEdge == DockAdjacentEdge.TOP) 16.dp else baseTopPadding
    val bottomPadding = if (dockAdjacentEdge == DockAdjacentEdge.BOTTOM) 16.dp else baseBottomPadding
    val startPadding = if (dockAdjacentEdge == DockAdjacentEdge.LEFT) 16.dp else 24.dp
    val endPadding = if (dockAdjacentEdge == DockAdjacentEdge.RIGHT) 16.dp else 24.dp

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

    PriorityTitlesLayout(
        titleOrder = displaySettings.titleOrder,
        artistContent = {
            BalancedTitleText(
                text = currentSong?.artist ?: "Traveling Tunes",
                fontSize = artistFontSize,
                lineHeight = artistLineHeight,
                fontFamily = artistFont,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = if (displaySettings.artistBold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (displaySettings.artistItalic) FontStyle.Italic else FontStyle.Normal,
                textDecoration = if (displaySettings.artistUnderline) TextDecoration.Underline else TextDecoration.None,
                textAlign = displaySettings.artistAlignment.toComposeAlignment(),
                minFontSize = minFontSize.sp,
                enableMarquee = displaySettings.titleScrollLong,
                modifier = Modifier.fillMaxWidth()
            )
        },
        songTitleContent = {
            BalancedTitleText(
                text = currentSong?.title ?: "Swipe or Tap Screen to Play",
                fontSize = songFontSize,
                lineHeight = songLineHeight,
                fontFamily = songFont,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = if (displaySettings.songBold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (displaySettings.songItalic) FontStyle.Italic else FontStyle.Normal,
                textDecoration = if (displaySettings.songUnderline) TextDecoration.Underline else TextDecoration.None,
                textAlign = displaySettings.songAlignment.toComposeAlignment(),
                minFontSize = minFontSize.sp,
                enableMarquee = displaySettings.titleScrollLong,
                modifier = Modifier.fillMaxWidth()
            )
        },
        albumContent = {
            BalancedTitleText(
                text = currentSong?.album ?: "No Song Selected",
                fontSize = albumFontSize,
                lineHeight = albumLineHeight,
                fontFamily = albumFont,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = if (displaySettings.albumBold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (displaySettings.albumItalic) FontStyle.Italic else FontStyle.Normal,
                textDecoration = if (displaySettings.albumUnderline) TextDecoration.Underline else TextDecoration.None,
                textAlign = displaySettings.albumAlignment.toComposeAlignment(),
                minFontSize = minFontSize.sp,
                enableMarquee = displaySettings.titleScrollLong,
                modifier = Modifier.fillMaxWidth()
            )
        },
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = startPadding,
                end = endPadding,
                top = topPadding,
                bottom = bottomPadding
            )
    )
}

@Composable
private fun PriorityTitlesLayout(
    titleOrder: List<TitleRowType>,
    artistContent: @Composable () -> Unit,
    songTitleContent: @Composable () -> Unit,
    albumContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val rowContents = mapOf(
        TitleRowType.ARTIST to artistContent,
        TitleRowType.SONG to songTitleContent,
        TitleRowType.ALBUM to albumContent
    )

    val orderedContents = titleOrder.mapNotNull { rowContents[it] }

    Layout(
        contents = orderedContents,
        modifier = modifier
    ) { (firstMeasurables, secondMeasurables, thirdMeasurables), constraints ->
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        val songIndex = titleOrder.indexOf(TitleRowType.SONG).coerceAtLeast(0)
        val measurablesList = listOf(firstMeasurables, secondMeasurables, thirdMeasurables)

        // 1. Song Title has priority - measure it FIRST!
        val songMeasurable = measurablesList.getOrNull(songIndex)?.firstOrNull()
        val songPlaceable = songMeasurable?.measure(looseConstraints)
        val songHeight = songPlaceable?.height ?: 0

        // 2. Measure remaining two items in remaining vertical height
        val remainingHeight = (constraints.maxHeight - songHeight).coerceAtLeast(0)
        val halfRemainingHeight = remainingHeight / 2

        val otherIndices = (0..2).filter { it != songIndex }
        val idx1 = otherIndices.getOrElse(0) { 0 }
        val idx2 = otherIndices.getOrElse(1) { 1 }

        val item1Measurable = measurablesList.getOrNull(idx1)?.firstOrNull()
        val item1Placeable = item1Measurable?.measure(looseConstraints.copy(maxHeight = halfRemainingHeight))
        val item1Height = item1Placeable?.height ?: 0

        val item2MaxHeight = (remainingHeight - item1Height).coerceAtLeast(0)
        val item2Measurable = measurablesList.getOrNull(idx2)?.firstOrNull()
        val item2Placeable = item2Measurable?.measure(looseConstraints.copy(maxHeight = item2MaxHeight))
        val item2Height = item2Placeable?.height ?: 0

        val placeables = arrayOfNulls<androidx.compose.ui.layout.Placeable>(3)
        placeables[songIndex] = songPlaceable
        placeables[idx1] = item1Placeable
        placeables[idx2] = item2Placeable

        val totalHeight = constraints.maxHeight
        val totalContentHeight = (songHeight + item1Height + item2Height)
        val slack = (totalHeight - totalContentHeight).coerceAtLeast(0)
        val spacer = slack / 2

        layout(constraints.maxWidth, totalHeight) {
            var currentY = 0
            for (p in placeables) {
                if (p != null) {
                    p.placeRelative(0, currentY)
                    currentY += p.height + spacer
                }
            }
        }
    }
}

@Composable
fun PlayerAlbumArtBackground(
    song: Song?,
    displaySettings: DisplaySettings,
    themeSettings: ThemeSettings = ThemeSettings(),
    modifier: Modifier = Modifier
) {
    val isMondrian = themeSettings.currentThemeName.equals("Mondrian", ignoreCase = true)
    if (!displaySettings.showAlbumArt || isMondrian) return

    val context = LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    var bitmap by remember(song?.id, song?.artworkUri) {
        mutableStateOf(song?.id?.let { AlbumArtCache.instance.get(it) })
    }

    LaunchedEffect(song?.id, song?.artworkUri, song?.contentUri) {
        if (song != null) {
            val cached = AlbumArtCache.instance.get(song.id)
            if (cached != null) {
                bitmap = cached
            } else {
                val loadedBitmap = withContext(Dispatchers.IO) {
                    loadSongArtwork(context, song)
                }
                if (loadedBitmap != null) {
                    val imgBmp = loadedBitmap.asImageBitmap()
                    AlbumArtCache.instance.put(song.id, imgBmp)
                    bitmap = imgBmp
                } else {
                    bitmap = null
                }
            }
        } else {
            bitmap = null
        }
    }

    val cachedBitmap = song?.id?.let { AlbumArtCache.instance.get(it) }
    val imgBitmap = bitmap ?: cachedBitmap
    val isDocked = displaySettings.artDisplayLayout == ArtLayoutOption.DOCKED

    if (imgBitmap != null) {
        val contentScale = when (displaySettings.albumArtScale) {
            ArtScaleOption.FILL_SCREEN -> ContentScale.Crop
            ArtScaleOption.ASPECT_FIT -> ContentScale.Fit
        }

        val imageAlignment = if (isDocked) {
            Alignment.Center
        } else if (isLandscape) {
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

        val artAlpha = if (isDocked) 1.0f else displaySettings.albumArtFade.coerceIn(0.1f, 1.0f)
        val letterboxBgColor = MaterialTheme.colorScheme.background

        Box(
            modifier = modifier
                .fillMaxSize()
                .then(
                    if (displaySettings.albumArtScale == ArtScaleOption.ASPECT_FIT) {
                        val bgAlpha = if (isDocked) 1.0f else displaySettings.albumArtFade.coerceIn(0.1f, 1.0f)
                        Modifier.background(letterboxBgColor.copy(alpha = bgAlpha))
                    } else Modifier
                )
        ) {
            Image(
                bitmap = imgBitmap,
                contentDescription = "Album Art Background",
                contentScale = contentScale,
                alignment = imageAlignment,
                colorFilter = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(artAlpha)
            )
        }
    } else if (isDocked) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "No Album Art",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp)
            )
        }
    }
}

@Composable
fun VolumeHudOverlay(
    volumeRatio: Float,
    displaySettings: DisplaySettings,
    themeSettings: ThemeSettings = ThemeSettings(),
    modifier: Modifier = Modifier
) {
    if (displaySettings.hudType == HudTypeOption.NONE || themeSettings.currentThemeName.equals("Mondrian", ignoreCase = true)) return

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

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(150)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        val animatedVolumeRatio by animateFloatAsState(
            targetValue = volumeRatio.coerceIn(0.01f, 1f),
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
            label = "volumeRatio"
        )

        val isMondrian = themeSettings.currentThemeName.equals("Mondrian", ignoreCase = true)
        val primaryColor = if (isMondrian) Color.Black else MaterialTheme.colorScheme.primary
        val lineThicknessDp = displaySettings.hudLineThickness.dp

        val shape = if (themeSettings.isRounded) RoundedCornerShape(lineThicknessDp / 2f) else RectangleShape
        val glassModifier = if (themeSettings.isGlass) {
            Modifier.border(1.dp, Color.White.copy(alpha = 0.45f), shape)
        } else Modifier

        when (displaySettings.hudType) {
            HudTypeOption.EDGE_HUD -> {
                // Geometric Vertical Strip along right edge
                BoxWithConstraints(modifier = modifier.fillMaxSize()) {
                    val filledHeight = this.maxHeight * animatedVolumeRatio
                    Box(
                        modifier = Modifier
                            .width(lineThicknessDp)
                            .height(filledHeight)
                            .align(Alignment.BottomEnd)
                            .clip(shape)
                            .background(
                                if (isMondrian) Color.Black
                                else if (themeSettings.isGlass) primaryColor.copy(alpha = 0.35f)
                                else primaryColor.copy(alpha = 0.50f)
                            )
                            .then(glassModifier)
                    )
                }
            }
            HudTypeOption.NUMBER -> {
                // Horizontal geometric line indicator at height corresponding to volume level
                BoxWithConstraints(modifier = modifier.fillMaxSize()) {
                    val topOffsetDp = (this.maxHeight - lineThicknessDp) * (1f - animatedVolumeRatio)
                    Box(
                        modifier = Modifier
                            .offset(y = topOffsetDp)
                            .then(if (themeSettings.isRounded) Modifier.padding(horizontal = 12.dp) else Modifier)
                            .fillMaxWidth()
                            .height(lineThicknessDp)
                            .clip(shape)
                            .background(
                                if (isMondrian) Color.Black
                                else if (themeSettings.isGlass) primaryColor.copy(alpha = 0.35f)
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
                        .fillMaxHeight(animatedVolumeRatio)
                        .then(barPadding)
                        .clip(barShape)
                        .background(
                            if (isMondrian) Color.Black
                            else if (themeSettings.isGlass) primaryColor.copy(alpha = 0.25f)
                            else primaryColor.copy(alpha = 0.20f)
                        )
                        .then(barGlassModifier)
                )
            }
            HudTypeOption.NONE -> {}
        }
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
    if (displaySettings.scrubHudType == ScrubHudTypeOption.NONE || durationMs <= 0L || themeSettings.currentThemeName.equals("Mondrian", ignoreCase = true)) return

    val rawProgressRatio = (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    val animatedProgressRatio by animateFloatAsState(
        targetValue = rawProgressRatio,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "progressRatio"
    )

    val isMondrian = themeSettings.currentThemeName.equals("Mondrian", ignoreCase = true)
    val primaryColor = if (isMondrian) Color.Black else MaterialTheme.colorScheme.primary
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
                        if (isMondrian) Color.Black.copy(alpha = 0.20f)
                        else if (themeSettings.isGlass) primaryColor.copy(alpha = 0.15f)
                        else primaryColor.copy(alpha = 0.15f)
                    )
                    .then(glassModifier)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgressRatio)
                        .clip(shape)
                        .background(
                            if (isMondrian) Color.Black
                            else if (themeSettings.isGlass) primaryColor.copy(alpha = 0.50f)
                            else primaryColor.copy(alpha = 0.85f)
                        )
                )
            }
        }
        ScrubHudTypeOption.POPUP -> {
            // Moving Geometric Vertical Line Indicator
            BoxWithConstraints(modifier = modifier.fillMaxSize()) {
                val leftOffsetDp = (this.maxWidth - lineThicknessDp) * animatedProgressRatio
                Box(
                    modifier = Modifier
                        .offset(x = leftOffsetDp)
                        .then(if (themeSettings.isRounded) Modifier.padding(vertical = 12.dp) else Modifier)
                        .fillMaxHeight()
                        .width(lineThicknessDp)
                        .clip(shape)
                        .background(
                            if (isMondrian) Color.Black
                            else if (themeSettings.isGlass) primaryColor.copy(alpha = 0.50f)
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
                    .fillMaxWidth(animatedProgressRatio)
                    .background(
                        if (isMondrian) Color.Black
                        else primaryColor.copy(alpha = 0.15f)
                    )
            )
        }
        ScrubHudTypeOption.NONE -> {}
    }
}

private fun handleGestureAction(
    action: GestureAction,
    trigger: GestureTrigger? = null,
    playbackManager: PlaybackManager,
    musicScanner: MusicScanner? = null,
    musicDatabase: MusicDatabase? = null,
    coroutineScope: kotlinx.coroutines.CoroutineScope? = null,
    onOpenSongPicker: (SlideDirection) -> Unit,
    onOpenQueue: (SlideDirection) -> Unit = {},
    onOpenSettings: (SlideDirection) -> Unit,
    onOpenQuickStart: () -> Unit
) {
    val direction = trigger?.getSlideDirection() ?: SlideDirection.BOTTOM
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
        GestureAction.SONG_PICKER -> onOpenSongPicker(direction)
        GestureAction.SHOW_QUEUE -> onOpenQueue(direction)
        GestureAction.MENU -> onOpenSettings(direction)
        GestureAction.SHOW_QUICK_START -> onOpenQuickStart()
        GestureAction.DELETE_DOWNLOADED_ART -> {
            val song = playbackManager.currentSong.value
            if (song != null && musicScanner != null && musicDatabase != null && coroutineScope != null) {
                coroutineScope.launch(Dispatchers.IO) {
                    val songsInAlbum = musicDatabase.getSongsByAlbumAndArtist(song.album, song.artist)
                    musicScanner.albumArtDownloader.deleteDownloadedArtworkForAlbum(song.album, song.artist, songsInAlbum)
                    playbackManager.refreshCurrentSongArtwork()
                }
            }
        }
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
    // 1. Try explicit song.artworkUri if present (downloaded or scanned artwork)
    if (song.artworkUri != null) {
        if (song.artworkUri.scheme == "file") {
            try {
                val bmp = BitmapFactory.decodeFile(song.artworkUri.path)
                if (bmp != null) return@withContext bmp
            } catch (ignored: Exception) {}
        }
        try {
            context.contentResolver.openInputStream(song.artworkUri)?.use { stream ->
                val bmp = BitmapFactory.decodeStream(stream)
                if (bmp != null) return@withContext bmp
            }
        } catch (ignored: Exception) {}
    }

    // 2. Try MediaStore album art URI from song.albumId
    if (song.albumId > 0) {
        try {
            val albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), song.albumId)
            context.contentResolver.openInputStream(albumArtUri)?.use { stream ->
                val bmp = BitmapFactory.decodeStream(stream)
                if (bmp != null) return@withContext bmp
            }
        } catch (ignored: Exception) {}
    }

    // 3. Try MediaMetadataRetriever on song.contentUri (embedded ID3 artwork)
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

    // 4. Try ContentResolver.loadThumbnail (Android 10+ / API 29+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        try {
            val bmp = context.contentResolver.loadThumbnail(song.contentUri, Size(1024, 1024), null)
            if (bmp != null) return@withContext bmp
        } catch (ignored: Exception) {}
    }

    null
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScreenRegionIconsOverlay(
    gestureBindings: Map<GestureTrigger, GestureBinding>,
    playbackManager: PlaybackManager,
    musicScanner: MusicScanner? = null,
    musicDatabase: MusicDatabase? = null,
    repeatMode: RepeatMode,
    shuffleMode: ShuffleMode,
    isPlaying: Boolean,
    onOpenSongPicker: (SlideDirection) -> Unit,
    onOpenQueue: (SlideDirection) -> Unit = {},
    onOpenSettings: (SlideDirection) -> Unit,
    onOpenQuickStart: () -> Unit,
    onShowRepeatOptions: () -> Unit,
    onShowShuffleOptions: () -> Unit,
    numEdgeRegions: Int = 3,
    dockAdjacentEdge: DockAdjacentEdge? = null,
    modifier: Modifier = Modifier
) {
    val currentSong by playbackManager.currentSong.collectAsState()
    val isDownloadedArt = remember(currentSong?.id, currentSong?.artworkUri) {
        musicScanner?.albumArtDownloader?.isDownloadedArtwork(currentSong) == true
    }
    val coroutineScope = rememberCoroutineScope()

    val activeSlotIndices = GestureTrigger.getActiveRegionSlots(numEdgeRegions)
    val n = activeSlotIndices.size

    val iconBoxSize = when {
        n > 5 -> 48.dp
        n > 3 -> 60.dp
        else -> 72.dp
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Top Edge Regions
        for ((index, slotIdx) in activeSlotIndices.withIndex()) {
            val trigger = GestureTrigger.TOP_REGION_SLOTS[slotIdx]
            val binding = resolveGestureBinding(trigger, gestureBindings)
            val action = binding.action

            if (action == GestureAction.DELETE_DOWNLOADED_ART && !isDownloadedArt) {
                continue
            }

            val horizontalBias = if (n == 1) 0.0f else (index.toFloat() / (n - 1) * 2.0f - 1.0f)
            val alignment = BiasAlignment(horizontalBias = horizontalBias, verticalBias = -1.0f)

            val baseStartPadding = if (horizontalBias == -1.0f) 8.dp else 0.dp
            val baseEndPadding = if (horizontalBias == 1.0f) 8.dp else 0.dp
            val startPadding = if (horizontalBias == -1.0f && dockAdjacentEdge == DockAdjacentEdge.LEFT) 4.dp else baseStartPadding
            val endPadding = if (horizontalBias == 1.0f && dockAdjacentEdge == DockAdjacentEdge.RIGHT) 4.dp else baseEndPadding
            val topPadding = if (dockAdjacentEdge == DockAdjacentEdge.TOP) 8.dp else 16.dp

            Box(
                modifier = Modifier
                    .align(alignment)
                    .padding(top = topPadding, start = startPadding, end = endPadding)
                    .size(iconBoxSize)
                    .clip(CircleShape)
                    .pointerInput(action) {
                        detectTapGestures(
                            onTap = {
                                if (action == GestureAction.TOGGLE_REPEAT) {
                                    playbackManager.toggleRepeat()
                                } else if (action == GestureAction.TOGGLE_SHUFFLE) {
                                    playbackManager.toggleShuffle()
                                } else if (action != GestureAction.UNASSIGNED) {
                                    handleGestureAction(
                                        action = action,
                                        trigger = trigger,
                                        playbackManager = playbackManager,
                                        musicScanner = musicScanner,
                                        musicDatabase = musicDatabase,
                                        coroutineScope = coroutineScope,
                                        onOpenSongPicker = onOpenSongPicker,
                                        onOpenQueue = onOpenQueue,
                                        onOpenSettings = onOpenSettings,
                                        onOpenQuickStart = onOpenQuickStart
                                    )
                                } else {
                                    onOpenSettings(SlideDirection.BOTTOM)
                                }
                            },
                            onLongPress = {
                                if (action == GestureAction.TOGGLE_REPEAT) {
                                    onShowRepeatOptions()
                                } else if (action == GestureAction.TOGGLE_SHUFFLE) {
                                    onShowShuffleOptions()
                                } else if (action != GestureAction.UNASSIGNED) {
                                    handleGestureAction(
                                        action = action,
                                        trigger = trigger,
                                        playbackManager = playbackManager,
                                        musicScanner = musicScanner,
                                        musicDatabase = musicDatabase,
                                        coroutineScope = coroutineScope,
                                        onOpenSongPicker = onOpenSongPicker,
                                        onOpenQueue = onOpenQueue,
                                        onOpenSettings = onOpenSettings,
                                        onOpenQuickStart = onOpenQuickStart
                                    )
                                } else {
                                    onOpenSettings(SlideDirection.BOTTOM)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (action != GestureAction.UNASSIGNED) {
                    ActionIcon(
                        action = action,
                        repeatMode = repeatMode,
                        shuffleMode = shuffleMode,
                        isPlaying = isPlaying,
                        iconSize = if (iconBoxSize < 60.dp) 24.dp else 36.dp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(if (iconBoxSize < 60.dp) 18.dp else 24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    )
                }
            }
        }

        // Bottom Edge Regions
        for ((index, slotIdx) in activeSlotIndices.withIndex()) {
            val trigger = GestureTrigger.BOTTOM_REGION_SLOTS[slotIdx]
            val binding = resolveGestureBinding(trigger, gestureBindings)
            val action = binding.action

            if (action == GestureAction.DELETE_DOWNLOADED_ART && !isDownloadedArt) {
                continue
            }

            val horizontalBias = if (n == 1) 0.0f else (index.toFloat() / (n - 1) * 2.0f - 1.0f)
            val alignment = BiasAlignment(horizontalBias = horizontalBias, verticalBias = 1.0f)

            val baseStartPadding = if (horizontalBias == -1.0f) 8.dp else 0.dp
            val baseEndPadding = if (horizontalBias == 1.0f) 8.dp else 0.dp
            val startPadding = if (horizontalBias == -1.0f && dockAdjacentEdge == DockAdjacentEdge.LEFT) 4.dp else baseStartPadding
            val endPadding = if (horizontalBias == 1.0f && dockAdjacentEdge == DockAdjacentEdge.RIGHT) 4.dp else baseEndPadding
            val bottomPadding = if (dockAdjacentEdge == DockAdjacentEdge.BOTTOM) 8.dp else 16.dp

            Box(
                modifier = Modifier
                    .align(alignment)
                    .padding(bottom = bottomPadding, start = startPadding, end = endPadding)
                    .size(iconBoxSize)
                    .clip(CircleShape)
                    .pointerInput(action) {
                        detectTapGestures(
                            onTap = {
                                if (action == GestureAction.TOGGLE_REPEAT) {
                                    playbackManager.toggleRepeat()
                                } else if (action == GestureAction.TOGGLE_SHUFFLE) {
                                    playbackManager.toggleShuffle()
                                } else if (action != GestureAction.UNASSIGNED) {
                                    handleGestureAction(
                                        action = action,
                                        trigger = trigger,
                                        playbackManager = playbackManager,
                                        musicScanner = musicScanner,
                                        musicDatabase = musicDatabase,
                                        coroutineScope = coroutineScope,
                                        onOpenSongPicker = onOpenSongPicker,
                                        onOpenQueue = onOpenQueue,
                                        onOpenSettings = onOpenSettings,
                                        onOpenQuickStart = onOpenQuickStart
                                    )
                                } else {
                                    onOpenSettings(SlideDirection.TOP)
                                }
                            },
                            onLongPress = {
                                if (action == GestureAction.TOGGLE_REPEAT) {
                                    onShowRepeatOptions()
                                } else if (action == GestureAction.TOGGLE_SHUFFLE) {
                                    onShowShuffleOptions()
                                } else if (action != GestureAction.UNASSIGNED) {
                                    handleGestureAction(
                                        action = action,
                                        trigger = trigger,
                                        playbackManager = playbackManager,
                                        musicScanner = musicScanner,
                                        musicDatabase = musicDatabase,
                                        coroutineScope = coroutineScope,
                                        onOpenSongPicker = onOpenSongPicker,
                                        onOpenQueue = onOpenQueue,
                                        onOpenSettings = onOpenSettings,
                                        onOpenQuickStart = onOpenQuickStart
                                    )
                                } else {
                                    onOpenSettings(SlideDirection.TOP)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (action != GestureAction.UNASSIGNED) {
                    ActionIcon(
                        action = action,
                        repeatMode = repeatMode,
                        shuffleMode = shuffleMode,
                        isPlaying = isPlaying,
                        iconSize = if (iconBoxSize < 60.dp) 24.dp else 36.dp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(if (iconBoxSize < 60.dp) 18.dp else 24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
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
