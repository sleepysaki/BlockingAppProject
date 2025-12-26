package com.exemple.blockingapps.utils

import android.content.Context
import com.exemple.blockingapps.model.GroupRuleDTO
import com.exemple.blockingapps.model.BlockRule
import java.util.Calendar
import java.util.Locale

object BlockManager {
    private const val PREF_NAME = "blocked_apps_pref"
    private const val KEY_BLOCKED_PACKAGES = "blocked_packages"
    private const val SEPARATOR = "|"

    /**
     * Clear all saved rules from SharedPreferences.
     * Use this during Login or Logout to ensure a clean state.
     */
    fun clearAllRules(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_BLOCKED_PACKAGES).apply()
        android.util.Log.d("BlockManager", "All local rules cleared.")
    }

    /**
     * Updated: Sync rules from Server (GroupRuleDTO).
     * Only save if startTime and endTime are NOT null.
     */
    fun saveBlockedPackages(context: Context, rules: List<GroupRuleDTO>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        // Filter: only apps that are blocked AND have valid time settings
        val blockedList = rules.filter { it.isBlocked && it.startTime != null && it.endTime != null }
            .map { rule ->
                "${rule.packageName}$SEPARATOR${rule.startTime}$SEPARATOR${rule.endTime}"
            }.toSet()

        prefs.edit().putStringSet(KEY_BLOCKED_PACKAGES, blockedList).apply()
        android.util.Log.d("BlockManager", "Rules updated from Server: $blockedList")
    }

    /**
     * Updated: Save rules from local UI (BlockRule).
     */
    fun saveRulesFromUI(context: Context, rules: List<BlockRule>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        // Only save if time is provided to avoid accidental 24h blocking
        val blockedList = rules.filter { it.isBlocked && !it.startTime.isNullOrEmpty() && !it.endTime.isNullOrEmpty() }
            .map { rule ->
                "${rule.packageName}$SEPARATOR${rule.startTime}$SEPARATOR${rule.endTime}"
            }.toSet()

        prefs.edit().putStringSet(KEY_BLOCKED_PACKAGES, blockedList).apply()
        android.util.Log.d("BlockManager", "Rules updated from UI: $blockedList")
    }

    fun isAppBlocked(context: Context, packageName: String): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val blockedList = prefs.getStringSet(KEY_BLOCKED_PACKAGES, emptySet()) ?: emptySet()

        if (blockedList.isEmpty()) return false

        for (entry in blockedList) {
            val parts = entry.split(SEPARATOR)
            if (parts.size == 3) {
                val savedPkg = parts[0]
                val startTime = parts[1]
                val endTime = parts[2]

                if (savedPkg == packageName) {
                    val shouldBlock = isCurrentTimeInBlockRange(startTime, endTime)
                    android.util.Log.d("BlockManager", "Check: $packageName | Range: $startTime-$endTime | Result: $shouldBlock")
                    return shouldBlock
                }
            }
        }
        return false
    }

    fun updateRules(context: Context, rules: List<GroupRuleDTO>) {
        saveBlockedPackages(context, rules)
    }

    fun isCurrentTimeInBlockRange(startTime: String?, endTime: String?): Boolean {
        // Strict check: No time = No blocking
        if (startTime.isNullOrEmpty() || endTime.isNullOrEmpty() || startTime == "null") {
            return false
        }

        val current = Calendar.getInstance()
        val currentHour = current.get(Calendar.HOUR_OF_DAY)
        val currentMinute = current.get(Calendar.MINUTE)
        val currentTimeString = String.format(Locale.getDefault(), "%02d:%02d", currentHour, currentMinute)

        return try {
            if (startTime <= endTime) {
                currentTimeString >= startTime && currentTimeString <= endTime
            } else {
                // Support for overnight blocking (e.g., 22:00 - 06:00)
                currentTimeString >= startTime || currentTimeString <= endTime
            }
        } catch (e: Exception) {
            android.util.Log.e("BlockManager", "Error comparing time: ${e.message}")
            false
        }
    }
}