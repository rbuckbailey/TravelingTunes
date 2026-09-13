package com.travelingtunes.app.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelingtunes.app.core.database.MusicDatabase
import com.travelingtunes.app.core.media.MusicMetadataBackupHelper
import com.travelingtunes.app.core.media.RestoreFieldOptions
import kotlinx.coroutines.launch

@Composable
fun MetadataRestoreDialog(
    jsonString: String,
    musicDatabase: MusicDatabase,
    onDismiss: () -> Unit,
    onRestoreComplete: (restoredCount: Int, failedCount: Int) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var options by remember { mutableStateOf(RestoreFieldOptions()) }
    var isRestoring by remember { mutableStateOf(false) }
    var progressCurrent by remember { mutableIntStateOf(0) }
    var progressTotal by remember { mutableIntStateOf(0) }
    var progressStatus by remember { mutableStateOf("") }

    val toggleAll = {
        val target = !options.isAllSelected
        options = RestoreFieldOptions(
            genre = target,
            albumArt = target,
            title = target,
            artist = target,
            album = target,
            yearAndTrack = target,
            userRating = target
        )
    }

    AlertDialog(
        onDismissRequest = { if (!isRestoring) onDismiss() },
        title = {
            Text("Restore Music Metadata", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isRestoring) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 16.dp)
                    ) {
                        val progress = if (progressTotal > 0) progressCurrent.toFloat() / progressTotal.toFloat() else 0f
                        Text(
                            text = progressStatus.ifBlank { "Restoring metadata..." },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Updating database and embedding ID3 tags...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = "Choose which ID3 fields to restore into your library and embed into audio files:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Master "Restore All" Option
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { toggleAll() }
                            .padding(vertical = 6.dp)
                    ) {
                        Checkbox(
                            checked = options.isAllSelected,
                            onCheckedChange = { toggleAll() }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Restore All Fields",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Field Checkboxes
                    RestoreCheckboxRow(
                        label = "Genre (ID3 TCON)",
                        description = "Restore ID3 genre tags (e.g. Rock, Jazz, Pop)",
                        checked = options.genre,
                        onCheckedChange = { options = options.copy(genre = it) }
                    )

                    RestoreCheckboxRow(
                        label = "Album Artwork (ID3 APIC)",
                        description = "Restore & embed artwork images into audio files",
                        checked = options.albumArt,
                        onCheckedChange = { options = options.copy(albumArt = it) }
                    )

                    RestoreCheckboxRow(
                        label = "Song Title (ID3 TIT2)",
                        description = "Restore track title metadata",
                        checked = options.title,
                        onCheckedChange = { options = options.copy(title = it) }
                    )

                    RestoreCheckboxRow(
                        label = "Artist Name (ID3 TPE1)",
                        description = "Restore artist name metadata",
                        checked = options.artist,
                        onCheckedChange = { options = options.copy(artist = it) }
                    )

                    RestoreCheckboxRow(
                        label = "Album Name (ID3 TALB)",
                        description = "Restore album title metadata",
                        checked = options.album,
                        onCheckedChange = { options = options.copy(album = it) }
                    )

                    RestoreCheckboxRow(
                        label = "Year, Track & Disc Numbers",
                        description = "Restore track ordering and release year",
                        checked = options.yearAndTrack,
                        onCheckedChange = { options = options.copy(yearAndTrack = it) }
                    )

                    RestoreCheckboxRow(
                        label = "User Ratings",
                        description = "Restore 1–5 star track ratings",
                        checked = options.userRating,
                        onCheckedChange = { options = options.copy(userRating = it) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    coroutineScope.launch {
                        isRestoring = true
                        val (succ, fail) = MusicMetadataBackupHelper.restoreMetadataFromJson(
                            context = context,
                            musicDatabase = musicDatabase,
                            jsonString = jsonString,
                            options = options,
                            onProgress = { current, total, status ->
                                progressCurrent = current
                                progressTotal = total
                                progressStatus = status
                            }
                        )
                        isRestoring = false
                        onRestoreComplete(succ, fail)
                    }
                },
                enabled = !isRestoring && options.hasAnySelected
            ) {
                Text(if (isRestoring) "Restoring..." else "Restore Selected Metadata")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isRestoring
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RestoreCheckboxRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onCheckedChange(it) }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
