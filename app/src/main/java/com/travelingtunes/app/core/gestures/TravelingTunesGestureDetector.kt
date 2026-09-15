package com.travelingtunes.app.core.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import com.travelingtunes.app.core.model.GestureAction
import com.travelingtunes.app.core.model.GestureBinding
import com.travelingtunes.app.core.model.GestureTrigger
import kotlin.math.abs
import kotlin.math.hypot

interface GestureEventListener {
    fun onGestureTriggered(trigger: GestureTrigger, isLongPress: Boolean = false, touchOffset: Offset = Offset.Unspecified): Boolean
    fun onContinuousGesture(
        trigger: GestureTrigger,
        delta: Float,
        deltaX: Float,
        deltaY: Float,
        totalDx: Float,
        totalDy: Float
    )
    fun onGesturePointerMove(touchOffset: Offset) {}
    fun onGesturePointerUp(touchOffset: Offset) {}
    fun onGestureEnd(totalDx: Float, totalDy: Float, fingers: Int)
}

@Composable
fun Modifier.travelingTunesGestures(
    listener: GestureEventListener,
    gestureBindings: Map<GestureTrigger, GestureBinding>? = null,
    numEdgeRegions: Int = 3,
    regionBounds: Rect = Rect(0f, 0f, 1f, 1f),
    isOverlayOpen: Boolean = false
): Modifier {
    val currentListener by rememberUpdatedState(listener)
    val currentBindings by rememberUpdatedState(gestureBindings)

    return this.pointerInput(isOverlayOpen, regionBounds, numEdgeRegions) {
        detectTravelingTunesGestures(
            listener = currentListener,
            gestureBindings = currentBindings,
            numEdgeRegions = numEdgeRegions,
            regionBounds = regionBounds,
            isOverlayOpen = isOverlayOpen
        )
    }
}

suspend fun PointerInputScope.detectTravelingTunesGestures(
    listener: GestureEventListener,
    gestureBindings: Map<GestureTrigger, GestureBinding>? = null,
    numEdgeRegions: Int = 3,
    regionBounds: Rect = Rect(0f, 0f, 1f, 1f),
    isOverlayOpen: Boolean = false
) {
    val minTranslationPx = 12f * density
    val slopPx = 40f * density
    val doubleTapTimeoutMs = 300L

    awaitEachGesture {
        val firstDown = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val tap1 = awaitPressResult(firstDown, minTranslationPx, listener, numEdgeRegions, regionBounds)

        if (tap1.isSwipe || tap1.isLongPress) {
            return@awaitEachGesture
        }

        if (isOverlayOpen && tap1.fingers == 1) {
            return@awaitEachGesture
        }

        val canDouble1 = hasDoubleTap(tap1.fingers, gestureBindings)
        val canTriple1 = hasTripleTap(tap1.fingers, gestureBindings)

        if (!canDouble1 && !canTriple1) {
            emitSingleTap(tap1.fingers, tap1.startPosition, size.width.toFloat(), size.height.toFloat(), listener, numEdgeRegions, regionBounds)
            return@awaitEachGesture
        }

        // Wait for potential second tap down
        val secondDown = try {
            withTimeout(doubleTapTimeoutMs) {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            }
        } catch (_: PointerEventTimeoutCancellationException) {
            null
        }

        if (secondDown == null) {
            if (!isOverlayOpen || tap1.fingers > 1) {
                emitSingleTap(tap1.fingers, tap1.startPosition, size.width.toFloat(), size.height.toFloat(), listener, numEdgeRegions, regionBounds)
            }
            return@awaitEachGesture
        }

        val dist12 = hypot(secondDown.position.x - tap1.startPosition.x, secondDown.position.y - tap1.startPosition.y)
        if (dist12 > slopPx) {
            if (!isOverlayOpen || tap1.fingers > 1) {
                emitSingleTap(tap1.fingers, tap1.startPosition, size.width.toFloat(), size.height.toFloat(), listener, numEdgeRegions, regionBounds)
            }
            return@awaitEachGesture
        }

        val tap2 = awaitPressResult(secondDown, minTranslationPx, listener, numEdgeRegions, regionBounds)
        if (tap2.isSwipe || tap2.isLongPress) {
            emitSingleTap(tap1.fingers, tap1.startPosition, size.width.toFloat(), size.height.toFloat(), listener, numEdgeRegions, regionBounds)
            return@awaitEachGesture
        }

        val effectiveFingers2 = maxOf(tap1.fingers, tap2.fingers).coerceAtMost(3)

        if (!canTriple1) {
            emitDoubleTap(effectiveFingers2, listener)
            return@awaitEachGesture
        }

        // Wait for potential third tap down
        val thirdDown = try {
            withTimeout(doubleTapTimeoutMs) {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            }
        } catch (_: PointerEventTimeoutCancellationException) {
            null
        }

        if (thirdDown == null) {
            emitDoubleTap(effectiveFingers2, listener)
            return@awaitEachGesture
        }

        val dist23 = hypot(thirdDown.position.x - tap2.startPosition.x, thirdDown.position.y - tap2.startPosition.y)
        if (dist23 > slopPx) {
            emitDoubleTap(effectiveFingers2, listener)
            return@awaitEachGesture
        }

        val tap3 = awaitPressResult(thirdDown, minTranslationPx, listener, numEdgeRegions, regionBounds)
        if (tap3.isSwipe || tap3.isLongPress) {
            emitDoubleTap(effectiveFingers2, listener)
            return@awaitEachGesture
        }

        val effectiveFingers3 = maxOf(effectiveFingers2, tap3.fingers).coerceAtMost(3)
        emitTripleTap(effectiveFingers3, listener)
    }
}

