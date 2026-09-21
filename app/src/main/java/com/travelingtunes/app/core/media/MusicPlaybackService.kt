package com.travelingtunes.app.core.media

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
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
import com.travelingtunes.app.R
import com.travelingtunes.app.core.database.MusicDatabase
import com.travelingtunes.app.core.datastore.SettingsDataStore
import com.travelingtunes.app.core.model.AutoCategory
import com.travelingtunes.app.core.model.DisplaySettings
import com.travelingtunes.app.core.model.GestureAction
import com.travelingtunes.app.core.model.GestureBinding
import com.travelingtunes.app.core.model.GestureTrigger
import com.travelingtunes.app.core.model.RepeatMode
import com.travelingtunes.app.core.model.ShuffleMode
import com.travelingtunes.app.core.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicPlaybackService : MediaLibraryService() {

    companion object {
        @Volatile
        private var sharedPlayer: ExoPlayer? = null

        @Volatile
        private var sharedSession: MediaLibrarySession? = null

        fun isPlayerValid(player: ExoPlayer?): Boolean {
            if (player == null) return false
            return try {
                player.playbackState
                player.applicationLooper
                true
            } catch (e: Exception) {
                false
            }
        }

        fun getOrCreatePlayer(context: Context): ExoPlayer {
            val existing = sharedPlayer
            if (existing != null && isPlayerValid(existing)) {
                return existing
            }
            return recreatePlayer(context)
        }

        @OptIn(UnstableApi::class)
        @Synchronized
        fun recreatePlayer(context: Context): ExoPlayer {
            val oldPlayer = sharedPlayer
            if (oldPlayer != null) {
                try {
                    oldPlayer.release()
                } catch (e: Exception) {
                    android.util.Log.w("MusicPlaybackService", "Error releasing old sharedPlayer", e)
                }
                sharedPlayer = null
            }

            val newPlayer = ExoPlayer.Builder(context.applicationContext)
                .setLooper(android.os.Looper.getMainLooper())
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    true
                )
                .setHandleAudioBecomingNoisy(true)
                .build()

            sharedPlayer = newPlayer

            val session = sharedSession
            if (session != null) {
                try {
                    session.player = newPlayer
                } catch (e: Exception) {
                    android.util.Log.w("MusicPlaybackService", "Could not update session.player directly", e)
                }
            }

            android.util.Log.i("MusicPlaybackService", "Recreated shared ExoPlayer instance $newPlayer")
            return newPlayer
        }

        fun resetSharedPlayer() {
            val oldPlayer = sharedPlayer
            sharedPlayer = null
            sharedSession = null
            if (oldPlayer != null) {
                try {
                    oldPlayer.release()
                } catch (_: Exception) {}
            }
        }

        fun startService(context: Context) {
            val intent = Intent(context.applicationContext, MusicPlaybackService::class.java)
            try {
                context.applicationContext.startService(intent)
            } catch (e: Exception) {
                android.util.Log.w("MusicPlaybackService", "Failed to startService", e)
            }
        }
    }

    private lateinit var musicDatabase: MusicDatabase
    private lateinit var mediaStoreRepository: MediaStoreRepository
    private lateinit var settingsDataStore: SettingsDataStore
    private var autoDisplaySettings = DisplaySettings()
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        musicDatabase = MusicDatabase(applicationContext)
        mediaStoreRepository = MediaStoreRepository(applicationContext)
        settingsDataStore = SettingsDataStore(applicationContext)

        val player = getOrCreatePlayer(applicationContext)

        val sessionCallback = AutoLibrarySessionCallback()
        val session = MediaLibrarySession.Builder(this, player, sessionCallback)
            .build()
        addSession(session)
        sharedSession = session

        serviceScope.launch {
            val playbackManager = PlaybackManager.getInstance(applicationContext, settingsDataStore, musicDatabase)
            combine(
                playbackManager.repeatMode,
                playbackManager.shuffleMode,
                settingsDataStore.gestureBindingsFlow,
                settingsDataStore.displaySettingsFlow
            ) { repeatMode, shuffleMode, bindings, settings ->
                autoDisplaySettings = settings
                sharedSession?.let { session ->
                    updateCustomLayout(session, bindings, settings, repeatMode, shuffleMode)
                    notifyAutoChildrenChanged(session)
                }
            }.collect {}
        }

        serviceScope.launch {
            val playbackManager = PlaybackManager.getInstance(applicationContext, settingsDataStore, musicDatabase)
            combine(
                playbackManager.currentSong,
                playbackManager.currentPlaylist
            ) { _, _ ->
                sharedSession?.let { session ->
                    notifyAutoChildrenChanged(session)
                }
            }.collect {}
        }
    }

    private fun notifyAutoChildrenChanged(session: MediaLibrarySession) {
        listOf("root", "show_play_screen", "category_songs", "category_albums", "category_artists", "category_genres", "category_folders").forEach { parentId ->
            try {
                session.notifyChildrenChanged(parentId, 0, null)
            } catch (e: Exception) {
                android.util.Log.w("MusicPlaybackService", "Error notifying children changed for $parentId", e)
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
        android.util.Log.i("MusicPlaybackService", "onDestroy called")
        sharedSession?.let { session ->
            try {
                removeSession(session)
                session.release()
            } catch (e: Exception) {
                android.util.Log.e("MusicPlaybackService", "Error releasing session in onDestroy", e)
            }
            sharedSession = null
        }
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
            GestureAction.NEXT_ALBUM -> "com.travelingtunes.app.ACTION_NEXT_ALBUM"
            GestureAction.PREVIOUS_ALBUM -> "com.travelingtunes.app.ACTION_PREVIOUS_ALBUM"
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
            GestureAction.TOGGLE_DRIVING_MODE -> "com.travelingtunes.app.ACTION_TOGGLE_DRIVING_MODE"
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

    private fun actionToCommandButton(
        action: GestureAction,
        repeatMode: RepeatMode = RepeatMode.OFF,
        shuffleMode: ShuffleMode = ShuffleMode.OFF
    ): CommandButton? {
        val command = actionToSessionCommand(action) ?: return null
        val (displayName, iconRes) = when (action) {
            GestureAction.TOGGLE_REPEAT -> {
                repeatMode.displayName to when (repeatMode) {
                    RepeatMode.OFF -> R.drawable.ic_repeat_off
                    RepeatMode.SONG -> R.drawable.ic_repeat_song
                    RepeatMode.ALBUM -> R.drawable.ic_repeat_album
                    RepeatMode.ARTIST -> R.drawable.ic_repeat_artist
                    RepeatMode.GENRE -> R.drawable.ic_repeat_genre
                    RepeatMode.FOLDER -> R.drawable.ic_repeat_folder
                }
            }
            GestureAction.TOGGLE_SHUFFLE -> {
                shuffleMode.displayName to when (shuffleMode) {
                    ShuffleMode.OFF -> R.drawable.ic_shuffle_off
                    ShuffleMode.SONGS -> R.drawable.ic_shuffle_songs
                    ShuffleMode.ALBUMS -> R.drawable.ic_shuffle_albums
                }
            }
            else -> action.displayName to actionToIconRes(action)
        }
        return CommandButton.Builder()
            .setSessionCommand(command)
            .setDisplayName(displayName)
            .setIconResId(iconRes)
            .setEnabled(true)
            .build()
    }

    private fun updateCustomLayout(
        session: MediaLibrarySession,
        bindings: Map<GestureTrigger, GestureBinding>,
        autoSettings: DisplaySettings,
        repeatMode: RepeatMode = RepeatMode.OFF,
        shuffleMode: ShuffleMode = ShuffleMode.OFF
    ) {
        val buttons = mutableListOf<CommandButton>()
        val configuredActions = autoSettings.autoActionButtonOrder.filter {
            it != GestureAction.UNASSIGNED && it != GestureAction.OTHER_OPTION
        }

        val actionsToUse = configuredActions.ifEmpty {
            val regionTriggers = GestureTrigger.TOP_REGION_SLOTS + GestureTrigger.BOTTOM_REGION_SLOTS
            val list = mutableListOf<GestureAction>()
            for (trigger in regionTriggers) {
                val action = bindings[trigger]?.action ?: GestureAction.fromKey(trigger.defaultActionKey)
                if (action != GestureAction.UNASSIGNED && action !in list) {
                    list.add(action)
                }
            }
            if (GestureAction.PLAY_CURRENT_ALBUM !in list) list.add(GestureAction.PLAY_CURRENT_ALBUM)
            if (GestureAction.PLAY_CURRENT_ARTIST !in list) list.add(GestureAction.PLAY_CURRENT_ARTIST)
            if (GestureAction.PLAY_PAUSE !in list) list.add(GestureAction.PLAY_PAUSE)
            if (GestureAction.NEXT !in list) list.add(GestureAction.NEXT)
            if (GestureAction.PREVIOUS !in list) list.add(GestureAction.PREVIOUS)
            list
        }

        for (action in actionsToUse) {
            actionToCommandButton(action, repeatMode, shuffleMode)?.let {
                buttons.add(it)
            }
        }

        val customLayout = ImmutableList.copyOf(buttons)
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            session.setCustomLayout(customLayout)
        } else {
            serviceScope.launch(Dispatchers.Main) {
                session.setCustomLayout(customLayout)
            }
        }
    }

    private fun playCurrentAlbum() {
        val playbackManager = PlaybackManager.getInstance(applicationContext, settingsDataStore, musicDatabase)
        playbackManager.playCurrentAlbum()
    }

    private fun playCurrentArtist() {
        val playbackManager = PlaybackManager.getInstance(applicationContext, settingsDataStore, musicDatabase)
        playbackManager.playCurrentArtist()
    }

    private fun shuffleAllSongs() {
        val playbackManager = PlaybackManager.getInstance(applicationContext, settingsDataStore, musicDatabase)
        playbackManager.shuffleAllSongs()
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

        private val showPlayScreenItem = MediaItem.Builder()
            .setMediaId("show_play_screen")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Show Play Screen")
                    .setSubtitle("Return to play screen")
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setExtras(Bundle().apply {
                        putInt(
                            MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE,
                            MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
                        )
                    })
                    .build()
            )
            .build()

        private val shuffleAllItem = MediaItem.Builder()
            .setMediaId("shuffle_all")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Shuffle All Songs")
                    .setSubtitle("Shuffle entire music library")
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setExtras(Bundle().apply {
                        putInt(
                            MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE,
                            MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
                        )
                    })
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
                GestureAction.MENU,
                GestureAction.TOGGLE_DRIVING_MODE
            )

            for (act in allActions) {
                actionToSessionCommand(act)?.let { availableCommands.add(it) }
            }

            val restoreJob = serviceScope.launch(Dispatchers.Main) {
                val savedState = settingsDataStore.savedPlaybackStateFlow.first()
                val displaySettings = settingsDataStore.displaySettingsFlow.first()
                val dbSongs = musicDatabase.getAllSongs()
                val allSongs = dbSongs.ifEmpty { mediaStoreRepository.getAllSongs() }

                val playbackManager = PlaybackManager.getInstance(applicationContext, settingsDataStore, musicDatabase)

                if (playbackManager.currentPlaylist.value.isEmpty() || playbackManager.player.mediaItemCount == 0) {
                    if (allSongs.isNotEmpty()) {
                        if (savedState.queueIds.isNotEmpty()) {
                            val songMap = allSongs.associateBy { it.id }
                            val restoredQueue = savedState.queueIds.mapNotNull { songMap[it] }
                            val finalQueue = restoredQueue.ifEmpty { allSongs }

                            playbackManager.restorePlaybackState(
                                songs = finalQueue,
                                startIndex = savedState.activeSongIndex,
                                positionMs = savedState.positionMs,
                                shuffle = savedState.isShuffle,
                                repeat = savedState.isRepeat,
                                repeatMode = savedState.repeatMode,
                                shuffleMode = savedState.shuffleMode
                            )
                        } else {
                            playbackManager.restorePlaybackState(
                                songs = allSongs,
                                startIndex = 0,
                                positionMs = 0L,
                                shuffle = false,
                                repeat = false
                            )
                        }
                    }
                } else {
                    playbackManager.ensurePlayerReadyForPlayback()
                }

                if (displaySettings.autoAutoplayOnConnect) {
                    playbackManager.play()
                }
            }

            kotlinx.coroutines.runBlocking {
                try {
                    kotlinx.coroutines.withTimeout(500L) {
                        restoreJob.join()
                    }
                } catch (_: Exception) {}
            }

            val playerCommands = connectionResult.availablePlayerCommands.buildUpon()
                .add(androidx.media3.common.Player.COMMAND_GET_CURRENT_MEDIA_ITEM)
                .add(androidx.media3.common.Player.COMMAND_GET_TIMELINE)
                .add(androidx.media3.common.Player.COMMAND_GET_TRACKS)
                .add(androidx.media3.common.Player.COMMAND_GET_METADATA)
                .add(androidx.media3.common.Player.COMMAND_PLAY_PAUSE)
                .add(androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT)
                .add(androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(androidx.media3.common.Player.COMMAND_SEEK_TO_MEDIA_ITEM)
                .add(androidx.media3.common.Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                .add(androidx.media3.common.Player.COMMAND_SET_SHUFFLE_MODE)
                .add(androidx.media3.common.Player.COMMAND_SET_REPEAT_MODE)
                .build()

            return MediaSession.ConnectionResult.accept(
                availableCommands.build(),
                playerCommands
            )
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            val playbackManager = PlaybackManager.getInstance(applicationContext, settingsDataStore, musicDatabase)
            when (customCommand.customAction) {
                "com.travelingtunes.app.ACTION_PLAY_CURRENT_ALBUM" -> {
                    playCurrentAlbum()
                }
                "com.travelingtunes.app.ACTION_PLAY_CURRENT_ARTIST" -> {
                    playCurrentArtist()
                }
                "com.travelingtunes.app.ACTION_NEXT_ALBUM" -> {
                    playbackManager.nextAlbum()
                }
                "com.travelingtunes.app.ACTION_PREVIOUS_ALBUM" -> {
                    playbackManager.previousAlbum()
                }
                "com.travelingtunes.app.ACTION_SHUFFLE_ALL_SONGS" -> {
                    shuffleAllSongs()
                }
                "com.travelingtunes.app.ACTION_TOGGLE_REPEAT" -> {
                    playbackManager.toggleRepeat()
                }
                "com.travelingtunes.app.ACTION_TOGGLE_SHUFFLE" -> {
                    playbackManager.toggleShuffle()
                }
                "com.travelingtunes.app.ACTION_TOGGLE_DRIVING_MODE" -> {
                    serviceScope.launch {
                        settingsDataStore.toggleDrivingMode()
                    }
                }
                "com.travelingtunes.app.ACTION_PLAY_PAUSE" -> {
                    playbackManager.togglePlayPause()
                }
                "com.travelingtunes.app.ACTION_PLAY" -> {
                    playbackManager.play()
                }
                "com.travelingtunes.app.ACTION_PAUSE" -> {
                    playbackManager.pause()
                }
                "com.travelingtunes.app.ACTION_NEXT" -> {
                    playbackManager.next()
                }
                "com.travelingtunes.app.ACTION_PREVIOUS" -> {
                    playbackManager.previous()
                }
                "com.travelingtunes.app.ACTION_FAST_FORWARD" -> {
                    playbackManager.fastForward()
                }
                "com.travelingtunes.app.ACTION_REWIND" -> {
                    playbackManager.rewind()
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val playbackManager = PlaybackManager.getInstance(applicationContext, settingsDataStore, musicDatabase)
            val isPlaying = playbackManager.isPlaying.value || playbackManager.player.isPlaying
            val activeRootItem = if (isPlaying) showPlayScreenItem else rootItem

            val rootParams = LibraryParams.Builder()
                .setExtras(Bundle().apply {
                    putBoolean("android.media.browse.SEARCH_SUPPORTED", autoDisplaySettings.autoVoiceSearch)
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

            return Futures.immediateFuture(LibraryResult.ofItem(activeRootItem, rootParams))
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
                val showArt = autoDisplaySettings.autoShowAlbumArt
                if (parentId != "show_play_screen") {
                    items.add(showPlayScreenItem)
                }
                when (parentId) {
                    "show_play_screen" -> {
                        // "show_play_screen" is a playable media item representing the play screen; no children
                    }
                    "root" -> {
                        items.add(shuffleAllItem)
                        val categoryMap = mapOf(
                            AutoCategory.SONGS to categorySongs,
                            AutoCategory.ALBUMS to categoryAlbums,
                            AutoCategory.ARTISTS to categoryArtists,
                            AutoCategory.GENRES to categoryGenres,
                            AutoCategory.FOLDERS to categoryFolders
                        )
                        val catOrder = autoDisplaySettings.autoCategoryOrder.ifEmpty {
                            listOf(AutoCategory.SONGS, AutoCategory.ALBUMS, AutoCategory.ARTISTS, AutoCategory.GENRES, AutoCategory.FOLDERS)
                        }
                        catOrder.forEach { cat ->
                            categoryMap[cat]?.let { items.add(it) }
                        }
                    }
                    "category_songs" -> {
                        items.add(shuffleAllItem)
                        val songs = getAllSongsHelper()
                        items.addAll(songs.map { songToMediaItem(it, showArt) })
                    }
                    "category_albums" -> {
                        val dbAlbums = musicDatabase.getAlbums()
                        val albums = dbAlbums.ifEmpty {
                            getAllSongsHelper().groupBy { it.albumKey }.map { (_, albumSongs) ->
                                com.travelingtunes.app.core.database.AlbumInfo(
                                    name = albumSongs.firstOrNull()?.album ?: "Unknown Album",
                                    artist = albumSongs.firstOrNull()?.effectiveArtist ?: "Unknown Artist",
                                    songCount = albumSongs.size,
                                    artworkUri = albumSongs.firstOrNull()?.artworkUri
                                )
                            }
                        }
                        val albumStyle = if (autoDisplaySettings.autoAlbumStyleGrid) {
                            MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
                        } else {
                            MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
                        }
                        items.addAll(albums.map { album ->
                            MediaItem.Builder()
                                .setMediaId("album_${album.name}")
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(album.name)
                                        .setArtist(album.artist)
                                        .apply {
                                            if (showArt) setArtworkUri(album.artworkUri)
                                        }
                                        .setIsBrowsable(true)
                                        .setIsPlayable(false)
                                        .setExtras(Bundle().apply {
                                            putInt(
                                                MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                                                albumStyle
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
                        val artistStyle = if (autoDisplaySettings.autoArtistStyleGrid) {
                            MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
                        } else {
                            MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
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
                                                artistStyle
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
                if (mediaId == "show_play_screen") {
                    future.set(LibraryResult.ofItem(showPlayScreenItem, null))
                    return@launch
                }
                if (mediaId == "shuffle_all") {
                    future.set(LibraryResult.ofItem(shuffleAllItem, null))
                    return@launch
                }
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
                val resolvedSongs = mutableListOf<Song>()
                var playStartIndex = startIndex

                if (mediaItems.size == 1) {
                    val requestedId = mediaItems[0].mediaId
                    val matchedSong = songMap[requestedId]

                    if (requestedId == "show_play_screen") {
                        var returnPosMs = startPositionMs
                        withContext(Dispatchers.Main) {
                            val playbackManager = PlaybackManager.getInstance(applicationContext, settingsDataStore, musicDatabase)
                            val playlist = playbackManager.currentPlaylist.value
                            val currentSong = playbackManager.currentSong.value
                            if (playlist.isNotEmpty() && currentSong != null) {
                                val songIndex = playlist.indexOfFirst { it.id == currentSong.id }.coerceAtLeast(0)
                                val currentPos = playbackManager.player.currentPosition.coerceAtLeast(0L)
                                returnPosMs = currentPos
                                resolvedSongs.addAll(playlist)
                                resolvedItems.addAll(playlist.map { songToMediaItem(it) })
                                playStartIndex = songIndex
                                playbackManager.ensurePlayerReadyForPlayback(songIndex, currentPos)
                            } else {
                                playbackManager.shuffleAllSongs()
                                val shuffledQueue = playbackManager.currentPlaylist.value
                                resolvedSongs.addAll(shuffledQueue)
                                resolvedItems.addAll(shuffledQueue.map { songToMediaItem(it) })
                                playStartIndex = 0
                                returnPosMs = 0L
                            }
                        }
                        val safeStartIndex = playStartIndex.coerceIn(0, (resolvedItems.size - 1).coerceAtLeast(0))
                        future.set(MediaSession.MediaItemsWithStartPosition(resolvedItems, safeStartIndex, returnPosMs))
                        return@launch
                    } else if (requestedId == "shuffle_all") {
                        withContext(Dispatchers.Main) {
                            val playbackManager = PlaybackManager.getInstance(applicationContext, settingsDataStore, musicDatabase)
                            playbackManager.shuffleAllSongs()
                            val shuffledQueue = playbackManager.currentPlaylist.value
                            resolvedSongs.addAll(shuffledQueue)
                            resolvedItems.addAll(shuffledQueue.map { songToMediaItem(it) })
                        }
                        playStartIndex = 0
                    } else if (matchedSong != null) {
                        val albumSongs = allSongs.filter { it.album.equals(matchedSong.album, ignoreCase = true) }
                        val queueSongs = if (albumSongs.size > 1) albumSongs else allSongs

                        val songIndex = queueSongs.indexOfFirst { it.id == matchedSong.id }
                        if (songIndex >= 0) {
                            resolvedSongs.addAll(queueSongs)
                            resolvedItems.addAll(queueSongs.map { songToMediaItem(it) })
                            playStartIndex = songIndex
                        } else {
                            resolvedSongs.add(matchedSong)
                            resolvedItems.add(songToMediaItem(matchedSong))
                            playStartIndex = 0
                        }
                    } else if (requestedId.startsWith("album_")) {
                        val albumName = requestedId.removePrefix("album_")
                        val songs = allSongs.filter { it.album.equals(albumName, ignoreCase = true) }
                        resolvedSongs.addAll(songs)
                        resolvedItems.addAll(songs.map { songToMediaItem(it) })
                        playStartIndex = 0
                    } else if (requestedId.startsWith("artist_")) {
                        val artistName = requestedId.removePrefix("artist_")
                        val songs = allSongs.filter { it.artist.equals(artistName, ignoreCase = true) }
                        resolvedSongs.addAll(songs)
                        resolvedItems.addAll(songs.map { songToMediaItem(it) })
                        playStartIndex = 0
                    } else if (requestedId.startsWith("genre_")) {
                        val genreName = requestedId.removePrefix("genre_")
                        val songs = allSongs.filter { it.genre.equals(genreName, ignoreCase = true) }
                        resolvedSongs.addAll(songs)
                        resolvedItems.addAll(songs.map { songToMediaItem(it) })
                        playStartIndex = 0
                    } else if (requestedId.startsWith("folder_")) {
                        val folderPath = requestedId.removePrefix("folder_")
                        val songs = allSongs.filter { it.folderPath.equals(folderPath, ignoreCase = true) }
                        resolvedSongs.addAll(songs)
                        resolvedItems.addAll(songs.map { songToMediaItem(it) })
                        playStartIndex = 0
                    } else {
                        resolvedItems.addAll(mediaItems)
                    }
                } else {
                    for (item in mediaItems) {
                        val song = songMap[item.mediaId]
                        if (song != null) {
                            resolvedSongs.add(song)
                            resolvedItems.add(songToMediaItem(song))
                        } else {
                            resolvedItems.add(item)
                        }
                    }
                }

                val safeStartIndex = playStartIndex.coerceIn(0, (resolvedItems.size - 1).coerceAtLeast(0))

                if (resolvedSongs.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        val playbackManager = PlaybackManager.getInstance(applicationContext, settingsDataStore, musicDatabase)
                        playbackManager.setPlaylistFromAuto(resolvedSongs, safeStartIndex)
                    }
                }

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
                    if (item.mediaId == "show_play_screen") {
                        val playbackManager = PlaybackManager.getInstance(applicationContext, settingsDataStore, musicDatabase)
                        val playlist = playbackManager.currentPlaylist.value
                        if (playlist.isNotEmpty()) {
                            resolvedItems.addAll(playlist.map { songToMediaItem(it) })
                        }
                    } else if (item.mediaId == "shuffle_all") {
                        val allSongs = getAllSongsHelper()
                        val shuffled = allSongs.shuffled()
                        resolvedItems.addAll(shuffled.map { songToMediaItem(it) })
                    } else if (song != null) {
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

@OptIn(UnstableApi::class)
fun songToMediaItem(song: Song, showAlbumArt: Boolean = true): MediaItem {
    val artUri = if (showAlbumArt) song.artworkUri else null
    val metadata = MediaMetadata.Builder()
        .setTitle(song.title)
        .setDisplayTitle(song.title)
        .setArtist(song.artist)
        .setSubtitle(if (song.album.isNotBlank()) "${song.artist} — ${song.album}" else song.artist)
        .setDescription(song.album)
        .setAlbumTitle(song.album)
        .setAlbumArtist(song.artist)
        .setGenre(song.genre)
        .setTrackNumber(song.trackNumber)
        .setDurationMs(song.durationMs)
        .setIsPlayable(true)
        .setIsBrowsable(false)
        .apply {
            if (artUri != null) setArtworkUri(artUri)
        }
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
