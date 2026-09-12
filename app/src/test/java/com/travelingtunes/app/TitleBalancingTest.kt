package com.travelingtunes.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TitleBalancingTest {

    private fun calculateWordScaleFactor(
        words: List<String>,
        charWidthPerSp: Float,
        baseFontSizeSp: Float,
        availableWidthPx: Float,
        minFontSizeSp: Float = 8f
    ): Float {
        var currentSp = baseFontSizeSp
        var maxWordWidthPx = 0f
        for (word in words) {
            val wordWidthPx = word.length * charWidthPerSp * currentSp
            if (wordWidthPx > maxWordWidthPx) {
                maxWordWidthPx = wordWidthPx
            }
        }

        if (maxWordWidthPx > availableWidthPx) {
            val scale = availableWidthPx / maxWordWidthPx
            currentSp = (currentSp * scale).coerceAtLeast(minFontSizeSp)
        }

        return currentSp
    }

    private fun splitBalancedLines(title: String): Pair<String, String> {
        val words = title.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (words.size <= 1) return Pair(title, "")

        var bestIndex = 1
        var minDiff = Int.MAX_VALUE

        for (i in 1 until words.size) {
            val line1 = words.subList(0, i).joinToString(" ")
            val line2 = words.subList(i, words.size).joinToString(" ")
            val diff = kotlin.math.abs(line1.length - line2.length)
            if (diff < minDiff) {
                minDiff = diff
                bestIndex = i
            }
        }

        val line1 = words.subList(0, bestIndex).joinToString(" ")
        val line2 = words.subList(bestIndex, words.size).joinToString(" ")
        return Pair(line1, line2)
    }

    @Test
    fun testShortWordDoesNotReduceFontSize() {
        val words = listOf("Bohemian", "Rhapsody")
        val scaledSp = calculateWordScaleFactor(
            words = words,
            charWidthPerSp = 0.5f,
            baseFontSizeSp = 32f,
            availableWidthPx = 300f
        )
        assertEquals(32f, scaledSp, 0.01f)
    }

    @Test
    fun testLongWordReducesFontSizeToFit() {
        val words = listOf("Supercalifragilisticexpialidocious")
        val baseSp = 32f
        val charWidth = 0.5f
        val availableWidth = 200f
        val scaledSp = calculateWordScaleFactor(
            words = words,
            charWidthPerSp = charWidth,
            baseFontSizeSp = baseSp,
            availableWidthPx = availableWidth
        )

        assertTrue("Font size must be reduced for long word", scaledSp < baseSp)
        val wordWidthPx = words[0].length * charWidth * scaledSp
        assertTrue("Single word must fit within available width", wordWidthPx <= availableWidth + 0.01f)
    }

    @Test
    fun testWordsAreKeptIntactInBalancedLines() {
        val title = "The Great Gatsby Special Edition"
        val (line1, line2) = splitBalancedLines(title)

        // Ensure no words are broken in half
        val originalWords = title.split(" ")
        val line1Words = line1.split(" ")
        val line2Words = line2.split(" ")

        assertEquals(originalWords, line1Words + line2Words)
        assertTrue("Line lengths should be balanced", kotlin.math.abs(line1.length - line2.length) <= 10)
    }
}
