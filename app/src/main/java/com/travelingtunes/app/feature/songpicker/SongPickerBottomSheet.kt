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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import com.travelingtunes.app.core.media.MusicScanner
import com.travelingtunes.app.feature.player.loadSongArtwork
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
import com.travelingtunes.app.core.media.PlaybackManager
import com.travelingtunes.app.core.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PickerCategory(val displayName: String) {
    ALL("All"),
    SONGS("Songs"),
    ALBUMS("Albums"),
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
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    val isScanning by musicScanner?.isScanning?.collectAsState() ?: remember { mutableStateOf(false) }
    val scanStatusMessage by musicScanner?.statusMessage?.collectAsState() ?: remember { mutableStateOf(null) }
    val scannedCount by musicScanner?.scannedCount?.collectAsState() ?: remember { mutableStateOf(0) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(PickerCategory.ALL) }

    var songsList by remember { mutableStateOf<List<Song>>(emptyList()) }
    var albumsList by remember { mutableStateOf<List<AlbumInfo>>(emptyList()) }
    var artistsList by remember { mutableStateOf<List<String>>(emptyList()) }
    var genresList by remember { mutableStateOf<List<String>>(emptyList()) }
    var foldersList by remember { mutableStateOf<List<String>>(emptyList()) }

    val currentPlaylist by playbackManager.currentPlaylist.collectAsState()

    // Query database when search or category changes or playlist updates
    LaunchedEffect(searchQuery, selectedCategory, currentPlaylist) {
        withContext(Dispatchers.IO) {
            // Ensure database is populated if empty but playlist exists
            if (musicDatabase.getAllSongs().isEmpty() && currentPlaylist.isNotEmpty()) {
                musicDatabase.insertOrReplaceSongs(currentPlaylist)
            }

            when (selectedCategory) {
                PickerCategory.ALL, PickerCategory.SONGS -> {
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search title, artist, album, genre...") },
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

            Spacer(modifier = Modifier.height(8.dp))

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(PickerCategory.entries.toTypedArray()) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category.displayName) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Content Area according to selected category
            when (selectedCategory) {
                PickerCategory.ALL, PickerCategory.SONGS -> {
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
                                    onClick = {
                                        playbackManager.setPlaylistAndPlay(songsList, index)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                }
                PickerCategory.ALBUMS -> {
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
                                    onClick = {
                                        coroutineScope.launch {
                                            val albumSongs = musicDatabase.getSongsByAlbum(album.name)
                                            val playSongs = if (albumSongs.isNotEmpty()) albumSongs else currentPlaylist.filter { it.album == album.name }
                                            if (playSongs.isNotEmpty()) {
                                                playbackManager.setPlaylistAndPlay(playSongs, 0)
                                            }
                                            onDismiss()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                PickerCategory.ARTISTS -> {
                    if (artistsList.isEmpty()) {
                        EmptyListState("No artists found")
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        ) {
                            items(artistsList) { artist ->
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
                                    modifier = Modifier.clickable {
                                        coroutineScope.launch {
                                            val artistSongs = musicDatabase.getSongsByArtist(artist)
                                            val playSongs = if (artistSongs.isNotEmpty()) artistSongs else currentPlaylist.filter { it.artist == artist }
                                            if (playSongs.isNotEmpty()) {
                                                playbackManager.setPlaylistAndPlay(playSongs, 0)
                                            }
                                            onDismiss()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                PickerCategory.GENRES -> {
                    if (genresList.isEmpty()) {
                        EmptyListState("No genres found")
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        ) {
                            items(genresList) { genre ->
                                ListItem(
                                    headlineContent = { Text(genre, fontWeight = FontWeight.Medium) },
                                    modifier = Modifier.clickable {
                                        coroutineScope.launch {
                                            val genreSongs = musicDatabase.getSongsByGenre(genre)
                                            val playSongs = if (genreSongs.isNotEmpty()) genreSongs else currentPlaylist.filter { it.genre == genre }
                                            if (playSongs.isNotEmpty()) {
                                                playbackManager.setPlaylistAndPlay(playSongs, 0)
                                            }
                                            onDismiss()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                PickerCategory.FOLDERS -> {
                    if (foldersList.isEmpty()) {
                        EmptyListState("No subfolders found")
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        ) {
                            items(foldersList) { folder ->
                                ListItem(
                                    headlineContent = { Text(folder.ifEmpty { "Root Folder" }, fontWeight = FontWeight.Medium) },
                                    modifier = Modifier.clickable {
                                        coroutineScope.launch {
                                            val folderSongs = musicDatabase.getSongsByFolder(folder)
                                            val playSongs = if (folderSongs.isNotEmpty()) folderSongs else currentPlaylist.filter { it.folderPath == folder }
                                            if (playSongs.isNotEmpty()) {
                                                playbackManager.setPlaylistAndPlay(playSongs, 0)
                                            }
                                            onDismiss()
                                        }
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

@Composable
private fun SongItemRow(
    song: Song,
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
            Text(
                text = formatDuration(song.durationMs),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
private fun AlbumItemRow(
    album: AlbumInfo,
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
