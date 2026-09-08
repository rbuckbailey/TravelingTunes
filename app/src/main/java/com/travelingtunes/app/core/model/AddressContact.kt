package com.travelingtunes.app.core.model

data class AddressContact(
    val id: String,
    val name: String,
    val street: String,
    val city: String = "",
    val state: String = "",
    val fullAddress: String = run {
        val parts = listOf(street, city, state).filter { it.isNotBlank() }
        parts.joinToString(", ")
    }
)
