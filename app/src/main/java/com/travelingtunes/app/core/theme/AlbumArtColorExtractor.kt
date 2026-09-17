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

        // Favor edge colors for letterboxing / background extraction according to priority
        val edgeSwatches = extractEdgeSwatches(targetBmp, innerEdge, priority)

        val allSwatches = palette.swatches.sortedByDescending { it.population }
        if (allSwatches.isEmpty() && edgeSwatches.isEmpty()) {
            return@withContext ColorTheme.MATCH_ALBUM_ART
        }

        // Maximized Edge Match Priority: Primary edge swatch is the background color
        val primaryBgSwatch = edgeSwatches.firstOrNull() ?: palette.dominantSwatch ?: allSwatches.firstOrNull()
        val bgInt = (primaryBgSwatch?.rgb ?: android.graphics.Color.BLACK) or 0xFF000000.toInt()

        val bgHsl = FloatArray(3)
        ColorUtils.colorToHSL(bgInt, bgHsl)
        val isBgSaturated = bgHsl[1] >= 0.12f

        // Ordered text candidates from palette swatches
        val preferredTextSwatches = listOfNotNull(
            palette.vibrantSwatch,
            palette.lightVibrantSwatch,
            palette.darkVibrantSwatch,
            palette.mutedSwatch,
            palette.lightMutedSwatch,
            palette.darkMutedSwatch
        )
        val textCandidates = (preferredTextSwatches + allSwatches)
            .distinctBy { it.rgb }
            .filter { (it.rgb or 0xFF000000.toInt()) != bgInt }

        val matchedSwatchesInts = (preferredTextSwatches + allSwatches).map { it.rgb }.distinct()

        // Score text candidates favoring complementary hue (180° opposite on color wheel) and high contrast/distance
        val scoredCandidates = textCandidates.map { swatch ->
            val candInt = swatch.rgb or 0xFF000000.toInt()
            val candHsl = FloatArray(3)
            ColorUtils.colorToHSL(candInt, candHsl)

            val contrast = calculateContrastSafe(candInt, bgInt)
            val distance = colorDistance(candInt, bgInt)
            val isCandSaturated = candHsl[1] >= 0.12f

            val hueDiff = calculateHueDifference(bgHsl[0], candHsl[0])
            val complementaryFactor = if (isBgSaturated && isCandSaturated) {
                1.0 + (hueDiff / 180.0)
            } else {
                1.0
            }

            val score = contrast * (distance + 10.0) * complementaryFactor
            Triple(swatch, contrast, score)
        }.sortedByDescending { it.third }

        // Find primary text color: pick top strict contrast or adjust brightness/saturation for best candidate
        val bestStrict = scoredCandidates.firstOrNull { it.second >= 4.5 }
        val primaryTextInt = if (bestStrict != null) {
            bestStrict.first.rgb or 0xFF000000.toInt()
        } else {
            val bestFallback = scoredCandidates.firstOrNull()
            if (bestFallback != null) {
                improveContrastHsl(bestFallback.first.rgb or 0xFF000000.toInt(), bgInt, targetContrast = 4.5)
            } else {
                // Synthesize complementary foreground color (180° rotated hue)
                val compHue = (bgHsl[0] + 180f) % 360f
                val bgLum = ColorUtils.calculateLuminance(bgInt)
                val isBgDark = bgLum < 0.5
                val compHsl = floatArrayOf(compHue, 0.75f, if (isBgDark) 0.85f else 0.15f)
                improveContrastHsl(ColorUtils.HSLToColor(compHsl) or 0xFF000000.toInt(), bgInt, targetContrast = 4.5)
            }
        }

        val secondaryTextInt = findSecondaryTextColor(bgInt, primaryTextInt, textCandidates)

        ColorTheme(
            name = "Album Art Dynamic",
            backgroundColor = Color(bgInt),
            textColor = Color(primaryTextInt),
            secondaryTextColor = Color(secondaryTextInt),
            matchedSwatches = matchedSwatchesInts
        )
    }

    fun improveContrastHsl(
        foregroundInt: Int,
        backgroundInt: Int,
        targetContrast: Double = 4.5
    ): Int {
        val fgInt = foregroundInt or 0xFF000000.toInt()
        val bgInt = backgroundInt or 0xFF000000.toInt()

        val currentContrast = calculateContrastSafe(fgInt, bgInt)
        if (currentContrast >= targetContrast) return fgInt

        val fgHsl = FloatArray(3)
        ColorUtils.colorToHSL(fgInt, fgHsl)
        val bgLum = ColorUtils.calculateLuminance(bgInt)
        val isBgDark = bgLum < 0.5

        var bestInt = fgInt
        var bestContrast = currentContrast

        for (step in 1..15) {
            if (isBgDark) {
                fgHsl[2] = (fgHsl[2] + 0.05f * step).coerceIn(0.50f, 1.0f)
                if (fgHsl[1] > 0.05f) fgHsl[1] = (fgHsl[1] * 1.05f).coerceIn(0.15f, 1.0f)
            } else {
                fgHsl[2] = (fgHsl[2] - 0.05f * step).coerceIn(0.0f, 0.40f)
            }

            val adjInt = (ColorUtils.HSLToColor(fgHsl) and 0x00FFFFFF) or 0xFF000000.toInt()
            val c = calculateContrastSafe(adjInt, bgInt)
            if (c > bestContrast) {
                bestContrast = c
                bestInt = adjInt
            }

            if (c >= targetContrast) {
                return adjInt
            }
        }

        if (bestContrast < targetContrast) {
            val whiteContrast = calculateContrastSafe(android.graphics.Color.WHITE, bgInt)
            val blackContrast = calculateContrastSafe(android.graphics.Color.BLACK, bgInt)
            return if (whiteContrast >= blackContrast) android.graphics.Color.WHITE else android.graphics.Color.BLACK
        }

        return bestInt
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

            val borderX = (width * 0.03f).toInt().coerceIn(2, 16)
            val borderY = (height * 0.03f).toInt().coerceIn(2, 16)
            val stepX = (width / 100).coerceAtLeast(1)
            val stepY = (height / 100).coerceAtLeast(1)

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
                        for (y in 0 until height step stepY) {
                            for (x in (width - borderX) until width) {
                                addPixel(x, y, isVerticalEdge = true)
                            }
                        }
                    }
                    InnerEdge.LEFT -> {
                        for (y in 0 until height step stepY) {
                            for (x in 0 until borderX) {
                                addPixel(x, y, isVerticalEdge = true)
                            }
                        }
                    }
                    InnerEdge.BOTTOM -> {
                        for (y in (height - borderY) until height) {
                            for (x in 0 until width step stepX) {
                                addPixel(x, y, isVerticalEdge = false)
                            }
                        }
                    }
                    InnerEdge.TOP -> {
                        for (y in 0 until borderY) {
                            for (x in 0 until width step stepX) {
                                addPixel(x, y, isVerticalEdge = false)
                            }
                        }
                    }
                }
            } else {
                // All 4 borders
                for (y in 0 until borderY) {
                    for (x in 0 until width step stepX) {
                        addPixel(x, y, isVerticalEdge = false)
                    }
                }
                for (y in (height - borderY) until height) {
                    for (x in 0 until width step stepX) {
                        addPixel(x, y, isVerticalEdge = false)
                    }
                }
                for (y in borderY until (height - borderY) step stepY) {
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

                if (hasClearWinner || primarySwatches.size <= 1) {
                    primarySwatches.distinctBy { it.rgb }
                } else {
                    // Only blend top swatches if they are color-similar (LAB distance <= 25.0)
                    val topSwatches = primarySwatches.take(3)
                    val firstLab = DoubleArray(3)
                    val otherLab = DoubleArray(3)
                    topSwatch?.rgb?.let { ColorUtils.colorToLAB(it, firstLab) }
                    val similarSwatches = topSwatches.filter { s ->
                        ColorUtils.colorToLAB(s.rgb, otherLab)
                        ColorUtils.distanceEuclidean(firstLab, otherLab) <= 25.0
                    }
                    if (similarSwatches.size > 1) {
                        val blendedRgb = blendColors(similarSwatches)
                        val blendedSwatch = Palette.Swatch(blendedRgb, totalPop)
                        (listOf(blendedSwatch) + primarySwatches).distinctBy { it.rgb }
                    } else {
                        primarySwatches.distinctBy { it.rgb }
                    }
                }
            } else {
                val secondarySwatches = clusterPixels(secondaryPixels)
                (primarySwatches + secondarySwatches).distinctBy { it.rgb }
            }
        } catch (e: Exception) {
            emptyList()
        }
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
        val primaryHsl = FloatArray(3)
        ColorUtils.colorToHSL(primaryTextInt, primaryHsl)

        // Pass 1: Strict pass requiring bg contrast >= 3.0, LAB dist to primary >= 28.0, and hue diff >= 30 degrees (if saturated)
        for (swatch in candidates) {
            val candInt = swatch.rgb or 0xFF000000.toInt()
            if (candInt == primaryTextInt) continue

            val contrastToBg = calculateContrastSafe(candInt, bgInt)
            if (contrastToBg < 3.0) continue

            val distToBg = colorDistance(candInt, bgInt)
            if (distToBg < 18.0) continue

            val distToPrimary = colorDistance(candInt, primaryTextInt)
            if (distToPrimary < 28.0) continue

            val candHsl = FloatArray(3)
            ColorUtils.colorToHSL(candInt, candHsl)

            if (primaryHsl[1] >= 0.15f && candHsl[1] >= 0.15f) {
                val hueDiff = calculateHueDifference(primaryHsl[0], candHsl[0])
                if (hueDiff < 30f) continue
            }

            return candInt
        }

        // Pass 2: Relaxed pass (bg contrast >= 2.5, LAB dist to primary >= 20.0, hue diff >= 20 degrees if saturated)
        for (swatch in candidates) {
            val candInt = swatch.rgb or 0xFF000000.toInt()
            if (candInt == primaryTextInt) continue

            val contrastToBg = calculateContrastSafe(candInt, bgInt)
            if (contrastToBg < 2.5) continue

            val distToPrimary = colorDistance(candInt, primaryTextInt)
            if (distToPrimary < 20.0) continue

            val candHsl = FloatArray(3)
            ColorUtils.colorToHSL(candInt, candHsl)

            if (primaryHsl[1] >= 0.15f && candHsl[1] >= 0.15f) {
                val hueDiff = calculateHueDifference(primaryHsl[0], candHsl[0])
                if (hueDiff < 20f) continue
            }

            return candInt
        }

        // Pass 3: Maximize score among valid candidates with bg contrast >= 2.5
        val validCandidates = candidates.map { it.rgb or 0xFF000000.toInt() }.filter { it != primaryTextInt && calculateContrastSafe(it, bgInt) >= 2.5 }
        val bestCandidate = validCandidates.maxByOrNull { candInt ->
            val candHsl = FloatArray(3)
            ColorUtils.colorToHSL(candInt, candHsl)
            val hueDiff = calculateHueDifference(primaryHsl[0], candHsl[0])
            val distToPrimary = colorDistance(candInt, primaryTextInt)
            val contrastToBg = calculateContrastSafe(candInt, bgInt)
            contrastToBg * (hueDiff.toDouble() + distToPrimary)
        }

        if (bestCandidate != null) {
            return bestCandidate
        }

        return synthesizeSecondaryColor(bgInt, primaryTextInt)
    }

    private fun calculateHueDifference(h1: Float, h2: Float): Float {
        val diff = kotlin.math.abs(h1 - h2)
        return minOf(diff, 360f - diff)
    }

    private fun synthesizeSecondaryColor(bgInt: Int, primaryTextInt: Int): Int {
        val primaryHsl = FloatArray(3)
        ColorUtils.colorToHSL(primaryTextInt, primaryHsl)

        val bgLum = ColorUtils.calculateLuminance(bgInt)
        val isBgDark = bgLum < 0.5

        if (primaryHsl[1] >= 0.15f) {
            primaryHsl[0] = (primaryHsl[0] + 50f) % 360f
        } else {
            if (isBgDark) {
                primaryHsl[2] = (primaryHsl[2] - 0.35f).coerceIn(0.55f, 0.85f)
            } else {
                primaryHsl[2] = (primaryHsl[2] + 0.35f).coerceIn(0.15f, 0.45f)
            }
        }
        val synthesized = ColorUtils.HSLToColor(primaryHsl) or 0xFF000000.toInt()
        if (calculateContrastSafe(synthesized, bgInt) >= 2.8) {
            return synthesized
        }
        return ColorUtils.blendARGB(primaryTextInt, bgInt, 0.35f) or 0xFF000000.toInt()
    }

    private fun calculateContrastSafe(foreground: Int, background: Int): Double {
        val fgOpaque = foreground or 0xFF000000.toInt()
        val bgOpaque = background or 0xFF000000.toInt()
        return try {
            ColorUtils.calculateContrast(fgOpaque, bgOpaque)
        } catch (e: IllegalArgumentException) {
            val lum1 = ColorUtils.calculateLuminance(fgOpaque)
            val lum2 = ColorUtils.calculateLuminance(bgOpaque)
            val maxLum = maxOf(lum1, lum2)
            val minLum = minOf(lum1, lum2)
            (maxLum + 0.05) / (minLum + 0.05)
        }
    }

    private fun colorDistance(c1: Int, c2: Int): Double {
        val lab1 = DoubleArray(3)
        val lab2 = DoubleArray(3)
        ColorUtils.colorToLAB(c1, lab1)
        ColorUtils.colorToLAB(c2, lab2)
        return ColorUtils.distanceEuclidean(lab1, lab2)
    }
}
