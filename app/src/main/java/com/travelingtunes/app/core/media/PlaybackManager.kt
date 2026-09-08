package com.travelingtunes.app.core.media

import android.content.Context
import android.media.AudioManager
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.travelingtunes.app.core.datastore.SettingsDataStore
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

    val player: ExoPlayer = ExoPlayer.Builder(context).build()
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

    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.Job())

    init {
        updateVolumeRatio()
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val mediaId = mediaItem?.mediaId?.toLongOrNull()
                _currentSong.value = _currentPlaylist.value.find { it.id == mediaId }
                _durationMs.value = player.duration.coerceAtLeast(0L)
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
        val isShuffle = player.shuffleModeEnabled
        val isRepeat = player.repeatMode != Player.REPEAT_MODE_OFF

        scope.launch {
            store.savePlaybackState(
                queueIds = playlist.map { it.id },
                activeSongId = songId,
                activeSongIndex = songIndex,
                positionMs = posMs,
                isShuffle = isShuffle,
                isRepeat = isRepeat
            )
        }
    }

    fun restorePlaybackState(
        songs: List<Song>,
        startIndex: Int,
        positionMs: Long,
        shuffle: Boolean,
        repeat: Boolean
    ) {
        if (songs.isEmpty()) return
        _currentPlaylist.value = songs
        player.clearMediaItems()

        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(song.contentUri)
                .build()
        }

        player.setMediaItems(mediaItems)
        player.shuffleModeEnabled = shuffle
        player.repeatMode = if (repeat) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
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
        _currentPlaylist.value = songs
        player.clearMediaItems()

        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(song.contentUri)
                .build()
        }

        player.setMediaItems(mediaItems)
        player.shuffleModeEnabled = shuffle
        player.seekTo(startIndex, 0L)
        player.prepare()
        player.play()

        _currentSong.value = songs.getOrNull(startIndex)
        AlbumArtCache.instance.preCacheSurroundingSongs(context, songs, startIndex)
        persistCurrentPlaybackState()
        showHudAction(if (shuffle) "Playing Playlist (Shuffled)" else "Playing Playlist")
    }

    fun playSongAtIndex(index: Int) {
        val playlist = _currentPlaylist.value
        if (index in playlist.indices) {
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
            if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0, 0)
            }
            player.play()
            showHudAction("Play")
        }
    }

    fun play() {
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
        player.repeatMode = if (player.repeatMode == Player.REPEAT_MODE_OFF) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
        val isRepeat = player.repeatMode != Player.REPEAT_MODE_OFF
        showHudAction(if (isRepeat) "Repeat On" else "Repeat Off")
    }

    fun toggleShuffle() {
        player.shuffleModeEnabled = !player.shuffleModeEnabled
        showHudAction(if (player.shuffleModeEnabled) "Shuffle On" else "Shuffle Off")
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
