package com.exemple.blockingapps.utils

import android.content.Context
import com.exemple.blockingapps.model.GroupRuleDTO
import com.exemple.blockingapps.model.BlockRule
import java.util.Calendar
import java.util.Locale

object BlockManager {
    private const val PREF_NAME = "blocked_apps_pref"
    private const val KEY_BLOCKED_PACKAGES = "blocked_packages"

    // Separator character to join package name and time
    private const val SEPARATOR = "|"

    /**
     * Update: Save PackageName AND Time info (Start|End)
     * Format saved in Prefs: "com.facebook.katana|22:00|07:00"
     */
    fun saveBlockedPackages(context: Context, rules: List<GroupRuleDTO>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        // Filter only blocked apps, then map to a string with time info
        val blockedList = rules.filter { it.isBlocked }.map { rule ->
            val start = rule.startTime ?: "00:00"
            val end = rule.endTime ?: "23:59"
            "${rule.packageName}$SEPARATOR$start$SEPARATOR$end"
        }.toSet()

        prefs.edit().putStringSet(KEY_BLOCKED_PACKAGES, blockedList).apply()

        android.util.Log.d("BlockManager", "Saved rules: $blockedList")
    }
    fun saveRulesFromUI(context: Context, rules: List<BlockRule>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val blockedList = rules.filter { it.isBlocked }.map { rule ->
            val start = rule.startTime ?: "00:00"
            val end = rule.endTime ?: "23:59"
            "${rule.packageName}|$start|$end"
        }.toSet()

        prefs.edit().putStringSet(KEY_BLOCKED_PACKAGES, blockedList).apply()

        android.util.Log.d("BlockManager", "UI Updated rules: $blockedList")
    }

    /**
     * Update: Parse the saved string and check Time Range
     */
    fun isAppBlocked(context: Context, packageName: String): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val blockedList = prefs.getStringSet(KEY_BLOCKED_PACKAGES, emptySet()) ?: emptySet()

        // Iterate through the saved rules to find the matching package
        for (entry in blockedList) {
            // entry format: "pkgName|startTime|endTime"
            val parts = entry.split(SEPARATOR)
            if (parts.size == 3) {
                val savedPkg = parts[0]
                val startTime = parts[1]
                val endTime = parts[2]

                // If package matches, check the time
                if (savedPkg == packageName) {
                    val shouldBlock = isCurrentTimeInBlockRange(startTime, endTime)

                    android.util.Log.d("BlockManager", "Checking $packageName. Time: $startTime-$endTime. Result: $shouldBlock")
                    return shouldBlock
                }
            }
        }

        // If not found in the list, allow it
        return false
    }

    fun updateRules(context: Context, rules: List<GroupRuleDTO>) {
        saveBlockedPackages(context, rules)
    }

    // Your function (kept mostly the same)
    fun isCurrentTimeInBlockRange(startTime: String?, endTime: String?): Boolean {
        if (startTime.isNullOrEmpty() || endTime.isNullOrEmpty()) return true

        val current = Calendar.getInstance()
        val currentHour = current.get(Calendar.HOUR_OF_DAY)
        val currentMinute = current.get(Calendar.MINUTE)

        // Format current time to "HH:mm"
        val currentTimeString = String.format(Locale.getDefault(), "%02d:%02d", currentHour, currentMinute)

        // Check if logic crosses midnight (e.g., 23:00 to 06:00)
        // Case 1: Standard (08:00 to 17:00) -> Start <= Current <= End
        // Case 2: Cross Midnight (22:00 to 06:00) -> Current >= Start OR Current <= End
        if (startTime <= endTime) {
            return currentTimeString >= startTime && currentTimeString <= endTime
        } else {
            // Example: Start 22:00, End 06:00. Current 23:30 (True), Current 05:00 (True), Current 10:00 (False)
            return currentTimeString >= startTime || currentTimeString <= endTime
        }
    }
}