package com.travelingtunes.app

import com.travelingtunes.app.core.media.MetadataBackupPayload
import com.travelingtunes.app.core.media.MetadataTrackRecord
import com.travelingtunes.app.core.media.MusicMetadataBackupHelper
import com.travelingtunes.app.core.media.RestoreFieldOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicMetadataBackupTest {

    @Test
    fun testSerializeAndParseMetadataPayload() {
        val tracks = listOf(
            MetadataTrackRecord(
                title = "Stairway to Heaven",
                artist = "Led Zeppelin",
                album = "Led Zeppelin IV",
                genre = "Classic Rock",
                year = 1971,
                trackNumber = 4,
                discNumber = 1,
                userRating = 5,
                folderPath = "Led Zeppelin",
                fileName = "04 - Stairway to Heaven.mp3"
            ),
            MetadataTrackRecord(
                title = "Hotel California",
                artist = "Eagles",
                album = "Hotel California",
                genre = "Rock",
                year = 1976,
                trackNumber = 1,
                discNumber = 1,
                userRating = 4,
                folderPath = "Eagles",
                fileName = "01 - Hotel California.mp3"
            )
        )

        val artworks = mapOf(
            "led zeppelin|led zeppelin iv" to "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
            "eagles|hotel california" to "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
        )

        val payload = MetadataBackupPayload(
            version = 1,
            timestamp = 1600000000000L,
            tracks = tracks,
            albumArtworks = artworks
        )

        val jsonString = MusicMetadataBackupHelper.serializePayloadToJson(payload)
        assertTrue(jsonString.contains("Classic Rock"))
        assertTrue(jsonString.contains("04 - Stairway to Heaven.mp3"))

        val parsed = MusicMetadataBackupHelper.parsePayloadFromJson(jsonString)
        assertNotNull(parsed)
        assertEquals(2, parsed!!.tracks.size)

        val track1 = parsed.tracks[0]
        assertEquals("Stairway to Heaven", track1.title)
        assertEquals("Led Zeppelin", track1.artist)
        assertEquals("Classic Rock", track1.genre)
        assertEquals(1971, track1.year)
        assertEquals(4, track1.trackNumber)
        assertEquals(5, track1.userRating)

        assertEquals(2, parsed.albumArtworks.size)
        assertTrue(parsed.albumArtworks.containsKey("led zeppelin|led zeppelin iv"))
    }

    @Test
    fun testRestoreFieldOptions() {
        val defaultOptions = RestoreFieldOptions()
        assertTrue(defaultOptions.isAllSelected)
        assertTrue(defaultOptions.hasAnySelected)
        assertTrue(defaultOptions.genre)
        assertTrue(defaultOptions.albumArt)

        val customOptions = RestoreFieldOptions(
            genre = true,
            albumArt = true,
            title = false,
            artist = false,
            album = false,
            yearAndTrack = false,
            userRating = false
        )

        assertFalse(customOptions.isAllSelected)
        assertTrue(customOptions.hasAnySelected)
        assertTrue(customOptions.genre)
        assertTrue(customOptions.albumArt)
        assertFalse(customOptions.title)
    }
}
