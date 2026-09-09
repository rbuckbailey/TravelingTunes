package com.travelingtunes.app.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelingtunes.app.core.database.LibraryStats
import com.travelingtunes.app.core.datastore.SettingsDataStore
import com.travelingtunes.app.core.model.ArtLayoutOption
import com.travelingtunes.app.core.model.ColorTheme
import com.travelingtunes.app.core.model.DisplaySettings
import com.travelingtunes.app.core.model.HudTypeOption
import com.travelingtunes.app.core.model.ScrubHudTypeOption
import com.travelingtunes.app.core.model.ThemeSettings
import com.travelingtunes.app.core.theme.FontHelper
import com.travelingtunes.app.core.theme.FontOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var availableFonts by remember { mutableStateOf(FontHelper.getAvailableFonts(context)) }

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
            // Section: Music Library
            SectionHeader("Music Library")
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

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Section: Gestures
            SectionHeader("Gesture Configuration")
            ListItem(
                headlineContent = { Text("Reconfigure Gesture Assignments") },
                supportingContent = { Text("Customize 1/2/3 finger swipes, taps, long presses, and corners") },
                modifier = Modifier.clickable { onOpenGestureAssignments() }
            )
            ListItem(
                headlineContent = { Text("Reset All Gesture Assignments") },
                modifier = Modifier.clickable {
                    coroutineScope.launch {
                        settingsDataStore.resetAllGestureBindings()
                    }
                }
            )

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Section: Typography & Labels
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                SectionHeader("Typography & Labels")
                OutlinedButton(
                    onClick = { fontPickerLauncher.launch(arrayOf("*/*")) },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Font", fontSize = 13.sp)
                }
            }

            // Artist Label Font & Size
            FontSelectorRow(
                label = "Artist Label Font Family",
                currentFontKey = displaySettings.artistFontKey,
                availableFonts = availableFonts,
                onFontSelected = { key ->
                    coroutineScope.launch {
                        settingsDataStore.updateDisplaySettings(displaySettings.copy(artistFontKey = key))
                    }
                }
            )
            Text("Artist Font Size: ${displaySettings.artistFontSize.toInt()} pt", modifier = Modifier.padding(top = 4.dp))
            Slider(
                value = displaySettings.artistFontSize,
                onValueChange = { size ->
                    coroutineScope.launch {
                        settingsDataStore.updateDisplaySettings(displaySettings.copy(artistFontSize = size))
                    }
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
                    coroutineScope.launch {
                        settingsDataStore.updateDisplaySettings(displaySettings.copy(songFontKey = key))
                    }
                }
            )
            Text("Song Title Font Size: ${displaySettings.songFontSize.toInt()} pt", modifier = Modifier.padding(top = 4.dp))
            Slider(
                value = displaySettings.songFontSize,
                onValueChange = { size ->
                    coroutineScope.launch {
                        settingsDataStore.updateDisplaySettings(displaySettings.copy(songFontSize = size))
                    }
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
                    coroutineScope.launch {
                        settingsDataStore.updateDisplaySettings(displaySettings.copy(albumFontKey = key))
                    }
                }
            )
            Text("Album Font Size: ${displaySettings.albumFontSize.toInt()} pt", modifier = Modifier.padding(top = 4.dp))
            Slider(
                value = displaySettings.albumFontSize,
                onValueChange = { size ->
                    coroutineScope.launch {
                        settingsDataStore.updateDisplaySettings(displaySettings.copy(albumFontSize = size))
                    }
                },
                valueRange = 20f..100f
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Text("Scroll Long Titles (Marquee)", modifier = Modifier.weight(1f))
                Switch(
                    checked = displaySettings.titleScrollLong,
                    onCheckedChange = { checked ->
                        coroutineScope.launch {
                            settingsDataStore.updateDisplaySettings(displaySettings.copy(titleScrollLong = checked))
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Album Art Layout", fontWeight = FontWeight.Bold)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
            ) {
                ArtLayoutOption.entries.forEach { option ->
                    FilterChip(
                        selected = displaySettings.artDisplayLayout == option,
                        onClick = {
                            coroutineScope.launch {
                                settingsDataStore.updateDisplaySettings(displaySettings.copy(artDisplayLayout = option))
                            }
                        },
                        label = { Text(option.displayName) }
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Section: HUD & Progress Displays
            SectionHeader("HUD & Progress Displays")

            Text("Volume Bar Display Style", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                HudTypeOption.entries.forEach { option ->
                    FilterChip(
                        selected = displaySettings.hudType == option,
                        onClick = {
                            coroutineScope.launch {
                                settingsDataStore.updateDisplaySettings(displaySettings.copy(hudType = option))
                            }
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
                            coroutineScope.launch {
                                settingsDataStore.updateDisplaySettings(displaySettings.copy(scrubHudType = option))
                            }
                        },
                        label = { Text(option.displayName) }
                    )
                }
            }

            Text("HUD Line & Bar Thickness: ${displaySettings.hudLineThickness.toInt()} dp", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            Slider(
                value = displaySettings.hudLineThickness,
                onValueChange = { thickness ->
                    coroutineScope.launch {
                        settingsDataStore.updateDisplaySettings(displaySettings.copy(hudLineThickness = thickness))
                    }
                },
                valueRange = 6f..40f
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text("Always Show Volume Overlay", modifier = Modifier.weight(1f))
                Switch(
                    checked = displaySettings.volumeAlwaysOn,
                    onCheckedChange = { checked ->
                        coroutineScope.launch {
                            settingsDataStore.updateDisplaySettings(displaySettings.copy(volumeAlwaysOn = checked))
                        }
                    }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text("Show Album Art", modifier = Modifier.weight(1f))
                Switch(
                    checked = displaySettings.showAlbumArt,
                    onCheckedChange = { checked ->
                        coroutineScope.launch {
                            settingsDataStore.updateDisplaySettings(displaySettings.copy(showAlbumArt = checked))
                        }
                    }
                )
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Section: Color Themes & Options
            SectionHeader("Color Themes & Styles")

            Text("Theme Appearance Options", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text("Rounded Corners", modifier = Modifier.weight(1f))
                Switch(
                    checked = themeSettings.isRounded,
                    onCheckedChange = { checked ->
                        coroutineScope.launch {
                            settingsDataStore.updateThemeSettings(themeSettings.copy(isRounded = checked))
                        }
                    }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text("Glass Effect (Translucent & Blur)", modifier = Modifier.weight(1f))
                Switch(
                    checked = themeSettings.isGlass,
                    onCheckedChange = { checked ->
                        coroutineScope.launch {
                            settingsDataStore.updateThemeSettings(themeSettings.copy(isGlass = checked))
                        }
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
                            coroutineScope.launch {
                                settingsDataStore.updateThemeSettings(themeSettings.copy(currentThemeName = theme.name))
                            }
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
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Section: Navigation & Addresses
            SectionHeader("Navigation Addresses")
            OutlinedTextField(
                value = homeAddress,
                onValueChange = { newHome ->
                    coroutineScope.launch {
                        settingsDataStore.setAddresses(home = newHome)
                    }
                },
                label = { Text("Home Address") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = workAddress,
                onValueChange = { newWork ->
                    coroutineScope.launch {
                        settingsDataStore.setAddresses(work = newWork)
                    }
                },
                label = { Text("Work Address") },
                modifier = Modifier.fillMaxWidth()
            )

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Section: Quick Start
            SectionHeader("Help & Tutorials")
            ListItem(
                headlineContent = { Text("Show Quick Start Tutorial") },
                modifier = Modifier.clickable { onOpenQuickStart() }
            )
            ListItem(
                headlineContent = { Text("Pick Contact Address") },
                modifier = Modifier.clickable { onOpenContacts() }
            )
        }
    }
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

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}
