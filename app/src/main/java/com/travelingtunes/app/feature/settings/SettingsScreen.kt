package com.travelingtunes.app.feature.settings

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.travelingtunes.app.core.database.MusicDatabase
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import com.travelingtunes.app.core.model.NormalizationMode
import com.travelingtunes.app.core.model.NormalizationSettings
import com.travelingtunes.app.core.model.NormalizationSummary
import com.travelingtunes.app.core.model.Song
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelingtunes.app.core.database.LibraryStats
import com.travelingtunes.app.core.datastore.SettingsDataStore
import com.travelingtunes.app.core.media.AlbumArtAuditReport
import com.travelingtunes.app.core.media.MusicScanner
import kotlinx.coroutines.flow.flowOf
import com.travelingtunes.app.core.model.ArtAlignmentLandscape
import com.travelingtunes.app.core.model.ArtAlignmentPortrait
import com.travelingtunes.app.core.model.ArtColorPriority
import com.travelingtunes.app.core.model.ArtLayoutOption
import com.travelingtunes.app.core.model.ArtScaleOption
import com.travelingtunes.app.core.model.AutoCategory
import com.travelingtunes.app.core.model.TextAlignmentOption
import com.travelingtunes.app.core.model.TitleRowType
import com.travelingtunes.app.core.model.ColorTheme
import com.travelingtunes.app.core.model.DisplaySettings
import com.travelingtunes.app.core.model.GestureAction
import com.travelingtunes.app.core.model.HudTypeOption
import com.travelingtunes.app.core.model.ScrubHudTypeOption
import com.travelingtunes.app.core.model.ThemeSettings
import com.travelingtunes.app.core.theme.FontHelper
import com.travelingtunes.app.core.theme.FontOption
import com.travelingtunes.app.core.model.Profile
import com.travelingtunes.app.core.model.ProfileSelectionMode
import kotlinx.coroutines.launch
import com.travelingtunes.app.core.theme.luminance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Settings Submenus enum for clean navigation and adaptive master-detail layout.
 */
