package com.travelingtunes.app

import com.travelingtunes.app.core.model.GestureCategory
import com.travelingtunes.app.core.model.GestureTrigger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TravelingTunesGestureDetectorTest {

    @Test
    fun testTwoFingerSwipeTriggers() {
        val swipe2Left = GestureTrigger.fromKey("2SwipeLeft")
        assertNotNull(swipe2Left)
        assertEquals(GestureTrigger.SWIPE_2_LEFT, swipe2Left)
        assertEquals(GestureCategory.TWO_FINGER_SWIPE, swipe2Left?.category)

        val swipe2Right = GestureTrigger.fromKey("2SwipeRight")
        assertNotNull(swipe2Right)
        assertEquals(GestureTrigger.SWIPE_2_RIGHT, swipe2Right)
        assertEquals(GestureCategory.TWO_FINGER_SWIPE, swipe2Right?.category)

        val swipe2Up = GestureTrigger.fromKey("2SwipeUp")
        assertNotNull(swipe2Up)
        assertEquals(GestureTrigger.SWIPE_2_UP, swipe2Up)
        assertEquals(GestureCategory.TWO_FINGER_SWIPE, swipe2Up?.category)

        val swipe2Down = GestureTrigger.fromKey("2SwipeDown")
        assertNotNull(swipe2Down)
        assertEquals(GestureTrigger.SWIPE_2_DOWN, swipe2Down)
        assertEquals(GestureCategory.TWO_FINGER_SWIPE, swipe2Down?.category)
    }

    @Test
    fun testTwoFingerTapTriggers() {
        val tap21 = GestureTrigger.fromKey("21Tap")
        assertNotNull(tap21)
        assertEquals(GestureTrigger.TAP_2_1, tap21)
        assertEquals(GestureCategory.TWO_FINGER_TAP, tap21?.category)

        val tap22 = GestureTrigger.fromKey("22Tap")
        assertNotNull(tap22)
        assertEquals(GestureTrigger.TAP_2_2, tap22)
        assertEquals(GestureCategory.TWO_FINGER_TAP, tap22?.category)

        val tap23 = GestureTrigger.fromKey("23Tap")
        assertNotNull(tap23)
        assertEquals(GestureTrigger.TAP_2_3, tap23)
        assertEquals(GestureCategory.TWO_FINGER_TAP, tap23?.category)
    }

    @Test
    fun testTwoFingerLongPressTrigger() {
        val longPress2 = GestureTrigger.fromKey("2LongPress")
        assertNotNull(longPress2)
        assertEquals(GestureTrigger.LONG_PRESS_2, longPress2)
        assertEquals(GestureCategory.LONG_PRESS, longPress2?.category)
    }
}
