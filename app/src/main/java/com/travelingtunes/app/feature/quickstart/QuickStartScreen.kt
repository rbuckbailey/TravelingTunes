package com.travelingtunes.app.feature.quickstart

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class QuickStartPage(
    val title: String,
    val description: String,
    val gestureInstructions: List<String>
)

val PAGES = listOf(
    QuickStartPage(
        title = "Welcome to Traveling Tunes",
        description = "Designed for blind-friendly & touch-free music control while traveling.",
        gestureInstructions = listOf(
            "1-Finger Tap: Play / Pause",
            "1-Finger Swipe Up: Volume Up",
            "1-Finger Swipe Down: Volume Down",
            "1-Finger Swipe Left: Rewind",
            "1-Finger Swipe Right: Fast Forward"
        )
    ),
    QuickStartPage(
        title = "2-Finger Gesture Controls",
        description = "Use two fingers for track navigation & song selection.",
        gestureInstructions = listOf(
            "2-Finger Swipe Right: Next Song",
            "2-Finger Swipe Left: Restart / Previous",
            "2-Finger Tap: Open Song Picker",
            "2-Finger Swipe Up: Increase Rating",
            "2-Finger Swipe Down: Decrease Rating"
        )
    ),
    QuickStartPage(
        title = "3-Finger & Corner Controls",
        description = "Advanced artist/album controls and corner triggers.",
        gestureInstructions = listOf(
            "3-Finger Swipe Up: Play Current Artist",
            "3-Finger Swipe Down: Play Current Album",
            "Top-Left Corner: Toggle Repeat",
            "Top-Right Corner: Toggle Shuffle",
            "1-Finger Long Press: Open Settings Menu"
        )
    )
)

val SHUFFLE_MODES = listOf("Shuffle Off", "Shuffle Songs", "Shuffle Albums")
val REPEAT_MODES = listOf("Repeat Off", "Repeat Song", "Repeat Album", "Repeat Artist", "Repeat Genre", "Repeat Folder")

val MATRIX_GRID: Array<Array<String>> = arrayOf(
    // Row 0: Repeat Off
    arrayOf(
        "Plays queue in order once, then stops.",
        "Shuffles all songs, plays once to end, then stops.",
        "Shuffles album order (keeping tracks in order), plays once to end, then stops."
    ),
    // Row 1: Repeat Song
    arrayOf(
        "Loops active single track continuously.",
        "Loops active single track continuously.",
        "Loops active single track continuously."
    ),
    // Row 2: Repeat Album
    arrayOf(
        "Plays album in track order, loops album continuously.",
        "Shuffles songs within current album, loops album continuously.",
        "Plays album in track order, loops album continuously."
    ),
    // Row 3: Repeat Artist
    arrayOf(
        "Plays artist's songs in order, loops artist continuously.",
        "Shuffles songs by artist, loops artist continuously.",
        "Shuffles albums by artist (keeping tracks in order), loops artist continuously."
    ),
    // Row 4: Repeat Genre
    arrayOf(
        "Plays genre's songs in order, loops genre continuously.",
        "Shuffles songs in genre, loops genre continuously.",
        "Shuffles albums in genre (keeping tracks in order), loops genre continuously."
    ),
    // Row 5: Repeat Folder
    arrayOf(
        "Plays folder's songs in order, loops folder continuously.",
        "Shuffles songs in folder, loops folder continuously.",
        "Shuffles albums in folder (keeping tracks in order), loops folder continuously."
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickStartScreen(
    onDone: () -> Unit
) {
    val totalPages = PAGES.size + 1
    val pagerState = rememberPagerState(pageCount = { totalPages })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { pageIndex ->
            if (pageIndex < PAGES.size) {
                val page = PAGES[pageIndex]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = page.title,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = page.description,
                        fontSize = 16.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    page.gestureInstructions.forEach { instr ->
                        Text(
                            text = "• $instr",
                            fontSize = 18.sp,
                            color = Color.Yellow,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        )
                    }
                }
            } else {
                // Page 4: 3x6 Shuffle & Repeat Grid Matrix Table
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Shuffle & Repeat Matrix (3x6)",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Grid matrix combining 3 Shuffle settings & 6 Repeat settings",
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ShuffleRepeatGridMatrix(modifier = Modifier.weight(1f))
                }
            }
        }

        // Page Indicator Dots
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(totalPages) { iteration ->
                val color = if (pagerState.currentPage == iteration) Color.White else Color.DarkGray
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(10.dp)
                )
            }
        }

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text("Done")
        }
    }
}

@Composable
fun ShuffleRepeatGridMatrix(
    modifier: Modifier = Modifier
) {
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()

    val rowHeaderWidth = 110.dp
    val colWidth = 180.dp

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScrollState)
        ) {
            Column {
                // Table Header Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Top-Left Header Cell
                    Box(
                        modifier = Modifier
                            .width(rowHeaderWidth)
                            .height(48.dp)
                            .clip(RoundedCornerShape(topStart = 8.dp))
                            .background(Color(0xFF2C2C2C))
                            .border(0.5.dp, Color(0xFF444444)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Repeat \\ Shuffle",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                    }

                    // 3 Shuffle Column Headers
                    SHUFFLE_MODES.forEachIndexed { colIdx, shuffleTitle ->
                        Box(
                            modifier = Modifier
                                .width(colWidth)
                                .height(48.dp)
                                .clip(if (colIdx == SHUFFLE_MODES.lastIndex) RoundedCornerShape(topEnd = 8.dp) else RoundedCornerShape(0.dp))
                                .background(Color(0xFF333A56))
                                .border(0.5.dp, Color(0xFF555E88)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🔀 $shuffleTitle",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF9BB2FF),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Table Data Rows (6 Repeat Rows)
                Column(
                    modifier = Modifier.verticalScroll(verticalScrollState)
                ) {
                    REPEAT_MODES.forEachIndexed { rowIdx, repeatTitle ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Left Row Header Cell
                            Box(
                                modifier = Modifier
                                    .width(rowHeaderWidth)
                                    .height(86.dp)
                                    .background(Color(0xFF3A282B))
                                    .border(0.5.dp, Color(0xFF66444A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🔁\n$repeatTitle",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFBCC4),
                                    textAlign = TextAlign.Center
                                )
                            }

                            // 3 Data Cells for this Repeat Mode
                            SHUFFLE_MODES.indices.forEach { colIdx ->
                                val behaviorText = MATRIX_GRID[rowIdx][colIdx]
                                Box(
                                    modifier = Modifier
                                        .width(colWidth)
                                        .height(86.dp)
                                        .background(if (rowIdx % 2 == 0) Color(0xFF1E1E1E) else Color(0xFF161616))
                                        .border(0.5.dp, Color(0xFF333333))
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = behaviorText,
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
