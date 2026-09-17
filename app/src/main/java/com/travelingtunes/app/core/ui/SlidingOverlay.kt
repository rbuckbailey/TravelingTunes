package com.travelingtunes.app.core.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.travelingtunes.app.core.model.GestureCategory
import com.travelingtunes.app.core.model.GestureTrigger
import com.travelingtunes.app.core.model.SlideDirection
import com.travelingtunes.app.core.model.getReverseTrigger
import com.travelingtunes.app.core.model.getSlideDirection
import kotlin.math.abs

@Composable
fun SlidingOverlay(
    visible: Boolean,
    slideDirection: SlideDirection,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    openingTrigger: GestureTrigger? = null,
    content: @Composable () -> Unit,
) {
    if (visible) {
        BackHandler {
            onDismiss()
        }
    }

    val (enterTransition, exitTransition) = remember(slideDirection) {
        val enterSpec = spring<androidx.compose.ui.unit.IntOffset>(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        )
        val exitSpec = tween<androidx.compose.ui.unit.IntOffset>(durationMillis = 200, easing = LinearOutSlowInEasing)

        when (slideDirection) {
            SlideDirection.TOP -> {
                (slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = enterSpec
                ) + fadeIn(animationSpec = tween(200))) to
                (slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = exitSpec
                ) + fadeOut(animationSpec = tween(180)))
            }
            SlideDirection.BOTTOM -> {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = enterSpec
                ) + fadeIn(animationSpec = tween(200)) to
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = exitSpec
                ) + fadeOut(animationSpec = tween(180))
            }
            SlideDirection.LEFT -> {
                slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = enterSpec
                ) + fadeIn(animationSpec = tween(200)) to
                slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = exitSpec
                ) + fadeOut(animationSpec = tween(180))
            }
            SlideDirection.RIGHT -> {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = enterSpec
                ) + fadeIn(animationSpec = tween(200)) to
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = exitSpec
                ) + fadeOut(animationSpec = tween(180))
            }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = enterTransition,
        exit = exitTransition,
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // Consume clicks on overlay surface so taps on the menu itself do not exit the menu
                    }
                    .overlayGestureDismiss(
                        openingTrigger = openingTrigger,
                        slideDirection = slideDirection,
                        onDismiss = onDismiss
                    ),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 8.dp
            ) {
                content()
            }
        }
    }
}

fun Modifier.overlayGestureDismiss(
    openingTrigger: GestureTrigger?,
    slideDirection: SlideDirection,
    onDismiss: () -> Unit
): Modifier = pointerInput(openingTrigger, slideDirection) {
    val minTranslationPx = 14f * density

    awaitEachGesture {
        val firstDown = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val startPosition = firstDown.position

        val seenPointerIds = mutableSetOf(firstDown.id)
        currentEvent.changes.forEach { if (it.pressed) seenPointerIds.add(it.id) }
        var maxFingers = seenPointerIds.size.coerceAtMost(3)

        var totalDx = 0f
        var totalDy = 0f
        var lastPosition = startPosition
        var trackedPointerId = firstDown.id

        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val activePointers = event.changes.filter { it.pressed }

            activePointers.forEach { seenPointerIds.add(it.id) }
            if (seenPointerIds.size > maxFingers) maxFingers = seenPointerIds.size.coerceAtMost(3)
            if (activePointers.size > maxFingers) maxFingers = activePointers.size.coerceAtMost(3)

            if (activePointers.isEmpty()) {
                if (maxFingers > 1) {
                    val trigger = when (maxFingers) {
                        2 -> GestureTrigger.TAP_2_1
                        3 -> GestureTrigger.TAP_3_1
                        else -> GestureTrigger.TAP_1_1
                    }
                    if (isReverseActionForOverlay(trigger, openingTrigger, slideDirection)) {
                        onDismiss()
                        return@awaitEachGesture
                    }
                }
                break
            }

            val currentPointer = activePointers.find { it.id == trackedPointerId } ?: activePointers.firstOrNull()
            if (currentPointer != null) {
                if (currentPointer.id != trackedPointerId) {
                    trackedPointerId = currentPointer.id
                    lastPosition = currentPointer.position
                } else {
                    val dx = currentPointer.position.x - lastPosition.x
                    val dy = currentPointer.position.y - lastPosition.y
                    totalDx += dx
                    totalDy += dy
                    lastPosition = currentPointer.position
                }

                val hasMovedPastMin = abs(totalDx) > minTranslationPx || abs(totalDy) > minTranslationPx
                if (hasMovedPastMin) {
                    val swipedTrigger = determineSwipeTrigger(maxFingers, totalDx, totalDy)
                    if (swipedTrigger != null && isReverseActionForOverlay(swipedTrigger, openingTrigger, slideDirection)) {
                        event.changes.forEach { it.consume() }
                        onDismiss()
                        return@awaitEachGesture
                    }
                }
            }
        }
    }
}

private fun isReverseActionForOverlay(
    trigger: GestureTrigger,
    openingTrigger: GestureTrigger?,
    slideDirection: SlideDirection
): Boolean {
    if (openingTrigger != null) {
        val reverseTrigger = openingTrigger.getReverseTrigger()
        if (trigger == reverseTrigger || trigger == openingTrigger) {
            return true
        }
    }

    val isMultiFingerSwipe = trigger.category == GestureCategory.TWO_FINGER_SWIPE || trigger.category == GestureCategory.THREE_FINGER_SWIPE
    if (isMultiFingerSwipe && trigger.getSlideDirection() != slideDirection) {
        return true
    }

    return false
}

private fun determineSwipeTrigger(fingers: Int, dx: Float, dy: Float): GestureTrigger? {
    val isHorizontal = abs(dx) > abs(dy)
    return when (fingers) {
        1 -> if (isHorizontal) {
            if (dx > 0) GestureTrigger.SWIPE_1_RIGHT else GestureTrigger.SWIPE_1_LEFT
        } else {
            if (dy < 0) GestureTrigger.SWIPE_1_UP else GestureTrigger.SWIPE_1_DOWN
        }
        2 -> if (isHorizontal) {
            if (dx > 0) GestureTrigger.SWIPE_2_RIGHT else GestureTrigger.SWIPE_2_LEFT
        } else {
            if (dy < 0) GestureTrigger.SWIPE_2_UP else GestureTrigger.SWIPE_2_DOWN
        }
        3 -> if (isHorizontal) {
            if (dx > 0) GestureTrigger.SWIPE_3_RIGHT else GestureTrigger.SWIPE_3_LEFT
        } else {
            if (dy < 0) GestureTrigger.SWIPE_3_UP else GestureTrigger.SWIPE_3_DOWN
        }
        else -> null
    }
}