private data class TapPressResult(
    val fingers: Int,
    val startPosition: Offset,
    val durationMs: Long,
    val totalDx: Float,
    val totalDy: Float,
    val isSwipe: Boolean,
    val isLongPress: Boolean
)

private suspend fun AwaitPointerEventScope.awaitPressResult(
    firstDown: PointerInputChange,
    minTranslationPx: Float,
    listener: GestureEventListener,
    numEdgeRegions: Int = 3,
    regionBounds: Rect = Rect(0f, 0f, 1f, 1f)
): TapPressResult {
    val startTime = System.currentTimeMillis()
    val startPosition = firstDown.position
    val multiTouchWindowMs = 120L
    val longPressThresholdMs = 380L
    val longPressSlopPx = 32f * density

    val systemEdgeMarginPx = maxOf(32f * density, size.height.toFloat() * 0.05f)
    val isSystemEdgeDrag = startPosition.y < systemEdgeMarginPx || startPosition.y > (size.height.toFloat() - systemEdgeMarginPx)

    val seenPointerIds = mutableSetOf<PointerId>()
    seenPointerIds.add(firstDown.id)

    currentEvent.changes.forEach {
        if (it.pressed) {
            seenPointerIds.add(it.id)
        }
    }

    var maxFingers = seenPointerIds.size.coerceAtMost(3)
    var totalDx = 0f
    var totalDy = 0f
    var isSwipeHandled = false
    var isLongPressHandled = false
    var swipedTrigger: GestureTrigger? = null
    var lastPosition = startPosition
    var trackedPointerId: PointerId? = firstDown.id

    while (true) {
        val currentTime = System.currentTimeMillis()
        val duration = currentTime - startTime

        val event = if (!isSwipeHandled && !isLongPressHandled && duration < longPressThresholdMs) {
            val timeoutRemaining = longPressThresholdMs - duration
            try {
                withTimeout(timeoutRemaining) {
                    awaitPointerEvent(PointerEventPass.Initial)
                }
            } catch (_: PointerEventTimeoutCancellationException) {
                null
            }
        } else {
            awaitPointerEvent(PointerEventPass.Initial)
        }

        // Long press timeout expired while finger is still held down
        if (event == null) {
            val currDuration = System.currentTimeMillis() - startTime
            if (!isSwipeHandled && !isLongPressHandled && currDuration >= longPressThresholdMs) {
                val totalDist = hypot(totalDx, totalDy)
                if (totalDist < longPressSlopPx) {
                    val trigger = if (maxFingers == 1) {
                        detectCornerRegion(startPosition, size.width.toFloat(), size.height.toFloat(), numEdgeRegions, regionBounds) ?: GestureTrigger.LONG_PRESS_1
                    } else {
                        when (maxFingers) {
                            2 -> GestureTrigger.LONG_PRESS_2
                            3 -> GestureTrigger.LONG_PRESS_3
                            else -> GestureTrigger.LONG_PRESS_1
                        }
                    }
                    val handled = listener.onGestureTriggered(trigger, isLongPress = true, touchOffset = startPosition)
                    if (handled) {
                        isLongPressHandled = true
                    }
                }
            }
            continue
        }

        val activePointers = event.changes.filter { it.pressed }

        activePointers.forEach {
            seenPointerIds.add(it.id)
        }

        if (seenPointerIds.size > maxFingers) {
            maxFingers = seenPointerIds.size.coerceAtMost(3)
        }
        if (activePointers.size > maxFingers) {
            maxFingers = activePointers.size.coerceAtMost(3)
        }

        if (activePointers.isEmpty()) {
            if (isLongPressHandled) {
                listener.onGesturePointerUp(lastPosition)
                return TapPressResult(
                    fingers = maxFingers,
                    startPosition = startPosition,
                    durationMs = duration,
                    totalDx = totalDx,
                    totalDy = totalDy,
                    isSwipe = false,
                    isLongPress = true
                )
            }

            if (duration < multiTouchWindowMs && maxFingers == 1) {
                val remainingMs = multiTouchWindowMs - duration
                val extraEvent = try {
                    withTimeout(remainingMs) {
                        awaitPointerEvent(PointerEventPass.Initial)
                    }
                } catch (_: PointerEventTimeoutCancellationException) {
                    null
                }

                if (extraEvent != null) {
                    val newActive = extraEvent.changes.filter { it.pressed }
                    newActive.forEach { seenPointerIds.add(it.id) }
                    if (seenPointerIds.size > maxFingers) {
                        maxFingers = seenPointerIds.size.coerceAtMost(3)
                    }
                    if (newActive.isNotEmpty()) {
                        val newPointer = newActive.firstOrNull()
                        if (newPointer != null) {
                            trackedPointerId = newPointer.id
                            lastPosition = newPointer.position
                        }
                        continue
                    }
                }
            }

            if (isSwipeHandled) {
                listener.onGestureEnd(totalDx, totalDy, maxFingers)
            }
            return TapPressResult(
                fingers = maxFingers,
                startPosition = startPosition,
                durationMs = duration,
                totalDx = totalDx,
                totalDy = totalDy,
                isSwipe = isSwipeHandled,
                isLongPress = isLongPressHandled
            )
        }

        val currentPointer = activePointers.find { it.id == trackedPointerId } ?: activePointers.firstOrNull()
        if (currentPointer != null) {
            val dx: Float
            val dy: Float
            if (currentPointer.id != trackedPointerId) {
                trackedPointerId = currentPointer.id
                lastPosition = currentPointer.position
                dx = 0f
                dy = 0f
            } else {
                dx = currentPointer.position.x - lastPosition.x
                dy = currentPointer.position.y - lastPosition.y
                totalDx += dx
                totalDy += dy
                lastPosition = currentPointer.position
            }

            if (isLongPressHandled) {
                listener.onGesturePointerMove(currentPointer.position)
                event.changes.forEach { it.consume() }
            }

            if (!isSwipeHandled && !isLongPressHandled) {
                val hasMovedPastMin = abs(totalDx) > minTranslationPx || abs(totalDy) > minTranslationPx
                val canCommitSwipe = hasMovedPastMin && !isSystemEdgeDrag

                if (canCommitSwipe) {
                    swipedTrigger = determineSwipeTrigger(maxFingers, totalDx, totalDy)
                    if (swipedTrigger != null) {
                        val handled = listener.onGestureTriggered(swipedTrigger, isLongPress = false, touchOffset = startPosition)
                        if (handled) {
                            isSwipeHandled = true
                            event.changes.forEach { it.consume() }
                        } else {
                            // Gesture was NOT handled (e.g., overlay is open and gesture is NOT a reverse close action).
                            // Return early without consuming event so it passes through to menu layer!
                            return TapPressResult(
                                fingers = maxFingers,
                                startPosition = startPosition,
                                durationMs = duration,
                                totalDx = totalDx,
                                totalDy = totalDy,
                                isSwipe = false,
                                isLongPress = false
                            )
                        }
                    }
                }
            }

            if (isSwipeHandled) {
                event.changes.forEach { it.consume() }
                if (swipedTrigger != null) {
                    val isVerticalSwipe = abs(totalDy) > abs(totalDx)
                    val delta = if (!isVerticalSwipe) dx else -dy
                    listener.onContinuousGesture(swipedTrigger, delta, dx, dy, totalDx, totalDy)
                }
            }
        }

        if (!isSwipeHandled && !isLongPressHandled && duration >= longPressThresholdMs) {
            val totalDist = hypot(totalDx, totalDy)
            if (totalDist < longPressSlopPx) {
                val trigger = if (maxFingers == 1) {
                    detectCornerRegion(startPosition, size.width.toFloat(), size.height.toFloat(), numEdgeRegions, regionBounds) ?: GestureTrigger.LONG_PRESS_1
                } else {
                    when (maxFingers) {
                        2 -> GestureTrigger.LONG_PRESS_2
                        3 -> GestureTrigger.LONG_PRESS_3
                        else -> GestureTrigger.LONG_PRESS_1
                    }
                }
                val handled = listener.onGestureTriggered(trigger, isLongPress = true, touchOffset = startPosition)
                if (handled) {
                    isLongPressHandled = true
                    event.changes.forEach { it.consume() }
                    return TapPressResult(
                        fingers = maxFingers,
                        startPosition = startPosition,
                        durationMs = duration,
                        totalDx = totalDx,
                        totalDy = totalDy,
                        isSwipe = false,
                        isLongPress = true
                    )
                }
            }
        }
    }
}

