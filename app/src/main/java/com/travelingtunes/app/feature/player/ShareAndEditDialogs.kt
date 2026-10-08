package com.travelingtunes.app.feature.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.travelingtunes.app.core.model.Song

enum class ShareTunesScope {
    TRACK,
    ALBUM
}

enum class ShareTunesFormat {
    INFO,
    FILES
}

enum class EditTagsScope {
    TRACK,
    ALBUM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareTunesDialog(
    song: Song?,
    onDismissRequest: () -> Unit,
    onShare: (scope: ShareTunesScope, format: ShareTunesFormat) -> Unit
) {
    var selectedScope by remember { mutableStateOf(ShareTunesScope.TRACK) }
    var selectedFormat by remember { mutableStateOf(ShareTunesFormat.INFO) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Share Tunes") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(
                        text = "Selection",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = selectedScope == ShareTunesScope.TRACK,
                            onClick = { selectedScope = ShareTunesScope.TRACK },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("Track")
                        }
                        SegmentedButton(
                            selected = selectedScope == ShareTunesScope.ALBUM,
                            onClick = { selectedScope = ShareTunesScope.ALBUM },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("Album")
                        }
                    }
                    if (song != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (selectedScope == ShareTunesScope.TRACK) "Track: ${song.title}" else "Album: ${song.album}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column {
                    Text(
                        text = "Format",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = selectedFormat == ShareTunesFormat.INFO,
                            onClick = { selectedFormat = ShareTunesFormat.INFO },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("Info")
                        }
                        SegmentedButton(
                            selected = selectedFormat == ShareTunesFormat.FILES,
                            onClick = { selectedFormat = ShareTunesFormat.FILES },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("Files")
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (selectedFormat == ShareTunesFormat.INFO) "Share title, artist & details as text" else if (selectedScope == ShareTunesScope.TRACK) "Share audio file" else "Share album files as ZIP",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onShare(selectedScope, selectedFormat)
                    onDismissRequest()
                }
            ) {
                Text("Share")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTagsChoiceDialog(
    song: Song?,
    onDismissRequest: () -> Unit,
    onSelectScope: (scope: EditTagsScope) -> Unit
) {
    var selectedScope by remember { mutableStateOf(EditTagsScope.TRACK) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Edit Tags") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Choose whether to edit tags for the current track or the entire album:",
                    style = MaterialTheme.typography.bodyMedium
                )

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedScope == EditTagsScope.TRACK,
                        onClick = { selectedScope = EditTagsScope.TRACK },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Track")
                    }
                    SegmentedButton(
                        selected = selectedScope == EditTagsScope.ALBUM,
                        onClick = { selectedScope = EditTagsScope.ALBUM },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Album")
                    }
                }

                if (song != null) {
                    Text(
                        text = if (selectedScope == EditTagsScope.TRACK) "Target: ${song.title}" else "Target: ${song.album} (All Tracks)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSelectScope(selectedScope)
                    onDismissRequest()
                }
            ) {
                Text("Edit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}
