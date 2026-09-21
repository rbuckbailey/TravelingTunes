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
import androidx.compose.ui.graphics.TransformOrigin
import com.travelingtunes.app.core.model.ColorTheme
import com.travelingtunes.app.core.theme.TravelingTunesTheme
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import kotlin.math.hypot
import kotlinx.coroutines.withTimeout
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
import androidx.media3.common.Player
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
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
import com.travelingtunes.app.core.model.ConfigOption
import com.travelingtunes.app.core.model.DisplaySettings
import com.travelingtunes.app.core.model.GestureAction
import com.travelingtunes.app.core.model.GestureBinding
import com.travelingtunes.app.core.model.GestureCategory
import com.travelingtunes.app.core.model.GestureTrigger
import com.travelingtunes.app.core.model.HudTypeOption
import com.travelingtunes.app.core.model.Profile
import com.travelingtunes.app.core.model.ProfileSelectionMode
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
import com.travelingtunes.app.feature.settings.DuplicateTrackIdentifierScreen
import com.travelingtunes.app.feature.settings.GestureAssignmentScreen
import com.travelingtunes.app.feature.settings.SettingsScreen
import com.travelingtunes.app.feature.songpicker.PickerCategory
import com.travelingtunes.app.feature.songpicker.SongPickerBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
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

fun keyCodeToKeyboardTrigger(keyCode: Int, isShiftPressed: Boolean = false, unicodeChar: Int = 0): GestureTrigger? {
    if (unicodeChar == '?'.code) return GestureTrigger.KEY_QUESTION
    return when (keyCode) {
        android.view.KeyEvent.KEYCODE_SPACE -> GestureTrigger.KEY_SPACE
        android.view.KeyEvent.KEYCODE_F -> GestureTrigger.KEY_F
        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> GestureTrigger.KEY_LEFT
        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> GestureTrigger.KEY_RIGHT
        android.view.KeyEvent.KEYCODE_DPAD_UP -> GestureTrigger.KEY_UP
        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> GestureTrigger.KEY_DOWN
        android.view.KeyEvent.KEYCODE_ESCAPE -> GestureTrigger.KEY_ESC
        android.view.KeyEvent.KEYCODE_TAB -> GestureTrigger.KEY_TAB
        android.view.KeyEvent.KEYCODE_Q -> GestureTrigger.KEY_Q
        android.view.KeyEvent.KEYCODE_SLASH -> GestureTrigger.KEY_QUESTION
        android.view.KeyEvent.KEYCODE_F1 -> GestureTrigger.KEY_F1
        android.view.KeyEvent.KEYCODE_F2 -> GestureTrigger.KEY_F2
        android.view.KeyEvent.KEYCODE_F3 -> GestureTrigger.KEY_F3
        android.view.KeyEvent.KEYCODE_F4 -> GestureTrigger.KEY_F4
        android.view.KeyEvent.KEYCODE_F5 -> GestureTrigger.KEY_F5
        android.view.KeyEvent.KEYCODE_F6 -> GestureTrigger.KEY_F6
        android.view.KeyEvent.KEYCODE_F7 -> GestureTrigger.KEY_F7
        android.view.KeyEvent.KEYCODE_F8 -> GestureTrigger.KEY_F8
        android.view.KeyEvent.KEYCODE_F9 -> GestureTrigger.KEY_F9
        android.view.KeyEvent.KEYCODE_F10 -> GestureTrigger.KEY_F10
        android.view.KeyEvent.KEYCODE_F11 -> GestureTrigger.KEY_F11
        android.view.KeyEvent.KEYCODE_F12 -> GestureTrigger.KEY_F12
        android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
        android.view.KeyEvent.KEYCODE_MEDIA_PLAY,
        android.view.KeyEvent.KEYCODE_MEDIA_PAUSE -> GestureTrigger.KEY_MEDIA_PLAY_PAUSE
        android.view.KeyEvent.KEYCODE_MEDIA_NEXT -> GestureTrigger.KEY_MEDIA_NEXT
        android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS -> GestureTrigger.KEY_MEDIA_PREVIOUS
        android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> GestureTrigger.KEY_MEDIA_FAST_FORWARD
        android.view.KeyEvent.KEYCODE_MEDIA_REWIND -> GestureTrigger.KEY_MEDIA_REWIND
        android.view.KeyEvent.KEYCODE_MEDIA_STOP -> GestureTrigger.KEY_MEDIA_STOP
        else -> null
    }
}

data class PlayerScannerStatus(
    val isScanning: Boolean = false,
    val scanStatusMessage: String? = null,
    val isDownloadingArt: Boolean = false,
    val artDownloadStatusMessage: String? = null,
    val artDownloadDownloadedCount: Int = 0,
    val artDownloadFailedCount: Int = 0,
    val artDownloadTotalCount: Int = 0,
    val lastAuditReport: AlbumArtAuditReport? = null,
    val autoRescanEnabled: Boolean = false,
    val autoRescanStatusMessage: String? = null,
    val isAutoRescanWaiting: Boolean = false
)

