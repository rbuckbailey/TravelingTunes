package com.travelingtunes.app.core.model

data class GestureBinding(
    val trigger: GestureTrigger,
    val action: GestureAction,
    val isContinuous: Boolean = false,
    val otherOptionKey: String? = null
)
