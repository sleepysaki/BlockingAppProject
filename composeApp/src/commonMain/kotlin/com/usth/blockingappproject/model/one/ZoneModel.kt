package com.usth.blockingappproject.model.zone

import kotlinx.serialization.Serializable

@Serializable
enum class ZoneType { SCHOOL, HOME, LIBRARY, OTHER }

@Serializable
data class ZoneDto(
    val id: Int,
    val name: String,
    val type: ZoneType,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double
)

@Serializable
data class CreateZoneRequest(
    val name: String,
    val type: ZoneType,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double
)