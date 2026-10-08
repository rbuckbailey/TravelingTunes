package com.travelingtunes.app.core.media

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.travelingtunes.app.MainActivity
import java.io.File
import com.travelingtunes.app.R
import com.travelingtunes.app.core.database.MusicDatabase
import com.travelingtunes.app.core.datastore.SettingsDataStore
import com.travelingtunes.app.core.media.DeviceConnectionReceiver
import com.travelingtunes.app.core.media.MediaStoreRepository
import com.travelingtunes.app.core.media.PlaybackManager
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
        const val NOTIFICATION_CHANNEL_ID = "traveling_tunes_playback_channel"
        const val ACTION_PREVIOUS = "com.travelingtunes.app.ACTION_PREVIOUS"
        const val ACTION_TOGGLE_PLAY_PAUSE = "com.travelingtunes.app.ACTION_TOGGLE_PLAY_PAUSE"
        const val ACTION_NEXT = "com.travelingtunes.app.ACTION_NEXT"

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
                .setWakeMode(C.WAKE_MODE_LOCAL)
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
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && e is android.app.ForegroundServiceStartNotAllowedException) {
                    android.util.Log.w("MusicPlaybackService", "startService not allowed in background", e)
                } else {
                    android.util.Log.w("MusicPlaybackService", "startService failed, attempting startForegroundService fallback", e)
                    try {
                        intent.putExtra("is_foreground_start", true)
                        androidx.core.content.ContextCompat.startForegroundService(context.applicationContext, intent)
                    } catch (e2: Exception) {
                        android.util.Log.w("MusicPlaybackService", "Fallback startForegroundService failed", e2)
                    }
                }
            }
        }
    }

    private lateinit var musicDatabase: MusicDatabase
    private lateinit var mediaStoreRepository: MediaStoreRepository
    private lateinit var settingsDataStore: SettingsDataStore
    private var autoDisplaySettings = DisplaySettings()
    private val serviceScope = CoroutineScope(Dispatchers.IO)



    private fun loadArtworkBitmapForMediaItem(context: Context, mediaItem: MediaItem?): Bitmap? {
        if (mediaItem == null) return null
        val metadata = mediaItem.mediaMetadata
        val uri = metadata.artworkUri
        if (uri != null) {
            try {
                if (uri.scheme == "file" && uri.path != null) {
                    val file = File(uri.path!!)
                    if (file.exists() && file.length() > 0L) {
                        return BitmapFactory.decodeFile(file.absolutePath)
                    }
                } else {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        return BitmapFactory.decodeStream(stream)
                    }
                }
            } catch (_: Exception) {}
        }
        val data = metadata.artworkData
        if (data != null && data.isNotEmpty()) {
            try {
                return BitmapFactory.decodeByteArray(data, 0, data.size)
            } catch (_: Exception) {}
        }
        return null
    }

    @OptIn(UnstableApi::class)
    private fun startForegroundIfNeeded(): Boolean {
        return try {
            val player = sharedPlayer
            val currentMediaItem = player?.currentMediaItem
            val title = currentMediaItem?.mediaMetadata?.title?.toString()
                ?.ifEmpty { getString(R.string.app_name) } ?: getString(R.string.app_name)
            val artist = currentMediaItem?.mediaMetadata?.artist?.toString().orEmpty()
            val album = currentMediaItem?.mediaMetadata?.albumTitle?.toString().orEmpty()
            val subtitle = if (album.isNotBlank()) "$artist — $album" else artist

            val isPlaying = player?.isPlaying == true

            val artBitmap = loadArtworkBitmapForMediaItem(applicationContext, currentMediaItem)

            val openAppIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                openAppIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val prevPendingIntent = PendingIntent.getService(
                this,
                1,
                Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_PREVIOUS },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val playPausePendingIntent = PendingIntent.getService(
                this,
                2,
                Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_TOGGLE_PLAY_PAUSE },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val nextPendingIntent = PendingIntent.getService(
                this,
                3,
                Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_NEXT },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val prevAction = NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_previous,
                "Previous",
                prevPendingIntent
            ).build()

            val playPauseAction = NotificationCompat.Action.Builder(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                playPausePendingIntent
            ).build()

            val nextAction = NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_next,
                "Next",
                nextPendingIntent
            ).build()

            val session = sharedSession
            val mediaStyle = if (session != null) {
                MediaStyleNotificationHelper.MediaStyle(session)
                    .setShowActionsInCompactView(0, 1, 2)
            } else {
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            }

            val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(subtitle)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .addAction(prevAction)
                .addAction(playPauseAction)
                .addAction(nextAction)
                .setStyle(mediaStyle)
                .setOngoing(isPlaying)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_LOW)

            if (artBitmap != null) {
                builder.setLargeIcon(artBitmap)
            }

            val notification = builder.build()

            val fgsType = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            }
            androidx.core.app.ServiceCompat.startForeground(
                this,
                1001,
                notification,
                fgsType
            )
            true
        } catch (e: Exception) {
            android.util.Log.e("MusicPlaybackService", "Failed to start foreground service in onStartCommand", e)
            false
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        musicDatabase = MusicDatabase(applicationContext)
        mediaStoreRepository = MediaStoreRepository(applicationContext)
        settingsDataStore = SettingsDataStore(applicationContext)

        createNotificationChannel()
        DeviceConnectionReceiver.register(applicationContext)

        val defaultProvider = DefaultMediaNotificationProvider.Builder(applicationContext)
            .setChannelId(NOTIFICATION_CHANNEL_ID)
            .setChannelName(R.string.app_name)
            .build()
        setMediaNotificationProvider(object : MediaNotification.Provider {
            override fun createNotification(
                mediaSession: MediaSession,
                customLayout: ImmutableList<CommandButton>,
                actionFactory: MediaNotification.ActionFactory,
                onNotificationChangedListener: MediaNotification.Provider.Callback
            ): MediaNotification {
                val mediaNotification = defaultProvider.createNotification(mediaSession, customLayout, actionFactory, onNotificationChangedListener)
                mediaNotification.notification.flags = mediaNotification.notification.flags or android.app.Notification.FLAG_ONGOING_EVENT
                return mediaNotification
            }

            override fun handleCustomCommand(
                session: MediaSession,
                action: String,
                extras: Bundle
            ): Boolean {
                return defaultProvider.handleCustomCommand(session, action, extras)
            }
        })

        setListener(object : Listener {
            @OptIn(UnstableApi::class)
            override fun onForegroundServiceStartNotAllowedException() {
                android.util.Log.e("MusicPlaybackService", "Foreground service start not allowed exception triggered")
            }
        })

        val player = getOrCreatePlayer(applicationContext)
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                startForegroundIfNeeded()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                startForegroundIfNeeded()
            }
        })

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val sessionCallback = AutoLibrarySessionCallback()
        val session = MediaLibrarySession.Builder(this, player, sessionCallback)
            .setSessionActivity(sessionActivityPendingIntent)
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

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Media Playback Controls",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Media controls and track information notification for background playback"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    private fun notifyAutoChildrenChanged(session: MediaLibrarySession) {
        listOf("root", "show_play_screen", "category_queue", "queue").forEach { parentId ->
            try {
                session.notifyChildrenChanged(parentId, 0, null)
            } catch (e: Exception) {
                android.util.Log.w("MusicPlaybackService", "Error notifying children changed for $parentId", e)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_PREVIOUS -> {
                PlaybackManager.getInstance(applicationContext, settingsDataStore, musicDatabase).previous()
            }
            ACTION_TOGGLE_PLAY_PAUSE -> {
                PlaybackManager.getInstance(applicationContext, settingsDataStore, musicDatabase).togglePlayPause()
            }
            ACTION_NEXT -> {
                PlaybackManager.getInstance(applicationContext, settingsDataStore, musicDatabase).next()
            }
        }

        val isForegroundStart = intent?.getBooleanExtra("is_foreground_start", false) == true
        val player = sharedPlayer
        val isPlayingOrReady = player != null && player.playWhenReady && player.playbackState != ExoPlayer.STATE_IDLE

        if (isForegroundStart || isPlayingOrReady || intent?.action != null) {
            val foregroundStarted = startForegroundIfNeeded()
            if (isForegroundStart && !foregroundStarted) {
                android.util.Log.w("MusicPlaybackService", "startForeground failed on foreground start request; stopping service to prevent crash")
                stopSelf(startId)
                return START_NOT_STICKY
            }
        }

        if (intent?.action == "android.media.action.MEDIA_PLAY_FROM_SEARCH") {
            val query = intent.getStringExtra(android.app.SearchManager.QUERY) ?: intent.getStringExtra("query")
            val focus = intent.getStringExtra(android.provider.MediaStore.EXTRA_MEDIA_FOCUS)
            val title = intent.getStringExtra(android.provider.MediaStore.EXTRA_MEDIA_TITLE)
            val artist = intent.getStringExtra(android.provider.MediaStore.EXTRA_MEDIA_ARTIST)
            val album = intent.getStringExtra(android.provider.MediaStore.EXTRA_MEDIA_ALBUM)
            val genre = intent.getStringExtra(android.provider.MediaStore.EXTRA_MEDIA_GENRE)

            serviceScope.launch(Dispatchers.Main) {
                val playbackManager = PlaybackManager.getInstance(applicationContext, settingsDataStore, musicDatabase)
                playbackManager.playFromSearchQuery(
                    query = query,
                    focus = focus,
                    title = title,
                    artist = artist,
                    album = album,
                    genre = genre
                )
            }
        }
        return START_STICKY
    }

    @OptIn(UnstableApi::class)
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = sharedPlayer
        if (player != null && player.playWhenReady && player.playbackState != ExoPlayer.STATE_IDLE) {
            android.util.Log.i("MusicPlaybackService", "onTaskRemoved: active playback running, maintaining foreground service")
        } else {
            super.onTaskRemoved(rootIntent)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibraryService.MediaLibrarySession? {
        return sharedSession
    }

    override fun onDestroy() {
        android.util.Log.i("MusicPlaybackService", "onDestroy called")
        DeviceConnectionReceiver.unregister(applicationContext)
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
            GestureAction.EDIT_TAGS -> "com.travelingtunes.app.ACTION_EDIT_TAGS"
            GestureAction.SHARE_TUNES -> "com.travelingtunes.app.ACTION_SHARE_TUNES"
            else -> return null
        }
        return SessionCommand(actionString, Bundle.EMPTY)
    }

    private fun actionToIconRes(action: GestureAction): Int {
        return when (action) {
            GestureAction.PLAY_CURRENT_ALBUM -> R.drawable.ic_play_current_album
            GestureAction.PLAY_CURRENT_ARTIST -> R.drawable.ic_play_current_artist
            GestureAction.SHUFFLE_ALL_SONGS -> R.drawable.ic_shuffle_all
            GestureAction.TOGGLE_REPEAT -> R.drawable.ic_toggle_repeat
            GestureAction.TOGGLE_SHUFFLE -> R.drawable.ic_toggle_shuffle
            GestureAction.PLAY_PAUSE, GestureAction.PLAY, GestureAction.PAUSE -> R.drawable.ic_play_pause
            GestureAction.NEXT -> R.drawable.ic_next_song
            GestureAction.PREVIOUS -> R.drawable.ic_previous_song
            GestureAction.FAST_FORWARD -> R.drawable.ic_fast_forward
            GestureAction.REWIND -> R.drawable.ic_rewind
            GestureAction.SONG_PICKER, GestureAction.SHOW_QUEUE -> R.drawable.ic_song_picker
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

    private fun isAndroidAutoController(controller: MediaSession.ControllerInfo): Boolean {
        val pkg = controller.packageName
        if (pkg == applicationContext.packageName) return false
        return pkg == "com.google.android.projection.gearhead" ||
                pkg == "com.google.android.car.messenger" ||
                pkg == "com.google.android.autoservice" ||
                pkg.contains("projection", ignoreCase = true) ||
                pkg.contains("gearhead", ignoreCase = true) ||
                pkg.contains("car", ignoreCase = true)
    }

    private fun buildCustomLayoutButtons(
        bindings: Map<GestureTrigger, GestureBinding>,
        autoSettings: DisplaySettings,
        repeatMode: RepeatMode = RepeatMode.OFF,
        shuffleMode: ShuffleMode = ShuffleMode.OFF
    ): ImmutableList<CommandButton> {
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

        return ImmutableList.copyOf(buttons)
    }

    private fun updateCustomLayout(
        session: MediaLibrarySession,
        bindings: Map<GestureTrigger, GestureBinding>,
        autoSettings: DisplaySettings,
        repeatMode: RepeatMode = RepeatMode.OFF,
        shuffleMode: ShuffleMode = ShuffleMode.OFF
    ) {
        val customLayout = buildCustomLayoutButtons(bindings, autoSettings, repeatMode, shuffleMode)
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
                    .setArtworkUri(android.net.Uri.parse("android.resource://${applicationContext.packageName}/${R.drawable.ic_shuffle_all}"))
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

        private val categoryQueue = MediaItem.Builder()
            .setMediaId("category_queue")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Current Queue")
                    .setSubtitle("View playing queue")
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

        private val songPickerItem = MediaItem.Builder()
            .setMediaId("category_picker")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Song Picker")
                    .setSubtitle("Browse Music Library (Songs, Albums, Artists...)")
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

        override fun onMediaButtonEvent(
            session: MediaSession,
            controllerInfo: MediaSession.ControllerInfo,
            intent: Intent
        ): Boolean {
            val keyEvent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, android.view.KeyEvent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
            }
            if (keyEvent != null && keyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                val playbackManager = PlaybackManager.getInstance(applicationContext, settingsDataStore, musicDatabase)
                when (keyEvent.keyCode) {
                    android.view.KeyEvent.KEYCODE_MEDIA_NEXT -> {
                        playbackManager.next()
                        return true
                    }
                    android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                        playbackManager.previous()
                        return true
                    }
                    android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                    android.view.KeyEvent.KEYCODE_MEDIA_PLAY,
                    android.view.KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                        playbackManager.togglePlayPause()
                        return true
                    }
                    android.view.KeyEvent.KEYCODE_MEDIA_STOP -> {
                        playbackManager.pause()
                        return true
                    }
                    android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                        playbackManager.fastForward()
                        return true
                    }
                    android.view.KeyEvent.KEYCODE_MEDIA_REWIND -> {
                        playbackManager.rewind()
                        return true
                    }
                }
            }
            return super.onMediaButtonEvent(session, controllerInfo, intent)
        }

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

            val isAutoController = isAndroidAutoController(controller)
            val bindings = kotlinx.coroutines.runBlocking { settingsDataStore.gestureBindingsFlow.first() }
            val customLayout = buildCustomLayoutButtons(
                bindings = bindings,
                autoSettings = autoDisplaySettings,
                repeatMode = PlaybackManager.getInstance(applicationContext, settingsDataStore, musicDatabase).repeatMode.value,
                shuffleMode = PlaybackManager.getInstance(applicationContext, settingsDataStore, musicDatabase).shuffleMode.value
            )

            if (isAutoController) {
                serviceScope.launch(Dispatchers.IO) {
                    val record = settingsDataStore.recordDeviceConnected(
                        id = "android_auto_vehicle",
                        name = "Android Auto Vehicle",
                        type = com.travelingtunes.app.core.model.ConnectedDeviceType.ANDROID_AUTO
                    )
                    val savedState = settingsDataStore.savedPlaybackStateFlow.first()
                    val dbSongs = musicDatabase.getAllSongs()
                    val allSongs = dbSongs.ifEmpty { mediaStoreRepository.getAllSongs() }

                    withContext(Dispatchers.Main) {
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

                        if (record.actions.isNotEmpty()) {
                            playbackManager.executeActionSequence(record.actions)
                        } else if (autoDisplaySettings.autoAutoplayOnConnect) {
                            playbackManager.play()
                        }
                    }
                }
            } else {
                serviceScope.launch(Dispatchers.IO) {
                    val savedState = settingsDataStore.savedPlaybackStateFlow.first()
                    val dbSongs = musicDatabase.getAllSongs()
                    val allSongs = dbSongs.ifEmpty { mediaStoreRepository.getAllSongs() }

                    withContext(Dispatchers.Main) {
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
                    }
                }
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

            session.setCustomLayout(controller, customLayout)

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
                "com.travelingtunes.app.ACTION_SHOW_QUEUE", "com.travelingtunes.app.ACTION_SONG_PICKER" -> {
                    sharedSession?.let { session ->
                        notifyAutoChildrenChanged(session)
                    }
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

                val effectivePageSize = if (pageSize > 0) pageSize else 50
                val effectivePage = if (page >= 0) page else 0

                fun <T> paginateDomainList(list: List<T>): List<T> {
                    val fromIndex = (effectivePage * effectivePageSize).coerceIn(0, list.size)
                    val toIndex = ((effectivePage + 1) * effectivePageSize).coerceIn(fromIndex, list.size)
                    return list.subList(fromIndex, toIndex)
                }

                if (parentId != "show_play_screen" && effectivePage == 0) {
                    items.add(showPlayScreenItem)
                }

                when (parentId) {
                    "show_play_screen", "root" -> {
                        val rootCategoryItems = mutableListOf<MediaItem>()
                        rootCategoryItems.add(categoryQueue)
                        rootCategoryItems.add(songPickerItem)
                        rootCategoryItems.add(shuffleAllItem)
                        val categoryMap = mapOf(
                            AutoCategory.QUEUE to categoryQueue,
                            AutoCategory.SONGS to categorySongs,
                            AutoCategory.ALBUMS to categoryAlbums,
                            AutoCategory.ARTISTS to categoryArtists,
                            AutoCategory.GENRES to categoryGenres,
                            AutoCategory.FOLDERS to categoryFolders
                        )
                        val catOrder = autoDisplaySettings.autoCategoryOrder.ifEmpty {
                            listOf(AutoCategory.QUEUE, AutoCategory.SONGS, AutoCategory.ALBUMS, AutoCategory.ARTISTS, AutoCategory.GENRES, AutoCategory.FOLDERS)
                        }
                        catOrder.forEach { cat ->
                            categoryMap[cat]?.let { if (!rootCategoryItems.contains(it)) rootCategoryItems.add(it) }
                        }
                        items.addAll(paginateDomainList(rootCategoryItems))
                    }
                    "category_queue", "queue" -> {
                        val playbackManager = PlaybackManager.getInstance(applicationContext, settingsDataStore, musicDatabase)
                        val currentQueue = playbackManager.currentPlaylist.value
                        val rawSongs = if (currentQueue.isNotEmpty()) currentQueue else getAllSongsHelper()
                        val pagedSongs = paginateDomainList(rawSongs)
                        items.addAll(pagedSongs.map { songToMediaItem(it, applicationContext, showArt) })
                    }
                    "category_picker" -> {
                        val pickerItems = listOf(shuffleAllItem, categorySongs, categoryAlbums, categoryArtists, categoryGenres, categoryFolders)
                        items.addAll(paginateDomainList(pickerItems))
                    }
                    "category_songs" -> {
                        val songs = getAllSongsHelper()
                        val pagedSongs = paginateDomainList(songs)
                        items.addAll(pagedSongs.map { songToMediaItem(it, applicationContext, showArt) })
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
                        val pagedAlbums = paginateDomainList(albums)
                        items.addAll(pagedAlbums.map { album ->
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
                        val pagedArtists = paginateDomainList(artists)
                        items.addAll(pagedArtists.map { artist ->
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
                        val pagedGenres = paginateDomainList(genres)
                        items.addAll(pagedGenres.map { genre ->
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
                        val pagedFolders = paginateDomainList(folders)
                        items.addAll(pagedFolders.map { folder ->
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
                        val pagedSongs = paginateDomainList(songs)
                        items.addAll(pagedSongs.map { songToMediaItem(it, applicationContext) })
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
                if (mediaId == "category_queue" || mediaId == "queue") {
                    future.set(LibraryResult.ofItem(categoryQueue, null))
                    return@launch
                }
                if (mediaId == "category_picker") {
                    future.set(LibraryResult.ofItem(songPickerItem, null))
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
                        future.set(LibraryResult.ofItem(songToMediaItem(song, applicationContext, includeArtworkData = true), null))
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

                val mediaItems = results.map { songToMediaItem(it, applicationContext) }
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

                val playbackManager = PlaybackManager.getInstance(applicationContext, settingsDataStore, musicDatabase)

                val resolvedSongs = mutableListOf<Song>()
                var playStartIndex = startIndex
                var returnPosMs = startPositionMs

                if (mediaItems.size == 1) {
                    val requestedId = mediaItems[0].mediaId
                    val matchedSong = songMap[requestedId]

                    if (requestedId == "show_play_screen") {
                        val playlist = playbackManager.currentPlaylist.value
                        val currentSong = playbackManager.currentSong.value
                        if (playlist.isNotEmpty() && currentSong != null) {
                            val songIndex = playlist.indexOfFirst { it.id == currentSong.id }.coerceAtLeast(0)
                            val currentPos = try { playbackManager.player.currentPosition.coerceAtLeast(0L) } catch (_: Exception) { 0L }
                            returnPosMs = currentPos
                            resolvedSongs.addAll(playlist)
                            playStartIndex = songIndex
                        } else {
                            withContext(Dispatchers.Main) {
                                playbackManager.shuffleAllSongs()
                            }
                            val shuffledQueue = playbackManager.currentPlaylist.value
                            resolvedSongs.addAll(shuffledQueue)
                            playStartIndex = 0
                            returnPosMs = 0L
                        }
                    } else if (requestedId == "shuffle_all") {
                        withContext(Dispatchers.Main) {
                            playbackManager.shuffleAllSongs()
                        }
                        val shuffledQueue = playbackManager.currentPlaylist.value
                        resolvedSongs.addAll(shuffledQueue)
                        playStartIndex = 0
                    } else if (requestedId == "category_queue" || requestedId == "queue" || requestedId == "category_picker") {
                        val playlist = playbackManager.currentPlaylist.value
                        val currentSong = playbackManager.currentSong.value
                        if (playlist.isNotEmpty()) {
                            val songIndex = if (currentSong != null) playlist.indexOfFirst { it.id == currentSong.id }.coerceAtLeast(0) else 0
                            resolvedSongs.addAll(playlist)
                            playStartIndex = songIndex
                        } else {
                            withContext(Dispatchers.Main) {
                                playbackManager.shuffleAllSongs()
                            }
                            val shuffledQueue = playbackManager.currentPlaylist.value
                            resolvedSongs.addAll(shuffledQueue)
                            playStartIndex = 0
                        }
                    } else if (matchedSong != null) {
                        val (activeQueue, activeIndex) = playbackManager.buildActiveQueue(targetSong = matchedSong)
                        resolvedSongs.addAll(activeQueue)
                        playStartIndex = activeIndex
                    } else if (requestedId.startsWith("album_")) {
                        val albumName = requestedId.removePrefix("album_")
                        val songs = allSongs.filter { it.album.equals(albumName, ignoreCase = true) }
                        if (songs.isNotEmpty()) {
                            val targetSong = songs.first()
                            val (activeQueue, activeIndex) = playbackManager.buildActiveQueue(targetSong = targetSong, baseList = songs)
                            resolvedSongs.addAll(activeQueue)
                            playStartIndex = activeIndex
                        }
                    } else if (requestedId.startsWith("artist_")) {
                        val artistName = requestedId.removePrefix("artist_")
                        val songs = allSongs.filter { it.artist.equals(artistName, ignoreCase = true) }
                        if (songs.isNotEmpty()) {
                            val targetSong = songs.first()
                            val (activeQueue, activeIndex) = playbackManager.buildActiveQueue(targetSong = targetSong, baseList = songs)
                            resolvedSongs.addAll(activeQueue)
                            playStartIndex = activeIndex
                        }
                    } else if (requestedId.startsWith("genre_")) {
                        val genreName = requestedId.removePrefix("genre_")
                        val songs = allSongs.filter { it.genre.equals(genreName, ignoreCase = true) }
                        if (songs.isNotEmpty()) {
                            val targetSong = songs.first()
                            val (activeQueue, activeIndex) = playbackManager.buildActiveQueue(targetSong = targetSong, baseList = songs)
                            resolvedSongs.addAll(activeQueue)
                            playStartIndex = activeIndex
                        }
                    } else if (requestedId.startsWith("folder_")) {
                        val folderPath = requestedId.removePrefix("folder_")
                        val songs = allSongs.filter { it.folderPath.equals(folderPath, ignoreCase = true) }
                        if (songs.isNotEmpty()) {
                            val targetSong = songs.first()
                            val (activeQueue, activeIndex) = playbackManager.buildActiveQueue(targetSong = targetSong, baseList = songs)
                            resolvedSongs.addAll(activeQueue)
                            playStartIndex = activeIndex
                        }
                    } else {
                        val songsFromItems = mediaItems.mapNotNull { songMap[it.mediaId] }
                        if (songsFromItems.isNotEmpty()) {
                            val targetSong = songsFromItems.first()
                            val (activeQueue, activeIndex) = playbackManager.buildActiveQueue(targetSong = targetSong, baseList = songsFromItems)
                            resolvedSongs.addAll(activeQueue)
                            playStartIndex = activeIndex
                        }
                    }
                } else {
                    val songsFromItems = mediaItems.mapNotNull { songMap[it.mediaId] }
                    if (songsFromItems.isNotEmpty()) {
                        val targetIndex = startIndex.coerceIn(0, songsFromItems.size - 1)
                        val targetSong = songsFromItems[targetIndex]
                        val (activeQueue, activeIndex) = playbackManager.buildActiveQueue(targetSong = targetSong, baseList = songsFromItems)
                        resolvedSongs.addAll(activeQueue)
                        playStartIndex = activeIndex
                    }
                }

                val safeStartIndex = playStartIndex.coerceIn(0, (resolvedSongs.size - 1).coerceAtLeast(0))
                val finalMediaItems = resolvedSongs.mapIndexed { idx, song ->
                    songToMediaItem(song, applicationContext, includeArtworkData = (Math.abs(idx - safeStartIndex) <= 1))
                }

                if (resolvedSongs.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        playbackManager.setPlaylistFromAuto(resolvedSongs, safeStartIndex)
                    }
                }

                future.set(MediaSession.MediaItemsWithStartPosition(finalMediaItems, safeStartIndex, returnPosMs))
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
                            resolvedItems.addAll(playlist.map { songToMediaItem(it, applicationContext) })
                        }
                    } else if (item.mediaId == "shuffle_all") {
                        val allSongs = getAllSongsHelper()
                        val shuffled = allSongs.shuffled()
                        resolvedItems.addAll(shuffled.map { songToMediaItem(it, applicationContext) })
                    } else if (song != null) {
                        resolvedItems.add(songToMediaItem(song, applicationContext))
                    } else if (item.mediaId.startsWith("album_")) {
                        val albumName = item.mediaId.removePrefix("album_")
                        val albumSongs = allSongs.filter { it.album.equals(albumName, ignoreCase = true) }
                        resolvedItems.addAll(albumSongs.map { songToMediaItem(it, applicationContext) })
                    } else if (item.mediaId.startsWith("artist_")) {
                        val artistName = item.mediaId.removePrefix("artist_")
                        val artistSongs = allSongs.filter { it.artist.equals(artistName, ignoreCase = true) }
                        resolvedItems.addAll(artistSongs.map { songToMediaItem(it, applicationContext) })
                    } else if (item.mediaId.startsWith("genre_")) {
                        val genreName = item.mediaId.removePrefix("genre_")
                        val genreSongs = allSongs.filter { it.genre.equals(genreName, ignoreCase = true) }
                        resolvedItems.addAll(genreSongs.map { songToMediaItem(it, applicationContext) })
                    } else if (item.mediaId.startsWith("folder_")) {
                        val folderPath = item.mediaId.removePrefix("folder_")
                        val folderSongs = allSongs.filter { it.folderPath.equals(folderPath, ignoreCase = true) }
                        resolvedItems.addAll(folderSongs.map { songToMediaItem(it, applicationContext) })
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

fun getArtworkBytesForSong(context: Context, song: Song): ByteArray? {
    try {
        val downloadedFile = AlbumArtDownloader.getDownloadedArtworkFile(context, song.artist, song.album)
        if (downloadedFile != null) {
            val bytes = downloadedFile.readBytes()
            if (bytes.isNotEmpty()) {
                if (bytes.size <= 300 * 1024) {
                    return bytes
                }
                val bmp = com.travelingtunes.app.feature.player.decodeSampledBitmapFromFile(downloadedFile.absolutePath, 500, 500)
                if (bmp != null) {
                    val stream = java.io.ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                    return stream.toByteArray()
                }
            }
        }

        if (song.artworkUri != null) {
            val uri = song.artworkUri
            val bytes = if (uri.scheme == "file" && uri.path != null) {
                val file = File(uri.path!!)
                if (file.exists() && file.length() > 0L) file.readBytes() else null
            } else {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
            if (bytes != null && bytes.isNotEmpty()) {
                if (bytes.size <= 300 * 1024) {
                    return bytes
                }
                val bmp = com.travelingtunes.app.feature.player.decodeSampledBitmapFromByteArray(bytes, 500, 500)
                if (bmp != null) {
                    val stream = java.io.ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                    return stream.toByteArray()
                }
            }
        }

        val md = java.security.MessageDigest.getInstance("MD5")
        val digest = md.digest("${song.artist}-${song.album}".toByteArray())
        val hashKey = digest.joinToString("") { "%02x".format(it) }

        val cacheArtFile = File(context.cacheDir, "album_art/art_$hashKey.jpg")
        val embeddedArtFile = File(context.cacheDir, "embedded_art/art_embedded_$hashKey.jpg")
        val downloadedArtFile = File(context.filesDir, "downloaded_art/art_downloaded_$hashKey.jpg")

        val targetFile = when {
            downloadedArtFile.exists() && downloadedArtFile.length() > 0L -> downloadedArtFile
            embeddedArtFile.exists() && embeddedArtFile.length() > 0L -> embeddedArtFile
            cacheArtFile.exists() && cacheArtFile.length() > 0L -> cacheArtFile
            else -> null
        }

        if (targetFile != null) {
            val bytes = targetFile.readBytes()
            if (bytes.size <= 300 * 1024) {
                return bytes
            }
            val bmp = com.travelingtunes.app.feature.player.decodeSampledBitmapFromFile(targetFile.absolutePath, 500, 500)
            if (bmp != null) {
                val stream = java.io.ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                return stream.toByteArray()
            }
        }

        val mmr = android.media.MediaMetadataRetriever()
        return try {
            mmr.setDataSource(context, song.contentUri)
            val rawBytes = mmr.embeddedPicture
            if (rawBytes != null && rawBytes.isNotEmpty()) {
                if (rawBytes.size <= 300 * 1024) {
                    rawBytes
                } else {
                    val bmp = com.travelingtunes.app.feature.player.decodeSampledBitmapFromByteArray(rawBytes, 500, 500)
                    if (bmp != null) {
                        val stream = java.io.ByteArrayOutputStream()
                        bmp.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                        stream.toByteArray()
                    } else rawBytes
                }
            } else null
        } catch (_: Exception) {
            null
        } finally {
            try { mmr.release() } catch (_: Exception) {}
        }
    } catch (_: Exception) {
        return null
    }
}

@OptIn(UnstableApi::class)
fun songToMediaItem(
    song: Song,
    context: Context? = null,
    showAlbumArt: Boolean = true,
    includeArtworkData: Boolean = false
): MediaItem {
    val downloadedFile = if (showAlbumArt && context != null) {
        val songUri = song.artworkUri
        val uriStr = songUri?.toString() ?: ""
        if (uriStr.contains("downloaded_art") || uriStr.contains("art_downloaded") || uriStr.contains("art_custom")) {
            if (songUri != null && songUri.scheme == "file" && songUri.path != null) {
                File(songUri.path!!)
            } else {
                AlbumArtDownloader.getDownloadedArtworkFile(context, song.artist, song.album)
            }
        } else {
            AlbumArtDownloader.getDownloadedArtworkFile(context, song.artist, song.album)
        }
    } else null
    fun toContentUriIfNeeded(file: File): Uri {
        return if (context != null) {
            try {
                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } catch (_: Exception) {
                Uri.fromFile(file)
            }
        } else {
            Uri.fromFile(file)
        }
    }

    val artUri = when {
        !showAlbumArt -> null
        downloadedFile != null -> toContentUriIfNeeded(downloadedFile)
        song.artworkUri != null && song.artworkUri.scheme == "file" && song.artworkUri.path != null && context != null -> {
            try {
                val file = File(song.artworkUri.path!!)
                if (file.exists()) toContentUriIfNeeded(file) else song.artworkUri
            } catch (_: Exception) {
                song.artworkUri
            }
        }
        else -> song.artworkUri
    }
    val artBytes = if (showAlbumArt && includeArtworkData && context != null) getArtworkBytesForSong(context, song) else null
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
            if (artBytes != null && artBytes.isNotEmpty()) {
                setArtworkData(artBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
            }
            if (artUri != null) {
                setArtworkUri(artUri)
            } else if (showAlbumArt && context != null) {
                val md = java.security.MessageDigest.getInstance("MD5")
                val digest = md.digest("${song.artist}-${song.album}".toByteArray())
                val hashKey = digest.joinToString("") { "%02x".format(it) }

                val cacheArtFile = File(context.cacheDir, "album_art/art_$hashKey.jpg")
                val embeddedArtFile = File(context.cacheDir, "embedded_art/art_embedded_$hashKey.jpg")
                val downloadedArtFile = File(context.filesDir, "downloaded_art/art_downloaded_$hashKey.jpg")
                val targetFile = when {
                    downloadedArtFile.exists() && downloadedArtFile.length() > 0L -> downloadedArtFile
                    embeddedArtFile.exists() && embeddedArtFile.length() > 0L -> embeddedArtFile
                    cacheArtFile.exists() && cacheArtFile.length() > 0L -> cacheArtFile
                    else -> null
                }
                if (targetFile != null) {
                    val targetUri = try {
                        androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            targetFile
                        )
                    } catch (_: Exception) {
                        Uri.fromFile(targetFile)
                    }
                    setArtworkUri(targetUri)
                }
            }
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
