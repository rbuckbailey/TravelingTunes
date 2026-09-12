package com.travelingtunes.app.core.media

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.travelingtunes.app.MainActivity
import com.travelingtunes.app.R
import com.travelingtunes.app.core.database.MusicDatabase
import com.travelingtunes.app.core.datastore.SettingsDataStore
import com.travelingtunes.app.core.model.GestureAction
import com.travelingtunes.app.core.model.GestureBinding
import com.travelingtunes.app.core.model.GestureTrigger
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
                } catch (_: Exception) {}
            }
        }
    }

    private lateinit var musicDatabase: MusicDatabase
    private lateinit var mediaStoreRepository: MediaStoreRepository
    private lateinit var settingsDataStore: SettingsDataStore
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        musicDatabase = MusicDatabase(applicationContext)
        mediaStoreRepository = MediaStoreRepository(applicationContext)
        settingsDataStore = SettingsDataStore(applicationContext)

        val player = getOrCreatePlayer(applicationContext)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val sessionCallback = AutoLibrarySessionCallback()
        val session = MediaLibrarySession.Builder(this, player, sessionCallback)
            .setSessionActivity(pendingIntent)
            .build()
        addSession(session)
        sharedSession = session

        serviceScope.launch {
            settingsDataStore.gestureBindingsFlow.collect { bindings ->
                updateCustomLayout(session, bindings)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return sharedSession
    }

    override fun onDestroy() {
        sharedSession?.let { session ->
            removeSession(session)
            session.player.release()
            session.release()
            sharedSession = null
        }
        sharedPlayer = null
        musicDatabase.close()
        super.onDestroy()
    }

    private suspend fun getAllSongsHelper(): List<Song> {
        val dbSongs = musicDatabase.getAllSongs()
        return dbSongs.ifEmpty { mediaStoreRepository.getAllSongs() }
    }

    private fun actionToSessionCommand(action: GestureAction): SessionCommand? {
        val actionString = when (action) {
            GestureAction.PLAY_CURRENT_ALBUM -> "com.travelingtunes.app.ACTION_PLAY_CURRENT_ALBUM"
            GestureAction.PLAY_CURRENT_ARTIST -> "com.travelingtunes.app.ACTION_PLAY_CURRENT_ARTIST"
            GestureAction.SHUFFLE_ALL_SONGS -> "com.travelingtunes.app.ACTION_SHUFFLE_ALL_SONGS"
            GestureAction.TOGGLE_REPEAT -> "com.travelingtunes.app.ACTION_TOGGLE_REPEAT"
            GestureAction.TOGGLE_SHUFFLE -> "com.travelingtunes.app.ACTION_TOGGLE_SHUFFLE"
            GestureAction.PLAY_PAUSE -> "com.travelingtunes.app.ACTION_PLAY_PAUSE"
            GestureAction.PLAY -> "com.travelingtunes.app.ACTION_PLAY"
            GestureAction.PAUSE -> "com.travelingtunes.app.ACTION_PAUSE"
            GestureAction.NEXT -> "com.travelingtunes.app.ACTION_NEXT"
            GestureAction.PREVIOUS -> "com.travelingtunes.app.ACTION_PREVIOUS"
            GestureAction.FAST_FORWARD -> "com.travelingtunes.app.ACTION_FAST_FORWARD"
            GestureAction.REWIND -> "com.travelingtunes.app.ACTION_REWIND"
            GestureAction.SONG_PICKER -> "com.travelingtunes.app.ACTION_SONG_PICKER"
            GestureAction.SELECT_ALBUM_VIEW -> "com.travelingtunes.app.ACTION_SELECT_ALBUM_VIEW"
            GestureAction.SELECT_ARTIST_VIEW -> "com.travelingtunes.app.ACTION_SELECT_ARTIST_VIEW"
            GestureAction.SHOW_QUEUE -> "com.travelingtunes.app.ACTION_SHOW_QUEUE"
            GestureAction.MENU -> "com.travelingtunes.app.ACTION_MENU"
            else -> return null
        }
        return SessionCommand(actionString, Bundle.EMPTY)
    }

    private fun actionToIconRes(action: GestureAction): Int {
        return when (action) {
            GestureAction.PLAY_CURRENT_ALBUM -> R.drawable.ic_play_current_album
            GestureAction.PLAY_CURRENT_ARTIST -> R.drawable.ic_play_current_artist
            GestureAction.SHUFFLE_ALL_SONGS -> R.drawable.ic_toggle_shuffle
            GestureAction.TOGGLE_REPEAT -> R.drawable.ic_toggle_repeat
            GestureAction.TOGGLE_SHUFFLE -> R.drawable.ic_toggle_shuffle
            GestureAction.PLAY_PAUSE, GestureAction.PLAY, GestureAction.PAUSE -> R.drawable.ic_play_pause
            GestureAction.NEXT -> R.drawable.ic_next_song
            GestureAction.PREVIOUS -> R.drawable.ic_previous_song
            GestureAction.FAST_FORWARD -> R.drawable.ic_fast_forward
            GestureAction.REWIND -> R.drawable.ic_rewind
            GestureAction.SONG_PICKER -> R.drawable.ic_song_picker
            GestureAction.SELECT_ALBUM_VIEW -> R.drawable.ic_play_current_album
            GestureAction.SELECT_ARTIST_VIEW -> R.drawable.ic_play_current_artist
            GestureAction.MENU -> R.drawable.ic_menu_settings
            else -> R.drawable.ic_play_pause
        }
    }

    private fun actionToCommandButton(action: GestureAction): CommandButton? {
        val command = actionToSessionCommand(action) ?: return null
        val iconRes = actionToIconRes(action)
        return CommandButton.Builder()
            .setSessionCommand(command)
            .setDisplayName(action.displayName)
            .setIconResId(iconRes)
            .setEnabled(true)
            .build()
    }

    private fun updateCustomLayout(
        session: MediaLibrarySession,
        bindings: Map<GestureTrigger, GestureBinding>
    ) {
        val regionTriggers = GestureTrigger.TOP_REGION_SLOTS + GestureTrigger.BOTTOM_REGION_SLOTS

        val assignedActions = mutableSetOf<GestureAction>()
        val buttons = mutableListOf<CommandButton>()

        // 1. Add assigned screen region actions (e.g. Top-Center -> Play Current Album)
        for (trigger in regionTriggers) {
            val action = bindings[trigger]?.action ?: GestureAction.fromKey(trigger.defaultActionKey)
            if (action != GestureAction.UNASSIGNED && action !in assignedActions) {
                actionToCommandButton(action)?.let {
                    buttons.add(it)
                    assignedActions.add(action)
                }
            }
        }

        // 2. High priority mode/playback actions (Repeat, Shuffle, Play/Pause) ahead of Next/Previous
        val priorityActions = listOf(
            GestureAction.TOGGLE_REPEAT,
            GestureAction.TOGGLE_SHUFFLE,
            GestureAction.PLAY_PAUSE
        )

        for (action in priorityActions) {
            val alreadyHasAction = assignedActions.any {
                it == action ||
                (action == GestureAction.PLAY_PAUSE && (it == GestureAction.PLAY || it == GestureAction.PAUSE))
            }
            if (!alreadyHasAction) {
                actionToCommandButton(action)?.let {
                    buttons.add(it)
                    assignedActions.add(action)
                }
            }
        }

        // 3. Secondary navigation actions (Next, Previous)
        val secondaryActions = listOf(
            GestureAction.NEXT,
            GestureAction.PREVIOUS
        )

        for (action in secondaryActions) {
            if (action !in assignedActions) {
                actionToCommandButton(action)?.let {
                    buttons.add(it)
                    assignedActions.add(action)
                }
            }
        }

        val customLayout = ImmutableList.copyOf(buttons)
        session.setCustomLayout(customLayout)
    }

    private fun playCurrentAlbum() {
        val player = sharedPlayer ?: return
        val currentItem = player.currentMediaItem ?: return
        val albumName = currentItem.mediaMetadata.albumTitle?.toString() ?: ""

        serviceScope.launch {
            val allSongs = getAllSongsHelper()
            val albumSongs = if (albumName.isNotBlank()) {
                allSongs.filter { it.album.equals(albumName, ignoreCase = true) }
            } else emptyList()

            val finalQueue = albumSongs.ifEmpty { listOfNotNull(allSongs.find { it.id.toString() == currentItem.mediaId }) }
            if (finalQueue.isNotEmpty()) {
                val mediaItems = finalQueue.map { songToMediaItem(it) }
                launch(Dispatchers.Main) {
                    player.setMediaItems(mediaItems, 0, 0L)
                    player.repeatMode = Player.REPEAT_MODE_ALL
                    player.prepare()
                    player.play()
                }
            }
        }
    }

    private fun playCurrentArtist() {
        val player = sharedPlayer ?: return
        val currentItem = player.currentMediaItem ?: return
        val artistName = currentItem.mediaMetadata.artist?.toString() ?: ""

        serviceScope.launch {
            val allSongs = getAllSongsHelper()
            val artistSongs = if (artistName.isNotBlank()) {
                allSongs.filter { it.artist.equals(artistName, ignoreCase = true) }
            } else emptyList()

            val finalQueue = artistSongs.ifEmpty { listOfNotNull(allSongs.find { it.id.toString() == currentItem.mediaId }) }
            if (finalQueue.isNotEmpty()) {
                val mediaItems = finalQueue.map { songToMediaItem(it) }
                launch(Dispatchers.Main) {
                    player.setMediaItems(mediaItems, 0, 0L)
                    player.repeatMode = Player.REPEAT_MODE_ALL
                    player.prepare()
                    player.play()
                }
            }
        }
    }

    private fun shuffleAllSongs() {
        val player = sharedPlayer ?: return
        serviceScope.launch {
            val allSongs = getAllSongsHelper()
            if (allSongs.isNotEmpty()) {
                val mediaItems = allSongs.map { songToMediaItem(it) }
                val randomIndex = if (allSongs.size > 1) allSongs.indices.random() else 0
                launch(Dispatchers.Main) {
                    player.setMediaItems(mediaItems, randomIndex, 0L)
                    player.repeatMode = Player.REPEAT_MODE_OFF
                    player.shuffleModeEnabled = true
                    player.prepare()
                    player.play()
                }
            }
        }
    }

    @OptIn(UnstableApi::class)
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
                    .setExtras(Bundle().apply {
                        putInt(
                            MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                            MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
                        )
                    })
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
                    .setExtras(Bundle().apply {
                        putInt(
                            MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                            MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
                        )
                    })
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
                    .setExtras(Bundle().apply {
                        putInt(
                            MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                            MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
                        )
                    })
                    .build()
            )
            .build()

        private val categoryGenres = MediaItem.Builder()
            .setMediaId("category_genres")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Genres")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setExtras(Bundle().apply {
                        putInt(
                            MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                            MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
                        )
                    })
                    .build()
            )
            .build()

        private val categoryFolders = MediaItem.Builder()
            .setMediaId("category_folders")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Folders")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setExtras(Bundle().apply {
                        putInt(
                            MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                            MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
                        )
                    })
                    .build()
            )
            .build()

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            val availableCommands = connectionResult.availableSessionCommands.buildUpon()

            val allActions = listOf(
                GestureAction.PLAY_CURRENT_ALBUM,
                GestureAction.PLAY_CURRENT_ARTIST,
                GestureAction.SHUFFLE_ALL_SONGS,
                GestureAction.TOGGLE_REPEAT,
                GestureAction.TOGGLE_SHUFFLE,
                GestureAction.PLAY_PAUSE,
                GestureAction.PLAY,
                GestureAction.PAUSE,
                GestureAction.NEXT,
                GestureAction.PREVIOUS,
                GestureAction.FAST_FORWARD,
                GestureAction.REWIND,
                GestureAction.SONG_PICKER,
                GestureAction.SELECT_ALBUM_VIEW,
                GestureAction.SELECT_ARTIST_VIEW,
                GestureAction.SHOW_QUEUE,
                GestureAction.MENU
            )

            for (act in allActions) {
                actionToSessionCommand(act)?.let { availableCommands.add(it) }
            }

            return MediaSession.ConnectionResult.accept(
                availableCommands.build(),
                connectionResult.availablePlayerCommands
            )
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                "com.travelingtunes.app.ACTION_PLAY_CURRENT_ALBUM" -> {
                    playCurrentAlbum()
                }
                "com.travelingtunes.app.ACTION_PLAY_CURRENT_ARTIST" -> {
                    playCurrentArtist()
                }
                "com.travelingtunes.app.ACTION_SHUFFLE_ALL_SONGS" -> {
                    shuffleAllSongs()
                }
                "com.travelingtunes.app.ACTION_TOGGLE_REPEAT" -> {
                    val player = session.player
                    player.repeatMode = when (player.repeatMode) {
                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                        Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                        else -> Player.REPEAT_MODE_OFF
                    }
                }
                "com.travelingtunes.app.ACTION_TOGGLE_SHUFFLE" -> {
                    val player = session.player
                    player.shuffleModeEnabled = !player.shuffleModeEnabled
                }
                "com.travelingtunes.app.ACTION_PLAY_PAUSE", "com.travelingtunes.app.ACTION_PLAY", "com.travelingtunes.app.ACTION_PAUSE" -> {
                    val player = session.player
                    if (player.isPlaying) player.pause() else player.play()
                }
                "com.travelingtunes.app.ACTION_NEXT" -> {
                    val player = session.player
                    if (player.hasNextMediaItem()) player.seekToNextMediaItem()
                }
                "com.travelingtunes.app.ACTION_PREVIOUS" -> {
                    val player = session.player
                    if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem()
                }
                "com.travelingtunes.app.ACTION_FAST_FORWARD" -> {
                    val player = session.player
                    val target = (player.currentPosition + 10000L).coerceAtMost(player.duration)
                    player.seekTo(target)
                }
                "com.travelingtunes.app.ACTION_REWIND" -> {
                    val player = session.player
                    val target = (player.currentPosition - 10000L).coerceAtLeast(0L)
                    player.seekTo(target)
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootParams = LibraryParams.Builder()
                .setExtras(Bundle().apply {
                    putBoolean("android.media.browse.SEARCH_SUPPORTED", true)
                    putInt(
                        MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                        MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
                    )
                    putInt(
                        MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE,
                        MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
                    )
                })
                .build()

            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, rootParams))
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
                        items.add(categoryGenres)
                        items.add(categoryFolders)
                    }
                    "category_songs" -> {
                        val songs = getAllSongsHelper()
                        items.addAll(songs.map { songToMediaItem(it) })
                    }
                    "category_albums" -> {
                        val dbAlbums = musicDatabase.getAlbums()
                        val albums = dbAlbums.ifEmpty {
                            getAllSongsHelper().groupBy { it.album }.map { (albumName, albumSongs) ->
                                com.travelingtunes.app.core.database.AlbumInfo(
                                    name = albumName,
                                    artist = albumSongs.firstOrNull()?.artist ?: "Unknown Artist",
                                    songCount = albumSongs.size,
                                    artworkUri = albumSongs.firstOrNull()?.artworkUri
                                )
                            }
                        }
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
                                        .setExtras(Bundle().apply {
                                            putInt(
                                                MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                                                MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
                                            )
                                        })
                                        .build()
                                )
                                .build()
                        })
                    }
                    "category_artists" -> {
                        val dbArtists = musicDatabase.getArtists()
                        val artists = dbArtists.ifEmpty {
                            getAllSongsHelper().map { it.artist }.distinct().sorted()
                        }
                        items.addAll(artists.map { artist ->
                            MediaItem.Builder()
                                .setMediaId("artist_$artist")
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(artist)
                                        .setIsBrowsable(true)
                                        .setIsPlayable(false)
                                        .setExtras(Bundle().apply {
                                            putInt(
                                                MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                                                MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
                                            )
                                        })
                                        .build()
                                )
                                .build()
                        })
                    }
                    "category_genres" -> {
                        val dbGenres = musicDatabase.getGenres()
                        val genres = dbGenres.ifEmpty {
                            getAllSongsHelper().map { it.genre }.distinct().sorted()
                        }
                        items.addAll(genres.map { genre ->
                            MediaItem.Builder()
                                .setMediaId("genre_$genre")
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(genre)
                                        .setIsBrowsable(true)
                                        .setIsPlayable(false)
                                        .setExtras(Bundle().apply {
                                            putInt(
                                                MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                                                MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
                                            )
                                        })
                                        .build()
                                )
                                .build()
                        })
                    }
                    "category_folders" -> {
                        val dbFolders = musicDatabase.getFolders()
                        val folders = dbFolders.ifEmpty {
                            getAllSongsHelper().map { it.folderPath }.filter { it.isNotBlank() }.distinct().sorted()
                        }
                        items.addAll(folders.map { folder ->
                            MediaItem.Builder()
                                .setMediaId("folder_$folder")
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(folder)
                                        .setIsBrowsable(true)
                                        .setIsPlayable(false)
                                        .setExtras(Bundle().apply {
                                            putInt(
                                                MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                                                MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
                                            )
                                        })
                                        .build()
                                )
                                .build()
                        })
                    }
                    else -> {
                        val songs = when {
                            parentId.startsWith("album_") -> {
                                val albumName = parentId.removePrefix("album_")
                                val dbResult = musicDatabase.getSongsByAlbum(albumName)
                                dbResult.ifEmpty { getAllSongsHelper().filter { it.album.equals(albumName, ignoreCase = true) } }
                            }
                            parentId.startsWith("artist_") -> {
                                val artistName = parentId.removePrefix("artist_")
                                val dbResult = musicDatabase.getSongsByArtist(artistName)
                                dbResult.ifEmpty { getAllSongsHelper().filter { it.artist.equals(artistName, ignoreCase = true) } }
                            }
                            parentId.startsWith("genre_") -> {
                                val genreName = parentId.removePrefix("genre_")
                                val dbResult = musicDatabase.getSongsByGenre(genreName)
                                dbResult.ifEmpty { getAllSongsHelper().filter { it.genre.equals(genreName, ignoreCase = true) } }
                            }
                            parentId.startsWith("folder_") -> {
                                val folderPath = parentId.removePrefix("folder_")
                                val dbResult = musicDatabase.getSongsByFolder(folderPath)
                                dbResult.ifEmpty { getAllSongsHelper().filter { it.folderPath.equals(folderPath, ignoreCase = true) } }
                            }
                            else -> emptyList()
                        }
                        items.addAll(songs.map { songToMediaItem(it) })
                    }
                }

                future.set(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
            }

            return future
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val future = SettableFuture.create<LibraryResult<MediaItem>>()

            serviceScope.launch {
                val songId = mediaId.toLongOrNull()
                if (songId != null) {
                    val allSongs = getAllSongsHelper()
                    val song = allSongs.find { it.id == songId }
                    if (song != null) {
                        future.set(LibraryResult.ofItem(songToMediaItem(song), null))
                        return@launch
                    }
                }
                future.set(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
            }

            return future
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<Void>> {
            serviceScope.launch {
                val dbResults = musicDatabase.searchSongs(query)
                val results = dbResults.ifEmpty {
                    val all = mediaStoreRepository.getAllSongs()
                    all.filter {
                        it.title.contains(query, ignoreCase = true) ||
                        it.artist.contains(query, ignoreCase = true) ||
                        it.album.contains(query, ignoreCase = true)
                    }
                }
                session.notifySearchResultChanged(browser, query, results.size, params)
            }
            return Futures.immediateFuture(LibraryResult.ofVoid())
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

            serviceScope.launch {
                val dbResults = musicDatabase.searchSongs(query)
                val results = dbResults.ifEmpty {
                    val all = mediaStoreRepository.getAllSongs()
                    all.filter {
                        it.title.contains(query, ignoreCase = true) ||
                        it.artist.contains(query, ignoreCase = true) ||
                        it.album.contains(query, ignoreCase = true)
                    }
                }

                val mediaItems = results.map { songToMediaItem(it) }
                val fromIndex = (page * pageSize).coerceIn(0, mediaItems.size)
                val toIndex = (fromIndex + pageSize).coerceIn(fromIndex, mediaItems.size)
                val pagedList = mediaItems.subList(fromIndex, toIndex)

                future.set(LibraryResult.ofItemList(ImmutableList.copyOf(pagedList), params))
            }

            return future
        }

        @OptIn(UnstableApi::class)
        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()

            serviceScope.launch {
                val allSongs = getAllSongsHelper()
                val songMap = allSongs.associateBy { it.id.toString() }

                val resolvedItems = mutableListOf<MediaItem>()
                var playStartIndex = startIndex

                if (mediaItems.size == 1) {
                    val requestedId = mediaItems[0].mediaId
                    val matchedSong = songMap[requestedId]

                    if (matchedSong != null) {
                        val albumSongs = allSongs.filter { it.album.equals(matchedSong.album, ignoreCase = true) }
                        val queueSongs = if (albumSongs.size > 1) albumSongs else allSongs

                        val songIndex = queueSongs.indexOfFirst { it.id == matchedSong.id }
                        if (songIndex >= 0) {
                            resolvedItems.addAll(queueSongs.map { songToMediaItem(it) })
                            playStartIndex = songIndex
                        } else {
                            resolvedItems.add(songToMediaItem(matchedSong))
                            playStartIndex = 0
                        }
                    } else if (requestedId.startsWith("album_")) {
                        val albumName = requestedId.removePrefix("album_")
                        val songs = allSongs.filter { it.album.equals(albumName, ignoreCase = true) }
                        resolvedItems.addAll(songs.map { songToMediaItem(it) })
                        playStartIndex = 0
                    } else if (requestedId.startsWith("artist_")) {
                        val artistName = requestedId.removePrefix("artist_")
                        val songs = allSongs.filter { it.artist.equals(artistName, ignoreCase = true) }
                        resolvedItems.addAll(songs.map { songToMediaItem(it) })
                        playStartIndex = 0
                    } else if (requestedId.startsWith("genre_")) {
                        val genreName = requestedId.removePrefix("genre_")
                        val songs = allSongs.filter { it.genre.equals(genreName, ignoreCase = true) }
                        resolvedItems.addAll(songs.map { songToMediaItem(it) })
                        playStartIndex = 0
                    } else if (requestedId.startsWith("folder_")) {
                        val folderPath = requestedId.removePrefix("folder_")
                        val songs = allSongs.filter { it.folderPath.equals(folderPath, ignoreCase = true) }
                        resolvedItems.addAll(songs.map { songToMediaItem(it) })
                        playStartIndex = 0
                    } else {
                        resolvedItems.addAll(mediaItems)
                    }
                } else {
                    for (item in mediaItems) {
                        val song = songMap[item.mediaId]
                        if (song != null) {
                            resolvedItems.add(songToMediaItem(song))
                        } else {
                            resolvedItems.add(item)
                        }
                    }
                }

                val safeStartIndex = playStartIndex.coerceIn(0, (resolvedItems.size - 1).coerceAtLeast(0))
                future.set(MediaSession.MediaItemsWithStartPosition(resolvedItems, safeStartIndex, startPositionMs))
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
                val allSongs = getAllSongsHelper()
                val songMap = allSongs.associateBy { it.id.toString() }

                val resolvedItems = mutableListOf<MediaItem>()
                for (item in mediaItems) {
                    val song = songMap[item.mediaId]
                    if (song != null) {
                        resolvedItems.add(songToMediaItem(song))
                    } else if (item.mediaId.startsWith("album_")) {
                        val albumName = item.mediaId.removePrefix("album_")
                        val albumSongs = allSongs.filter { it.album.equals(albumName, ignoreCase = true) }
                        resolvedItems.addAll(albumSongs.map { songToMediaItem(it) })
                    } else if (item.mediaId.startsWith("artist_")) {
                        val artistName = item.mediaId.removePrefix("artist_")
                        val artistSongs = allSongs.filter { it.artist.equals(artistName, ignoreCase = true) }
                        resolvedItems.addAll(artistSongs.map { songToMediaItem(it) })
                    } else if (item.mediaId.startsWith("genre_")) {
                        val genreName = item.mediaId.removePrefix("genre_")
                        val genreSongs = allSongs.filter { it.genre.equals(genreName, ignoreCase = true) }
                        resolvedItems.addAll(genreSongs.map { songToMediaItem(it) })
                    } else if (item.mediaId.startsWith("folder_")) {
                        val folderPath = item.mediaId.removePrefix("folder_")
                        val folderSongs = allSongs.filter { it.folderPath.equals(folderPath, ignoreCase = true) }
                        resolvedItems.addAll(folderSongs.map { songToMediaItem(it) })
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
        .setGenre(song.genre)
        .setTrackNumber(song.trackNumber)
        .setArtworkUri(artUri)
        .setIsPlayable(true)
        .setIsBrowsable(false)
        .setExtras(Bundle().apply {
            putString("folder_path", song.folderPath)
        })
        .build()

    return MediaItem.Builder()
        .setMediaId(song.id.toString())
        .setUri(song.contentUri)
        .setMediaMetadata(metadata)
        .build()
}
