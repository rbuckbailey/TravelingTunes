package com.travelingtunes.app.core.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import com.travelingtunes.app.core.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LibraryStats(
    val totalSongs: Int = 0,
    val totalAlbums: Int = 0,
    val totalArtists: Int = 0,
    val totalGenres: Int = 0
)

data class AlbumInfo(
    val name: String,
    val artist: String,
    val songCount: Int,
    val artworkUri: Uri?
)

class MusicDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "traveling_tunes_music.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_SONGS = "songs"
        private const val COL_ID = "id"
        private const val COL_TITLE = "title"
        private const val COL_ARTIST = "artist"
        private const val COL_ALBUM = "album"
        private const val COL_ALBUM_ID = "album_id"
        private const val COL_GENRE = "genre"
        private const val COL_DURATION_MS = "duration_ms"
        private const val COL_CONTENT_URI = "content_uri"
        private const val COL_ARTWORK_URI = "artwork_uri"
        private const val COL_FOLDER_PATH = "folder_path"
        private const val COL_FILE_NAME = "file_name"
        private const val COL_TRACK_NUMBER = "track_number"
        private const val COL_YEAR = "year"
        private const val COL_USER_RATING = "user_rating"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createSongsTable = """
            CREATE TABLE $TABLE_SONGS (
                $COL_ID INTEGER PRIMARY KEY,
                $COL_TITLE TEXT NOT NULL,
                $COL_ARTIST TEXT NOT NULL,
                $COL_ALBUM TEXT NOT NULL,
                $COL_ALBUM_ID INTEGER NOT NULL DEFAULT 0,
                $COL_GENRE TEXT NOT NULL DEFAULT 'Unknown Genre',
                $COL_DURATION_MS INTEGER NOT NULL DEFAULT 0,
                $COL_CONTENT_URI TEXT NOT NULL,
                $COL_ARTWORK_URI TEXT,
                $COL_FOLDER_PATH TEXT NOT NULL DEFAULT '',
                $COL_FILE_NAME TEXT NOT NULL DEFAULT '',
                $COL_TRACK_NUMBER INTEGER NOT NULL DEFAULT 0,
                $COL_YEAR INTEGER NOT NULL DEFAULT 0,
                $COL_USER_RATING INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent()
        db.execSQL(createSongsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SONGS")
        onCreate(db)
    }

    suspend fun insertOrReplaceSongs(songs: List<Song>) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (song in songs) {
                val cv = ContentValues().apply {
                    put(COL_ID, song.id)
                    put(COL_TITLE, song.title)
                    put(COL_ARTIST, song.artist)
                    put(COL_ALBUM, song.album)
                    put(COL_ALBUM_ID, song.albumId)
                    put(COL_GENRE, song.genre)
                    put(COL_DURATION_MS, song.durationMs)
                    put(COL_CONTENT_URI, song.contentUri.toString())
                    put(COL_ARTWORK_URI, song.artworkUri?.toString())
                    put(COL_FOLDER_PATH, song.folderPath)
                    put(COL_FILE_NAME, song.fileName)
                    put(COL_TRACK_NUMBER, song.trackNumber)
                    put(COL_YEAR, song.year)
                    put(COL_USER_RATING, song.userRating)
                }
                db.insertWithOnConflict(TABLE_SONGS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    suspend fun clearDatabase() = withContext(Dispatchers.IO) {
        writableDatabase.execSQL("DELETE FROM $TABLE_SONGS")
    }

    suspend fun getAllSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_SONGS ORDER BY $COL_ARTIST COLLATE NOCASE ASC, $COL_ALBUM COLLATE NOCASE ASC, CASE WHEN $COL_TRACK_NUMBER > 0 THEN $COL_TRACK_NUMBER ELSE 999999 END ASC, $COL_TITLE COLLATE NOCASE ASC", null)
        cursor.use { c ->
            while (c.moveToNext()) {
                songs.add(cursorToSong(c))
            }
        }
        songs
    }

    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext getAllSongs()
        val songs = mutableListOf<Song>()
        val db = readableDatabase
        val pattern = "%${query.trim()}%"
        val sql = """
            SELECT * FROM $TABLE_SONGS 
            WHERE $COL_TITLE LIKE ? 
               OR $COL_ARTIST LIKE ? 
               OR $COL_ALBUM LIKE ? 
               OR $COL_GENRE LIKE ? 
               OR $COL_FOLDER_PATH LIKE ? 
               OR $COL_FILE_NAME LIKE ? 
            ORDER BY $COL_ARTIST COLLATE NOCASE ASC, $COL_ALBUM COLLATE NOCASE ASC, CASE WHEN $COL_TRACK_NUMBER > 0 THEN $COL_TRACK_NUMBER ELSE 999999 END ASC, $COL_TITLE COLLATE NOCASE ASC
        """.trimIndent()
        val cursor = db.rawQuery(sql, arrayOf(pattern, pattern, pattern, pattern, pattern, pattern))
        cursor.use { c ->
            while (c.moveToNext()) {
                songs.add(cursorToSong(c))
            }
        }
        songs
    }

    suspend fun getSongsByArtist(artist: String): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_SONGS WHERE $COL_ARTIST = ? ORDER BY $COL_ALBUM COLLATE NOCASE ASC, CASE WHEN $COL_TRACK_NUMBER > 0 THEN $COL_TRACK_NUMBER ELSE 999999 END ASC, $COL_TITLE COLLATE NOCASE ASC", arrayOf(artist))
        cursor.use { c ->
            while (c.moveToNext()) {
                songs.add(cursorToSong(c))
            }
        }
        songs
    }

    suspend fun getSongsByAlbum(album: String): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_SONGS WHERE $COL_ALBUM = ? ORDER BY CASE WHEN $COL_TRACK_NUMBER > 0 THEN $COL_TRACK_NUMBER ELSE 999999 END ASC, $COL_TITLE COLLATE NOCASE ASC", arrayOf(album))
        cursor.use { c ->
            while (c.moveToNext()) {
                songs.add(cursorToSong(c))
            }
        }
        songs
    }

    suspend fun getSongsByGenre(genre: String): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_SONGS WHERE $COL_GENRE = ? ORDER BY $COL_ARTIST COLLATE NOCASE ASC, $COL_ALBUM COLLATE NOCASE ASC, CASE WHEN $COL_TRACK_NUMBER > 0 THEN $COL_TRACK_NUMBER ELSE 999999 END ASC, $COL_TITLE COLLATE NOCASE ASC", arrayOf(genre))
        cursor.use { c ->
            while (c.moveToNext()) {
                songs.add(cursorToSong(c))
            }
        }
        songs
    }

    suspend fun getSongsByFolder(folderPath: String): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_SONGS WHERE $COL_FOLDER_PATH = ? ORDER BY $COL_TITLE ASC", arrayOf(folderPath))
        cursor.use { c ->
            while (c.moveToNext()) {
                songs.add(cursorToSong(c))
            }
        }
        songs
    }

    suspend fun getArtists(): List<String> = withContext(Dispatchers.IO) {
        val artists = mutableListOf<String>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT DISTINCT $COL_ARTIST FROM $TABLE_SONGS ORDER BY $COL_ARTIST ASC", null)
        cursor.use { c ->
            while (c.moveToNext()) {
                artists.add(c.getString(0))
            }
        }
        artists
    }

    suspend fun getAlbums(): List<AlbumInfo> = withContext(Dispatchers.IO) {
        val albums = mutableListOf<AlbumInfo>()
        val db = readableDatabase
        val sql = """
            SELECT $COL_ALBUM, $COL_ARTIST, COUNT(*) as song_count, MAX($COL_ARTWORK_URI) as art_uri
            FROM $TABLE_SONGS 
            GROUP BY $COL_ALBUM
            ORDER BY $COL_ALBUM ASC
        """.trimIndent()
        val cursor = db.rawQuery(sql, null)
        cursor.use { c ->
            while (c.moveToNext()) {
                val album = c.getString(0)
                val artist = c.getString(1)
                val count = c.getInt(2)
                val artStr = c.getString(3)
                val artUri = artStr?.let { Uri.parse(it) }
                albums.add(AlbumInfo(album, artist, count, artUri))
            }
        }
        albums
    }

    suspend fun getGenres(): List<String> = withContext(Dispatchers.IO) {
        val genres = mutableListOf<String>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT DISTINCT $COL_GENRE FROM $TABLE_SONGS ORDER BY $COL_GENRE ASC", null)
        cursor.use { c ->
            while (c.moveToNext()) {
                genres.add(c.getString(0))
            }
        }
        genres
    }

    suspend fun getFolders(): List<String> = withContext(Dispatchers.IO) {
        val folders = mutableListOf<String>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT DISTINCT $COL_FOLDER_PATH FROM $TABLE_SONGS ORDER BY $COL_FOLDER_PATH ASC", null)
        cursor.use { c ->
            while (c.moveToNext()) {
                val f = c.getString(0)
                if (f.isNotBlank()) folders.add(f)
            }
        }
        folders
    }

    suspend fun getLibraryStats(): LibraryStats = withContext(Dispatchers.IO) {
        val db = readableDatabase
        var totalSongs = 0
        var totalAlbums = 0
        var totalArtists = 0
        var totalGenres = 0

        db.rawQuery("SELECT COUNT(*), COUNT(DISTINCT $COL_ALBUM), COUNT(DISTINCT $COL_ARTIST), COUNT(DISTINCT $COL_GENRE) FROM $TABLE_SONGS", null).use { c ->
            if (c.moveToFirst()) {
                totalSongs = c.getInt(0)
                totalAlbums = c.getInt(1)
                totalArtists = c.getInt(2)
                totalGenres = c.getInt(3)
            }
        }
        LibraryStats(totalSongs, totalAlbums, totalArtists, totalGenres)
    }

    private fun cursorToSong(c: android.database.Cursor): Song {
        val id = c.getLong(c.getColumnIndexOrThrow(COL_ID))
        val title = c.getString(c.getColumnIndexOrThrow(COL_TITLE))
        val artist = c.getString(c.getColumnIndexOrThrow(COL_ARTIST))
        val album = c.getString(c.getColumnIndexOrThrow(COL_ALBUM))
        val albumId = c.getLong(c.getColumnIndexOrThrow(COL_ALBUM_ID))
        val genre = c.getString(c.getColumnIndexOrThrow(COL_GENRE))
        val durationMs = c.getLong(c.getColumnIndexOrThrow(COL_DURATION_MS))
        val contentUriStr = c.getString(c.getColumnIndexOrThrow(COL_CONTENT_URI))
        val artworkUriStr = c.getString(c.getColumnIndexOrThrow(COL_ARTWORK_URI))
        val folderPath = c.getString(c.getColumnIndexOrThrow(COL_FOLDER_PATH))
        val fileName = c.getString(c.getColumnIndexOrThrow(COL_FILE_NAME))
        val trackNumber = c.getInt(c.getColumnIndexOrThrow(COL_TRACK_NUMBER))
        val year = c.getInt(c.getColumnIndexOrThrow(COL_YEAR))
        val userRating = c.getInt(c.getColumnIndexOrThrow(COL_USER_RATING))

        return Song(
            id = id,
            title = title,
            artist = artist,
            album = album,
            albumId = albumId,
            durationMs = durationMs,
            contentUri = Uri.parse(contentUriStr),
            artworkUri = artworkUriStr?.let { Uri.parse(it) },
            userRating = userRating,
            genre = genre,
            folderPath = folderPath,
            fileName = fileName,
            trackNumber = trackNumber,
            year = year
        )
    }
}
