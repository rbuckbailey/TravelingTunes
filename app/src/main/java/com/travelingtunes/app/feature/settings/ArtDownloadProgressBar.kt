package com.travelingtunes.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ArtDownloadProgressBar(
    downloadedCount: Int,
    failedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    val total = if (totalCount > 0) totalCount else 1
    val processedCount = (downloadedCount + failedCount).coerceAtMost(total)
    val successFraction = (downloadedCount.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val failedFraction = (failedCount.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val remainingFraction = (1f - successFraction - failedFraction).coerceAtLeast(0f)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Progress: $processedCount / $totalCount",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            if (downloadedCount > 0) {
                Text(
                    text = "✓ $downloadedCount",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
            }
            if (failedCount > 0) {
                if (downloadedCount > 0) Text("  ", fontSize = 12.sp)
                Text(
                    text = "✗ $failedCount",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        if (downloadedCount == 0 && failedCount == 0) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = Color(0xFF2E7D32),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                    if (successFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(successFraction)
                                .background(Color(0xFF2E7D32))
                        )
                    }
                    if (failedFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(failedFraction)
                                .background(MaterialTheme.colorScheme.error)
                        )
                    }
                    if (remainingFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(remainingFraction)
                                .background(Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}
