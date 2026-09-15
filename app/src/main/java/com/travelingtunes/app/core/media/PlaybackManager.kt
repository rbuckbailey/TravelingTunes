package com.travelingtunes.app.core.media

import android.content.Context
import android.media.AudioManager
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.travelingtunes.app.core.database.MusicDatabase
import com.travelingtunes.app.core.datastore.SettingsDataStore
import com.travelingtunes.app.core.model.NormalizationMode
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

    companion object {
        @Volatile
        private var instance: PlaybackManager? = null

        fun getInstance(
            context: Context,
            settingsDataStore: SettingsDataStore? = null,
            musicDatabase: MusicDatabase? = null
        ): PlaybackManager {
            return instance ?: synchronized(this) {
                instance ?: PlaybackManager(
                    context.applicationContext,
                    settingsDataStore ?: SettingsDataStore(context.applicationContext),
                    musicDatabase ?: MusicDatabase(context.applicationContext)
                ).also { instance = it }
            }
        }
    }

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

    private val _normalizationMode = MutableStateFlow(NormalizationMode.ALBUM)
    val normalizationMode: StateFlow<NormalizationMode> = _normalizationMode.asStateFlow()

    private val _lastMediaItemTransitionReason = MutableStateFlow<Int>(Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED)
    val lastMediaItemTransitionReason: StateFlow<Int> = _lastMediaItemTransitionReason.asStateFlow()

    private var unshuffledPlaylist: List<Song> = emptyList()
    private var masterPlaylist: List<Song> = emptyList()

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var consecutiveErrorCount = 0
    private var lastErrorTimestampMs = 0L

    init {
        instance = this
        updateVolumeRatio()
        if (settingsDataStore != null) {
            scope.launch {
                settingsDataStore.normalizationModeFlow.collect { mode ->
                    _normalizationMode.value = mode
                    applyNormalizationModifier()
                }
            }
        }
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    consecutiveErrorCount = 0
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _lastMediaItemTransitionReason.value = reason
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
                applyNormalizationModifier(song)
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("PlaybackManager", "Player error encountered: ${error.message}", error)
                val now = System.currentTimeMillis()
                if (now - lastErrorTimestampMs < 3000L) {
                    consecutiveErrorCount++
                } else {
                    consecutiveErrorCount = 1
                }
                lastErrorTimestampMs = now

                if (consecutiveErrorCount >= 3) {
                    android.util.Log.w("PlaybackManager", "Circuit breaker triggered: Stopping player to prevent audio server lockup.")
                    _isPlaying.value = false
                    _actionHudText.value = "Audio engine busy"
                    player.stop()
                    consecutiveErrorCount = 0
                    return
                }

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

    fun applyNormalizationModifier(song: Song? = _currentSong.value, mode: NormalizationMode = _normalizationMode.value) {
        if (song == null) {
            player.volume = 1.0f
            return
        }
        val modifier = when (mode) {
            NormalizationMode.ALBUM -> if (song.albumGain > 0.001f) song.albumGain else 1.0f
            NormalizationMode.TRACK -> if (song.trackGain > 0.001f) song.trackGain else 1.0f
            NormalizationMode.OFF -> 1.0f
        }
        player.volume = modifier.coerceIn(0.0f, 2.0f)
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

        val mediaItems = songs.map { songToMediaItem(it) }
        val safeIndex = startIndex.coerceIn(0, songs.size - 1)
        val safePos = positionMs.coerceAtLeast(0L)

        _repeatMode.value = repeatMode
        _shuffleMode.value = shuffleMode

        player.shuffleModeEnabled = false
        player.repeatMode = when (repeatMode) {
            RepeatMode.SONG -> Player.REPEAT_MODE_ONE
            RepeatMode.ALBUM, RepeatMode.ARTIST, RepeatMode.GENRE, RepeatMode.FOLDER -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }

        player.setMediaItems(mediaItems, safeIndex, safePos)
        player.prepare()

        _currentSong.value = songs.getOrNull(safeIndex)
        _currentPositionMs.value = safePos
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

    fun getAlbumKey(song: Song): String {
        return if (song.albumId > 0) {
            "id_${song.albumId}"
        } else {
            "${song.artist.trim().lowercase()}_${song.album.trim().lowercase()}"
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

        val activeQueue: List<Song>
        val playIndex: Int

        if (_shuffleMode.value != ShuffleMode.OFF) {
            val sourceList = if (songs.map { getAlbumKey(it) }.distinct().size > 1) songs else masterPlaylist.ifEmpty { songs }

            when (_shuffleMode.value) {
                ShuffleMode.SONGS -> {
                    val safeIndex = if (shuffle && startIndex == 0 && songs.size > 1) {
                        songs.indices.random()
                    } else {
                        startIndex.coerceIn(0, songs.size - 1)
                    }
                    val chosenSong = songs[safeIndex]
                    val remainingSongs = songs.filter { it.id != chosenSong.id }
                    activeQueue = listOf(chosenSong) + remainingSongs.shuffled()
                    playIndex = 0
                }
                ShuffleMode.ALBUMS -> {
                    val albumsMap = sourceList.groupBy { getAlbumKey(it) }
                    val queue = mutableListOf<Song>()

                    if (shuffle && startIndex == 0 && albumsMap.size > 1) {
                        val shuffledAlbumKeys = albumsMap.keys.shuffled()
                        for (albumKey in shuffledAlbumKeys) {
                            val albumSongs = albumsMap[albumKey] ?: emptyList()
                            queue.addAll(sortAlbumSongs(albumSongs))
                        }
                        activeQueue = queue
                        playIndex = 0
                    } else {
                        val safeIndex = startIndex.coerceIn(0, songs.size - 1)
                        val chosenSong = songs[safeIndex]
                        val chosenAlbumKey = getAlbumKey(chosenSong)

                        val activeAlbumSongs = sortAlbumSongs(albumsMap[chosenAlbumKey] ?: sourceList.filter { getAlbumKey(it) == chosenAlbumKey })
                        val chosenIdx = activeAlbumSongs.indexOfFirst { it.id == chosenSong.id }

                        val otherAlbumKeys = (albumsMap.keys - chosenAlbumKey).shuffled()

                        queue.addAll(activeAlbumSongs)
                        for (albumKey in otherAlbumKeys) {
                            val albumSongs = albumsMap[albumKey] ?: emptyList()
                            queue.addAll(sortAlbumSongs(albumSongs))
                        }
                        activeQueue = queue
                        playIndex = if (chosenIdx != -1) chosenIdx else 0
                    }
                }
                ShuffleMode.OFF -> {
                    val safeIndex = startIndex.coerceIn(0, songs.size - 1)
                    activeQueue = songs
                    playIndex = safeIndex
                }
            }
        } else {
            activeQueue = songs
            playIndex = startIndex.coerceIn(0, songs.size - 1)
        }

        _currentPlaylist.value = activeQueue
        _currentSong.value = activeQueue.getOrNull(playIndex)

        player.shuffleModeEnabled = false
        player.setMediaItems(activeQueue.map { songToMediaItem(it) }, playIndex, 0L)
        player.prepare()
        player.play()

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
                _currentSong.value = shuffledList.firstOrNull()

                player.shuffleModeEnabled = false
                player.setMediaItems(shuffledList.map { songToMediaItem(it) }, 0, 0L)
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

    private var isContinuousSeeking = false

    fun seekByDeltaContinuous(deltaMs: Long) {
        isContinuousSeeking = true
        val duration = _durationMs.value.coerceAtLeast(0L)
        val currentPos = _currentPositionMs.value
        val targetPos = (currentPos + deltaMs).coerceIn(0L, duration)

        _currentPositionMs.value = targetPos

        if (duration > 0L) {
            val currentSec = targetPos / 1000L
            val durSec = duration / 1000L
            val formatted = String.format(java.util.Locale.US, "%02d:%02d / %02d:%02d", currentSec / 60, currentSec % 60, durSec / 60, durSec % 60)
            showHudAction(formatted)
        }
    }

    fun commitContinuousSeek() {
        if (isContinuousSeeking) {
            isContinuousSeeking = false
            val finalPos = _currentPositionMs.value
            try {
                player.seekTo(finalPos)
            } catch (ignored: Exception) {}
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

    fun sortAlbumSongs(songs: List<Song>): List<Song> {
        return songs.sortedWith(
            compareBy<Song> { if (it.discNumber > 0) it.discNumber else 1 }
                .thenBy { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
        )
    }

    fun sortLibrarySongs(songs: List<Song>): List<Song> {
        return songs.sortedWith(
            compareBy(
                String.CASE_INSENSITIVE_ORDER
            ) { song: Song -> song.artist }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { song -> song.album }
                .thenBy { song -> if (song.discNumber > 0) song.discNumber else 1 }
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
                val currentKey = getAlbumKey(current)
                val matching = baseList.filter { getAlbumKey(it) == currentKey }
                if (matching.isNotEmpty()) targetSongs = matching
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

        val activeQueue: List<Song>
        val newCurrentIndex: Int

        when (_shuffleMode.value) {
            ShuffleMode.OFF -> {
                val currentIdxInCandidates = candidateSongs.indexOfFirst { it.id == current.id }
                val upcoming = if (currentIdxInCandidates != -1) {
                    val after = candidateSongs.drop(currentIdxInCandidates + 1)
                    val before = candidateSongs.take(currentIdxInCandidates)
                    after + before
                } else {
                    candidateSongs.filter { it.id != current.id }
                }
                activeQueue = priorSongs + listOf(current) + upcoming
                newCurrentIndex = priorSongs.size
            }
            ShuffleMode.SONGS -> {
                val upcoming = candidateSongs.filter { it.id != current.id }.shuffled()
                activeQueue = priorSongs + listOf(current) + upcoming
                newCurrentIndex = priorSongs.size
            }
            ShuffleMode.ALBUMS -> {
                val currentAlbumKey = getAlbumKey(current)
                val currentAlbumAllSongs = sortAlbumSongs(targetSongs.filter { getAlbumKey(it) == currentAlbumKey })
                val currentIdx = currentAlbumAllSongs.indexOfFirst { it.id == current.id }

                val currentAlbumAfter = if (currentIdx != -1) currentAlbumAllSongs.drop(currentIdx + 1) else emptyList()
                val currentAlbumBeforeUnplayed = if (currentIdx != -1) {
                    currentAlbumAllSongs.take(currentIdx).filter { song -> priorSongs.none { it.id == song.id } }
                } else emptyList()

                val otherAlbumsMap = candidateSongs
                    .filter { getAlbumKey(it) != currentAlbumKey }
                    .groupBy { getAlbumKey(it) }

                val shuffledAlbumKeys = otherAlbumsMap.keys.shuffled()

                val upcoming = mutableListOf<Song>()
                upcoming.addAll(currentAlbumAfter)

                for (albumKey in shuffledAlbumKeys) {
                    val songsInAlbum = otherAlbumsMap[albumKey] ?: emptyList()
                    upcoming.addAll(sortAlbumSongs(songsInAlbum))
                }

                activeQueue = priorSongs + currentAlbumBeforeUnplayed + listOf(current) + upcoming
                newCurrentIndex = priorSongs.size + currentAlbumBeforeUnplayed.size
            }
        }

        _currentPlaylist.value = activeQueue

        player.shuffleModeEnabled = false

        // Seamlessly update ExoPlayer's queue
        val currentPos = player.currentPosition.coerceAtLeast(0L)
        val mediaItems = activeQueue.map { songToMediaItem(it) }
        player.setMediaItems(mediaItems, newCurrentIndex, currentPos)
    }

    fun playCurrentAlbum() {
        val current = _currentSong.value ?: return
        if (current.album.isBlank()) return
        val currentKey = getAlbumKey(current)

        val rawBase = unshuffledPlaylist.ifEmpty { masterPlaylist.ifEmpty { _currentPlaylist.value } }
        val baseList = sortLibrarySongs(rawBase)
        val albumSongs = sortAlbumSongs(baseList.filter { getAlbumKey(it) == currentKey })
        val firstTrack = albumSongs.firstOrNull() ?: current

        _shuffleMode.value = ShuffleMode.OFF
        _currentSong.value = firstTrack
        setRepeatMode(RepeatMode.ALBUM)

        val queueIndex = _currentPlaylist.value.indexOfFirst { it.id == firstTrack.id }
        if (queueIndex != -1) {
            player.seekTo(queueIndex, 0L)
            player.play()
        }
    }

    fun playCurrentArtist() {
        val current = _currentSong.value ?: return
        val artistName = current.artist
        if (artistName.isBlank()) return

        val rawBase = unshuffledPlaylist.ifEmpty { masterPlaylist.ifEmpty { _currentPlaylist.value } }
        val baseList = sortLibrarySongs(rawBase)
        val artistSongs = baseList.filter { it.artist.equals(artistName, ignoreCase = true) }
        val firstTrack = artistSongs.firstOrNull() ?: current

        _shuffleMode.value = ShuffleMode.OFF
        _currentSong.value = firstTrack
        setRepeatMode(RepeatMode.ARTIST)

        val queueIndex = _currentPlaylist.value.indexOfFirst { it.id == firstTrack.id }
        if (queueIndex != -1) {
            player.seekTo(queueIndex, 0L)
            player.play()
        }
    }

    fun playCurrentFolder() {
        val current = _currentSong.value ?: return
        val folderPath = current.folderPath
        if (folderPath.isBlank()) return

        val rawBase = unshuffledPlaylist.ifEmpty { masterPlaylist.ifEmpty { _currentPlaylist.value } }
        val baseList = sortLibrarySongs(rawBase)
        val folderSongs = baseList.filter {
            it.folderPath.equals(folderPath, ignoreCase = true) ||
            it.folderPath.startsWith(folderPath, ignoreCase = true)
        }
        val firstTrack = folderSongs.firstOrNull() ?: current

        _shuffleMode.value = ShuffleMode.OFF
        _currentSong.value = firstTrack
        setRepeatMode(RepeatMode.FOLDER)

        val queueIndex = _currentPlaylist.value.indexOfFirst { it.id == firstTrack.id }
        if (queueIndex != -1) {
            player.seekTo(queueIndex, 0L)
            player.play()
        }
    }

    fun nextAlbum() {
        val current = _currentSong.value ?: return
        val rawBase = unshuffledPlaylist.ifEmpty { masterPlaylist.ifEmpty { _currentPlaylist.value } }
        val baseList = sortLibrarySongs(rawBase)
        if (baseList.isEmpty()) return

        val scopeSongs = when (_repeatMode.value) {
            RepeatMode.ARTIST -> {
                val artistName = current.artist
                if (artistName.isNotBlank()) {
                    baseList.filter { it.artist.equals(artistName, ignoreCase = true) }
                } else baseList
            }
            RepeatMode.GENRE -> {
                val genreName = current.genre
                if (genreName.isNotBlank()) {
                    baseList.filter { it.genre.equals(genreName, ignoreCase = true) }
                } else baseList
            }
            else -> baseList
        }

        val albumsMap = scopeSongs.groupBy { getAlbumKey(it) }
        val sortedAlbumKeys = albumsMap.keys.sortedWith(
            compareBy<String, String>(String.CASE_INSENSITIVE_ORDER) { key -> albumsMap[key]?.firstOrNull()?.artist ?: "" }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { key -> albumsMap[key]?.firstOrNull()?.album ?: "" }
        )

        if (sortedAlbumKeys.isEmpty()) return

        val currentKey = getAlbumKey(current)
        val currentIdx = sortedAlbumKeys.indexOf(currentKey)

        val nextIdx = if (currentIdx != -1) {
            (currentIdx + 1) % sortedAlbumKeys.size
        } else 0

        val nextAlbumKey = sortedAlbumKeys[nextIdx]
        val nextAlbumSongs = sortAlbumSongs(albumsMap[nextAlbumKey] ?: emptyList())
        val firstTrack = nextAlbumSongs.firstOrNull() ?: return

        _currentSong.value = firstTrack
        updateQueuePreservingCurrentSong()

        val queueIndex = _currentPlaylist.value.indexOfFirst { it.id == firstTrack.id }
        if (queueIndex != -1) {
            player.seekTo(queueIndex, 0L)
            player.play()
        }
    }

    fun previousAlbum() {
        val current = _currentSong.value ?: return
        val rawBase = unshuffledPlaylist.ifEmpty { masterPlaylist.ifEmpty { _currentPlaylist.value } }
        val baseList = sortLibrarySongs(rawBase)
        if (baseList.isEmpty()) return

        val scopeSongs = when (_repeatMode.value) {
            RepeatMode.ARTIST -> {
                val artistName = current.artist
                if (artistName.isNotBlank()) {
                    baseList.filter { it.artist.equals(artistName, ignoreCase = true) }
                } else baseList
            }
            RepeatMode.GENRE -> {
                val genreName = current.genre
                if (genreName.isNotBlank()) {
                    baseList.filter { it.genre.equals(genreName, ignoreCase = true) }
                } else baseList
            }
            else -> baseList
        }

        val albumsMap = scopeSongs.groupBy { getAlbumKey(it) }
        val sortedAlbumKeys = albumsMap.keys.sortedWith(
            compareBy<String, String>(String.CASE_INSENSITIVE_ORDER) { key -> albumsMap[key]?.firstOrNull()?.artist ?: "" }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { key -> albumsMap[key]?.firstOrNull()?.album ?: "" }
        )

        if (sortedAlbumKeys.isEmpty()) return

        val currentKey = getAlbumKey(current)
        val currentIdx = sortedAlbumKeys.indexOf(currentKey)

        val currentAlbumSongs = sortAlbumSongs(albumsMap[currentKey] ?: emptyList())
        val currentAlbumTrack1 = currentAlbumSongs.firstOrNull()

        val isPastThreshold = player.currentPosition > 3000L && currentAlbumTrack1 != null && current.id != currentAlbumTrack1.id

        val prevIdx = if (isPastThreshold) {
            currentIdx.coerceAtLeast(0)
        } else if (currentIdx != -1) {
            if (currentIdx - 1 < 0) sortedAlbumKeys.size - 1 else currentIdx - 1
        } else 0

        val prevAlbumKey = sortedAlbumKeys[prevIdx]
        val prevAlbumSongs = sortAlbumSongs(albumsMap[prevAlbumKey] ?: emptyList())
        val targetTrack = prevAlbumSongs.firstOrNull() ?: return

        _currentSong.value = targetTrack
        updateQueuePreservingCurrentSong()

        val queueIndex = _currentPlaylist.value.indexOfFirst { it.id == targetTrack.id }
        if (queueIndex != -1) {
            player.seekTo(queueIndex, 0L)
            player.play()
        }
    }

    fun getNextSong(): Song? {
        val playlist = _currentPlaylist.value
        val current = _currentSong.value ?: return null
        val idx = playlist.indexOfFirst { it.id == current.id }
        return if (idx != -1 && idx + 1 in playlist.indices) playlist[idx + 1] else playlist.firstOrNull()
    }

    fun getPreviousSong(): Song? {
        val playlist = _currentPlaylist.value
        val current = _currentSong.value ?: return null
        val idx = playlist.indexOfFirst { it.id == current.id }
        return if (idx != -1 && idx - 1 in playlist.indices) playlist[idx - 1] else playlist.lastOrNull()
    }

    fun getNextAlbumFirstTrack(): Song? {
        val current = _currentSong.value ?: return null
        val rawBase = unshuffledPlaylist.ifEmpty { masterPlaylist.ifEmpty { _currentPlaylist.value } }
        val baseList = sortLibrarySongs(rawBase)
        if (baseList.isEmpty()) return null

        val scopeSongs = when (_repeatMode.value) {
            RepeatMode.ARTIST -> {
                val artistName = current.artist
                if (artistName.isNotBlank()) baseList.filter { it.artist.equals(artistName, ignoreCase = true) } else baseList
            }
            RepeatMode.GENRE -> {
                val genreName = current.genre
                if (genreName.isNotBlank()) baseList.filter { it.genre.equals(genreName, ignoreCase = true) } else baseList
            }
            else -> baseList
        }

        val albumsMap = scopeSongs.groupBy { getAlbumKey(it) }
        val sortedAlbumKeys = albumsMap.keys.sortedWith(
            compareBy<String, String>(String.CASE_INSENSITIVE_ORDER) { key -> albumsMap[key]?.firstOrNull()?.artist ?: "" }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { key -> albumsMap[key]?.firstOrNull()?.album ?: "" }
        )
        if (sortedAlbumKeys.isEmpty()) return null

        val currentKey = getAlbumKey(current)
        val currentIdx = sortedAlbumKeys.indexOf(currentKey)
        val nextIdx = if (currentIdx != -1) (currentIdx + 1) % sortedAlbumKeys.size else 0

        val nextAlbumKey = sortedAlbumKeys[nextIdx]
        val nextAlbumSongs = sortAlbumSongs(albumsMap[nextAlbumKey] ?: emptyList())
        return nextAlbumSongs.firstOrNull()
    }

    fun getPreviousAlbumFirstTrack(): Song? {
        val current = _currentSong.value ?: return null
        val rawBase = unshuffledPlaylist.ifEmpty { masterPlaylist.ifEmpty { _currentPlaylist.value } }
        val baseList = sortLibrarySongs(rawBase)
        if (baseList.isEmpty()) return null

        val scopeSongs = when (_repeatMode.value) {
            RepeatMode.ARTIST -> {
                val artistName = current.artist
                if (artistName.isNotBlank()) baseList.filter { it.artist.equals(artistName, ignoreCase = true) } else baseList
            }
            RepeatMode.GENRE -> {
                val genreName = current.genre
                if (genreName.isNotBlank()) baseList.filter { it.genre.equals(genreName, ignoreCase = true) } else baseList
            }
            else -> baseList
        }

        val albumsMap = scopeSongs.groupBy { getAlbumKey(it) }
        val sortedAlbumKeys = albumsMap.keys.sortedWith(
            compareBy<String, String>(String.CASE_INSENSITIVE_ORDER) { key -> albumsMap[key]?.firstOrNull()?.artist ?: "" }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { key -> albumsMap[key]?.firstOrNull()?.album ?: "" }
        )
        if (sortedAlbumKeys.isEmpty()) return null

        val currentKey = getAlbumKey(current)
        val currentIdx = sortedAlbumKeys.indexOf(currentKey)
        val currentAlbumSongs = sortAlbumSongs(albumsMap[currentKey] ?: emptyList())
        val currentAlbumTrack1 = currentAlbumSongs.firstOrNull()

        val isPastThreshold = player.currentPosition > 3000L && currentAlbumTrack1 != null && current.id != currentAlbumTrack1.id

        val prevIdx = if (isPastThreshold) {
            currentIdx.coerceAtLeast(0)
        } else if (currentIdx != -1) {
            if (currentIdx - 1 < 0) sortedAlbumKeys.size - 1 else currentIdx - 1
        } else 0

        val prevAlbumKey = sortedAlbumKeys[prevIdx]
        val prevAlbumSongs = sortAlbumSongs(albumsMap[prevAlbumKey] ?: emptyList())
        return prevAlbumSongs.firstOrNull()
    }

    fun getArtistFirstTrack(): Song? {
        val current = _currentSong.value ?: return null
        val artistName = current.artist
        if (artistName.isBlank()) return null
        val rawBase = unshuffledPlaylist.ifEmpty { masterPlaylist.ifEmpty { _currentPlaylist.value } }
        val baseList = sortLibrarySongs(rawBase)
        return baseList.filter { it.artist.equals(artistName, ignoreCase = true) }.firstOrNull()
    }

    fun getAlbumFirstTrack(): Song? {
        val current = _currentSong.value ?: return null
        val currentKey = getAlbumKey(current)
        val rawBase = unshuffledPlaylist.ifEmpty { masterPlaylist.ifEmpty { _currentPlaylist.value } }
        val baseList = sortLibrarySongs(rawBase)
        return sortAlbumSongs(baseList.filter { getAlbumKey(it) == currentKey }).firstOrNull()
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

    fun refreshCurrentSongArtwork() {
        val current = _currentSong.value ?: return
        scope.launch(Dispatchers.IO) {
            val updatedSongs = musicDatabase?.getAllSongs() ?: emptyList()
            val updatedSong = updatedSongs.find { it.id == current.id }
            if (updatedSong != null) {
                _currentSong.value = updatedSong
            }
        }
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
