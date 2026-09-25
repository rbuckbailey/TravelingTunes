package com.travelingtunes.app.core.theme

import android.content.Context
import com.travelingtunes.app.core.model.ArtColorPriority
import com.travelingtunes.app.core.model.ColorTheme
import com.travelingtunes.app.core.model.Song
import com.travelingtunes.app.feature.player.loadSongArtwork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlbumArtColorCache private constructor() {

    private val maxSize = 100
    private val themeCache = java.util.Collections.synchronizedMap(
        object : java.util.LinkedHashMap<String, ColorTheme>(maxSize, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ColorTheme>?): Boolean {
                return size > maxSize
            }
        }
    )
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    companion object {
        val instance = AlbumArtColorCache()

        fun makeKey(songId: Long, innerEdge: InnerEdge?, priority: ArtColorPriority): String {
            return "${songId}_${innerEdge?.name ?: "NONE"}_${priority.name}"
        }
    }

    fun get(key: String): ColorTheme? {
        return themeCache.get(key)
    }

    fun get(songId: Long, innerEdge: InnerEdge?, priority: ArtColorPriority): ColorTheme? {
        return themeCache.get(makeKey(songId, innerEdge, priority))
    }

    fun getAnyForSong(songId: Long): ColorTheme? {
        val prefix = "${songId}_"
        synchronized(themeCache) {
            val entry = themeCache.entries.firstOrNull { it.key.startsWith(prefix) }
            return entry?.value
        }
    }

    fun put(key: String, theme: ColorTheme) {
        themeCache.put(key, theme)
    }

    fun put(songId: Long, innerEdge: InnerEdge?, priority: ArtColorPriority, theme: ColorTheme) {
        themeCache.put(makeKey(songId, innerEdge, priority), theme)
    }

    fun clear() {
        themeCache.clear()
    }

    suspend fun getOrExtract(
        context: Context,
        song: Song,
        innerEdge: InnerEdge?,
        priority: ArtColorPriority
    ): ColorTheme = withContext(Dispatchers.IO) {
        val key = makeKey(song.id, innerEdge, priority)
        val cached = themeCache.get(key)
        if (cached != null) return@withContext cached

        val bitmap = loadSongArtwork(context, song)
        if (bitmap != null) {
            val extracted = AlbumArtColorExtractor.extractThemeFromBitmap(
                bitmap = bitmap,
                innerEdge = innerEdge,
                priority = priority
            )
            themeCache.put(key, extracted)
            return@withContext extracted
        }
        return@withContext ColorTheme.MATCH_ALBUM_ART
    }

    fun preCacheSongTheme(
        context: Context,
        song: Song,
        innerEdge: InnerEdge?,
        priority: ArtColorPriority
    ) {
        val key = makeKey(song.id, innerEdge, priority)
        if (themeCache.get(key) != null) return

        scope.launch {
            getOrExtract(context, song, innerEdge, priority)
        }
    }

    fun preCacheSongs(
        context: Context,
        songs: List<Song?>,
        innerEdge: InnerEdge?,
        priority: ArtColorPriority
    ) {
        val nonNullSongs = songs.filterNotNull().distinctBy { it.id }
        if (nonNullSongs.isEmpty()) return

        scope.launch {
            for (song in nonNullSongs) {
                getOrExtract(context, song, innerEdge, priority)
            }
        }
    }
}
