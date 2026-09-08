package com.travelingtunes.app.core.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import com.travelingtunes.app.core.model.GestureTrigger
import kotlin.math.abs

interface GestureEventListener {
    fun onGestureTriggered(trigger: GestureTrigger)
    fun onContinuousGesture(
        trigger: GestureTrigger,
        delta: Float,
        deltaX: Float,
        deltaY: Float,
        totalDx: Float,
        totalDy: Float
    )
    fun onGestureEnd(totalDx: Float, totalDy: Float, fingers: Int)
}

fun Modifier.travelingTunesGestures(listener: GestureEventListener): Modifier =
    this.pointerInput(listener) {
        detectTravelingTunesGestures(listener)
    }

suspend fun PointerInputScope.detectTravelingTunesGestures(listener: GestureEventListener) {
    val minTranslationPx = 10f * density

    awaitEachGesture {
        val firstDown = awaitFirstDown(requireUnconsumed = false)
        val startTime = System.currentTimeMillis()
        val startPosition = firstDown.position

        var maxFingers = 1
        var totalDx = 0f
        var totalDy = 0f
        var isSwipeHandled = false
        var lastPosition = startPosition

        while (true) {
            val event = awaitPointerEvent()
            val activePointers = event.changes.filter { it.pressed }

            if (activePointers.size > maxFingers) {
                maxFingers = activePointers.size.coerceAtMost(3)
            }

            if (activePointers.isEmpty()) {
                // All fingers lifted
                val duration = System.currentTimeMillis() - startTime

                if (isSwipeHandled) {
                    listener.onGestureEnd(totalDx, totalDy, maxFingers)
                } else {
                    if (duration > 500L && abs(totalDx) < minTranslationPx && abs(totalDy) < minTranslationPx) {
                        // Long press detected
                        val trigger = when (maxFingers) {
                            1 -> GestureTrigger.LONG_PRESS_1
                            2 -> GestureTrigger.LONG_PRESS_2
                            3 -> GestureTrigger.LONG_PRESS_3
                            else -> GestureTrigger.LONG_PRESS_1
                        }
                        listener.onGestureTriggered(trigger)
                    } else if (abs(totalDx) < minTranslationPx && abs(totalDy) < minTranslationPx) {
                        // Check if corner tap or standard single tap
                        val cornerTrigger = detectCornerRegion(startPosition, size.width.toFloat(), size.height.toFloat())
                        if (cornerTrigger != null) {
                            listener.onGestureTriggered(cornerTrigger)
                        } else {
                            val trigger = when (maxFingers) {
                                1 -> GestureTrigger.TAP_1_1
                                2 -> GestureTrigger.TAP_2_1
                                else -> GestureTrigger.TAP_1_1
                            }
                            listener.onGestureTriggered(trigger)
                        }
                    }
                }
                break
            }

            // Track movement
            val currentPointer = activePointers.firstOrNull()
            if (currentPointer != null) {
                val dx = currentPointer.position.x - lastPosition.x
                val dy = currentPointer.position.y - lastPosition.y
                totalDx += dx
                totalDy += dy
                lastPosition = currentPointer.position

                if (!isSwipeHandled) {
                    if (abs(totalDx) > minTranslationPx || abs(totalDy) > minTranslationPx) {
                        isSwipeHandled = true
                        val trigger = determineSwipeTrigger(maxFingers, totalDx, totalDy)
                        if (trigger != null) {
                            listener.onGestureTriggered(trigger)
                        }
                    }
                }

                if (isSwipeHandled) {
                    val isVerticalSwipe = abs(totalDy) > abs(totalDx)
                    val trigger = if (isVerticalSwipe) {
                        if (dy < 0) {
                            determineSwipeTrigger(maxFingers, 0f, -100f)
                        } else if (dy > 0) {
                            determineSwipeTrigger(maxFingers, 0f, 100f)
                        } else {
                            determineSwipeTrigger(maxFingers, totalDx, totalDy)
                        }
                    } else {
                        determineSwipeTrigger(maxFingers, totalDx, totalDy)
                    }

                    if (trigger != null) {
                        val delta = if (!isVerticalSwipe) dx else -dy
                        listener.onContinuousGesture(trigger, delta, dx, dy, totalDx, totalDy)
                    }
                }
            }
        }
    }
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

private fun detectCornerRegion(pos: Offset, width: Float, height: Float): GestureTrigger? {
    if (width <= 0 || height <= 0) return null
    val relX = pos.x / width
    val relY = pos.y / height

    return when {
        relY < 0.20f && relX < 0.30f -> GestureTrigger.CORNER_TOP_LEFT
        relY < 0.20f && relX in 0.35f..0.65f -> GestureTrigger.CORNER_TOP_CENTER
        relY < 0.20f && relX > 0.70f -> GestureTrigger.CORNER_TOP_RIGHT
        relY > 0.80f && relX < 0.30f -> GestureTrigger.CORNER_BOTTOM_LEFT
        relY > 0.80f && relX in 0.35f..0.65f -> GestureTrigger.CORNER_BOTTOM_CENTER
        relY > 0.80f && relX > 0.70f -> GestureTrigger.CORNER_BOTTOM_RIGHT
        else -> null
    }
}
