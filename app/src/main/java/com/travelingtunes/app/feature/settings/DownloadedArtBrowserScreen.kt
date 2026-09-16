package com.travelingtunes.app.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelingtunes.app.core.database.AlbumArtBrowserInfo
import com.travelingtunes.app.core.database.ArtworkType
import com.travelingtunes.app.core.database.MusicDatabase
import com.travelingtunes.app.core.media.AlbumArtDownloader
import com.travelingtunes.app.core.media.ArtworkCandidate
import com.travelingtunes.app.core.media.MusicScanner
import com.travelingtunes.app.core.media.PlaybackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadedArtBrowserScreen(
    musicDatabase: MusicDatabase,
    albumArtDownloader: AlbumArtDownloader,
    playbackManager: PlaybackManager,
    musicScanner: MusicScanner? = null,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var allAlbums by remember { mutableStateOf<List<AlbumArtBrowserInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "DOWNLOADED", "EMBEDDED", "MISSING"
    var albumSearchQuery by remember { mutableStateOf("") }

    val selectedAlbums = remember { mutableStateListOf<AlbumArtBrowserInfo>() }

    // Dialog states
    var replaceTargetAlbum by remember { mutableStateOf<AlbumArtBrowserInfo?>(null) }
    var showReplaceDialog by remember { mutableStateOf(false) }

    var copySourceAlbum by remember { mutableStateOf<AlbumArtBrowserInfo?>(null) }
    var showCopyDialog by remember { mutableStateOf(false) }

    // Zoom Preview state
    var zoomPreviewCandidate by remember { mutableStateOf<ArtworkCandidate?>(null) }
    var zoomPreviewUri by remember { mutableStateOf<Uri?>(null) }
    var zoomPreviewTitle by remember { mutableStateOf("") }

    // Background Embedding States
    val isEmbeddingArt by (musicScanner?.isEmbeddingArt?.collectAsState() ?: remember { mutableStateOf(false) })
    val embeddingStatusMessage by (musicScanner?.embeddingStatusMessage?.collectAsState() ?: remember { mutableStateOf(null) })
    val embeddingProgressCurrent by (musicScanner?.embeddingProgressCurrent?.collectAsState() ?: remember { mutableStateOf(0) })
    val embeddingProgressTotal by (musicScanner?.embeddingProgressTotal?.collectAsState() ?: remember { mutableStateOf(0) })
    val embeddingResultSummary by (musicScanner?.embeddingResultSummary?.collectAsState() ?: remember { mutableStateOf(null) })

    fun refreshList() {
        coroutineScope.launch {
            isLoading = true
            allAlbums = musicDatabase.getAllAlbumsWithArtInfo(context)
            selectedAlbums.removeAll { selected -> allAlbums.none { it.album == selected.album && it.artist == selected.artist } }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshList()
    }

    LaunchedEffect(isEmbeddingArt) {
        if (!isEmbeddingArt) {
            refreshList()
        }
    }

    val filteredAlbums = remember(allAlbums, selectedFilter, albumSearchQuery) {
        allAlbums.filter { albumInfo ->
            val matchesType = when (selectedFilter) {
                "DOWNLOADED" -> albumInfo.artType == ArtworkType.DOWNLOADED
                "EMBEDDED" -> albumInfo.artType == ArtworkType.EMBEDDED
                "MISSING" -> albumInfo.artType == ArtworkType.MISSING
                else -> true
            }
            val matchesQuery = albumSearchQuery.isBlank() ||
                    albumInfo.album.contains(albumSearchQuery, ignoreCase = true) ||
                    albumInfo.artist.contains(albumSearchQuery, ignoreCase = true)
            matchesType && matchesQuery
        }
    }

    // Photo & File Pickers for Replace Art
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val target = replaceTargetAlbum
        if (uri != null && target != null) {
            coroutineScope.launch {
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null && bytes.isNotEmpty()) {
                        val songs = musicDatabase.getSongsByAlbumAndArtist(target.album, target.artist)
                        albumArtDownloader.saveCustomArtworkForAlbum(target.album, target.artist, bytes, songs)
                        playbackManager.refreshCurrentSongArtwork()
                        refreshList()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        replaceTargetAlbum = null
        showReplaceDialog = false
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val target = replaceTargetAlbum
        if (uri != null && target != null) {
            coroutineScope.launch {
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null && bytes.isNotEmpty()) {
                        val songs = musicDatabase.getSongsByAlbumAndArtist(target.album, target.artist)
                        albumArtDownloader.saveCustomArtworkForAlbum(target.album, target.artist, bytes, songs)
                        playbackManager.refreshCurrentSongArtwork()
                        refreshList()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        replaceTargetAlbum = null
        showReplaceDialog = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Album Art Editor", fontWeight = FontWeight.Bold)
                        Text(
                            text = "${filteredAlbums.size} of ${allAlbums.size} albums",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (filteredAlbums.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                if (selectedAlbums.size == filteredAlbums.size) {
                                    selectedAlbums.clear()
                                } else {
                                    selectedAlbums.clear()
                                    selectedAlbums.addAll(filteredAlbums)
                                }
                            }
                        ) {
                            Text(
                                if (selectedAlbums.size == filteredAlbums.size) "Deselect All" else "Select All",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Exit to Play Screen")
                    }
                }
            )
        },
        bottomBar = {
            if (filteredAlbums.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val targets = if (selectedAlbums.isNotEmpty()) selectedAlbums.toList() else filteredAlbums
                        val labelSuffix = if (selectedAlbums.isNotEmpty()) "(${selectedAlbums.size})" else "(All ${filteredAlbums.size})"

                        OutlinedButton(
                            onClick = {
                                // Bulk Delete / Clear Artwork
                                coroutineScope.launch {
                                    for (item in targets) {
                                        val songs = musicDatabase.getSongsByAlbumAndArtist(item.album, item.artist)
                                        albumArtDownloader.deleteDownloadedArtworkForAlbum(item.album, item.artist, songs)
                                    }
                                    playbackManager.refreshCurrentSongArtwork()
                                    refreshList()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear $labelSuffix", fontSize = 13.sp)
                        }

                        Button(
                            enabled = !isEmbeddingArt,
                            onClick = {
                                // Bulk Embed ID3 as background process!
                                coroutineScope.launch {
                                    val pairs = targets.map { Pair(it.album, it.artist) }
                                    val uriMap = targets.associate { Pair(it.album, it.artist) to it.artworkUri }
                                    musicScanner?.embedArtworkInBackground(pairs, uriMap, playbackManager)
                                    refreshList()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Embed ID3 $labelSuffix", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Background Embedding Progress Bar Banner
            if (isEmbeddingArt) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Embedding ID3 Tags in Background...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            TextButton(onClick = { musicScanner?.cancelEmbedding() }) {
                                Text("Stop", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        if (embeddingProgressTotal > 0) {
                            LinearProgressIndicator(
                                progress = { (embeddingProgressCurrent.toFloat() / embeddingProgressTotal.toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        if (!embeddingStatusMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = embeddingStatusMessage ?: "",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            } else if (embeddingResultSummary != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = embeddingResultSummary ?: "",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { musicScanner?.clearEmbeddingResultSummary() }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss")
                        }
                    }
                }
            }

            // Album Text Search Filter Bar
            OutlinedTextField(
                value = albumSearchQuery,
                onValueChange = { albumSearchQuery = it },
                placeholder = { Text("Search albums or artists...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (albumSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { albumSearchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )

            // Filter Chips Bar
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("All (${allAlbums.size})") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "DOWNLOADED",
                        onClick = { selectedFilter = "DOWNLOADED" },
                        label = { Text("Downloaded (${allAlbums.count { it.artType == ArtworkType.DOWNLOADED }})") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "EMBEDDED",
                        onClick = { selectedFilter = "EMBEDDED" },
                        label = { Text("Embedded (${allAlbums.count { it.artType == ArtworkType.EMBEDDED }})") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "MISSING",
                        onClick = { selectedFilter = "MISSING" },
                        label = { Text("Missing (${allAlbums.count { it.artType == ArtworkType.MISSING }})") }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (filteredAlbums.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Albums Found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (albumSearchQuery.isNotBlank()) "No albums match search \"$albumSearchQuery\"." else "No albums match the selected filter.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(filteredAlbums) { albumInfo ->
                            val isSelected = selectedAlbums.contains(albumInfo)

                            AlbumArtBrowserItemRow(
                                albumInfo = albumInfo,
                                isSelected = isSelected,
                                onToggleSelect = {
                                    if (isSelected) selectedAlbums.remove(albumInfo) else selectedAlbums.add(albumInfo)
                                },
                                onZoomArt = {
                                    zoomPreviewUri = albumInfo.artworkUri
                                    zoomPreviewTitle = "${albumInfo.artist} - ${albumInfo.album}"
                                    zoomPreviewCandidate = null
                                },
                                onDelete = {
                                    coroutineScope.launch {
                                        val songs = musicDatabase.getSongsByAlbumAndArtist(albumInfo.album, albumInfo.artist)
                                        albumArtDownloader.deleteDownloadedArtworkForAlbum(albumInfo.album, albumInfo.artist, songs)
                                        playbackManager.refreshCurrentSongArtwork()
                                        refreshList()
                                    }
                                },
                                onReplace = {
                                    replaceTargetAlbum = albumInfo
                                    showReplaceDialog = true
                                },
                                onCopy = {
                                    copySourceAlbum = albumInfo
                                    showCopyDialog = true
                                },
                                onEmbedId3 = {
                                    coroutineScope.launch {
                                        musicScanner?.embedArtworkInBackground(
                                            targets = listOf(Pair(albumInfo.album, albumInfo.artist)),
                                            artworkUris = mapOf(Pair(albumInfo.album, albumInfo.artist) to albumInfo.artworkUri),
                                            playbackManager = playbackManager
                                        )
                                        refreshList()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Replace Artwork Options Dialog
    if (showReplaceDialog && replaceTargetAlbum != null) {
        val target = replaceTargetAlbum!!
        ReplaceArtworkDialog(
            targetAlbum = target,
            allAlbums = allAlbums,
            albumArtDownloader = albumArtDownloader,
            onDismiss = { showReplaceDialog = false },
            onSelectFromPhotos = {
                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onSelectFromFiles = {
                filePickerLauncher.launch(arrayOf("image/*"))
            },
            onCopyFromOtherAlbum = { sourceAlbum ->
                coroutineScope.launch {
                    val sourceUri = sourceAlbum.artworkUri
                    if (sourceUri != null) {
                        albumArtDownloader.copyArtworkToAlbums(sourceUri, listOf(Pair(target.album, target.artist)))
                        playbackManager.refreshCurrentSongArtwork()
                        showReplaceDialog = false
                        refreshList()
                    }
                }
            },
            onZoomCandidate = { candidate ->
                zoomPreviewCandidate = candidate
                zoomPreviewUri = null
                zoomPreviewTitle = candidate.source
            },
            onCustomSearchResult = { candidate ->
                coroutineScope.launch {
                    val songs = musicDatabase.getSongsByAlbumAndArtist(target.album, target.artist)
                    albumArtDownloader.applyCandidateToAlbum(candidate, target.artist, target.album, songs)
                    playbackManager.refreshCurrentSongArtwork()
                    showReplaceDialog = false
                    refreshList()
                }
            }
        )
    }

    // Copy Artwork Dialog
    if (showCopyDialog && copySourceAlbum != null) {
        val source = copySourceAlbum!!
        CopyArtworkDialog(
            sourceAlbum = source,
            allAlbums = allAlbums,
            onDismiss = { showCopyDialog = false },
            onConfirmCopy = { targets ->
                coroutineScope.launch {
                    val sourceUri = source.artworkUri
                    if (sourceUri != null) {
                        val pairs = targets.map { Pair(it.album, it.artist) }
                        albumArtDownloader.copyArtworkToAlbums(sourceUri, pairs)
                        playbackManager.refreshCurrentSongArtwork()
                        showCopyDialog = false
                        refreshList()
                    }
                }
            }
        )
    }

    // Zoomable Artwork Preview Dialog
    if (zoomPreviewCandidate != null || zoomPreviewUri != null) {
        ZoomableArtPreviewDialog(
            candidate = zoomPreviewCandidate,
            artworkUri = zoomPreviewUri,
            title = zoomPreviewTitle,
            onDismiss = {
                zoomPreviewCandidate = null
                zoomPreviewUri = null
            }
        )
    }
}

@Composable
private fun AlbumArtBrowserItemRow(
    albumInfo: AlbumArtBrowserInfo,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onZoomArt: () -> Unit,
    onDelete: () -> Unit,
    onReplace: () -> Unit,
    onCopy: () -> Unit,
    onEmbedId3: () -> Unit
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }

    val bitmap = remember(albumInfo.artworkUri) {
        val uri = albumInfo.artworkUri ?: return@remember null
        try {
            if (uri.scheme == "file") {
                android.graphics.BitmapFactory.decodeFile(uri.path)
            } else {
                context.contentResolver.openInputStream(uri)?.use {
                    android.graphics.BitmapFactory.decodeStream(it)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleSelect() }
            .padding(horizontal = 6.dp, vertical = 1.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(enabled = albumInfo.artworkUri != null) { onZoomArt() },
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(1.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "Zoom",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Single line fitting Album, Artist, Track Count, and Artwork Badge!
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = albumInfo.album,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "• ${albumInfo.artist}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "(${albumInfo.songCount})",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Artwork Type Badge
                val (badgeText, badgeBg, badgeTextColor) = when (albumInfo.artType) {
                    ArtworkType.DOWNLOADED -> Triple("Downloaded", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                    ArtworkType.EMBEDDED -> Triple("Embedded", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                    ArtworkType.MISSING -> Triple("Missing", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
                }

                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeTextColor,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Replace Art")
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onReplace()
                        }
                    )

                    if (albumInfo.artworkUri != null) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Copy Art to Other Albums")
                                }
                            },
                            onClick = {
                                menuExpanded = false
                                onCopy()
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Embed Art into ID3 Tags")
                                }
                            },
                            onClick = {
                                menuExpanded = false
                                onEmbedId3()
                            }
                        )
                    }

                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Clear / Remove Art", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReplaceArtworkDialog(
    targetAlbum: AlbumArtBrowserInfo,
    allAlbums: List<AlbumArtBrowserInfo>,
    albumArtDownloader: AlbumArtDownloader,
    onDismiss: () -> Unit,
    onSelectFromPhotos: () -> Unit,
    onSelectFromFiles: () -> Unit,
    onCopyFromOtherAlbum: (AlbumArtBrowserInfo) -> Unit,
    onZoomCandidate: (ArtworkCandidate) -> Unit,
    onCustomSearchResult: (ArtworkCandidate) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("${targetAlbum.artist} ${targetAlbum.album}") }
    var isSearching by remember { mutableStateOf(false) }
    var searchCandidates by remember { mutableStateOf<List<ArtworkCandidate>>(emptyList()) }
    var hasSearched by remember { mutableStateOf(false) }

    // Search Engine Selection
    val enabledEngines = remember {
        mutableStateListOf(
            AlbumArtDownloader.SearchEngine.DEEZER,
            AlbumArtDownloader.SearchEngine.ITUNES,
            AlbumArtDownloader.SearchEngine.COVER_ART_ARCHIVE,
            AlbumArtDownloader.SearchEngine.WEB_SEARCH
        )
    }

    var showAlbumCopyPicker by remember { mutableStateOf(false) }

    fun performSearch() {
        if (searchQuery.isBlank()) return
        coroutineScope.launch {
            isSearching = true
            hasSearched = true
            searchCandidates = albumArtDownloader.searchCandidatesWithQuery(searchQuery, enabledEngines.toSet())
            isSearching = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Replace Artwork for \"${targetAlbum.album}\"", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Select replacement artwork source:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))

                // Photos & Files & Copy Buttons
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSelectFromPhotos,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Photos", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onSelectFromFiles,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Files", fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { showAlbumCopyPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy from another album", fontSize = 12.sp)
                    }
                }

                if (showAlbumCopyPicker) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Select source album to copy artwork from:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))

                    val albumsWithArt = remember(allAlbums) { allAlbums.filter { it.artworkUri != null && it.album != targetAlbum.album } }
                    if (albumsWithArt.isEmpty()) {
                        Text("No other albums with artwork available.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp)
                        ) {
                            items(albumsWithArt) { source ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onCopyFromOtherAlbum(source) }
                                        .padding(vertical = 6.dp, horizontal = 4.dp)
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("${source.album} (${source.artist})", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Search online with custom query:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))

                // Search Engine Selection FilterChips
                Text("Search methods / providers:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    AlbumArtDownloader.SearchEngine.entries.forEach { engine ->
                        item {
                            val isSelected = enabledEngines.contains(engine)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) {
                                        if (enabledEngines.size > 1) enabledEngines.remove(engine)
                                    } else {
                                        enabledEngines.add(engine)
                                    }
                                },
                                label = { Text(engine.displayName, fontSize = 10.sp) }
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search terms") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { performSearch() }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (hasSearched && searchCandidates.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No artwork results found for query.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else if (searchCandidates.isNotEmpty()) {
                    Text("Tap an image to select, or tap zoom icon to inspect:", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                    ) {
                        items(searchCandidates) { candidate ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .clickable { onCustomSearchResult(candidate) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                CandidateImageThumbnail(
                                    candidate = candidate,
                                    onZoom = { onZoomCandidate(candidate) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun CopyArtworkDialog(
    sourceAlbum: AlbumArtBrowserInfo,
    allAlbums: List<AlbumArtBrowserInfo>,
    onDismiss: () -> Unit,
    onConfirmCopy: (List<AlbumArtBrowserInfo>) -> Unit
) {
    val targetCandidates = remember(allAlbums, sourceAlbum) {
        allAlbums.filter { it.album != sourceAlbum.album || it.artist != sourceAlbum.artist }
    }
    val selectedTargets = remember { mutableStateListOf<AlbumArtBrowserInfo>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Copy Art From \"${sourceAlbum.album}\"", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Select target albums to receive this artwork:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))

                if (targetCandidates.isEmpty()) {
                    Text("No target albums found in library.", fontSize = 12.sp)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                if (selectedTargets.size == targetCandidates.size) selectedTargets.clear()
                                else {
                                    selectedTargets.clear()
                                    selectedTargets.addAll(targetCandidates)
                                }
                            }
                        ) {
                            Text(if (selectedTargets.size == targetCandidates.size) "Deselect All" else "Select All", fontSize = 12.sp)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                    ) {
                        items(targetCandidates) { target ->
                            val isSelected = selectedTargets.contains(target)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isSelected) selectedTargets.remove(target) else selectedTargets.add(target)
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        if (isSelected) selectedTargets.remove(target) else selectedTargets.add(target)
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(target.album, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${target.artist} • ${target.songCount} tracks", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedTargets.isNotEmpty(),
                onClick = { onConfirmCopy(selectedTargets.toList()) }
            ) {
                Text("Copy Artwork (${selectedTargets.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ZoomableArtPreviewDialog(
    candidate: ArtworkCandidate? = null,
    artworkUri: Uri? = null,
    title: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(candidate, artworkUri) {
        withContext(Dispatchers.IO) {
            isLoading = true
            try {
                if (candidate != null) {
                    val conn = java.net.URL(candidate.url).openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 6000
                    conn.readTimeout = 6000
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                    if (conn.responseCode == 200) {
                        bitmap = conn.inputStream.use { android.graphics.BitmapFactory.decodeStream(it) }
                    }
                    conn.disconnect()
                } else if (artworkUri != null) {
                    bitmap = if (artworkUri.scheme == "file") {
                        android.graphics.BitmapFactory.decodeFile(artworkUri.path)
                    } else {
                        context.contentResolver.openInputStream(artworkUri)?.use {
                            android.graphics.BitmapFactory.decodeStream(it)
                        }
                    }
                }
            } catch (_: Exception) {}
            isLoading = false
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
                Text(
                    text = title.ifBlank { "Artwork Preview" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White)
                    } else if (bitmap != null) {
                        Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = "Zoomed Artwork Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text("Unable to load high-res image preview", color = Color.White, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val dimText = if (bitmap != null) {
                    "${bitmap!!.width} x ${bitmap!!.height} px"
                } else if (candidate != null) {
                    "${candidate.width} x ${candidate.height} px"
                } else ""

                val sourceText = candidate?.source ?: "Local Library"

                Text(
                    text = "$sourceText • $dimText",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun CandidateImageThumbnail(
    candidate: ArtworkCandidate,
    onZoom: () -> Unit
) {
    var bitmap by remember(candidate.url) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(candidate.url) {
        withContext(Dispatchers.IO) {
            try {
                val conn = java.net.URL(candidate.url).openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                if (conn.responseCode == 200) {
                    val bmp = conn.inputStream.use { android.graphics.BitmapFactory.decodeStream(it) }
                    bitmap = bmp
                }
                conn.disconnect()
            } catch (_: Exception) {}
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            CircularProgressIndicator(modifier = Modifier.size(20.dp))
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(candidate.source, fontSize = 9.sp, color = Color.White)
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { onZoom() }
                .padding(2.dp)
        ) {
            Icon(Icons.Default.ZoomIn, contentDescription = "Zoom", tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}