data class PlayerNavigationCallbacks(
    val onOpenSettings: () -> Unit = {},
    val onOpenQuickStart: () -> Unit = {},
    val onOpenGestureAssignments: () -> Unit = {},
    val onOpenDownloadedArtBrowser: () -> Unit = {},
    val onOpenDuplicateTrackIdentifier: () -> Unit = {}
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerScreen(
    playbackManager: PlaybackManager,
    musicDatabase: MusicDatabase,
    displaySettings: DisplaySettings = DisplaySettings(),
    gestureBindings: Map<GestureTrigger, GestureBinding> = emptyMap(),
    musicScanner: MusicScanner? = null,
    settingsDataStore: SettingsDataStore? = null,
    themeSettings: ThemeSettings = ThemeSettings(),
    showFirstRunPrompt: Boolean = false,
    musicFolderName: String? = null,
    lastScanTime: Long = 0L,
    libraryStats: LibraryStats = LibraryStats(),
    scannerStatus: PlayerScannerStatus = PlayerScannerStatus(),
    navigationCallbacks: PlayerNavigationCallbacks = PlayerNavigationCallbacks(),
    onToggleAutoRescan: (Boolean) -> Unit = {},
    onDismissFirstRunPrompt: () -> Unit = {},
    onPickMusicFolder: () -> Unit = {},
    onRescanMusicFolder: () -> Unit = {},
    onDownloadMissingArt: () -> Unit = {}
) {
    val isScanning = scannerStatus.isScanning
    val scanStatusMessage = scannerStatus.scanStatusMessage
    val isDownloadingArt = scannerStatus.isDownloadingArt
    val artDownloadStatusMessage = scannerStatus.artDownloadStatusMessage
    val artDownloadDownloadedCount = scannerStatus.artDownloadDownloadedCount
    val artDownloadFailedCount = scannerStatus.artDownloadFailedCount
    val artDownloadTotalCount = scannerStatus.artDownloadTotalCount
    val lastAuditReport = scannerStatus.lastAuditReport
    val autoRescanEnabled = scannerStatus.autoRescanEnabled
    val autoRescanStatusMessage = scannerStatus.autoRescanStatusMessage
    val isAutoRescanWaiting = scannerStatus.isAutoRescanWaiting

    val onOpenSettings = navigationCallbacks.onOpenSettings
    val onOpenQuickStart = navigationCallbacks.onOpenQuickStart
    val onOpenGestureAssignments = navigationCallbacks.onOpenGestureAssignments
    val onOpenDownloadedArtBrowser = navigationCallbacks.onOpenDownloadedArtBrowser
    val onOpenDuplicateTrackIdentifier = navigationCallbacks.onOpenDuplicateTrackIdentifier

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
    val actionHudText by playbackManager.actionHudText.collectAsState()

    val currentPositionMs by playbackManager.currentPositionMs.collectAsState()
    val durationMs by playbackManager.durationMs.collectAsState()
    val currentVolumeRatio by playbackManager.currentVolumeRatio.collectAsState()

    val currentPositionMsProvider = remember(currentPositionMs) { { currentPositionMs } }
    val durationMsProvider = remember(durationMs) { { durationMs } }
    val currentVolumeRatioProvider = remember(currentVolumeRatio) { { currentVolumeRatio } }

    val isPlaying by playbackManager.isPlaying.collectAsState()
    val repeatMode by playbackManager.repeatMode.collectAsState()
    val shuffleMode by playbackManager.shuffleMode.collectAsState()
    val lastTransitionReason by playbackManager.lastMediaItemTransitionReason.collectAsState()

    var showSongPicker by remember { mutableStateOf(false) }
    var songPickerSlideDirection by remember { mutableStateOf(SlideDirection.BOTTOM) }
    var pickerOpeningTrigger by remember { mutableStateOf<GestureTrigger?>(null) }
    var pickerInitialCategory by remember { mutableStateOf<PickerCategory?>(null) }
    var pickerInitialArtist by remember { mutableStateOf<String?>(null) }
    var pickerInitialAlbum by remember { mutableStateOf<String?>(null) }

    var showQueue by remember { mutableStateOf(false) }
    var queueSlideDirection by remember { mutableStateOf(SlideDirection.BOTTOM) }
    var queueOpeningTrigger by remember { mutableStateOf<GestureTrigger?>(null) }

    var showMenu by remember { mutableStateOf(false) }
    var menuSlideDirection by remember { mutableStateOf(SlideDirection.BOTTOM) }
    var menuOpeningTrigger by remember { mutableStateOf<GestureTrigger?>(null) }

    var showDownloadedArtBrowser by remember { mutableStateOf(false) }
    var showGestureAssignments by remember { mutableStateOf(false) }
    var showDuplicateTrackIdentifier by remember { mutableStateOf(false) }
    var showProfilePicker by remember { mutableStateOf(false) }

    var showRadialMenu by remember { mutableStateOf(false) }
    var activeRadialTrigger by remember { mutableStateOf<GestureTrigger?>(null) }
    var activeRadialTouchOffset by remember { mutableStateOf(Offset.Unspecified) }
    var activeRadialDragOffset by remember { mutableStateOf<Offset?>(null) }
    var activeRadialSelectedAction by remember { mutableStateOf<GestureAction?>(null) }
    var activeRadialSelectedIndex by remember { mutableStateOf<Int?>(null) }

    val activeRadialActionsFlow = remember(activeRadialTrigger) {
        val trigKey = activeRadialTrigger?.key
        if (trigKey != null && settingsDataStore != null) {
            settingsDataStore.getRadialMenuActionsFlow(trigKey)
        } else null
    }
    val activeRadialActions by (activeRadialActionsFlow?.collectAsState(initial = SettingsDataStore.DEFAULT_RADIAL_ACTIONS)
        ?: remember { mutableStateOf(SettingsDataStore.DEFAULT_RADIAL_ACTIONS) })

    val activeRadialOtherOptionKeys by produceState<List<String?>>(initialValue = emptyList(), key1 = activeRadialTrigger, key2 = activeRadialActions) {
        val trigKey = activeRadialTrigger?.key
        if (trigKey != null && settingsDataStore != null) {
            val list = mutableListOf<String?>()
            for (i in activeRadialActions.indices) {
                val key = settingsDataStore.getRadialOtherOptionFlow(trigKey, i).first()
                list.add(key)
            }
            value = list
        } else {
            value = emptyList()
        }
    }

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

    val effectiveSettingsDataStore = settingsDataStore ?: remember { SettingsDataStore(context) }
    val activeAutoRescanEnabled by effectiveSettingsDataStore.autoRescanFlow.collectAsState(initial = autoRescanEnabled)
    val activeIsAutoRescanWaiting by (musicScanner?.isAutoRescanWaiting ?: kotlinx.coroutines.flow.MutableStateFlow(isAutoRescanWaiting)).collectAsState()
    val activeAutoRescanStatusMessage by (musicScanner?.autoRescanStatusMessage ?: kotlinx.coroutines.flow.MutableStateFlow(autoRescanStatusMessage)).collectAsState()

    var activeLibraryStats by remember { mutableStateOf(libraryStats) }
    LaunchedEffect(activeLastScanTime, activeIsScanning, currentPlaylist) {
        activeLibraryStats = musicDatabase.getLibraryStats()
    }

    fun openSongPicker(
        direction: SlideDirection = SlideDirection.BOTTOM,
        trigger: GestureTrigger? = null,
        initialCategory: PickerCategory? = null,
        initialArtist: String? = null,
        initialAlbum: String? = null
    ) {
        songPickerSlideDirection = direction
        pickerOpeningTrigger = trigger
        pickerInitialCategory = initialCategory
        pickerInitialArtist = initialArtist
        pickerInitialAlbum = initialAlbum
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

    var isProgrammaticScroll by remember { mutableStateOf(false) }

    // Sync pagerState -> PlaybackManager when user swipes pager to a settled page
    LaunchedEffect(pagerState.settledPage) {
        if (!isProgrammaticScroll && currentPlaylist.isNotEmpty() && pagerState.settledPage in currentPlaylist.indices) {
            val selectedSong = currentPlaylist[pagerState.settledPage]
            if (selectedSong.id != currentSong?.id) {
                playbackManager.playSongAtIndex(pagerState.settledPage)
            }
        }
    }

    // Sync PlaybackManager -> pagerState when song or playlist changes externally
    LaunchedEffect(currentSong?.id, currentPlaylist, lastTransitionReason) {
        if (songIndex in 0 until pageCount && pagerState.currentPage != songIndex) {
            isProgrammaticScroll = true
            try {
                val isNextTrackAuto = lastTransitionReason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                        lastTransitionReason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT ||
                        songIndex == pagerState.currentPage + 1
                if (isNextTrackAuto) {
                    pagerState.animateScrollToPage(
                        page = songIndex,
                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                    )
                } else {
                    pagerState.scrollToPage(songIndex)
                }
            } finally {
                isProgrammaticScroll = false
            }
        }
    }

    // Pre-cache surrounding album art
    LaunchedEffect(songIndex, pagerState.currentPage, currentPlaylist) {
        val activeIndex = if (pagerState.currentPage in currentPlaylist.indices) pagerState.currentPage else songIndex
        AlbumArtCache.instance.preCacheSurroundingSongs(context, currentPlaylist, activeIndex, radius = 4)
    }

    // Auto-clear action HUD text after 2 seconds
    LaunchedEffect(actionHudText) {
        if (actionHudText != null) {
            delay(2000L)
            playbackManager.clearHudAction()
        }
    }

    val isMondrian = themeSettings.currentThemeName.equals("Mondrian", ignoreCase = true)
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isMultiWindow = (context as? android.app.Activity)?.isInMultiWindowMode == true ||
            (if (isLandscape) configuration.screenHeightDp < 420 else configuration.screenHeightDp < 500)
    val isAdaptiveDockedActive = displaySettings.adaptiveDockedArt && isMultiWindow
    val isDockedScreen = displaySettings.artDisplayLayout == ArtLayoutOption.DOCKED && displaySettings.showAlbumArt && !isMondrian && !isAdaptiveDockedActive
    val isSeparateTouchZones = isDockedScreen && displaySettings.separateTouchZones && !displaySettings.adaptiveDockedArt

    val artFractionX = if (isLandscape) {
        if (screenWidthPx > 0f) (screenHeightPx / screenWidthPx).coerceIn(0.2f, 0.45f) else 0.45f
    } else {
        0.45f
    }
    val artFractionY = if (!isLandscape) {
        if (screenHeightPx > 0f) (screenWidthPx / screenHeightPx).coerceIn(0.2f, 0.45f) else 0.45f
    } else {
        0.45f
    }

    val artRegionBoundsNormalized = if (isDockedScreen) {
        if (isLandscape) {
            when (displaySettings.artAlignmentLandscape) {
                com.travelingtunes.app.core.model.ArtAlignmentLandscape.RIGHT -> Rect(1f - artFractionX, 0f, 1f, 1f)
                com.travelingtunes.app.core.model.ArtAlignmentLandscape.TOP -> Rect(0f, 0f, 1f, artFractionY)
                com.travelingtunes.app.core.model.ArtAlignmentLandscape.BOTTOM -> Rect(0f, 1f - artFractionY, 1f, 1f)
                else -> Rect(0f, 0f, artFractionX, 1f)
            }
        } else {
            when (displaySettings.artAlignmentPortrait) {
                com.travelingtunes.app.core.model.ArtAlignmentPortrait.BOTTOM -> Rect(0f, 1f - artFractionY, 1f, 1f)
                com.travelingtunes.app.core.model.ArtAlignmentPortrait.LEFT -> Rect(0f, 0f, artFractionX, 1f)
                com.travelingtunes.app.core.model.ArtAlignmentPortrait.RIGHT -> Rect(1f - artFractionX, 0f, 1f, 1f)
                else -> Rect(0f, 0f, 1f, artFractionY)
            }
        }
    } else {
        Rect(0f, 0f, 1f, 1f)
    }

    var activeContinuousAction by remember { mutableStateOf<GestureAction?>(null) }
    var pageDragOffsetX by remember { mutableStateOf(0f) }
    var pageDragOffsetY by remember { mutableStateOf(0f) }
    var activePageAction by remember { mutableStateOf<GestureAction?>(null) }
    var activePageTrigger by remember { mutableStateOf<GestureTrigger?>(null) }
    var activePageOtherKey by remember { mutableStateOf<String?>(null) }
    var dragStartTimeMs by remember { mutableStateOf(0L) }

    val isForegroundBusy = pagerState.isScrollInProgress ||
            activePageAction != null ||
            showRadialMenu ||
            activeContinuousAction != null ||
            pageDragOffsetX != 0f ||
            pageDragOffsetY != 0f ||
            showSongPicker ||
            showQueue ||
            showMenu

    LaunchedEffect(isForegroundBusy) {
        com.travelingtunes.app.core.media.BackgroundTaskGate.notifyForegroundBusy(isForegroundBusy)
    }

    val gestureListener = object : GestureEventListener {
        override fun onGestureTriggered(trigger: GestureTrigger, isLongPress: Boolean, touchOffset: Offset): Boolean {
            var binding = resolveGestureBinding(trigger, gestureBindings)

            val touchRegion = if (isSeparateTouchZones && touchOffset != Offset.Unspecified && screenWidthPx > 0f && screenHeightPx > 0f) {
                val normX = touchOffset.x / screenWidthPx
                val normY = touchOffset.y / screenHeightPx
                if (artRegionBoundsNormalized.contains(Offset(normX, normY))) {
                    com.travelingtunes.app.core.model.TouchRegionTarget.ART
                } else {
                    com.travelingtunes.app.core.model.TouchRegionTarget.TITLE
                }
            } else {
                com.travelingtunes.app.core.model.TouchRegionTarget.BOTH
            }

            val (resolvedAction, resolvedOtherKey) = when (touchRegion) {
                com.travelingtunes.app.core.model.TouchRegionTarget.ART -> {
                    if (binding.artAction != GestureAction.UNASSIGNED) {
                        binding.artAction to binding.artOtherOptionKey
                    } else if (binding.action != GestureAction.UNASSIGNED) {
                        binding.action to binding.otherOptionKey
                    } else {
                        GestureAction.UNASSIGNED to null
                    }
                }
                com.travelingtunes.app.core.model.TouchRegionTarget.TITLE -> {
                    if (binding.titleAction != GestureAction.UNASSIGNED) {
                        binding.titleAction to binding.titleOtherOptionKey
                    } else if (binding.action != GestureAction.UNASSIGNED) {
                        binding.action to binding.otherOptionKey
                    } else {
                        GestureAction.UNASSIGNED to null
                    }
                }
                com.travelingtunes.app.core.model.TouchRegionTarget.BOTH -> {
                    binding.action to binding.otherOptionKey
                }
            }

            var action = resolvedAction
            activeContinuousAction = action

            val isAnyOverlayOpen = showSongPicker || showQueue || showMenu || showDownloadedArtBrowser || showGestureAssignments || showDuplicateTrackIdentifier || showRadialMenu

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
                    if (showDuplicateTrackIdentifier) showDuplicateTrackIdentifier = false
                    if (showRadialMenu) showRadialMenu = false
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

            if (action == GestureAction.RADIAL_MENU) {
                activeRadialTrigger = trigger
                activeRadialTouchOffset = touchOffset
                activeRadialDragOffset = touchOffset
                activeRadialSelectedAction = null
                showRadialMenu = true
                return true
            }

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
                } else if (action == GestureAction.SELECT_ALBUM_VIEW) {
                    openSongPicker(slideDir, trigger, PickerCategory.ALBUMS, currentSong?.artist, currentSong?.album)
                } else if (action == GestureAction.SELECT_ARTIST_VIEW) {
                    openSongPicker(slideDir, trigger, PickerCategory.ARTISTS, currentSong?.artist, null)
                } else if (action == GestureAction.SHOW_QUEUE) {
                    openQueue(slideDir, trigger)
                }
                return true
            }

            val isPageSlideAction = action in listOf(
                GestureAction.NEXT,
                GestureAction.PREVIOUS,
                GestureAction.RESTART_PREVIOUS,
                GestureAction.NEXT_ALBUM,
                GestureAction.PREVIOUS_ALBUM,
                GestureAction.PLAY_CURRENT_ARTIST,
                GestureAction.PLAY_CURRENT_ALBUM
            )

            if (isPageSlideAction && trigger.category != GestureCategory.LONG_PRESS && trigger.category != GestureCategory.SCREEN_REGION) {
                activePageAction = action
                activePageTrigger = trigger
                activePageOtherKey = resolvedOtherKey
                dragStartTimeMs = System.currentTimeMillis()
                pageDragOffsetX = 0f
                pageDragOffsetY = 0f
                return true
            }

            when (action) {
                GestureAction.NEXT -> {
                    val nextIndex = pagerState.currentPage + 1
                    if (nextIndex in 0 until pageCount) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(
                                page = nextIndex,
                                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                            )
                        }
                    } else {
                        playbackManager.next()
                    }
                }
                GestureAction.PREVIOUS, GestureAction.RESTART_PREVIOUS -> {
                    val prevIndex = pagerState.currentPage - 1
                    if (prevIndex >= 0) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(
                                page = prevIndex,
                                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                            )
                        }
                    } else {
                        playbackManager.previous()
                    }
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
                        onOpenSongPickerWithFilter = { dir, cat, art, alb -> openSongPicker(dir, trigger, cat, art, alb) },
                        onOpenQueue = { dir -> openQueue(dir, trigger) },
                        onOpenSettings = { dir -> openMenu(dir, trigger) },
                        onOpenQuickStart = onOpenQuickStart,
                        onOpenProfilePicker = { showProfilePicker = true },
                        settingsDataStore = effectiveSettingsDataStore,
                        gestureBindings = gestureBindings,
                        overrideOtherOptionKey = resolvedOtherKey,
                        pagerState = pagerState,
                        pageCount = pageCount
                    )
                }
            }
            return true
        }

        override fun onGesturePointerMove(touchOffset: Offset) {
            if (showRadialMenu) {
                activeRadialDragOffset = touchOffset
            }
        }

        override fun onGesturePointerUp(touchOffset: Offset) {
            if (showRadialMenu) {
                val selectedAction = activeRadialSelectedAction
                showRadialMenu = false
                if (selectedAction != null) {
                    val trig = activeRadialTrigger ?: GestureTrigger.TAP_1_1
                    handleGestureAction(
                        action = selectedAction,
                        trigger = trig,
                        playbackManager = playbackManager,
                        musicScanner = musicScanner,
                        musicDatabase = musicDatabase,
                        coroutineScope = coroutineScope,
                        onOpenSongPicker = { dir -> openSongPicker(dir, trig) },
                        onOpenQueue = { dir -> openQueue(dir, trig) },
                        onOpenSettings = { dir -> openMenu(dir, trig) },
                        onOpenQuickStart = onOpenQuickStart,
                        onOpenProfilePicker = { showProfilePicker = true },
                        settingsDataStore = effectiveSettingsDataStore,
                        gestureBindings = gestureBindings
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
            val isAnyOverlayOpen = showSongPicker || showQueue || showMenu || showDownloadedArtBrowser || showGestureAssignments || showDuplicateTrackIdentifier
            if (isAnyOverlayOpen) return

            if (activePageAction != null) {
                val isHorizontalGesture = when (activePageTrigger) {
                    GestureTrigger.SWIPE_1_LEFT, GestureTrigger.SWIPE_1_RIGHT,
                    GestureTrigger.SWIPE_2_LEFT, GestureTrigger.SWIPE_2_RIGHT,
                    GestureTrigger.SWIPE_3_LEFT, GestureTrigger.SWIPE_3_RIGHT -> true
                    GestureTrigger.SWIPE_1_UP, GestureTrigger.SWIPE_1_DOWN,
                    GestureTrigger.SWIPE_2_UP, GestureTrigger.SWIPE_2_DOWN,
                    GestureTrigger.SWIPE_3_UP, GestureTrigger.SWIPE_3_DOWN -> false
                    else -> kotlin.math.abs(totalDx) >= kotlin.math.abs(totalDy)
                }

                if (isHorizontalGesture) {
                    pageDragOffsetX = totalDx
                    pageDragOffsetY = 0f
                } else {
                    pageDragOffsetX = 0f
                    pageDragOffsetY = totalDy
                }
                return
            }

            val binding = resolveGestureBinding(trigger, gestureBindings)
            val action = activeContinuousAction
                ?: binding.artAction.takeIf { it != GestureAction.UNASSIGNED }
                ?: binding.titleAction.takeIf { it != GestureAction.UNASSIGNED }
                ?: binding.action

            val msPerPixel = ((playbackManager.durationMs.value.coerceAtLeast(30000L)).toFloat() / screenWidthPx.coerceAtLeast(1f) * 0.5f).coerceIn(20f, 250f)

            when (action) {
                GestureAction.VOLUME_UP, GestureAction.VOLUME_DOWN -> {
                    playbackManager.adjustVolumeByDelta(deltaY, screenHeightPx)
                }
                GestureAction.FAST_FORWARD, GestureAction.REWIND -> {
                    val deltaMs = (deltaX * msPerPixel).toLong()
                    playbackManager.seekByDeltaContinuous(deltaMs)
                }
                else -> {
                    // Actions not explicitly bound to Volume or Seek should not trigger continuous Volume or Seeking
                }
            }
        }

        override fun onGestureEnd(totalDx: Float, totalDy: Float, fingers: Int) {
            playbackManager.commitContinuousSeek()
            activeContinuousAction = null

            val currentSlideAction = activePageAction
            val currentSlideTrigger = activePageTrigger
            val currentOtherKey = activePageOtherKey

            if (currentSlideAction != null && currentSlideTrigger != null) {
                val durationMs = (System.currentTimeMillis() - dragStartTimeMs).coerceAtLeast(1L)
                val isHorizontal = kotlin.math.abs(totalDx) >= kotlin.math.abs(totalDy)
                val netDisplacement = if (isHorizontal) totalDx else totalDy
                val screenSizePx = if (isHorizontal) screenWidthPx else screenHeightPx
                val commitThresholdPx = screenSizePx * 0.12f

                val isCommitted = when (currentSlideTrigger) {
                    GestureTrigger.SWIPE_1_LEFT, GestureTrigger.SWIPE_2_LEFT, GestureTrigger.SWIPE_3_LEFT -> totalDx < -commitThresholdPx
                    GestureTrigger.SWIPE_1_RIGHT, GestureTrigger.SWIPE_2_RIGHT, GestureTrigger.SWIPE_3_RIGHT -> totalDx > commitThresholdPx
                    GestureTrigger.SWIPE_1_UP, GestureTrigger.SWIPE_2_UP, GestureTrigger.SWIPE_3_UP -> totalDy < -commitThresholdPx
                    GestureTrigger.SWIPE_1_DOWN, GestureTrigger.SWIPE_2_DOWN, GestureTrigger.SWIPE_3_DOWN -> totalDy > commitThresholdPx
                    else -> kotlin.math.abs(netDisplacement) > commitThresholdPx
                }

                if (isCommitted) {
                    val speedPxPerMs = (kotlin.math.abs(netDisplacement) / durationMs.toFloat()).coerceIn(0.6f, 3.5f)
                    val remainingPx = screenSizePx - kotlin.math.abs(netDisplacement)
                    val slideOutDurationMs = (remainingPx / speedPxPerMs).toLong().coerceIn(120L, 400L)

                    val exitTargetX = if (isHorizontal) (if (totalDx < 0) -screenSizePx else screenSizePx) else 0f
                    val exitTargetY = if (!isHorizontal) (if (totalDy < 0) -screenSizePx else screenSizePx) else 0f

                    coroutineScope.launch {
                        val animX = androidx.compose.animation.core.Animatable(pageDragOffsetX)
                        val animY = androidx.compose.animation.core.Animatable(pageDragOffsetY)
                        launch { animX.animateTo(exitTargetX, androidx.compose.animation.core.tween(slideOutDurationMs.toInt(), easing = androidx.compose.animation.core.LinearOutSlowInEasing)) { pageDragOffsetX = value } }
                        launch { animY.animateTo(exitTargetY, androidx.compose.animation.core.tween(slideOutDurationMs.toInt(), easing = androidx.compose.animation.core.LinearOutSlowInEasing)) { pageDragOffsetY = value } }.join()

                        handleGestureAction(
                            action = currentSlideAction,
                            trigger = currentSlideTrigger,
                            playbackManager = playbackManager,
                            musicScanner = musicScanner,
                            musicDatabase = musicDatabase,
                            coroutineScope = coroutineScope,
                            onOpenSongPicker = { dir -> openSongPicker(dir, currentSlideTrigger) },
                            onOpenSongPickerWithFilter = { dir, cat, art, alb -> openSongPicker(dir, currentSlideTrigger, cat, art, alb) },
                            onOpenQueue = { dir -> openQueue(dir, currentSlideTrigger) },
                            onOpenSettings = { dir -> openMenu(dir, currentSlideTrigger) },
                            onOpenQuickStart = onOpenQuickStart,
                            onOpenProfilePicker = { showProfilePicker = true },
                            settingsDataStore = effectiveSettingsDataStore,
                            gestureBindings = gestureBindings,
                            overrideOtherOptionKey = currentOtherKey,
                            pagerState = pagerState,
                            pageCount = pageCount
                        )

                        delay(16L)

                        pageDragOffsetX = 0f
                        pageDragOffsetY = 0f
                        activePageAction = null
                        activePageTrigger = null
                        activePageOtherKey = null
                    }
                } else {
                    coroutineScope.launch {
                        val animX = androidx.compose.animation.core.Animatable(pageDragOffsetX)
                        val animY = androidx.compose.animation.core.Animatable(pageDragOffsetY)
                        launch { animX.animateTo(0f, androidx.compose.animation.core.tween(220, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { pageDragOffsetX = value } }
                        launch { animY.animateTo(0f, androidx.compose.animation.core.tween(220, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { pageDragOffsetY = value } }.join()

                        pageDragOffsetX = 0f
                        pageDragOffsetY = 0f
                        activePageAction = null
                        activePageTrigger = null
                        activePageOtherKey = null
                    }
                }
            }

            val isAnyOverlayOpen = showSongPicker || showQueue || showMenu || showDownloadedArtBrowser || showGestureAssignments || showDuplicateTrackIdentifier
            if (!isAnyOverlayOpen) {
                playbackManager.persistCurrentPlaybackState()
            }
        }
    }

    var frozenAdjacentSong by remember { mutableStateOf<Song?>(null) }
    val contextForArt = LocalContext.current

    val innerEdgeForCache = remember(displaySettings.artDisplayLayout, displaySettings.artAlignmentLandscape, displaySettings.artAlignmentPortrait, contextForArt) {
        if (displaySettings.artDisplayLayout == ArtLayoutOption.DOCKED) {
            val configuration = contextForArt.resources.configuration
            val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            if (isLandscape) {
                when (displaySettings.artAlignmentLandscape) {
                    com.travelingtunes.app.core.model.ArtAlignmentLandscape.RIGHT -> com.travelingtunes.app.core.theme.InnerEdge.LEFT
                    com.travelingtunes.app.core.model.ArtAlignmentLandscape.TOP -> com.travelingtunes.app.core.theme.InnerEdge.BOTTOM
                    com.travelingtunes.app.core.model.ArtAlignmentLandscape.BOTTOM -> com.travelingtunes.app.core.theme.InnerEdge.TOP
                    else -> com.travelingtunes.app.core.theme.InnerEdge.RIGHT
                }
            } else {
                when (displaySettings.artAlignmentPortrait) {
                    com.travelingtunes.app.core.model.ArtAlignmentPortrait.BOTTOM -> com.travelingtunes.app.core.theme.InnerEdge.TOP
                    com.travelingtunes.app.core.model.ArtAlignmentPortrait.LEFT -> com.travelingtunes.app.core.theme.InnerEdge.RIGHT
                    com.travelingtunes.app.core.model.ArtAlignmentPortrait.RIGHT -> com.travelingtunes.app.core.theme.InnerEdge.LEFT
                    else -> com.travelingtunes.app.core.theme.InnerEdge.BOTTOM
                }
            }
        } else null
    }

    LaunchedEffect(currentSong?.id, currentPlaylist, pagerState.currentPage, displaySettings.matchArtColorPriority, displaySettings.albumArtColors, innerEdgeForCache) {
        if (displaySettings.albumArtColors && currentSong != null) {
            val candidateSongs = listOfNotNull(
                currentSong,
                playbackManager.getNextSong(),
                playbackManager.getPreviousSong(),
                currentPlaylist.getOrNull(pagerState.currentPage + 1),
                currentPlaylist.getOrNull(pagerState.currentPage - 1),
                currentPlaylist.getOrNull(pagerState.currentPage + 2),
                currentPlaylist.getOrNull(pagerState.currentPage - 2),
                playbackManager.getNextAlbumFirstTrack(),
                playbackManager.getPreviousAlbumFirstTrack(),
                playbackManager.getArtistFirstTrack(),
                playbackManager.getAlbumFirstTrack()
            )
            com.travelingtunes.app.core.theme.AlbumArtColorCache.instance.preCacheSongs(
                context = contextForArt,
                songs = candidateSongs,
                innerEdge = innerEdgeForCache,
                priority = displaySettings.matchArtColorPriority
            )
        }
    }

    LaunchedEffect(activePageAction, currentSong) {
        if (activePageAction != null) {
            val resolved = when (activePageAction) {
                GestureAction.NEXT -> playbackManager.getNextSong() ?: currentPlaylist.getOrNull(pagerState.currentPage + 1)
                GestureAction.PREVIOUS, GestureAction.RESTART_PREVIOUS -> playbackManager.getPreviousSong() ?: currentPlaylist.getOrNull(pagerState.currentPage - 1)
                GestureAction.NEXT_ALBUM -> playbackManager.getNextAlbumFirstTrack()
                GestureAction.PREVIOUS_ALBUM -> playbackManager.getPreviousAlbumFirstTrack()
                GestureAction.PLAY_CURRENT_ARTIST -> playbackManager.getArtistFirstTrack()
                GestureAction.PLAY_CURRENT_ALBUM -> playbackManager.getAlbumFirstTrack()
                else -> null
            }
            if (resolved != null) {
                frozenAdjacentSong = resolved
                val cached = AlbumArtCache.instance.get(resolved.id)
                if (cached == null) {
                    withContext(Dispatchers.IO) {
                        loadSongArtwork(contextForArt, resolved)?.let {
                            AlbumArtCache.instance.put(resolved.id, it.asImageBitmap())
                        }
                    }
                }
                com.travelingtunes.app.core.theme.AlbumArtColorCache.instance.getOrExtract(
                    context = contextForArt,
                    song = resolved,
                    innerEdge = innerEdgeForCache,
                    priority = displaySettings.matchArtColorPriority
                )
            }
        } else {
            frozenAdjacentSong = null
        }
    }

    val keyboardFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        keyboardFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(keyboardFocusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    val nativeEvent = keyEvent.nativeKeyEvent
                    val trigger = keyCodeToKeyboardTrigger(
                        keyCode = nativeEvent.keyCode,
                        isShiftPressed = keyEvent.isShiftPressed,
                        unicodeChar = nativeEvent.getUnicodeChar(nativeEvent.metaState)
                    )
                    if (trigger != null) {
                        val binding = resolveGestureBinding(trigger, gestureBindings)
                        val action = binding.action
                        if (action != GestureAction.UNASSIGNED) {
                            handleGestureAction(
                                action = action,
                                trigger = trigger,
                                playbackManager = playbackManager,
                                musicScanner = musicScanner,
                                musicDatabase = musicDatabase,
                                coroutineScope = coroutineScope,
                                onOpenSongPicker = { dir -> openSongPicker(dir, trigger) },
                                onOpenSongPickerWithFilter = { dir, cat, art, alb -> openSongPicker(dir, trigger, cat, art, alb) },
                                onOpenQueue = { dir -> openQueue(dir, trigger) },
                                onOpenSettings = { dir -> openMenu(dir, trigger) },
                                onOpenQuickStart = onOpenQuickStart,
                                onOpenProfilePicker = { showProfilePicker = true },
                                settingsDataStore = effectiveSettingsDataStore,
                                gestureBindings = gestureBindings,
                                pagerState = pagerState,
                                pageCount = pageCount
                            )
                            return@onKeyEvent true
                        }
                    }
                }
                false
            }
    ) {
        if (isMondrian) {
            MondrianBackground(
                song = currentSong,
                currentPositionMsProvider = currentPositionMsProvider,
                durationMsProvider = durationMsProvider,
                volumeRatioProvider = currentVolumeRatioProvider,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 1. Sliding Page Transition (Album Art + Song Titles & Labels)
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 2,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = pageDragOffsetX
                    translationY = pageDragOffsetY
                }
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
                onShowShuffleOptions = { showShuffleOptionsDialog = true },
                onOpenProfilePicker = { showProfilePicker = true },
                settingsDataStore = effectiveSettingsDataStore
            )
        }

        // 1b. Pre-rendered Adjacent Page locked to current page edge during drag
        val adjacentSong = frozenAdjacentSong
        val isDraggingByGesture = activePageAction != null || pageDragOffsetX != 0f || pageDragOffsetY != 0f

        if (adjacentSong != null && isDraggingByGesture) {
            val activeTopTriggersAdj = GestureTrigger.getActiveTopTriggers(displaySettings.numEdgeRegions)
            val activeBottomTriggersAdj = GestureTrigger.getActiveBottomTriggers(displaySettings.numEdgeRegions)
            val isAdjDownloadedArt = musicScanner?.albumArtDownloader?.isDownloadedArtwork(adjacentSong) == true
            val hasTopBtns = activeTopTriggersAdj.any {
                val act = resolveGestureBinding(it, gestureBindings).action
                act != GestureAction.UNASSIGNED && !(act == GestureAction.DELETE_DOWNLOADED_ART && !isAdjDownloadedArt)
            }
            val hasBottomBtns = activeBottomTriggersAdj.any {
                val act = resolveGestureBinding(it, gestureBindings).action
                act != GestureAction.UNASSIGNED && !(act == GestureAction.DELETE_DOWNLOADED_ART && !isAdjDownloadedArt)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val dx = pageDragOffsetX
                        val dy = pageDragOffsetY
                        val isHoriz = kotlin.math.abs(dx) > kotlin.math.abs(dy)
                        translationX = if (isHoriz) (dx + if (dx < 0) screenWidthPx else -screenWidthPx) else 0f
                        translationY = if (!isHoriz) (dy + if (dy < 0) screenHeightPx else -screenHeightPx) else 0f
                    }
            ) {
                PlayerPageContent(
                    pageSong = adjacentSong,
                    displaySettings = displaySettings,
                    themeSettings = themeSettings,
                    currentPositionMs = 0L,
                    durationMs = adjacentSong.durationMs,
                    volumeRatio = currentVolumeRatio,
                    hasTopButtons = hasTopBtns,
                    hasBottomButtons = hasBottomBtns,
                    gestureBindings = gestureBindings,
                    playbackManager = playbackManager,
                    musicScanner = musicScanner,
                    musicDatabase = musicDatabase,
                    repeatMode = repeatMode,
                    shuffleMode = shuffleMode,
                    isPlaying = isPlaying,
                    onOpenSongPicker = {},
                    onOpenQueue = {},
                    onOpenSettings = {},
                    onOpenQuickStart = {},
                    onShowRepeatOptions = {},
                    onShowShuffleOptions = {}
                )
            }
        }

        // 2. Gesture Detector Configuration
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val contextGesture = LocalContext.current
        val isMultiWindowGesture = (contextGesture as? android.app.Activity)?.isInMultiWindowMode == true ||
                (if (isLandscape) configuration.screenHeightDp < 420 else configuration.screenHeightDp < 500)
        val isAdaptiveDockedActiveGesture = displaySettings.adaptiveDockedArt && isMultiWindowGesture
        val isDockedScreen = displaySettings.artDisplayLayout == ArtLayoutOption.DOCKED && displaySettings.showAlbumArt && !isMondrian && !isAdaptiveDockedActiveGesture

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
                    numArtEdgeRegions = displaySettings.numArtEdgeRegions,
                    artRegionBounds = artRegionBoundsNormalized,
                    isSeparateTouchZones = isSeparateTouchZones,
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
                volumeRatioProvider = currentVolumeRatioProvider,
                displaySettings = displaySettings,
                themeSettings = themeSettings,
                modifier = Modifier.align(Alignment.BottomStart)
            )

            // 4. Geometric Progress / Playback Bar Overlay (Edge Bar / Line)
            ProgressHudOverlay(
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                currentPositionMsProvider = currentPositionMsProvider,
                durationMsProvider = durationMsProvider,
                displaySettings = displaySettings,
                themeSettings = themeSettings,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }

        // 5. Screen Region Icons Overlay (only if not docked)
        if (!isDockedScreen) {
            MondrianMaskedLayout(
                song = currentSong,
                themeSettings = themeSettings,
                currentPositionMsProvider = currentPositionMsProvider,
                durationMsProvider = durationMsProvider,
                volumeRatioProvider = currentVolumeRatioProvider,
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
                    onOpenProfilePicker = { showProfilePicker = true },
                    numEdgeRegions = displaySettings.numEdgeRegions,
                    displaySettings = displaySettings,
                    settingsDataStore = effectiveSettingsDataStore
                )
            }
        }

        // 7. Song Picker Sheet
        SongPickerBottomSheet(
            visible = showSongPicker,
            slideDirection = songPickerSlideDirection,
            openingTrigger = pickerOpeningTrigger,
            initialCategory = pickerInitialCategory,
            initialArtist = pickerInitialArtist,
            initialAlbum = pickerInitialAlbum,
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

        val cddbOverridesCount by androidx.compose.runtime.produceState(initialValue = 0, key1 = activeLastScanTime) {
            value = musicScanner?.getCddbOverridesCount() ?: 0
        }
        val isEmbeddingCddb by (musicScanner?.isEmbeddingCddb ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsState()
        val cddbEmbeddingStatus by (musicScanner?.cddbStatusMessage ?: kotlinx.coroutines.flow.MutableStateFlow(null)).collectAsState()

        val normalizationMode by (settingsDataStore?.normalizationModeFlow ?: kotlinx.coroutines.flow.flowOf(com.travelingtunes.app.core.model.NormalizationMode.ALBUM)).collectAsState(initial = com.travelingtunes.app.core.model.NormalizationMode.ALBUM)
        val isAnalyzingVolume by (musicScanner?.isAnalyzingVolume ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsState()
        val volumeAnalysisStatusMessage by (musicScanner?.volumeAnalysisStatusMessage ?: kotlinx.coroutines.flow.MutableStateFlow(null)).collectAsState()

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
                musicDatabase = musicDatabase,
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
                autoRescanEnabled = activeAutoRescanEnabled,
                autoRescanStatusMessage = activeAutoRescanStatusMessage,
                isAutoRescanWaiting = activeIsAutoRescanWaiting,
                cddbOverridesCount = cddbOverridesCount,
                isEmbeddingCddb = isEmbeddingCddb,
                cddbEmbeddingStatus = cddbEmbeddingStatus,
                normalizationMode = normalizationMode,
                isAnalyzingVolume = isAnalyzingVolume,
                volumeAnalysisStatusMessage = volumeAnalysisStatusMessage,
                onSelectNormalizationMode = { mode ->
                    coroutineScope.launch {
                        effectiveSettingsDataStore.setNormalizationMode(mode)
                    }
                },
                onAnalyzeVolumeLevels = { force ->
                    coroutineScope.launch {
                        musicScanner?.analyzeLibraryVolumeLevels(forceRescan = force)
                    }
                },
                onCancelAnalyzeVolumeLevels = {
                    musicScanner?.cancelVolumeAnalysis()
                },
                onToggleAutoRescan = { enabled ->
                    coroutineScope.launch {
                        effectiveSettingsDataStore.setAutoRescan(enabled)
                    }
                    onToggleAutoRescan(enabled)
                },
                onPickMusicFolder = onPickMusicFolder,
                onRescanMusicFolder = effectiveOnRescanMusicFolder,
                onDownloadMissingArt = effectiveOnDownloadMissingArt,
                onCancelDownloadArt = { musicScanner?.cancelDownloadArt() },
                onEmbedCddbOverrides = { coroutineScope.launch { musicScanner?.embedCddbOverrides() } },
                onNavigateBack = { showMenu = false },
                onOpenGestureAssignments = {
                    showGestureAssignments = true
                    onOpenGestureAssignments()
                },
                onOpenQuickStart = onOpenQuickStart,
                onOpenDownloadedArtBrowser = {
                    showDownloadedArtBrowser = true
                    onOpenDownloadedArtBrowser()
                },
                onOpenDuplicateTrackIdentifier = {
                    showDuplicateTrackIdentifier = true
                    onOpenDuplicateTrackIdentifier()
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

        // Select Profile Dialog
        if (showProfilePicker) {
            val profiles by effectiveSettingsDataStore.profilesFlow.collectAsState(initial = listOf(Profile.DEFAULT, Profile.TRAVELING))
            val activeProfile by effectiveSettingsDataStore.activeProfileFlow.collectAsState(initial = Profile.DEFAULT)
            AlertDialog(
                onDismissRequest = { showProfilePicker = false },
                title = { Text("Select Settings Profile") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        profiles.forEach { profile ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        coroutineScope.launch {
                                            effectiveSettingsDataStore.setActiveProfile(profile.id)
                                        }
                                        showProfilePicker = false
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                            ) {
                                RadioButton(
                                    selected = profile.id == activeProfile.id,
                                    onClick = {
                                        coroutineScope.launch {
                                            effectiveSettingsDataStore.setActiveProfile(profile.id)
                                        }
                                        showProfilePicker = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("${profile.emoji}  ${profile.name}", fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showProfilePicker = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // 12. Duplicate Track Identifier Overlay
        SlidingOverlay(
            visible = showDuplicateTrackIdentifier,
            slideDirection = menuSlideDirection,
            openingTrigger = menuOpeningTrigger,
            onDismiss = { showDuplicateTrackIdentifier = false }
        ) {
            DuplicateTrackIdentifierScreen(
                musicDatabase = musicDatabase,
                musicFolderName = activeMusicFolderName,
                musicScanner = musicScanner,
                onNavigateBack = { showDuplicateTrackIdentifier = false }
            )
        }

        // 12. Radial Menu Overlay
        if (showRadialMenu && activeRadialTrigger != null) {
            RadialMenuOverlay(
                centerOffset = activeRadialTouchOffset,
                actions = activeRadialActions,
                otherOptionKeys = activeRadialOtherOptionKeys,
                dragOffset = activeRadialDragOffset,
                repeatMode = repeatMode,
                shuffleMode = shuffleMode,
                isPlaying = isPlaying,
                onSelectedActionChanged = { selected ->
                    activeRadialSelectedAction = selected
                },
                onSelectedIndexChanged = { idx ->
                    activeRadialSelectedIndex = idx
                },
                onSelectAction = { selectedRadialAction ->
                    showRadialMenu = false
                    val trig = activeRadialTrigger ?: GestureTrigger.TAP_1_1
                    handleGestureAction(
                        action = selectedRadialAction,
                        trigger = trig,
                        playbackManager = playbackManager,
                        musicScanner = musicScanner,
                        musicDatabase = musicDatabase,
                        coroutineScope = coroutineScope,
                        onOpenSongPicker = { dir -> openSongPicker(dir, trig) },
                        onOpenQueue = { dir -> openQueue(dir, trig) },
                        onOpenSettings = { dir -> openMenu(dir, trig) },
                        onOpenQuickStart = onOpenQuickStart,
                        onOpenProfilePicker = { showProfilePicker = true },
                        settingsDataStore = effectiveSettingsDataStore,
                        gestureBindings = gestureBindings,
                        radialSlotIndex = activeRadialSelectedIndex
                    )
                },
                onDismiss = {
                    showRadialMenu = false
                }
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
    onOpenProfilePicker: () -> Unit = {},
    dockAdjacentEdge: DockAdjacentEdge? = null,
    settingsDataStore: SettingsDataStore? = null,
    modifier: Modifier = Modifier
) {
    val contextTitle = LocalContext.current
    val effectiveSettingsDataStore = settingsDataStore ?: remember(contextTitle) { SettingsDataStore(contextTitle) }
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
                onOpenProfilePicker = onOpenProfilePicker,
                numEdgeRegions = displaySettings.numEdgeRegions,
                dockAdjacentEdge = dockAdjacentEdge,
                displaySettings = displaySettings,
                settingsDataStore = effectiveSettingsDataStore
            )
        }
    }
}

@Composable
fun rememberPageTheme(
    song: Song?,
    themeSettings: ThemeSettings,
    displaySettings: DisplaySettings
): ColorTheme {
    val context = LocalContext.current

    val innerEdge = remember(displaySettings.artDisplayLayout, displaySettings.artAlignmentLandscape, displaySettings.artAlignmentPortrait, context) {
        if (displaySettings.artDisplayLayout == ArtLayoutOption.DOCKED) {
            val configuration = context.resources.configuration
            val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            if (isLandscape) {
                when (displaySettings.artAlignmentLandscape) {
                    com.travelingtunes.app.core.model.ArtAlignmentLandscape.RIGHT -> com.travelingtunes.app.core.theme.InnerEdge.LEFT
                    com.travelingtunes.app.core.model.ArtAlignmentLandscape.TOP -> com.travelingtunes.app.core.theme.InnerEdge.BOTTOM
                    com.travelingtunes.app.core.model.ArtAlignmentLandscape.BOTTOM -> com.travelingtunes.app.core.theme.InnerEdge.TOP
                    else -> com.travelingtunes.app.core.theme.InnerEdge.RIGHT
                }
            } else {
                when (displaySettings.artAlignmentPortrait) {
                    com.travelingtunes.app.core.model.ArtAlignmentPortrait.BOTTOM -> com.travelingtunes.app.core.theme.InnerEdge.TOP
                    com.travelingtunes.app.core.model.ArtAlignmentPortrait.LEFT -> com.travelingtunes.app.core.theme.InnerEdge.RIGHT
                    com.travelingtunes.app.core.model.ArtAlignmentPortrait.RIGHT -> com.travelingtunes.app.core.theme.InnerEdge.LEFT
                    else -> com.travelingtunes.app.core.theme.InnerEdge.BOTTOM
                }
            }
        } else null
    }

    val isMatchArt = displaySettings.albumArtColors && (
        themeSettings.currentThemeName.equals("Match Album Art", ignoreCase = true) ||
        themeSettings.currentThemeName.equals("Auto By Art", ignoreCase = true)
    )

    val cacheKey = remember(song?.id, song?.artworkUri, innerEdge, displaySettings.matchArtColorPriority) {
        if (song != null) com.travelingtunes.app.core.theme.AlbumArtColorCache.makeKey(song.id, innerEdge, displaySettings.matchArtColorPriority) else ""
    }

    var extractedTheme: ColorTheme? by remember(cacheKey, isMatchArt) {
        mutableStateOf(if (isMatchArt && cacheKey.isNotEmpty()) com.travelingtunes.app.core.theme.AlbumArtColorCache.instance.get(cacheKey) else null)
    }

    LaunchedEffect(cacheKey, isMatchArt) {
        if (isMatchArt && song != null) {
            val cached = com.travelingtunes.app.core.theme.AlbumArtColorCache.instance.get(cacheKey)
            if (cached != null) {
                extractedTheme = cached
            } else {
                val theme = com.travelingtunes.app.core.theme.AlbumArtColorCache.instance.getOrExtract(
                    context = context,
                    song = song,
                    innerEdge = innerEdge,
                    priority = displaySettings.matchArtColorPriority
                )
                extractedTheme = theme
            }
        }
    }

    return remember(themeSettings, extractedTheme, displaySettings.albumArtColors) {
        com.travelingtunes.app.core.theme.resolveActiveTheme(
            themeSettings = themeSettings,
            dynamicAlbumArtTheme = extractedTheme,
            useAlbumArtColors = displaySettings.albumArtColors
        )
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
    onShowShuffleOptions: () -> Unit,
    onOpenProfilePicker: () -> Unit = {},
    settingsDataStore: SettingsDataStore? = null
) {
    val contextPage = LocalContext.current
    val effectiveSettingsDataStore = settingsDataStore ?: remember(contextPage) { SettingsDataStore(contextPage) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isMondrian = themeSettings.currentThemeName.equals("Mondrian", ignoreCase = true)

    val pageTheme = rememberPageTheme(pageSong, themeSettings, displaySettings)

    TravelingTunesTheme(
        themeSettings = themeSettings,
        dynamicAlbumArtTheme = pageTheme,
        useAlbumArtColors = displaySettings.albumArtColors
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isMondrian) Modifier
                    else Modifier.background(pageTheme.backgroundColor)
                )
        ) {
            if (isMondrian) {
                MondrianBackground(
                    song = pageSong,
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    volumeRatio = volumeRatio,
                    modifier = Modifier.fillMaxSize()
                )
            }

    val contextLayout = LocalContext.current
    val isMultiWindowLayout = (contextLayout as? android.app.Activity)?.isInMultiWindowMode == true ||
            (if (isLandscape) configuration.screenHeightDp < 420 else configuration.screenHeightDp < 500)
    val isAdaptiveDockedActiveLayout = displaySettings.adaptiveDockedArt && isMultiWindowLayout
    val isDocked = displaySettings.artDisplayLayout == ArtLayoutOption.DOCKED && displaySettings.showAlbumArt && !isMondrian && !isAdaptiveDockedActiveLayout

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
                onOpenProfilePicker = onOpenProfilePicker,
                dockAdjacentEdge = dockEdge,
                modifier = mod
            )
        }

        val isSeparateTouchZones = isDocked && displaySettings.separateTouchZones && !displaySettings.adaptiveDockedArt
        val albumArtContainer: @Composable (Modifier) -> Unit = { mod ->
            Box(modifier = mod) {
                PlayerAlbumArtBackground(
                    song = pageSong,
                    displaySettings = displaySettings,
                    themeSettings = themeSettings,
                    modifier = Modifier.fillMaxSize()
                )
                if (isSeparateTouchZones) {
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
                        onOpenProfilePicker = onOpenProfilePicker,
                        numEdgeRegions = displaySettings.numArtEdgeRegions,
                        useArtBindings = true,
                        dockAdjacentEdge = dockEdge,
                        displaySettings = displaySettings,
                        settingsDataStore = effectiveSettingsDataStore
                    )
                }
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
                    .widthIn(max = if (isLandscape) screenHeightDp.dp else (screenWidthDp * artFractionX).dp)
                    .aspectRatio(1f, matchHeightConstraintsFirst = true)
                if (dockEdge == DockAdjacentEdge.LEFT) {
                    albumArtContainer(artMod)
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        if (displaySettings.stretchArt) {
                            StretchedEdgeBackground(
                                song = pageSong,
                                dockEdge = dockEdge,
                                albumArtFade = displaySettings.albumArtFade,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        titlesContainer(Modifier.fillMaxSize())
                    }
                } else {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        if (displaySettings.stretchArt) {
                            StretchedEdgeBackground(
                                song = pageSong,
                                dockEdge = dockEdge,
                                albumArtFade = displaySettings.albumArtFade,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        titlesContainer(Modifier.fillMaxSize())
                    }
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
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (displaySettings.stretchArt) {
                            StretchedEdgeBackground(
                                song = pageSong,
                                dockEdge = dockEdge,
                                albumArtFade = displaySettings.albumArtFade,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        titlesContainer(Modifier.fillMaxSize())
                    }
                } else {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (displaySettings.stretchArt) {
                            StretchedEdgeBackground(
                                song = pageSong,
                                dockEdge = dockEdge,
                                albumArtFade = displaySettings.albumArtFade,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        titlesContainer(Modifier.fillMaxSize())
                    }
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
                onOpenProfilePicker = onOpenProfilePicker,
                modifier = Modifier.fillMaxSize()
            )
        }
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
        val measurablesList = listOf(firstMeasurables, secondMeasurables, thirdMeasurables)

        // 1. Measure top title (index 0 in titleOrder) first with loose height constraint
        val topMeasurable = measurablesList.getOrNull(0)?.firstOrNull()
        val topPlaceable = topMeasurable?.measure(looseConstraints.copy(maxHeight = (constraints.maxHeight * 0.45f).toInt()))
        val topHeight = topPlaceable?.height ?: 0

        val remainingHeight = (constraints.maxHeight - topHeight).coerceAtLeast(0)

        // 2. Measure middle title (index 1) with fair remaining height allocation
        val middleMeasurable = measurablesList.getOrNull(1)?.firstOrNull()
        val middleMaxHeight = (remainingHeight * 0.55f).toInt()
        val middlePlaceable = middleMeasurable?.measure(looseConstraints.copy(maxHeight = middleMaxHeight))
        val middleHeight = middlePlaceable?.height ?: 0

        // 3. Measure bottom title (index 2) with remaining height
        val bottomMaxHeight = (remainingHeight - middleHeight).coerceAtLeast(0)
        val bottomMeasurable = measurablesList.getOrNull(2)?.firstOrNull()
        val bottomPlaceable = bottomMeasurable?.measure(looseConstraints.copy(maxHeight = bottomMaxHeight))
        val bottomHeight = bottomPlaceable?.height ?: 0

        val placeables = arrayOf(topPlaceable, middlePlaceable, bottomPlaceable)

        val totalHeight = constraints.maxHeight
        val totalContentHeight = (topHeight + middleHeight + bottomHeight)
        val slack = (totalHeight - totalContentHeight).coerceAtLeast(0)
        val spacer = slack / 3

        layout(constraints.maxWidth, totalHeight) {
            var currentY = spacer / 2
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

    val imgBitmap = bitmap
    val contextBg = LocalContext.current
    val configurationBg = LocalConfiguration.current
    val isLandscapeBg = configurationBg.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isMultiWindowBg = (contextBg as? android.app.Activity)?.isInMultiWindowMode == true ||
            (if (isLandscapeBg) configurationBg.screenHeightDp < 420 else configurationBg.screenHeightDp < 500)
    val isAdaptiveDockedActiveBg = displaySettings.adaptiveDockedArt && isMultiWindowBg
    val isDocked = displaySettings.artDisplayLayout == ArtLayoutOption.DOCKED && !isAdaptiveDockedActiveBg

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
    volumeRatio: Float = 0.5f,
    displaySettings: DisplaySettings,
    themeSettings: ThemeSettings = ThemeSettings(),
    modifier: Modifier = Modifier,
    volumeRatioProvider: (() -> Float)? = null
) {
    if (displaySettings.hudType == HudTypeOption.NONE || themeSettings.currentThemeName.equals("Mondrian", ignoreCase = true)) return

    var isVisible by remember { mutableStateOf(displaySettings.volumeAlwaysOn) }
    val volVal = volumeRatioProvider?.invoke() ?: volumeRatio

    LaunchedEffect(volVal, displaySettings.volumeAlwaysOn) {
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
        val isMondrian = themeSettings.currentThemeName.equals("Mondrian", ignoreCase = true)
        val primaryColor = if (isMondrian) Color.Black else MaterialTheme.colorScheme.primary
        val lineThicknessDp = displaySettings.hudLineThickness.dp

        val shape = if (themeSettings.isRounded) RoundedCornerShape(lineThicknessDp / 2f) else RectangleShape
        val glassModifier = if (themeSettings.isGlass) {
            Modifier.border(1.dp, Color.White.copy(alpha = 0.45f), shape)
        } else Modifier

        Box(modifier = modifier.fillMaxSize()) {
            val lineThicknessPx = with(LocalDensity.current) { lineThicknessDp.toPx() }
            val currentRatio = (volumeRatioProvider?.invoke() ?: volumeRatio).coerceIn(0.01f, 1f)

            when (displaySettings.hudType) {
                HudTypeOption.EDGE_HUD -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Track background line
                        Box(
                            modifier = Modifier
                                .width(lineThicknessDp)
                                .fillMaxHeight()
                                .align(Alignment.BottomEnd)
                                .clip(shape)
                                .background(primaryColor.copy(alpha = 0.12f))
                        )
                        // Active volume edge bar
                        Box(
                            modifier = Modifier
                                .width(lineThicknessDp)
                                .fillMaxHeight()
                                .align(Alignment.BottomEnd)
                                .graphicsLayer {
                                    scaleY = currentRatio
                                    transformOrigin = TransformOrigin(0.5f, 1.0f)
                                }
                                .clip(shape)
                                .background(
                                    if (isMondrian) Color.Black
                                    else if (themeSettings.isGlass) primaryColor.copy(alpha = 0.50f)
                                    else primaryColor.copy(alpha = 0.75f)
                                )
                                .then(glassModifier)
                        )
                    }
                }
                HudTypeOption.NUMBER -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (themeSettings.isRounded) Modifier.padding(horizontal = 12.dp) else Modifier)
                            .graphicsLayer {
                                translationY = (size.height - lineThicknessPx) * (1f - currentRatio)
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(lineThicknessDp)
                                .clip(shape)
                                .background(
                                    if (isMondrian) Color.Black
                                    else if (themeSettings.isGlass) primaryColor.copy(alpha = 0.60f)
                                    else primaryColor.copy(alpha = 0.85f)
                                )
                                .then(glassModifier)
                        )
                    }
                }
                HudTypeOption.BAR_VOLUME -> {
                    val barShape = if (themeSettings.isRounded) RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp) else RectangleShape
                    val barGlassModifier = if (themeSettings.isGlass) {
                        Modifier.border(1.dp, Color.White.copy(alpha = 0.35f), barShape)
                    } else Modifier
                    val barPadding = if (themeSettings.isRounded) Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp) else Modifier

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .then(barPadding)
                    ) {
                        // Track background container
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(barShape)
                                .background(
                                    if (isMondrian) Color.Black.copy(alpha = 0.08f)
                                    else primaryColor.copy(alpha = 0.06f)
                                )
                        )
                        // Volume fill bar
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleY = currentRatio
                                    transformOrigin = TransformOrigin(0.5f, 1.0f)
                                }
                                .clip(barShape)
                                .background(
                                    if (isMondrian) Color.Black
                                    else if (themeSettings.isGlass) primaryColor.copy(alpha = 0.35f)
                                    else primaryColor.copy(alpha = 0.30f)
                                )
                                .then(barGlassModifier)
                        )
                        // Bright top indicator cap line across top edge of volume level
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    translationY = (1f - currentRatio) * size.height
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isMondrian) Color.Black
                                        else MaterialTheme.colorScheme.primary
                                    )
                            )
                        }
                    }
                }
                HudTypeOption.NONE -> {}
            }
        }
    }
}

