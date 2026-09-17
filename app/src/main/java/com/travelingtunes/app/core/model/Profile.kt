package com.travelingtunes.app.core.model

import org.json.JSONArray
import org.json.JSONObject

data class Profile(
    val id: String,
    val name: String,
    val isBuiltIn: Boolean = false,
    val isDeletable: Boolean = true,
    val overrides: Map<String, String> = emptyMap()
) {
    fun toJson(): String {
        val overridesEntries = overrides.entries.joinToString(",") { (k, v) ->
            "\"${escapeJson(k)}\": \"${escapeJson(v)}\""
        }
        return "{\"id\": \"${escapeJson(id)}\", \"name\": \"${escapeJson(name)}\", \"isBuiltIn\": $isBuiltIn, \"isDeletable\": $isDeletable, \"overrides\": {$overridesEntries}}"
    }

    companion object {
        const val DEFAULT_ID = "default"
        const val TRAVELING_ID = "traveling"

        val DEFAULT = Profile(
            id = DEFAULT_ID,
            name = "Default",
            isBuiltIn = true,
            isDeletable = false,
            overrides = emptyMap()
        )

        val TRAVELING = Profile(
            id = TRAVELING_ID,
            name = "Traveling",
            isBuiltIn = true,
            isDeletable = false,
            overrides = mapOf(
                "AUTO_autoEnableDrivingMode" to "true",
                "autoEnableDrivingMode" to "true",
                "LIBRARY_gpsVolume" to "true",
                "gpsVolume" to "true",
                "autoSpeedVolumeEnabled" to "true"
            )
        )

        private fun escapeJson(s: String): String {
            return s.replace("\\", "\\\\").replace("\"", "\\\"")
        }

        fun fromJson(json: JSONObject): Profile {
            val id = runCatching { json.optString("id", DEFAULT_ID) }.getOrDefault(DEFAULT_ID).ifEmpty { DEFAULT_ID }
            val name = runCatching { json.optString("name", "Default") }.getOrDefault("Default").ifEmpty { "Default" }
            val isBuiltIn = runCatching { json.optBoolean("isBuiltIn", false) }.getOrDefault(false)
            val isDeletable = runCatching { json.optBoolean("isDeletable", true) }.getOrDefault(true)
            val overridesMap = mutableMapOf<String, String>()
            runCatching {
                val overridesObj = json.optJSONObject("overrides")
                overridesObj?.keys()?.forEach { k ->
                    overridesMap[k] = overridesObj.optString(k)
                }
            }
            return Profile(
                id = id,
                name = name,
                isBuiltIn = isBuiltIn || id == DEFAULT_ID || id == TRAVELING_ID,
                isDeletable = if (id == DEFAULT_ID || id == TRAVELING_ID) false else isDeletable,
                overrides = overridesMap
            )
        }

        fun listToJson(profiles: List<Profile>): String {
            return "[" + profiles.joinToString(",") { it.toJson() } + "]"
        }

        fun listFromJson(jsonStr: String?): List<Profile> {
            if (jsonStr.isNullOrEmpty()) {
                return listOf(DEFAULT, TRAVELING)
            }
            val fromOrgJson = runCatching {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<Profile>()
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val id = item.optString("id", DEFAULT_ID).ifEmpty { DEFAULT_ID }
                    val name = item.optString("name", "Default").ifEmpty { "Default" }
                    val isBuiltIn = item.optBoolean("isBuiltIn", false)
                    val isDeletable = item.optBoolean("isDeletable", true)
                    val overridesMap = mutableMapOf<String, String>()
                    val overridesObj = item.optJSONObject("overrides")
                    overridesObj?.keys()?.forEach { k ->
                        overridesMap[k] = overridesObj.optString(k)
                    }
                    list.add(Profile(id, name, isBuiltIn || id == DEFAULT_ID || id == TRAVELING_ID, if (id == DEFAULT_ID || id == TRAVELING_ID) false else isDeletable, overridesMap))
                }
                list.toList()
            }.getOrNull()

            val rawList = if (!fromOrgJson.isNullOrEmpty()) {
                fromOrgJson
            } else {
                val profilesList = mutableListOf<Profile>()
                val objectRegex = Regex("""\{[^{}]*("overrides"\s*:\s*\{[^{}]*\})?[^{}]*\}""")
                val matches = objectRegex.findAll(jsonStr).toList()
                for (match in matches) {
                    val objStr = match.value
                    val idMatch = Regex(""""id"\s*:\s*"([^"]+)"""").find(objStr)
                    val nameMatch = Regex(""""name"\s*:\s*"([^"]+)"""").find(objStr)
                    val isBuiltInMatch = Regex(""""isBuiltIn"\s*:\s*(true|false)""").find(objStr)
                    val isDeletableMatch = Regex(""""isDeletable"\s*:\s*(true|false)""").find(objStr)

                    val id = idMatch?.groupValues?.get(1) ?: continue
                    val name = nameMatch?.groupValues?.get(1) ?: id
                    val isBuiltIn = isBuiltInMatch?.groupValues?.get(1)?.toBoolean() ?: false
                    val isDeletable = isDeletableMatch?.groupValues?.get(1)?.toBoolean() ?: true

                    val overridesMap = mutableMapOf<String, String>()
                    val overridesSub = Regex(""""overrides"\s*:\s*\{([^}]+)\}""").find(objStr)?.groupValues?.get(1)
                    if (overridesSub != null) {
                        val pairRegex = Regex(""""([^"]+)"\s*:\s*"([^"]+)"""")
                        pairRegex.findAll(overridesSub).forEach { p ->
                            overridesMap[p.groupValues[1]] = p.groupValues[2]
                        }
                    }
                    profilesList.add(Profile(id, name, isBuiltIn || id == DEFAULT_ID || id == TRAVELING_ID, if (id == DEFAULT_ID || id == TRAVELING_ID) false else isDeletable, overridesMap))
                }
                profilesList
            }

            var resultList = rawList
            if (resultList.none { it.id == DEFAULT_ID }) {
                resultList = listOf(DEFAULT) + resultList
            }
            if (resultList.none { it.id == TRAVELING_ID }) {
                val defaultIdx = resultList.indexOfFirst { it.id == DEFAULT_ID }
                resultList = resultList.toMutableList().apply {
                    add(if (defaultIdx >= 0) defaultIdx + 1 else 0, TRAVELING)
                }
            }
            return resultList
        }
    }
}

enum class ProfileSelectionMode(val displayName: String) {
    MENU("Present Profile Menu"),
    SEQUENTIAL("Switch Selected Profiles");

    companion object {
        fun fromKey(key: String?): ProfileSelectionMode {
            return entries.find { it.name.equals(key, ignoreCase = true) } ?: MENU
        }
    }
}
