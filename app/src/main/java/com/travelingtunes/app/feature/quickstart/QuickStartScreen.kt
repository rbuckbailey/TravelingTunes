package com.travelingtunes.app.feature.quickstart

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

data class MatrixRow(
    val shuffle: String,
    val repeat: String,
    val behavior: String
)

val MATRIX_ROWS = listOf(
    MatrixRow("Shuffle Off", "Repeat Off", "Plays queue in order once, then stops."),
    MatrixRow("Shuffle Off", "Repeat Song", "Loops active single track continuously."),
    MatrixRow("Shuffle Off", "Repeat Album", "Plays album in track order, loops album continuously."),
    MatrixRow("Shuffle Off", "Repeat Artist", "Plays artist's songs in order, loops artist continuously."),
    MatrixRow("Shuffle Off", "Repeat Genre", "Plays genre's songs in order, loops genre continuously."),
    MatrixRow("Shuffle Off", "Repeat Folder", "Plays folder's songs in order, loops folder continuously."),

    MatrixRow("Shuffle Songs", "Repeat Off", "Shuffles all songs, plays once to end, then stops."),
    MatrixRow("Shuffle Songs", "Repeat Song", "Loops active single track continuously."),
    MatrixRow("Shuffle Songs", "Repeat Album", "Shuffles songs within current album, loops album continuously."),
    MatrixRow("Shuffle Songs", "Repeat Artist", "Shuffles songs within current artist, loops artist continuously."),
    MatrixRow("Shuffle Songs", "Repeat Genre", "Shuffles songs within current genre, loops genre continuously."),
    MatrixRow("Shuffle Songs", "Repeat Folder", "Shuffles songs within current folder, loops folder continuously."),

    MatrixRow("Shuffle Albums", "Repeat Off", "Shuffles album order (keeping tracks in order within each album), plays once to end, then stops."),
    MatrixRow("Shuffle Albums", "Repeat Song", "Loops active single track continuously."),
    MatrixRow("Shuffle Albums", "Repeat Album", "Plays album in track order, loops album continuously."),
    MatrixRow("Shuffle Albums", "Repeat Artist", "Shuffles albums by artist, keeping tracks in order within each album, loops artist continuously."),
    MatrixRow("Shuffle Albums", "Repeat Genre", "Shuffles albums in genre, keeping tracks in order within each album, loops genre continuously."),
    MatrixRow("Shuffle Albums", "Repeat Folder", "Shuffles albums in folder, keeping tracks in order within each album, loops folder continuously.")
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
            .padding(24.dp),
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
                // Page 4: Shuffle & Repeat Matrix Table
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Shuffle & Repeat Matrix",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Combinations of Shuffle (Off, Songs, Albums) & Repeat (Off, Song, Album, Artist, Genre, Folder)",
                        fontSize = 13.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(MATRIX_ROWS) { row ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "🔀 ${row.shuffle}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF9BB2FF)
                                        )
                                        Text(
                                            text = "🔁 ${row.repeat}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFBCC4)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = row.behavior,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Page Indicator Dots
        Row(
            modifier = Modifier.padding(16.dp),
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
                .padding(vertical = 8.dp)
        ) {
            Text("Done")
        }
    }
}
