package com.travelingtunes.app.core.media

import android.content.Context
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.travelingtunes.app.core.model.Song
import com.travelingtunes.app.feature.player.loadSongArtwork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AlbumArtCache private constructor() {

    private val maxMemoryBytes = (Runtime.getRuntime().maxMemory() / 8)
        .toInt()
        .coerceIn(8 * 1024 * 1024, 32 * 1024 * 1024)

    private val cache = object : LruCache<Long, ImageBitmap>(maxMemoryBytes) {
        override fun sizeOf(key: Long, value: ImageBitmap): Int {
            return value.width * value.height * 4
        }
    }
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    companion object {
        val instance = AlbumArtCache()
    }

    fun get(songId: Long): ImageBitmap? {
        return cache.get(songId)
    }

    fun put(songId: Long, bitmap: ImageBitmap) {
        cache.put(songId, bitmap)
    }

    fun remove(songId: Long) {
        cache.remove(songId)
    }

    fun preCacheSurroundingSongs(context: Context, playlist: List<Song>, currentIndex: Int, radius: Int = 4) {
        if (playlist.isEmpty()) return
        scope.launch {
            val safeIndex = currentIndex.coerceIn(0, playlist.size - 1)
            val startIndex = (safeIndex - radius).coerceAtLeast(0)
            val endIndex = (safeIndex + radius).coerceAtMost(playlist.size - 1)

            for (i in startIndex..endIndex) {
                val song = playlist[i]
                if (cache.get(song.id) == null) {
                    val bmp = loadSongArtwork(context, song)
                    if (bmp != null) {
                        cache.put(song.id, bmp.asImageBitmap())
                    }
                }
                com.travelingtunes.app.core.theme.AlbumArtColorCache.instance.preCacheSongTheme(
                    context = context,
                    song = song,
                    innerEdge = null,
                    priority = com.travelingtunes.app.core.model.ArtColorPriority.CENTER
                )
            }
        }
    }
}
