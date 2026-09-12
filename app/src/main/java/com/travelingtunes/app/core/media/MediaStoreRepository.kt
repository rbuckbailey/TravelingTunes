package com.travelingtunes.app.core.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.travelingtunes.app.core.model.Playlist
import com.travelingtunes.app.core.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreRepository(private val context: Context) {

    suspend fun getAllSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val displayNameColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val rawTitle = cursor.getString(titleColumn)?.trim()
                val rawArtist = cursor.getString(artistColumn)?.trim()
                val rawAlbum = cursor.getString(albumColumn)?.trim()
                val albumId = cursor.getLong(albumIdColumn)
                val duration = cursor.getLong(durationColumn)

                val displayName = if (displayNameColumn != -1) cursor.getString(displayNameColumn)?.trim() else null
                val filePath = if (dataColumn != -1) cursor.getString(dataColumn)?.trim() else null

                val fileName = displayName ?: filePath?.substringAfterLast('/') ?: "Track $id"
                val cleanFileName = fileName.substringBeforeLast('.').ifBlank { fileName }

                val folderName = if (!filePath.isNullOrBlank() && filePath.contains('/')) {
                    val parentPath = filePath.substringBeforeLast('/')
                    parentPath.substringAfterLast('/').ifBlank { "Music" }
                } else {
                    "Music"
                }

                val title = if (!rawTitle.isNullOrBlank() && !rawTitle.equals("<unknown>", ignoreCase = true) && !rawTitle.equals("Unknown Title", ignoreCase = true) && !rawTitle.equals("Unknown", ignoreCase = true)) {
                    rawTitle
                } else {
                    cleanFileName
                }

                val album = if (!rawAlbum.isNullOrBlank() && !rawAlbum.equals("<unknown>", ignoreCase = true) && !rawAlbum.equals("Unknown Album", ignoreCase = true) && !rawAlbum.equals("Unknown", ignoreCase = true)) {
                    rawAlbum
                } else {
                    folderName
                }

                val artist = if (!rawArtist.isNullOrBlank() && !rawArtist.equals("<unknown>", ignoreCase = true) && !rawArtist.equals("Unknown Artist", ignoreCase = true) && !rawArtist.equals("Unknown", ignoreCase = true)) {
                    rawArtist
                } else {
                    "Unknown Artist"
                }

                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                val artworkUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)

                songs.add(
                    Song(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        albumId = albumId,
                        durationMs = duration,
                        contentUri = contentUri,
                        artworkUri = artworkUri,
                        folderPath = folderName,
                        fileName = fileName
                    )
                )
            }
        }
        songs.sortedWith(
            compareBy(
                String.CASE_INSENSITIVE_ORDER
            ) { song: Song -> song.artist }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { song -> song.album }
                .thenBy { song -> if (song.trackNumber > 0) song.trackNumber else Int.MAX_VALUE }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { song -> song.title }
        )
    }

    suspend fun getSongsByArtist(artistName: String): List<Song> {
        return getAllSongs().filter { it.artist.equals(artistName, ignoreCase = true) }
    }

    suspend fun getSongsByAlbum(albumName: String): List<Song> {
        return getAllSongs().filter { it.album.equals(albumName, ignoreCase = true) }
    }

    suspend fun getPlaylists(): List<Playlist> = withContext(Dispatchers.IO) {
        val playlists = mutableListOf<Playlist>()
        playlists.add(Playlist(1L, "All Songs, Shuffled"))
        playlists.add(Playlist(2L, "Default Traveling Tunes Playlist"))
        playlists
    }
}
