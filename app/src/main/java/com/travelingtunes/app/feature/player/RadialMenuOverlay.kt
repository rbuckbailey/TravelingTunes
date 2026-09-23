package com.travelingtunes.app.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.travelingtunes.app.core.model.RadialMenuStyle
import com.travelingtunes.app.core.model.RepeatMode
import com.travelingtunes.app.core.model.ShuffleMode
import kotlin.math.PI
import kotlin.math.abs
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
    return abs(diff)
}

@Composable
fun RadialMenuOverlay(
    centerOffset: Offset,
    actions: List<GestureAction>,
    otherOptionKeys: List<String?> = emptyList(),
    dragOffset: Offset? = null,
    menuStyle: RadialMenuStyle = RadialMenuStyle.FAN,
    repeatMode: RepeatMode = RepeatMode.OFF,
    shuffleMode: ShuffleMode = ShuffleMode.OFF,
    isPlaying: Boolean = false,
    onSelectedActionChanged: (GestureAction?) -> Unit = {},
    onSelectedIndexChanged: (Int?) -> Unit = {},
    onSelectAction: (GestureAction) -> Unit,
    onDismiss: () -> Unit
) {
    val displayActions = remember(actions) { actions.filter { it != GestureAction.UNASSIGNED }.take(12) }
    if (displayActions.isEmpty()) {
        onDismiss()
        return
    }
    val numActions = displayActions.size
    val density = LocalDensity.current

    val itemSize = 42.dp
    val itemDiameterDp = itemSize.value
    val itemRadiusDp = itemDiameterDp / 2f // 21.dp
    val minPaddingDp = itemDiameterDp * 0.20f // 8.4.dp minimum 20% diameter padding between circles
    val minCenterDistanceDp = itemDiameterDp + minPaddingDp // 50.4.dp

    val centerDiskSize = 64.dp
    val minRadiusDp = (centerDiskSize.value / 2f) + itemRadiusDp + 4f // 57.dp (packed tight to center)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
    ) {
        val screenWidthPx = with(density) { this@BoxWithConstraints.maxWidth.toPx() }
        val screenHeightPx = with(density) { this@BoxWithConstraints.maxHeight.toPx() }

        val hasOffset = centerOffset.isSpecified && centerOffset != Offset.Unspecified && centerOffset != Offset.Zero
        val rawX = if (hasOffset) centerOffset.x else screenWidthPx / 2f
        val rawY = if (hasOffset) centerOffset.y else screenHeightPx / 2f

        val fanBaseAngle = remember(rawX, rawY, screenWidthPx, screenHeightPx) {
            atan2(screenHeightPx / 2f - rawY, screenWidthPx / 2f - rawX)
        }

        // 1. Calculate Full Circle (RADIAL) radius needed for N items to maintain >= 20% diameter padding
        val radialRadiusDp = if (numActions <= 1) minRadiusDp.dp else {
            val sinHalfAngle = sin(PI / numActions).toFloat()
            val rCalc = minCenterDistanceDp / (2f * sinHalfAngle)
            maxOf(minRadiusDp, rCalc).dp
        }

        val radialRadiusPx = with(density) { radialRadiusDp.toPx() }
        val itemRadiusPx = with(density) { itemRadiusDp.dp.toPx() }
        val totalRadialRadiusPx = radialRadiusPx + itemRadiusPx + with(density) { 6.dp.toPx() }

        // 2. Check if full circle fits at raw trigger position without clipping screen edges
        val fitsFullCircle = (rawX >= totalRadialRadiusPx) &&
                (rawX <= screenWidthPx - totalRadialRadiusPx) &&
                (rawY >= totalRadialRadiusPx) &&
                (rawY <= screenHeightPx - totalRadialRadiusPx)

        val effectiveStyle = when {
            menuStyle == RadialMenuStyle.LIST -> RadialMenuStyle.LIST
            menuStyle == RadialMenuStyle.RADIAL -> RadialMenuStyle.RADIAL
            fitsFullCircle -> RadialMenuStyle.RADIAL
            else -> RadialMenuStyle.FAN
        }

        // 3. Determine final effective radius
        val requiredRadiusDp = when (effectiveStyle) {
            RadialMenuStyle.LIST -> minRadiusDp.dp
            RadialMenuStyle.RADIAL -> radialRadiusDp
            RadialMenuStyle.FAN -> {
                when {
                    numActions <= 4 -> minRadiusDp.dp
                    numActions <= 6 -> 68.dp
                    numActions <= 8 -> 78.dp
                    else -> 90.dp
                }
            }
        }

        val radiusPx = with(density) { requiredRadiusDp.toPx() }
        val totalRadiusPx = radiusPx + itemRadiusPx + with(density) { 6.dp.toPx() }

        val marginPx = with(density) { 30.dp.toPx() }

        val menuCenterX = if (effectiveStyle == RadialMenuStyle.RADIAL) {
            rawX.coerceIn(totalRadiusPx, screenWidthPx - totalRadiusPx)
        } else {
            rawX.coerceIn(marginPx, screenWidthPx - marginPx)
        }

        val menuCenterY = if (effectiveStyle == RadialMenuStyle.RADIAL) {
            rawY.coerceIn(totalRadiusPx, screenHeightPx - totalRadiusPx)
        } else {
            rawY.coerceIn(marginPx, screenHeightPx - marginPx)
        }

        var selectedIndex by remember { mutableStateOf<Int?>(null) }

        fun getItemAngle(index: Int): Float {
            return when (effectiveStyle) {
                RadialMenuStyle.RADIAL -> {
                    ((2 * PI * index) / numActions - (PI / 2)).toFloat()
                }
                RadialMenuStyle.FAN -> {
                    if (numActions <= 1) {
                        fanBaseAngle
                    } else {
                        val arcSpan = (PI * 0.78f).toFloat()
                        val startAngle = fanBaseAngle - arcSpan / 2f
                        startAngle + (index * arcSpan / (numActions - 1))
                    }
                }
                RadialMenuStyle.LIST -> 0f
            }
        }

        fun updateSelection(touchPx: Offset) {
            val dx = touchPx.x - menuCenterX
            val dy = touchPx.y - menuCenterY
            val dist = hypot(dx, dy)
            val deadZonePx = with(density) { 16.dp.toPx() }

            if (effectiveStyle == RadialMenuStyle.LIST) {
                val itemHeightPx = with(density) { 48.dp.toPx() }
                val startY = menuCenterY - (numActions * itemHeightPx) / 2f
                val relativeY = touchPx.y - startY
                val bestIndex = (relativeY / itemHeightPx).toInt().coerceIn(0, numActions - 1)
                selectedIndex = if (dist > deadZonePx || abs(touchPx.y - menuCenterY) > deadZonePx) bestIndex else null
            } else {
                if (dist > deadZonePx) {
                    val touchAngle = atan2(dy, dx)
                    var minDiff = Float.MAX_VALUE
                    var bestIndex = 0

                    for (i in 0 until numActions) {
                        val itemAngle = getItemAngle(i)
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
                        val firstDown = awaitFirstDown(requireUnconsumed = false)
                        val startPos = firstDown.position
                        var maxDragDist = 0f

                        updateSelection(startPos)

                        while (true) {
                            val event = awaitPointerEvent()
                            val activePointers = event.changes.filter { it.pressed }

                            if (activePointers.isNotEmpty()) {
                                val currentPointer = activePointers.first()
                                val currentPos = currentPointer.position
                                val distFromStart = hypot(currentPos.x - startPos.x, currentPos.y - startPos.y)
                                if (distFromStart > maxDragDist) maxDragDist = distFromStart

                                updateSelection(currentPos)
                                currentPointer.consume()
                            } else {
                                // Finger released after drag
                                val dragThresholdPx = with(density) { 18.dp.toPx() }
                                val sel = selectedIndex
                                if (maxDragDist > dragThresholdPx && sel != null && sel in displayActions.indices) {
                                    onSelectAction(displayActions[sel])
                                }
                                // If released in center or without significant drag, keep menu open on screen!
                                break
                            }
                        }
                    }
                }
        ) {
            val selectedAction = selectedIndex?.let { displayActions.getOrNull(it) }

            if (effectiveStyle == RadialMenuStyle.LIST) {
                // Render List Menu
                val itemHeightDp = 48.dp
                val listWidthDp = 220.dp
                val startYPx = menuCenterY - with(density) { (numActions * itemHeightDp.toPx()) / 2f }

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (menuCenterX - with(density) { (listWidthDp / 2).toPx() }).roundToInt(),
                                startYPx.roundToInt()
                            )
                        }
                        .width(listWidthDp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        displayActions.forEachIndexed { i, action ->
                            val isHighlighted = selectedIndex == i
                            val isRepeatActive = action == GestureAction.TOGGLE_REPEAT && repeatMode != RepeatMode.OFF
                            val isShuffleActive = action == GestureAction.TOGGLE_SHUFFLE && shuffleMode != ShuffleMode.OFF
                            val isActiveControl = isRepeatActive || isShuffleActive

                            val itemBg = when {
                                isHighlighted -> MaterialTheme.colorScheme.primaryContainer
                                isActiveControl -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.surface
                            }
                            val itemTextColor = when {
                                isHighlighted -> MaterialTheme.colorScheme.onPrimaryContainer
                                isActiveControl -> MaterialTheme.colorScheme.onSecondaryContainer
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                            val itemTint = when {
                                isHighlighted -> MaterialTheme.colorScheme.primary
                                isActiveControl -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.onSurface
                            }

                            Surface(
                                modifier = Modifier
                                    .scale(if (isHighlighted) 1.05f else 1.0f)
                                    .clip(RoundedCornerShape(24.dp))
                                    .border(
                                        width = if (isHighlighted) 2.5.dp else 1.dp,
                                        color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    .clickable {
                                        onSelectAction(action)
                                    },
                                color = itemBg,
                                tonalElevation = if (isHighlighted) 8.dp else 2.dp
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .fillMaxSize()
                                ) {
                                    ActionIcon(
                                        action = action,
                                        optionKey = otherOptionKeys.getOrNull(i),
                                        repeatMode = repeatMode,
                                        shuffleMode = shuffleMode,
                                        isPlaying = isPlaying,
                                        tint = itemTint,
                                        iconSize = 22.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = action.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isHighlighted) FontWeight.ExtraBold else FontWeight.Bold,
                                        color = itemTextColor,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Center Disk for RADIAL and FAN modes
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
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                        .border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            text = selectedAction?.displayName ?: if (effectiveStyle == RadialMenuStyle.FAN) "Fan Menu" else "Radial Menu",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            fontSize = if (selectedAction != null) 11.sp else 10.sp
                        )
                    }
                }

                // Render Action Items along Fan Arc or Circle
                displayActions.forEachIndexed { i, action ->
                    val itemAngle = getItemAngle(i)
                    val itemX = menuCenterX + radiusPx * cos(itemAngle)
                    val itemY = menuCenterY + radiusPx * sin(itemAngle)

                    val isHighlighted = selectedIndex == i

                    val (scale, alpha) = if (selectedIndex == null) {
                        1.0f to 1.0f
                    } else {
                        val selAngle = getItemAngle(selectedIndex ?: 0)
                        val rotDist = shortestAngleDiff(itemAngle, selAngle)
                        val normDist = (rotDist / PI.toFloat()).coerceIn(0f, 1f)

                        if (isHighlighted) {
                            1.45f to 1.0f
                        } else {
                            val calculatedScale = (1.0f - 0.35f * normDist).coerceIn(0.6f, 1.0f)
                            val calculatedAlpha = (1.0f - 0.65f * normDist).coerceIn(0.35f, 1.0f)
                            calculatedScale to calculatedAlpha
                        }
                    }

                    val itemSize = 46.dp

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
                                color = if (isHighlighted) MaterialTheme.colorScheme.primary else if (isActiveControl) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                shape = CircleShape
                            )
                            .clickable {
                                onSelectAction(action)
                            },
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
}
