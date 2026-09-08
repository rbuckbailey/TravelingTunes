package com.travelingtunes.app.feature.quickstart

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickStartScreen(
    onDone: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { PAGES.size })

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
        }

        // Page Indicator Dots
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(PAGES.size) { iteration ->
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
