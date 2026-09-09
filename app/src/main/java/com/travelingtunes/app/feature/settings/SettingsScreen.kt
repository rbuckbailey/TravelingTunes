package com.travelingtunes.app.feature.settings

import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
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
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.travelingtunes.app.core.model.ArtAlignmentLandscape
import com.travelingtunes.app.core.model.ArtAlignmentPortrait
import com.travelingtunes.app.core.model.ArtLayoutOption
import com.travelingtunes.app.core.model.ArtScaleOption
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
    onPickMusicFolder: () -> Unit = {},
    onRescanMusicFolder: () -> Unit = {},
    onNavigateBack: () -> Unit,
    onOpenGestureAssignments: () -> Unit,
    onOpenQuickStart: () -> Unit
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

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val isTightPortrait = isPortrait && screenWidthDp < 600
    val isWideTablet = screenWidthDp >= 600

    var libraryExpanded by rememberSaveable(isWideTablet, isTightPortrait) { mutableStateOf(!isTightPortrait) }
    var gesturesExpanded by rememberSaveable(isWideTablet, isTightPortrait) { mutableStateOf(!isTightPortrait) }
    var titlesArtExpanded by rememberSaveable(isWideTablet, isTightPortrait) { mutableStateOf(!isTightPortrait) }
    var themesExpanded by rememberSaveable(isWideTablet, isTightPortrait) { mutableStateOf(!isTightPortrait) }
    var aboutExpanded by rememberSaveable(isWideTablet, isTightPortrait) { mutableStateOf(!isTightPortrait) }

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
                title = { Text("Traveling Tunes Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (isWideTablet) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Column 1: Titles and Art (Long), About (Very Short)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        TitlesAndArtSettingsCard(
                            expanded = titlesArtExpanded,
                            onExpandToggle = { titlesArtExpanded = !titlesArtExpanded },
                            displaySettings = displaySettings,
                            availableFonts = availableFonts,
                            onAddFont = { fontPickerLauncher.launch(arrayOf("*/*")) },
                            onUpdateDisplaySettings = { newSettings ->
                                coroutineScope.launch {
                                    settingsDataStore.updateDisplaySettings(newSettings)
                                }
                            }
                        )

                        AboutSettingsCard(
                            expanded = aboutExpanded,
                            onExpandToggle = { aboutExpanded = !aboutExpanded },
                            onOpenQuickStart = onOpenQuickStart
                        )
                    }

                    // Column 2: Library (Short), Gestures (Short), Themes (Medium)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        LibrarySettingsCard(
                            expanded = libraryExpanded,
                            onExpandToggle = { libraryExpanded = !libraryExpanded },
                            musicFolderName = musicFolderName,
                            isScanning = isScanning,
                            lastScanTime = lastScanTime,
                            scanStatusMessage = scanStatusMessage,
                            libraryStats = libraryStats,
                            onPickMusicFolder = onPickMusicFolder,
                            onRescanMusicFolder = onRescanMusicFolder
                        )

                        GesturesSettingsCard(
                            expanded = gesturesExpanded,
                            onExpandToggle = { gesturesExpanded = !gesturesExpanded },
                            onOpenGestureAssignments = onOpenGestureAssignments,
                            onResetGestureAssignments = {
                                coroutineScope.launch {
                                    settingsDataStore.resetAllGestureBindings()
                                }
                            }
                        )

                        ThemesSettingsCard(
                            expanded = themesExpanded,
                            onExpandToggle = { themesExpanded = !themesExpanded },
                            themeSettings = themeSettings,
                            displaySettings = displaySettings,
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
                            onOpenTextPicker = { activeColorPicker = "TEXT" }
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LibrarySettingsCard(
                        expanded = libraryExpanded,
                        onExpandToggle = { libraryExpanded = !libraryExpanded },
                        musicFolderName = musicFolderName,
                        isScanning = isScanning,
                        lastScanTime = lastScanTime,
                        scanStatusMessage = scanStatusMessage,
                        libraryStats = libraryStats,
                        onPickMusicFolder = onPickMusicFolder,
                        onRescanMusicFolder = onRescanMusicFolder
                    )

                    GesturesSettingsCard(
                        expanded = gesturesExpanded,
                        onExpandToggle = { gesturesExpanded = !gesturesExpanded },
                        onOpenGestureAssignments = onOpenGestureAssignments,
                        onResetGestureAssignments = {
                            coroutineScope.launch {
                                settingsDataStore.resetAllGestureBindings()
                            }
                        }
                    )

                    TitlesAndArtSettingsCard(
                        expanded = titlesArtExpanded,
                        onExpandToggle = { titlesArtExpanded = !titlesArtExpanded },
                        displaySettings = displaySettings,
                        availableFonts = availableFonts,
                        onAddFont = { fontPickerLauncher.launch(arrayOf("*/*")) },
                        onUpdateDisplaySettings = { newSettings ->
                            coroutineScope.launch {
                                settingsDataStore.updateDisplaySettings(newSettings)
                            }
                        }
                    )

                    ThemesSettingsCard(
                        expanded = themesExpanded,
                        onExpandToggle = { themesExpanded = !themesExpanded },
                        themeSettings = themeSettings,
                        displaySettings = displaySettings,
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
                        onOpenTextPicker = { activeColorPicker = "TEXT" }
                    )

                    AboutSettingsCard(
                        expanded = aboutExpanded,
                        onExpandToggle = { aboutExpanded = !aboutExpanded },
                        onOpenQuickStart = onOpenQuickStart
                    )
                }
            }
        }

        if (activeColorPicker == "BG") {
            val initialBg = Color(
                red = (themeSettings.customBGRed / 255f).coerceIn(0f, 1f),
                green = (themeSettings.customBGGreen / 255f).coerceIn(0f, 1f),
                blue = (themeSettings.customBGBlue / 255f).coerceIn(0f, 1f)
            )
            ColorPickerDialog(
                title = "Custom Background Color",
                initialColor = initialBg,
                onColorSelected = { selected ->
                    coroutineScope.launch {
                        settingsDataStore.updateThemeSettings(
                            themeSettings.copy(
                                customBGRed = selected.red * 255f,
                                customBGGreen = selected.green * 255f,
                                customBGBlue = selected.blue * 255f,
                                currentThemeName = "Custom"
                            )
                        )
                    }
                },
                onDismiss = { activeColorPicker = null }
            )
        } else if (activeColorPicker == "TEXT") {
            val initialText = Color(
                red = (themeSettings.customTextRed / 255f).coerceIn(0f, 1f),
                green = (themeSettings.customTextGreen / 255f).coerceIn(0f, 1f),
                blue = (themeSettings.customTextBlue / 255f).coerceIn(0f, 1f)
            )
            ColorPickerDialog(
                title = "Custom Text / Title Color",
                initialColor = initialText,
                onColorSelected = { selected ->
                    coroutineScope.launch {
                        settingsDataStore.updateThemeSettings(
                            themeSettings.copy(
                                customTextRed = selected.red * 255f,
                                customTextGreen = selected.green * 255f,
                                customTextBlue = selected.blue * 255f,
                                currentThemeName = "Custom"
                            )
                        )
                    }
                },
                onDismiss = { activeColorPicker = null }
            )
        }
    }
}