private fun getActionForTrigger(trigger: GestureTrigger, gestureBindings: Map<GestureTrigger, GestureBinding>?): GestureAction {
    if (gestureBindings == null) return GestureAction.fromKey(trigger.defaultActionKey)
    val binding = gestureBindings[trigger] ?: GestureBinding(
        trigger = trigger,
        action = GestureAction.fromKey(trigger.defaultActionKey),
        isContinuous = trigger.isContinuousDefault
    )
    return binding.action
}

private fun hasDoubleTap(fingers: Int, gestureBindings: Map<GestureTrigger, GestureBinding>?): Boolean {
    val trigger = when (fingers) {
        1 -> GestureTrigger.TAP_1_2
        2 -> GestureTrigger.TAP_2_2
        3 -> GestureTrigger.TAP_3_2
        else -> GestureTrigger.TAP_1_2
    }
    val action = getActionForTrigger(trigger, gestureBindings)
    return action != GestureAction.UNASSIGNED
}

private fun hasTripleTap(fingers: Int, gestureBindings: Map<GestureTrigger, GestureBinding>?): Boolean {
    val trigger = when (fingers) {
        1 -> GestureTrigger.TAP_1_3
        2 -> GestureTrigger.TAP_2_3
        3 -> GestureTrigger.TAP_3_3
        else -> GestureTrigger.TAP_1_3
    }
    val action = getActionForTrigger(trigger, gestureBindings)
    return action != GestureAction.UNASSIGNED
}

