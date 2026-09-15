package com.travelingtunes.app.core.theme

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.travelingtunes.app.core.model.ArtColorPriority
import com.travelingtunes.app.core.model.ColorTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class InnerEdge {
    LEFT, RIGHT, TOP, BOTTOM
}

object AlbumArtColorExtractor {

    suspend fun extractThemeFromBitmap(
        bitmap: Bitmap,
        innerEdge: InnerEdge? = null,
        priority: ArtColorPriority = ArtColorPriority.CENTER
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

        // Favor edge colors for letterboxing / background extraction according to priority
        val edgeSwatches = extractEdgeSwatches(targetBmp, innerEdge, priority)

        // Separate palette swatches into non-black and near-black
        val nonBlackPalette = allSwatches.filter { !isNearBlack(it.rgb) }
        val nearBlackPalette = allSwatches.filter { isNearBlack(it.rgb) }

        // Combine edge swatches first to favor edge colors, followed by non-black palette, then near-black palette
        val bgCandidates = (edgeSwatches + nonBlackPalette + nearBlackPalette).distinctBy { it.rgb }

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

        // Pass 2: Moderately strict fallback pass (contrast >= 3.5 and distance >= 22.0)
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

    private fun extractEdgeSwatches(
        bitmap: Bitmap,
        innerEdge: InnerEdge? = null,
        priority: ArtColorPriority = ArtColorPriority.CENTER
    ): List<Palette.Swatch> {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            if (width <= 0 || height <= 0) return emptyList()

            val borderX = (width * 0.015f).toInt().coerceIn(1, 8)
            val borderY = (height * 0.015f).toInt().coerceIn(1, 8)

            // Collect edge pixels classified into primary (prioritized) vs secondary regions based on normalized position t
            val primaryPixels = mutableListOf<Int>()
            val secondaryPixels = mutableListOf<Int>()

            fun addPixel(x: Int, y: Int, isVerticalEdge: Boolean) {
                val pixel = bitmap.getPixel(x, y) or 0xFF000000.toInt()
                val t = if (isVerticalEdge) y.toFloat() / height.coerceAtLeast(1) else x.toFloat() / width.coerceAtLeast(1)
                val isCenter = t in 0.25f..0.75f

                when (priority) {
                    ArtColorPriority.CENTER -> {
                        if (isCenter) primaryPixels.add(pixel) else secondaryPixels.add(pixel)
                    }
                    ArtColorPriority.OUTER_EDGE -> {
                        if (!isCenter) primaryPixels.add(pixel) else secondaryPixels.add(pixel)
                    }
                    ArtColorPriority.WHOLE -> {
                        primaryPixels.add(pixel)
                    }
                }
            }

            if (innerEdge != null) {
                when (innerEdge) {
                    InnerEdge.RIGHT -> {
                        for (y in 0 until height) {
                            for (x in (width - borderX) until width) {
                                addPixel(x, y, isVerticalEdge = true)
                            }
                        }
                    }
                    InnerEdge.LEFT -> {
                        for (y in 0 until height) {
                            for (x in 0 until borderX) {
                                addPixel(x, y, isVerticalEdge = true)
                            }
                        }
                    }
                    InnerEdge.BOTTOM -> {
                        for (y in (height - borderY) until height) {
                            for (x in 0 until width) {
                                addPixel(x, y, isVerticalEdge = false)
                            }
                        }
                    }
                    InnerEdge.TOP -> {
                        for (y in 0 until borderY) {
                            for (x in 0 until width) {
                                addPixel(x, y, isVerticalEdge = false)
                            }
                        }
                    }
                }
            } else {
                // All 4 borders
                for (y in 0 until borderY) {
                    for (x in 0 until width) {
                        addPixel(x, y, isVerticalEdge = false)
                    }
                }
                for (y in (height - borderY) until height) {
                    for (x in 0 until width) {
                        addPixel(x, y, isVerticalEdge = false)
                    }
                }
                for (y in borderY until (height - borderY)) {
                    for (x in 0 until borderX) {
                        addPixel(x, y, isVerticalEdge = true)
                    }
                    for (x in (width - borderX) until width) {
                        addPixel(x, y, isVerticalEdge = true)
                    }
                }
            }

            fun clusterPixels(pixels: List<Int>): List<Palette.Swatch> {
                if (pixels.isEmpty()) return emptyList()

                val exactCounts = HashMap<Int, Int>()
                for (pixel in pixels) {
                    exactCounts[pixel] = (exactCounts[pixel] ?: 0) + 1
                }

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

                return clusters.sortedByDescending { it.population }
            }

            val primarySwatches = clusterPixels(primaryPixels)

            if (priority == ArtColorPriority.WHOLE) {
                val totalPop = primarySwatches.sumOf { it.population }
                val topSwatch = primarySwatches.firstOrNull()
                val secondSwatch = primarySwatches.getOrNull(1)

                val hasClearWinner = when {
                    topSwatch == null -> false
                    secondSwatch == null -> true
                    totalPop > 0 && (topSwatch.population.toFloat() / totalPop) >= 0.40f -> true
                    secondSwatch.population > 0 && (topSwatch.population.toFloat() / secondSwatch.population) >= 1.8f -> true
                    else -> false
                }

                val nonBlackSwatches = primarySwatches.filter { !isNearBlack(it.rgb) }
                val nearBlackSwatches = primarySwatches.filter { isNearBlack(it.rgb) }

                if (hasClearWinner || nonBlackSwatches.size <= 1) {
                    (nonBlackSwatches + nearBlackSwatches).distinctBy { it.rgb }
                } else {
                    // Blend top edge swatches into a unified edge swatch
                    val blendedRgb = blendColors(nonBlackSwatches.take(3))
                    val blendedSwatch = Palette.Swatch(blendedRgb, totalPop)
                    (listOf(blendedSwatch) + nonBlackSwatches + nearBlackSwatches).distinctBy { it.rgb }
                }
            } else {
                val secondarySwatches = clusterPixels(secondaryPixels)

                val primaryNonBlack = primarySwatches.filter { !isNearBlack(it.rgb) }
                val secondaryNonBlack = secondarySwatches.filter { !isNearBlack(it.rgb) }
                val primaryNearBlack = primarySwatches.filter { isNearBlack(it.rgb) }
                val secondaryNearBlack = secondarySwatches.filter { isNearBlack(it.rgb) }

                (primaryNonBlack + secondaryNonBlack + primaryNearBlack + secondaryNearBlack).distinctBy { it.rgb }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun isNearBlack(colorInt: Int): Boolean {
        val lum = ColorUtils.calculateLuminance(colorInt)
        val r = (colorInt shr 16) and 0xFF
        val g = (colorInt shr 8) and 0xFF
        val b = colorInt and 0xFF
        return lum < 0.06 || (r < 25 && g < 25 && b < 25)
    }

    private fun blendColors(swatches: List<Palette.Swatch>): Int {
        if (swatches.isEmpty()) return android.graphics.Color.DKGRAY
        if (swatches.size == 1) return swatches[0].rgb

        var totalPop = 0L
        var rSum = 0.0
        var gSum = 0.0
        var bSum = 0.0

        for (s in swatches) {
            val pop = s.population.toLong().coerceAtLeast(1L)
            totalPop += pop
            rSum += ((s.rgb shr 16) and 0xFF) * pop
            gSum += ((s.rgb shr 8) and 0xFF) * pop
            bSum += (s.rgb and 0xFF) * pop
        }

        if (totalPop == 0L) return swatches[0].rgb

        val r = (rSum / totalPop).toInt().coerceIn(0, 255)
        val g = (gSum / totalPop).toInt().coerceIn(0, 255)
        val b = (bSum / totalPop).toInt().coerceIn(0, 255)

        return android.graphics.Color.rgb(r, g, b)
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

        return ColorUtils.blendARGB(primaryTextInt, bgInt, 0.30f)
    }
}
