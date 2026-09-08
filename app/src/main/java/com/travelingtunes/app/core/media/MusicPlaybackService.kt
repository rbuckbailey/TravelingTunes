package com.travelingtunes.app.core.media

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.travelingtunes.app.core.database.MusicDatabase
import com.travelingtunes.app.core.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MusicPlaybackService : MediaLibraryService() {

    companion object {
        @Volatile
        private var sharedPlayer: ExoPlayer? = null

        @Volatile
        private var sharedSession: MediaLibrarySession? = null

        fun getOrCreatePlayer(context: Context): ExoPlayer {
            return sharedPlayer ?: synchronized(this) {
                sharedPlayer ?: ExoPlayer.Builder(context.applicationContext)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                            .setUsage(C.USAGE_MEDIA)
                            .build(),
                        true
                    )
                    .setHandleAudioBecomingNoisy(true)
                    .build().also {
                        sharedPlayer = it
                    }
            }
        }

        fun startService(context: Context) {
            val intent = Intent(context.applicationContext, MusicPlaybackService::class.java)
            try {
                ContextCompat.startForegroundService(context.applicationContext, intent)
            } catch (e: Exception) {
                try {
                    context.applicationContext.startService(intent)
                } catch (ignored: Exception) {}
            }
        }
    }

    private lateinit var musicDatabase: MusicDatabase
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        musicDatabase = MusicDatabase(applicationContext)

        val player = getOrCreatePlayer(applicationContext)

        val sessionCallback = AutoLibrarySessionCallback()
        val session = MediaLibrarySession.Builder(this, player, sessionCallback).build()
        sharedSession = session
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return sharedSession
    }

    override fun onDestroy() {
        sharedSession?.run {
            player.release()
            release()
            sharedSession = null
        }
        sharedPlayer = null
        musicDatabase.close()
        super.onDestroy()
    }

    private inner class AutoLibrarySessionCallback : MediaLibrarySession.Callback {

        private val rootItem = MediaItem.Builder()
            .setMediaId("root")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Traveling Tunes")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

        private val categorySongs = MediaItem.Builder()
            .setMediaId("category_songs")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Songs")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

        private val categoryAlbums = MediaItem.Builder()
            .setMediaId("category_albums")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Albums")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

        private val categoryArtists = MediaItem.Builder()
            .setMediaId("category_artists")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Artists")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

            serviceScope.launch {
                val items = mutableListOf<MediaItem>()
                when (parentId) {
                    "root" -> {
                        items.add(categorySongs)
                        items.add(categoryAlbums)
                        items.add(categoryArtists)
                    }
                    "category_songs" -> {
                        val songs = musicDatabase.getAllSongs()
                        items.addAll(songs.map { songToMediaItem(it) })
                    }
                    "category_albums" -> {
                        val albums = musicDatabase.getAlbums()
                        items.addAll(albums.map { album ->
                            MediaItem.Builder()
                                .setMediaId("album_${album.name}")
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(album.name)
                                        .setArtist(album.artist)
                                        .setArtworkUri(album.artworkUri)
                                        .setIsBrowsable(true)
                                        .setIsPlayable(false)
                                        .build()
                                )
                                .build()
                        })
                    }
                    "category_artists" -> {
                        val artists = musicDatabase.getArtists()
                        items.addAll(artists.map { artist ->
                            MediaItem.Builder()
                                .setMediaId("artist_$artist")
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(artist)
                                        .setIsBrowsable(true)
                                        .setIsPlayable(false)
                                        .build()
                                )
                                .build()
                        })
                    }
                    else -> {
                        if (parentId.startsWith("album_")) {
                            val albumName = parentId.removePrefix("album_")
                            val songs = musicDatabase.getSongsByAlbum(albumName)
                            items.addAll(songs.map { songToMediaItem(it) })
                        } else if (parentId.startsWith("artist_")) {
                            val artistName = parentId.removePrefix("artist_")
                            val songs = musicDatabase.getSongsByArtist(artistName)
                            items.addAll(songs.map { songToMediaItem(it) })
                        }
                    }
                }

                future.set(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
            }

            return future
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val future = SettableFuture.create<MutableList<MediaItem>>()

            serviceScope.launch {
                val resolvedItems = mutableListOf<MediaItem>()
                val allSongs = musicDatabase.getAllSongs()
                val songMap = allSongs.associateBy { it.id.toString() }

                for (item in mediaItems) {
                    val song = songMap[item.mediaId]
                    if (song != null) {
                        resolvedItems.add(songToMediaItem(song))
                    } else if (item.mediaId.startsWith("album_")) {
                        val albumName = item.mediaId.removePrefix("album_")
                        val albumSongs = musicDatabase.getSongsByAlbum(albumName)
                        resolvedItems.addAll(albumSongs.map { songToMediaItem(it) })
                    } else if (item.mediaId.startsWith("artist_")) {
                        val artistName = item.mediaId.removePrefix("artist_")
                        val artistSongs = musicDatabase.getSongsByArtist(artistName)
                        resolvedItems.addAll(artistSongs.map { songToMediaItem(it) })
                    } else {
                        resolvedItems.add(item)
                    }
                }

                future.set(resolvedItems)
            }

            return future
        }
    }
}

fun songToMediaItem(song: Song): MediaItem {
    val artUri = song.artworkUri ?: song.contentUri
    val metadata = MediaMetadata.Builder()
        .setTitle(song.title)
        .setArtist(song.artist)
        .setAlbumTitle(song.album)
        .setArtworkUri(artUri)
        .setIsPlayable(true)
        .setIsBrowsable(false)
        .build()

    return MediaItem.Builder()
        .setMediaId(song.id.toString())
        .setUri(song.contentUri)
        .setMediaMetadata(metadata)
        .build()
}