private fun emitSingleTap(
    fingers: Int,
    startPosition: Offset,
    width: Float,
    height: Float,
    listener: GestureEventListener,
    numEdgeRegions: Int = 3,
    regionBounds: Rect = Rect(0f, 0f, 1f, 1f)
): Boolean {
    if (fingers == 1) {
        val cornerTrigger = detectCornerRegion(startPosition, width, height, numEdgeRegions, regionBounds)
        if (cornerTrigger != null) {
            return listener.onGestureTriggered(cornerTrigger, touchOffset = startPosition)
        }
    }
    val trigger = when (fingers) {
        1 -> GestureTrigger.TAP_1_1
        2 -> GestureTrigger.TAP_2_1
        3 -> GestureTrigger.TAP_3_1
        else -> GestureTrigger.TAP_1_1
    }
    return listener.onGestureTriggered(trigger, touchOffset = startPosition)
}

private fun emitDoubleTap(
    fingers: Int,
    listener: GestureEventListener
): Boolean {
    val trigger = when (fingers) {
        1 -> GestureTrigger.TAP_1_2
        2 -> GestureTrigger.TAP_2_2
        3 -> GestureTrigger.TAP_3_2
        else -> GestureTrigger.TAP_1_2
    }
    return listener.onGestureTriggered(trigger)
}

private fun emitTripleTap(
    fingers: Int,
    listener: GestureEventListener
): Boolean {
    val trigger = when (fingers) {
        1 -> GestureTrigger.TAP_1_3
        2 -> GestureTrigger.TAP_2_3
        3 -> GestureTrigger.TAP_3_3
        else -> GestureTrigger.TAP_1_3
    }
    return listener.onGestureTriggered(trigger)
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

private fun detectCornerRegion(
    pos: Offset,
    width: Float,
    height: Float,
    numEdgeRegions: Int = 3,
    regionBounds: Rect = Rect(0f, 0f, 1f, 1f)
): GestureTrigger? {
    if (width <= 0 || height <= 0) return null

    val normX = pos.x / width
    val normY = pos.y / height

    if (normX < regionBounds.left || normX > regionBounds.right ||
        normY < regionBounds.top || normY > regionBounds.bottom) {
        return null
    }

    val containerWidthNorm = regionBounds.width
    val containerHeightNorm = regionBounds.height
    if (containerWidthNorm <= 0f || containerHeightNorm <= 0f) return null

    val relX = (normX - regionBounds.left) / containerWidthNorm
    val relY = (normY - regionBounds.top) / containerHeightNorm

    val isTop = relY < 0.22f
    val isBottom = relY > 0.78f

    if (!isTop && !isBottom) return null

    val n = numEdgeRegions.coerceIn(1, 7)
    val regionIndex = (relX * n).toInt().coerceIn(0, n - 1)
    val activeSlots = GestureTrigger.getActiveRegionSlots(n)
    val slotIndex = activeSlots[regionIndex]

    return if (isTop) {
        GestureTrigger.TOP_REGION_SLOTS[slotIndex]
    } else {
        GestureTrigger.BOTTOM_REGION_SLOTS[slotIndex]
    }
}