@Composable
fun ProgressHudOverlay(
    currentPositionMs: Long = 0L,
    durationMs: Long = 0L,
    displaySettings: DisplaySettings,
    themeSettings: ThemeSettings = ThemeSettings(),
    modifier: Modifier = Modifier,
    currentPositionMsProvider: (() -> Long)? = null,
    durationMsProvider: (() -> Long)? = null
) {
    val durVal = durationMsProvider?.invoke() ?: durationMs
    if (displaySettings.scrubHudType == ScrubHudTypeOption.NONE || durVal <= 0L || themeSettings.currentThemeName.equals("Mondrian", ignoreCase = true)) return

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
                        .fillMaxWidth()
                        .graphicsLayer {
                            val d = durationMsProvider?.invoke() ?: durationMs
                            val p = currentPositionMsProvider?.invoke() ?: currentPositionMs
                            val ratio = if (d > 0L) (p.toFloat() / d.toFloat()).coerceIn(0f, 1f) else 0f
                            scaleX = ratio
                            transformOrigin = TransformOrigin(0f, 0.5f)
                        }
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
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val d = durationMsProvider?.invoke() ?: durationMs
                        val p = currentPositionMsProvider?.invoke() ?: currentPositionMs
                        val ratio = if (d > 0L) (p.toFloat() / d.toFloat()).coerceIn(0f, 1f) else 0f
                        val lineThicknessPx = with(density) { lineThicknessDp.toPx() }
                        translationX = (size.width - lineThicknessPx) * ratio
                    }
            ) {
                Box(
                    modifier = Modifier
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
            Box(
                modifier = modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .graphicsLayer {
                        val d = durationMsProvider?.invoke() ?: durationMs
                        val p = currentPositionMsProvider?.invoke() ?: currentPositionMs
                        val ratio = if (d > 0L) (p.toFloat() / d.toFloat()).coerceIn(0f, 1f) else 0f
                        scaleX = ratio
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
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
    onOpenSongPickerWithFilter: ((SlideDirection, PickerCategory, String?, String?) -> Unit)? = null,
    onOpenQueue: (SlideDirection) -> Unit = {},
    onOpenSettings: (SlideDirection) -> Unit,
    onOpenQuickStart: () -> Unit,
    onOpenProfilePicker: () -> Unit = {},
    settingsDataStore: SettingsDataStore? = null,
    gestureBindings: Map<GestureTrigger, GestureBinding>? = null,
    radialSlotIndex: Int? = null,
    overrideOtherOptionKey: String? = null,
    pagerState: androidx.compose.foundation.pager.PagerState? = null,
    pageCount: Int = 0
) {
    val direction = trigger?.getSlideDirection() ?: SlideDirection.BOTTOM
    when (action) {
        GestureAction.PLAY_PAUSE -> playbackManager.togglePlayPause()
        GestureAction.PLAY -> playbackManager.play()
        GestureAction.PAUSE -> playbackManager.pause()
        GestureAction.NEXT -> {
            if (pagerState != null && coroutineScope != null && pageCount > 0) {
                val nextIndex = pagerState.currentPage + 1
                if (nextIndex < pageCount) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(
                            page = nextIndex,
                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                        )
                    }
                } else {
                    playbackManager.next()
                }
            } else {
                playbackManager.next()
            }
        }
        GestureAction.PREVIOUS, GestureAction.RESTART_PREVIOUS -> {
            if (pagerState != null && coroutineScope != null && pageCount > 0) {
                val prevIndex = pagerState.currentPage - 1
                if (prevIndex >= 0) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(
                            page = prevIndex,
                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                        )
                    }
                } else {
                    playbackManager.previous()
                }
            } else {
                playbackManager.previous()
            }
        }
        GestureAction.RESTART -> playbackManager.restart()
        GestureAction.FAST_FORWARD -> playbackManager.fastForward()
        GestureAction.REWIND -> playbackManager.rewind()
        GestureAction.VOLUME_UP -> playbackManager.increaseVolume()
        GestureAction.VOLUME_DOWN -> playbackManager.decreaseVolume()
        GestureAction.TOGGLE_REPEAT -> playbackManager.toggleRepeat()
        GestureAction.TOGGLE_SHUFFLE -> playbackManager.toggleShuffle()
        GestureAction.SHUFFLE_ALL_SONGS -> playbackManager.shuffleAllSongs()
        GestureAction.PLAY_CURRENT_ALBUM -> playbackManager.playCurrentAlbum()
        GestureAction.PLAY_CURRENT_ARTIST -> playbackManager.playCurrentArtist()
        GestureAction.NEXT_ALBUM -> playbackManager.nextAlbum()
        GestureAction.PREVIOUS_ALBUM -> playbackManager.previousAlbum()
        GestureAction.INCREASE_RATING -> playbackManager.increaseRating()
        GestureAction.DECREASE_RATING -> playbackManager.decreaseRating()
        GestureAction.SONG_PICKER -> onOpenSongPicker(direction)
        GestureAction.SELECT_ALBUM_VIEW -> {
            val song = playbackManager.currentSong.value
            if (onOpenSongPickerWithFilter != null) {
                onOpenSongPickerWithFilter(direction, PickerCategory.ALBUMS, song?.artist, song?.album)
            } else {
                onOpenSongPicker(direction)
            }
        }
        GestureAction.SELECT_ARTIST_VIEW -> {
            val song = playbackManager.currentSong.value
            if (onOpenSongPickerWithFilter != null) {
                onOpenSongPickerWithFilter(direction, PickerCategory.ARTISTS, song?.artist, null)
            } else {
                onOpenSongPicker(direction)
            }
        }
        GestureAction.SHOW_QUEUE -> onOpenQueue(direction)
        GestureAction.MENU -> onOpenSettings(direction)
        GestureAction.SHOW_QUICK_START -> onOpenQuickStart()
        GestureAction.TOGGLE_DRIVING_MODE -> {
            if (settingsDataStore != null && coroutineScope != null) {
                coroutineScope.launch {
                    settingsDataStore.toggleDrivingMode()
                }
            }
        }
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
        GestureAction.TOGGLE_DOCKED_ART -> {
            if (settingsDataStore != null && coroutineScope != null) {
                coroutineScope.launch {
                    val currentDisplay = settingsDataStore.displaySettingsFlow.first()
                    val newLayout = if (currentDisplay.artDisplayLayout == ArtLayoutOption.DOCKED) ArtLayoutOption.OVERLAY else ArtLayoutOption.DOCKED
                    settingsDataStore.updateDisplaySettings(currentDisplay.copy(artDisplayLayout = newLayout))
                }
            }
        }
        GestureAction.SELECT_PROFILE -> {
            if (settingsDataStore != null && coroutineScope != null) {
                coroutineScope.launch {
                    val mode = settingsDataStore.profileSelectionModeFlow.first()
                    if (mode == ProfileSelectionMode.MENU) {
                        onOpenProfilePicker()
                    } else {
                        settingsDataStore.cycleToNextProfile()
                    }
                }
            }
        }
        GestureAction.OTHER_OPTION -> {
            if (settingsDataStore != null && coroutineScope != null) {
                val trig = trigger ?: GestureTrigger.TAP_1_1
                val binding = gestureBindings?.get(trig)
                val targetKey = overrideOtherOptionKey
                    ?: if (binding?.artAction == GestureAction.OTHER_OPTION) binding.artOtherOptionKey
                    else if (binding?.titleAction == GestureAction.OTHER_OPTION) binding.titleOtherOptionKey
                    else binding?.otherOptionKey
                if (targetKey != null) {
                    coroutineScope.launch {
                        settingsDataStore.toggleOtherOption(trig.key, targetKey)
                    }
                } else {
                    val slotIndex = radialSlotIndex ?: 0
                    coroutineScope.launch {
                        val radialTargetKey = settingsDataStore.getRadialOtherOptionFlow(trig.key, slotIndex).first()
                            ?: ConfigOption.ALL_OPTIONS.first().key
                        settingsDataStore.toggleOtherOption("${trig.key}_radial_$slotIndex", radialTargetKey)
                    }
                }
            }
        }
        GestureAction.RADIAL_MENU -> {}
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

fun android.graphics.Bitmap.cropToSquare(): android.graphics.Bitmap {
    if (width == height) return this
    val size = minOf(width, height)
    val x = (width - size) / 2
    val y = (height - size) / 2
    return android.graphics.Bitmap.createBitmap(this, x, y, size, size)
}

suspend fun loadSongArtwork(context: android.content.Context, song: Song): android.graphics.Bitmap? = withContext(Dispatchers.IO) {
    // 1. Try explicit song.artworkUri if present (downloaded or scanned artwork)
    if (song.artworkUri != null) {
        if (song.artworkUri.scheme == "file") {
            try {
                val bmp = BitmapFactory.decodeFile(song.artworkUri.path)
                if (bmp != null) return@withContext bmp.cropToSquare()
            } catch (ignored: Exception) {}
        }
        try {
            context.contentResolver.openInputStream(song.artworkUri)?.use { stream ->
                val bmp = BitmapFactory.decodeStream(stream)
                if (bmp != null) return@withContext bmp.cropToSquare()
            }
        } catch (ignored: Exception) {}
    }

    // 2. Try MediaStore album art URI from song.albumId
    if (song.albumId > 0) {
        try {
            val albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), song.albumId)
            context.contentResolver.openInputStream(albumArtUri)?.use { stream ->
                val bmp = BitmapFactory.decodeStream(stream)
                if (bmp != null) return@withContext bmp.cropToSquare()
            }
        } catch (ignored: Exception) {}
    }

    // 3. Try MediaMetadataRetriever on song.contentUri (embedded ID3 artwork)
    val mmr = MediaMetadataRetriever()
    try {
        mmr.setDataSource(context, song.contentUri)
        val bytes = mmr.embeddedPicture
        if (bytes != null) {
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bmp != null) return@withContext bmp.cropToSquare()
        }
    } catch (ignored: Exception) {
    } finally {
        try { mmr.release() } catch (ignored: Exception) {}
    }

    // 4. Try ContentResolver.loadThumbnail (Android 10+ / API 29+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        try {
            val bmp = context.contentResolver.loadThumbnail(song.contentUri, Size(1024, 1024), null)
            if (bmp != null) return@withContext bmp.cropToSquare()
        } catch (ignored: Exception) {}
    }

    null
}

