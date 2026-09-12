package com.travelingtunes.app.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelingtunes.app.core.database.LibraryStats
import com.travelingtunes.app.core.datastore.SettingsDataStore
import com.travelingtunes.app.core.media.AlbumArtAuditReport
import com.travelingtunes.app.core.model.ArtAlignmentLandscape
import com.travelingtunes.app.core.model.ArtAlignmentPortrait
import com.travelingtunes.app.core.model.ArtLayoutOption
import com.travelingtunes.app.core.model.ArtScaleOption
import com.travelingtunes.app.core.model.TextAlignmentOption
import com.travelingtunes.app.core.model.TitleRowType
import com.travelingtunes.app.core.model.ColorTheme
import com.travelingtunes.app.core.model.DisplaySettings
import com.travelingtunes.app.core.model.HudTypeOption
import com.travelingtunes.app.core.model.ScrubHudTypeOption
import com.travelingtunes.app.core.model.ThemeSettings
import com.travelingtunes.app.core.theme.FontHelper
import com.travelingtunes.app.core.theme.FontOption
import com.travelingtunes.app.core.theme.luminance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    val icon: ImageVector
) {
    LIBRARY(
        title = "Music Library",
        description = "Folder selection, rescan & album art downloads",
        icon = Icons.Default.Folder
    ),
    TYPOGRAPHY_HUD(
        title = "Font & Layout",
        description = "Font sizes, title wrap/marquee, alignment & HUD overlays",
        icon = Icons.Default.Title
    ),
    GESTURES(
        title = "Gestures & Controls",
        description = "Touch region sensitivity & gesture action assignments",
        icon = Icons.Default.TouchApp
    ),
    THEMES(
        title = "Colors",
        description = "Color presets, custom colors, glass effect & rounded corners",
        icon = Icons.Default.Palette
    ),
    ABOUT(
        title = "Tutorial & About",
        description = "Gesture tutorial & app information",
        icon = Icons.Default.Info
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsDataStore: SettingsDataStore,
    displaySettings: DisplaySettings,
    themeSettings: ThemeSettings,
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
    onPickMusicFolder: () -> Unit = {},
    onRescanMusicFolder: () -> Unit = {},
    onDownloadMissingArt: () -> Unit = {},
    onCancelDownloadArt: () -> Unit = {},
    onNavigateBack: () -> Unit,
    onOpenGestureAssignments: () -> Unit,
    onOpenQuickStart: () -> Unit,
    onOpenDownloadedArtBrowser: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val window = (context as? android.app.Activity)?.window
    if (window != null) {
        androidx.compose.runtime.DisposableEffect(Unit) {
            val insetsController = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
            insetsController.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            onDispose { }
        }
    }

    var availableFonts by remember { mutableStateOf(FontHelper.getAvailableFonts(context)) }
    var activeColorPicker by remember { mutableStateOf<String?>(null) }
    var showAuditDialog by remember { mutableStateOf(false) }

    var selectedSubmenu by rememberSaveable { mutableStateOf<SettingsSubmenu?>(null) }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isWideScreen = screenWidthDp >= 600

    val activeSubmenu = if (isWideScreen) (selectedSubmenu ?: SettingsSubmenu.LIBRARY) else selectedSubmenu

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (!isWideScreen && activeSubmenu != null) activeSubmenu.title else "Traveling Tunes Settings"
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (!isWideScreen && selectedSubmenu != null) {
                                selectedSubmenu = null
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                    )

                    SettingsSubmenu.entries.forEach { submenu ->
                        val isSelected = activeSubmenu == submenu
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSubmenu = submenu }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = submenu.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = submenu.title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                VerticalDivider()

                // Detail Content (Right Pane)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    SubmenuContent(
                        submenu = activeSubmenu ?: SettingsSubmenu.LIBRARY,
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
                        availableFonts = availableFonts,
                        onPickMusicFolder = onPickMusicFolder,
                        onRescanMusicFolder = onRescanMusicFolder,
                        onDownloadMissingArt = onDownloadMissingArt,
                        onCancelDownloadArt = onCancelDownloadArt,
                        onViewAudit = { showAuditDialog = true },
                        onOpenDownloadedArtBrowser = onOpenDownloadedArtBrowser,
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
                        onOpenQuickStart = onOpenQuickStart
                    )
                }
            }
        } else {
            // Single-Pane Navigation for Compact Screens
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                if (activeSubmenu == null) {
                    // Main Submenu Navigation List
                    Text(
                        text = "Settings Categories",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingsSubmenu.entries.forEach { submenu ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedSubmenu = submenu }
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
                                        contentDescription = "Open",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Selected Submenu Content Page
                    SubmenuContent(
                        submenu = activeSubmenu,
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
                        availableFonts = availableFonts,
                        onPickMusicFolder = onPickMusicFolder,
                        onRescanMusicFolder = onRescanMusicFolder,
                        onDownloadMissingArt = onDownloadMissingArt,
                        onCancelDownloadArt = onCancelDownloadArt,
                        onViewAudit = { showAuditDialog = true },
                        onOpenDownloadedArtBrowser = onOpenDownloadedArtBrowser,
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
                        onOpenQuickStart = onOpenQuickStart
                    )
                }
            }
        }
    }

    val currentAuditReport = lastAuditReport
    if (showAuditDialog && currentAuditReport != null) {
        AlbumArtAuditDialog(
            report = currentAuditReport,
            onDismiss = { showAuditDialog = false }
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
                    val newTheme = when (pickerType) {
                        "BG" -> themeSettings.copy(customBGRed = r, customBGGreen = g, customBGBlue = b)
                        "SONG" -> themeSettings.copy(customSongTitleRed = r, customSongTitleGreen = g, customSongTitleBlue = b)
                        "ARTIST" -> themeSettings.copy(customArtistTitleRed = r, customArtistTitleGreen = g, customArtistTitleBlue = b)
                        "ALBUM" -> themeSettings.copy(customAlbumTitleRed = r, customAlbumTitleGreen = g, customAlbumTitleBlue = b)
                        else -> themeSettings
                    }
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
    availableFonts: List<FontOption>,
    onPickMusicFolder: () -> Unit,
    onRescanMusicFolder: () -> Unit,
    onDownloadMissingArt: () -> Unit,
    onCancelDownloadArt: () -> Unit,
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
    onOpenDownloadedArtBrowser: () -> Unit = {}
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
            Divider(modifier = Modifier.padding(vertical = 12.dp))

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
                    libraryStats = libraryStats,
                    onPickMusicFolder = onPickMusicFolder,
                    onRescanMusicFolder = onRescanMusicFolder,
                    onDownloadMissingArt = onDownloadMissingArt,
                    onCancelDownloadArt = onCancelDownloadArt,
                    onViewAudit = onViewAudit,
                    onOpenDownloadedArtBrowser = onOpenDownloadedArtBrowser
                )

                SettingsSubmenu.TYPOGRAPHY_HUD -> TypographyHudSettingsContent(
                    displaySettings = displaySettings,
                    availableFonts = availableFonts,
                    onAddFont = onAddFont,
                    onUpdateDisplaySettings = onUpdateDisplaySettings
                )

                SettingsSubmenu.GESTURES -> GesturesSettingsContent(
                    displaySettings = displaySettings,
                    onUpdateDisplaySettings = onUpdateDisplaySettings,
                    onOpenGestureAssignments = onOpenGestureAssignments,
                    onResetGestureAssignments = onResetGestureAssignments
                )

                SettingsSubmenu.THEMES -> ThemesSettingsContent(
                    themeSettings = themeSettings,
                    displaySettings = displaySettings,
                    onSelectPreset = onSelectPreset,
                    onUpdateThemeSettings = onUpdateThemeSettings,
                    onOpenBgPicker = onOpenBgPicker,
                    onOpenSongPicker = onOpenSongPicker,
                    onOpenArtistPicker = onOpenArtistPicker,
                    onOpenAlbumPicker = onOpenAlbumPicker
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
    onPickMusicFolder: () -> Unit,
    onRescanMusicFolder: () -> Unit,
    onDownloadMissingArt: () -> Unit = {},
    onCancelDownloadArt: () -> Unit = {},
    onViewAudit: () -> Unit = {},
    onOpenDownloadedArtBrowser: () -> Unit = {}
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
            headlineContent = { Text("Downloaded Art Browser") },
            supportingContent = {
                Text("Manage, replace, or embed downloaded album artwork into ID3 tags")
            },
            modifier = Modifier.clickable { onOpenDownloadedArtBrowser() }
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
    }
}

