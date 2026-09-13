package com.travelingtunes.app.core.media

import android.content.Context
import com.travelingtunes.app.core.database.MusicDatabase
import com.travelingtunes.app.core.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class DuplicateTrackInfo(
    val song: Song,
    val fileSize: Long,
    val fullPath: String
)

data class DuplicateMatchPair(
    val id: String,
    val trackA: DuplicateTrackInfo,
    val trackB: DuplicateTrackInfo,
    val likelihoodPercentage: Int,
    val matchReasons: List<String>
)

object DuplicateTrackFinder {

    fun getSongFileSize(context: Context, song: Song): Long {
        try {
            context.contentResolver.openFileDescriptor(song.contentUri, "r")?.use { pfd ->
                if (pfd.statSize > 0) return pfd.statSize
            }
        } catch (_: Exception) {}

        if (song.contentUri.scheme == "file") {
            val file = File(song.contentUri.path ?: "")
            if (file.exists()) return file.length()
        }

        if (song.folderPath.isNotBlank() && song.fileName.isNotBlank()) {
            val file = File(song.folderPath, song.fileName)
            if (file.exists()) return file.length()
        }

        return 0L
    }

    fun getFullFolderPath(song: Song, musicFolderName: String? = null): String {
        if (song.contentUri.scheme == "file") {
            val parent = File(song.contentUri.path ?: "").parent
            if (!parent.isNullOrBlank()) return parent
        }
        val folder = song.folderPath.trim()
        if (folder.startsWith("/")) {
            return folder
        }
        if (!musicFolderName.isNullOrBlank()) {
            return if (folder.isBlank()) musicFolderName else "$musicFolderName/$folder"
        }
        return folder.ifBlank { "Music Folder" }
    }

    suspend fun findDuplicates(
        context: Context,
        songs: List<Song>,
        musicFolderName: String? = null
    ): List<DuplicateMatchPair> = withContext(Dispatchers.IO) {
        if (songs.size < 2) return@withContext emptyList()

        val songInfos = songs.map { song ->
            val size = getSongFileSize(context, song)
            val fullPath = getFullFolderPath(song, musicFolderName)
            DuplicateTrackInfo(song, size, fullPath)
        }

        val pairs = mutableListOf<DuplicateMatchPair>()

        for (i in songInfos.indices) {
            val infoA = songInfos[i]
            for (j in i + 1 until songInfos.size) {
                val infoB = songInfos[j]

                val match = calculateMatch(infoA, infoB)
                if (match.likelihoodPercentage >= 40) {
                    pairs.add(match)
                }
            }
        }

        pairs.sortedWith(
            compareByDescending<DuplicateMatchPair> { it.likelihoodPercentage }
                .thenBy { it.trackA.song.title }
        )
    }

