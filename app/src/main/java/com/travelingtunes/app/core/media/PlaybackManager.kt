package com.travelingtunes.app.core.media

import android.content.Context
import android.media.AudioManager
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.travelingtunes.app.core.database.MusicDatabase
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
import kotlinx.coroutines.withContext

class PlaybackManager(
    private val context: Context,
    private val settingsDataStore: SettingsDataStore? = null,
    private val musicDatabase: MusicDatabase? = null
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
    private var masterPlaylist: List<Song> = emptyList()

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    init {
        updateVolumeRatio()
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val playlist = _currentPlaylist.value
                val currentIndex = player.currentMediaItemIndex
                val song = if (currentIndex in playlist.indices) {
                    playlist[currentIndex]
                } else {
                    val mediaId = mediaItem?.mediaId?.toLongOrNull()
                    playlist.find { it.id == mediaId }
                }
                _currentSong.value = song
                _durationMs.value = player.duration.coerceAtLeast(0L)
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("PlaybackManager", "Player error encountered: ${error.message}", error)
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                    player.prepare()
                    player.play()
                } else {
                    _isPlaying.value = false
                }
            }
        })

        launchTicker()
    }

    private fun launchTicker() {
        scope.launch {
            var tickCount = 0
            while (isActive) {
                val pos = player.currentPosition.coerceAtLeast(0L)
                val dur = player.duration.coerceAtLeast(0L)
                if (player.isPlaying || kotlin.math.abs(pos - _currentPositionMs.value) > 1000L) {
                    _currentPositionMs.value = pos
                }
                _durationMs.value = dur
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
        shuffleMode: ShuffleMode = if (shuffle) ShuffleMode.SONGS else ShuffleMode.OFF
    ) {
        if (songs.isEmpty()) return
        unshuffledPlaylist = songs
        if (masterPlaylist.isEmpty()) {
            masterPlaylist = songs
        }
        _currentPlaylist.value = songs
        player.clearMediaItems()

        val mediaItems = songs.map { songToMediaItem(it) }

        player.setMediaItems(mediaItems)
        _repeatMode.value = repeatMode
        _shuffleMode.value = shuffleMode

        player.shuffleModeEnabled = false
        player.repeatMode = when (repeatMode) {
            RepeatMode.SONG -> Player.REPEAT_MODE_ONE
            RepeatMode.ALBUM, RepeatMode.ARTIST, RepeatMode.GENRE, RepeatMode.FOLDER -> Player.REPEAT_MODE_ALL
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

        val sortedSongs = sortLibrarySongs(songs)
        unshuffledPlaylist = sortedSongs
        if (masterPlaylist.isEmpty() || sortedSongs.size >= masterPlaylist.size) {
            masterPlaylist = sortedSongs
        }

        val isShuffle = shuffle || _shuffleMode.value != ShuffleMode.OFF
        if (isShuffle && _shuffleMode.value == ShuffleMode.OFF) {
            _shuffleMode.value = ShuffleMode.SONGS
        }

        player.repeatMode = when (_repeatMode.value) {
            RepeatMode.SONG -> Player.REPEAT_MODE_ONE
            RepeatMode.ALBUM, RepeatMode.ARTIST, RepeatMode.GENRE, RepeatMode.FOLDER -> Player.REPEAT_MODE_ALL
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
        }

        val safeIndex = if (isShuffle && startIndex == 0 && songs.size > 1) {
            songs.indices.random()
        } else {
            startIndex.coerceIn(0, songs.size - 1)
        }

        val activeQueue: List<Song>
        val playIndex: Int

        if (_shuffleMode.value != ShuffleMode.OFF) {
            val chosenSong = songs[safeIndex]
            val remainingSongs = songs.filter { it.id != chosenSong.id }
            val upcomingSongs = when (_shuffleMode.value) {
                ShuffleMode.SONGS -> remainingSongs.shuffled()
                ShuffleMode.ALBUMS -> {
                    val albumMap = remainingSongs.groupBy { it.album }
                    val shuffledAlbums = albumMap.keys.shuffled()
                    val list = mutableListOf<Song>()
                    for (album in shuffledAlbums) {
                        list.addAll(albumMap[album]?.sortedBy { it.trackNumber } ?: emptyList())
                    }
                    list
                }
                ShuffleMode.OFF -> remainingSongs.shuffled()
            }
            activeQueue = listOf(chosenSong) + upcomingSongs
            playIndex = 0
        } else {
            activeQueue = songs
            playIndex = safeIndex
        }

        _currentPlaylist.value = activeQueue
        player.clearMediaItems()
        player.setMediaItems(activeQueue.map { songToMediaItem(it) })
        player.shuffleModeEnabled = false

        player.seekTo(playIndex, 0L)
        player.prepare()
        player.play()

        _currentSong.value = activeQueue.getOrNull(playIndex)
        AlbumArtCache.instance.preCacheSurroundingSongs(context, activeQueue, playIndex)
        persistCurrentPlaybackState()
    }

    fun shuffleAllSongs() {
        MusicPlaybackService.startService(context)
        scope.launch(Dispatchers.IO) {
            val dbSongs = musicDatabase?.getAllSongs() ?: emptyList()
            val allSongs = if (dbSongs.isNotEmpty()) dbSongs else masterPlaylist.ifEmpty { unshuffledPlaylist.ifEmpty { _currentPlaylist.value } }
            if (allSongs.isEmpty()) return@launch

            masterPlaylist = allSongs
            unshuffledPlaylist = allSongs

            withContext(Dispatchers.Main) {
                _repeatMode.value = RepeatMode.OFF
                player.repeatMode = Player.REPEAT_MODE_OFF

                _shuffleMode.value = ShuffleMode.SONGS

                val shuffledList = allSongs.shuffled()
                _currentPlaylist.value = shuffledList

                player.clearMediaItems()
                player.setMediaItems(shuffledList.map { songToMediaItem(it) })
                player.shuffleModeEnabled = false

                player.seekTo(0, 0L)
                player.prepare()
                player.play()

                _currentSong.value = shuffledList.firstOrNull()
                AlbumArtCache.instance.preCacheSurroundingSongs(context, shuffledList, 0)
                persistCurrentPlaybackState()
            }
        }
    }

    fun playSongAtIndex(index: Int) {
        val playlist = _currentPlaylist.value
        if (index in playlist.indices) {
            MusicPlaybackService.startService(context)
            player.seekTo(index, 0L)
            player.play()
            _currentSong.value = playlist[index]
            AlbumArtCache.instance.preCacheSurroundingSongs(context, playlist, index)
            persistCurrentPlaybackState()
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            MusicPlaybackService.startService(context)
            if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0, 0)
            }
            player.play()
        }
        persistCurrentPlaybackState()
    }

    fun play() {
        MusicPlaybackService.startService(context)
        player.play()
        persistCurrentPlaybackState()
    }

    fun pause() {
        player.pause()
        persistCurrentPlaybackState()
    }

    fun next() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
            persistCurrentPlaybackState()
        }
    }

    fun previous() {
        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
            persistCurrentPlaybackState()
        }
    }

    fun restart() {
        player.seekTo(0L)
    }

    fun restartOrPrevious() {
        if (player.currentPosition > 3000L) {
            player.seekTo(0L)
        } else {
            previous()
        }
    }

    fun seekByDelta(deltaMs: Long) {
        val duration = _durationMs.value.coerceAtLeast(0L)
        val currentPos = _currentPositionMs.value
        val targetPos = (currentPos + deltaMs).coerceIn(0L, duration)

        _currentPositionMs.value = targetPos
        player.seekTo(targetPos)

        if (duration > 0L) {
            val currentSec = targetPos / 1000L
            val durSec = duration / 1000L
            val formatted = String.format(java.util.Locale.US, "%02d:%02d / %02d:%02d", currentSec / 60, currentSec % 60, durSec / 60, durSec % 60)
            showHudAction(formatted)
        }
    }

    fun seekToPosition(positionMs: Long) {
        val duration = _durationMs.value.coerceAtLeast(0L)
        val targetPos = positionMs.coerceIn(0L, duration)

        _currentPositionMs.value = targetPos
        player.seekTo(targetPos)
    }

    fun fastForward(deltaMs: Long = 10000L) {
        seekByDelta(deltaMs)
    }

    fun rewind(deltaMs: Long = 10000L) {
        seekByDelta(-deltaMs)
    }

    fun increaseVolume() {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0)
        updateVolumeRatio()
    }

    fun decreaseVolume() {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0)
        updateVolumeRatio()
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
            RepeatMode.GENRE -> RepeatMode.FOLDER
            RepeatMode.FOLDER -> RepeatMode.OFF
        }
        setRepeatMode(nextMode)
    }

    fun setRepeatMode(mode: RepeatMode) {
        _repeatMode.value = mode
        updateQueuePreservingCurrentSong()
        persistCurrentPlaybackState()
    }

    fun toggleShuffle() {
        val nextMode = when (_shuffleMode.value) {
            ShuffleMode.OFF -> ShuffleMode.SONGS
            ShuffleMode.SONGS -> ShuffleMode.ALBUMS
            ShuffleMode.ALBUMS -> ShuffleMode.OFF
        }
        setShuffleMode(nextMode)
    }

    fun setShuffleMode(mode: ShuffleMode) {
        _shuffleMode.value = mode
        updateQueuePreservingCurrentSong()
        persistCurrentPlaybackState()
    }

    fun sortLibrarySongs(songs: List<Song>): List<Song> {
        return songs.sortedWith(
            compareBy(
                String.CASE_INSENSITIVE_ORDER
            ) { song: Song -> song.artist }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { song -> song.album }
                .thenBy { song -> if (song.trackNumber > 0) song.trackNumber else Int.MAX_VALUE }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { song -> song.title }
        )
    }

    private fun updateQueuePreservingCurrentSong() {
        val current = _currentSong.value
        val playlist = _currentPlaylist.value
        val rawBase = unshuffledPlaylist.ifEmpty { masterPlaylist.ifEmpty { playlist } }
        val baseList = sortLibrarySongs(rawBase)

        player.repeatMode = when (_repeatMode.value) {
            RepeatMode.SONG -> Player.REPEAT_MODE_ONE
            RepeatMode.ALBUM, RepeatMode.ARTIST, RepeatMode.GENRE, RepeatMode.FOLDER -> Player.REPEAT_MODE_ALL
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
        }

        if (baseList.isEmpty() || current == null || player.mediaItemCount == 0) {
            return
        }

        val currentIndex = player.currentMediaItemIndex.coerceIn(0, (playlist.size - 1).coerceAtLeast(0))
        val priorSongs = if (playlist.isNotEmpty() && currentIndex in playlist.indices) {
            playlist.take(currentIndex)
        } else emptyList()

        var targetSongs = baseList
        when (_repeatMode.value) {
            RepeatMode.ALBUM -> {
                val albumName = current.album
                if (albumName.isNotBlank()) {
                    val matching = baseList.filter { it.album.equals(albumName, ignoreCase = true) }
                    if (matching.isNotEmpty()) targetSongs = matching
                }
            }
            RepeatMode.ARTIST -> {
                val artistName = current.artist
                if (artistName.isNotBlank()) {
                    val matching = baseList.filter { it.artist.equals(artistName, ignoreCase = true) }
                    if (matching.isNotEmpty()) targetSongs = matching
                }
            }
            RepeatMode.GENRE -> {
                val genreName = current.genre
                if (genreName.isNotBlank()) {
                    val matching = baseList.filter { it.genre.equals(genreName, ignoreCase = true) }
                    if (matching.isNotEmpty()) targetSongs = matching
                }
            }
            RepeatMode.FOLDER -> {
                val folderPath = current.folderPath
                if (folderPath.isNotBlank()) {
                    val matching = baseList.filter {
                        it.folderPath.equals(folderPath, ignoreCase = true) ||
                        it.folderPath.startsWith(folderPath, ignoreCase = true)
                    }
                    if (matching.isNotEmpty()) targetSongs = matching
                }
            }
            RepeatMode.SONG, RepeatMode.OFF -> {}
        }

        val priorIds = priorSongs.map { it.id }.toSet()
        val candidateSongs = targetSongs.filter { !priorIds.contains(it.id) }

        val upcomingSongs = when (_shuffleMode.value) {
            ShuffleMode.OFF -> {
                val currentIdxInCandidates = candidateSongs.indexOfFirst { it.id == current.id }
                if (currentIdxInCandidates != -1) {
                    val after = candidateSongs.drop(currentIdxInCandidates + 1)
                    val before = candidateSongs.take(currentIdxInCandidates)
                    after + before
                } else {
                    candidateSongs.filter { it.id != current.id }
                }
            }
            ShuffleMode.SONGS -> candidateSongs.filter { it.id != current.id }.shuffled()
            ShuffleMode.ALBUMS -> {
                val remaining = candidateSongs.filter { it.id != current.id }
                val albumMap = remaining.groupBy { it.album }
                val shuffledAlbums = albumMap.keys.shuffled()
                val list = mutableListOf<Song>()
                for (album in shuffledAlbums) {
                    list.addAll(albumMap[album] ?: emptyList())
                }
                list
            }
        }

        val activeQueue = priorSongs + listOf(current) + upcomingSongs
        _currentPlaylist.value = activeQueue

        player.shuffleModeEnabled = false

        // Seamlessly update ExoPlayer's queue
        val currentPos = player.currentPosition.coerceAtLeast(0L)
        val mediaItems = activeQueue.map { songToMediaItem(it) }
        val newCurrentIndex = priorSongs.size
        player.setMediaItems(mediaItems, newCurrentIndex, currentPos)
    }

    fun playCurrentAlbum() {
        val song = _currentSong.value ?: return
        if (song.album.isBlank()) return
        setRepeatMode(RepeatMode.ALBUM)
    }

    fun playCurrentArtist() {
        val song = _currentSong.value ?: return
        if (song.artist.isBlank()) return
        setRepeatMode(RepeatMode.ARTIST)
    }

    fun playCurrentFolder() {
        val song = _currentSong.value ?: return
        if (song.folderPath.isBlank()) return
        setRepeatMode(RepeatMode.FOLDER)
    }

    fun increaseRating() {
        if (_currentRating.value < 5) {
            _currentRating.value += 1
        }
    }

    fun decreaseRating() {
        if (_currentRating.value > 0) {
            _currentRating.value -= 1
        }
    }

    fun addSongToQueue(song: Song) {
        addSongsToQueue(listOf(song))
    }

    fun addSongsToQueue(songs: List<Song>) {
        if (songs.isEmpty()) return
        val wasEmpty = _currentPlaylist.value.isEmpty()
        val updated = _currentPlaylist.value + songs
        _currentPlaylist.value = updated
        player.addMediaItems(songs.map { songToMediaItem(it) })
        if (wasEmpty) {
            player.prepare()
            _currentSong.value = songs.firstOrNull()
        }
        persistCurrentPlaybackState()
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val list = _currentPlaylist.value.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices || fromIndex == toIndex) return
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        _currentPlaylist.value = list
        player.moveMediaItem(fromIndex, toIndex)
        persistCurrentPlaybackState()
    }

    fun removeQueueItem(index: Int) {
        val list = _currentPlaylist.value.toMutableList()
        if (index !in list.indices) return
        list.removeAt(index)
        _currentPlaylist.value = list
        player.removeMediaItem(index)
        if (_currentPlaylist.value.isEmpty()) {
            _currentSong.value = null
        } else {
            val newIndex = player.currentMediaItemIndex.coerceIn(0, _currentPlaylist.value.size - 1)
            _currentSong.value = _currentPlaylist.value.getOrNull(newIndex)
        }
        persistCurrentPlaybackState()
    }

    fun showHudAction(actionText: String) {
        // Disabled pop-up announcements per user requirement
        _actionHudText.value = null
    }

    fun clearHudAction() {
        _actionHudText.value = null
    }

    fun release() {
        player.release()
    }
}
