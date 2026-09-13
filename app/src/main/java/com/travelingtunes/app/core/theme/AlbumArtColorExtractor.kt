package com.travelingtunes.app.core.theme

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.travelingtunes.app.core.model.ColorTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class InnerEdge {
    LEFT, RIGHT, TOP, BOTTOM
}

object AlbumArtColorExtractor {

    suspend fun extractThemeFromBitmap(
        bitmap: Bitmap,
        innerEdge: InnerEdge? = null
    ): ColorTheme = withContext(Dispatchers.Default) {
        val safeBmp = if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }
        val targetBmp = safeBmp ?: bitmap
        val palette = Palette.from(targetBmp).generate()

        val allSwatches = palette.swatches.sortedByDescending { it.population }
        if (allSwatches.isEmpty()) {
            return@withContext ColorTheme.MATCH_ALBUM_ART
        }

        // Favor edge colors for letterboxing / background extraction
        val edgeSwatches = extractEdgeSwatches(targetBmp, innerEdge)

        // Combine edge swatches first to favor edge colors, followed by full image swatches
        val bgCandidates = (edgeSwatches + allSwatches).distinctBy { it.rgb }

        // Ordered text candidates from palette swatches
        val preferredTextSwatches = listOfNotNull(
            palette.vibrantSwatch,
            palette.lightVibrantSwatch,
            palette.darkVibrantSwatch,
            palette.mutedSwatch,
            palette.lightMutedSwatch,
            palette.darkMutedSwatch
        )

        val targetMinContrastStrict = 4.5
        val targetMinDistanceStrict = 35.0 // Perceptual distance in CIELAB space

        // Pass 1: Try text candidates down the line matching strict contrast and distinctness
        for (bgSwatch in bgCandidates) {
            val bgInt = bgSwatch.rgb

            val textCandidates = (preferredTextSwatches + allSwatches)
                .distinctBy { it.rgb }
                .filter { it.rgb != bgInt }

            for (primaryTextSwatch in textCandidates) {
                val primaryInt = primaryTextSwatch.rgb
                val contrast = ColorUtils.calculateContrast(primaryInt, bgInt)
                val distance = colorDistance(primaryInt, bgInt)

                if (contrast >= targetMinContrastStrict && distance >= targetMinDistanceStrict) {
                    val secondaryInt = findSecondaryTextColor(bgInt, primaryInt, textCandidates)
                    return@withContext ColorTheme(
                        name = "Album Art Dynamic",
                        backgroundColor = Color(bgInt),
                        textColor = Color(primaryInt),
                        secondaryTextColor = Color(secondaryInt)
                    )
                }
            }
        }

        // Pass 2: Moderately strict fallbackpass (moving down the line for contrast >= 3.5 and distance >= 22.0)
        for (bgSwatch in bgCandidates) {
            val bgInt = bgSwatch.rgb
            val textCandidates = (preferredTextSwatches + allSwatches)
                .distinctBy { it.rgb }
                .filter { it.rgb != bgInt }

            for (primaryTextSwatch in textCandidates) {
                val primaryInt = primaryTextSwatch.rgb
                val contrast = ColorUtils.calculateContrast(primaryInt, bgInt)
                val distance = colorDistance(primaryInt, bgInt)

                if (contrast >= 3.5 && distance >= 22.0) {
                    val secondaryInt = findSecondaryTextColor(bgInt, primaryInt, textCandidates)
                    return@withContext ColorTheme(
                        name = "Album Art Dynamic",
                        backgroundColor = Color(bgInt),
                        textColor = Color(primaryInt),
                        secondaryTextColor = Color(secondaryInt)
                    )
                }
            }
        }

        // Pass 3: Select the artwork swatch down the line that offers the highest distinctness
        val primaryBgInt = (edgeSwatches.firstOrNull() ?: palette.dominantSwatch ?: allSwatches.first()).rgb
        val allTextCandidates = (preferredTextSwatches + allSwatches).distinctBy { it.rgb }.filter { it.rgb != primaryBgInt }

        val bestArtworkCandidate = allTextCandidates.maxByOrNull {
            ColorUtils.calculateContrast(it.rgb, primaryBgInt) * colorDistance(it.rgb, primaryBgInt)
        }

        val primaryTextInt = if (bestArtworkCandidate != null && ColorUtils.calculateContrast(bestArtworkCandidate.rgb, primaryBgInt) >= 2.8) {
            bestArtworkCandidate.rgb
        } else {
            val whiteContrast = ColorUtils.calculateContrast(android.graphics.Color.WHITE, primaryBgInt)
            val blackContrast = ColorUtils.calculateContrast(android.graphics.Color.BLACK, primaryBgInt)
            if (whiteContrast >= blackContrast) android.graphics.Color.WHITE else android.graphics.Color.BLACK
        }

        val secondaryTextInt = findSecondaryTextColor(primaryBgInt, primaryTextInt, allTextCandidates)

