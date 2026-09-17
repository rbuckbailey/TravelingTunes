package com.travelingtunes.app.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelingtunes.app.core.model.GestureAction
import com.travelingtunes.app.core.model.RepeatMode
import com.travelingtunes.app.core.model.ShuffleMode
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Calculates the shortest angular distance in radians between two angles, in [0, PI].
 */
fun shortestAngleDiff(angle1: Float, angle2: Float): Float {
    var diff = angle1 - angle2
    while (diff < -PI.toFloat()) diff += (2 * PI).toFloat()
    while (diff > PI.toFloat()) diff -= (2 * PI).toFloat()
    return kotlin.math.abs(diff)
}

@Composable
fun RadialMenuOverlay(
    centerOffset: Offset,
    actions: List<GestureAction>,
    otherOptionKeys: List<String?> = emptyList(),
    dragOffset: Offset? = null,
    repeatMode: RepeatMode = RepeatMode.OFF,
    shuffleMode: ShuffleMode = ShuffleMode.OFF,
    isPlaying: Boolean = false,
    onSelectedActionChanged: (GestureAction?) -> Unit = {},
    onSelectedIndexChanged: (Int?) -> Unit = {},
    onSelectAction: (GestureAction) -> Unit,
    onDismiss: () -> Unit
) {
    if (actions.isEmpty()) {
        onDismiss()
        return
    }

    val displayActions = remember(actions) { actions.take(12) }
    val numActions = displayActions.size
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
    ) {
        val screenWidthPx = with(density) { this@BoxWithConstraints.maxWidth.toPx() }
        val screenHeightPx = with(density) { this@BoxWithConstraints.maxHeight.toPx() }
        val radiusPx = with(density) { 130.dp.toPx() }
        val marginPx = with(density) { 50.dp.toPx() }

        // Clamp the center point so the radial circle stays on screen
        val menuCenterX = if (centerOffset.isSpecified && centerOffset != Offset.Unspecified && centerOffset != Offset.Zero) {
            centerOffset.x.coerceIn(marginPx + radiusPx, screenWidthPx - marginPx - radiusPx)
        } else {
            screenWidthPx / 2f
        }

        val menuCenterY = if (centerOffset.isSpecified && centerOffset != Offset.Unspecified && centerOffset != Offset.Zero) {
            centerOffset.y.coerceIn(marginPx + radiusPx, screenHeightPx - marginPx - radiusPx)
        } else {
            screenHeightPx / 2f
        }

        var currentTouchPx by remember { mutableStateOf<Offset?>(null) }
        var selectedIndex by remember { mutableStateOf<Int?>(null) }

        fun updateSelection(touchPx: Offset) {
            currentTouchPx = touchPx
            val dx = touchPx.x - menuCenterX
            val dy = touchPx.y - menuCenterY
            val dist = hypot(dx, dy)
            val deadZonePx = with(density) { 18.dp.toPx() }

            if (dist > deadZonePx) {
                val touchAngle = atan2(dy, dx)
                var minDiff = Float.MAX_VALUE
                var bestIndex = 0

                for (i in 0 until numActions) {
                    val itemAngle = ((2 * PI * i) / numActions - (PI / 2)).toFloat()
                    val diff = shortestAngleDiff(itemAngle, touchAngle)
                    if (diff < minDiff) {
                        minDiff = diff
                        bestIndex = i
                    }
                }
                selectedIndex = bestIndex
            } else {
                selectedIndex = null
            }
        }

        LaunchedEffect(dragOffset) {
            val offset = dragOffset
            if (offset != null && offset.isSpecified && offset != Offset.Unspecified) {
                updateSelection(offset)
            }
        }

        LaunchedEffect(selectedIndex) {
            val selectedAction = selectedIndex?.let { displayActions.getOrNull(it) }
            onSelectedActionChanged(selectedAction)
            onSelectedIndexChanged(selectedIndex)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        var initialPointer = currentEvent.changes.firstOrNull { it.pressed }
                        if (initialPointer == null) {
                            initialPointer = awaitFirstDown(requireUnconsumed = false)
                        }

                        updateSelection(initialPointer.position)

                        while (true) {
                            val event = awaitPointerEvent()
                            val activePointers = event.changes.filter { it.pressed }

                            if (activePointers.isNotEmpty()) {
                                val currentPointer = activePointers.first()
                                updateSelection(currentPointer.position)
                                currentPointer.consume()
                            } else {
                                val sel = selectedIndex
                                if (sel != null && sel in displayActions.indices) {
                                    onSelectAction(displayActions[sel])
                                }
                                onDismiss()
                                break
                            }
                        }
                    }
                }
        ) {
            // Center Disk
            val centerDiskSize = 88.dp
            val selectedAction = selectedIndex?.let { displayActions.getOrNull(it) }

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (menuCenterX - with(density) { (centerDiskSize / 2).toPx() }).roundToInt(),
                            (menuCenterY - with(density) { (centerDiskSize / 2).toPx() }).roundToInt()
                        )
                    }
                    .size(centerDiskSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(
                        text = selectedAction?.displayName ?: "Radial Menu",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        fontSize = if (selectedAction != null) 11.sp else 10.sp
                    )
                }
            }

            // Radial Action Items around circle
            displayActions.forEachIndexed { i, action ->
                val itemAngle = ((2 * PI * i) / numActions - (PI / 2)).toFloat()
                val itemX = menuCenterX + radiusPx * cos(itemAngle)
                val itemY = menuCenterY + radiusPx * sin(itemAngle)

                val isHighlighted = selectedIndex == i

                // Rotational distance scale & alpha
                val (scale, alpha) = if (selectedIndex == null) {
                    1.0f to 1.0f
                } else {
                    val selAngle = ((2 * PI * (selectedIndex ?: 0)) / numActions - (PI / 2)).toFloat()
                    val rotDist = shortestAngleDiff(itemAngle, selAngle) // [0, PI]
                    val normDist = (rotDist / PI.toFloat()).coerceIn(0f, 1f)

                    if (isHighlighted) {
                        1.45f to 1.0f
                    } else {
                        // Decrease size (1.0 -> 0.65) and fade (1.0 -> 0.35) proportional to rotational distance
                        val calculatedScale = (1.0f - 0.35f * normDist).coerceIn(0.6f, 1.0f)
                        val calculatedAlpha = (1.0f - 0.65f * normDist).coerceIn(0.3f, 1.0f)
                        calculatedScale to calculatedAlpha
                    }
                }

                val itemSize = 44.dp

                val isRepeatActive = action == GestureAction.TOGGLE_REPEAT && repeatMode != RepeatMode.OFF
                val isShuffleActive = action == GestureAction.TOGGLE_SHUFFLE && shuffleMode != ShuffleMode.OFF
                val isActiveControl = isRepeatActive || isShuffleActive

                val itemBg = when {
                    isHighlighted -> MaterialTheme.colorScheme.primaryContainer
                    isActiveControl -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surface
                }
                val itemTint = when {
                    isHighlighted -> MaterialTheme.colorScheme.primary
                    isActiveControl -> MaterialTheme.colorScheme.onSecondaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                }

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (itemX - with(density) { (itemSize / 2).toPx() }).roundToInt(),
                                (itemY - with(density) { (itemSize / 2).toPx() }).roundToInt()
                            )
                        }
                        .scale(scale)
                        .alpha(alpha)
                        .size(itemSize)
                        .clip(CircleShape)
                        .background(itemBg)
                        .border(
                            width = if (isHighlighted || isActiveControl) 2.5.dp else 1.dp,
                            color = if (isHighlighted) MaterialTheme.colorScheme.primary else if (isActiveControl) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    ActionIcon(
                        action = action,
                        optionKey = otherOptionKeys.getOrNull(i),
                        repeatMode = repeatMode,
                        shuffleMode = shuffleMode,
                        isPlaying = isPlaying,
                        tint = itemTint,
                        iconSize = 24.dp
                    )
                }
            }
        }
    }
}
