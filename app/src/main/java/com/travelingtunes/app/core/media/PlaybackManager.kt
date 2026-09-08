package com.travelingtunes.app.core.media

import android.content.Context
import android.media.AudioManager
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.travelingtunes.app.core.datastore.SettingsDataStore
import com.travelingtunes.app.core.model.RepeatMode
import com.travelingtunes.app.core.model.ShuffleMode
import com.travelingtunes.app.core.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlaybackManager(
    private val context: Context,
    private val settingsDataStore: SettingsDataStore? = null
) {

    val player: ExoPlayer = MusicPlaybackService.getOrCreatePlayer(context)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPlaylist = MutableStateFlow<List<Song>>(emptyList())
    val currentPlaylist: StateFlow<List<Song>> = _currentPlaylist.asStateFlow()

    private val _actionHudText = MutableStateFlow<String?>(null)
    val actionHudText: StateFlow<String?> = _actionHudText.asStateFlow()

    private val _currentRating = MutableStateFlow(0) // 0 to 5 stars
    val currentRating: StateFlow<Int> = _currentRating.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _currentVolumeRatio = MutableStateFlow(0.5f)
    val currentVolumeRatio: StateFlow<Float> = _currentVolumeRatio.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _shuffleMode = MutableStateFlow(ShuffleMode.OFF)
    val shuffleMode: StateFlow<ShuffleMode> = _shuffleMode.asStateFlow()

    private var unshuffledPlaylist: List<Song> = emptyList()

    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.Job())

    init {
        updateVolumeRatio()
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val mediaId = mediaItem?.mediaId?.toLongOrNull()
                val previousSong = _currentSong.value
                val newSong = _currentPlaylist.value.find { it.id == mediaId }
                _currentSong.value = newSong
                _durationMs.value = player.duration.coerceAtLeast(0L)

                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && previousSong != null && newSong != null) {
                    when (_repeatMode.value) {
                        RepeatMode.ALBUM -> {
                            if (!newSong.album.equals(previousSong.album, ignoreCase = true)) {
                                val firstAlbumSongIndex = _currentPlaylist.value.indexOfFirst {
                                    it.album.equals(previousSong.album, ignoreCase = true)
                                }
                                if (firstAlbumSongIndex >= 0) {
                                    player.seekTo(firstAlbumSongIndex, 0L)
                                }
                            }
                        }
                        RepeatMode.ARTIST -> {
                            if (!newSong.artist.equals(previousSong.artist, ignoreCase = true)) {
                                val firstArtistSongIndex = _currentPlaylist.value.indexOfFirst {
                                    it.artist.equals(previousSong.artist, ignoreCase = true)
                                }
                                if (firstArtistSongIndex >= 0) {
                                    player.seekTo(firstArtistSongIndex, 0L)
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }
        })

        launchTicker()
    }

    private fun launchTicker() {
        scope.launch {
            var tickCount = 0
            while (isActive) {
                if (player.isPlaying) {
                    _currentPositionMs.value = player.currentPosition.coerceAtLeast(0L)
                    _durationMs.value = player.duration.coerceAtLeast(0L)
                }
                updateVolumeRatio()

                tickCount++
                if (tickCount % 10 == 0) { // Every 1 second
                    persistCurrentPlaybackState()
                }

                delay(100L)
            }
        }
    }

    fun persistCurrentPlaybackState() {
        val store = settingsDataStore ?: return
        val playlist = _currentPlaylist.value
        if (playlist.isEmpty()) return

        val song = _currentSong.value
        val songId = song?.id ?: -1L
        val songIndex = playlist.indexOfFirst { it.id == songId }.coerceAtLeast(0)
        val posMs = player.currentPosition.coerceAtLeast(0L)
        val currRepeat = _repeatMode.value
        val currShuffle = _shuffleMode.value

        scope.launch {
            store.savePlaybackState(
                queueIds = playlist.map { it.id },
                activeSongId = songId,
                activeSongIndex = songIndex,
                positionMs = posMs,
                isShuffle = currShuffle != ShuffleMode.OFF,
                isRepeat = currRepeat != RepeatMode.OFF,
                repeatMode = currRepeat,
                shuffleMode = currShuffle
            )
        }
    }

    fun restorePlaybackState(
        songs: List<Song>,
        startIndex: Int,
        positionMs: Long,
        shuffle: Boolean,
        repeat: Boolean,
        repeatMode: RepeatMode = if (repeat) RepeatMode.SONG else RepeatMode.OFF,
        shuffleMode: ShuffleMode = if (shuffle) ShuffleMode.ALL else ShuffleMode.OFF
    ) {
        if (songs.isEmpty()) return
        unshuffledPlaylist = songs
        _currentPlaylist.value = songs
        player.clearMediaItems()

        val mediaItems = songs.map { songToMediaItem(it) }

        player.setMediaItems(mediaItems)
        _repeatMode.value = repeatMode
        _shuffleMode.value = shuffleMode

        player.shuffleModeEnabled = false
        player.repeatMode = when (repeatMode) {
            RepeatMode.SONG -> Player.REPEAT_MODE_ONE
            RepeatMode.ALBUM, RepeatMode.ARTIST -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }

        val safeIndex = startIndex.coerceIn(0, songs.size - 1)
        player.seekTo(safeIndex, positionMs.coerceAtLeast(0L))
        player.prepare()

        _currentSong.value = songs.getOrNull(safeIndex)
        _currentPositionMs.value = positionMs.coerceAtLeast(0L)
        _durationMs.value = player.duration.coerceAtLeast(0L)
        AlbumArtCache.instance.preCacheSurroundingSongs(context, songs, safeIndex)
    }

    private fun updateVolumeRatio() {
        val curr = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (max > 0) {
            _currentVolumeRatio.value = (curr.toFloat() / max.toFloat()).coerceIn(0f, 1f)
        }
    }

    fun setPlaylistAndPlay(songs: List<Song>, startIndex: Int = 0, shuffle: Boolean = false) {
        if (songs.isEmpty()) return
        MusicPlaybackService.startService(context)

        unshuffledPlaylist = songs

        if (shuffle || _shuffleMode.value != ShuffleMode.OFF) {
            if (_shuffleMode.value == ShuffleMode.OFF) {
                _shuffleMode.value = ShuffleMode.ALL
            }

            val startSong = songs.getOrNull(startIndex)
            val activeQueue = if (startSong != null) {
                val remaining = songs.filter { it.id != startSong.id }.shuffled()
                listOf(startSong) + remaining
            } else {
                songs.shuffled()
            }

            _currentPlaylist.value = activeQueue
            player.clearMediaItems()
            player.setMediaItems(activeQueue.map { songToMediaItem(it) })
            player.shuffleModeEnabled = false
            player.seekTo(0, 0L)
            player.prepare()
            player.play()

            _currentSong.value = activeQueue.firstOrNull()
        } else {
            _currentPlaylist.value = songs
            player.clearMediaItems()
            player.setMediaItems(songs.map { songToMediaItem(it) })
            player.shuffleModeEnabled = false

            val safeIndex = startIndex.coerceIn(0, songs.size - 1)
            player.seekTo(safeIndex, 0L)
            player.prepare()
            player.play()

            _currentSong.value = songs.getOrNull(safeIndex)
        }

        AlbumArtCache.instance.preCacheSurroundingSongs(context, _currentPlaylist.value, 0)
        persistCurrentPlaybackState()
        showHudAction(if (shuffle || _shuffleMode.value != ShuffleMode.OFF) "Playing Playlist (Shuffled)" else "Playing Playlist")
    }

    fun playSongAtIndex(index: Int) {
        val playlist = _currentPlaylist.value
        if (index in playlist.indices) {
            MusicPlaybackService.startService(context)
            player.seekTo(index, 0L)
            player.play()
            _currentSong.value = playlist[index]
            AlbumArtCache.instance.preCacheSurroundingSongs(context, playlist, index)
            showHudAction("Playing: ${playlist[index].title}")
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
            showHudAction("Paused")
        } else {
            MusicPlaybackService.startService(context)
            if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0, 0)
            }
            player.play()
            showHudAction("Play")
        }
    }

    fun play() {
        MusicPlaybackService.startService(context)
        player.play()
        showHudAction("Play")
    }

    fun pause() {
        player.pause()
        showHudAction("Paused")
    }

    fun next() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
            showHudAction("Next Track")
        }
    }

    fun previous() {
        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
            showHudAction("Previous Track")
        }
    }

    fun restart() {
        player.seekTo(0L)
        showHudAction("Restart Track")
    }

    fun restartOrPrevious() {
        if (player.currentPosition > 3000L) {
            player.seekTo(0L)
            showHudAction("Restart Track")
        } else {
            previous()
        }
    }

    fun fastForward(deltaMs: Long = 10000L) {
        val newPos = (player.currentPosition + deltaMs).coerceAtMost(player.duration.coerceAtLeast(0L))
        player.seekTo(newPos)
        showHudAction("Fast Forward")
    }

    fun rewind(deltaMs: Long = 10000L) {
        val newPos = (player.currentPosition - deltaMs).coerceAtLeast(0L)
        player.seekTo(newPos)
        showHudAction("Rewind")
    }

    fun increaseVolume() {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0)
        updateVolumeRatio()
        showHudAction("Volume Up")
    }

    fun decreaseVolume() {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0)
        updateVolumeRatio()
        showHudAction("Volume Down")
    }

    fun adjustVolumeByDelta(deltaY: Float, heightPx: Float = 1000f) {
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (maxVol <= 0) return

        val sensitivity = 1.8f
        val ratioDelta = (-deltaY / heightPx.coerceAtLeast(200f)) * sensitivity

        val currentRatio = _currentVolumeRatio.value
        val newRatio = (currentRatio + ratioDelta).coerceIn(0f, 1f)
        _currentVolumeRatio.value = newRatio

        val targetVol = (newRatio * maxVol).toInt().coerceIn(0, maxVol)
        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (targetVol != currentVol) {
            try {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
            } catch (ignored: Exception) {}
        }
    }

    fun toggleRepeat() {
        val nextMode = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.SONG
            RepeatMode.SONG -> RepeatMode.ALBUM
            RepeatMode.ALBUM -> RepeatMode.ARTIST
            RepeatMode.ARTIST -> RepeatMode.GENRE
            RepeatMode.GENRE -> RepeatMode.OFF
        }
        setRepeatMode(nextMode)
    }

    fun setRepeatMode(mode: RepeatMode) {
        _repeatMode.value = mode
        applyRepeatAndShuffleQueue(keepCurrentSongFirst = true)
        showHudAction(mode.displayName)
        persistCurrentPlaybackState()
    }

    fun toggleShuffle() {
        val nextMode = when (_shuffleMode.value) {
            ShuffleMode.OFF -> ShuffleMode.ALL
            ShuffleMode.ALL -> ShuffleMode.GENRE
            ShuffleMode.GENRE -> ShuffleMode.ARTIST
            ShuffleMode.ARTIST -> ShuffleMode.ALBUM
            ShuffleMode.ALBUM -> ShuffleMode.OFF
        }
        setShuffleMode(nextMode)
    }

    fun setShuffleMode(mode: ShuffleMode) {
        _shuffleMode.value = mode
        applyRepeatAndShuffleQueue(keepCurrentSongFirst = true)
        showHudAction(mode.displayName)
        persistCurrentPlaybackState()
    }

    private fun applyRepeatAndShuffleQueue(keepCurrentSongFirst: Boolean = true) {
        val baseList = unshuffledPlaylist.ifEmpty { _currentPlaylist.value }
        if (baseList.isEmpty()) return

        val current = _currentSong.value

        var targetSongs = baseList

        when (_repeatMode.value) {
            RepeatMode.ALBUM -> {
                val albumName = current?.album ?: ""
                if (albumName.isNotBlank()) {
                    val matching = baseList.filter { it.album.equals(albumName, ignoreCase = true) }
                    if (matching.isNotEmpty()) {
                        targetSongs = matching.sortedBy { it.trackNumber }
                    }
                }
            }
            RepeatMode.ARTIST -> {
                val artistName = current?.artist ?: ""
                if (artistName.isNotBlank()) {
                    val matching = baseList.filter { it.artist.equals(artistName, ignoreCase = true) }
                    if (matching.isNotEmpty()) {
                        targetSongs = matching
                    }
                }
            }
            RepeatMode.GENRE -> {
                val genreName = current?.genre ?: ""
                if (genreName.isNotBlank()) {
                    val matching = baseList.filter { it.genre.equals(genreName, ignoreCase = true) }
                    if (matching.isNotEmpty()) {
                        targetSongs = matching
                    }
                }
            }
            RepeatMode.SONG, RepeatMode.OFF -> {
                when (_shuffleMode.value) {
                    ShuffleMode.GENRE -> {
                        val genreName = current?.genre ?: ""
                        if (genreName.isNotBlank()) {
                            val matching = baseList.filter { it.genre.equals(genreName, ignoreCase = true) }
                            if (matching.isNotEmpty()) targetSongs = matching
                        }
                    }
                    ShuffleMode.ARTIST -> {
                        val artistName = current?.artist ?: ""
                        if (artistName.isNotBlank()) {
                            val matching = baseList.filter { it.artist.equals(artistName, ignoreCase = true) }
                            if (matching.isNotEmpty()) targetSongs = matching
                        }
                    }
                    ShuffleMode.ALBUM -> {
                        val albumName = current?.album ?: ""
                        if (albumName.isNotBlank()) {
                            val matching = baseList.filter { it.album.equals(albumName, ignoreCase = true) }
                            if (matching.isNotEmpty()) targetSongs = matching
                        }
                    }
                    ShuffleMode.ALL, ShuffleMode.OFF -> {}
                }
            }
        }

        val activeQueue = if (_shuffleMode.value != ShuffleMode.OFF && _repeatMode.value != RepeatMode.SONG) {
            if (keepCurrentSongFirst && current != null && targetSongs.any { it.id == current.id }) {
                val remaining = targetSongs.filter { it.id != current.id }.shuffled()
                listOf(current) + remaining
            } else {
                targetSongs.shuffled()
            }
        } else {
            targetSongs
        }

        _currentPlaylist.value = activeQueue
        player.clearMediaItems()
        player.setMediaItems(activeQueue.map { songToMediaItem(it) })
        player.shuffleModeEnabled = false

        player.repeatMode = when (_repeatMode.value) {
            RepeatMode.SONG -> Player.REPEAT_MODE_ONE
            RepeatMode.ALBUM, RepeatMode.ARTIST, RepeatMode.GENRE -> Player.REPEAT_MODE_ALL
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
        }

        val currIndex = if (current != null) activeQueue.indexOfFirst { it.id == current.id }.coerceAtLeast(0) else 0
        val currPos = player.currentPosition.coerceAtLeast(0L)
        player.seekTo(currIndex, currPos)
        player.prepare()
    }

    fun playCurrentAlbum() {
        val song = _currentSong.value ?: return
        val currentAlbumName = song.album
        if (currentAlbumName.isBlank()) return

        val albumSongs = _currentPlaylist.value
            .filter { it.album.equals(currentAlbumName, ignoreCase = true) }
            .sortedBy { it.trackNumber }
            .ifEmpty { listOf(song) }

        _shuffleMode.value = ShuffleMode.OFF
        player.shuffleModeEnabled = false

        setPlaylistAndPlay(albumSongs, startIndex = 0, shuffle = false)
        _repeatMode.value = RepeatMode.ALBUM
        player.repeatMode = Player.REPEAT_MODE_ALL

        showHudAction("Playing Album: $currentAlbumName")
    }

    fun playCurrentArtist() {
        val song = _currentSong.value ?: return
        val currentArtistName = song.artist
        if (currentArtistName.isBlank()) return

        val artistSongs = _currentPlaylist.value
            .filter { it.artist.equals(currentArtistName, ignoreCase = true) }
            .ifEmpty { listOf(song) }

        _shuffleMode.value = ShuffleMode.OFF
        player.shuffleModeEnabled = false

        setPlaylistAndPlay(artistSongs, startIndex = 0, shuffle = false)
        _repeatMode.value = RepeatMode.ARTIST
        player.repeatMode = Player.REPEAT_MODE_ALL

        showHudAction("Playing Artist: $currentArtistName")
    }

    fun increaseRating() {
        if (_currentRating.value < 5) {
            _currentRating.value += 1
            showHudAction("Rating: ${_currentRating.value} Stars")
        }
    }

    fun decreaseRating() {
        if (_currentRating.value > 0) {
            _currentRating.value -= 1
            showHudAction("Rating: ${_currentRating.value} Stars")
        }
    }

    fun showHudAction(actionText: String) {
        _actionHudText.value = actionText
    }

    fun clearHudAction() {
        _actionHudText.value = null
    }

    fun release() {
        player.release()
    }
}
