package com.travelingtunes.app.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelingtunes.app.core.database.LibraryStats
import com.travelingtunes.app.core.datastore.SettingsDataStore
import com.travelingtunes.app.core.model.ArtAlignmentLandscape
import com.travelingtunes.app.core.model.ArtAlignmentPortrait
import com.travelingtunes.app.core.model.ArtScaleOption
import com.travelingtunes.app.core.model.ColorTheme
import com.travelingtunes.app.core.model.DisplaySettings
import com.travelingtunes.app.core.model.HudTypeOption
import com.travelingtunes.app.core.model.ScrubHudTypeOption
import com.travelingtunes.app.core.model.ThemeSettings
import com.travelingtunes.app.core.theme.luminance
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class ColorTarget {
    BACKGROUND,
    TEXT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsDataStore: SettingsDataStore,
    displaySettings: DisplaySettings,
    themeSettings: ThemeSettings,
    homeAddress: String,
    workAddress: String,
    musicFolderName: String? = null,
    lastScanTime: Long = 0L,
    libraryStats: LibraryStats = LibraryStats(),
    isScanning: Boolean = false,
    scanStatusMessage: String? = null,
    onPickMusicFolder: () -> Unit = {},
    onRescanMusicFolder: () -> Unit = {},
    onNavigateBack: () -> Unit,
    onOpenGestureAssignments: () -> Unit,
    onOpenQuickStart: () -> Unit,
    onOpenContacts: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Collapsed by default
    var isLibraryExpanded by remember { mutableStateOf(false) }
    var isGesturesExpanded by remember { mutableStateOf(false) }
    var isTitlesAndArtExpanded by remember { mutableStateOf(false) }
    var isThemesExpanded by remember { mutableStateOf(false) }
    var isAboutExpanded by remember { mutableStateOf(false) }

    var activeColorPickerTarget by remember { mutableStateOf<ColorTarget?>(null) }

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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isTabletWide = this.maxWidth >= 600.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                if (isTabletWide) {
                    // Side-by-side tablet layout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            CollapsibleSection(
                                title = "Library",
                                icon = Icons.Default.LibraryMusic,
                                isExpanded = true,
                                onToggleExpand = {}
                            ) {
                                LibrarySectionContent(
                                    musicFolderName = musicFolderName,
                                    isScanning = isScanning,
                                    scanStatusMessage = scanStatusMessage,
                                    lastScanTime = lastScanTime,
                                    libraryStats = libraryStats,
                                    onPickMusicFolder = onPickMusicFolder,
                                    onRescanMusicFolder = onRescanMusicFolder
                                )
                            }

                            CollapsibleSection(
                                title = "Gestures",
                                icon = Icons.Default.TouchApp,
                                isExpanded = true,
                                onToggleExpand = {}
                            ) {
                                GesturesSectionContent(
                                    onOpenGestureAssignments = onOpenGestureAssignments,
                                    onResetBindings = {
                                        coroutineScope.launch {
                                            settingsDataStore.resetAllGestureBindings()
                                        }
                                    }
                                )
                            }

                            CollapsibleSection(
                                title = "About",
                                icon = Icons.Default.Info,
                                isExpanded = true,
                                onToggleExpand = {}
                            ) {
                                AboutSectionContent(onOpenQuickStart = onOpenQuickStart)
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            CollapsibleSection(
                                title = "Titles and Art",
                                icon = Icons.Default.TextFields,
                                isExpanded = true,
                                onToggleExpand = {}
                            ) {
                                TitlesAndArtSectionContent(
                                    displaySettings = displaySettings,
                                    onUpdateDisplaySettings = { newSettings ->
                                        coroutineScope.launch {
                                            settingsDataStore.updateDisplaySettings(newSettings)
                                        }
                                    }
                                )
                            }

                            CollapsibleSection(
                                title = "Themes",
                                icon = Icons.Default.ColorLens,
                                isExpanded = true,
                                onToggleExpand = {}
                            ) {
                                ThemesSectionContent(
                                    themeSettings = themeSettings,
                                    displaySettings = displaySettings,
                                    onSelectTheme = { themeName ->
                                        coroutineScope.launch {
                                            settingsDataStore.updateThemeSettings(themeSettings.copy(currentThemeName = themeName))
                                        }
                                    },
                                    onUpdateDisplaySettings = { newSettings ->
                                        coroutineScope.launch {
                                            settingsDataStore.updateDisplaySettings(newSettings)
                                        }
                                    },
                                    onOpenColorPicker = { target ->
                                        activeColorPickerTarget = target
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Vertical collapsible layout (collapsed by default)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        CollapsibleSection(
                            title = "Library",
                            icon = Icons.Default.LibraryMusic,
                            isExpanded = isLibraryExpanded,
                            onToggleExpand = { isLibraryExpanded = !isLibraryExpanded }
                        ) {
                            LibrarySectionContent(
                                musicFolderName = musicFolderName,
                                isScanning = isScanning,
                                scanStatusMessage = scanStatusMessage,
                                lastScanTime = lastScanTime,
                                libraryStats = libraryStats,
                                onPickMusicFolder = onPickMusicFolder,
                                onRescanMusicFolder = onRescanMusicFolder
                            )
                        }

                        CollapsibleSection(
                            title = "Gestures",
                            icon = Icons.Default.TouchApp,
                            isExpanded = isGesturesExpanded,
                            onToggleExpand = { isGesturesExpanded = !isGesturesExpanded }
                        ) {
                            GesturesSectionContent(
                                onOpenGestureAssignments = onOpenGestureAssignments,
                                onResetBindings = {
                                    coroutineScope.launch {
                                        settingsDataStore.resetAllGestureBindings()
                                    }
                                }
                            )
                        }

                        CollapsibleSection(
                            title = "Titles and Art",
                            icon = Icons.Default.TextFields,
                            isExpanded = isTitlesAndArtExpanded,
                            onToggleExpand = { isTitlesAndArtExpanded = !isTitlesAndArtExpanded }
                        ) {
                            TitlesAndArtSectionContent(
                                displaySettings = displaySettings,
                                onUpdateDisplaySettings = { newSettings ->
                                    coroutineScope.launch {
                                        settingsDataStore.updateDisplaySettings(newSettings)
                                    }
                                }
                            )
                        }

                        CollapsibleSection(
                            title = "Themes",
                            icon = Icons.Default.ColorLens,
                            isExpanded = isThemesExpanded,
                            onToggleExpand = { isThemesExpanded = !isThemesExpanded }
                        ) {
                            ThemesSectionContent(
                                themeSettings = themeSettings,
                                displaySettings = displaySettings,
                                onSelectTheme = { themeName ->
                                    coroutineScope.launch {
                                        settingsDataStore.updateThemeSettings(themeSettings.copy(currentThemeName = themeName))
                                    }
                                },
                                onUpdateDisplaySettings = { newSettings ->
                                    coroutineScope.launch {
                                        settingsDataStore.updateDisplaySettings(newSettings)
                                    }
                                },
                                onOpenColorPicker = { target ->
                                    activeColorPickerTarget = target
                                }
                            )
                        }

                        CollapsibleSection(
                            title = "About",
                            icon = Icons.Default.Info,
                            isExpanded = isAboutExpanded,
                            onToggleExpand = { isAboutExpanded = !isAboutExpanded }
                        ) {
                            AboutSectionContent(onOpenQuickStart = onOpenQuickStart)
                        }
                    }
                }
            }

            // Color Picker Popup Dialog
            if (activeColorPickerTarget != null) {
                val target = activeColorPickerTarget!!
                val isBg = target == ColorTarget.BACKGROUND
                val initialColor = if (isBg) {
                    Color(
                        red = (themeSettings.customBGRed / 255f).coerceIn(0f, 1f),
                        green = (themeSettings.customBGGreen / 255f).coerceIn(0f, 1f),
                        blue = (themeSettings.customBGBlue / 255f).coerceIn(0f, 1f)
                    )
                } else {
                    Color(
                        red = (themeSettings.customTextRed / 255f).coerceIn(0f, 1f),
                        green = (themeSettings.customTextGreen / 255f).coerceIn(0f, 1f),
                        blue = (themeSettings.customTextBlue / 255f).coerceIn(0f, 1f)
                    )
                }

                ColorPickerDialog(
                    title = if (isBg) "Custom Background Color" else "Custom Text & Icons Color",
                    initialColor = initialColor,
                    onDismiss = { activeColorPickerTarget = null },
                    onColorSelected = { r, g, b ->
                        coroutineScope.launch {
                            val updated = if (isBg) {
                                themeSettings.copy(customBGRed = r, customBGGreen = g, customBGBlue = b)
                            } else {
                                themeSettings.copy(customTextRed = r, customTextGreen = g, customTextBlue = b)
                            }
                            settingsDataStore.updateThemeSettings(updated)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun CollapsibleSection(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun LibrarySectionContent(
    musicFolderName: String?,
    isScanning: Boolean,
    scanStatusMessage: String?,
    lastScanTime: Long,
    libraryStats: LibraryStats,
    onPickMusicFolder: () -> Unit,
    onRescanMusicFolder: () -> Unit
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
                val dateStr = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(lastScanTime))
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

@Composable
fun GesturesSectionContent(
    onOpenGestureAssignments: () -> Unit,
    onResetBindings: () -> Unit
) {
    ListItem(
        headlineContent = { Text("Reconfigure Gesture Assignments") },
        supportingContent = { Text("Customize 1/2/3 finger swipes, taps, long presses, and corners") },
        modifier = Modifier.clickable { onOpenGestureAssignments() }
    )
    ListItem(
        headlineContent = { Text("Reset All Gesture Assignments") },
        modifier = Modifier.clickable { onResetBindings() }
    )
}

@Composable
fun TitlesAndArtSectionContent(
    displaySettings: DisplaySettings,
    onUpdateDisplaySettings: (DisplaySettings) -> Unit
) {
    Text("Artist Font Size: ${displaySettings.artistFontSize.toInt()} pt")
    Slider(
        value = displaySettings.artistFontSize,
        onValueChange = { size -> onUpdateDisplaySettings(displaySettings.copy(artistFontSize = size)) },
        valueRange = 20f..100f
    )

    Text("Song Title Font Size: ${displaySettings.songFontSize.toInt()} pt")
    Slider(
        value = displaySettings.songFontSize,
        onValueChange = { size -> onUpdateDisplaySettings(displaySettings.copy(songFontSize = size)) },
        valueRange = 30f..120f
    )

    Text("Album Font Size: ${displaySettings.albumFontSize.toInt()} pt")
    Slider(
        value = displaySettings.albumFontSize,
        onValueChange = { size -> onUpdateDisplaySettings(displaySettings.copy(albumFontSize = size)) },
        valueRange = 20f..100f
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Scroll Long Titles (Marquee)", modifier = Modifier.weight(1f))
        Switch(
            checked = displaySettings.titleScrollLong,
            onCheckedChange = { checked -> onUpdateDisplaySettings(displaySettings.copy(titleScrollLong = checked)) }
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Text("Volume Bar Display Style", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        HudTypeOption.entries.forEach { option ->
            FilterChip(
                selected = displaySettings.hudType == option,
                onClick = { onUpdateDisplaySettings(displaySettings.copy(hudType = option)) },
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
                onClick = { onUpdateDisplaySettings(displaySettings.copy(scrubHudType = option)) },
                label = { Text(option.displayName) }
            )
        }
    }

    Text("HUD Line & Bar Thickness: ${displaySettings.hudLineThickness.toInt()} dp", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
    Slider(
        value = displaySettings.hudLineThickness,
        onValueChange = { thickness -> onUpdateDisplaySettings(displaySettings.copy(hudLineThickness = thickness)) },
        valueRange = 6f..40f
    )

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("Always Show Volume Overlay", modifier = Modifier.weight(1f))
        Switch(
            checked = displaySettings.volumeAlwaysOn,
            onCheckedChange = { checked -> onUpdateDisplaySettings(displaySettings.copy(volumeAlwaysOn = checked)) }
        )
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("Show Album Art", modifier = Modifier.weight(1f))
        Switch(
            checked = displaySettings.showAlbumArt,
            onCheckedChange = { checked -> onUpdateDisplaySettings(displaySettings.copy(showAlbumArt = checked)) }
        )
    }

    if (displaySettings.showAlbumArt) {
        Spacer(modifier = Modifier.height(8.dp))
        Text("Album Art Scale Mode", fontWeight = FontWeight.Bold)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
        ) {
            ArtScaleOption.entries.forEach { option ->
                FilterChip(
                    selected = displaySettings.albumArtScale == option,
                    onClick = { onUpdateDisplaySettings(displaySettings.copy(albumArtScale = option)) },
                    label = { Text(option.displayName) }
                )
            }
        }

        Text("Portrait Alignment", fontWeight = FontWeight.Bold)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
        ) {
            ArtAlignmentPortrait.entries.forEach { option ->
                FilterChip(
                    selected = displaySettings.artAlignmentPortrait == option,
                    onClick = { onUpdateDisplaySettings(displaySettings.copy(artAlignmentPortrait = option)) },
                    label = { Text(option.displayName) }
                )
            }
        }

        Text("Landscape Alignment", fontWeight = FontWeight.Bold)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
        ) {
            ArtAlignmentLandscape.entries.forEach { option ->
                FilterChip(
                    selected = displaySettings.artAlignmentLandscape == option,
                    onClick = { onUpdateDisplaySettings(displaySettings.copy(artAlignmentLandscape = option)) },
                    label = { Text(option.displayName) }
                )
            }
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("Prevent Screen Autolock (Keep Screen On)", modifier = Modifier.weight(1f))
        Switch(
            checked = displaySettings.disableAutolock,
            onCheckedChange = { checked -> onUpdateDisplaySettings(displaySettings.copy(disableAutolock = checked)) }
        )
    }
}

@Composable
fun ThemesSectionContent(
    themeSettings: ThemeSettings,
    displaySettings: DisplaySettings,
    onSelectTheme: (String) -> Unit,
    onUpdateDisplaySettings: (DisplaySettings) -> Unit,
    onOpenColorPicker: (ColorTarget) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text("Auto By Art (Override Theme with Album Art)", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Switch(
            checked = displaySettings.albumArtColors,
            onCheckedChange = { checked -> onUpdateDisplaySettings(displaySettings.copy(albumArtColors = checked)) }
        )
    }

    ColorTheme.PRESETS.forEach { theme ->
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectTheme(theme.name) }
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

            if (theme.name.equals("Custom", ignoreCase = true) && themeSettings.currentThemeName.equals("Custom", ignoreCase = true)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 4.dp, bottom = 12.dp)
                ) {
                    PaintBucketItem(
                        label = "Background",
                        color = Color(
                            red = (themeSettings.customBGRed / 255f).coerceIn(0f, 1f),
                            green = (themeSettings.customBGGreen / 255f).coerceIn(0f, 1f),
                            blue = (themeSettings.customBGBlue / 255f).coerceIn(0f, 1f)
                        ),
                        onClick = { onOpenColorPicker(ColorTarget.BACKGROUND) }
                    )

                    PaintBucketItem(
                        label = "Text & Icons",
                        color = Color(
                            red = (themeSettings.customTextRed / 255f).coerceIn(0f, 1f),
                            green = (themeSettings.customTextGreen / 255f).coerceIn(0f, 1f),
                            blue = (themeSettings.customTextBlue / 255f).coerceIn(0f, 1f)
                        ),
                        onClick = { onOpenColorPicker(ColorTarget.TEXT) }
                    )
                }
            }
        }
    }
}

@Composable
fun AboutSectionContent(
    onOpenQuickStart: () -> Unit
) {
    ListItem(
        headlineContent = { Text("Show Quick Start Tutorial") },
        modifier = Modifier.clickable { onOpenQuickStart() }
    )
}

@Composable
fun PaintBucketItem(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color)
                .border(2.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FormatColorFill,
                contentDescription = label,
                tint = if (color.luminance() < 0.5f) Color.White else Color.Black,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ColorPickerDialog(
    title: String,
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (r: Float, g: Float, b: Float) -> Unit
) {
    var red by remember { mutableStateOf((initialColor.red * 255f).roundToInt()) }
    var green by remember { mutableStateOf((initialColor.green * 255f).roundToInt()) }
    var blue by remember { mutableStateOf((initialColor.blue * 255f).roundToInt()) }

    var hsv by remember { mutableStateOf(rgbToHsv(red, green, blue)) }
    var hue by remember { mutableStateOf(hsv[0]) }
    var saturation by remember { mutableStateOf(hsv[1] * 100f) }
    var brightness by remember { mutableStateOf(hsv[2] * 100f) }

    val currentColor = Color(red, green, blue)

    fun updateFromRgb(r: Int, g: Int, b: Int) {
        red = r.coerceIn(0, 255)
        green = g.coerceIn(0, 255)
        blue = b.coerceIn(0, 255)
        val newHsv = rgbToHsv(red, green, blue)
        hue = newHsv[0]
        saturation = newHsv[1] * 100f
        brightness = newHsv[2] * 100f
    }

    fun updateFromHsb(h: Float, s: Float, b: Float) {
        hue = h.coerceIn(0f, 360f)
        saturation = s.coerceIn(0f, 100f)
        brightness = b.coerceIn(0f, 100f)
        val (newR, newG, newB) = hsvToRgb(hue, saturation / 100f, brightness / 100f)
        red = newR
        green = newG
        blue = newB
    }

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
                        .height(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(currentColor)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#%02X%02X%02X".format(red, green, blue),
                        color = if (currentColor.luminance() < 0.5f) Color.White else Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // RGB Section
                Text("RGB Colors", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Red: $red", fontSize = 12.sp)
                Slider(
                    value = red.toFloat(),
                    onValueChange = { updateFromRgb(it.roundToInt(), green, blue) },
                    valueRange = 0f..255f
                )

                Text("Green: $green", fontSize = 12.sp)
                Slider(
                    value = green.toFloat(),
                    onValueChange = { updateFromRgb(red, it.roundToInt(), blue) },
                    valueRange = 0f..255f
                )

                Text("Blue: $blue", fontSize = 12.sp)
                Slider(
                    value = blue.toFloat(),
                    onValueChange = { updateFromRgb(red, green, it.roundToInt()) },
                    valueRange = 0f..255f
                )

                Spacer(modifier = Modifier.height(12.dp))

                // HSB Section
                Text("Hue - Saturation - Brightness (HSB)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Hue: ${hue.roundToInt()}°", fontSize = 12.sp)
                Slider(
                    value = hue,
                    onValueChange = { updateFromHsb(it, saturation, brightness) },
                    valueRange = 0f..360f
                )

                Text("Saturation: ${saturation.roundToInt()}%", fontSize = 12.sp)
                Slider(
                    value = saturation,
                    onValueChange = { updateFromHsb(hue, it, brightness) },
                    valueRange = 0f..100f
                )

                Text("Brightness: ${brightness.roundToInt()}%", fontSize = 12.sp)
                Slider(
                    value = brightness,
                    onValueChange = { updateFromHsb(hue, saturation, it) },
                    valueRange = 0f..100f
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onColorSelected(red.toFloat(), green.toFloat(), blue.toFloat())
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun rgbToHsv(r: Int, g: Int, b: Int): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(r, g, b, hsv)
    return hsv
}

fun hsvToRgb(h: Float, s: Float, v: Float): Triple<Int, Int, Int> {
    val hsv = floatArrayOf(h.coerceIn(0f, 360f), s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))
    val argb = android.graphics.Color.HSVToColor(hsv)
    val r = android.graphics.Color.red(argb)
    val g = android.graphics.Color.green(argb)
    val b = android.graphics.Color.blue(argb)
    return Triple(r, g, b)
}
