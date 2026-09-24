package com.travelingtunes.app.core.model

import org.json.JSONArray
import org.json.JSONObject

enum class ConnectedDeviceType(val displayName: String) {
    BLUETOOTH("Bluetooth Audio"),
    ANDROID_AUTO("Android Auto"),
    WIRED_HEADPHONES("Wired Headphones"),
    CAST("Cast / Remote Audio"),
    OTHER("Audio Device")
}

data class ConnectedDevice(
    val id: String,
    val name: String,
    val type: ConnectedDeviceType,
    val lastConnectedMs: Long = System.currentTimeMillis(),
    val actions: List<GestureAction> = emptyList()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("type", type.name)
            put("lastConnectedMs", lastConnectedMs)
            put("actions", JSONArray().apply {
                actions.forEach { put(it.name) }
            })
        }
    }

    fun toJsonString(): String {
        val actionsJson = actions.joinToString(",") { "\"${it.name}\"" }
        return """{"id":"$id","name":"$name","type":"${type.name}","lastConnectedMs":$lastConnectedMs,"actions":[$actionsJson]}"""
    }

    companion object {
        fun parseFromString(raw: String): ConnectedDevice? {
            val idMatch = """"id"\s*:\s*"([^"]*)"""".toRegex().find(raw)?.groupValues?.get(1) ?: return null
            if (idMatch.isBlank()) return null
            val nameMatch = """"name"\s*:\s*"([^"]*)"""".toRegex().find(raw)?.groupValues?.get(1) ?: "Audio Device"
            val typeMatch = """"type"\s*:\s*"([^"]*)"""".toRegex().find(raw)?.groupValues?.get(1) ?: ConnectedDeviceType.OTHER.name
            val type = runCatching { ConnectedDeviceType.valueOf(typeMatch) }.getOrDefault(ConnectedDeviceType.OTHER)
            val lastConnMatch = """"lastConnectedMs"\s*:\s*(\d+)""".toRegex().find(raw)?.groupValues?.get(1)?.toLongOrNull() ?: System.currentTimeMillis()
            
            val actionsMatch = """"actions"\s*:\s*\[([^\]]*)\]""".toRegex().find(raw)?.groupValues?.get(1) ?: ""
            val actionNames = """"([^"]+)"""".toRegex().findAll(actionsMatch).map { it.groupValues[1] }.toList()
            val actionsList = actionNames.mapNotNull { name ->
                runCatching { GestureAction.valueOf(name) }.getOrNull()
            }.filter { it != GestureAction.UNASSIGNED && it != GestureAction.OTHER_OPTION }

            return ConnectedDevice(
                id = idMatch,
                name = nameMatch,
                type = type,
                lastConnectedMs = lastConnMatch,
                actions = actionsList
            )
        }

        fun fromJson(jsonStr: String): ConnectedDevice? {
            return parseFromString(jsonStr)
        }

        fun fromJson(json: JSONObject): ConnectedDevice? {
            return try {
                val id = json.optString("id")
                if (id.isNullOrBlank()) return parseFromString(json.toString())
                val name = json.optString("name", "Audio Device")
                val typeName = json.optString("type", ConnectedDeviceType.OTHER.name)
                val type = runCatching { ConnectedDeviceType.valueOf(typeName) }.getOrDefault(ConnectedDeviceType.OTHER)
                val lastConnectedMs = json.optLong("lastConnectedMs", System.currentTimeMillis())
                val actionsJson = json.optJSONArray("actions") ?: JSONArray()
                val actionsList = mutableListOf<GestureAction>()
                for (i in 0 until actionsJson.length()) {
                    val actionName = actionsJson.optString(i)
                    val action = runCatching { GestureAction.valueOf(actionName) }.getOrNull()
                    if (action != null && action != GestureAction.UNASSIGNED && action != GestureAction.OTHER_OPTION) {
                        actionsList.add(action)
                    }
                }
                ConnectedDevice(
                    id = id,
                    name = name,
                    type = type,
                    lastConnectedMs = lastConnectedMs,
                    actions = actionsList
                )
            } catch (_: Exception) {
                parseFromString(json.toString())
            }
        }
    }
}
