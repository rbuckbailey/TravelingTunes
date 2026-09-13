package com.travelingtunes.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelingtunes.app.core.database.MusicDatabase
import com.travelingtunes.app.core.media.DuplicateMatchPair
import com.travelingtunes.app.core.media.DuplicateTrackFinder
import com.travelingtunes.app.core.media.DuplicateTrackInfo
import com.travelingtunes.app.core.model.Song
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateTrackIdentifierScreen(
    musicDatabase: MusicDatabase,
    musicFolderName: String?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isLoading by remember { mutableStateOf(true) }
    var duplicatePairs by remember { mutableStateOf<List<DuplicateMatchPair>>(emptyList()) }
    var minLikelihoodThreshold by remember { mutableStateOf(50) }
    var trackToDelete by remember { mutableStateOf<Song?>(null) }
    var isDeleting by remember { mutableStateOf(false) }

    val scanDuplicates = {
        coroutineScope.launch {
            isLoading = true
            val songs = musicDatabase.getAllSongs()
            duplicatePairs = DuplicateTrackFinder.findDuplicates(context, songs, musicFolderName)
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        scanDuplicates()
    }

    val filteredPairs = remember(duplicatePairs, minLikelihoodThreshold) {
        duplicatePairs.filter { it.likelihoodPercentage >= minLikelihoodThreshold }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Duplicate Track Identifier") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { scanDuplicates() }, enabled = !isLoading) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh scan")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Compare tracks by file name, file size, and audio metadata to find potential duplicate files.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = "Filter:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                listOf(
                    50 to "All (50%+)",
                    75 to "75%+",
                    90 to "90%+ High"
                ).forEach { (threshold, label) ->
                    FilterChip(
                        selected = minLikelihoodThreshold == threshold,
                        onClick = { minLikelihoodThreshold = threshold },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }

            if (isLoading) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Analyzing library tracks for duplicates...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (filteredPairs.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (duplicatePairs.isEmpty()) "No duplicate tracks found!" else "No duplicates matching filter",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (duplicatePairs.isEmpty())
                                "Your music library has no matching duplicate audio files based on file name, size, and metadata."
                            else
                                "Try selecting a lower threshold to view lower confidence matches.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${filteredPairs.size} potential duplicate pair${if (filteredPairs.size > 1) "s" else ""} found",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Ordered by likelihood",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredPairs, key = { it.id }) { pair ->
                        DuplicatePairCard(
                            pair = pair,
                            onDeleteTrack = { song ->
                                trackToDelete = song
                            }
                        )
                    }
                }
            }
        }
    }

    trackToDelete?.let { song ->
        AlertDialog(
            onDismissRequest = { if (!isDeleting) trackToDelete = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Duplicate Track?") },
            text = {
                Column {
                    Text("Are you sure you want to delete this track from your device storage and library?")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Title: ${song.title}", fontWeight = FontWeight.Bold)
                    Text("File: ${song.fileName}")
                    val fullPath = DuplicateTrackFinder.getFullFolderPath(song, musicFolderName)
                    Text("Folder Path: $fullPath", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isDeleting = true
                            val deleted = DuplicateTrackFinder.deleteTrack(context, musicDatabase, song)
                            isDeleting = false
                            trackToDelete = null
                            if (deleted) {
                                snackbarHostState.showSnackbar("Deleted ${song.fileName}")
                            } else {
                                snackbarHostState.showSnackbar("Removed ${song.title} from library")
                            }
                            scanDuplicates()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = !isDeleting
                ) {
                    Text(if (isDeleting) "Deleting..." else "Delete Track")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { trackToDelete = null },
                    enabled = !isDeleting
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DuplicatePairCard(
    pair: DuplicateMatchPair,
    onDeleteTrack: (Song) -> Unit
) {
    val likelihoodColor = when {
        pair.likelihoodPercentage >= 90 -> Color(0xFF2E7D32) // Green
        pair.likelihoodPercentage >= 75 -> Color(0xFFE65100) // Orange
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = likelihoodColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "${pair.likelihoodPercentage}% Likelihood Match",
                        color = likelihoodColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                if (pair.matchReasons.isNotEmpty()) {
                    Text(
                        text = pair.matchReasons.joinToString(" • "),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false).padding(start = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Track A Card
            TrackItemCard(
                label = "Track A",
                info = pair.trackA,
                onDelete = { onDeleteTrack(pair.trackA.song) }
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            // Track B Card
            TrackItemCard(
                label = "Track B",
                info = pair.trackB,
                onDelete = { onDeleteTrack(pair.trackB.song) }
            )
        }
    }
}

@Composable
private fun TrackItemCard(
    label: String,
    info: DuplicateTrackInfo,
    onDelete: () -> Unit
) {
    val song = info.song
    val fileSizeStr = DuplicateTrackFinder.formatFileSize(info.fileSize)
    val durationStr = DuplicateTrackFinder.formatDuration(song.durationMs)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "$fileSizeStr • $durationStr",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = song.title.ifBlank { song.fileName },
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (song.artist.isNotBlank() || song.album.isNotBlank()) {
            Text(
                text = listOf(song.artist, song.album).filter { it.isNotBlank() }.joinToString(" — "),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.InsertDriveFile,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "File: ${song.fileName}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Folder Path: ${info.fullPath}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onDelete,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.align(Alignment.End)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Delete This Track", fontSize = 12.sp)
        }
    }
}
