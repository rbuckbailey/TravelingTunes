package com.travelingtunes.app.feature.settings

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelingtunes.app.core.database.DownloadedAlbumArtInfo
import com.travelingtunes.app.core.database.MusicDatabase
import com.travelingtunes.app.core.media.AlbumArtDownloader
import com.travelingtunes.app.core.media.ArtworkCandidate
import com.travelingtunes.app.core.media.Id3ArtworkEmbedder
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
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var downloadedAlbums by remember { mutableStateOf<List<DownloadedAlbumArtInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val selectedAlbums = remember { mutableStateListOf<DownloadedAlbumArtInfo>() }

    // Dialog states
    var replaceTargetAlbum by remember { mutableStateOf<DownloadedAlbumArtInfo?>(null) }
    var showReplaceDialog by remember { mutableStateOf(false) }

    // Operation Progress Dialog
    var showProgressDialog by remember { mutableStateOf(false) }
    var progressTitle by remember { mutableStateOf("") }
    var progressMessage by remember { mutableStateOf("") }
    var progressCurrent by remember { mutableStateOf(0) }
    var progressTotal by remember { mutableStateOf(0) }
    var operationResultSummary by remember { mutableStateOf<String?>(null) }

    fun refreshList() {
        coroutineScope.launch {
            isLoading = true
            downloadedAlbums = musicDatabase.getAlbumsWithDownloadedArt(context)
            selectedAlbums.removeAll { selected -> downloadedAlbums.none { it.album == selected.album && it.artist == selected.artist } }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshList()
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
                        Text("Downloaded Art Browser", fontWeight = FontWeight.Bold)
                        Text(
                            text = "${downloadedAlbums.size} downloaded albums",
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
                    if (downloadedAlbums.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                if (selectedAlbums.size == downloadedAlbums.size) {
                                    selectedAlbums.clear()
                                } else {
                                    selectedAlbums.clear()
                                    selectedAlbums.addAll(downloadedAlbums)
                                }
                            }
                        ) {
                            Text(
                                if (selectedAlbums.size == downloadedAlbums.size) "Deselect All" else "Select All",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (downloadedAlbums.isNotEmpty()) {
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
                        val targets = if (selectedAlbums.isNotEmpty()) selectedAlbums.toList() else downloadedAlbums
                        val labelSuffix = if (selectedAlbums.isNotEmpty()) "(${selectedAlbums.size})" else "(All ${downloadedAlbums.size})"

                        OutlinedButton(
                            onClick = {
                                // Bulk Delete
                                coroutineScope.launch {
                                    showProgressDialog = true
                                    progressTitle = "Deleting Downloaded Art"
                                    progressCurrent = 0
                                    progressTotal = targets.size
                                    operationResultSummary = null

                                    var deletedCount = 0
                                    for ((index, item) in targets.withIndex()) {
                                        progressCurrent = index + 1
                                        progressMessage = "Deleting art for: ${item.album}"
                                        val songs = musicDatabase.getSongsByAlbumAndArtist(item.album, item.artist)
                                        albumArtDownloader.deleteDownloadedArtworkForAlbum(item.album, item.artist, songs)
                                        deletedCount++
                                    }
                                    playbackManager.refreshCurrentSongArtwork()
                                    refreshList()
                                    operationResultSummary = "Successfully deleted downloaded art for $deletedCount albums."
                                }
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete $labelSuffix", fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                // Bulk Embed ID3
                                coroutineScope.launch {
                                    showProgressDialog = true
                                    progressTitle = "Embedding Art as ID3 Metadata"
                                    progressCurrent = 0
                                    progressTotal = targets.size
                                    operationResultSummary = null

                                    var totalSongsEmbedded = 0
                                    var totalFailedSongs = 0

                                    for ((index, item) in targets.withIndex()) {
                                        progressCurrent = index + 1
                                        progressMessage = "Embedding artwork for: ${item.album}"
                                        val songs = musicDatabase.getSongsByAlbumAndArtist(item.album, item.artist)
                                        val (succ, fail) = Id3ArtworkEmbedder.embedArtworkIntoAlbum(context, songs, item.artworkUri)
                                        totalSongsEmbedded += succ
                                        totalFailedSongs += fail
                                    }
                                    playbackManager.refreshCurrentSongArtwork()
                                    refreshList()
                                    operationResultSummary = "ID3 Embedding Complete!\nSuccessfully embedded artwork into $totalSongsEmbedded tracks across ${targets.size} albums ($totalFailedSongs failed)."
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (downloadedAlbums.isEmpty()) {
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
                        text = "No Downloaded Artwork",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Albums with downloaded or custom replaced artwork will appear here for management and ID3 tag embedding.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(downloadedAlbums) { albumInfo ->
                        val isSelected = selectedAlbums.contains(albumInfo)

                        DownloadedAlbumItemRow(
                            albumInfo = albumInfo,
                            isSelected = isSelected,
                            onToggleSelect = {
                                if (isSelected) selectedAlbums.remove(albumInfo) else selectedAlbums.add(albumInfo)
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
                            onEmbedId3 = {
                                coroutineScope.launch {
                                    showProgressDialog = true
                                    progressTitle = "Embedding ID3 Artwork"
                                    progressCurrent = 1
                                    progressTotal = 1
                                    progressMessage = "Embedding artwork into \"${albumInfo.album}\"..."
                                    operationResultSummary = null

                                    val songs = musicDatabase.getSongsByAlbumAndArtist(albumInfo.album, albumInfo.artist)
                                    val (succ, fail) = Id3ArtworkEmbedder.embedArtworkIntoAlbum(context, songs, albumInfo.artworkUri)
                                    playbackManager.refreshCurrentSongArtwork()
                                    refreshList()
                                    operationResultSummary = "ID3 Embedding Complete!\nEmbedded artwork into $succ tracks of \"${albumInfo.album}\" ($fail failed)."
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Replace Artwork Options Dialog
    if (showReplaceDialog && replaceTargetAlbum != null) {
        val target = replaceTargetAlbum!!
        ReplaceArtworkDialog(
            albumInfo = target,
            albumArtDownloader = albumArtDownloader,
            onDismiss = { showReplaceDialog = false },
            onSelectFromPhotos = {
                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onSelectFromFiles = {
                filePickerLauncher.launch(arrayOf("image/*"))
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

    // Progress / Result Dialog
    if (showProgressDialog) {
        AlertDialog(
            onDismissRequest = {
                if (operationResultSummary != null) showProgressDialog = false
            },
            title = { Text(progressTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (operationResultSummary == null) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Processing $progressCurrent of $progressTotal...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = progressMessage, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text(text = operationResultSummary ?: "", fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                if (operationResultSummary != null) {
                    Button(onClick = { showProgressDialog = false }) {
                        Text("Done")
                    }
                }
            }
        )
    }
}

@Composable
private fun DownloadedAlbumItemRow(
    albumInfo: DownloadedAlbumArtInfo,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onDelete: () -> Unit,
    onReplace: () -> Unit,
    onEmbedId3: () -> Unit
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }

    val bitmap = remember(albumInfo.artworkUri) {
        try {
            if (albumInfo.artworkUri.scheme == "file") {
                android.graphics.BitmapFactory.decodeFile(albumInfo.artworkUri.path)
            } else {
                context.contentResolver.openInputStream(albumInfo.artworkUri)?.use {
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
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = albumInfo.album,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${albumInfo.artist} • ${albumInfo.songCount} tracks",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
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
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Delete Downloaded Art", color = MaterialTheme.colorScheme.error)
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
    albumInfo: DownloadedAlbumArtInfo,
    albumArtDownloader: AlbumArtDownloader,
    onDismiss: () -> Unit,
    onSelectFromPhotos: () -> Unit,
    onSelectFromFiles: () -> Unit,
    onCustomSearchResult: (ArtworkCandidate) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("${albumInfo.artist} ${albumInfo.album}") }
    var isSearching by remember { mutableStateOf(false) }
    var searchCandidates by remember { mutableStateOf<List<ArtworkCandidate>>(emptyList()) }
    var hasSearched by remember { mutableStateOf(false) }

    fun performSearch() {
        if (searchQuery.isBlank()) return
        coroutineScope.launch {
            isSearching = true
            hasSearched = true
            searchCandidates = albumArtDownloader.searchCandidatesWithQuery(searchQuery)
            isSearching = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Replace Artwork for \"${albumInfo.album}\"", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Select replacement artwork source:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))

                // Photos & Files Buttons
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

                Spacer(modifier = Modifier.height(12.dp))
                Text("Or search online with custom query:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))

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
                    Text("Tap an image to select:", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
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
                                CandidateImageThumbnail(candidate = candidate)
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
private fun CandidateImageThumbnail(candidate: ArtworkCandidate) {
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
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(candidate.source, fontSize = 9.sp, color = androidx.compose.ui.graphics.Color.White)
        }
    }
}