    fun calculateMatch(infoA: DuplicateTrackInfo, infoB: DuplicateTrackInfo): DuplicateMatchPair {
        val songA = infoA.song
        val songB = infoB.song

        val nameA = cleanFileName(songA.fileName)
        val nameB = cleanFileName(songB.fileName)

        val titleA = cleanMetadata(songA.title)
        val titleB = cleanMetadata(songB.title)

        val artistA = cleanMetadata(songA.artist)
        val artistB = cleanMetadata(songB.artist)

        val albumA = cleanMetadata(songA.album)
        val albumB = cleanMetadata(songB.album)

        val fileNameScore = stringSimilarity(nameA, nameB)
        val titleScore = stringSimilarity(titleA, titleB)

        val artistScore = if (artistA.isBlank() || artistB.isBlank() || artistA == "unknown" || artistB == "unknown") {
            0.5f
        } else {
            stringSimilarity(artistA, artistB)
        }

        val albumScore = if (albumA.isBlank() || albumB.isBlank() || albumA == "unknown" || albumB == "unknown") {
            0.5f
        } else {
            stringSimilarity(albumA, albumB)
        }

        val sizeA = infoA.fileSize
        val sizeB = infoB.fileSize

        val sizeScore = if (sizeA > 0L && sizeB > 0L) {
            if (sizeA == sizeB) {
                1.0f
            } else {
                val diffRatio = abs(sizeA - sizeB).toDouble() / max(sizeA, sizeB).toDouble()
                max(0.0, 1.0 - (diffRatio * 5.0)).toFloat()
            }
        } else {
            0.5f
        }

        val durA = songA.durationMs
        val durB = songB.durationMs

        val durationScore = if (durA > 0L && durB > 0L) {
            val diffMs = abs(durA - durB)
            when {
                diffMs <= 1000L -> 1.0f
                diffMs <= 3000L -> 0.9f
                diffMs <= 10000L -> 0.5f
                else -> 0.0f
            }
        } else {
            0.5f
        }

        val weightedScore = (fileNameScore * 0.30f) + (sizeScore * 0.30f) + (titleScore * 0.15f) + (artistScore * 0.10f) + (albumScore * 0.08f) + (durationScore * 0.07f)
        var likelihood = (weightedScore * 100).toInt().coerceIn(0, 100)

        // Boost likelihood for exact matches
        if (sizeA > 0L && sizeA == sizeB && nameA == nameB) {
            likelihood = max(likelihood, 98)
        }
        if (sizeA > 0L && sizeA == sizeB && titleA == titleB && durA > 0L && abs(durA - durB) <= 1000L) {
            likelihood = max(likelihood, 99)
        }

        val reasons = mutableListOf<String>()
        if (sizeA > 0L && sizeA == sizeB) {
            reasons.add("Identical file size (${formatFileSize(sizeA)})")
        } else if (sizeScore > 0.8f) {
            reasons.add("Similar file size")
        }

        if (nameA == nameB && nameA.isNotBlank()) {
            reasons.add("Identical file name")
        } else if (fileNameScore > 0.8f) {
            reasons.add("Similar file name")
        }

        if (titleA == titleB && titleA.isNotBlank()) {
            reasons.add("Identical title")
        } else if (titleScore > 0.8f) {
            reasons.add("Similar title")
        }

        if (durA > 0L && durB > 0L && abs(durA - durB) <= 1000L) {
            reasons.add("Matching duration (${formatDuration(durA)})")
        }

        if (reasons.isEmpty() && likelihood >= 40) {
            reasons.add("General metadata & size similarity")
        }

        val pairId = "${min(songA.id, songB.id)}_${max(songA.id, songB.id)}"

        return DuplicateMatchPair(
            id = pairId,
            trackA = infoA,
            trackB = infoB,
            likelihoodPercentage = likelihood,
            matchReasons = reasons
        )
    }

    fun cleanFileName(fileName: String): String {
        var name = fileName.substringBeforeLast('.').lowercase().trim()
        name = name.replace(Regex("^[0-9]+[\\s._-]+"), "") // remove leading track numbers like "01 - "
        name = name.replace(Regex("\\([0-9]+\\)$"), "") // remove trailing "(1)"
        name = name.replace(Regex("_[0-9]+$"), "")
        return name.replace(Regex("[^a-z0-9]"), "")
    }

    fun cleanMetadata(str: String): String {
        val s = str.lowercase().trim()
        if (s == "unknown" || s == "<unknown>" || s == "unknown title" || s == "unknown artist" || s == "unknown album") {
            return ""
        }
        return s.replace(Regex("[^a-z0-9]"), "")
    }

    fun stringSimilarity(s1: String, s2: String): Float {
        if (s1 == s2) return 1.0f
        if (s1.isEmpty() || s2.isEmpty()) return 0.0f
        if (s1.contains(s2) || s2.contains(s1)) {
            val minLen = min(s1.length, s2.length).toFloat()
            val maxLen = max(s1.length, s2.length).toFloat()
            return 0.85f + 0.15f * (minLen / maxLen)
        }
        val dist = levenshteinDistance(s1, s2)
        val maxLen = max(s1.length, s2.length)
        return max(0.0f, 1.0f - (dist.toFloat() / maxLen.toFloat()))
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.2f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
            else -> "$bytes B"
        }
    }

    fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0) return "0:00"
        val totalSecs = durationMs / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format(Locale.US, "%d:%02d", mins, secs)
    }

    suspend fun deleteTrack(context: Context, database: MusicDatabase, song: Song): Boolean = withContext(Dispatchers.IO) {
        // 1. Delete from database
        database.deleteSong(song.id)

        // 2. Delete physical file
        var deleted = false
        try {
            val rows = context.contentResolver.delete(song.contentUri, null, null)
            if (rows > 0) deleted = true
        } catch (_: Exception) {}

        if (!deleted && song.contentUri.scheme == "file") {
            try {
                val file = File(song.contentUri.path ?: "")
                if (file.exists() && file.delete()) deleted = true
            } catch (_: Exception) {}
        }

        if (!deleted && song.folderPath.isNotBlank() && song.fileName.isNotBlank()) {
            try {
                val file = File(song.folderPath, song.fileName)
                if (file.exists() && file.delete()) deleted = true
            } catch (_: Exception) {}
        }

        deleted
    }
}
