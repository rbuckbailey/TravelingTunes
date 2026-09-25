package com.travelingtunes.app.feature.settings

import android.content.Intent
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import com.travelingtunes.app.core.theme.AlbumArtColorCache
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelingtunes.app.core.database.AlbumArtBrowserInfo
import com.travelingtunes.app.core.database.ArtworkType
import com.travelingtunes.app.core.database.MusicDatabase
import com.travelingtunes.app.core.media.AlbumArtCache
import com.travelingtunes.app.core.media.AlbumArtDownloader
import com.travelingtunes.app.core.media.ArtworkCandidate
import com.travelingtunes.app.core.media.Id3TagEmbedder
import com.travelingtunes.app.core.media.MusicScanner
import com.travelingtunes.app.core.media.PlaybackManager
import com.travelingtunes.app.core.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class ArtEditorCategory(val displayName: String) {
    ALBUMS("Albums"),
    TRACKS("Tracks"),
    ARTISTS("Artists"),
    GENRES("Genres"),
    FOLDERS("Folders")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadedArtBrowserScreen(
    musicDatabase: MusicDatabase,
    albumArtDownloader: AlbumArtDownloader,
    playbackManager: PlaybackManager,
    musicScanner: MusicScanner? = null,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedCategory by remember { mutableStateOf(ArtEditorCategory.ALBUMS) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "DOWNLOADED", "EMBEDDED", "MISSING"

    var allSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var allAlbums by remember { mutableStateOf<List<AlbumArtBrowserInfo>>(emptyList()) }
    var allArtists by remember { mutableStateOf<List<String>>(emptyList()) }
    var allGenres by remember { mutableStateOf<List<String>>(emptyList()) }
    var allFolders by remember { mutableStateOf<List<String>>(emptyList()) }

    var isLoading by remember { mutableStateOf(true) }

    val selectedSongs = remember { mutableStateListOf<Song>() }
    val selectedAlbums = remember { mutableStateListOf<AlbumArtBrowserInfo>() }

    // Dialog & Editing States
    var editingTargetTracks by remember { mutableStateOf<List<Song>?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

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
    val embeddingProgressCurrent by (musicScanner?.embeddingProgressCurrent?.collectAsState() ?: remember { androidx.compose.runtime.mutableIntStateOf(0) })
    val embeddingProgressTotal by (musicScanner?.embeddingProgressTotal?.collectAsState() ?: remember { androidx.compose.runtime.mutableIntStateOf(0) })
    val embeddingResultSummary by (musicScanner?.embeddingResultSummary?.collectAsState() ?: remember { mutableStateOf(null) })

    fun refreshList() {
        coroutineScope.launch {
            isLoading = true
            withContext(Dispatchers.IO) {
                allSongs = musicDatabase.getAllSongs()
                allAlbums = musicDatabase.getAllAlbumsWithArtInfo()
                allArtists = musicDatabase.getArtists()
                allGenres = musicDatabase.getGenres()
                allFolders = musicDatabase.getFolders()
            }
            selectedAlbums.removeAll { selected -> allAlbums.none { (it.album == selected.album) && (it.artist == selected.artist) } }
            selectedSongs.removeAll { selected -> allSongs.none { it.id == selected.id } }
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

    // Filtered lists based on category, search, and art filter
    val filteredAlbums = remember(allAlbums, selectedFilter, searchQuery) {
        allAlbums.filter { albumInfo ->
            val matchesType = when (selectedFilter) {
                "DOWNLOADED" -> albumInfo.artType == ArtworkType.DOWNLOADED
                "EMBEDDED" -> albumInfo.artType == ArtworkType.EMBEDDED
                "MISSING" -> albumInfo.artType == ArtworkType.MISSING
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() ||
                    albumInfo.album.contains(searchQuery, ignoreCase = true) ||
                    albumInfo.artist.contains(searchQuery, ignoreCase = true)
            matchesType && matchesQuery
        }
    }

    val filteredSongs = remember(allSongs, searchQuery) {
        if (searchQuery.isBlank()) allSongs
        else allSongs.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true) ||
                    it.album.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredArtists = remember(allArtists, searchQuery) {
        if (searchQuery.isBlank()) allArtists
        else allArtists.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    val filteredGenres = remember(allGenres, searchQuery) {
        if (searchQuery.isBlank()) allGenres
        else allGenres.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    val filteredFolders = remember(allFolders, searchQuery) {
        if (searchQuery.isBlank()) allFolders
        else allFolders.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    val albumsListState = rememberLazyListState()
    val tracksListState = rememberLazyListState()
    val artistsListState = rememberLazyListState()
    val genresListState = rememberLazyListState()
    val foldersListState = rememberLazyListState()

    // Photo & File Pickers for Replace Art
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val target = replaceTargetAlbum
        if ((uri != null) && (target != null)) {
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
        contract = ActivityResultContracts.OpenDocument(),
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
                        Text("Art & Tags Editor", fontWeight = FontWeight.Bold)
                        Text(
                            text = when (selectedCategory) {
                                ArtEditorCategory.ALBUMS -> "${filteredAlbums.size} of ${allAlbums.size} albums"
                                ArtEditorCategory.TRACKS -> "${filteredSongs.size} of ${allSongs.size} tracks"
                                ArtEditorCategory.ARTISTS -> "${filteredArtists.size} artists"
                                ArtEditorCategory.GENRES -> "${filteredGenres.size} genres"
                                ArtEditorCategory.FOLDERS -> "${filteredFolders.size} folders"
                            },
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
                    val isAllSelected = when (selectedCategory) {
                        ArtEditorCategory.ALBUMS -> selectedAlbums.size == filteredAlbums.size && filteredAlbums.isNotEmpty()
                        ArtEditorCategory.TRACKS -> selectedSongs.size == filteredSongs.size && filteredSongs.isNotEmpty()
                        else -> false
                    }
                    if (selectedCategory == ArtEditorCategory.ALBUMS || selectedCategory == ArtEditorCategory.TRACKS) {
                        TextButton(
                            onClick = {
                                if (selectedCategory == ArtEditorCategory.ALBUMS) {
                                    if (isAllSelected) selectedAlbums.clear() else {
                                        selectedAlbums.clear()
                                        selectedAlbums.addAll(filteredAlbums)
                                    }
                                } else {
                                    if (isAllSelected) selectedSongs.clear() else {
                                        selectedSongs.clear()
                                        selectedSongs.addAll(filteredSongs)
                                    }
                                }
                            }
                        ) {
                            Text(if (isAllSelected) "Deselect All" else "Select All", fontWeight = FontWeight.Bold)
                        }
                    }
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Exit to Play Screen")
                    }
                }
            )
        },
        bottomBar = {
            val hasSelection = selectedAlbums.isNotEmpty() || selectedSongs.isNotEmpty()
            val totalSelectedTracksCount = if (selectedCategory == ArtEditorCategory.TRACKS) {
                selectedSongs.size
            } else if (selectedAlbums.isNotEmpty()) {
                selectedAlbums.sumOf { it.songCount }
            } else 0

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
                    if (hasSelection) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val tracksToEdit = if (selectedCategory == ArtEditorCategory.TRACKS) {
                                        selectedSongs.toList()
                                    } else {
                                        val list = mutableListOf<Song>()
                                        for (album in selectedAlbums) {
                                            list.addAll(musicDatabase.getSongsByAlbumAndArtist(album.album, album.artist))
                                        }
                                        list
                                    }
                                    if (tracksToEdit.isNotEmpty()) {
                                        editingTargetTracks = tracksToEdit
                                        showEditDialog = true
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Tags & Art ($totalSelectedTracksCount)", fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    if (selectedCategory == ArtEditorCategory.TRACKS) {
                                        for (song in selectedSongs) {
                                            albumArtDownloader.deleteDownloadedArtworkForAlbum(song.album, song.artist, listOf(song))
                                        }
                                    } else {
                                        for (album in selectedAlbums) {
                                            val songs = musicDatabase.getSongsByAlbumAndArtist(album.album, album.artist)
                                            albumArtDownloader.deleteDownloadedArtworkForAlbum(album.album, album.artist, songs)
                                        }
                                    }
                                    playbackManager.refreshCurrentSongArtwork()
                                    refreshList()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear Art", fontSize = 13.sp)
                        }
                    } else {
                        val targets = filteredAlbums
                        OutlinedButton(
                            onClick = {
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
                            Text("Clear All Art", fontSize = 13.sp)
                        }

                        Button(
                            enabled = !isEmbeddingArt,
                            onClick = {
                                coroutineScope.launch {
                                    val pairs = targets.map { Pair(it.album, it.artist) }
                                    val uriMap = targets.associateBy({ Pair(it.album, it.artist) }) { it.artworkUri }
                                    musicScanner?.embedArtworkInBackground(pairs, uriMap, playbackManager)
                                    refreshList()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Embed All ID3", fontSize = 13.sp)
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
            // Category Tabs
            ScrollableTabRow(
                selectedTabIndex = ArtEditorCategory.entries.indexOf(selectedCategory),
                modifier = Modifier.fillMaxWidth()
            ) {
                ArtEditorCategory.entries.forEach { category ->
                    Tab(
                        selected = selectedCategory == category,
                        onClick = {
                            selectedCategory = category
                            searchQuery = ""
                        },
                        text = { Text(category.displayName, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search ${selectedCategory.displayName.lowercase()}...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )

            // Filter Chips Bar (for Albums mode)
            if (selectedCategory == ArtEditorCategory.ALBUMS) {
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
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    when (selectedCategory) {
                        ArtEditorCategory.ALBUMS -> {
                            LazyColumn(
                                state = albumsListState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                items(filteredAlbums, key = { album -> "${album.artist}_${album.album}" }) { albumInfo ->
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
                                                songs.forEach { song ->
                                                    AlbumArtCache.instance.remove(song.id)
                                                    AlbumArtColorCache.instance.removeForSong(song.id)
                                                }
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
                                        onEditTags = {
                                            coroutineScope.launch {
                                                val songs = musicDatabase.getSongsByAlbumAndArtist(albumInfo.album, albumInfo.artist)
                                                editingTargetTracks = songs
                                                showEditDialog = true
                                            }
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
                        ArtEditorCategory.TRACKS -> {
                            LazyColumn(
                                state = tracksListState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                items(filteredSongs, key = { song -> song.id }) { song ->
                                    val isSelected = selectedSongs.contains(song)
                                    TrackItemRow(
                                        song = song,
                                        isSelected = isSelected,
                                        onToggleSelect = {
                                            if (isSelected) selectedSongs.remove(song) else selectedSongs.add(song)
                                        },
                                        onEditTags = {
                                            editingTargetTracks = listOf(song)
                                            showEditDialog = true
                                        }
                                    )
                                }
                            }
                        }
                        ArtEditorCategory.ARTISTS -> {
                            LazyColumn(
                                state = artistsListState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                items(filteredArtists, key = { artist -> artist }) { artist ->
                                    val artistSongs = remember(artist, allSongs) { allSongs.filter { it.artist.equals(artist, ignoreCase = true) } }
                                    GroupItemRow(
                                        title = artist,
                                        subtitle = "${artistSongs.size} tracks",
                                        icon = Icons.Default.MusicNote,
                                        onEditTags = {
                                            editingTargetTracks = artistSongs
                                            showEditDialog = true
                                        }
                                    )
                                }
                            }
                        }
                        ArtEditorCategory.GENRES -> {
                            LazyColumn(
                                state = genresListState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                items(filteredGenres, key = { genre -> genre }) { genre ->
                                    val genreSongs = remember(genre, allSongs) { allSongs.filter { it.genre.equals(genre, ignoreCase = true) } }
                                    GroupItemRow(
                                        title = genre,
                                        subtitle = "${genreSongs.size} tracks",
                                        icon = Icons.Default.MusicNote,
                                        onEditTags = {
                                            editingTargetTracks = genreSongs
                                            showEditDialog = true
                                        }
                                    )
                                }
                            }
                        }
                        ArtEditorCategory.FOLDERS -> {
                            LazyColumn(
                                state = foldersListState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                items(filteredFolders, key = { folder -> folder }) { folder ->
                                    val folderSongs = remember(folder, allSongs) { allSongs.filter { it.folderPath.equals(folder, ignoreCase = true) } }
                                    GroupItemRow(
                                        title = folder.substringAfterLast('/'),
                                        subtitle = "$folder • ${folderSongs.size} tracks",
                                        icon = Icons.Default.Folder,
                                        onEditTags = {
                                            editingTargetTracks = folderSongs
                                            showEditDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Tags & Artwork Dialog
    if (showEditDialog && !editingTargetTracks.isNullOrEmpty()) {
        val targetTracks = editingTargetTracks!!
        EditTagsAndArtworkDialog(
            targetTracks = targetTracks,
            musicDatabase = musicDatabase,
            albumArtDownloader = albumArtDownloader,
            playbackManager = playbackManager,
            onDismiss = {
                showEditDialog = false
                editingTargetTracks = null
            },
            onSaveComplete = {
                showEditDialog = false
                editingTargetTracks = null
                refreshList()
            }
        )
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
            albumArtDownloader = albumArtDownloader,
            onDismiss = {
                zoomPreviewCandidate = null
                zoomPreviewUri = null
            }
        )
    }
}

@Composable
private fun EditTagsAndArtworkDialog(
    targetTracks: List<Song>,
    musicDatabase: MusicDatabase,
    albumArtDownloader: AlbumArtDownloader,
    playbackManager: PlaybackManager,
    onDismiss: () -> Unit,
    onSaveComplete: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isMultiTrack = targetTracks.size > 1
    val sampleTrack = targetTracks.first()

    // Single track field states
    var songTitle by remember { mutableStateOf(if (isMultiTrack) "" else sampleTrack.title) }
    var trackNumberStr by remember { mutableStateOf(if (isMultiTrack) "" else if (sampleTrack.trackNumber > 0) sampleTrack.trackNumber.toString() else "") }

    // Shared field states
    var artist by remember { mutableStateOf(if (targetTracks.all { it.artist == sampleTrack.artist }) sampleTrack.artist else "") }
    var album by remember { mutableStateOf(if (targetTracks.all { it.album == sampleTrack.album }) sampleTrack.album else "") }
    var genre by remember { mutableStateOf(if (targetTracks.all { it.genre == sampleTrack.genre }) sampleTrack.genre else "") }
    var yearStr by remember { mutableStateOf(if (targetTracks.all { it.year == sampleTrack.year }) if (sampleTrack.year > 0) sampleTrack.year.toString() else "" else "") }
    var discNumberStr by remember { mutableStateOf(if (targetTracks.all { it.discNumber == sampleTrack.discNumber }) if (sampleTrack.discNumber > 0) sampleTrack.discNumber.toString() else "" else "") }

    // Checkboxes for bulk mode to select which fields to apply
    var applyArtist by remember { mutableStateOf(!isMultiTrack) }
    var applyAlbum by remember { mutableStateOf(!isMultiTrack) }
    var applyGenre by remember { mutableStateOf(!isMultiTrack) }
    var applyYear by remember { mutableStateOf(!isMultiTrack) }
    var applyDiscNumber by remember { mutableStateOf(!isMultiTrack) }

    var isSaving by remember { mutableStateOf(false) }

    fun saveChanges() {
        coroutineScope.launch {
            isSaving = true
            withContext(Dispatchers.IO) {
                val newYear = yearStr.toIntOrNull()
                val newTrackNum = trackNumberStr.toIntOrNull()
                val newDiscNum = discNumberStr.toIntOrNull()

                for (song in targetTracks) {
                    val finalTitle = if (!isMultiTrack) songTitle.ifBlank { song.title } else song.title
                    val finalTrackNum = if (!isMultiTrack) newTrackNum ?: song.trackNumber else song.trackNumber
                    val finalArtist = if (!isMultiTrack || applyArtist) artist else song.artist
                    val finalAlbum = if (!isMultiTrack || applyAlbum) album else song.album
                    val finalGenre = if (!isMultiTrack || applyGenre) genre else song.genre
                    val finalYear = if (!isMultiTrack || applyYear) newYear ?: song.year else song.year
                    val finalDiscNum = if (!isMultiTrack || applyDiscNumber) newDiscNum ?: song.discNumber else song.discNumber

                    // 1. Update Database
                    musicDatabase.updateSongMetadata(
                        songId = song.id,
                        title = finalTitle,
                        artist = finalArtist,
                        album = finalAlbum,
                        genre = finalGenre,
                        year = finalYear,
                        trackNumber = finalTrackNum,
                        discNumber = finalDiscNum
                    )

                    // 2. Embed Tags into MP3 / FLAC file directly
                    Id3TagEmbedder.embedMetadataTagsIntoSong(
                        context = context,
                        song = song,
                        genre = finalGenre,
                        title = finalTitle,
                        artist = finalArtist,
                        album = finalAlbum,
                        year = finalYear,
                        trackNumber = finalTrackNum,
                        discNumber = finalDiscNum
                    )

                    AlbumArtCache.instance.remove(song.id)
                }
            }
            playbackManager.refreshCurrentSongArtwork()
            isSaving = false
            onSaveComplete()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isMultiTrack) "Bulk Edit Tags (${targetTracks.size} Tracks)" else "Edit Tags & Art",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (isMultiTrack) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "Editing shared tags across ${targetTracks.size} selected tracks. Check the fields you want to update. Unique track fields (Title, Track Number) are preserved per track.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (!isMultiTrack) {
                    OutlinedTextField(
                        value = songTitle,
                        onValueChange = { songTitle = it },
                        label = { Text("Song Title") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = trackNumberStr,
                            onValueChange = { trackNumberStr = it },
                            label = { Text("Track #") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = discNumberStr,
                            onValueChange = { discNumberStr = it },
                            label = { Text("Disc #") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Shared Artist Field
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isMultiTrack) {
                        Checkbox(checked = applyArtist, onCheckedChange = { applyArtist = it })
                    }
                    OutlinedTextField(
                        value = artist,
                        onValueChange = { artist = it },
                        enabled = !isMultiTrack || applyArtist,
                        label = { Text("Artist") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Shared Album Field
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isMultiTrack) {
                        Checkbox(checked = applyAlbum, onCheckedChange = { applyAlbum = it })
                    }
                    OutlinedTextField(
                        value = album,
                        onValueChange = { album = it },
                        enabled = !isMultiTrack || applyAlbum,
                        label = { Text("Album") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Shared Genre Field
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isMultiTrack) {
                        Checkbox(checked = applyGenre, onCheckedChange = { applyGenre = it })
                    }
                    OutlinedTextField(
                        value = genre,
                        onValueChange = { genre = it },
                        enabled = !isMultiTrack || applyGenre,
                        label = { Text("Genre") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Shared Year Field
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isMultiTrack) {
                        Checkbox(checked = applyYear, onCheckedChange = { applyYear = it })
                    }
                    OutlinedTextField(
                        value = yearStr,
                        onValueChange = { yearStr = it },
                        enabled = !isMultiTrack || applyYear,
                        label = { Text("Year") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (isMultiTrack) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(checked = applyDiscNumber, onCheckedChange = { applyDiscNumber = it })
                        OutlinedTextField(
                            value = discNumberStr,
                            onValueChange = { discNumberStr = it },
                            enabled = applyDiscNumber,
                            label = { Text("Disc #") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (isSaving) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                onClick = { saveChanges() }
            ) {
                Text(if (isSaving) "Saving Tags..." else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isSaving,
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun TrackItemRow(
    song: Song,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onEditTags: () -> Unit
) {
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
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${song.artist} • ${song.album}${if (song.trackNumber > 0) " (Track ${song.trackNumber})" else ""}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onEditTags) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Tags", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun GroupItemRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onEditTags: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditTags() }
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEditTags) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Group Tags", tint = MaterialTheme.colorScheme.primary)
            }
        }
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
    onEditTags: () -> Unit,
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
                                Text("Edit Album Tags")
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onEditTags()
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(20.dp))
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

enum class TargetAlbumFilterMode(val displayName: String) {
    ALL("All"),
    SAME_ARTIST("Same Artist"),
    MISSING_ART("Missing Art Only"),
    EMBEDDED("Embedded Art"),
    DOWNLOADED("Downloaded Art")
}

enum class SourceAlbumFilterMode(val displayName: String) {
    ALL("All Albums"),
    SAME_ARTIST("Same Artist Only")
}

fun filterTargetAlbums(
    allAlbums: List<AlbumArtBrowserInfo>,
    sourceAlbum: AlbumArtBrowserInfo,
    query: String,
    filterMode: TargetAlbumFilterMode
): List<AlbumArtBrowserInfo> {
    return allAlbums.filter { candidate ->
        val isNotSource = (candidate.album != sourceAlbum.album || candidate.artist != sourceAlbum.artist)
        if (!isNotSource) return@filter false

        val matchesQuery = query.isBlank() ||
                candidate.album.contains(query, ignoreCase = true) ||
                candidate.artist.contains(query, ignoreCase = true)
        if (!matchesQuery) return@filter false

        when (filterMode) {
            TargetAlbumFilterMode.ALL -> true
            TargetAlbumFilterMode.SAME_ARTIST -> candidate.artist.equals(sourceAlbum.artist, ignoreCase = true)
            TargetAlbumFilterMode.MISSING_ART -> candidate.artType == ArtworkType.MISSING || candidate.artworkUri == null
            TargetAlbumFilterMode.EMBEDDED -> candidate.artType == ArtworkType.EMBEDDED
            TargetAlbumFilterMode.DOWNLOADED -> candidate.artType == ArtworkType.DOWNLOADED
        }
    }
}

fun filterSourceAlbums(
    allAlbums: List<AlbumArtBrowserInfo>,
    targetAlbum: AlbumArtBrowserInfo,
    query: String,
    filterMode: SourceAlbumFilterMode
): List<AlbumArtBrowserInfo> {
    return allAlbums.filter { candidate ->
        val hasArtAndNotTarget = candidate.artworkUri != null &&
                (candidate.album != targetAlbum.album || candidate.artist != targetAlbum.artist)
        if (!hasArtAndNotTarget) return@filter false

        val matchesQuery = query.isBlank() ||
                candidate.album.contains(query, ignoreCase = true) ||
                candidate.artist.contains(query, ignoreCase = true)
        if (!matchesQuery) return@filter false

        when (filterMode) {
            SourceAlbumFilterMode.ALL -> true
            SourceAlbumFilterMode.SAME_ARTIST -> candidate.artist.equals(targetAlbum.artist, ignoreCase = true)
        }
    }
}

@Composable
private fun AlbumArtThumbnail(
    artworkUri: Uri?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val context = LocalContext.current
    val bitmap = remember(artworkUri) {
        val uri = artworkUri ?: return@remember null
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

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(6.dp))
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}

@Composable
private fun ArtworkStatusBadge(artType: ArtworkType) {
    val (label, containerColor, contentColor) = when (artType) {
        ArtworkType.DOWNLOADED -> Triple("Downloaded", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        ArtworkType.EMBEDDED -> Triple("Embedded", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        ArtworkType.MISSING -> Triple("Missing Art", MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f), MaterialTheme.colorScheme.onErrorContainer)
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.padding(vertical = 1.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("${targetAlbum.artist} ${targetAlbum.album}") }
    var isSearching by remember { mutableStateOf(false) }
    var searchCandidates by remember { mutableStateOf<List<ArtworkCandidate>>(emptyList()) }
    var hasSearched by remember { mutableStateOf(false) }

    var sourceSearchQuery by remember { mutableStateOf("") }
    var sourceFilterMode by remember { mutableStateOf(SourceAlbumFilterMode.ALL) }

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

    fun openSearchInBrowser() {
        val queryToUse = if (searchQuery.isNotBlank()) searchQuery else "${targetAlbum.artist} ${targetAlbum.album}"
        val encodedQuery = Uri.encode("$queryToUse album cover")
        val url = "https://www.google.com/search?q=$encodedQuery&tbm=isch"
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Replace Artwork for \"${targetAlbum.album}\"", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Select replacement artwork source:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))

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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAlbumCopyPicker = !showAlbumCopyPicker },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (showAlbumCopyPicker) "Hide Copy Picker" else "Copy Album Art", fontSize = 11.sp, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = { openSearchInBrowser() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Web Browser", fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }

                if (showAlbumCopyPicker) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Select source album to copy artwork from:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))

                    val filteredSources = remember(allAlbums, targetAlbum, sourceSearchQuery, sourceFilterMode) {
                        filterSourceAlbums(allAlbums, targetAlbum, sourceSearchQuery, sourceFilterMode)
                    }

                    OutlinedTextField(
                        value = sourceSearchQuery,
                        onValueChange = { sourceSearchQuery = it },
                        placeholder = { Text("Filter source albums...", fontSize = 11.sp) },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        trailingIcon = {
                            if (sourceSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { sourceSearchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(SourceAlbumFilterMode.entries.toTypedArray()) { mode ->
                            val isSelected = sourceFilterMode == mode
                            FilterChip(
                                selected = isSelected,
                                onClick = { sourceFilterMode = mode },
                                label = { Text(mode.displayName, fontSize = 10.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (filteredSources.isEmpty()) {
                        Text("No matching source albums found.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 150.dp)
                        ) {
                            items(filteredSources, key = { "${it.artist}_${it.album}" }) { source ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onCopyFromOtherAlbum(source) }
                                        .padding(vertical = 4.dp, horizontal = 4.dp)
                                ) {
                                    AlbumArtThumbnail(artworkUri = source.artworkUri, size = 32.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(source.album, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${source.artist} • ${source.songCount} tracks", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    ArtworkStatusBadge(artType = source.artType)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Search online with custom query:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))

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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Custom Search Query") },
                        singleLine = true,
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { performSearch() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Search In-App", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { openSearchInBrowser() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open in Browser", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Searching artwork providers...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else if (hasSearched && searchCandidates.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No artwork results found for search query.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else if (searchCandidates.isNotEmpty()) {
                    Text("Tap an image to select artwork:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                    ) {
                        items(searchCandidates) { candidate ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(135.dp)
                                    .clickable { onCustomSearchResult(candidate) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                CandidateImageThumbnail(
                                    candidate = candidate,
                                    albumArtDownloader = albumArtDownloader,
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
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterMode by remember { mutableStateOf(TargetAlbumFilterMode.ALL) }

    val filteredTargets = remember(allAlbums, sourceAlbum, searchQuery, selectedFilterMode) {
        filterTargetAlbums(allAlbums, sourceAlbum, searchQuery, selectedFilterMode)
    }

    val selectedTargets = remember { mutableStateListOf<AlbumArtBrowserInfo>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy Art to Other Albums", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Source Album Banner Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        AlbumArtThumbnail(artworkUri = sourceAlbum.artworkUri, size = 44.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Source: ${sourceAlbum.album}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${sourceAlbum.artist} • ${sourceAlbum.songCount} tracks",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        ArtworkStatusBadge(artType = sourceAlbum.artType)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Select target albums to receive artwork:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Search Filter TextField
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter albums or artists...", fontSize = 12.sp) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Filter Chips Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(TargetAlbumFilterMode.entries.toTypedArray()) { filterMode ->
                        val isSelected = selectedFilterMode == filterMode
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilterMode = filterMode },
                            label = { Text(filterMode.displayName, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Quick Action Bar & Selection Stats
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${selectedTargets.size} selected (${filteredTargets.size} showing)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        val missingInFiltered = remember(filteredTargets) {
                            filteredTargets.filter { it.artType == ArtworkType.MISSING || it.artworkUri == null }
                        }
                        if (missingInFiltered.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    missingInFiltered.forEach { missingItem ->
                                        if (!selectedTargets.contains(missingItem)) {
                                            selectedTargets.add(missingItem)
                                        }
                                    }
                                },
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Select Missing", fontSize = 11.sp)
                            }
                        }

                        val allFilteredSelected = filteredTargets.isNotEmpty() && filteredTargets.all { selectedTargets.contains(it) }
                        TextButton(
                            onClick = {
                                if (allFilteredSelected) {
                                    selectedTargets.removeAll(filteredTargets)
                                } else {
                                    filteredTargets.forEach { target ->
                                        if (!selectedTargets.contains(target)) {
                                            selectedTargets.add(target)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(if (allFilteredSelected) "Deselect Filtered" else "Select All Filtered", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Target Albums List
                if (filteredTargets.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No albums match \"$searchQuery\"" else "No target albums match filter.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                    ) {
                        items(filteredTargets, key = { "${it.artist}_${it.album}" }) { target ->
                            val isSelected = selectedTargets.contains(target)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isSelected) selectedTargets.remove(target) else selectedTargets.add(target)
                                    }
                                    .padding(vertical = 4.dp, horizontal = 2.dp)
                                ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        if (isSelected) selectedTargets.remove(target) else selectedTargets.add(target)
                                    },
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))

                                AlbumArtThumbnail(artworkUri = target.artworkUri, size = 36.dp)

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = target.album,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${target.artist} • ${target.songCount} tracks",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                ArtworkStatusBadge(artType = target.artType)
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
    albumArtDownloader: AlbumArtDownloader? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(candidate, artworkUri) {
        withContext(Dispatchers.IO) {
            isLoading = true
            if (candidate != null && albumArtDownloader != null) {
                bitmap = albumArtDownloader.fetchImageBitmap(candidate.url)
            } else if (artworkUri != null) {
                try {
                    bitmap = if (artworkUri.scheme == "file") {
                        android.graphics.BitmapFactory.decodeFile(artworkUri.path)
                    } else {
                        context.contentResolver.openInputStream(artworkUri)?.use {
                            android.graphics.BitmapFactory.decodeStream(it)
                        }
                    }
                } catch (e: Exception) {
                    bitmap = null
                }
            }
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = "Artwork Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("Failed to load image preview.", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun CandidateImageThumbnail(
    candidate: ArtworkCandidate,
    albumArtDownloader: AlbumArtDownloader,
    onZoom: () -> Unit
) {
    var bitmap by remember(candidate.url) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(candidate.url) {
        withContext(Dispatchers.IO) {
            bitmap = albumArtDownloader.fetchImageBitmap(candidate.url)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(2.dp)
                    .clickable { onZoom() }
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = "Zoom",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
    }
}
