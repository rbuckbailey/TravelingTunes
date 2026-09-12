package com.travelingtunes.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.travelingtunes.app.core.database.LibraryStats
import com.travelingtunes.app.core.database.MusicDatabase
import com.travelingtunes.app.core.datastore.SettingsDataStore
import com.travelingtunes.app.core.location.SpeedVolumeManager
import com.travelingtunes.app.core.media.MediaStoreRepository
import com.travelingtunes.app.core.media.MusicScanner
import com.travelingtunes.app.core.media.PlaybackManager
import com.travelingtunes.app.core.theme.TravelingTunesTheme
import com.travelingtunes.app.feature.player.PlayerScreen
import com.travelingtunes.app.feature.quickstart.QuickStartScreen
import com.travelingtunes.app.feature.settings.DownloadedArtBrowserScreen
import com.travelingtunes.app.feature.settings.GestureAssignmentScreen
import com.travelingtunes.app.feature.settings.SettingsScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var playbackManager: PlaybackManager
    private lateinit var mediaStoreRepository: MediaStoreRepository
    private lateinit var speedVolumeManager: SpeedVolumeManager
    private lateinit var musicDatabase: MusicDatabase
    private lateinit var musicScanner: MusicScanner

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.READ_MEDIA_AUDIO] == true ||
                permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        if (audioGranted) {
            loadInitialMusic()
        }
    }

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val docFile = DocumentFile.fromTreeUri(applicationContext, uri)
            val folderName = docFile?.name ?: uri.lastPathSegment ?: "Music Folder"

            lifecycleScope.launch {
                settingsDataStore.setMusicFolder(uri.toString(), folderName)
                settingsDataStore.setFirstRunPrompted(true)
                musicScanner.scanFolder(uri)
                settingsDataStore.setLastScanTime(System.currentTimeMillis())
                val scannedSongs = musicDatabase.getAllSongs()
                if (scannedSongs.isNotEmpty()) {
                    playbackManager.setPlaylistAndPlay(scannedSongs, 0, shuffle = true)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsDataStore = SettingsDataStore(applicationContext)
        mediaStoreRepository = MediaStoreRepository(applicationContext)
        speedVolumeManager = SpeedVolumeManager(applicationContext)
        musicDatabase = MusicDatabase(applicationContext)
        musicScanner = MusicScanner(applicationContext, musicDatabase)
        playbackManager = PlaybackManager(applicationContext, settingsDataStore, musicDatabase)

        requestRequiredPermissions()

        setContent {
            val displaySettings by settingsDataStore.displaySettingsFlow.collectAsState(initial = com.travelingtunes.app.core.model.DisplaySettings())
            val themeSettings by settingsDataStore.themeSettingsFlow.collectAsState(initial = com.travelingtunes.app.core.model.ThemeSettings())
            val gestureBindings by settingsDataStore.gestureBindingsFlow.collectAsState(initial = emptyMap())
            val gpsVolumeEnabled by settingsDataStore.gpsVolumeEnabledFlow.collectAsState(initial = false)
            val gpsSensitivity by settingsDataStore.gpsSensitivityFlow.collectAsState(initial = 0.5f)

            val musicFolderUri by settingsDataStore.musicFolderUriFlow.collectAsState(initial = null)
            val musicFolderName by settingsDataStore.musicFolderNameFlow.collectAsState(initial = null)
            val lastScanTime by settingsDataStore.lastScanTimeFlow.collectAsState(initial = 0L)
            val firstRunPrompted by settingsDataStore.firstRunPromptedFlow.collectAsState(initial = false)
            val autoRescanEnabled by settingsDataStore.autoRescanFlow.collectAsState(initial = false)

            val isScanning by musicScanner.isScanning.collectAsState()
            val scanStatusMessage by musicScanner.statusMessage.collectAsState()
            val isAutoRescanWaiting by musicScanner.isAutoRescanWaiting.collectAsState()
            val autoRescanStatusMessage by musicScanner.autoRescanStatusMessage.collectAsState()

            val isDownloadingArt by musicScanner.isDownloadingArt.collectAsState()
            val artDownloadStatusMessage by musicScanner.artDownloadStatusMessage.collectAsState()
            val artDownloadDownloadedCount by musicScanner.artDownloadDownloadedCount.collectAsState()
            val artDownloadFailedCount by musicScanner.artDownloadFailedCount.collectAsState()
            val artDownloadTotalCount by musicScanner.artDownloadTotalCount.collectAsState()
            val lastAuditReport by musicScanner.lastAuditReport.collectAsState()

            var libraryStats by remember { mutableStateOf(LibraryStats()) }

            LaunchedEffect(lastScanTime, isScanning) {
                libraryStats = musicDatabase.getLibraryStats()
            }

            LaunchedEffect(autoRescanEnabled, musicFolderUri) {
                val folderUriStr = musicFolderUri
                if (autoRescanEnabled && !folderUriStr.isNullOrEmpty()) {
                    musicScanner.startAutoRescanWatcher(
                        treeUri = folderUriStr.toUri(),
                        coroutineScope = lifecycleScope
                    ) {
                        settingsDataStore.setLastScanTime(System.currentTimeMillis())
                        val scannedSongs = musicDatabase.getAllSongs()
                        if (scannedSongs.isNotEmpty() && playbackManager.currentSong.value == null) {
                            playbackManager.setPlaylistAndPlay(scannedSongs, 0, shuffle = true)
                        }
                    }
                } else {
                    musicScanner.stopAutoRescanWatcher()
                }
            }

            LaunchedEffect(gpsVolumeEnabled, gpsSensitivity) {
                if (gpsVolumeEnabled) {
                    speedVolumeManager.startTracking(gpsSensitivity)
                } else {
                    speedVolumeManager.stopTracking()
                }
            }

            LaunchedEffect(displaySettings.keepScreenOn) {
                if (displaySettings.keepScreenOn) {
                    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            LaunchedEffect(displaySettings.immersiveMode) {
                val insetsController = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
                if (displaySettings.immersiveMode) {
                    insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                    insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    insetsController.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                }
            }

            val currentSong by playbackManager.currentSong.collectAsState()
            var dynamicAlbumArtTheme by remember { mutableStateOf<com.travelingtunes.app.core.model.ColorTheme?>(null) }
            var lastExtractedTheme by remember { mutableStateOf<com.travelingtunes.app.core.model.ColorTheme?>(null) }

            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val innerEdge = if (displaySettings.artDisplayLayout == com.travelingtunes.app.core.model.ArtLayoutOption.DOCKED && displaySettings.showAlbumArt) {
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

            LaunchedEffect(currentSong?.id, currentSong?.artworkUri, innerEdge) {
                val song = currentSong
                if (song != null) {
                    val bitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        com.travelingtunes.app.feature.player.loadSongArtwork(applicationContext, song)
                    }
                    if (bitmap != null) {
                        val extracted = com.travelingtunes.app.core.theme.AlbumArtColorExtractor.extractThemeFromBitmap(bitmap, innerEdge)
                        lastExtractedTheme = extracted
                        dynamicAlbumArtTheme = extracted
                    } else {
                        dynamicAlbumArtTheme = lastExtractedTheme
                    }
                } else {
                    dynamicAlbumArtTheme = lastExtractedTheme
                }
            }

            val isMatchAlbumArt = themeSettings.currentThemeName.equals("Match Album Art", ignoreCase = true) ||
                                  themeSettings.currentThemeName.equals("Auto By Art", ignoreCase = true) ||
                                  displaySettings.albumArtColors

            TravelingTunesTheme(
                themeSettings = themeSettings,
                dynamicAlbumArtTheme = dynamicAlbumArtTheme,
                useAlbumArtColors = isMatchAlbumArt
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TravelingTunesNavHost(
                        playbackManager = playbackManager,
                        musicDatabase = musicDatabase,
                        musicScanner = musicScanner,
                        settingsDataStore = settingsDataStore,
                        displaySettings = displaySettings,
                        themeSettings = themeSettings,
                        gestureBindings = gestureBindings,
                        musicFolderUri = musicFolderUri,
                        musicFolderName = musicFolderName,
                        lastScanTime = lastScanTime,
                        firstRunPrompted = firstRunPrompted,
                        libraryStats = libraryStats,
                        isScanning = isScanning,
                        scanStatusMessage = scanStatusMessage,
                        isDownloadingArt = isDownloadingArt,
                        artDownloadStatusMessage = artDownloadStatusMessage,
                        artDownloadDownloadedCount = artDownloadDownloadedCount,
                        artDownloadFailedCount = artDownloadFailedCount,
                        artDownloadTotalCount = artDownloadTotalCount,
                        lastAuditReport = lastAuditReport,
                        autoRescanEnabled = autoRescanEnabled,
                        autoRescanStatusMessage = autoRescanStatusMessage,
                        isAutoRescanWaiting = isAutoRescanWaiting,
                        onToggleAutoRescan = { enabled ->
                            lifecycleScope.launch {
                                settingsDataStore.setAutoRescan(enabled)
                            }
                        },
                        onPickMusicFolder = { folderPickerLauncher.launch(null) },
                        onRescanMusicFolder = {
                            musicFolderUri?.let { uriStr ->
                                lifecycleScope.launch {
                                    musicScanner.scanFolder(uriStr.toUri())
                                    settingsDataStore.setLastScanTime(System.currentTimeMillis())
                                    val scannedSongs = musicDatabase.getAllSongs()
                                    if (scannedSongs.isNotEmpty()) {
                                        playbackManager.setPlaylistAndPlay(scannedSongs, 0, shuffle = true)
                                    }
                                }
                            }
                        },
                        onDownloadMissingArt = {
                            lifecycleScope.launch {
                                musicScanner.downloadMissingArtwork()
                            }
                        },
                        onDismissFirstRunPrompt = {
                            lifecycleScope.launch {
                                settingsDataStore.setFirstRunPrompted(true)
                            }
                        }
                    )
                }
            }
        }
    }

    private fun requestRequiredPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            loadInitialMusic()
        }
    }

    private fun loadInitialMusic() {
        lifecycleScope.launch {
            val savedState = settingsDataStore.savedPlaybackStateFlow.first()
            val dbSongs = musicDatabase.getAllSongs()
            val allSongs = dbSongs.ifEmpty { mediaStoreRepository.getAllSongs() }

            if (allSongs.isNotEmpty()) {
                if (dbSongs.isEmpty()) {
                    musicDatabase.insertOrReplaceSongs(allSongs)
                }

                if (savedState.queueIds.isNotEmpty()) {
                    val songMap = allSongs.associateBy { it.id }
                    val restoredQueue = savedState.queueIds.mapNotNull { songMap[it] }
                    val finalQueue = restoredQueue.ifEmpty { allSongs }

                    playbackManager.restorePlaybackState(
                        songs = finalQueue,
                        startIndex = savedState.activeSongIndex,
                        positionMs = savedState.positionMs,
                        shuffle = savedState.isShuffle,
                        repeat = savedState.isRepeat
                    )
                } else {
                    playbackManager.setPlaylistAndPlay(allSongs, 0, shuffle = true)
                }
            }
        }
    }

    override fun onDestroy() {
        musicScanner.stopAutoRescanWatcher()
        speedVolumeManager.stopTracking()
        playbackManager.release()
        musicDatabase.close()
        super.onDestroy()
    }
}

