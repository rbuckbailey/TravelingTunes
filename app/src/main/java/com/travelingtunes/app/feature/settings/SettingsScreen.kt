package com.travelingtunes.app.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelingtunes.app.core.database.LibraryStats
import com.travelingtunes.app.core.datastore.SettingsDataStore
import com.travelingtunes.app.core.model.ColorTheme
import com.travelingtunes.app.core.model.DisplaySettings
import com.travelingtunes.app.core.model.HudTypeOption
import com.travelingtunes.app.core.model.ScrubHudTypeOption
import com.travelingtunes.app.core.model.ThemeSettings
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
    val coroutineScope = rememberCoroutineScope()

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
            SectionHeader("Typography & Labels")
            Text("Artist Font Size: ${displaySettings.artistFontSize.toInt()} pt")
            Slider(
                value = displaySettings.artistFontSize,
                onValueChange = { size ->
                    coroutineScope.launch {
                        settingsDataStore.updateDisplaySettings(displaySettings.copy(artistFontSize = size))
                    }
                },
                valueRange = 20f..100f
            )

            Text("Song Title Font Size: ${displaySettings.songFontSize.toInt()} pt")
            Slider(
                value = displaySettings.songFontSize,
                onValueChange = { size ->
                    coroutineScope.launch {
                        settingsDataStore.updateDisplaySettings(displaySettings.copy(songFontSize = size))
                    }
                },
                valueRange = 30f..120f
            )

            Text("Album Font Size: ${displaySettings.albumFontSize.toInt()} pt")
            Slider(
                value = displaySettings.albumFontSize,
                onValueChange = { size ->
                    coroutineScope.launch {
                        settingsDataStore.updateDisplaySettings(displaySettings.copy(albumFontSize = size))
                    }
                },
                valueRange = 20f..100f
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
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

            // Section: Color Themes
            SectionHeader("Color Themes")
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
