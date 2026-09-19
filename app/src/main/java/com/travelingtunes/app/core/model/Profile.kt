package com.travelingtunes.app.core.model

import org.json.JSONArray
import org.json.JSONObject

data class Profile(
    val id: String,
    val name: String,
    val isBuiltIn: Boolean = false,
    val isDeletable: Boolean = true,
    val overrides: Map<String, String> = emptyMap(),
    val parentId: String? = null,
    val emoji: String = "🏷️"
) {
    fun toJson(): String {
        val overridesEntries = overrides.entries.joinToString(",") { (k, v) ->
            "\"${escapeJson(k)}\": \"${escapeJson(v)}\""
        }
        val parentField = if (parentId != null) ", \"parentId\": \"${escapeJson(parentId)}\"" else ""
        return "{\"id\": \"${escapeJson(id)}\", \"name\": \"${escapeJson(name)}\", \"emoji\": \"${escapeJson(emoji)}\", \"isBuiltIn\": $isBuiltIn, \"isDeletable\": $isDeletable$parentField, \"overrides\": {$overridesEntries}}"
    }

    fun getEffectiveOverride(key: String, allProfiles: List<Profile>): String? {
        var current: Profile? = this
        val visited = mutableSetOf<String>()
        while (current != null) {
            if (!visited.add(current.id)) break
            if (current.overrides.containsKey(key)) {
                return current.overrides[key]
            }
            val nextParentId = current.parentId ?: if (current.id != DEFAULT_ID) DEFAULT_ID else null
            current = if (nextParentId != null) allProfiles.find { it.id == nextParentId } else null
        }
        return null
    }

    fun getAncestorChain(allProfiles: List<Profile>): List<Profile> {
        val chain = mutableListOf<Profile>()
        var current: Profile? = this
        val visited = mutableSetOf<String>()
        while (current != null) {
            if (!visited.add(current.id)) break
            chain.add(current)
            val nextParentId = current.parentId ?: if (current.id != DEFAULT_ID) DEFAULT_ID else null
            current = if (nextParentId != null) allProfiles.find { it.id == nextParentId } else null
        }
        return chain
    }

    companion object {
        const val DEFAULT_ID = "default"
        const val TRAVELING_ID = "traveling"
        const val DRIVING_ID = "driving"
        const val TRANSIT_ID = "transit"
        const val DOCKED_ID = "docked"
        const val UNDOCKED_ID = "undocked"

        private val BUILT_IN_IDS = setOf(DEFAULT_ID, TRAVELING_ID, DRIVING_ID, TRANSIT_ID, DOCKED_ID, UNDOCKED_ID, "dock")

        val DEFAULT = Profile(
            id = DEFAULT_ID,
            name = "Default",
            isBuiltIn = true,
            isDeletable = false,
            overrides = emptyMap(),
            parentId = null,
            emoji = "🏷️"
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
                "gpsVolume" to "true"
            ),
            parentId = DEFAULT_ID,
            emoji = "🧳"
        )

        val DRIVING = Profile(
            id = DRIVING_ID,
            name = "Driving",
            isBuiltIn = true,
            isDeletable = false,
            overrides = mapOf(
                "AUTO_drivingMode" to "true",
                "drivingModeEnabled" to "true",
                "AUTO_speedVolume" to "true",
                "autoSpeedVolumeEnabled" to "true"
            ),
            parentId = TRAVELING_ID,
            emoji = "🚗"
        )

        val TRANSIT = Profile(
            id = TRANSIT_ID,
            name = "Transit",
            isBuiltIn = true,
            isDeletable = false,
            overrides = mapOf(
                "AUTO_ambientNoise" to "true",
                "autoAmbientNoiseEnabled" to "true"
            ),
            parentId = TRAVELING_ID,
            emoji = "🚆"
        )

        val DOCKED = Profile(
            id = DOCKED_ID,
            name = "Docked Art",
            isBuiltIn = true,
            isDeletable = false,
            overrides = mapOf(
                "DISPLAY_artDisplayLayout" to "DOCKED",
                "artDisplayLayout" to "DOCKED",
                "ART_LAYOUT_DOCKED" to "true"
            ),
            parentId = DEFAULT_ID,
            emoji = "🖼️"
        )

        val UNDOCKED = Profile(
            id = UNDOCKED_ID,
            name = "Undocked Art",
            isBuiltIn = true,
            isDeletable = false,
            overrides = mapOf(
                "DISPLAY_artDisplayLayout" to "OVERLAY",
                "artDisplayLayout" to "OVERLAY",
                "ART_LAYOUT_OVERLAY" to "true"
            ),
            parentId = DEFAULT_ID,
            emoji = "📱"
        )

        private fun escapeJson(s: String): String {
            return s.replace("\\", "\\\\").replace("\"", "\\\"")
        }

        fun fromJson(json: JSONObject): Profile {
            val id = runCatching { json.optString("id", DEFAULT_ID) }.getOrDefault(DEFAULT_ID).ifEmpty { DEFAULT_ID }
            val name = runCatching { json.optString("name", "Default") }.getOrDefault("Default").ifEmpty { "Default" }
            val emoji = runCatching { json.optString("emoji", "🏷️") }.getOrDefault("🏷️").ifEmpty { "🏷️" }
            val isBuiltIn = runCatching { json.optBoolean("isBuiltIn", false) }.getOrDefault(false)
            val isDeletable = runCatching { json.optBoolean("isDeletable", true) }.getOrDefault(true)
            val parentId = runCatching { json.optString("parentId", "").takeIf { it.isNotEmpty() } }.getOrNull()
            val overridesMap = mutableMapOf<String, String>()
            runCatching {
                val overridesObj = json.optJSONObject("overrides")
                overridesObj?.keys()?.forEach { k ->
                    overridesMap[k] = overridesObj.optString(k)
                }
            }
            val isBuiltInProfile = isBuiltIn || id in BUILT_IN_IDS
            return Profile(
                id = id,
                name = name,
                isBuiltIn = isBuiltInProfile,
                isDeletable = if (isBuiltInProfile) false else isDeletable,
                overrides = overridesMap,
                parentId = if (id == DEFAULT_ID) null else (parentId ?: DEFAULT_ID),
                emoji = emoji
            )
        }

        fun listToJson(profiles: List<Profile>): String {
            return "[" + profiles.joinToString(",") { it.toJson() } + "]"
        }

        fun listFromJson(jsonStr: String?): List<Profile> {
            if (jsonStr.isNullOrEmpty()) {
                return listOf(DEFAULT, TRAVELING, DRIVING, TRANSIT, DOCKED, UNDOCKED)
            }
            val fromOrgJson = runCatching {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<Profile>()
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    list.add(fromJson(item))
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
                    val emojiMatch = Regex(""""emoji"\s*:\s*"([^"]+)"""").find(objStr)
                    val isBuiltInMatch = Regex(""""isBuiltIn"\s*:\s*(true|false)""").find(objStr)
                    val isDeletableMatch = Regex(""""isDeletable"\s*:\s*(true|false)""").find(objStr)
                    val parentIdMatch = Regex(""""parentId"\s*:\s*"([^"]+)"""").find(objStr)

                    val id = idMatch?.groupValues?.get(1) ?: continue
                    val name = nameMatch?.groupValues?.get(1) ?: id
                    val emoji = emojiMatch?.groupValues?.get(1)?.ifEmpty { "🏷️" } ?: "🏷️"
                    val isBuiltIn = isBuiltInMatch?.groupValues?.get(1)?.toBoolean() ?: false
                    val isDeletable = isDeletableMatch?.groupValues?.get(1)?.toBoolean() ?: true
                    val parentId = parentIdMatch?.groupValues?.get(1)

                    val overridesMap = mutableMapOf<String, String>()
                    val overridesSub = Regex(""""overrides"\s*:\s*\{([^}]+)\}""").find(objStr)?.groupValues?.get(1)
                    if (overridesSub != null) {
                        val pairRegex = Regex(""""([^"]+)"\s*:\s*"([^"]+)"""")
                        pairRegex.findAll(overridesSub).forEach { p ->
                            overridesMap[p.groupValues[1]] = p.groupValues[2]
                        }
                    }
                    val isBuiltInProfile = isBuiltIn || id in BUILT_IN_IDS
                    profilesList.add(Profile(id, name, isBuiltInProfile, if (isBuiltInProfile) false else isDeletable, overridesMap, if (id == DEFAULT_ID) null else (parentId ?: DEFAULT_ID), emoji))
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
            if (resultList.none { it.id == DRIVING_ID }) {
                val travIdx = resultList.indexOfFirst { it.id == TRAVELING_ID }
                resultList = resultList.toMutableList().apply {
                    add(if (travIdx >= 0) travIdx + 1 else resultList.size, DRIVING)
                }
            }
            if (resultList.none { it.id == TRANSIT_ID }) {
                val drvIdx = resultList.indexOfFirst { it.id == DRIVING_ID }
                resultList = resultList.toMutableList().apply {
                    add(if (drvIdx >= 0) drvIdx + 1 else resultList.size, TRANSIT)
                }
            }
            if (resultList.none { it.id == DOCKED_ID }) {
                val trsIdx = resultList.indexOfFirst { it.id == TRANSIT_ID }
                resultList = resultList.toMutableList().apply {
                    add(if (trsIdx >= 0) trsIdx + 1 else resultList.size, DOCKED)
                }
            }
            if (resultList.none { it.id == UNDOCKED_ID }) {
                val dockIdx = resultList.indexOfFirst { it.id == DOCKED_ID }
                resultList = resultList.toMutableList().apply {
                    add(if (dockIdx >= 0) dockIdx + 1 else resultList.size, UNDOCKED)
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