@Composable
fun TravelingTunesNavHost(
    playbackManager: PlaybackManager,
    musicDatabase: MusicDatabase,
    musicScanner: MusicScanner,
    settingsDataStore: SettingsDataStore,
    displaySettings: com.travelingtunes.app.core.model.DisplaySettings,
    themeSettings: com.travelingtunes.app.core.model.ThemeSettings,
    gestureBindings: Map<com.travelingtunes.app.core.model.GestureTrigger, com.travelingtunes.app.core.model.GestureBinding>,
    musicFolderUri: String?,
    musicFolderName: String?,
    lastScanTime: Long,
    firstRunPrompted: Boolean,
    libraryStats: LibraryStats,
    isScanning: Boolean,
    scanStatusMessage: String?,
    isDownloadingArt: Boolean = false,
    artDownloadStatusMessage: String? = null,
    artDownloadDownloadedCount: Int = 0,
    artDownloadFailedCount: Int = 0,
    artDownloadTotalCount: Int = 0,
    lastAuditReport: com.travelingtunes.app.core.media.AlbumArtAuditReport? = null,
    autoRescanEnabled: Boolean = false,
    autoRescanStatusMessage: String? = null,
    isAutoRescanWaiting: Boolean = false,
    onToggleAutoRescan: (Boolean) -> Unit = {},
    onPickMusicFolder: () -> Unit,
    onRescanMusicFolder: () -> Unit,
    onDownloadMissingArt: () -> Unit = {},
    onDismissFirstRunPrompt: () -> Unit
) {
    val navController = rememberNavController()
    val showFirstRunPrompt = musicFolderUri.isNullOrEmpty() && !firstRunPrompted

    NavHost(navController = navController, startDestination = "player") {
        composable("player") {
            PlayerScreen(
                playbackManager = playbackManager,
                musicDatabase = musicDatabase,
                displaySettings = displaySettings,
                gestureBindings = gestureBindings,
                musicScanner = musicScanner,
                settingsDataStore = settingsDataStore,
                themeSettings = themeSettings,
                showFirstRunPrompt = showFirstRunPrompt,
                musicFolderName = musicFolderName,
                lastScanTime = lastScanTime,
                libraryStats = libraryStats,
                isScanning = isScanning,
                scanStatusMessage = scanStatusMessage,
                isDownloadingArt = isDownloadingArt,
                artDownloadStatusMessage = artDownloadStatusMessage,
                artDownloadDownloadedCount = artDownloadDownloadedCount,
                artDownloadFailedCount = artDownloadFailedCount,
                artDownloadTotalCount = artDownloadTotalCount,
                lastAuditReport = lastAuditReport,
                onDismissFirstRunPrompt = onDismissFirstRunPrompt,
                onPickMusicFolder = onPickMusicFolder,
                onRescanMusicFolder = onRescanMusicFolder,
                onDownloadMissingArt = onDownloadMissingArt,
                onOpenSettings = { navController.navigate("settings") },
                onOpenQuickStart = { navController.navigate("quickstart") },
                onOpenGestureAssignments = { navController.navigate("gesture_assignments") },
                onOpenDownloadedArtBrowser = { navController.navigate("downloaded_art_browser") }
            )
        }
        composable("settings") {
            SettingsScreen(
                settingsDataStore = settingsDataStore,
                displaySettings = displaySettings,
                themeSettings = themeSettings,
                musicFolderName = musicFolderName,
                lastScanTime = lastScanTime,
                libraryStats = libraryStats,
                isScanning = isScanning,
                scanStatusMessage = scanStatusMessage,
                isDownloadingArt = isDownloadingArt,
                artDownloadStatusMessage = artDownloadStatusMessage,
                artDownloadDownloadedCount = artDownloadDownloadedCount,
                artDownloadFailedCount = artDownloadFailedCount,
                artDownloadTotalCount = artDownloadTotalCount,
                lastAuditReport = lastAuditReport,
                autoRescanEnabled = autoRescanEnabled,
                autoRescanStatusMessage = autoRescanStatusMessage,
                isAutoRescanWaiting = isAutoRescanWaiting,
                onToggleAutoRescan = onToggleAutoRescan,
                onPickMusicFolder = onPickMusicFolder,
                onRescanMusicFolder = onRescanMusicFolder,
                onDownloadMissingArt = onDownloadMissingArt,
                onCancelDownloadArt = { musicScanner.cancelDownloadArt() },
                onNavigateBack = { navController.popBackStack() },
                onOpenGestureAssignments = { navController.navigate("gesture_assignments") },
                onOpenQuickStart = { navController.navigate("quickstart") },
                onOpenDownloadedArtBrowser = { navController.navigate("downloaded_art_browser") }
            )
        }
        composable("gesture_assignments") {
            GestureAssignmentScreen(
                settingsDataStore = settingsDataStore,
                gestureBindings = gestureBindings,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("quickstart") {
            QuickStartScreen(
                onDone = { navController.popBackStack() }
            )
        }
        composable("downloaded_art_browser") {
            DownloadedArtBrowserScreen(
                musicDatabase = musicDatabase,
                albumArtDownloader = musicScanner.albumArtDownloader,
                playbackManager = playbackManager,
                musicScanner = musicScanner,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