@Composable
fun CollapsibleSettingsCard(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandToggle() }
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse section" else "Expand section",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    Divider(
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    content()
                }
            }
        }
    }
}

@Composable
private fun LibrarySettingsCard(
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    musicFolderName: String?,
    isScanning: Boolean,
    lastScanTime: Long,
    scanStatusMessage: String?,
    libraryStats: LibraryStats,
    onPickMusicFolder: () -> Unit,
    onRescanMusicFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    CollapsibleSettingsCard(
        title = "Library",
        icon = Icons.Default.LibraryMusic,
        expanded = expanded,
        onExpandToggle = onExpandToggle,
        modifier = modifier
    ) {
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
private fun GesturesSettingsCard(
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onOpenGestureAssignments: () -> Unit,
    onResetGestureAssignments: () -> Unit,
    modifier: Modifier = Modifier
) {
    CollapsibleSettingsCard(
        title = "Gestures",
        icon = Icons.Default.TouchApp,
        expanded = expanded,
        onExpandToggle = onExpandToggle,
        modifier = modifier
    ) {
        ListItem(
            headlineContent = { Text("Reconfigure Gesture Assignments") },
            supportingContent = { Text("Customize 1/2/3 finger swipes, taps, long presses, and corners") },
            modifier = Modifier.clickable { onOpenGestureAssignments() }
        )
        ListItem(
            headlineContent = { Text("Reset All Gesture Assignments") },
            modifier = Modifier.clickable { onResetGestureAssignments() }
        )
    }
}

@Composable
private fun TitlesAndArtSettingsCard(
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    displaySettings: DisplaySettings,
    availableFonts: List<FontOption>,
    onAddFont: () -> Unit,
    onUpdateDisplaySettings: (DisplaySettings) -> Unit,
    modifier: Modifier = Modifier
) {
    CollapsibleSettingsCard(
        title = "Titles and Art",
        icon = Icons.Default.Title,
        expanded = expanded,
        onExpandToggle = onExpandToggle,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Typography & Labels", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
            OutlinedButton(
                onClick = onAddFont,
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Font", fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Artist Label Font & Size
        FontSelectorRow(
            label = "Artist Label Font Family",
            currentFontKey = displaySettings.artistFontKey,
            availableFonts = availableFonts,
            onFontSelected = { key ->
                onUpdateDisplaySettings(displaySettings.copy(artistFontKey = key))
            }
        )
        Text("Artist Font Size: ${displaySettings.artistFontSize.toInt()} pt", modifier = Modifier.padding(top = 4.dp))
        Slider(
            value = displaySettings.artistFontSize,
            onValueChange = { size ->
                onUpdateDisplaySettings(displaySettings.copy(artistFontSize = size))
            },
            valueRange = 20f..100f
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Song Label Font & Size
        FontSelectorRow(
            label = "Song Title Label Font Family",
            currentFontKey = displaySettings.songFontKey,
            availableFonts = availableFonts,
            onFontSelected = { key ->
                onUpdateDisplaySettings(displaySettings.copy(songFontKey = key))
            }
        )
        Text("Song Title Font Size: ${displaySettings.songFontSize.toInt()} pt", modifier = Modifier.padding(top = 4.dp))
        Slider(
            value = displaySettings.songFontSize,
            onValueChange = { size ->
                onUpdateDisplaySettings(displaySettings.copy(songFontSize = size))
            },
            valueRange = 30f..120f
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Album Label Font & Size
        FontSelectorRow(
            label = "Album Label Font Family",
            currentFontKey = displaySettings.albumFontKey,
            availableFonts = availableFonts,
            onFontSelected = { key ->
                onUpdateDisplaySettings(displaySettings.copy(albumFontKey = key))
            }
        )
        Text("Album Font Size: ${displaySettings.albumFontSize.toInt()} pt", modifier = Modifier.padding(top = 4.dp))
        Slider(
            value = displaySettings.albumFontSize,
            onValueChange = { size ->
                onUpdateDisplaySettings(displaySettings.copy(albumFontSize = size))
            },
            valueRange = 20f..100f
        )

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            Text("Scroll Long Titles (Marquee)", modifier = Modifier.weight(1f))
            Switch(
                checked = displaySettings.titleScrollLong,
                onCheckedChange = { checked ->
                    onUpdateDisplaySettings(displaySettings.copy(titleScrollLong = checked))
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
private fun ThemesSettingsCard(
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    themeSettings: ThemeSettings,
    displaySettings: DisplaySettings,
    onSelectPreset: (String, Boolean) -> Unit,
    onUpdateThemeSettings: (ThemeSettings) -> Unit,
    onOpenBgPicker: () -> Unit,
    onOpenTextPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    CollapsibleSettingsCard(
        title = "Themes",
        icon = Icons.Default.Palette,
        expanded = expanded,
        onExpandToggle = onExpandToggle,
        modifier = modifier
    ) {
        val isAutoByArtActive = themeSettings.currentThemeName.equals("Auto By Art", ignoreCase = true) || displaySettings.albumArtColors

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Auto By Art Theme", fontWeight = FontWeight.SemiBold)
                Text("Extract dynamic color palette from album art", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = isAutoByArtActive,
                onCheckedChange = { checked ->
                    val newThemeName = if (checked) "Auto By Art" else "White on Grey"
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
                        val isAuto = theme.name.equals("Auto By Art", ignoreCase = true)
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
                    onOpenTextPicker = onOpenTextPicker
                )
            }
        }
    }
}

@Composable
private fun AboutSettingsCard(
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onOpenQuickStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    CollapsibleSettingsCard(
        title = "About",
        icon = Icons.Default.Info,
        expanded = expanded,
        onExpandToggle = onExpandToggle,
        modifier = modifier
    ) {
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
    onOpenTextPicker: () -> Unit
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
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            val bgColor = Color(
                red = (themeSettings.customBGRed / 255f).coerceIn(0f, 1f),
                green = (themeSettings.customBGGreen / 255f).coerceIn(0f, 1f),
                blue = (themeSettings.customBGBlue / 255f).coerceIn(0f, 1f)
            )
            val textColor = Color(
                red = (themeSettings.customTextRed / 255f).coerceIn(0f, 1f),
                green = (themeSettings.customTextGreen / 255f).coerceIn(0f, 1f),
                blue = (themeSettings.customTextBlue / 255f).coerceIn(0f, 1f)
            )

            PaintBucketButton(
                label = "Background",
                color = bgColor,
                onClick = onOpenBgPicker
            )

            Spacer(modifier = Modifier.width(36.dp))

            PaintBucketButton(
                label = "Text / Title",
                color = textColor,
                onClick = onOpenTextPicker
            )
        }
    }
}

@Composable
fun PaintBucketButton(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            shape = CircleShape,
            color = color,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
            shadowElevation = 6.dp,
            modifier = Modifier.size(60.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.FormatPaint,
                    contentDescription = label,
                    tint = if (color.luminance() < 0.5f) Color.White else Color.Black,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ColorPickerDialog(
    title: String,
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var red by remember { mutableStateOf(initialColor.red * 255f) }
    var green by remember { mutableStateOf(initialColor.green * 255f) }
    var blue by remember { mutableStateOf(initialColor.blue * 255f) }

    val initialHsv = FloatArray(3)
    android.graphics.Color.colorToHSV(
        android.graphics.Color.rgb(
            (initialColor.red * 255).toInt().coerceIn(0, 255),
            (initialColor.green * 255).toInt().coerceIn(0, 255),
            (initialColor.blue * 255).toInt().coerceIn(0, 255)
        ),
        initialHsv
    )
    var hue by remember { mutableStateOf(initialHsv[0]) }
    var sat by remember { mutableStateOf(initialHsv[1]) }
    var valBrightness by remember { mutableStateOf(initialHsv[2]) }

    fun updateFromRgb(r: Float, g: Float, b: Float) {
        red = r
        green = g
        blue = b
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(
            android.graphics.Color.rgb(r.toInt().coerceIn(0, 255), g.toInt().coerceIn(0, 255), b.toInt().coerceIn(0, 255)),
            hsv
        )
        hue = hsv[0]
        sat = hsv[1]
        valBrightness = hsv[2]
    }

    fun updateFromHsv(h: Float, s: Float, v: Float) {
        hue = h
        sat = s
        valBrightness = v
        val colorInt = android.graphics.Color.HSVToColor(floatArrayOf(h.coerceIn(0f, 360f), s.coerceIn(0f, 1f), v.coerceIn(0f, 1f)))
        red = android.graphics.Color.red(colorInt).toFloat()
        green = android.graphics.Color.green(colorInt).toFloat()
        blue = android.graphics.Color.blue(colorInt).toFloat()
    }

    val currentColor = Color(red / 255f, green / 255f, blue / 255f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Color Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(currentColor)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val hexStr = String.format("#%02X%02X%02X", red.toInt().coerceIn(0, 255), green.toInt().coerceIn(0, 255), blue.toInt().coerceIn(0, 255))
                    Text(
                        text = hexStr,
                        color = if (currentColor.luminance() < 0.5f) Color.White else Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Hue - Saturation - Brightness (HSB)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)

                Text("Hue: ${hue.toInt()}°", fontSize = 12.sp)
                Slider(
                    value = hue,
                    onValueChange = { h -> updateFromHsv(h, sat, valBrightness) },
                    valueRange = 0f..360f
                )

                Text("Saturation: ${(sat * 100).toInt()}%", fontSize = 12.sp)
                Slider(
                    value = sat,
                    onValueChange = { s -> updateFromHsv(hue, s, valBrightness) },
                    valueRange = 0f..1f
                )

                Text("Brightness: ${(valBrightness * 100).toInt()}%", fontSize = 12.sp)
                Slider(
                    value = valBrightness,
                    onValueChange = { v -> updateFromHsv(hue, sat, v) },
                    valueRange = 0f..1f
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Red - Green - Blue (RGB)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)

                Text("Red: ${red.toInt()}", fontSize = 12.sp)
                Slider(
                    value = red,
                    onValueChange = { r -> updateFromRgb(r, green, blue) },
                    valueRange = 0f..255f
                )

                Text("Green: ${green.toInt()}", fontSize = 12.sp)
                Slider(
                    value = green,
                    onValueChange = { g -> updateFromRgb(red, g, blue) },
                    valueRange = 0f..255f
                )

                Text("Blue: ${red.toInt()}", fontSize = 12.sp)
                Slider(
                    value = blue,
                    onValueChange = { b -> updateFromRgb(red, green, b) },
                    valueRange = 0f..255f
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onColorSelected(currentColor)
                onDismiss()
            }) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontSelectorRow(
    label: String,
    currentFontKey: String,
    availableFonts: List<FontOption>,
    onFontSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentOption = availableFonts.find { it.key == currentFontKey } ?: FontOption("DEFAULT", "System Default")

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        OutlinedCard(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = currentOption.displayName,
                    fontFamily = FontHelper.getFontFamily(currentOption.key),
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Font")
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.85f).heightIn(max = 350.dp)
        ) {
            availableFonts.forEach { font ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = font.displayName,
                            fontFamily = FontHelper.getFontFamily(font.key),
                            fontWeight = if (font.key == currentFontKey) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onFontSelected(font.key)
                        expanded = false
                    },
                    trailingIcon = if (font.key == currentFontKey) {
                        { Text("✓", color = MaterialTheme.colorScheme.primary) }
                    } else null
                )
            }
        }
    }
}
