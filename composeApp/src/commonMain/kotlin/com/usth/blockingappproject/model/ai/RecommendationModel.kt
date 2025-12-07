package com.usth.blockingappproject.model.ai

import kotlinx.serialization.Serializable

@Serializable
data class UsagePatternDto(
    val appCategory: String, // "Social", "Game"
    val averageDailyUsageMinutes: Int,
    val peakUsageHour: Int // 0-23
)

@Serializable
data class LimitRecommendationDto(
    val profileId: Int,
    val appPackageName: String,
    val suggestedLimitMinutes: Int,
    val reasoning: String // e.g., "Usage is 30% higher than peer average"
)