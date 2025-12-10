package com.usth.blockingappproject.model.zone

import kotlinx.serialization.Serializable

/**
 * Models for geo-fencing and location-based app restrictions.
 * 
 * Zones define geographic areas where specific app policies apply.
 * For example: "Block social media apps when at school"
 */

// =============================================================================
// ENUMS
// =============================================================================

/**
 * Predefined zone types with typical default behaviors.
 */
@Serializable
enum class ZoneType {
    /** School zone - typically blocks entertainment/social apps */
    SCHOOL,
    /** Home zone - may have relaxed rules */
    HOME,
    /** Library/study zone - focus-friendly restrictions */
    LIBRARY,
    /** Work zone - professional environment restrictions */
    WORK,
    /** Custom user-defined zone */
    CUSTOM
}

/**
 * What happens to apps when user enters a zone.
 */
@Serializable
enum class ZonePolicyAction {
    /** Block specified apps in this zone */
    BLOCK,
    /** Only allow specified apps in this zone (block everything else) */
    ALLOW_ONLY,
    /** No restrictions, just log presence */
    MONITOR_ONLY
}

// =============================================================================
// ZONE DATA
// =============================================================================

/**
 * Represents a geographic zone with associated restrictions.
 * 
 * @property id Unique zone identifier
 * @property name Human-readable name (e.g., "Lincoln High School")
 * @property type Category of zone
 * @property latitude Center point latitude
 * @property longitude Center point longitude
 * @property radiusMeters Radius of the circular zone in meters
 * @property isActive Whether this zone is currently enforced
 */
@Serializable
data class ZoneDto(
    val id: Int,
    val name: String,
    val type: ZoneType,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
    val isActive: Boolean = true
)

/**
 * Request to create a new geo-zone.
 */
@Serializable
data class CreateZoneRequest(
    val name: String,
    val type: ZoneType,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double
)

/**
 * Request to update an existing zone.
 */
@Serializable
data class UpdateZoneRequest(
    val id: Int,
    val name: String? = null,
    val type: ZoneType? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Double? = null,
    val isActive: Boolean? = null
)

// =============================================================================
// ZONE RULES
// =============================================================================

/**
 * Rule defining what apps are affected when user is in a zone.
 * 
 * @property zoneId Zone this rule applies to
 * @property packageNames List of app package names affected
 * @property action What to do with these apps in this zone
 */
@Serializable
data class ZoneRuleDto(
    val id: Int? = null,
    val zoneId: Int,
    val packageNames: List<String>,
    val action: ZonePolicyAction
)

/**
 * Complete zone configuration including its rules.
 */
@Serializable
data class ZoneWithRulesDto(
    val zone: ZoneDto,
    val rules: List<ZoneRuleDto>
)