enum class SettingsSubmenu(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val categoryGroup: String?
) {
    PROFILES(
        title = "Profiles",
        description = "Manage setting profiles, inheritance & action switcher",
        icon = Icons.Default.AccountCircle,
        categoryGroup = null
    ),
    LIBRARY(
        title = "Music Library",
        description = "Folder selection, rescan & album art downloads",
        icon = Icons.Default.Folder,
        categoryGroup = null
    ),
    TITLES(
        title = "Titles",
        description = "Font family, sizes, style, alignment & title row order",
        icon = Icons.Default.Title,
        categoryGroup = "Appearance"
    ),
    ART(
        title = "Art",
        description = "Album art display layout, scale mode & alignment",
        icon = Icons.Default.Image,
        categoryGroup = "Appearance"
    ),
    HUD(
        title = "HUD",
        description = "Volume/progress overlays, line thickness, rounded corners & glass effect",
        icon = Icons.Default.Tune,
        categoryGroup = "Appearance"
    ),
    THEMES(
        title = "Colors",
        description = "Dynamic album art theme & color presets",
        icon = Icons.Default.Palette,
        categoryGroup = "Appearance"
    ),
    GESTURES(
        title = "Gestures & Controls",
        description = "Touch region sensitivity & gesture action assignments",
        icon = Icons.Default.TouchApp,
        categoryGroup = "Controls"
    ),
    ANDROID_AUTO(
        title = "Android Auto",
        description = "Root category browse order & Android Auto preferences",
        icon = Icons.Default.DirectionsCar,
        categoryGroup = "Controls"
    ),
    ABOUT(
        title = "Tutorial & About",
        description = "Gesture tutorial & app information",
        icon = Icons.Default.Info,
        categoryGroup = "Controls"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsDataStore: SettingsDataStore,
    displaySettings: DisplaySettings,
    themeSettings: ThemeSettings,
    musicDatabase: MusicDatabase? = null,
    musicScanner: MusicScanner? = null,
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
    autoRescanEnabled: Boolean = false,
    autoRescanStatusMessage: String? = null,
    isAutoRescanWaiting: Boolean = false,
    cddbOverridesCount: Int = 0,
    isEmbeddingCddb: Boolean = false,
    cddbEmbeddingStatus: String? = null,
    normalizationMode: NormalizationMode = NormalizationMode.ALBUM,
    normalizationSettings: NormalizationSettings = NormalizationSettings(),
    normalizationSummary: NormalizationSummary = NormalizationSummary(),
    isAnalyzingVolume: Boolean = false,
    volumeAnalysisStatusMessage: String? = null,
    volumeAnalysisProgressCurrent: Int = 0,
    volumeAnalysisProgressTotal: Int = 0,
    onSelectNormalizationMode: (NormalizationMode) -> Unit = {},
    onUpdateNormalizationSettings: (NormalizationSettings) -> Unit = {},
    onAnalyzeVolumeLevels: (forceRescan: Boolean) -> Unit = {},
    onCancelAnalyzeVolumeLevels: () -> Unit = {},
    onPickMusicFolder: () -> Unit = {},
    onRescanMusicFolder: () -> Unit = {},
    onToggleAutoRescan: (Boolean) -> Unit = {},
    onDownloadMissingArt: () -> Unit = {},
    onCancelDownloadArt: () -> Unit = {},
    onEmbedCddbOverrides: () -> Unit = {},
    onNavigateBack: () -> Unit,
    onOpenGestureAssignments: () -> Unit,
    onOpenQuickStart: () -> Unit,
    onOpenDownloadedArtBrowser: () -> Unit = {},
    onOpenDuplicateTrackIdentifier: () -> Unit = {},
    onBackupMetadata: () -> Unit = {},
    onRestoreMetadata: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var availableFonts by remember { mutableStateOf(FontHelper.getAvailableFonts(context)) }
    var activeColorPicker by remember { mutableStateOf<String?>(null) }
    var showAuditDialog by remember { mutableStateOf(false) }
    var showNormalizationReportDialog by remember { mutableStateOf(false) }
    var reportSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var statusToastMessage by remember { mutableStateOf<String?>(null) }

    val effectiveNormalizationSettings by settingsDataStore.normalizationSettingsFlow.collectAsState(initial = normalizationSettings)
    val currentVolumeProgress by (musicScanner?.volumeAnalysisProgressCurrent ?: flowOf(volumeAnalysisProgressCurrent)).collectAsState(initial = volumeAnalysisProgressCurrent)
    val totalVolumeProgress by (musicScanner?.volumeAnalysisProgressTotal ?: flowOf(volumeAnalysisProgressTotal)).collectAsState(initial = volumeAnalysisProgressTotal)
    val currentVolumeStatusMessage by (musicScanner?.volumeAnalysisStatusMessage ?: flowOf(volumeAnalysisStatusMessage)).collectAsState(initial = volumeAnalysisStatusMessage)
    val currentIsAnalyzingVolume by (musicScanner?.isAnalyzingVolume ?: flowOf(isAnalyzingVolume)).collectAsState(initial = isAnalyzingVolume)

    var effectiveNormalizationSummary by remember { mutableStateOf(normalizationSummary) }

    LaunchedEffect(musicDatabase, effectiveNormalizationSettings, currentVolumeProgress, currentIsAnalyzingVolume) {
        musicDatabase?.let { db ->
            effectiveNormalizationSummary = db.getNormalizationSummary(effectiveNormalizationSettings)
        }
    }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val jsonString = settingsDataStore.exportSettingsToJson()
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(jsonString.toByteArray(Charsets.UTF_8))
                    }
                    withContext(Dispatchers.Main) {
                        statusToastMessage = "Settings backed up successfully."
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        statusToastMessage = "Failed to backup settings."
                    }
                }
            }
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val jsonString = context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader().readText()
                    } ?: ""
                    val success = settingsDataStore.importSettingsFromJson(jsonString)
                    withContext(Dispatchers.Main) {
                        if (success) {
                            statusToastMessage = "Settings restored successfully."
                        } else {
                            statusToastMessage = "Failed to restore settings: Invalid file format."
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        statusToastMessage = "Failed to restore settings."
                    }
                }
            }
        }
    }

    val activeMusicDatabase = musicDatabase ?: remember { MusicDatabase(context) }
    var metadataRestoreJsonString by remember { mutableStateOf<String?>(null) }
    var showMetadataRestoreDialog by remember { mutableStateOf(false) }

    val createMetadataBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val jsonString = com.travelingtunes.app.core.media.MusicMetadataBackupHelper.exportMetadataToJson(context, activeMusicDatabase)
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(jsonString.toByteArray(Charsets.UTF_8))
                    }
                    withContext(Dispatchers.Main) {
                        statusToastMessage = "Music metadata exported successfully."
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        statusToastMessage = "Failed to export metadata."
                    }
                }
            }
        }
    }

    val restoreMetadataLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val jsonString = context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader().readText()
                    } ?: ""
                    if (jsonString.isNotBlank()) {
                        withContext(Dispatchers.Main) {
                            metadataRestoreJsonString = jsonString
                            showMetadataRestoreDialog = true
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        statusToastMessage = "Failed to read metadata backup file."
                    }
                }
            }
        }
    }

    val savedSubmenuName by settingsDataStore.lastSettingsSubmenuFlow.collectAsState(initial = null)
    val selectedSubmenu = SettingsSubmenu.entries.find { it.name == savedSubmenuName }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isWideScreen = screenWidthDp >= 600

    val activeSubmenu = if (isWideScreen) (selectedSubmenu ?: SettingsSubmenu.TITLES) else selectedSubmenu

    val handleBack: () -> Unit = {
        if (activeColorPicker != null) {
            activeColorPicker = null
        } else if (showAuditDialog) {
            showAuditDialog = false
        } else if (selectedSubmenu != null) {
            coroutineScope.launch {
                settingsDataStore.setLastSettingsSubmenu(null)
            }
        } else {
            onNavigateBack()
        }
    }

    BackHandler(enabled = true, onBack = handleBack)

    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val importedOption = FontHelper.importFontFile(context, uri)
                if (importedOption != null) {
                    val updatedFonts = FontHelper.getAvailableFonts(context)
                    launch(Dispatchers.Main) {
                        availableFonts = updatedFonts
                    }
                }
            }
        }
    }

    val standaloneSubmenus = remember { SettingsSubmenu.entries.filter { it.categoryGroup == null } }
    val groupedSubmenus = remember { SettingsSubmenu.entries.filter { it.categoryGroup != null }.groupBy { it.categoryGroup!! } }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Consume taps on the menu layout so they do not pass through or exit the menu
            },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (!isWideScreen && activeSubmenu != null) activeSubmenu.title else "Traveling Tunes Settings"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Exit to Play Screen")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isWideScreen) {
            // Adaptive Two-Pane Layout for Wide Screens
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Master Submenu List (Left Pane)
                Column(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Standalone Items on Top (No Category Header Label)
                    standaloneSubmenus.forEach { submenu ->
                        val isSelected = activeSubmenu == submenu
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch {
                                        settingsDataStore.setLastSettingsSubmenu(submenu.name)
                                    }
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = submenu.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = submenu.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Grouped Submenus
                    groupedSubmenus.forEach { (groupName, submenus) ->
                        Text(
                            text = groupName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp, start = 4.dp)
                        )
                        submenus.forEach { submenu ->
                            val isSelected = activeSubmenu == submenu
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        coroutineScope.launch {
                                            settingsDataStore.setLastSettingsSubmenu(submenu.name)
                                        }
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = submenu.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = submenu.title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                VerticalDivider()

                // Detail Content (Right Pane) with Slide Animation
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(20.dp)
                ) {
                    AnimatedContent(
                        targetState = activeSubmenu ?: SettingsSubmenu.LIBRARY,
                        transitionSpec = {
                            (slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }) + fadeIn()).togetherWith(
                                slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth / 2 }) + fadeOut()
                            )
                        },
                        label = "SettingsDetailSubmenuTransition"
                    ) { targetSubmenu ->
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            SubmenuContent(
                                submenu = targetSubmenu,
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
                                cddbOverridesCount = cddbOverridesCount,
                                isEmbeddingCddb = isEmbeddingCddb,
                                cddbEmbeddingStatus = cddbEmbeddingStatus,
                                musicScanner = musicScanner,
                                normalizationMode = normalizationMode,
                                normalizationSettings = effectiveNormalizationSettings,
                                normalizationSummary = effectiveNormalizationSummary,
                                isAnalyzingVolume = currentIsAnalyzingVolume,
                                volumeAnalysisStatusMessage = currentVolumeStatusMessage,
                                volumeAnalysisProgressCurrent = currentVolumeProgress,
                                volumeAnalysisProgressTotal = totalVolumeProgress,
                                onSelectNormalizationMode = onSelectNormalizationMode,
                                onUpdateNormalizationSettings = { newSettings ->
                                    coroutineScope.launch {
                                        settingsDataStore.setNormalizationSettings(newSettings)
                                    }
                                },
                                onOpenNormalizationReport = {
                                    coroutineScope.launch {
                                        reportSongs = musicDatabase?.getAllSongs() ?: emptyList()
                                        showNormalizationReportDialog = true
                                    }
                                },
                                onAnalyzeVolumeLevels = onAnalyzeVolumeLevels,
                                onCancelAnalyzeVolumeLevels = onCancelAnalyzeVolumeLevels,
                                onToggleAutoRescan = onToggleAutoRescan,
                                availableFonts = availableFonts,
                                onPickMusicFolder = onPickMusicFolder,
                                onRescanMusicFolder = onRescanMusicFolder,
                                onDownloadMissingArt = onDownloadMissingArt,
                                onCancelDownloadArt = onCancelDownloadArt,
                                onEmbedCddbOverrides = onEmbedCddbOverrides,
                                onViewAudit = { showAuditDialog = true },
                                onOpenDownloadedArtBrowser = onOpenDownloadedArtBrowser,
                                onOpenDuplicateTrackIdentifier = onOpenDuplicateTrackIdentifier,
                                onBackupMetadata = { createMetadataBackupLauncher.launch("traveling_tunes_metadata_backup.json") },
                                onRestoreMetadata = { restoreMetadataLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                                onAddFont = { fontPickerLauncher.launch(arrayOf("*/*")) },
                                onUpdateDisplaySettings = { newSettings ->
                                    coroutineScope.launch {
                                        settingsDataStore.updateDisplaySettings(newSettings)
                                    }
                                },
                                onOpenGestureAssignments = onOpenGestureAssignments,
                                onResetGestureAssignments = {
                                    coroutineScope.launch {
                                        settingsDataStore.resetAllGestureBindings()
                                    }
                                },
                                onSelectPreset = { presetName, isAuto ->
                                    coroutineScope.launch {
                                        settingsDataStore.updateThemeSettings(themeSettings.copy(currentThemeName = presetName))
                                        settingsDataStore.updateDisplaySettings(displaySettings.copy(albumArtColors = isAuto))
                                    }
                                },
                                onUpdateThemeSettings = { newTheme ->
                                    coroutineScope.launch {
                                        settingsDataStore.updateThemeSettings(newTheme)
                                    }
                                },
                                onOpenBgPicker = { activeColorPicker = "BG" },
                                onOpenSongPicker = { activeColorPicker = "SONG" },
                                onOpenArtistPicker = { activeColorPicker = "ARTIST" },
                                onOpenAlbumPicker = { activeColorPicker = "ALBUM" },
                                onOpenQuickStart = onOpenQuickStart,
                                onBackupSettings = { createBackupLauncher.launch("traveling_tunes_settings.json") },
                                onRestoreSettings = { restoreBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
                            )
                        }
                    }
                }
            }
        } else {
            // Single-Pane Navigation with Slide-In from Right Animation
            AnimatedContent(
                targetState = activeSubmenu,
                transitionSpec = {
                    if (targetState != null) {
                        (slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }) + fadeIn()).togetherWith(
                            slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth / 3 }) + fadeOut()
                        )
                    } else {
                        (slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth / 3 }) + fadeIn()).togetherWith(
                            slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }) + fadeOut()
                        )
                    }
                },
                label = "SettingsSinglePaneTransition",
                modifier = Modifier.padding(paddingValues)
            ) { targetSubmenu ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    if (targetSubmenu == null) {
                        // Main Submenu Navigation List
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Standalone Items at the top without category label
                            standaloneSubmenus.forEach { submenu ->
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            coroutineScope.launch {
                                                settingsDataStore.setLastSettingsSubmenu(submenu.name)
                                            }
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = submenu.icon,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = submenu.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = submenu.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Grouped Submenus with section headers
                            groupedSubmenus.forEach { (groupName, submenus) ->
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = groupName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                                    )
                                    submenus.forEach { submenu ->
                                        Card(
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    coroutineScope.launch {
                                                        settingsDataStore.setLastSettingsSubmenu(submenu.name)
                                                    }
                                                }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(16.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = submenu.icon,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = submenu.title,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = submenu.description,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Selected Submenu Content Page
                        SubmenuContent(
                            submenu = targetSubmenu,
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
                        cddbOverridesCount = cddbOverridesCount,
                        isEmbeddingCddb = isEmbeddingCddb,
                        cddbEmbeddingStatus = cddbEmbeddingStatus,
                        musicScanner = musicScanner,
                        normalizationMode = normalizationMode,
                        normalizationSettings = effectiveNormalizationSettings,
                        normalizationSummary = effectiveNormalizationSummary,
                        isAnalyzingVolume = currentIsAnalyzingVolume,
                        volumeAnalysisStatusMessage = currentVolumeStatusMessage,
                        volumeAnalysisProgressCurrent = currentVolumeProgress,
                        volumeAnalysisProgressTotal = totalVolumeProgress,
                        onSelectNormalizationMode = onSelectNormalizationMode,
                        onUpdateNormalizationSettings = { newSettings ->
                            coroutineScope.launch {
                                settingsDataStore.setNormalizationSettings(newSettings)
                            }
                        },
                        onOpenNormalizationReport = {
                            coroutineScope.launch {
                                reportSongs = musicDatabase?.getAllSongs() ?: emptyList()
                                showNormalizationReportDialog = true
                            }
                        },
                        onAnalyzeVolumeLevels = onAnalyzeVolumeLevels,
                        onCancelAnalyzeVolumeLevels = onCancelAnalyzeVolumeLevels,
                        onToggleAutoRescan = onToggleAutoRescan,
                        availableFonts = availableFonts,
                        onPickMusicFolder = onPickMusicFolder,
                        onRescanMusicFolder = onRescanMusicFolder,
                        onDownloadMissingArt = onDownloadMissingArt,
                        onCancelDownloadArt = onCancelDownloadArt,
                        onEmbedCddbOverrides = onEmbedCddbOverrides,
                        onViewAudit = { showAuditDialog = true },
                        onOpenDownloadedArtBrowser = onOpenDownloadedArtBrowser,
                        onOpenDuplicateTrackIdentifier = onOpenDuplicateTrackIdentifier,
                        onBackupMetadata = { createMetadataBackupLauncher.launch("traveling_tunes_metadata_backup.json") },
                        onRestoreMetadata = { restoreMetadataLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        onAddFont = { fontPickerLauncher.launch(arrayOf("*/*")) },
                        onUpdateDisplaySettings = { newSettings ->
                            coroutineScope.launch {
                                settingsDataStore.updateDisplaySettings(newSettings)
                            }
                        },
                        onOpenGestureAssignments = onOpenGestureAssignments,
                        onResetGestureAssignments = {
                            coroutineScope.launch {
                                settingsDataStore.resetAllGestureBindings()
                            }
                        },
                        onSelectPreset = { presetName, isAuto ->
                            coroutineScope.launch {
                                settingsDataStore.updateThemeSettings(themeSettings.copy(currentThemeName = presetName))
                                settingsDataStore.updateDisplaySettings(displaySettings.copy(albumArtColors = isAuto))
                            }
                        },
                        onUpdateThemeSettings = { newTheme ->
                            coroutineScope.launch {
                                settingsDataStore.updateThemeSettings(newTheme)
                            }
                        },
                        onOpenBgPicker = { activeColorPicker = "BG" },
                        onOpenSongPicker = { activeColorPicker = "SONG" },
                        onOpenArtistPicker = { activeColorPicker = "ARTIST" },
                        onOpenAlbumPicker = { activeColorPicker = "ALBUM" },
                        onOpenQuickStart = onOpenQuickStart,
                        onBackupSettings = { createBackupLauncher.launch("traveling_tunes_settings.json") },
                        onRestoreSettings = { restoreBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
                    )
                }
            }
        }
    }
}

    if (statusToastMessage != null) {
        AlertDialog(
            onDismissRequest = { statusToastMessage = null },
            title = { Text("Settings Backup & Restore") },
            text = { Text(statusToastMessage ?: "") },
            confirmButton = {
                TextButton(onClick = { statusToastMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    val currentAuditReport = lastAuditReport
    if (showAuditDialog && currentAuditReport != null) {
        AlbumArtAuditDialog(
            report = currentAuditReport,
            onDismiss = { showAuditDialog = false }
        )
    }

    if (showMetadataRestoreDialog && metadataRestoreJsonString != null) {
        MetadataRestoreDialog(
            jsonString = metadataRestoreJsonString!!,
            musicDatabase = activeMusicDatabase,
            onDismiss = {
                showMetadataRestoreDialog = false
                metadataRestoreJsonString = null
            },
            onRestoreComplete = { succ, fail ->
                showMetadataRestoreDialog = false
                metadataRestoreJsonString = null
                statusToastMessage = "Restored metadata for $succ tracks ($fail skipped)."
            }
        )
    }

    if (activeColorPicker != null) {
        val pickerType = activeColorPicker!!
        val titleText = when (pickerType) {
            "BG" -> "Background Color"
            "SONG" -> "Song Title Color"
            "ARTIST" -> "Artist Label Color"
            "ALBUM" -> "Album Label Color"
            else -> "Color Picker"
        }

        val initialRed = when (pickerType) {
            "BG" -> themeSettings.customBGRed
            "SONG" -> themeSettings.customSongTitleRed
            "ARTIST" -> themeSettings.customArtistTitleRed
            "ALBUM" -> themeSettings.customAlbumTitleRed
            else -> 128f
        }
        val initialGreen = when (pickerType) {
            "BG" -> themeSettings.customBGGreen
            "SONG" -> themeSettings.customSongTitleGreen
            "ARTIST" -> themeSettings.customArtistTitleGreen
            "ALBUM" -> themeSettings.customAlbumTitleGreen
            else -> 128f
        }
        val initialBlue = when (pickerType) {
            "BG" -> themeSettings.customBGBlue
            "SONG" -> themeSettings.customSongTitleBlue
            "ARTIST" -> themeSettings.customArtistTitleBlue
            "ALBUM" -> themeSettings.customAlbumTitleBlue
            else -> 128f
        }

        CustomColorPickerDialog(
            title = titleText,
            initialRed = initialRed,
            initialGreen = initialGreen,
            initialBlue = initialBlue,
            onDismiss = { activeColorPicker = null },
            onConfirmColor = { r, g, b ->
                coroutineScope.launch {
                    val baseTheme = when (pickerType) {
                        "BG" -> themeSettings.copy(customBGRed = r, customBGGreen = g, customBGBlue = b)
                        "SONG" -> themeSettings.copy(customSongTitleRed = r, customSongTitleGreen = g, customSongTitleBlue = b)
                        "ARTIST" -> themeSettings.copy(customArtistTitleRed = r, customArtistTitleGreen = g, customArtistTitleBlue = b)
                        "ALBUM" -> themeSettings.copy(customAlbumTitleRed = r, customAlbumTitleGreen = g, customAlbumTitleBlue = b)
                        else -> themeSettings
                    }
                    val newTheme = baseTheme.copy(currentThemeName = "Custom")
                    settingsDataStore.updateThemeSettings(newTheme)
                }
                activeColorPicker = null
            }
        )
    }
}

@Composable
private fun SubmenuContent(
    submenu: SettingsSubmenu,
    settingsDataStore: SettingsDataStore,
    displaySettings: DisplaySettings,
    themeSettings: ThemeSettings,
    musicFolderName: String?,
    lastScanTime: Long,
    libraryStats: LibraryStats,
    isScanning: Boolean,
    scanStatusMessage: String?,
    isDownloadingArt: Boolean,
    artDownloadStatusMessage: String?,
    artDownloadDownloadedCount: Int,
    artDownloadFailedCount: Int,
    artDownloadTotalCount: Int,
    lastAuditReport: AlbumArtAuditReport?,
    autoRescanEnabled: Boolean = false,
    autoRescanStatusMessage: String? = null,
    isAutoRescanWaiting: Boolean = false,
    cddbOverridesCount: Int = 0,
    isEmbeddingCddb: Boolean = false,
    cddbEmbeddingStatus: String? = null,
    musicScanner: MusicScanner? = null,
    normalizationMode: NormalizationMode = NormalizationMode.ALBUM,
    normalizationSettings: NormalizationSettings = NormalizationSettings(),
    normalizationSummary: NormalizationSummary = NormalizationSummary(),
    isAnalyzingVolume: Boolean = false,
    volumeAnalysisStatusMessage: String? = null,
    volumeAnalysisProgressCurrent: Int = 0,
    volumeAnalysisProgressTotal: Int = 0,
    onSelectNormalizationMode: (NormalizationMode) -> Unit = {},
    onUpdateNormalizationSettings: (NormalizationSettings) -> Unit = {},
    onOpenNormalizationReport: () -> Unit = {},
    onAnalyzeVolumeLevels: (forceRescan: Boolean) -> Unit = {},
    onCancelAnalyzeVolumeLevels: () -> Unit = {},
    onToggleAutoRescan: (Boolean) -> Unit = {},
    availableFonts: List<FontOption>,
    onPickMusicFolder: () -> Unit,
    onRescanMusicFolder: () -> Unit,
    onDownloadMissingArt: () -> Unit,
    onCancelDownloadArt: () -> Unit,
    onEmbedCddbOverrides: () -> Unit = {},
    onViewAudit: () -> Unit,
    onAddFont: () -> Unit,
    onUpdateDisplaySettings: (DisplaySettings) -> Unit,
    onOpenGestureAssignments: () -> Unit,
    onResetGestureAssignments: () -> Unit,
    onSelectPreset: (String, Boolean) -> Unit,
    onUpdateThemeSettings: (ThemeSettings) -> Unit,
    onOpenBgPicker: () -> Unit,
    onOpenSongPicker: () -> Unit,
    onOpenArtistPicker: () -> Unit,
    onOpenAlbumPicker: () -> Unit,
    onOpenQuickStart: () -> Unit,
    onOpenDownloadedArtBrowser: () -> Unit = {},
    onOpenDuplicateTrackIdentifier: () -> Unit = {},
    onBackupMetadata: () -> Unit = {},
    onRestoreMetadata: () -> Unit = {},
    onBackupSettings: () -> Unit = {},
    onRestoreSettings: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = submenu.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = submenu.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            when (submenu) {
                SettingsSubmenu.LIBRARY -> LibrarySettingsContent(
                    musicFolderName = musicFolderName,
                    isScanning = isScanning,
                    lastScanTime = lastScanTime,
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
                    cddbOverridesCount = cddbOverridesCount,
                    isEmbeddingCddb = isEmbeddingCddb,
                    cddbEmbeddingStatus = cddbEmbeddingStatus,
                    musicScanner = musicScanner,
                    normalizationMode = normalizationMode,
                    normalizationSettings = normalizationSettings,
                    normalizationSummary = normalizationSummary,
                    isAnalyzingVolume = isAnalyzingVolume,
                    volumeAnalysisStatusMessage = volumeAnalysisStatusMessage,
                    volumeAnalysisProgressCurrent = volumeAnalysisProgressCurrent,
                    volumeAnalysisProgressTotal = volumeAnalysisProgressTotal,
                    onSelectNormalizationMode = onSelectNormalizationMode,
                    onUpdateNormalizationSettings = onUpdateNormalizationSettings,
                    onOpenNormalizationReport = onOpenNormalizationReport,
                    onAnalyzeVolumeLevels = onAnalyzeVolumeLevels,
                    onCancelAnalyzeVolumeLevels = onCancelAnalyzeVolumeLevels,
                    onToggleAutoRescan = onToggleAutoRescan,
                    libraryStats = libraryStats,
                    onPickMusicFolder = onPickMusicFolder,
                    onRescanMusicFolder = onRescanMusicFolder,
                    onDownloadMissingArt = onDownloadMissingArt,
                    onCancelDownloadArt = onCancelDownloadArt,
                    onEmbedCddbOverrides = onEmbedCddbOverrides,
                    onViewAudit = onViewAudit,
                    onOpenDownloadedArtBrowser = onOpenDownloadedArtBrowser,
                    onOpenDuplicateTrackIdentifier = onOpenDuplicateTrackIdentifier,
                    onBackupMetadata = onBackupMetadata,
                    onRestoreMetadata = onRestoreMetadata,
                    onBackupSettings = onBackupSettings,
                    onRestoreSettings = onRestoreSettings
                )

                SettingsSubmenu.TITLES -> TitlesSettingsContent(
                    displaySettings = displaySettings,
                    availableFonts = availableFonts,
                    onAddFont = onAddFont,
                    onUpdateDisplaySettings = onUpdateDisplaySettings
                )

                SettingsSubmenu.ART -> ArtSettingsContent(
                    displaySettings = displaySettings,
                    onUpdateDisplaySettings = onUpdateDisplaySettings
                )

                SettingsSubmenu.HUD -> HudSettingsContent(
                    displaySettings = displaySettings,
                    themeSettings = themeSettings,
                    onUpdateDisplaySettings = onUpdateDisplaySettings,
                    onUpdateThemeSettings = onUpdateThemeSettings
                )

                SettingsSubmenu.THEMES -> ThemesSettingsContent(
                    themeSettings = themeSettings,
                    displaySettings = displaySettings,
                    onUpdateDisplaySettings = onUpdateDisplaySettings,
                    onSelectPreset = onSelectPreset,
                    onOpenBgPicker = onOpenBgPicker,
                    onOpenSongPicker = onOpenSongPicker,
                    onOpenArtistPicker = onOpenArtistPicker,
                    onOpenAlbumPicker = onOpenAlbumPicker
                )

                SettingsSubmenu.ANDROID_AUTO -> AndroidAutoSettingsContent(
                    displaySettings = displaySettings,
                    onUpdateDisplaySettings = onUpdateDisplaySettings
                )

                SettingsSubmenu.GESTURES -> GesturesSettingsContent(
                    displaySettings = displaySettings,
                    onUpdateDisplaySettings = onUpdateDisplaySettings,
                    onOpenGestureAssignments = onOpenGestureAssignments,
                    onResetGestureAssignments = onResetGestureAssignments
                )

                SettingsSubmenu.PROFILES -> ProfilesSettingsContent(
                    settingsDataStore = settingsDataStore
                )

                SettingsSubmenu.ABOUT -> AboutSettingsContent(
                    onOpenQuickStart = onOpenQuickStart
                )
            }
        }
    }
}

@Composable
private fun LibrarySettingsContent(
    musicFolderName: String?,
    isScanning: Boolean,
    lastScanTime: Long,
    scanStatusMessage: String?,
    libraryStats: LibraryStats,
    isDownloadingArt: Boolean = false,
    artDownloadStatusMessage: String? = null,
    artDownloadDownloadedCount: Int = 0,
    artDownloadFailedCount: Int = 0,
    artDownloadTotalCount: Int = 0,
    lastAuditReport: AlbumArtAuditReport? = null,
    autoRescanEnabled: Boolean = false,
    autoRescanStatusMessage: String? = null,
    isAutoRescanWaiting: Boolean = false,
    cddbOverridesCount: Int = 0,
    isEmbeddingCddb: Boolean = false,
    cddbEmbeddingStatus: String? = null,
    musicScanner: MusicScanner? = null,
    normalizationMode: NormalizationMode = NormalizationMode.ALBUM,
    normalizationSettings: NormalizationSettings = NormalizationSettings(),
    normalizationSummary: NormalizationSummary = NormalizationSummary(),
    isAnalyzingVolume: Boolean = false,
    volumeAnalysisStatusMessage: String? = null,
    volumeAnalysisProgressCurrent: Int = 0,
    volumeAnalysisProgressTotal: Int = 0,
    onSelectNormalizationMode: (NormalizationMode) -> Unit = {},
    onUpdateNormalizationSettings: (NormalizationSettings) -> Unit = {},
    onOpenNormalizationReport: () -> Unit = {},
    onAnalyzeVolumeLevels: (forceRescan: Boolean) -> Unit = {},
    onCancelAnalyzeVolumeLevels: () -> Unit = {},
    onToggleAutoRescan: (Boolean) -> Unit = {},
    onPickMusicFolder: () -> Unit,
    onRescanMusicFolder: () -> Unit,
    onDownloadMissingArt: () -> Unit = {},
    onCancelDownloadArt: () -> Unit = {},
    onEmbedCddbOverrides: () -> Unit = {},
    onViewAudit: () -> Unit = {},
    onOpenDownloadedArtBrowser: () -> Unit = {},
    onOpenDuplicateTrackIdentifier: () -> Unit = {},
    onBackupMetadata: () -> Unit = {},
    onRestoreMetadata: () -> Unit = {},
    onBackupSettings: () -> Unit = {},
    onRestoreSettings: () -> Unit = {}
) {
    Column {
        ListItem(
            headlineContent = { Text("Library Folder") },
            supportingContent = {
                Text(
                    if (!musicFolderName.isNullOrBlank()) musicFolderName
                    else "No folder selected. Tap to choose folder."
                )
            },
            modifier = Modifier.clickable { onPickMusicFolder() }
        )
        ListItem(
            headlineContent = { Text("Automatically Re-scan Library") },
            supportingContent = {
                if (!autoRescanStatusMessage.isNullOrBlank()) {
                    Text(
                        autoRescanStatusMessage,
                        color = if (isAutoRescanWaiting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text("Monitor folder for file changes and auto-rescan after changes complete")
                }
            },
            trailingContent = {
                Switch(
                    checked = autoRescanEnabled,
                    onCheckedChange = { onToggleAutoRescan(it) }
                )
            }
        )
        ListItem(
            headlineContent = { Text("Rescan Music Folder") },
            supportingContent = {
                if (isScanning) {
                    Text(scanStatusMessage ?: "Scanning...", color = MaterialTheme.colorScheme.primary)
                } else if (lastScanTime > 0) {
                    val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(lastScanTime))
                    Text("Last scanned: $dateStr")
                } else {
                    Text("Tap to scan selected folder for music")
                }
            },
            modifier = Modifier.clickable(enabled = !isScanning && !musicFolderName.isNullOrBlank()) {
                onRescanMusicFolder()
            }
        )

        if (isDownloadingArt) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Downloading Missing Artwork...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        TextButton(onClick = onCancelDownloadArt) {
                            Text("Stop", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    ArtDownloadProgressBar(
                        downloadedCount = artDownloadDownloadedCount,
                        failedCount = artDownloadFailedCount,
                        totalCount = artDownloadTotalCount
                    )
                    if (!artDownloadStatusMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = artDownloadStatusMessage ?: "",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        } else {
            ListItem(
                headlineContent = { Text("Download Missing Album Art") },
                supportingContent = {
                    Text("Search and download high-res square artwork for missing albums")
                },
                modifier = Modifier.clickable { onDownloadMissingArt() }
            )
        }

        ListItem(
            headlineContent = { Text("Album Art Editor") },
            supportingContent = {
                Text("Manage, replace, or embed downloaded album artwork into ID3 tags")
            },
            modifier = Modifier.clickable { onOpenDownloadedArtBrowser() }
        )

        ListItem(
            headlineContent = { Text("Duplicate Track Identifier") },
            supportingContent = {
                Text("Compare tracks by file name, size, and metadata to find & remove duplicates")
            },
            modifier = Modifier.clickable { onOpenDuplicateTrackIdentifier() }
        )

        if (lastAuditReport != null) {
            ListItem(
                headlineContent = { Text("View / Share Download Audit Log") },
                supportingContent = {
                    Text("Downloaded ${lastAuditReport.successCount} of ${lastAuditReport.totalProcessed} albums (${lastAuditReport.failedCount} missing)")
                },
                modifier = Modifier.clickable { onViewAudit() }
            )
        }

        if (cddbOverridesCount > 0) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "CDDB Track Overrides",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$cddbOverridesCount tracks ordered using CDDB metadata",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onEmbedCddbOverrides,
                        enabled = !isEmbeddingCddb
                    ) {
                        Text(
                            text = if (isEmbeddingCddb) (cddbEmbeddingStatus ?: "Embedding ID3 tags...") else "Embed CDDB Overrides as ID3 Tags",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

                Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Volume Normalization",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Automatically adjusts song volume so quiet tracks are easy to hear and loud tracks don't blast your speakers.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                NormalizationMode.entries.forEach { mode ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectNormalizationMode(mode) }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = (normalizationMode == mode),
                            onClick = { onSelectNormalizationMode(mode) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = mode.displayName,
                                fontWeight = if (normalizationMode == mode) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                            val desc = when (mode) {
                                NormalizationMode.ALBUM -> "Matches volume across albums. Quiet intros stay quiet and big climaxes stay loud."
                                NormalizationMode.TRACK -> "Matches every song to the exact same average loudness."
                                NormalizationMode.OFF -> "Plays original recorded file volume with no changes."
                            }
                            Text(text = desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text(
                    text = "Fine-Tune Volume Targets",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Slider 1: Target Loudness
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Target Loudness", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "${(normalizationSettings.targetRms * 100).toInt()}% Loudness", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Text(text = "How loud songs should play on average.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                var sliderTargetRms by remember(normalizationSettings.targetRms) { mutableFloatStateOf(normalizationSettings.targetRms) }
                Slider(
                    value = sliderTargetRms,
                    onValueChange = { sliderTargetRms = it },
                    onValueChangeFinished = { onUpdateNormalizationSettings(normalizationSettings.copy(targetRms = sliderTargetRms)) },
                    valueRange = 0.08f..0.25f,
                    modifier = Modifier.fillMaxWidth()
                )

                // Slider 2: Max Loud Peak Limit
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Max Loud Peak Limit", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "${(normalizationSettings.maxPeak * 100).toInt()}% Max Peak", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Text(text = "Safety limit to prevent loud sound spikes from distorting.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                var sliderMaxPeak by remember(normalizationSettings.maxPeak) { mutableFloatStateOf(normalizationSettings.maxPeak) }
                Slider(
                    value = sliderMaxPeak,
                    onValueChange = { sliderMaxPeak = it },
                    onValueChangeFinished = { onUpdateNormalizationSettings(normalizationSettings.copy(maxPeak = sliderMaxPeak)) },
                    valueRange = 0.90f..0.99f,
                    modifier = Modifier.fillMaxWidth()
                )

                // Slider 3: Max Volume Boost
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Max Volume Boost", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "${"%.1f".format(normalizationSettings.maxGainBoost)}x Boost", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Text(text = "The most an extra-quiet song can be boosted.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                var sliderMaxGainBoost by remember(normalizationSettings.maxGainBoost) { mutableFloatStateOf(normalizationSettings.maxGainBoost) }
                Slider(
                    value = sliderMaxGainBoost,
                    onValueChange = { sliderMaxGainBoost = it },
                    onValueChangeFinished = { onUpdateNormalizationSettings(normalizationSettings.copy(maxGainBoost = sliderMaxGainBoost)) },
                    valueRange = 1.5f..6.0f,
                    modifier = Modifier.fillMaxWidth()
                )

                // Switch: Scan Whole Song
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onUpdateNormalizationSettings(normalizationSettings.copy(fullScanEnabled = !normalizationSettings.fullScanEnabled))
                        }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Scan Whole Song", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "Scans the whole song instead of just the first 20 seconds. Takes longer, but gives exact volume for songs with quiet starts.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = normalizationSettings.fullScanEnabled,
                        onCheckedChange = { checked ->
                            onUpdateNormalizationSettings(normalizationSettings.copy(fullScanEnabled = checked))
                        }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                if (isAnalyzingVolume) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            val progress = if (volumeAnalysisProgressTotal > 0) {
                                (volumeAnalysisProgressCurrent.toFloat() / volumeAnalysisProgressTotal.toFloat()).coerceIn(0f, 1f)
                            } else 0f
                            val pct = (progress * 100).toInt()

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Analyzing Volume ($pct%)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Text(
                                    text = "$volumeAnalysisProgressCurrent / $volumeAnalysisProgressTotal",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                            if (!volumeAnalysisStatusMessage.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = volumeAnalysisStatusMessage ?: "",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Summary Overview Pill Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Normalization Summary",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Analyzed: ${normalizationSummary.analyzedSongs} of ${normalizationSummary.totalSongs} songs (${if (normalizationSummary.totalSongs > 0) normalizationSummary.analyzedSongs * 100 / normalizationSummary.totalSongs else 0}%)",
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Avg RMS: ${"%.3f".format(normalizationSummary.avgRms)} • Peak Limited: ${normalizationSummary.peakLimitedCount} tracks",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Gain Range: ${"%.2f".format(normalizationSummary.minGain)}x – ${"%.2f".format(normalizationSummary.maxGain)}x",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val isPartialScan = normalizationSummary.analyzedSongs > 0 && normalizationSummary.analyzedSongs < normalizationSummary.totalSongs
                val isAllAnalyzed = normalizationSummary.totalSongs > 0 && normalizationSummary.analyzedSongs == normalizationSummary.totalSongs

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onOpenNormalizationReport) {
                        Text("View Report", fontWeight = FontWeight.Bold)
                    }

                    if (isAnalyzingVolume) {
                        TextButton(onClick = onCancelAnalyzeVolumeLevels) {
                            Text("Pause Scan", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        val buttonText = when {
                            isPartialScan -> "Resume Volume Scan"
                            isAllAnalyzed -> "Re-analyze Volume"
                            else -> "Analyze Volume"
                        }
                        val forceRescan = isAllAnalyzed

                        TextButton(
                            onClick = { onAnalyzeVolumeLevels(forceRescan) }
                        ) {
                            Text(
                                text = buttonText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Library Statistics", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Text("${libraryStats.totalSongs} Songs • ${libraryStats.totalAlbums} Albums")
                Text("${libraryStats.totalArtists} Artists • ${libraryStats.totalGenres} Genres")
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Backup & Restore",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Backup app settings or export ID3 metadata (genre, album art, ratings) to restore across devices.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("App Settings", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onBackupSettings,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Backup Settings")
                    }
                    OutlinedButton(
                        onClick = onRestoreSettings,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Restore Settings")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("ID3 Music Metadata & Artwork", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onBackupMetadata,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Export Metadata")
                    }
                    OutlinedButton(
                        onClick = onRestoreMetadata,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Restore Metadata")
                    }
                }
            }
        }
    }
}

@Composable
private fun TitlesSettingsContent(
    displaySettings: DisplaySettings,
    availableFonts: List<FontOption>,
    onAddFont: () -> Unit,
    onUpdateDisplaySettings: (DisplaySettings) -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Font & Label Styling", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
            OutlinedButton(
                onClick = onAddFont,
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Font", fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Reorder title rows and customize font family, size & alignment for each",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))

        val currentOrder = displaySettings.titleOrder
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                currentOrder.forEachIndexed { index, rowType ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            val (currentFontKey, isBold, isItalic, isUnderline) = when (rowType) {
                                TitleRowType.ARTIST -> Quadruple(displaySettings.artistFontKey, displaySettings.artistBold, displaySettings.artistItalic, displaySettings.artistUnderline)
                                TitleRowType.SONG -> Quadruple(displaySettings.songFontKey, displaySettings.songBold, displaySettings.songItalic, displaySettings.songUnderline)
                                TitleRowType.ALBUM -> Quadruple(displaySettings.albumFontKey, displaySettings.albumBold, displaySettings.albumItalic, displaySettings.albumUnderline)
                            }
                            val currentFontFamily = FontHelper.getFontFamily(currentFontKey)

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = "Drag handle",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "${index + 1}. ${rowType.displayName}",
                                    fontFamily = currentFontFamily,
                                    fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                                    fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                                    textDecoration = if (isUnderline) TextDecoration.Underline else TextDecoration.None,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )

                                if (index > 0) {
                                    IconButton(
                                        onClick = {
                                            val newOrder = currentOrder.toMutableList()
                                            val temp = newOrder[index]
                                            newOrder[index] = newOrder[index - 1]
                                            newOrder[index - 1] = temp
                                            onUpdateDisplaySettings(displaySettings.copy(titleOrder = newOrder))
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up")
                                    }
                                }
                                if (index < currentOrder.size - 1) {
                                    IconButton(
                                        onClick = {
                                            val newOrder = currentOrder.toMutableList()
                                            val temp = newOrder[index]
                                            newOrder[index] = newOrder[index + 1]
                                            newOrder[index + 1] = temp
                                            onUpdateDisplaySettings(displaySettings.copy(titleOrder = newOrder))
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down")
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FontSelectorRow(
                                    currentFontKey = currentFontKey,
                                    availableFonts = availableFonts,
                                    onFontSelected = { key ->
                                        val newSettings = when (rowType) {
                                            TitleRowType.ARTIST -> displaySettings.copy(artistFontKey = key)
                                            TitleRowType.SONG -> displaySettings.copy(songFontKey = key)
                                            TitleRowType.ALBUM -> displaySettings.copy(albumFontKey = key)
                                        }
                                        onUpdateDisplaySettings(newSettings)
                                    },
                                    modifier = Modifier.weight(1f)
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    FilterChip(
                                        selected = isBold,
                                        onClick = {
                                            val newSettings = when (rowType) {
                                                TitleRowType.ARTIST -> displaySettings.copy(artistBold = !isBold)
                                                TitleRowType.SONG -> displaySettings.copy(songBold = !isBold)
                                                TitleRowType.ALBUM -> displaySettings.copy(albumBold = !isBold)
                                            }
                                            onUpdateDisplaySettings(newSettings)
                                        },
                                        label = { Text("Bold", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                                    )
                                    FilterChip(
                                        selected = isItalic,
                                        onClick = {
                                            val newSettings = when (rowType) {
                                                TitleRowType.ARTIST -> displaySettings.copy(artistItalic = !isItalic)
                                                TitleRowType.SONG -> displaySettings.copy(songItalic = !isItalic)
                                                TitleRowType.ALBUM -> displaySettings.copy(albumItalic = !isItalic)
                                            }
                                            onUpdateDisplaySettings(newSettings)
                                        },
                                        label = { Text("Italic", fontStyle = FontStyle.Italic, fontSize = 11.sp) }
                                    )
                                    FilterChip(
                                        selected = isUnderline,
                                        onClick = {
                                            val newSettings = when (rowType) {
                                                TitleRowType.ARTIST -> displaySettings.copy(artistUnderline = !isUnderline)
                                                TitleRowType.SONG -> displaySettings.copy(songUnderline = !isUnderline)
                                                TitleRowType.ALBUM -> displaySettings.copy(albumUnderline = !isUnderline)
                                            }
                                            onUpdateDisplaySettings(newSettings)
                                        },
                                        label = { Text("Underline", textDecoration = TextDecoration.Underline, fontSize = 11.sp) }
                                    )
                                }
                            }

                            val (currentSize, minRange, maxRange) = when (rowType) {
                                TitleRowType.ARTIST -> Triple(displaySettings.artistFontSize, 20f, 100f)
                                TitleRowType.SONG -> Triple(displaySettings.songFontSize, 30f, 120f)
                                TitleRowType.ALBUM -> Triple(displaySettings.albumFontSize, 20f, 100f)
                            }
                            val currentAlign = when (rowType) {
                                TitleRowType.ARTIST -> displaySettings.artistAlignment
                                TitleRowType.SONG -> displaySettings.songAlignment
                                TitleRowType.ALBUM -> displaySettings.albumAlignment
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "${currentSize.toInt()} pt",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.width(42.dp)
                                )
                                Slider(
                                    value = currentSize,
                                    onValueChange = { newSize ->
                                        val newSettings = when (rowType) {
                                            TitleRowType.ARTIST -> displaySettings.copy(artistFontSize = newSize)
                                            TitleRowType.SONG -> displaySettings.copy(songFontSize = newSize)
                                            TitleRowType.ALBUM -> displaySettings.copy(albumFontSize = newSize)
                                        }
                                        onUpdateDisplaySettings(newSettings)
                                    },
                                    valueRange = minRange..maxRange,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextAlignmentOption.entries.forEach { option ->
                                        FilterChip(
                                            selected = currentAlign == option,
                                            onClick = {
                                                val newSettings = when (rowType) {
                                                    TitleRowType.ARTIST -> displaySettings.copy(artistAlignment = option)
                                                    TitleRowType.SONG -> displaySettings.copy(songAlignment = option)
                                                    TitleRowType.ALBUM -> displaySettings.copy(albumAlignment = option)
                                                }
                                                onUpdateDisplaySettings(newSettings)
                                            },
                                            label = { Text(option.displayName, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Title Display Mode", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
        ) {
            FilterChip(
                selected = !displaySettings.titleScrollLong,
                onClick = {
                    onUpdateDisplaySettings(displaySettings.copy(titleScrollLong = false))
                },
                label = { Text("Wrap Titles", fontWeight = FontWeight.Bold) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = displaySettings.titleScrollLong,
                onClick = {
                    onUpdateDisplaySettings(displaySettings.copy(titleScrollLong = true))
                },
                label = { Text("Scrolling Marquee", fontWeight = FontWeight.Bold) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ArtSettingsContent(
    displaySettings: DisplaySettings,
    onUpdateDisplaySettings: (DisplaySettings) -> Unit
) {
    Column {
        Text("Album Art Display Options", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Show Album Art", fontWeight = FontWeight.SemiBold)
                Text("Display album artwork on the player screen", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = displaySettings.showAlbumArt,
                onCheckedChange = { checked ->
                    onUpdateDisplaySettings(displaySettings.copy(showAlbumArt = checked))
                }
            )
        }

        if (displaySettings.showAlbumArt) {
            Text("Album Art Opacity (Behind Titles): ${(displaySettings.albumArtFade * 100).toInt()}%", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            Slider(
                value = displaySettings.albumArtFade,
                onValueChange = { fade ->
                    onUpdateDisplaySettings(displaySettings.copy(albumArtFade = fade))
                },
                valueRange = 0.1f..1.0f
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Album Art Scale Mode", fontWeight = FontWeight.Bold)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp)
        ) {
            ArtScaleOption.entries.forEach { option ->
                FilterChip(
                    selected = displaySettings.albumArtScale == option,
                    onClick = {
                        onUpdateDisplaySettings(displaySettings.copy(albumArtScale = option))
                    },
                    label = { Text(option.displayName) }
                )
            }
        }

        val isDocked = displaySettings.artDisplayLayout == ArtLayoutOption.DOCKED

        val rawPortraitOptions = if (displaySettings.albumArtScale == ArtScaleOption.FILL_SCREEN) {
            listOf(ArtAlignmentPortrait.LEFT, ArtAlignmentPortrait.CENTER, ArtAlignmentPortrait.RIGHT)
        } else {
            listOf(ArtAlignmentPortrait.TOP, ArtAlignmentPortrait.MIDDLE, ArtAlignmentPortrait.BOTTOM)
        }
        val portraitOptions = if (isDocked) {
            rawPortraitOptions.filter { it != ArtAlignmentPortrait.CENTER && it != ArtAlignmentPortrait.MIDDLE }
        } else {
            rawPortraitOptions
        }

        val rawLandscapeOptions = if (displaySettings.albumArtScale == ArtScaleOption.FILL_SCREEN) {
            listOf(ArtAlignmentLandscape.TOP, ArtAlignmentLandscape.MIDDLE, ArtAlignmentLandscape.BOTTOM)
        } else {
            listOf(ArtAlignmentLandscape.LEFT, ArtAlignmentLandscape.CENTER, ArtAlignmentLandscape.RIGHT)
        }
        val landscapeOptions = if (isDocked) {
            rawLandscapeOptions.filter { it != ArtAlignmentLandscape.CENTER && it != ArtAlignmentLandscape.MIDDLE }
        } else {
            rawLandscapeOptions
        }

        Text("Portrait Alignment", fontWeight = FontWeight.Bold)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp)
        ) {
            portraitOptions.forEach { option ->
                FilterChip(
                    selected = displaySettings.artAlignmentPortrait == option,
                    onClick = {
                        onUpdateDisplaySettings(displaySettings.copy(artAlignmentPortrait = option))
                    },
                    label = { Text(option.displayName) }
                )
            }
        }

        Text("Landscape Alignment", fontWeight = FontWeight.Bold)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp)
        ) {
            landscapeOptions.forEach { option ->
                FilterChip(
                    selected = displaySettings.artAlignmentLandscape == option,
                    onClick = {
                        onUpdateDisplaySettings(displaySettings.copy(artAlignmentLandscape = option))
                    },
                    label = { Text(option.displayName) }
                )
            }
        }

        Text("Album Art Layout", fontWeight = FontWeight.Bold)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp)
        ) {
            ArtLayoutOption.entries.forEach { option ->
                FilterChip(
                    selected = displaySettings.artDisplayLayout == option,
                    onClick = {
                        var newPortrait = displaySettings.artAlignmentPortrait
                        var newLandscape = displaySettings.artAlignmentLandscape
                        if (option == ArtLayoutOption.DOCKED) {
                            if (newPortrait == ArtAlignmentPortrait.CENTER || newPortrait == ArtAlignmentPortrait.MIDDLE) {
                                newPortrait = ArtAlignmentPortrait.LEFT
                            }
                            if (newLandscape == ArtAlignmentLandscape.CENTER || newLandscape == ArtAlignmentLandscape.MIDDLE) {
                                newLandscape = ArtAlignmentLandscape.LEFT
                            }
                        }
                        onUpdateDisplaySettings(displaySettings.copy(
                            artDisplayLayout = option,
                            artAlignmentPortrait = newPortrait,
                            artAlignmentLandscape = newLandscape
                        ))
                    },
                    label = { Text(option.displayName) }
                )
            }
        }

        if (displaySettings.artDisplayLayout == ArtLayoutOption.DOCKED) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Stretch Art", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Expand inner edge colors of artwork across background behind titles",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = displaySettings.stretchArt,
                    onCheckedChange = { checked ->
                        onUpdateDisplaySettings(displaySettings.copy(stretchArt = checked))
                    }
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Adaptive Docked Art", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Place art to side when space is available, or behind titles when reduced for split-screen",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = displaySettings.adaptiveDockedArt,
                    onCheckedChange = { checked ->
                        onUpdateDisplaySettings(
                            displaySettings.copy(
                                adaptiveDockedArt = checked,
                                separateTouchZones = if (checked) false else displaySettings.separateTouchZones
                            )
                        )
                    }
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Separate Touch Zones", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Art and Titles regions have separate gesture-mapped actions",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = displaySettings.separateTouchZones && !displaySettings.adaptiveDockedArt,
                    enabled = !displaySettings.adaptiveDockedArt,
                    onCheckedChange = { checked ->
                        onUpdateDisplaySettings(
                            displaySettings.copy(
                                separateTouchZones = checked,
                                adaptiveDockedArt = if (checked) false else displaySettings.adaptiveDockedArt
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun HudSettingsContent(
    displaySettings: DisplaySettings,
    themeSettings: ThemeSettings,
    onUpdateDisplaySettings: (DisplaySettings) -> Unit,
    onUpdateThemeSettings: (ThemeSettings) -> Unit
) {
    Column {
        Text("HUD & Overlay Controls", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)

        Text("Volume Bar Display Style", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            HudTypeOption.entries.forEach { option ->
                FilterChip(
                    selected = displaySettings.hudType == option,
                    onClick = {
                        onUpdateDisplaySettings(displaySettings.copy(hudType = option))
                    },
                    label = { Text(option.displayName) }
                )
            }
        }

        Text("Progress Bar Display Style", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            ScrubHudTypeOption.entries.forEach { option ->
                FilterChip(
                    selected = displaySettings.scrubHudType == option,
                    onClick = {
                        onUpdateDisplaySettings(displaySettings.copy(scrubHudType = option))
                    },
                    label = { Text(option.displayName) }
                )
            }
        }

        Text("HUD Line & Bar Thickness: ${displaySettings.hudLineThickness.toInt()} dp", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
        Slider(
            value = displaySettings.hudLineThickness,
            onValueChange = { thickness ->
                onUpdateDisplaySettings(displaySettings.copy(hudLineThickness = thickness))
            },
            valueRange = 6f..40f
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Text("Overlay Styles & Glass Features", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text("Rounded Corners", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Switch(
                checked = themeSettings.isRounded,
                onCheckedChange = { checked ->
                    onUpdateThemeSettings(themeSettings.copy(isRounded = checked))
                }
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text("Glass Effect (Translucent & Blur)", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Switch(
                checked = themeSettings.isGlass,
                onCheckedChange = { checked ->
                    onUpdateThemeSettings(themeSettings.copy(isGlass = checked))
                }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Text("Screen & Display Modes", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Always Show Volume Overlay", fontWeight = FontWeight.SemiBold)
            }
            Switch(
                checked = displaySettings.volumeAlwaysOn,
                onCheckedChange = { checked ->
                    onUpdateDisplaySettings(displaySettings.copy(volumeAlwaysOn = checked))
                }
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Prevent Screen From Turning Off", fontWeight = FontWeight.SemiBold)
                Text("Keep screen awake while using the app", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = displaySettings.keepScreenOn,
                onCheckedChange = { checked ->
                    onUpdateDisplaySettings(displaySettings.copy(keepScreenOn = checked))
                }
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Immersive Mode (Hide Status Bar)", fontWeight = FontWeight.SemiBold)
                Text("Hide status bar on the play screen", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = displaySettings.immersiveMode,
                onCheckedChange = { checked ->
                    onUpdateDisplaySettings(displaySettings.copy(immersiveMode = checked))
                }
            )
        }
    }
}

@Composable
private fun GesturesSettingsContent(
    displaySettings: DisplaySettings,
    onUpdateDisplaySettings: (DisplaySettings) -> Unit,
    onOpenGestureAssignments: () -> Unit,
    onResetGestureAssignments: () -> Unit
) {
    Column {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            val isSeparate = displaySettings.separateTouchZones && !displaySettings.adaptiveDockedArt
            Text(
                text = if (isSeparate) "Number of Title Edge Regions: ${displaySettings.numEdgeRegions}" else "Number of Edge Regions: ${displaySettings.numEdgeRegions}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (isSeparate) "Configures active Title region slots (1 to 7) along top & bottom Title edges" else "Configures active region slots (1 to 7) along top & bottom screen edges",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Slider(
                value = displaySettings.numEdgeRegions.toFloat(),
                onValueChange = { newValue ->
                    onUpdateDisplaySettings(displaySettings.copy(numEdgeRegions = newValue.roundToInt()))
                },
                valueRange = 1f..7f,
                steps = 5
            )

            if (isSeparate) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Number of Art Edge Regions: ${displaySettings.numArtEdgeRegions}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Configures active Art region slots (1 to 7) along top & bottom Art edges",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Slider(
                    value = displaySettings.numArtEdgeRegions.toFloat(),
                    onValueChange = { newValue ->
                        onUpdateDisplaySettings(displaySettings.copy(numArtEdgeRegions = newValue.roundToInt()))
                    },
                    valueRange = 1f..7f,
                    steps = 5
                )
            }
        }
        HorizontalDivider()
        ListItem(
            headlineContent = { Text("Reconfigure Gesture Assignments") },
            supportingContent = { Text("Customize 1/2/3 finger swipes, taps, long presses, and edge regions") },
            modifier = Modifier.clickable { onOpenGestureAssignments() }
        )
        ListItem(
            headlineContent = { Text("Reset All Gesture Assignments") },
            modifier = Modifier.clickable { onResetGestureAssignments() }
        )
    }
}

@Composable
private fun ThemesSettingsContent(
    themeSettings: ThemeSettings,
    displaySettings: DisplaySettings,
    onUpdateDisplaySettings: (DisplaySettings) -> Unit = {},
    onSelectPreset: (String, Boolean) -> Unit,
    onOpenBgPicker: () -> Unit,
    onOpenSongPicker: () -> Unit,
    onOpenArtistPicker: () -> Unit,
    onOpenAlbumPicker: () -> Unit
) {
    Column {
        val isMatchAlbumArtActive = themeSettings.currentThemeName.equals("Match Album Art", ignoreCase = true) ||
                                    themeSettings.currentThemeName.equals("Auto By Art", ignoreCase = true) ||
                                    displaySettings.albumArtColors

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Match Album Art Theme", fontWeight = FontWeight.SemiBold)
                Text("Extract dynamic color palette from album art", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = isMatchAlbumArtActive,
                onCheckedChange = { checked ->
                    val newThemeName = if (checked) "Match Album Art" else "White on Grey"
                    onSelectPreset(newThemeName, checked)
                }
            )
        }

        if (isMatchAlbumArtActive) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Match Album Art Edge Focus", fontWeight = FontWeight.Bold)
            Text(
                "Prioritize center region, outer edges, or whole edge (with blending)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp)
            ) {
                ArtColorPriority.entries.forEach { option ->
                    FilterChip(
                        selected = displaySettings.matchArtColorPriority == option,
                        onClick = {
                            onUpdateDisplaySettings(displaySettings.copy(matchArtColorPriority = option))
                        },
                        label = { Text(option.displayName) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Color Presets", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

        ColorTheme.PRESETS.forEach { theme ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val isAuto = theme.name.equals("Match Album Art", ignoreCase = true) || theme.name.equals("Auto By Art", ignoreCase = true)
                        onSelectPreset(theme.name, isAuto)
                    }
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = theme.name,
                    fontWeight = if (themeSettings.currentThemeName == theme.name) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                if (themeSettings.currentThemeName == theme.name) {
                    Text("✓", color = MaterialTheme.colorScheme.primary)
                }
            }

            if (theme.name == "Custom" && themeSettings.currentThemeName == "Custom") {
                CustomColorPaintBucketsRow(
                    themeSettings = themeSettings,
                    onOpenBgPicker = onOpenBgPicker,
                    onOpenSongPicker = onOpenSongPicker,
                    onOpenArtistPicker = onOpenArtistPicker,
                    onOpenAlbumPicker = onOpenAlbumPicker
                )
            }
        }
    }
}

@Composable
private fun AboutSettingsContent(
    onOpenQuickStart: () -> Unit
) {
    Column {
        ListItem(
            headlineContent = { Text("Show Quick Start Tutorial") },
            supportingContent = { Text("Learn app gestures, controls, and playback tips") },
            modifier = Modifier.clickable { onOpenQuickStart() }
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Traveling Tunes v1.0",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Composable
private fun AndroidAutoSettingsContent(
    displaySettings: DisplaySettings,
    onUpdateDisplaySettings: (DisplaySettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Card 1: Root Browse Category Preference Order (Re-orderable)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Browse Category Preference Order",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Re-order how music categories appear when browsing on your Android Auto screen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                val currentCatOrder = displaySettings.autoCategoryOrder
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currentCatOrder.forEachIndexed { index, category ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DragHandle,
                                        contentDescription = "Drag handle",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "${index + 1}. ${category.displayName}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (index > 0) {
                                        IconButton(
                                            onClick = {
                                                val newOrder = currentCatOrder.toMutableList()
                                                val temp = newOrder[index]
                                                newOrder[index] = newOrder[index - 1]
                                                newOrder[index - 1] = temp
                                                onUpdateDisplaySettings(displaySettings.copy(autoCategoryOrder = newOrder))
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.KeyboardArrowUp,
                                                contentDescription = "Move Up"
                                            )
                                        }
                                    }
                                    if (index < currentCatOrder.size - 1) {
                                        IconButton(
                                            onClick = {
                                                val newOrder = currentCatOrder.toMutableList()
                                                val temp = newOrder[index]
                                                newOrder[index] = newOrder[index + 1]
                                                newOrder[index + 1] = temp
                                                onUpdateDisplaySettings(displaySettings.copy(autoCategoryOrder = newOrder))
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Move Down"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Card 2: Display Title Preference Order (Re-orderable)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Display Title Preference Order",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Re-order title metadata fields (Artist, Song, Album)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                val currentTitleOrder = displaySettings.titleOrder
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currentTitleOrder.forEachIndexed { index, rowType ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DragHandle,
                                        contentDescription = "Drag handle",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "${index + 1}. ${rowType.displayName}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (index > 0) {
                                        IconButton(
                                            onClick = {
                                                val newOrder = currentTitleOrder.toMutableList()
                                                val temp = newOrder[index]
                                                newOrder[index] = newOrder[index - 1]
                                                newOrder[index - 1] = temp
                                                onUpdateDisplaySettings(displaySettings.copy(titleOrder = newOrder))
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.KeyboardArrowUp,
                                                contentDescription = "Move Up"
                                            )
                                        }
                                    }
                                    if (index < currentTitleOrder.size - 1) {
                                        IconButton(
                                            onClick = {
                                                val newOrder = currentTitleOrder.toMutableList()
                                                val temp = newOrder[index]
                                                newOrder[index] = newOrder[index + 1]
                                                newOrder[index + 1] = temp
                                                onUpdateDisplaySettings(displaySettings.copy(titleOrder = newOrder))
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Move Down"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Card 3: On-Screen Action Buttons (Configure & Re-order)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "On-Screen Action Buttons",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Configure and re-order playback control buttons displayed on your Android Auto screen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                val currentButtons = displaySettings.autoActionButtonOrder
                var showAddDropdown by remember { mutableStateOf(false) }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currentButtons.forEachIndexed { index, action ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DragHandle,
                                        contentDescription = "Drag handle",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "${index + 1}. ${action.displayName}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (index > 0) {
                                        IconButton(
                                            onClick = {
                                                val newOrder = currentButtons.toMutableList()
                                                val temp = newOrder[index]
                                                newOrder[index] = newOrder[index - 1]
                                                newOrder[index - 1] = temp
                                                onUpdateDisplaySettings(displaySettings.copy(autoActionButtonOrder = newOrder))
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.KeyboardArrowUp,
                                                contentDescription = "Move Up"
                                            )
                                        }
                                    }
                                    if (index < currentButtons.size - 1) {
                                        IconButton(
                                            onClick = {
                                                val newOrder = currentButtons.toMutableList()
                                                val temp = newOrder[index]
                                                newOrder[index] = newOrder[index + 1]
                                                newOrder[index + 1] = temp
                                                onUpdateDisplaySettings(displaySettings.copy(autoActionButtonOrder = newOrder))
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Move Down"
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            val newOrder = currentButtons.toMutableList().apply { removeAt(index) }
                                            onUpdateDisplaySettings(displaySettings.copy(autoActionButtonOrder = newOrder))
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove Action",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }

                        val availableActions = GestureAction.entries.filter {
                            it != GestureAction.UNASSIGNED &&
                            it != GestureAction.OTHER_OPTION &&
                            it !in currentButtons
                        }

                        if (availableActions.isNotEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                                OutlinedButton(
                                    onClick = { showAddDropdown = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Action Button")
                                }

                                DropdownMenu(
                                    expanded = showAddDropdown,
                                    onDismissRequest = { showAddDropdown = false }
                                ) {
                                    availableActions.forEach { act ->
                                        DropdownMenuItem(
                                            text = { Text(act.displayName) },
                                            onClick = {
                                                val newOrder = currentButtons.toMutableList().apply { add(act) }
                                                onUpdateDisplaySettings(displaySettings.copy(autoActionButtonOrder = newOrder))
                                                showAddDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Card 3: Android Auto Display & Behavior Options
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Android Auto Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Toggle: Show Album Art
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Show Album Art in Auto",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Display album cover artwork on vehicle screen items",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = displaySettings.autoShowAlbumArt,
                        onCheckedChange = { checked ->
                            onUpdateDisplaySettings(displaySettings.copy(autoShowAlbumArt = checked))
                        }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Layout: Album Browse Style
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Album Browse Style",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Display albums in grid or list view on car screen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = displaySettings.autoAlbumStyleGrid,
                            onClick = {
                                onUpdateDisplaySettings(displaySettings.copy(autoAlbumStyleGrid = true))
                            },
                            label = { Text("Grid") }
                        )
                        FilterChip(
                            selected = !displaySettings.autoAlbumStyleGrid,
                            onClick = {
                                onUpdateDisplaySettings(displaySettings.copy(autoAlbumStyleGrid = false))
                            },
                            label = { Text("List") }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Layout: Artist Browse Style
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Artist Browse Style",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Display artists in list or grid view on car screen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = displaySettings.autoArtistStyleGrid,
                            onClick = {
                                onUpdateDisplaySettings(displaySettings.copy(autoArtistStyleGrid = true))
                            },
                            label = { Text("Grid") }
                        )
                        FilterChip(
                            selected = !displaySettings.autoArtistStyleGrid,
                            onClick = {
                                onUpdateDisplaySettings(displaySettings.copy(autoArtistStyleGrid = false))
                            },
                            label = { Text("List") }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Toggle: Auto-Play on Connect
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Play on Connect",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Automatically resume playback when connected to Android Auto",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = displaySettings.autoAutoplayOnConnect,
                        onCheckedChange = { checked ->
                            onUpdateDisplaySettings(displaySettings.copy(autoAutoplayOnConnect = checked))
                        }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Toggle: Voice Search Support
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Voice Search Support",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Enable voice search and Google Assistant media integration",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = displaySettings.autoVoiceSearch,
                        onCheckedChange = { checked ->
                            onUpdateDisplaySettings(displaySettings.copy(autoVoiceSearch = checked))
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card 4: Driving Mode & Speed-Based Volume Adjustment
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Driving Mode & Speed Volume",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Toggle: Driving Mode
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Driving Mode",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Manually engage driving mode for driving-optimized behavior",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = displaySettings.drivingModeEnabled,
                        onCheckedChange = { checked ->
                            onUpdateDisplaySettings(displaySettings.copy(drivingModeEnabled = checked))
                        }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Toggle: Automatically Enable Driving Mode
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Automatically Enable Driving Mode",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Engage driving mode automatically when vehicular motion is detected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = displaySettings.autoEnableDrivingMode,
                        onCheckedChange = { checked ->
                            onUpdateDisplaySettings(displaySettings.copy(autoEnableDrivingMode = checked))
                        }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Toggle: Speed-Based Volume Adjustment
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Speed-Based Volume Adjustment",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Automatically adjust volume based on vehicle speed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = displaySettings.autoSpeedVolumeEnabled,
                        onCheckedChange = { checked ->
                            onUpdateDisplaySettings(displaySettings.copy(autoSpeedVolumeEnabled = checked))
                        }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Speed Unit Selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Speed Unit",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Choose units for speed measurements",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = displaySettings.autoSpeedUnit.equals("MPH", ignoreCase = true),
                            onClick = {
                                onUpdateDisplaySettings(displaySettings.copy(autoSpeedUnit = "MPH"))
                            },
                            label = { Text("MPH") }
                        )
                        FilterChip(
                            selected = displaySettings.autoSpeedUnit.equals("KPH", ignoreCase = true),
                            onClick = {
                                onUpdateDisplaySettings(displaySettings.copy(autoSpeedUnit = "KPH"))
                            },
                            label = { Text("KPH") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Configurable Default Volume
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Configurable Default Volume",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${displaySettings.autoDefaultVolume}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Baseline volume level before speed-based increase",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = displaySettings.autoDefaultVolume.toFloat(),
                        onValueChange = { value ->
                            onUpdateDisplaySettings(displaySettings.copy(autoDefaultVolume = value.roundToInt()))
                        },
                        valueRange = 0f..100f,
                        steps = 99
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Minimum Speed Threshold
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Minimum Speed Threshold",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${displaySettings.autoMinSpeedThreshold.roundToInt()} ${displaySettings.autoSpeedUnit}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Speed at which volume auto-increase begins",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = displaySettings.autoMinSpeedThreshold,
                        onValueChange = { value ->
                            onUpdateDisplaySettings(displaySettings.copy(autoMinSpeedThreshold = value))
                        },
                        valueRange = 5f..60f,
                        steps = 54
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Rate-of-Increase Ratio
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Volume Increase Rate",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${"%.1f".format(displaySettings.autoSpeedVolumeRatio)} vol step / 10 ${displaySettings.autoSpeedUnit}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Rate of volume increase per 10 ${displaySettings.autoSpeedUnit} above threshold",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = displaySettings.autoSpeedVolumeRatio,
                        onValueChange = { value ->
                            onUpdateDisplaySettings(displaySettings.copy(autoSpeedVolumeRatio = value))
                        },
                        valueRange = 0.1f..5.0f,
                        steps = 48
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfilesSettingsContent(
    settingsDataStore: SettingsDataStore
) {
    val coroutineScope = rememberCoroutineScope()
    val profiles by settingsDataStore.profilesFlow.collectAsState(initial = listOf(Profile.DEFAULT, Profile.TRAVELING))
    val activeProfile by settingsDataStore.activeProfileFlow.collectAsState(initial = Profile.DEFAULT)
    val selectionMode by settingsDataStore.profileSelectionModeFlow.collectAsState(initial = ProfileSelectionMode.MENU)
    val switchTargets by settingsDataStore.profileSwitchTargetsFlow.collectAsState(initial = listOf(Profile.DEFAULT_ID, Profile.TRAVELING_ID))

    var showCreateDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }
    var renamingProfileId by remember { mutableStateOf<String?>(null) }
    var renamingName by remember { mutableStateOf("") }

    var copyingProfile by remember { mutableStateOf<Profile?>(null) }
    var copyName by remember { mutableStateOf("") }
    var copyLinkForInheritance by remember { mutableStateOf(true) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Active Profile Selector Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Active Profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (activeProfile.id == Profile.DEFAULT_ID) "Current profile: ${activeProfile.name} (Default baseline settings)" else "Current profile: ${activeProfile.name} (${activeProfile.overrides.size} overrides relative to parent)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    profiles.forEach { profile ->
                        FilterChip(
                            selected = profile.id == activeProfile.id,
                            onClick = {
                                coroutineScope.launch {
                                    settingsDataStore.setActiveProfile(profile.id)
                                }
                            },
                            label = { Text(profile.name) }
                        )
                    }
                }
            }
        }

        // All Profiles List Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "All Profiles",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(onClick = {
                        newProfileName = ""
                        showCreateDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Profile")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                profiles.forEach { profile ->
                    val currentParent = profiles.find { it.id == (profile.parentId ?: Profile.DEFAULT_ID) } ?: Profile.DEFAULT
                    val validParents = profiles.filter { candidate ->
                        candidate.id != profile.id && !profile.getAncestorChain(profiles).any { it.id == candidate.id }
                    }

                    ListItem(
                        headlineContent = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(profile.name, fontWeight = FontWeight.SemiBold)
                                if (profile.id == activeProfile.id) {
                                    Text("(Active)", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                if (profile.isBuiltIn) {
                                    Text("(Built-in)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                }
                            }
                        },
                        supportingContent = {
                            Column {
                                Text(
                                    when (profile.id) {
                                        Profile.DEFAULT_ID -> "Default baseline settings for TravelingTunes"
                                        Profile.TRAVELING_ID -> "Built-in profile configured for Driving & Travel mode"
                                        Profile.DOCKED_ID -> "Built-in profile configured for Docked Art mode"
                                        Profile.UNDOCKED_ID -> "Built-in profile configured for Undocked Art mode"
                                        else -> "Inherits from: ${currentParent.name} • ${profile.overrides.size} local overrides"
                                    }
                                )
                                if (profile.id != Profile.DEFAULT_ID && validParents.size > 1) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Inherit from:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        validParents.forEach { parentCandidate ->
                                            FilterChip(
                                                selected = currentParent.id == parentCandidate.id,
                                                onClick = {
                                                    coroutineScope.launch {
                                                        settingsDataStore.setProfileParent(profile.id, parentCandidate.id)
                                                    }
                                                },
                                                label = { Text(parentCandidate.name, fontSize = 11.sp) },
                                                modifier = Modifier.height(28.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    copyingProfile = profile
                                    copyName = "${profile.name} Copy"
                                    copyLinkForInheritance = true
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Profile")
                                }
                                if (!profile.isBuiltIn && profile.isDeletable) {
                                    IconButton(onClick = {
                                        renamingProfileId = profile.id
                                        renamingName = profile.name
                                    }) {
                                        Icon(Icons.Default.FormatPaint, contentDescription = "Rename Profile")
                                    }
                                    IconButton(onClick = {
                                        coroutineScope.launch {
                                            settingsDataStore.deleteProfile(profile.id)
                                        }
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Delete Profile")
                                    }
                                }
                            }
                        }
                    )
                    HorizontalDivider()
                }

                if (activeProfile.id != Profile.DEFAULT_ID && activeProfile.overrides.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                settingsDataStore.revertAllSettingsForActiveProfile()
                            }
                        }
                    ) {
                        Text("Revert All Overrides for ${activeProfile.name}")
                    }
                }
            }
        }

        // Action Configuration Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "'Select Profile' Action Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Configure what happens when triggering the 'Select Profile' action",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                ProfileSelectionMode.entries.forEach { mode ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch {
                                    settingsDataStore.setProfileSelectionMode(mode)
                                }
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectionMode == mode,
                            onClick = {
                                coroutineScope.launch {
                                    settingsDataStore.setProfileSelectionMode(mode)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(mode.displayName)
                    }
                }

                if (selectionMode == ProfileSelectionMode.SEQUENTIAL) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Profiles included in switcher:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    profiles.forEach { profile ->
                        val isChecked = switchTargets.contains(profile.id)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newTargets = if (isChecked) {
                                        switchTargets.filterNot { it == profile.id }
                                    } else {
                                        switchTargets + profile.id
                                    }
                                    coroutineScope.launch {
                                        settingsDataStore.setProfileSwitchTargets(newTargets)
                                    }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Switch(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    val newTargets = if (checked) {
                                        switchTargets + profile.id
                                    } else {
                                        switchTargets.filterNot { it == profile.id }
                                    }
                                    coroutineScope.launch {
                                        settingsDataStore.setProfileSwitchTargets(newTargets)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(profile.name)
                        }
                    }
                }
            }
        }
    }

    // Dialog: Create Profile
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New Profile") },
            text = {
                Column {
                    Text("Enter a name for the new settings profile:")
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = newProfileName,
                        onValueChange = { newProfileName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newProfileName.isNotBlank()) {
                            coroutineScope.launch {
                                settingsDataStore.createProfile(newProfileName.trim())
                            }
                        }
                        showCreateDialog = false
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Rename Profile
    renamingProfileId?.let { profId ->
        AlertDialog(
            onDismissRequest = { renamingProfileId = null },
            title = { Text("Rename Profile") },
            text = {
                Column {
                    Text("Enter a new name for the profile:")
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = renamingName,
                        onValueChange = { renamingName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renamingName.isNotBlank()) {
                            coroutineScope.launch {
                                settingsDataStore.renameProfile(profId, renamingName.trim())
                            }
                        }
                        renamingProfileId = null
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingProfileId = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Copy Profile
    copyingProfile?.let { srcProf ->
        AlertDialog(
            onDismissRequest = { copyingProfile = null },
            title = { Text("Copy Profile: ${srcProf.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enter name for copied profile:")
                    androidx.compose.material3.OutlinedTextField(
                        value = copyName,
                        onValueChange = { copyName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { copyLinkForInheritance = !copyLinkForInheritance }
                            .padding(vertical = 4.dp)
                    ) {
                        Switch(
                            checked = copyLinkForInheritance,
                            onCheckedChange = { copyLinkForInheritance = it }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Link for Inheritance from ${srcProf.name}", fontWeight = FontWeight.Medium)
                            Text(
                                if (copyLinkForInheritance) "Inherits settings from ${srcProf.name}. Changes in ${srcProf.name} will automatically flow to this profile."
                                else "Creates an independent snapshot copy with ${srcProf.name}'s current values.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (copyName.isNotBlank()) {
                            coroutineScope.launch {
                                settingsDataStore.copyProfile(srcProf.id, copyName.trim(), copyLinkForInheritance)
                            }
                        }
                        copyingProfile = null
                    }
                ) {
                    Text("Copy")
                }
            },
            dismissButton = {
                TextButton(onClick = { copyingProfile = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CustomColorPaintBucketsRow(
    themeSettings: ThemeSettings,
    onOpenBgPicker: () -> Unit,
    onOpenSongPicker: () -> Unit,
    onOpenArtistPicker: () -> Unit,
    onOpenAlbumPicker: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "Custom Theme Paint Buckets",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            val bgColor = Color(
                red = (themeSettings.customBGRed / 255f).coerceIn(0f, 1f),
                green = (themeSettings.customBGGreen / 255f).coerceIn(0f, 1f),
                blue = (themeSettings.customBGBlue / 255f).coerceIn(0f, 1f)
            )
            val songColor = Color(
                red = (themeSettings.customSongTitleRed / 255f).coerceIn(0f, 1f),
                green = (themeSettings.customSongTitleGreen / 255f).coerceIn(0f, 1f),
                blue = (themeSettings.customSongTitleBlue / 255f).coerceIn(0f, 1f)
            )
            val artistColor = Color(
                red = (themeSettings.customArtistTitleRed / 255f).coerceIn(0f, 1f),
                green = (themeSettings.customArtistTitleGreen / 255f).coerceIn(0f, 1f),
                blue = (themeSettings.customArtistTitleBlue / 255f).coerceIn(0f, 1f)
            )
            val albumColor = Color(
                red = (themeSettings.customAlbumTitleRed / 255f).coerceIn(0f, 1f),
                green = (themeSettings.customAlbumTitleGreen / 255f).coerceIn(0f, 1f),
                blue = (themeSettings.customAlbumTitleBlue / 255f).coerceIn(0f, 1f)
            )

            PaintBucketItem(label = "BG", color = bgColor, onClick = onOpenBgPicker)
            PaintBucketItem(label = "Song", color = songColor, onClick = onOpenSongPicker)
            PaintBucketItem(label = "Artist", color = artistColor, onClick = onOpenArtistPicker)
            PaintBucketItem(label = "Album", color = albumColor, onClick = onOpenAlbumPicker)
        }
    }
}

@Composable
private fun PaintBucketItem(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color)
                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FormatPaint,
                contentDescription = "$label Paint Bucket",
                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun FontSelectorRow(
    currentFontKey: String,
    availableFonts: List<FontOption>,
    onFontSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedFontName = availableFonts.find { it.key == currentFontKey }?.displayName ?: "System Default"
    val selectedFontFamily = remember(currentFontKey) { FontHelper.getFontFamily(currentFontKey) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = selectedFontName,
                    fontFamily = selectedFontFamily,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Select Font",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            availableFonts.forEach { fontOption ->
                val itemFontFamily = remember(fontOption.key) { FontHelper.getFontFamily(fontOption.key) }
                DropdownMenuItem(
                    text = {
                        Text(
                            text = fontOption.displayName,
                            fontFamily = itemFontFamily,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onFontSelected(fontOption.key)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CustomColorPickerDialog(
    title: String,
    initialRed: Float,
    initialGreen: Float,
    initialBlue: Float,
    onDismiss: () -> Unit,
    onConfirmColor: (Float, Float, Float) -> Unit
) {
    var red by remember { mutableFloatStateOf(initialRed) }
    var green by remember { mutableFloatStateOf(initialGreen) }
    var blue by remember { mutableFloatStateOf(initialBlue) }

    val currentColor = Color(
        red = (red / 255f).coerceIn(0f, 1f),
        green = (green / 255f).coerceIn(0f, 1f),
        blue = (blue / 255f).coerceIn(0f, 1f)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(currentColor)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("Red: ${red.toInt()}", fontSize = 12.sp)
                Slider(
                    value = red,
                    onValueChange = { red = it },
                    valueRange = 0f..255f
                )

                Text("Green: ${green.toInt()}", fontSize = 12.sp)
                Slider(
                    value = green,
                    onValueChange = { green = it },
                    valueRange = 0f..255f
                )

                Text("Blue: ${blue.toInt()}", fontSize = 12.sp)
                Slider(
                    value = blue,
                    onValueChange = { blue = it },
                    valueRange = 0f..255f
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirmColor(red, green, blue) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
