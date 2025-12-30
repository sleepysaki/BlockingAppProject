package com.exemple.blockingapps.data.repository

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import com.exemple.blockingapps.data.model.DailyUsageSummary
import com.exemple.blockingapps.data.model.UsageCategory
import com.exemple.blockingapps.data.model.UsageRecord
import com.exemple.blockingapps.ui.home.RecommendationItem
import java.util.*

object UsageDataProvider {
    // Trong UsageDataProvider.kt

    fun getRealUsageForLast7Days(context: Context): List<DailyUsageSummary> {
        val usageHistory = mutableListOf<DailyUsageSummary>()

        // Chạy vòng lặp từ 0 (Hôm nay) đến 6 (6 ngày trước)
        for (i in 0..6) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -i)

            // Mốc bắt đầu ngày (00:00:00)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val start = calendar.timeInMillis

            // Mốc kết thúc ngày (23:59:59)
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            val end = calendar.timeInMillis

            // Gọi hàm truy vấn (Tận dụng logic đã viết ở bước trước)
            val summary = queryDeviceStats(context, start, end, i)
            usageHistory.add(summary)
        }
        return usageHistory
    }

    private fun queryDeviceStats(context: Context, start: Long, end: Long, daysAgo: Int): DailyUsageSummary {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val packageManager = context.packageManager
        val stats = usageStatsManager.queryAndAggregateUsageStats(start, end)

        val records = mutableListOf<UsageRecord>()
        var totalMins = 0

        stats.forEach { (pkg, usage) ->
            val mins = (usage.totalTimeInForeground / (1000 * 60)).toInt()
            if (mins > 0) {
                totalMins += mins
                val appName = try {
                    val appInfo = packageManager.getApplicationInfo(pkg, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) { pkg.substringAfterLast('.') }

                records.add(UsageRecord(pkg, appName, mins, 0, "Hệ thống"))
            }
        }

        val sdf = java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault())
        val dateLabel = if (daysAgo == 0) "Hôm nay" else sdf.format(Date(start))

        return DailyUsageSummary(
            date = dateLabel,
            totalScreenTimeMinutes = totalMins,
            comparisonPercent = if (daysAgo == 0) "Dữ liệu thực" else "",
            categories = listOf(UsageCategory("Ứng dụng", totalMins)),
            topApps = records.sortedByDescending { it.totalMinutes }.take(10),
            hourlyUsage = List(24) { if (it in 8..22 && totalMins > 0) totalMins / 12 else 0 }
        )
    }

    fun getRealDataAndAutoRec(context: Context): List<RecommendationItem> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val packageManager = context.packageManager
        val calendar = Calendar.getInstance()

        val endTime = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        val startTime = calendar.timeInMillis

        val stats = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
        val recList = mutableListOf<RecommendationItem>()

        stats.forEach { (pkg, usage) ->
            val minutes = (usage.totalTimeInForeground / (1000 * 60)).toInt()

            if (pkg != context.packageName && minutes > 1) {
                val appName = try {
                    val appInfo = packageManager.getApplicationInfo(pkg, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) { pkg.substringAfterLast('.') }

                recList.add(
                    RecommendationItem(
                        title = "Phát hiện: con đã dùng $appName hơn $minutes phút. Gợi ý đặt giới hạn 15 phút!",
                        appId = pkg,
                        suggestedLimitMinutes = 15
                    )
                )
            }
        }
        return recList
    }
}