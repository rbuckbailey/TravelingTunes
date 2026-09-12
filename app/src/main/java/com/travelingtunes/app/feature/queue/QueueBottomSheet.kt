package com.travelingtunes.app.feature.queue

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.travelingtunes.app.core.theme.BalancedTitleText
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelingtunes.app.core.media.PlaybackManager
import com.travelingtunes.app.core.model.Song
import com.travelingtunes.app.core.model.GestureTrigger
import com.travelingtunes.app.feature.songpicker.AlbumArtImage
import com.travelingtunes.app.core.model.SlideDirection
import com.travelingtunes.app.core.ui.SlidingOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    visible: Boolean,
    slideDirection: SlideDirection = SlideDirection.BOTTOM,
    openingTrigger: GestureTrigger? = null,
    playbackManager: PlaybackManager,
    onOpenSongPicker: (SlideDirection) -> Unit = {},
    onDismiss: () -> Unit
) {
    val currentPlaylist by playbackManager.currentPlaylist.collectAsState()
    val currentSong by playbackManager.currentSong.collectAsState()
    val isPlaying by playbackManager.isPlaying.collectAsState()

    val currentIndex = remember(currentPlaylist, currentSong) {
        val idx = currentPlaylist.indexOfFirst { it.id == currentSong?.id }
        if (idx != -1) idx else 0
    }

    SlidingOverlay(
        visible = visible,
        slideDirection = slideDirection,
        openingTrigger = openingTrigger,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    BalancedTitleText(
                        text = "Current Queue",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${currentPlaylist.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onOpenSongPicker(SlideDirection.BOTTOM) }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Songs (Open Song Picker)",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (currentPlaylist.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Queue is empty",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { onOpenSongPicker(SlideDirection.BOTTOM) }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Songs")
                        }
                    }
                }
            } else {
                var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
                var accumulatedDragY by remember { mutableStateOf(0f) }

                val listState = rememberLazyListState()

                // Auto-scroll to current playing song when sheet opens
                LaunchedEffect(currentIndex) {
                    if (currentIndex in currentPlaylist.indices) {
                        listState.animateScrollToItem(currentIndex.coerceAtLeast(0))
                    }
                }

                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    itemsIndexed(
                        items = currentPlaylist,
                        key = { index, song -> "${song.id}_$index" }
                    ) { index, song ->
                        val isCurrent = index == currentIndex
                        val isPrior = index < currentIndex
                        val isDragged = draggedItemIndex == index

                        val elevation by animateDpAsState(if (isDragged) 8.dp else 0.dp, label = "elevation")

                        QueueItemRow(
                            song = song,
                            index = index,
                            totalCount = currentPlaylist.size,
                            isCurrent = isCurrent,
                            isPrior = isPrior,
                            isPlaying = isPlaying && isCurrent,
                            elevation = elevation,
                            onPlay = {
                                playbackManager.playSongAtIndex(index)
                            },
                            onRemove = {
                                playbackManager.removeQueueItem(index)
                            },
                            onMoveUp = if (index > 0) {
                                { playbackManager.moveQueueItem(index, index - 1) }
                            } else null,
                            onMoveDown = if (index < currentPlaylist.size - 1) {
                                { playbackManager.moveQueueItem(index, index + 1) }
                            } else null,
                            onDrag = { dragAmountY ->
                                accumulatedDragY += dragAmountY
                                val threshold = 120f
                                if (accumulatedDragY > threshold && index < currentPlaylist.size - 1) {
                                    playbackManager.moveQueueItem(index, index + 1)
                                    draggedItemIndex = index + 1
                                    accumulatedDragY = 0f
                                } else if (accumulatedDragY < -threshold && index > 0) {
                                    playbackManager.moveQueueItem(index, index - 1)
                                    draggedItemIndex = index - 1
                                    accumulatedDragY = 0f
                                }
                            },
                            onDragStart = {
                                draggedItemIndex = index
                                accumulatedDragY = 0f
                            },
                            onDragEnd = {
                                draggedItemIndex = null
                                accumulatedDragY = 0f
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueItemRow(
    song: Song,
    index: Int,
    totalCount: Int,
    isCurrent: Boolean,
    isPrior: Boolean,
    isPlaying: Boolean,
    elevation: androidx.compose.ui.unit.Dp,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onDrag: (Float) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit
) {
    val containerColor = when {
        isCurrent -> MaterialTheme.colorScheme.primaryContainer
        isPrior -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surface
    }

    val contentColor = when {
        isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer
        isPrior -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // Drag Handle / Touch Reorder Gesture
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { onDragStart() },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.y)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Reorder",
                    tint = contentColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Album Art
            AlbumArtImage(
                song = song,
                artworkUri = song.artworkUri,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(6.dp))
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Song Information & Queue Status
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCurrent) {
                        Icon(
                            imageVector = if (isPlaying) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.PlayArrow,
                            contentDescription = "Now Playing",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    BalancedTitleText(
                        text = song.title,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 15.sp,
                        maxLines = 2,
                        color = contentColor
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPrior) {
                        Text(
                            text = "Played • ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor.copy(alpha = 0.7f)
                        )
                    } else if (isCurrent) {
                        Text(
                            text = "Now Playing • ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "${song.artist} • ${song.album}",
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                }
            }

            // Quick Up / Down Reorder Buttons
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (onMoveUp != null) {
                    IconButton(
                        onClick = onMoveUp,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Move Up",
                            tint = contentColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (onMoveDown != null) {
                    IconButton(
                        onClick = onMoveDown,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Move Down",
                            tint = contentColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Delete / Remove Button
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove from Queue",
                    tint = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
