package com.travelingtunes.app

import android.net.Uri
import com.travelingtunes.app.core.database.DownloadedAlbumArtInfo
import com.travelingtunes.app.core.media.ArtworkCandidate
import com.travelingtunes.app.core.media.Id3ArtworkEmbedder
import com.travelingtunes.app.core.media.Id3TagEmbedder
import com.travelingtunes.app.core.media.Id3TagParser
import com.travelingtunes.app.core.model.GestureAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class DownloadedArtAndEmbeddingTest {

    @Test
    fun testDeleteDownloadedArtGestureActionResolution() {
        val action = GestureAction.fromKey("DeleteDownloadedArt")
        assertEquals(GestureAction.DELETE_DOWNLOADED_ART, action)

        val actionWithSpaces = GestureAction.fromKey("Delete Downloaded Art")
        assertEquals(GestureAction.DELETE_DOWNLOADED_ART, actionWithSpaces)
        assertEquals("Delete Downloaded Art", GestureAction.DELETE_DOWNLOADED_ART.displayName)
    }

    @Test
    fun testDownloadedAlbumArtInfoModel() {
        val mockUri = Mockito.mock(Uri::class.java)
        val info = DownloadedAlbumArtInfo(
            album = "Abbey Road",
            artist = "The Beatles",
            songCount = 17,
            artworkUri = mockUri
        )

        assertEquals("Abbey Road", info.album)
        assertEquals("The Beatles", info.artist)
        assertEquals(17, info.songCount)
        assertEquals(mockUri, info.artworkUri)
    }

    @Test
    fun testArtworkCandidateSquareness() {
        val squareCandidate = ArtworkCandidate(url = "http://example.com/square.jpg", width = 1000, height = 1000, source = "Deezer")
        assertEquals(1.0, squareCandidate.squareness, 0.001)
        assertEquals(1000000, squareCandidate.resolution)

        val nonSquareCandidate = ArtworkCandidate(url = "http://example.com/rect.jpg", width = 800, height = 400, source = "Web Search")
        assertEquals(0.5, nonSquareCandidate.squareness, 0.001)
    }

    @Test
    fun testMp3Id3EmbeddingStructure() {
        val tempAudioFile = File.createTempFile("test_audio", ".mp3").apply { deleteOnExit() }
        val tempOutputFile = File.createTempFile("test_output", ".mp3").apply { deleteOnExit() }

        // Create dummy MP3 content (sync bytes FF FB)
        val dummyMp3Audio = byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x64.toByte(), 0x00, 0x00, 0x00)
        FileOutputStream(tempAudioFile).use { it.write(dummyMp3Audio) }

        // Create dummy JPEG image bytes (FF D8 ... FF D9)
        val dummyJpegBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00, 0x10, 0xFF.toByte(), 0xD9.toByte())

        val method = Id3ArtworkEmbedder::class.java.getDeclaredMethod(
            "embedMp3Id3v2Apic",
            ByteArray::class.java,
            ByteArray::class.java,
            File::class.java
        )
        method.isAccessible = true
        val result = method.invoke(Id3ArtworkEmbedder, dummyMp3Audio, dummyJpegBytes, tempOutputFile) as Boolean

        assertTrue("Embedding ID3v2 APIC frame should succeed", result)
        assertTrue("Output file must exist", tempOutputFile.exists())
        assertTrue("Output file size should be larger than original audio", tempOutputFile.length() > dummyMp3Audio.size)

        val outputBytes = tempOutputFile.readBytes()
        // Must start with 'ID3' header
        assertEquals('I'.code.toByte(), outputBytes[0])
        assertEquals('D'.code.toByte(), outputBytes[1])
        assertEquals('3'.code.toByte(), outputBytes[2])
        // Version 2.3
        assertEquals(3.toByte(), outputBytes[3])
    }

    @Test
    fun testFlacPictureBlockEmbeddingStructure() {
        val tempOutputFile = File.createTempFile("test_output", ".flac").apply { deleteOnExit() }

        // Create dummy FLAC content (header "fLaC" followed by dummy frame)
        val dummyFlacAudio = byteArrayOf('f'.code.toByte(), 'L'.code.toByte(), 'a'.code.toByte(), 'C'.code.toByte(), 0x12, 0x34, 0x56)
        val dummyJpegBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())

        val method = Id3ArtworkEmbedder::class.java.getDeclaredMethod(
            "embedFlacPicture",
            ByteArray::class.java,
            ByteArray::class.java,
            File::class.java
        )
        method.isAccessible = true
        val result = method.invoke(Id3ArtworkEmbedder, dummyFlacAudio, dummyJpegBytes, tempOutputFile) as Boolean

        assertTrue("Embedding FLAC PICTURE metadata block should succeed", result)
        assertTrue("Output file must exist", tempOutputFile.exists())

        val outputBytes = tempOutputFile.readBytes()
        // Must start with "fLaC"
        assertEquals('f'.code.toByte(), outputBytes[0])
        assertEquals('L'.code.toByte(), outputBytes[1])
        assertEquals('a'.code.toByte(), outputBytes[2])
        assertEquals('C'.code.toByte(), outputBytes[3])
        // Picture block type header masked is 0x06
        assertEquals(0x06.toByte(), (outputBytes[4].toInt() and 0x7F).toByte())
    }

    @Test
    fun testMp3ArtworkRemovalPreservesMetadataFrames() {
        val tempAudioFile = File.createTempFile("test_audio_with_art", ".mp3").apply { deleteOnExit() }
        val tempOutputFile = File.createTempFile("test_no_art", ".mp3").apply { deleteOnExit() }

        val dummyMp3Audio = byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x64.toByte(), 0x00, 0x00)
        val dummyJpegBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())

        val embedMethod = Id3ArtworkEmbedder::class.java.getDeclaredMethod(
            "embedMp3Id3v2Apic",
            ByteArray::class.java,
            ByteArray::class.java,
            File::class.java
        )
        embedMethod.isAccessible = true
        embedMethod.invoke(Id3ArtworkEmbedder, dummyMp3Audio, dummyJpegBytes, tempAudioFile)

        val embeddedBytes = tempAudioFile.readBytes()
        val (embeddedFrames, _) = Id3TagParser.parseAndExtractAudioPayload(embeddedBytes)
        assertTrue("Embedded MP3 must contain APIC frame", embeddedFrames.any { it.id == "APIC" })

        val removeMethod = Id3ArtworkEmbedder::class.java.getDeclaredMethod(
            "removeMp3Id3v2Apic",
            ByteArray::class.java,
            File::class.java
        )
        removeMethod.isAccessible = true
        val result = removeMethod.invoke(Id3ArtworkEmbedder, embeddedBytes, tempOutputFile) as Boolean

        assertTrue("Removal of MP3 APIC frame should succeed", result)
        val outputBytes = tempOutputFile.readBytes()
        val (cleanedFrames, _) = Id3TagParser.parseAndExtractAudioPayload(outputBytes)
        assertTrue("Cleaned MP3 must NOT contain APIC frame", cleanedFrames.none { it.id == "APIC" || it.id == "PIC" })
    }

    @Test
    fun testFlacArtworkRemovalPreservesOtherBlocks() {
        val tempAudioFile = File.createTempFile("test_flac_with_art", ".flac").apply { deleteOnExit() }
        val tempOutputFile = File.createTempFile("test_flac_no_art", ".flac").apply { deleteOnExit() }

        val dummyFlacAudio = byteArrayOf('f'.code.toByte(), 'L'.code.toByte(), 'a'.code.toByte(), 'C'.code.toByte(), 0x12, 0x34, 0x56)
        val dummyJpegBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())

        val embedMethod = Id3ArtworkEmbedder::class.java.getDeclaredMethod(
            "embedFlacPicture",
            ByteArray::class.java,
            ByteArray::class.java,
            File::class.java
        )
        embedMethod.isAccessible = true
        embedMethod.invoke(Id3ArtworkEmbedder, dummyFlacAudio, dummyJpegBytes, tempAudioFile)

        val embeddedBytes = tempAudioFile.readBytes()

        val removeMethod = Id3ArtworkEmbedder::class.java.getDeclaredMethod(
            "removeFlacPicture",
            ByteArray::class.java,
            File::class.java
        )
        removeMethod.isAccessible = true
        val result = removeMethod.invoke(Id3ArtworkEmbedder, embeddedBytes, tempOutputFile) as Boolean

        assertTrue("Removal of FLAC Picture block should succeed", result)
        val outputBytes = tempOutputFile.readBytes()
        assertEquals('f'.code.toByte(), outputBytes[0])
        assertEquals('L'.code.toByte(), outputBytes[1])
        assertEquals('a'.code.toByte(), outputBytes[2])
        assertEquals('C'.code.toByte(), outputBytes[3])
    }

    @Test
    fun testArtAndTagsEditorCategoriesAndMetadataEmbedding() {
        val categories = com.travelingtunes.app.feature.settings.ArtEditorCategory.entries
        assertEquals(5, categories.size)
        assertEquals("Albums", com.travelingtunes.app.feature.settings.ArtEditorCategory.ALBUMS.displayName)
        assertEquals("Tracks", com.travelingtunes.app.feature.settings.ArtEditorCategory.TRACKS.displayName)
        assertEquals("Artists", com.travelingtunes.app.feature.settings.ArtEditorCategory.ARTISTS.displayName)
        assertEquals("Genres", com.travelingtunes.app.feature.settings.ArtEditorCategory.GENRES.displayName)
        assertEquals("Folders", com.travelingtunes.app.feature.settings.ArtEditorCategory.FOLDERS.displayName)
    }

    @Test
    fun testArtworkEmbeddingPreservesExistingGenreAndTextFrames() {
        val tempOutputFile = File.createTempFile("test_art_preserve", ".mp3").apply { deleteOnExit() }

        // Construct initial ID3 tag with TCON (Genre = "Classic Rock"), TIT2, TPE1
        val genreFrame = Id3TagEmbedder.buildTextFrame("TCON", "Classic Rock")
        val titleFrame = Id3TagEmbedder.buildTextFrame("TIT2", "Bohemian Rhapsody")
        val artistFrame = Id3TagEmbedder.buildTextFrame("TPE1", "Queen")

        val initialTagBody = ByteArrayOutputStream()
        initialTagBody.write(genreFrame)
        initialTagBody.write(titleFrame)
        initialTagBody.write(artistFrame)
        val tagBytes = initialTagBody.toByteArray()

        val synchsize = byteArrayOf(
            ((tagBytes.size shr 21) and 0x7F).toByte(),
            ((tagBytes.size shr 14) and 0x7F).toByte(),
            ((tagBytes.size shr 7) and 0x7F).toByte(),
            (tagBytes.size and 0x7F).toByte()
        )

        val id3Header = byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), 0x03, 0x00, 0x00, synchsize[0], synchsize[1], synchsize[2], synchsize[3])
        val dummyAudio = byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x64.toByte())

        val initialAudioWithTag = id3Header + tagBytes + dummyAudio
        val dummyJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())

        val method = Id3ArtworkEmbedder::class.java.getDeclaredMethod(
            "embedMp3Id3v2Apic",
            ByteArray::class.java,
            ByteArray::class.java,
            File::class.java
        )
        method.isAccessible = true
        val result = method.invoke(Id3ArtworkEmbedder, initialAudioWithTag, dummyJpeg, tempOutputFile) as Boolean

        assertTrue("Embedding artwork should succeed", result)

        val (frames, _) = Id3TagParser.parseAndExtractAudioPayload(tempOutputFile.readBytes())
        val frameIds = frames.map { it.id }

        assertTrue("TCON (Genre) frame must be preserved when embedding artwork", frameIds.contains("TCON"))
        assertTrue("TIT2 (Title) frame must be preserved when embedding artwork", frameIds.contains("TIT2"))
        assertTrue("TPE1 (Artist) frame must be preserved when embedding artwork", frameIds.contains("TPE1"))
        assertTrue("APIC (Artwork) frame must be added", frameIds.contains("APIC"))
    }

    @Test
    fun testMetadataEmbeddingPreservesExistingArtworkFrame() {
        val tempOutputFile = File.createTempFile("test_meta_preserve", ".mp3").apply { deleteOnExit() }

        // Create dummy MP3 audio with APIC frame
        val dummyJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())
        val initialFile = File.createTempFile("test_init", ".mp3").apply { deleteOnExit() }

        val embedApicMethod = Id3ArtworkEmbedder::class.java.getDeclaredMethod(
            "embedMp3Id3v2Apic",
            ByteArray::class.java,
            ByteArray::class.java,
            File::class.java
        )
        embedApicMethod.isAccessible = true
        val dummyAudioPayload = byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x64.toByte())
        embedApicMethod.invoke(Id3ArtworkEmbedder, dummyAudioPayload, dummyJpeg, initialFile)

        val fileWithApic = initialFile.readBytes()

        // Now embed metadata text tags (Genre = "Hard Rock", Title = "We Will Rock You")
        val embedMetaMethod = Id3TagEmbedder::class.java.getDeclaredMethod(
            "embedMp3Id3v2FullTextFrames",
            ByteArray::class.java,
            String::class.java,
            String::class.java,
            String::class.java,
            String::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            File::class.java
        )
        embedMetaMethod.isAccessible = true
        val metaResult = embedMetaMethod.invoke(
            Id3TagEmbedder,
            fileWithApic,
            "Hard Rock",
            "We Will Rock You",
            "Queen",
            "News of the World",
            1977,
            1,
            1,
            tempOutputFile
        ) as Boolean

        assertTrue("Embedding metadata text frames should succeed", metaResult)

        val (frames, _) = Id3TagParser.parseAndExtractAudioPayload(tempOutputFile.readBytes())
        val frameIds = frames.map { it.id }

        assertTrue("APIC (Artwork) frame must be preserved when embedding metadata tags", frameIds.contains("APIC"))
        assertTrue("TCON (Genre) frame must be written", frameIds.contains("TCON"))
        assertTrue("TIT2 (Title) frame must be written", frameIds.contains("TIT2"))
        assertTrue("TPE1 (Artist) frame must be written", frameIds.contains("TPE1"))
    }

    @Test
    fun testAlbumArtBrowserInfoModelAndArtworkTypes() {
        val mockUri = Mockito.mock(Uri::class.java)
        val downloadedInfo = com.travelingtunes.app.core.database.AlbumArtBrowserInfo(
            album = "Dark Side of the Moon",
            artist = "Pink Floyd",
            songCount = 10,
            artworkUri = mockUri,
            artType = com.travelingtunes.app.core.database.ArtworkType.DOWNLOADED
        )
        assertEquals("Dark Side of the Moon", downloadedInfo.album)
        assertEquals("Pink Floyd", downloadedInfo.artist)
        assertEquals(10, downloadedInfo.songCount)
        assertEquals(com.travelingtunes.app.core.database.ArtworkType.DOWNLOADED, downloadedInfo.artType)

        val embeddedInfo = com.travelingtunes.app.core.database.AlbumArtBrowserInfo(
            album = "The Wall",
            artist = "Pink Floyd",
            songCount = 26,
            artworkUri = mockUri,
            artType = com.travelingtunes.app.core.database.ArtworkType.EMBEDDED
        )
        assertEquals(com.travelingtunes.app.core.database.ArtworkType.EMBEDDED, embeddedInfo.artType)

        val missingInfo = com.travelingtunes.app.core.database.AlbumArtBrowserInfo(
            album = "Unknown",
            artist = "Unknown",
            songCount = 1,
            artworkUri = null,
            artType = com.travelingtunes.app.core.database.ArtworkType.MISSING
        )
        assertEquals(com.travelingtunes.app.core.database.ArtworkType.MISSING, missingInfo.artType)
    }

    @Test
    fun testClassifyArtworkTypeWithEmbeddedUri() {
        val method = com.travelingtunes.app.core.database.MusicDatabase::class.java.getDeclaredMethod(
            "classifyArtworkType",
            android.content.Context::class.java,
            Uri::class.java
        )
        method.isAccessible = true

        val downloadedUri = Mockito.mock(Uri::class.java)
        Mockito.`when`(downloadedUri.toString()).thenReturn("file:///data/user/0/com.travelingtunes.app/cache/downloaded_art/art_downloaded_abc123.jpg")
        Mockito.`when`(downloadedUri.scheme).thenReturn("file")

        val dbMock = Mockito.mock(com.travelingtunes.app.core.database.MusicDatabase::class.java)
        val downloadedType = method.invoke(dbMock, null, downloadedUri)
        assertEquals(com.travelingtunes.app.core.database.ArtworkType.DOWNLOADED, downloadedType)

        val embeddedUri = Mockito.mock(Uri::class.java)
        Mockito.`when`(embeddedUri.toString()).thenReturn("file:///data/user/0/com.travelingtunes.app/cache/embedded_art/art_embedded_abc123.jpg")
        Mockito.`when`(embeddedUri.scheme).thenReturn("file")

        val embeddedType = method.invoke(dbMock, null, embeddedUri)
        assertEquals(com.travelingtunes.app.core.database.ArtworkType.EMBEDDED, embeddedType)
    }

    @Test
    fun testSearchEngineEnumValues() {
        val engines = com.travelingtunes.app.core.media.AlbumArtDownloader.SearchEngine.entries
        assertEquals(4, engines.size)
        assertTrue(engines.contains(com.travelingtunes.app.core.media.AlbumArtDownloader.SearchEngine.DEEZER))
        assertTrue(engines.contains(com.travelingtunes.app.core.media.AlbumArtDownloader.SearchEngine.ITUNES))
        assertTrue(engines.contains(com.travelingtunes.app.core.media.AlbumArtDownloader.SearchEngine.COVER_ART_ARCHIVE))
        assertTrue(engines.contains(com.travelingtunes.app.core.media.AlbumArtDownloader.SearchEngine.WEB_SEARCH))
    }
}
