package com.travelingtunes.app.core.model

enum class TouchRegionTarget(val displayName: String) {
    BOTH("Both"),
    ART("Art"),
    TITLE("Title")
}

data class GestureBinding(
    val trigger: GestureTrigger,
    val action: GestureAction,
    val isContinuous: Boolean = false,
    val otherOptionKey: String? = null,
    val artAction: GestureAction = GestureAction.UNASSIGNED,
    val artOtherOptionKey: String? = null,
    val titleAction: GestureAction = GestureAction.UNASSIGNED,
    val titleOtherOptionKey: String? = null
)