private suspend fun PointerInputScope.detectRegionButtonGestures(
    onTap: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    isBottomRegion: Boolean = false,
    isTopRegion: Boolean = false
) {
    val slopPx = 12f * density
    val minUpwardExitPx = 3f * density
    val longPressTimeoutMs = 380L

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val startPos = down.position
        val startTime = System.currentTimeMillis()
        val isNearBottomEdge = isBottomRegion || startPos.y > (size.height.toFloat() - 28f * density)
        val isNearTopEdge = isTopRegion || startPos.y < (28f * density)

        var isCancelled = false
        var isLongPressFired = false
        var totalDx = 0f
        var totalDy = 0f
        var lastPos = startPos

        while (true) {
            val duration = System.currentTimeMillis() - startTime
            val timeoutRemaining = (longPressTimeoutMs - duration).coerceAtLeast(1L)

            val event = if (!isLongPressFired && onLongPress != null && duration < longPressTimeoutMs) {
                try {
                    withTimeout(timeoutRemaining) {
                        awaitPointerEvent(PointerEventPass.Initial)
                    }
                } catch (_: PointerEventTimeoutCancellationException) {
                    null
                }
            } else {
                awaitPointerEvent(PointerEventPass.Initial)
            }

            if (event == null) {
                if (!isLongPressFired && onLongPress != null && !isCancelled) {
                    val dist = hypot(totalDx, totalDy)
                    if (dist < slopPx) {
                        isLongPressFired = true
                        onLongPress()
                    }
                }
                continue
            }

            if (event.changes.any { it.isConsumed }) {
                isCancelled = true
            }

            val currentPointer = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull { it.pressed }
            if (currentPointer != null && currentPointer.pressed) {
                val dx = currentPointer.position.x - lastPos.x
                val dy = currentPointer.position.y - lastPos.y
                totalDx += dx
                totalDy += dy
                lastPos = currentPointer.position

                if (abs(totalDx) > slopPx || abs(totalDy) > slopPx) {
                    isCancelled = true
                }
                if (isNearBottomEdge && totalDy < -minUpwardExitPx) {
                    isCancelled = true
                }
                if (isNearTopEdge && totalDy > minUpwardExitPx) {
                    isCancelled = true
                }
            }

            val active = event.changes.filter { it.pressed }
            if (active.isEmpty()) {
                if (!isCancelled && !isLongPressFired) {
                    val dist = hypot(totalDx, totalDy)
                    if (dist < slopPx) {
                        onTap()
                    }
                }
                break
            }
        }
    }
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
    onOpenProfilePicker: () -> Unit = {},
    numEdgeRegions: Int = 3,
    useArtBindings: Boolean = false,
    dockAdjacentEdge: DockAdjacentEdge? = null,
    displaySettings: DisplaySettings? = null,
    settingsDataStore: SettingsDataStore? = null,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val effectiveSettingsDataStore = settingsDataStore ?: remember(context) { SettingsDataStore(context) }
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

    val isVolumeEdgeBar = displaySettings?.hudType == HudTypeOption.EDGE_HUD
    val volumeEdgeDisplacement = if (isVolumeEdgeBar && displaySettings != null) (displaySettings.hudLineThickness + 6f).dp else 0.dp
    val isDrivingModeEnabled = displaySettings?.drivingModeEnabled == true

    val activeProfileState = effectiveSettingsDataStore.activeProfileFlow.collectAsState(initial = Profile.DEFAULT)
    val activeProfileEmoji = activeProfileState.value.emoji
    val allProfilesState = effectiveSettingsDataStore.profilesFlow.collectAsState(initial = emptyList())
    val allProfilesList = allProfilesState.value

    Box(modifier = modifier.fillMaxSize()) {
        // Top Edge Regions
        for ((index, slotIdx) in activeSlotIndices.withIndex()) {
            val trigger = GestureTrigger.TOP_REGION_SLOTS[slotIdx]
            val binding = resolveGestureBinding(trigger, gestureBindings)
            val action = if (useArtBindings) {
                binding.artAction
            } else {
                if (binding.titleAction != GestureAction.UNASSIGNED) binding.titleAction else binding.action
            }

            val optionKey = if (useArtBindings) {
                binding.artOtherOptionKey
            } else {
                if (binding.titleAction != GestureAction.UNASSIGNED) binding.titleOtherOptionKey else binding.otherOptionKey
            }

            if (useArtBindings && action == GestureAction.UNASSIGNED) {
                continue
            }

            if (action == GestureAction.DELETE_DOWNLOADED_ART && !isDownloadedArt) {
                continue
            }

            val horizontalBias = if (n == 1) 0.0f else (index.toFloat() / (n - 1) * 2.0f - 1.0f)
            val alignment = BiasAlignment(horizontalBias = horizontalBias, verticalBias = -1.0f)

            val baseStartPadding = if (horizontalBias == -1.0f) 8.dp else 0.dp
            val baseEndPadding = if (horizontalBias == 1.0f) 8.dp + volumeEdgeDisplacement else 0.dp
            val startPadding = if (horizontalBias == -1.0f && dockAdjacentEdge == DockAdjacentEdge.LEFT) 4.dp else baseStartPadding
            val endPadding = if (horizontalBias == 1.0f && dockAdjacentEdge == DockAdjacentEdge.RIGHT) 4.dp + volumeEdgeDisplacement else baseEndPadding
            val topPadding = if (dockAdjacentEdge == DockAdjacentEdge.TOP) 8.dp else 16.dp

            val isRepeatActive = action == GestureAction.TOGGLE_REPEAT && repeatMode != RepeatMode.OFF
            val isShuffleActive = action == GestureAction.TOGGLE_SHUFFLE && shuffleMode != ShuffleMode.OFF
            val isDrivingActive = action == GestureAction.TOGGLE_DRIVING_MODE && isDrivingModeEnabled
            val isActiveControl = isRepeatActive || isShuffleActive || isDrivingActive

            val buttonBgColor = if (isActiveControl) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.15f)
            }
            val buttonTint = if (isActiveControl) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.primary
            }

            Box(
                modifier = Modifier
                    .align(alignment)
                    .padding(top = topPadding, start = startPadding, end = endPadding)
                    .size(iconBoxSize)
                    .clip(CircleShape)
                    .background(buttonBgColor)
                    .pointerInput(trigger, binding, action) {
                        detectRegionButtonGestures(
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
                                        onOpenQuickStart = onOpenQuickStart,
                                        onOpenProfilePicker = onOpenProfilePicker,
                                        settingsDataStore = effectiveSettingsDataStore,
                                        gestureBindings = gestureBindings,
                                        overrideOtherOptionKey = optionKey
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
                                        onOpenQuickStart = onOpenQuickStart,
                                        onOpenProfilePicker = onOpenProfilePicker,
                                        settingsDataStore = effectiveSettingsDataStore,
                                        gestureBindings = gestureBindings,
                                        overrideOtherOptionKey = optionKey
                                    )
                                } else {
                                    onOpenSettings(SlideDirection.BOTTOM)
                                }
                            },
                            isTopRegion = true
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (action != GestureAction.UNASSIGNED) {
                    ActionIcon(
                        action = action,
                        optionKey = optionKey,
                        repeatMode = repeatMode,
                        shuffleMode = shuffleMode,
                        isPlaying = isPlaying,
                        drivingModeEnabled = isDrivingModeEnabled,
                        activeProfileEmoji = activeProfileEmoji,
                        allProfiles = allProfilesList,
                        tint = buttonTint,
                        iconSize = if (iconBoxSize < 60.dp) 24.dp else 36.dp
                    )
                }
            }
        }

        // Bottom Edge Regions
        for ((index, slotIdx) in activeSlotIndices.withIndex()) {
            val trigger = GestureTrigger.BOTTOM_REGION_SLOTS[slotIdx]
            val binding = resolveGestureBinding(trigger, gestureBindings)
            val action = if (useArtBindings) {
                binding.artAction
            } else {
                if (binding.titleAction != GestureAction.UNASSIGNED) binding.titleAction else binding.action
            }
            val optionKey = if (useArtBindings) {
                binding.artOtherOptionKey
            } else {
                if (binding.titleAction != GestureAction.UNASSIGNED) binding.titleOtherOptionKey else binding.otherOptionKey
            }

            if (useArtBindings && action == GestureAction.UNASSIGNED) {
                continue
            }

            if (action == GestureAction.DELETE_DOWNLOADED_ART && !isDownloadedArt) {
                continue
            }

            val horizontalBias = if (n == 1) 0.0f else (index.toFloat() / (n - 1) * 2.0f - 1.0f)
            val alignment = BiasAlignment(horizontalBias = horizontalBias, verticalBias = 1.0f)

            val baseStartPadding = if (horizontalBias == -1.0f) 8.dp else 0.dp
            val baseEndPadding = if (horizontalBias == 1.0f) 8.dp + volumeEdgeDisplacement else 0.dp
            val startPadding = if (horizontalBias == -1.0f && dockAdjacentEdge == DockAdjacentEdge.LEFT) 4.dp else baseStartPadding
            val endPadding = if (horizontalBias == 1.0f && dockAdjacentEdge == DockAdjacentEdge.RIGHT) 4.dp + volumeEdgeDisplacement else baseEndPadding
            val bottomPadding = if (dockAdjacentEdge == DockAdjacentEdge.BOTTOM) 8.dp else 16.dp

            val isRepeatActive = action == GestureAction.TOGGLE_REPEAT && repeatMode != RepeatMode.OFF
            val isShuffleActive = action == GestureAction.TOGGLE_SHUFFLE && shuffleMode != ShuffleMode.OFF
            val isDrivingActive = action == GestureAction.TOGGLE_DRIVING_MODE && isDrivingModeEnabled
            val isActiveControl = isRepeatActive || isShuffleActive || isDrivingActive

            val buttonBgColor = if (isActiveControl) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.15f)
            }
            val buttonTint = if (isActiveControl) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.primary
            }

            Box(
                modifier = Modifier
                    .align(alignment)
                    .padding(bottom = bottomPadding, start = startPadding, end = endPadding)
                    .size(iconBoxSize)
                    .clip(CircleShape)
                    .background(buttonBgColor)
                    .pointerInput(trigger, binding, action) {
                        detectRegionButtonGestures(
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
                                        onOpenQuickStart = onOpenQuickStart,
                                        onOpenProfilePicker = onOpenProfilePicker,
                                        settingsDataStore = effectiveSettingsDataStore,
                                        gestureBindings = gestureBindings,
                                        overrideOtherOptionKey = optionKey
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
                                        onOpenQuickStart = onOpenQuickStart,
                                        onOpenProfilePicker = onOpenProfilePicker,
                                        settingsDataStore = effectiveSettingsDataStore,
                                        gestureBindings = gestureBindings,
                                        overrideOtherOptionKey = optionKey
                                    )
                                } else {
                                    onOpenSettings(SlideDirection.TOP)
                                }
                            },
                            isBottomRegion = true
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (action != GestureAction.UNASSIGNED) {
                    ActionIcon(
                        action = action,
                        optionKey = optionKey,
                        repeatMode = repeatMode,
                        shuffleMode = shuffleMode,
                        isPlaying = isPlaying,
                        drivingModeEnabled = isDrivingModeEnabled,
                        activeProfileEmoji = activeProfileEmoji,
                        allProfiles = allProfilesList,
                        tint = buttonTint,
                        iconSize = if (iconBoxSize < 60.dp) 24.dp else 36.dp
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

@Composable
private fun StretchedEdgeBackground(
    song: Song?,
    dockEdge: DockAdjacentEdge,
    albumArtFade: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    var bitmap by remember(song?.id, song?.artworkUri) {
        mutableStateOf<android.graphics.Bitmap?>(
            song?.id?.let { AlbumArtCache.instance.get(it)?.asAndroidBitmap() }
        )
    }

    LaunchedEffect(song?.id, song?.artworkUri) {
        if (song != null) {
            val cached = AlbumArtCache.instance.get(song.id)
            if (cached != null) {
                bitmap = cached.asAndroidBitmap()
            } else {
                val loadedBitmap = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    loadSongArtwork(context, song)
                }
                if (loadedBitmap != null) {
                    val imgBmp = loadedBitmap.asImageBitmap()
                    AlbumArtCache.instance.put(song.id, imgBmp)
                    bitmap = loadedBitmap
                } else {
                    bitmap = null
                }
            }
        } else {
            bitmap = null
        }
    }

    val imgBitmap = bitmap?.cropToSquare() ?: return

    val edgeBitmap = remember(imgBitmap, dockEdge) {
        val w = imgBitmap.width
        val h = imgBitmap.height
        if (w <= 0 || h <= 0) null else {
            try {
                when (dockEdge) {
                    DockAdjacentEdge.LEFT -> {
                        android.graphics.Bitmap.createBitmap(imgBitmap, (w - 1).coerceAtLeast(0), 0, 1, h)
                    }
                    DockAdjacentEdge.RIGHT -> {
                        android.graphics.Bitmap.createBitmap(imgBitmap, 0, 0, 1, h)
                    }
                    DockAdjacentEdge.TOP -> {
                        android.graphics.Bitmap.createBitmap(imgBitmap, 0, (h - 1).coerceAtLeast(0), w, 1)
                    }
                    DockAdjacentEdge.BOTTOM -> {
                        android.graphics.Bitmap.createBitmap(imgBitmap, 0, 0, w, 1)
                    }
                }.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    } ?: return

    val targetFade = albumArtFade.coerceIn(0.1f, 1.0f)

    Box(
        modifier = modifier
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                val containerLength = if (dockEdge == DockAdjacentEdge.LEFT || dockEdge == DockAdjacentEdge.RIGHT) size.width else size.height
                val quarterInchPx = 40f * density
                val quarterFraction = if (containerLength > 0f) (quarterInchPx / containerLength).coerceIn(0.02f, 0.40f) else 0.20f
                val stop1 = quarterFraction
                val stop2 = (1.0f - quarterFraction).coerceAtLeast(stop1)

                val colorStops = when (dockEdge) {
                    DockAdjacentEdge.LEFT, DockAdjacentEdge.TOP -> arrayOf(
                        0.0f to Color.Black.copy(alpha = 1.0f),
                        stop1 to Color.Black.copy(alpha = targetFade),
                        stop2 to Color.Black.copy(alpha = targetFade),
                        1.0f to Color.Black.copy(alpha = 0.0f)
                    )
                    DockAdjacentEdge.RIGHT, DockAdjacentEdge.BOTTOM -> arrayOf(
                        0.0f to Color.Black.copy(alpha = 0.0f),
                        stop1 to Color.Black.copy(alpha = targetFade),
                        stop2 to Color.Black.copy(alpha = targetFade),
                        1.0f to Color.Black.copy(alpha = 1.0f)
                    )
                }

                val gradientBrush = if (dockEdge == DockAdjacentEdge.LEFT || dockEdge == DockAdjacentEdge.RIGHT) {
                    androidx.compose.ui.graphics.Brush.horizontalGradient(colorStops = colorStops)
                } else {
                    androidx.compose.ui.graphics.Brush.verticalGradient(colorStops = colorStops)
                }

                drawContent()
                drawRect(
                    brush = gradientBrush,
                    blendMode = BlendMode.DstIn
                )
            }
    ) {
        Image(
            bitmap = edgeBitmap,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )
    }
}