@Composable
private fun TypographyHudSettingsContent(
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
                            val currentFontKey = when (rowType) {
                                TitleRowType.ARTIST -> displaySettings.artistFontKey
                                TitleRowType.SONG -> displaySettings.songFontKey
                                TitleRowType.ALBUM -> displaySettings.albumFontKey
                            }
                            val currentFontFamily = FontHelper.getFontFamily(currentFontKey)

                            // Title Header Row in its Selected Font Family
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
                                    fontWeight = FontWeight.Bold,
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

                            // Font Selector & Alignment Chips on Same Row
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
                                Box(modifier = Modifier.weight(1f)) {
                                    FontSelectorRow(
                                        label = "Font Family",
                                        currentFontKey = currentFontKey,
                                        availableFonts = availableFonts,
                                        onFontSelected = { key ->
                                            val newSettings = when (rowType) {
                                                TitleRowType.ARTIST -> displaySettings.copy(artistFontKey = key)
                                                TitleRowType.SONG -> displaySettings.copy(songFontKey = key)
                                                TitleRowType.ALBUM -> displaySettings.copy(albumFontKey = key)
                                            }
                                            onUpdateDisplaySettings(newSettings)
                                        }
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Align:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                            Spacer(modifier = Modifier.height(4.dp))

                            // Font Size Slider
                            val (currentSize, minRange, maxRange) = when (rowType) {
                                TitleRowType.ARTIST -> Triple(displaySettings.artistFontSize, 20f, 100f)
                                TitleRowType.SONG -> Triple(displaySettings.songFontSize, 30f, 120f)
                                TitleRowType.ALBUM -> Triple(displaySettings.albumFontSize, 20f, 100f)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Size: ${currentSize.toInt()} pt",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.width(80.dp)
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
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Title Display Mode", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(6.dp))

        // Toggle placed in the center between Scrolling Marquee and Wrap Titles options (Default: Wrap Titles)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Scrolling Marquee",
                fontWeight = if (displaySettings.titleScrollLong) FontWeight.Bold else FontWeight.Normal,
                color = if (displaySettings.titleScrollLong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.clickable {
                    onUpdateDisplaySettings(displaySettings.copy(titleScrollLong = true))
                }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = !displaySettings.titleScrollLong,
                onCheckedChange = { isWrap ->
                    onUpdateDisplaySettings(displaySettings.copy(titleScrollLong = !isWrap))
                }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Wrap Titles",
                fontWeight = if (!displaySettings.titleScrollLong) FontWeight.Bold else FontWeight.Normal,
                color = if (!displaySettings.titleScrollLong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.clickable {
                    onUpdateDisplaySettings(displaySettings.copy(titleScrollLong = false))
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Album Art Options", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(4.dp))

        Text("Album Art Scale Mode", fontWeight = FontWeight.Bold)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
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

        val portraitOptions = if (displaySettings.albumArtScale == ArtScaleOption.FILL_SCREEN) {
            listOf(ArtAlignmentPortrait.LEFT, ArtAlignmentPortrait.CENTER, ArtAlignmentPortrait.RIGHT)
        } else {
            listOf(ArtAlignmentPortrait.TOP, ArtAlignmentPortrait.MIDDLE, ArtAlignmentPortrait.BOTTOM)
        }

        val landscapeOptions = if (displaySettings.albumArtScale == ArtScaleOption.FILL_SCREEN) {
            listOf(ArtAlignmentLandscape.TOP, ArtAlignmentLandscape.MIDDLE, ArtAlignmentLandscape.BOTTOM)
        } else {
            listOf(ArtAlignmentLandscape.LEFT, ArtAlignmentLandscape.CENTER, ArtAlignmentLandscape.RIGHT)
        }

        Text("Portrait Alignment", fontWeight = FontWeight.Bold)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
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
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
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
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
        ) {
            ArtLayoutOption.entries.forEach { option ->
                FilterChip(
                    selected = displaySettings.artDisplayLayout == option,
                    onClick = {
                        onUpdateDisplaySettings(displaySettings.copy(artDisplayLayout = option))
                    },
                    label = { Text(option.displayName) }
                )
            }
        }

        Divider(modifier = Modifier.padding(vertical = 12.dp))

        // Section: HUD & Progress Displays
        Text("HUD & Progress Displays", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)

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

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text("Always Show Volume Overlay", modifier = Modifier.weight(1f))
            Switch(
                checked = displaySettings.volumeAlwaysOn,
                onCheckedChange = { checked ->
                    onUpdateDisplaySettings(displaySettings.copy(volumeAlwaysOn = checked))
                }
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text("Show Album Art", modifier = Modifier.weight(1f))
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
            Text(
                text = "Number of Edge Regions: ${displaySettings.numEdgeRegions}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Configures active region slots (1 to 7) along top & bottom screen edges",
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
        }
        Divider()
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
    onSelectPreset: (String, Boolean) -> Unit,
    onUpdateThemeSettings: (ThemeSettings) -> Unit,
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

        Spacer(modifier = Modifier.height(8.dp))
        Text("Theme Appearance Options", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text("Rounded Corners", modifier = Modifier.weight(1f))
            Switch(
                checked = themeSettings.isRounded,
                onCheckedChange = { checked ->
                    onUpdateThemeSettings(themeSettings.copy(isRounded = checked))
                }
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text("Glass Effect (Translucent & Blur)", modifier = Modifier.weight(1f))
            Switch(
                checked = themeSettings.isGlass,
                onCheckedChange = { checked ->
                    onUpdateThemeSettings(themeSettings.copy(isGlass = checked))
                }
            )
        }

        if (themeSettings.isRounded || themeSettings.isGlass) {
            val styleBadgeText = when {
                themeSettings.isRounded && themeSettings.isGlass -> "✨ Round Glass Style Active"
                themeSettings.isRounded -> "✨ Rounded Style Active"
                else -> "✨ Glass Style Active"
            }
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = styleBadgeText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
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

@Composable
private fun FontSelectorRow(
    label: String,
    currentFontKey: String,
    availableFonts: List<FontOption>,
    onFontSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedFontName = availableFonts.find { it.key == currentFontKey }?.displayName ?: "System Default"

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
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
                    Text(selectedFontName, color = MaterialTheme.colorScheme.onSurface)
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
                    DropdownMenuItem(
                        text = { Text(fontOption.displayName) },
                        onClick = {
                            onFontSelected(fontOption.key)
                            expanded = false
                        }
                    )
                }
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
    var red by remember { mutableStateOf(initialRed) }
    var green by remember { mutableStateOf(initialGreen) }
    var blue by remember { mutableStateOf(initialBlue) }

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
