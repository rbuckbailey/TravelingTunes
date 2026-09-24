package com.travelingtunes.app

import com.travelingtunes.app.core.model.ConnectedDevice
import com.travelingtunes.app.core.model.ConnectedDeviceType
import com.travelingtunes.app.core.model.GestureAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectedDeviceTest {

    @Test
    fun testConnectedDeviceJsonSerialization() {
        val device = ConnectedDevice(
            id = "bt_12345",
            name = "Pixel Buds Pro",
            type = ConnectedDeviceType.BLUETOOTH,
            lastConnectedMs = 1700000000000L,
            actions = listOf(
                GestureAction.PLAY_PAUSE,
                GestureAction.SHUFFLE_ALL_SONGS,
                GestureAction.VOLUME_UP
            )
        )

        val jsonStr = device.toJsonString()
        assertTrue(jsonStr.contains("bt_12345"))
        assertTrue(jsonStr.contains("Pixel Buds Pro"))
        assertTrue(jsonStr.contains("BLUETOOTH"))

        val restored = ConnectedDevice.parseFromString(jsonStr)
        assertNotNull(restored)
        assertEquals("bt_12345", restored!!.id)
        assertEquals("Pixel Buds Pro", restored.name)
        assertEquals(ConnectedDeviceType.BLUETOOTH, restored.type)
        assertEquals(3, restored.actions.size)
        assertEquals(GestureAction.PLAY_PAUSE, restored.actions[0])
        assertEquals(GestureAction.SHUFFLE_ALL_SONGS, restored.actions[1])
        assertEquals(GestureAction.VOLUME_UP, restored.actions[2])
    }

    @Test
    fun testDefaultConnectedDeviceWithNoActions() {
        val device = ConnectedDevice(
            id = "wired_headphones",
            name = "Wired Headphones",
            type = ConnectedDeviceType.WIRED_HEADPHONES
        )

        assertTrue(device.actions.isEmpty())

        val jsonStr = device.toJsonString()
        val restored = ConnectedDevice.parseFromString(jsonStr)
        assertNotNull(restored)
        assertTrue(restored!!.actions.isEmpty())
        assertEquals(ConnectedDeviceType.WIRED_HEADPHONES, restored.type)
    }

    @Test
    fun testInvalidJsonHandling() {
        val invalidJsonStr = """{"id":"", "name":""}"""
        val restored = ConnectedDevice.parseFromString(invalidJsonStr)
        assertTrue(restored == null)
    }
}
