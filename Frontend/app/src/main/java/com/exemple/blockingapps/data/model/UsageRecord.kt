package com.exemple.blockingapps.data.model

data class UsageRecord(
    val appId: String,
    val appName: String,
    val totalMinutes: Int,
    val accessCount: Int,
    val category: String
)

data class DailyUsage(
    val date: String,
    val totalScreenTimeMinutes: Int,
    val records: List<UsageRecord>
)

data class UsageCategory(
    val categoryName: String,
    val totalMinutes: Int
)

data class DailyUsageSummary(
    val date: String,
    val totalScreenTimeMinutes: Int,
    val comparisonPercent: String,
    val categories: List<UsageCategory>,
    val topApps: List<UsageRecord>,
    val hourlyUsage: List<Int>
)