        ColorTheme(
            name = "Album Art Dynamic",
            backgroundColor = Color(primaryBgInt),
            textColor = Color(primaryTextInt),
            secondaryTextColor = Color(secondaryTextInt)
        )
    }

    private fun extractEdgeSwatches(bitmap: Bitmap, innerEdge: InnerEdge? = null): List<Palette.Swatch> {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            if (width <= 0 || height <= 0) return emptyList()

            // Sample directly from unscaled bitmap edge to preserve exact edge colors without scaling artifacts
            val borderX = (width * 0.015f).toInt().coerceIn(1, 8)
            val borderY = (height * 0.015f).toInt().coerceIn(1, 8)

            val edgePixels: IntArray
            var index = 0

            if (innerEdge != null) {
                when (innerEdge) {
                    InnerEdge.RIGHT -> {
                        edgePixels = IntArray(height * borderX)
                        for (y in 0 until height) {
                            for (x in (width - borderX) until width) {
                                edgePixels[index++] = bitmap.getPixel(x, y)
                            }
                        }
                    }
                    InnerEdge.LEFT -> {
                        edgePixels = IntArray(height * borderX)
                        for (y in 0 until height) {
                            for (x in 0 until borderX) {
                                edgePixels[index++] = bitmap.getPixel(x, y)
                            }
                        }
                    }
                    InnerEdge.BOTTOM -> {
                        edgePixels = IntArray(width * borderY)
                        for (y in (height - borderY) until height) {
                            for (x in 0 until width) {
                                edgePixels[index++] = bitmap.getPixel(x, y)
                            }
                        }
                    }
                    InnerEdge.TOP -> {
                        edgePixels = IntArray(width * borderY)
                        for (y in 0 until borderY) {
                            for (x in 0 until width) {
                                edgePixels[index++] = bitmap.getPixel(x, y)
                            }
                        }
                    }
                }
            } else {
                // All 4 borders
                val totalCap = width * borderY * 2 + (height - borderY * 2) * borderX * 2
                edgePixels = IntArray(totalCap)
                // Top border
                for (y in 0 until borderY) {
                    for (x in 0 until width) {
                        edgePixels[index++] = bitmap.getPixel(x, y)
                    }
                }
                // Bottom border
                for (y in (height - borderY) until height) {
                    for (x in 0 until width) {
                        edgePixels[index++] = bitmap.getPixel(x, y)
                    }
                }
                // Left & Right borders (middle)
                for (y in borderY until (height - borderY)) {
                    for (x in 0 until borderX) {
                        edgePixels[index++] = bitmap.getPixel(x, y)
                    }
                    for (x in (width - borderX) until width) {
                        edgePixels[index++] = bitmap.getPixel(x, y)
                    }
                }
            }

            if (index <= 0) return emptyList()

            // Count frequency distribution of exact pixel RGB values along the edge
            val exactCounts = HashMap<Int, Int>()
            for (i in 0 until index) {
                val pixel = edgePixels[i] or 0xFF000000.toInt()
                exactCounts[pixel] = (exactCounts[pixel] ?: 0) + 1
            }

            // Cluster near-identical pixel colors (e.g. JPEG noise) while keeping exact pixel RGB values
            val sortedExact = exactCounts.entries.sortedByDescending { it.value }
            val clusters = mutableListOf<Palette.Swatch>()
            val visited = HashSet<Int>()

            val lab1 = DoubleArray(3)
            val lab2 = DoubleArray(3)

            for (entry in sortedExact) {
                val exactColor = entry.key
                if (exactColor in visited) continue

                var totalPopulation = 0
                ColorUtils.colorToLAB(exactColor, lab1)

                for (otherEntry in sortedExact) {
                    val otherColor = otherEntry.key
                    if (otherColor in visited) continue

                    ColorUtils.colorToLAB(otherColor, lab2)
                    if (ColorUtils.distanceEuclidean(lab1, lab2) <= 5.0) {
                        totalPopulation += otherEntry.value
                        visited.add(otherColor)
                    }
                }

                clusters.add(Palette.Swatch(exactColor, totalPopulation))
            }

            clusters.sortedByDescending { it.population }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun findSecondaryTextColor(
        bgInt: Int,
        primaryTextInt: Int,
        candidates: List<Palette.Swatch>
    ): Int {
        for (swatch in candidates) {
            val candInt = swatch.rgb
            if (candInt == primaryTextInt) continue

            val contrastToBg = ColorUtils.calculateContrast(candInt, bgInt)
            if (contrastToBg < 2.5) continue

            val distToBg = colorDistance(candInt, bgInt)
            if (distToBg < 18.0) continue

            val distToPrimary = colorDistance(candInt, primaryTextInt)
            if (distToPrimary >= 12.0) {
                return candInt
            }
        }

        return blendSecondaryColor(bgInt, primaryTextInt)
    }

    private fun colorDistance(c1: Int, c2: Int): Double {
        val lab1 = DoubleArray(3)
        val lab2 = DoubleArray(3)
        ColorUtils.colorToLAB(c1, lab1)
        ColorUtils.colorToLAB(c2, lab2)
        return ColorUtils.distanceEuclidean(lab1, lab2)
    }

    private fun blendSecondaryColor(bgInt: Int, primaryTextInt: Int): Int {
        val bgLuminance = ColorUtils.calculateLuminance(bgInt)
        val primaryLuminance = ColorUtils.calculateLuminance(primaryTextInt)

        return if (primaryLuminance > bgLuminance) {
            ColorUtils.blendARGB(primaryTextInt, bgInt, 0.30f)
        } else {
            ColorUtils.blendARGB(primaryTextInt, bgInt, 0.30f)
        }
    }
}
