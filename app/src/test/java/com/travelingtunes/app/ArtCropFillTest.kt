package com.travelingtunes.app

import com.travelingtunes.app.core.media.ArtAlignmentPosition
import com.travelingtunes.app.core.media.ArtCropFillHelper
import com.travelingtunes.app.core.media.ArtCropFillMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtCropFillTest {

    @Test
    fun testIsNonSquareDetection() {
        assertTrue("800x600 should be non-square", ArtCropFillHelper.isNonSquare(800, 600))
        assertTrue("600x800 should be non-square", ArtCropFillHelper.isNonSquare(600, 800))
        assertFalse("500x500 should be square", ArtCropFillHelper.isNonSquare(500, 500))
    }

    @Test
    fun testAlignmentPositionsNineGrid() {
        val entries = ArtAlignmentPosition.entries
        assertEquals("Should have 9 alignment positions", 9, entries.size)

        assertEquals(ArtAlignmentPosition.TOP_LEFT, ArtAlignmentPosition.fromRowCol(0, 0))
        assertEquals(ArtAlignmentPosition.CENTER, ArtAlignmentPosition.fromRowCol(1, 1))
        assertEquals(ArtAlignmentPosition.BOTTOM_RIGHT, ArtAlignmentPosition.fromRowCol(2, 2))
    }

    @Test
    fun testCropAndFillModeEnumNames() {
        assertEquals("Crop to Fit", ArtCropFillMode.CROP.displayName)
        assertEquals("Fill / Letterbox", ArtCropFillMode.FILL.displayName)
    }
}
