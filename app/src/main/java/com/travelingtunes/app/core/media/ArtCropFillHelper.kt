package com.travelingtunes.app.core.media

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

enum class ArtAlignmentPosition(val displayName: String, val row: Int, val col: Int) {
    TOP_LEFT("Top Left", 0, 0),
    TOP_CENTER("Top Center", 0, 1),
    TOP_RIGHT("Top Right", 0, 2),
    CENTER_LEFT("Center Left", 1, 0),
    CENTER("Center", 1, 1),
    CENTER_RIGHT("Center Right", 1, 2),
    BOTTOM_LEFT("Bottom Left", 2, 0),
    BOTTOM_CENTER("Bottom Center", 2, 1),
    BOTTOM_RIGHT("Bottom Right", 2, 2);

    companion object {
        fun fromRowCol(row: Int, col: Int): ArtAlignmentPosition {
            return entries.find { it.row == row && it.col == col } ?: CENTER
        }
    }
}

enum class ArtCropFillMode(val displayName: String, val description: String) {
    CROP("Crop to Fit", "Crops image to a square using alignment anchor"),
    FILL("Fill / Letterbox", "Pads space with background fill using alignment anchor"),
    STRETCH("Stretch Edges", "Fills square by stretching art edges to the border")
}

object ArtCropFillHelper {

    fun isNonSquare(bitmap: Bitmap?): Boolean {
        if (bitmap == null) return false
        return bitmap.width != bitmap.height
    }

    fun isNonSquare(width: Int, height: Int): Boolean {
        if (width <= 0 || height <= 0) return false
        return width != height
    }

    fun processNonDestructiveSquare(
        original: Bitmap,
        mode: ArtCropFillMode,
        alignment: ArtAlignmentPosition,
        targetSize: Int = 800
    ): Bitmap {
        val origW = original.width
        val origH = original.height

        if (origW == origH) {
            return if (origW == targetSize) original else Bitmap.createScaledBitmap(original, targetSize, targetSize, true)
        }

        return when (mode) {
            ArtCropFillMode.CROP -> {
                val squareDim = Math.min(origW, origH)
                val cropX = when (alignment.col) {
                    0 -> 0
                    1 -> (origW - squareDim) / 2
                    else -> origW - squareDim
                }.coerceIn(0, Math.max(0, origW - squareDim))

                val cropY = when (alignment.row) {
                    0 -> 0
                    1 -> (origH - squareDim) / 2
                    else -> origH - squareDim
                }.coerceIn(0, Math.max(0, origH - squareDim))

                val cropped = Bitmap.createBitmap(original, cropX, cropY, squareDim, squareDim)
                Bitmap.createScaledBitmap(cropped, targetSize, targetSize, true)
            }
            ArtCropFillMode.FILL -> {
                val squareDim = Math.max(origW, origH)
                val filledBitmap = Bitmap.createBitmap(squareDim, squareDim, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(filledBitmap)

                // Sample dominant / corner color for natural filling
                val cornerColor = try { original.getPixel(0, 0) } catch (_: Exception) { Color.BLACK }
                canvas.drawColor(cornerColor)

                val dstX = when (alignment.col) {
                    0 -> 0
                    1 -> (squareDim - origW) / 2
                    else -> squareDim - origW
                }.toFloat()

                val dstY = when (alignment.row) {
                    0 -> 0
                    1 -> (squareDim - origH) / 2
                    else -> squareDim - origH
                }.toFloat()

                canvas.drawBitmap(original, dstX, dstY, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                Bitmap.createScaledBitmap(filledBitmap, targetSize, targetSize, true)
            }
            ArtCropFillMode.STRETCH -> {
                val squareDim = Math.max(origW, origH)
                val stretchedBitmap = Bitmap.createBitmap(squareDim, squareDim, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(stretchedBitmap)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

                val dstX = when (alignment.col) {
                    0 -> 0
                    1 -> (squareDim - origW) / 2
                    else -> squareDim - origW
                }

                val dstY = when (alignment.row) {
                    0 -> 0
                    1 -> (squareDim - origH) / 2
                    else -> squareDim - origH
                }

                // 1. Draw main original image at dstX, dstY
                canvas.drawBitmap(original, dstX.toFloat(), dstY.toFloat(), paint)

                // 2. Stretch top margin if dstY > 0
                if (dstY > 0) {
                    val topSlice = Bitmap.createBitmap(original, 0, 0, origW, 1)
                    val srcRect = android.graphics.Rect(0, 0, origW, 1)
                    val dstRect = android.graphics.Rect(dstX, 0, dstX + origW, dstY)
                    canvas.drawBitmap(topSlice, srcRect, dstRect, paint)
                }

                // 3. Stretch bottom margin if dstY + origH < squareDim
                if (dstY + origH < squareDim) {
                    val bottomSlice = Bitmap.createBitmap(original, 0, origH - 1, origW, 1)
                    val srcRect = android.graphics.Rect(0, 0, origW, 1)
                    val dstRect = android.graphics.Rect(dstX, dstY + origH, dstX + origW, squareDim)
                    canvas.drawBitmap(bottomSlice, srcRect, dstRect, paint)
                }

                // 4. Stretch left margin if dstX > 0
                if (dstX > 0) {
                    val leftSlice = Bitmap.createBitmap(original, 0, 0, 1, origH)
                    val srcRect = android.graphics.Rect(0, 0, 1, origH)
                    val dstRect = android.graphics.Rect(0, dstY, dstX, dstY + origH)
                    canvas.drawBitmap(leftSlice, srcRect, dstRect, paint)
                }

                // 5. Stretch right margin if dstX + origW < squareDim
                if (dstX + origW < squareDim) {
                    val rightSlice = Bitmap.createBitmap(original, origW - 1, 0, 1, origH)
                    val srcRect = android.graphics.Rect(0, 0, 1, origH)
                    val dstRect = android.graphics.Rect(dstX + origW, dstY, squareDim, dstY + origH)
                    canvas.drawBitmap(rightSlice, srcRect, dstRect, paint)
                }

                // 6. Fill top-left, top-right, bottom-left, bottom-right corners if needed
                if (dstX > 0 && dstY > 0) {
                    val tlColor = try { original.getPixel(0, 0) } catch (_: Exception) { Color.BLACK }
                    val p = Paint().apply { color = tlColor }
                    canvas.drawRect(0f, 0f, dstX.toFloat(), dstY.toFloat(), p)
                }
                if (dstX + origW < squareDim && dstY > 0) {
                    val trColor = try { original.getPixel(origW - 1, 0) } catch (_: Exception) { Color.BLACK }
                    val p = Paint().apply { color = trColor }
                    canvas.drawRect((dstX + origW).toFloat(), 0f, squareDim.toFloat(), dstY.toFloat(), p)
                }
                if (dstX > 0 && dstY + origH < squareDim) {
                    val blColor = try { original.getPixel(0, origH - 1) } catch (_: Exception) { Color.BLACK }
                    val p = Paint().apply { color = blColor }
                    canvas.drawRect(0f, (dstY + origH).toFloat(), dstX.toFloat(), squareDim.toFloat(), p)
                }
                if (dstX + origW < squareDim && dstY + origH < squareDim) {
                    val brColor = try { original.getPixel(origW - 1, origH - 1) } catch (_: Exception) { Color.BLACK }
                    val p = Paint().apply { color = brColor }
                    canvas.drawRect((dstX + origW).toFloat(), (dstY + origH).toFloat(), squareDim.toFloat(), squareDim.toFloat(), p)
                }

                Bitmap.createScaledBitmap(stretchedBitmap, targetSize, targetSize, true)
            }
        }
    }
}
