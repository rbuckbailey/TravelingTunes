package com.travelingtunes.app.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelingtunes.app.core.model.NormalizationSettings
import com.travelingtunes.app.core.model.NormalizationSummary
import com.travelingtunes.app.core.model.Song

@Composable
fun NormalizationReportDialog(
    songs: List<Song>,
    settings: NormalizationSettings,
    summary: NormalizationSummary,
    onDismiss: () -> Unit,
    onReanalyze: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "PEAK_LIMITED", "HIGH_BOOST", "UNANALYZED"
    var searchQuery by remember { mutableStateOf("") }

    val peakLimitedSongs = remember(songs, settings) {
        songs.filter { song ->
            if (song.avgVolume <= 0.0001f) false
            else {
                val rawGain = settings.targetRms / song.avgVolume
                val maxPeakGain = if (song.peakVolume > 0.0001f) settings.maxPeak / song.peakVolume else settings.maxGainBoost
                maxPeakGain < rawGain - 0.01f
            }
        }
    }

    val highBoostSongs = remember(songs) {
        songs.filter { it.trackGain >= 2.0f }
    }

    val unanalyzedSongs = remember(songs) {
        songs.filter { it.avgVolume <= 0.0001f }
    }

    val filteredSongs = remember(songs, selectedFilter, searchQuery, peakLimitedSongs, highBoostSongs, unanalyzedSongs) {
        val baseList = when (selectedFilter) {
            "PEAK_LIMITED" -> peakLimitedSongs
            "HIGH_BOOST" -> highBoostSongs
            "UNANALYZED" -> unanalyzedSongs
            else -> songs
        }

        if (searchQuery.isBlank()) {
            baseList
        } else {
            val q = searchQuery.trim().lowercase()
            baseList.filter {
                it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.album.lowercase().contains(q)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Normalization Report",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
            ) {
                // Summary Overview Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Library Overview",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Analyzed: ${summary.analyzedSongs} / ${summary.totalSongs}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Target RMS: ${"%.2f".format(settings.targetRms)}",
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Avg RMS: ${"%.3f".format(summary.avgRms)}",
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Peak Ceiling: ${"%.2f".format(settings.maxPeak)}",
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Peak-Limited: ${peakLimitedSongs.size} tracks",
                                fontSize = 12.sp,
                                color = if (peakLimitedSongs.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Gains: ${"%.2f".format(summary.minGain)}x – ${"%.2f".format(summary.maxGain)}x",
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search title, artist, album...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilter == "ALL",
                            onClick = { selectedFilter = "ALL" },
                            label = { Text("All (${songs.size})") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "PEAK_LIMITED",
                            onClick = { selectedFilter = "PEAK_LIMITED" },
                            label = { Text("Peak Limited (${peakLimitedSongs.size})") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "HIGH_BOOST",
                            onClick = { selectedFilter = "HIGH_BOOST" },
                            label = { Text("High Boost (${highBoostSongs.size})") }
                        )
                    }
                    if (unanalyzedSongs.isNotEmpty()) {
                        item {
                            FilterChip(
                                selected = selectedFilter == "UNANALYZED",
                                onClick = { selectedFilter = "UNANALYZED" },
                                label = { Text("Unanalyzed (${unanalyzedSongs.size})") }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Songs List
                if (filteredSongs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No songs match filter",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredSongs) { song ->
                            SongNormalizationRow(song = song, settings = settings)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onReanalyze()
                }
            ) {
                Text("Re-Analyze Library")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun SongNormalizationRow(
    song: Song,
    settings: NormalizationSettings
) {
    val isUnanalyzed = song.avgVolume <= 0.0001f
    val isPeakLimited = remember(song, settings) {
        if (isUnanalyzed) false
        else {
            val rawGain = settings.targetRms / song.avgVolume
            val maxPeakGain = if (song.peakVolume > 0.0001f) settings.maxPeak / song.peakVolume else settings.maxGainBoost
            maxPeakGain < rawGain - 0.01f
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = song.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                Column {
                    Text(
                        text = "${song.artist} • ${song.album}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    if (isUnanalyzed) {
                        Text(
                            text = "Not analyzed yet",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        Text(
                            text = "RMS: ${"%.3f".format(song.avgVolume)} | Peak: ${"%.3f".format(song.peakVolume)} | Track Gain: ${"%.2f".format(song.trackGain)}x | Album Gain: ${"%.2f".format(song.albumGain)}x",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            trailingContent = {
                if (isUnanalyzed) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Unanalyzed",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if (isPeakLimited) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Peak Limited",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if (song.trackGain >= 2.0f) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "High Boost",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        )
    }
}
