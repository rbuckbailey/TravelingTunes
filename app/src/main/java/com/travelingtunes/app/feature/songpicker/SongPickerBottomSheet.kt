package com.travelingtunes.app.feature.songpicker

import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelingtunes.app.core.database.AlbumInfo
import com.travelingtunes.app.core.database.MusicDatabase
import com.travelingtunes.app.core.datastore.SettingsDataStore
import com.travelingtunes.app.core.media.MusicScanner
import com.travelingtunes.app.core.media.PlaybackManager
import com.travelingtunes.app.core.media.StreamingCatalogRepository
import com.travelingtunes.app.core.model.Song
import com.travelingtunes.app.core.model.StreamingAccount
import com.travelingtunes.app.core.model.StreamingServiceId
import com.travelingtunes.app.core.model.StreamingTrack
import com.travelingtunes.app.feature.player.loadSongArtwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PickerCategory(val displayName: String) {
    ALBUMS("Albums"),
    SONGS("Songs"),
    ARTISTS("Artists"),
    GENRES("Genres"),
    FOLDERS("Folders")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongPickerBottomSheet(
    musicDatabase: MusicDatabase,
    playbackManager: PlaybackManager,
    musicScanner: MusicScanner? = null,
    settingsDataStore: SettingsDataStore? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    /*
    // Streaming state placeholder (Commented out for future development)
    val effectiveDataStore = remember(settingsDataStore, context) {
        settingsDataStore ?: SettingsDataStore(context)
    }
    val streamingAccounts by effectiveDataStore.streamingAccountsFlow.collectAsState(initial = emptyMap())
    val signedInServicesList = remember(streamingAccounts) {
        StreamingServiceId.entries.filter { streamingAccounts.containsKey(it.id) }
    }
    var activeTopTab by remember { mutableStateOf("local") }

    LaunchedEffect(signedInServicesList) {
        if (activeTopTab != "local" && signedInServicesList.none { it.id == activeTopTab }) {
            activeTopTab = "local"
        }
    }
    */

    val isScanning by musicScanner?.isScanning?.collectAsState() ?: remember { mutableStateOf(false) }
    val scanStatusMessage by musicScanner?.statusMessage?.collectAsState() ?: remember { mutableStateOf(null) }
    val scannedCount by musicScanner?.scannedCount?.collectAsState() ?: remember { mutableStateOf(0) }

    val isDownloadingArt by musicScanner?.isDownloadingArt?.collectAsState() ?: remember { mutableStateOf(false) }
    val artDownloadStatusMessage by musicScanner?.artDownloadStatusMessage?.collectAsState() ?: remember { mutableStateOf(null) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(PickerCategory.ALBUMS) }

    // Drill-down hierarchy state
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var selectedArtist by remember { mutableStateOf<String?>(null) }
    var selectedAlbum by remember { mutableStateOf<String?>(null) }
    var selectedFolder by remember { mutableStateOf<String?>(null) }

    var songsList by remember { mutableStateOf<List<Song>>(emptyList()) }
    var albumsList by remember { mutableStateOf<List<AlbumInfo>>(emptyList()) }
    var artistsList by remember { mutableStateOf<List<String>>(emptyList()) }
    var genresList by remember { mutableStateOf<List<String>>(emptyList()) }
    var foldersList by remember { mutableStateOf<List<String>>(emptyList()) }

    val currentPlaylist by playbackManager.currentPlaylist.collectAsState()

    // Query database when search, category, or drill-down selection changes
    LaunchedEffect(searchQuery, selectedCategory, selectedGenre, selectedArtist, selectedAlbum, selectedFolder, currentPlaylist) {
        withContext(Dispatchers.IO) {
            if (musicDatabase.getAllSongs().isEmpty() && currentPlaylist.isNotEmpty()) {
                musicDatabase.insertOrReplaceSongs(currentPlaylist)
            }

            // Determine active view level
            if (selectedAlbum != null) {
                val albumSongs = musicDatabase.getSongsByAlbum(selectedAlbum!!)
                val filtered = albumSongs.filter { song ->
                    (selectedArtist == null || song.artist.equals(selectedArtist, true)) &&
                    (selectedGenre == null || song.genre.equals(selectedGenre, true))
                }
                val base = if (filtered.isNotEmpty()) filtered else albumSongs
                songsList = if (searchQuery.isBlank()) base else base.filter { it.title.contains(searchQuery, true) }
            } else if (selectedArtist != null) {
                val artistSongs = musicDatabase.getSongsByArtist(selectedArtist!!).filter { song ->
                    selectedGenre == null || song.genre.equals(selectedGenre, true)
                }
                val albumMap = artistSongs.groupBy { it.album }
                val albums = albumMap.map { (albumName, songs) ->
                    AlbumInfo(
                        name = albumName,
                        artist = selectedArtist!!,
                        songCount = songs.size,
                        artworkUri = songs.firstOrNull()?.artworkUri
                    )
                }
                albumsList = if (searchQuery.isBlank()) albums else albums.filter { it.name.contains(searchQuery, true) }
            } else if (selectedGenre != null) {
                val genreSongs = musicDatabase.getSongsByGenre(selectedGenre!!)
                val artists = genreSongs.map { it.artist }.distinct().sorted()
                artistsList = if (searchQuery.isBlank()) artists else artists.filter { it.contains(searchQuery, true) }
            } else if (selectedFolder != null) {
                val targetFolder = selectedFolder!!
                val dbSongs = musicDatabase.getAllSongs()
                val folderSongs = dbSongs.filter {
                    it.folderPath.equals(targetFolder, ignoreCase = true) ||
                    it.folderPath.startsWith(targetFolder, ignoreCase = true)
                }
                songsList = if (searchQuery.isBlank()) folderSongs else folderSongs.filter { it.title.contains(searchQuery, true) }
            } else {
                when (selectedCategory) {
                    PickerCategory.SONGS -> {
                        val dbSongs = musicDatabase.searchSongs(searchQuery)
                        val baseList = if (dbSongs.isNotEmpty()) dbSongs else currentPlaylist
                        songsList = if (searchQuery.isBlank()) {
                            baseList
                        } else {
                            baseList.filter {
                                it.title.contains(searchQuery, ignoreCase = true) ||
                                        it.artist.contains(searchQuery, ignoreCase = true) ||
                                        it.album.contains(searchQuery, ignoreCase = true) ||
                                        it.genre.contains(searchQuery, ignoreCase = true)
                            }
                        }
                    }
                    PickerCategory.ALBUMS -> {
                        val allAlbums = musicDatabase.getAlbums()
                        albumsList = if (allAlbums.isNotEmpty()) {
                            if (searchQuery.isBlank()) allAlbums
                            else allAlbums.filter {
                                it.name.contains(searchQuery, ignoreCase = true) ||
                                        it.artist.contains(searchQuery, ignoreCase = true)
                            }
                        } else {
                            currentPlaylist.groupBy { it.album }.map { (albumName, songs) ->
                                AlbumInfo(
                                    name = albumName,
                                    artist = songs.firstOrNull()?.artist ?: "Unknown Artist",
                                    songCount = songs.size,
                                    artworkUri = songs.firstOrNull()?.artworkUri
                                )
                            }
                        }
                    }
                    PickerCategory.ARTISTS -> {
                        val allArtists = musicDatabase.getArtists()
                        artistsList = if (allArtists.isNotEmpty()) {
                            if (searchQuery.isBlank()) allArtists
                            else allArtists.filter { it.contains(searchQuery, ignoreCase = true) }
                        } else {
                            currentPlaylist.map { it.artist }.distinct().sorted()
                        }
                    }
                    PickerCategory.GENRES -> {
                        val allGenres = musicDatabase.getGenres()
                        genresList = if (allGenres.isNotEmpty()) {
                            if (searchQuery.isBlank()) allGenres
                            else allGenres.filter { it.contains(searchQuery, ignoreCase = true) }
                        } else {
                            currentPlaylist.map { it.genre }.distinct().sorted()
                        }
                    }
                    PickerCategory.FOLDERS -> {
                        val allFolders = musicDatabase.getFolders()
                        foldersList = if (allFolders.isNotEmpty()) {
                            if (searchQuery.isBlank()) allFolders
                            else allFolders.filter { it.contains(searchQuery, ignoreCase = true) }
                        } else {
                            currentPlaylist.map { it.folderPath }.distinct().filter { it.isNotBlank() }.sorted()
                        }
                    }
                }
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxSize()
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
                Text(
                    text = "Song Picker",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (musicScanner != null) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    musicScanner.downloadMissingArtwork()
                                }
                            },
                            enabled = !isScanning && !isDownloadingArt
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Download Missing Album Art",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Local Library Storage View
            OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search local title, artist, album, genre...") },
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
                    shape = RoundedCornerShape(24.dp)
                )

                if (isScanning) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Library scan in progress ($scannedCount songs)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            if (!scanStatusMessage.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = scanStatusMessage ?: "",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                if (isDownloadingArt) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Downloading Missing Artwork...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            if (!artDownloadStatusMessage.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = artDownloadStatusMessage ?: "",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Category Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = false,
                            onClick = {
                                coroutineScope.launch {
                                    musicScanner?.downloadMissingArtwork()
                                }
                            },
                            enabled = !isScanning && !isDownloadingArt,
                            label = { Text("Download Missing Art") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                    items(PickerCategory.entries.toTypedArray()) { category ->
                        FilterChip(
                            selected = selectedCategory == category && selectedGenre == null && selectedArtist == null && selectedAlbum == null && selectedFolder == null,
                            onClick = {
                                selectedCategory = category
                                selectedGenre = null
                                selectedArtist = null
                                selectedAlbum = null
                                selectedFolder = null
                            },
                            label = { Text(category.displayName) }
                        )
                    }
                }

                // Breadcrumb Navigation Header when drilled down
                val hasDrillDown = selectedGenre != null || selectedArtist != null || selectedAlbum != null || selectedFolder != null
                if (hasDrillDown) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = {
                                if (selectedAlbum != null) selectedAlbum = null
                                else if (selectedArtist != null) selectedArtist = null
                                else if (selectedGenre != null) selectedGenre = null
                                else if (selectedFolder != null) selectedFolder = null
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Text(
                            text = listOfNotNull(selectedGenre, selectedArtist, selectedAlbum, selectedFolder).joinToString(" > "),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Content Area depending on current drill-down level
                val isSongsView = selectedAlbum != null || selectedFolder != null || (selectedCategory == PickerCategory.SONGS && !hasDrillDown)
                val isAlbumsView = selectedArtist != null && selectedAlbum == null
                val isArtistsView = selectedGenre != null && selectedArtist == null && selectedAlbum == null

                if (isSongsView) {
                    if (songsList.isEmpty()) {
                        EmptyListState("No songs found")
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        ) {
                            items(songsList.size) { index ->
                                val song = songsList[index]
                                SongItemRow(
                                    song = song,
                                    onPlay = {
                                        playbackManager.setPlaylistAndPlay(songsList, index)
                                        if (selectedAlbum != null) {
                                            playbackManager.setRepeatMode(com.travelingtunes.app.core.model.RepeatMode.ALBUM)
                                        }
                                        onDismiss()
                                    },
                                    onAddToQueue = {
                                        playbackManager.addSongToQueue(song)
                                    },
                                    onClick = {
                                        playbackManager.setPlaylistAndPlay(songsList, index)
                                        if (selectedAlbum != null) {
                                            playbackManager.setRepeatMode(com.travelingtunes.app.core.model.RepeatMode.ALBUM)
                                        }
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                } else if (isAlbumsView || (selectedCategory == PickerCategory.ALBUMS && !hasDrillDown)) {
                    if (albumsList.isEmpty()) {
                        EmptyListState("No albums found")
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        ) {
                            items(albumsList) { album ->
                                AlbumItemRow(
                                    album = album,
                                    onPlay = {
                                        coroutineScope.launch {
                                            val albumSongs = musicDatabase.getSongsByAlbum(album.name)
                                            val filteredSongs = if (selectedGenre != null || selectedArtist != null) {
                                                albumSongs.filter {
                                                    (selectedGenre == null || it.genre.equals(selectedGenre, true)) &&
                                                    (selectedArtist == null || it.artist.equals(selectedArtist, true))
                                                }
                                            } else albumSongs
                                            val playSongs = if (filteredSongs.isNotEmpty()) filteredSongs else albumSongs
                                            if (playSongs.isNotEmpty()) {
                                                playbackManager.setPlaylistAndPlay(playSongs, 0)
                                                playbackManager.setRepeatMode(com.travelingtunes.app.core.model.RepeatMode.ALBUM)
                                            }
                                            onDismiss()
                                        }
                                    },
                                    onAddToQueue = {
                                        coroutineScope.launch {
                                            val albumSongs = musicDatabase.getSongsByAlbum(album.name)
                                            val filteredSongs = if (selectedGenre != null || selectedArtist != null) {
                                                albumSongs.filter {
                                                    (selectedGenre == null || it.genre.equals(selectedGenre, true)) &&
                                                    (selectedArtist == null || it.artist.equals(selectedArtist, true))
                                                }
                                            } else albumSongs
                                            val addSongs = if (filteredSongs.isNotEmpty()) filteredSongs else albumSongs
                                            playbackManager.addSongsToQueue(addSongs)
                                        }
                                    },
                                    onClick = {
                                        selectedAlbum = album.name
                                    }
                                )
                            }
                        }
                    }
                } else if (isArtistsView || (selectedCategory == PickerCategory.ARTISTS && !hasDrillDown)) {
                    if (artistsList.isEmpty()) {
                        EmptyListState("No artists found")
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        ) {
                            items(artistsList) { artist ->
                                ArtistItemRow(
                                    artist = artist,
                                    onPlay = {
                                        coroutineScope.launch {
                                            val allSongs = if (selectedGenre != null) {
                                                musicDatabase.getSongsByGenre(selectedGenre!!).filter { it.artist.equals(artist, true) }
                                            } else {
                                                musicDatabase.getSongsByArtist(artist)
                                            }
                                            if (allSongs.isNotEmpty()) {
                                                playbackManager.setPlaylistAndPlay(allSongs, 0)
                                            }
                                            onDismiss()
                                        }
                                    },
                                    onAddToQueue = {
                                        coroutineScope.launch {
                                            val allSongs = if (selectedGenre != null) {
                                                musicDatabase.getSongsByGenre(selectedGenre!!).filter { it.artist.equals(artist, true) }
                                            } else {
                                                musicDatabase.getSongsByArtist(artist)
                                            }
                                            playbackManager.addSongsToQueue(allSongs)
                                        }
                                    },
                                    onClick = {
                                        selectedArtist = artist
                                    }
                                )
                            }
                        }
                    }
                } else if (selectedCategory == PickerCategory.GENRES && !hasDrillDown) {
                    if (genresList.isEmpty()) {
                        EmptyListState("No genres found")
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        ) {
                            items(genresList) { genre ->
                                GenreItemRow(
                                    genre = genre,
                                    onPlay = {
                                        coroutineScope.launch {
                                            val genreSongs = musicDatabase.getSongsByGenre(genre)
                                            if (genreSongs.isNotEmpty()) {
                                                playbackManager.setPlaylistAndPlay(genreSongs, 0)
                                            }
                                            onDismiss()
                                        }
                                    },
                                    onAddToQueue = {
                                        coroutineScope.launch {
                                            val genreSongs = musicDatabase.getSongsByGenre(genre)
                                            playbackManager.addSongsToQueue(genreSongs)
                                        }
                                    },
                                    onClick = {
                                        selectedGenre = genre
                                    }
                                )
                            }
                        }
                    }
                } else if (selectedCategory == PickerCategory.FOLDERS && !hasDrillDown) {
                    if (foldersList.isEmpty()) {
                        EmptyListState("No subfolders found")
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        ) {
                            items(foldersList) { folder ->
                                FolderItemRow(
                                    folder = folder,
                                    onPlay = {
                                        coroutineScope.launch {
                                            val dbSongs = musicDatabase.getAllSongs()
                                            val folderSongs = dbSongs.filter {
                                                it.folderPath.equals(folder, ignoreCase = true) ||
                                                it.folderPath.startsWith(folder, ignoreCase = true)
                                            }
                                            if (folderSongs.isNotEmpty()) {
                                                playbackManager.setPlaylistAndPlay(folderSongs, 0)
                                            }
                                            onDismiss()
                                        }
                                    },
                                    onAddToQueue = {
                                        coroutineScope.launch {
                                            val dbSongs = musicDatabase.getAllSongs()
                                            val folderSongs = dbSongs.filter {
                                                it.folderPath.equals(folder, ignoreCase = true) ||
                                                it.folderPath.startsWith(folder, ignoreCase = true)
                                            }
                                            playbackManager.addSongsToQueue(folderSongs)
                                        }
                                    },
                                    onClick = {
                                        selectedFolder = folder
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

/*
@Composable
fun StreamingPlatformView(
    serviceId: String,
    account: StreamingAccount?,
    playbackManager: PlaybackManager,
    onDismiss: () -> Unit
) {
    val service = remember(serviceId) { StreamingServiceId.fromId(serviceId) }
    var streamingSearchQuery by remember { mutableStateOf("") }
    var selectedPlaylistFilter by remember { mutableStateOf<String?>(null) }

    val tracks = remember(serviceId, streamingSearchQuery) {
        StreamingCatalogRepository.getTracksForService(serviceId, streamingSearchQuery)
    }

    val playlists = remember(serviceId) {
        StreamingCatalogRepository.getPlaylistsForService(serviceId)
    }

    val filteredTracks = if (selectedPlaylistFilter != null) {
        tracks.take(3)
    } else tracks

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(12.dp)
            ) {
                if (service != null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(service.brandColorHex)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = service.displayName.take(1),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${service?.displayName ?: serviceId} Catalog",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (account != null) "Signed in: ${account.username} (${account.accountType})"
                               else "Connected via Open API",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "LIVE API",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = streamingSearchQuery,
            onValueChange = { streamingSearchQuery = it },
            placeholder = { Text("Search ${service?.displayName ?: "streaming"} tracks...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (streamingSearchQuery.isNotEmpty()) {
                    IconButton(onClick = { streamingSearchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Featured Playlists & Stations",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedPlaylistFilter == null,
                    onClick = { selectedPlaylistFilter = null },
                    label = { Text("All Tracks") }
                )
            }
            items(playlists) { playlist ->
                FilterChip(
                    selected = selectedPlaylistFilter == playlist,
                    onClick = {
                        selectedPlaylistFilter = if (selectedPlaylistFilter == playlist) null else playlist
                    },
                    label = { Text(playlist) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredTracks.size} Tracks Available",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            TextButton(
                onClick = {
                    if (filteredTracks.isNotEmpty()) {
                        val songs = filteredTracks.map { it.toSong() }
                        playbackManager.setPlaylistAndPlay(songs, 0)
                        onDismiss()
                    }
                }
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Play All")
            }
        }

        if (filteredTracks.isEmpty()) {
            EmptyListState("No streaming tracks found")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(filteredTracks.size) { index ->
                    val track = filteredTracks[index]
                    StreamingTrackRow(
                        track = track,
                        onPlay = {
                            val songs = filteredTracks.map { it.toSong() }
                            playbackManager.setPlaylistAndPlay(songs, index)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StreamingTrackRow(
    track: StreamingTrack,
    onPlay: () -> Unit
) {
    val service = remember(track.serviceId) { StreamingServiceId.fromId(track.serviceId) }

    ListItem(
        headlineContent = {
            Text(
                text = track.title,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = "${track.artist} • ${track.album} • ${track.genre}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (service != null) Color(service.brandColorHex).copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = if (service != null) Color(service.brandColorHex) else MaterialTheme.colorScheme.primary
                )
            }
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatDuration(track.durationMs),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onPlay) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Track",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        modifier = Modifier.clickable { onPlay() }
    )
}
*/

@Composable
private fun GenreItemRow(
    genre: String,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(genre, fontWeight = FontWeight.Medium) },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onAddToQueue) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Genre to Queue",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onPlay) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Genre",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
private fun ArtistItemRow(
    artist: String,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(artist, fontWeight = FontWeight.SemiBold) },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = artist.take(1).uppercase(),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onAddToQueue) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Artist to Queue",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onPlay) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Artist",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
private fun AlbumItemRow(
    album: AlbumInfo,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = album.name,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = "${album.artist} • ${album.songCount} songs",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            AlbumArtImage(
                artworkUri = album.artworkUri,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onAddToQueue) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Album to Queue",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onPlay) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Album",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
private fun SongItemRow(
    song: Song,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = song.title,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = "${song.artist} • ${song.album}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            AlbumArtImage(
                song = song,
                artworkUri = song.artworkUri,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = formatDuration(song.durationMs),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onAddToQueue) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Song to Queue",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onPlay) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Song",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
private fun FolderItemRow(
    folder: String,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = folder.ifEmpty { "Root Folder" },
                fontWeight = FontWeight.Medium
            )
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onAddToQueue) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Folder to Queue",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onPlay) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Folder",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
fun AlbumArtImage(
    song: Song? = null,
    artworkUri: Uri? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(song?.id, artworkUri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(song?.id, artworkUri) {
        if (song != null) {
            val bmp = loadSongArtwork(context, song)
            bitmap = bmp?.asImageBitmap()
        } else if (artworkUri != null) {
            withContext(Dispatchers.IO) {
                try {
                    val bmp = if (artworkUri.scheme == "file") {
                        BitmapFactory.decodeFile(artworkUri.path)
                    } else {
                        context.contentResolver.openInputStream(artworkUri)?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    }
                    bitmap = bmp?.asImageBitmap()
                } catch (e: Exception) {
                    bitmap = null
                }
            }
        } else {
            bitmap = null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = "Album Art",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyListState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 16.sp
        )
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
