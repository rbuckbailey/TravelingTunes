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
import com.travelingtunes.app.core.location.TtsNavigationManager
import com.travelingtunes.app.core.media.MediaStoreRepository
import com.travelingtunes.app.core.media.MusicScanner
import com.travelingtunes.app.core.media.PlaybackManager
import com.travelingtunes.app.core.theme.TravelingTunesTheme
import com.travelingtunes.app.feature.contacts.ContactsPickerScreen
import com.travelingtunes.app.feature.player.PlayerScreen
import com.travelingtunes.app.feature.quickstart.QuickStartScreen
import com.travelingtunes.app.feature.settings.GestureAssignmentScreen
import com.travelingtunes.app.feature.settings.SettingsScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var playbackManager: PlaybackManager
    private lateinit var mediaStoreRepository: MediaStoreRepository
    private lateinit var speedVolumeManager: SpeedVolumeManager
    private lateinit var ttsNavigationManager: TtsNavigationManager
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
        ttsNavigationManager = TtsNavigationManager(applicationContext)
        musicDatabase = MusicDatabase(applicationContext)
        musicScanner = MusicScanner(applicationContext, musicDatabase)
        playbackManager = PlaybackManager(applicationContext, settingsDataStore)

        requestRequiredPermissions()

        setContent {
            val displaySettings by settingsDataStore.displaySettingsFlow.collectAsState(initial = com.travelingtunes.app.core.model.DisplaySettings())
            val themeSettings by settingsDataStore.themeSettingsFlow.collectAsState(initial = com.travelingtunes.app.core.model.ThemeSettings())
            val gestureBindings by settingsDataStore.gestureBindingsFlow.collectAsState(initial = emptyMap())
            val homeAddress by settingsDataStore.homeAddressFlow.collectAsState(initial = "")
            val workAddress by settingsDataStore.workAddressFlow.collectAsState(initial = "")
            val gpsVolumeEnabled by settingsDataStore.gpsVolumeEnabledFlow.collectAsState(initial = false)
            val gpsSensitivity by settingsDataStore.gpsSensitivityFlow.collectAsState(initial = 0.5f)

            val musicFolderUri by settingsDataStore.musicFolderUriFlow.collectAsState(initial = null)
            val musicFolderName by settingsDataStore.musicFolderNameFlow.collectAsState(initial = null)
            val lastScanTime by settingsDataStore.lastScanTimeFlow.collectAsState(initial = 0L)
            val firstRunPrompted by settingsDataStore.firstRunPromptedFlow.collectAsState(initial = false)

            val isScanning by musicScanner.isScanning.collectAsState()
            val scanStatusMessage by musicScanner.statusMessage.collectAsState()

            var libraryStats by remember { mutableStateOf(LibraryStats()) }

            LaunchedEffect(lastScanTime, isScanning) {
                libraryStats = musicDatabase.getLibraryStats()
            }

            LaunchedEffect(gpsVolumeEnabled, gpsSensitivity) {
                if (gpsVolumeEnabled) {
                    speedVolumeManager.startTracking(gpsSensitivity)
                } else {
                    speedVolumeManager.stopTracking()
                }
            }

            TravelingTunesTheme(
                themeSettings = themeSettings,
                useAlbumArtColors = displaySettings.albumArtColors
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
                        homeAddress = homeAddress,
                        workAddress = workAddress,
                        musicFolderUri = musicFolderUri,
                        musicFolderName = musicFolderName,
                        lastScanTime = lastScanTime,
                        firstRunPrompted = firstRunPrompted,
                        libraryStats = libraryStats,
                        isScanning = isScanning,
                        scanStatusMessage = scanStatusMessage,
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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_CONTACTS)
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
        speedVolumeManager.stopTracking()
        ttsNavigationManager.release()
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
    homeAddress: String,
    workAddress: String,
    musicFolderUri: String?,
    musicFolderName: String?,
    lastScanTime: Long,
    firstRunPrompted: Boolean,
    libraryStats: LibraryStats,
    isScanning: Boolean,
    scanStatusMessage: String?,
    onPickMusicFolder: () -> Unit,
    onRescanMusicFolder: () -> Unit,
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
                themeSettings = themeSettings,
                showFirstRunPrompt = showFirstRunPrompt,
                onDismissFirstRunPrompt = onDismissFirstRunPrompt,
                onPickMusicFolder = onPickMusicFolder,
                onOpenSettings = { navController.navigate("settings") },
                onOpenQuickStart = { navController.navigate("quickstart") },
                onOpenContacts = { navController.navigate("contacts") }
            )
        }
        composable("settings") {
            SettingsScreen(
                settingsDataStore = settingsDataStore,
                displaySettings = displaySettings,
                themeSettings = themeSettings,
                homeAddress = homeAddress,
                workAddress = workAddress,
                musicFolderName = musicFolderName,
                lastScanTime = lastScanTime,
                libraryStats = libraryStats,
                isScanning = isScanning,
                scanStatusMessage = scanStatusMessage,
                onPickMusicFolder = onPickMusicFolder,
                onRescanMusicFolder = onRescanMusicFolder,
                onNavigateBack = { navController.popBackStack() },
                onOpenGestureAssignments = { navController.navigate("gesture_assignments") },
                onOpenQuickStart = { navController.navigate("quickstart") },
                onOpenContacts = { navController.navigate("contacts") }
            )
        }
        composable("gesture_assignments") {
            GestureAssignmentScreen(
                settingsDataStore = settingsDataStore,
                gestureBindings = gestureBindings,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("contacts") {
            ContactsPickerScreen(
                settingsDataStore = settingsDataStore,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("quickstart") {
            QuickStartScreen(
                onDone = { navController.popBackStack() }
            )
        }
    }
}
