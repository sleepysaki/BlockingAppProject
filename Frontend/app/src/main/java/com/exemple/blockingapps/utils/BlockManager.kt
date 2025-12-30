package com.exemple.blockingapps.utils

import android.content.Context
import android.util.Log
import com.exemple.blockingapps.model.GroupRuleDTO
import com.exemple.blockingapps.model.BlockRule
import java.util.Calendar
import java.util.Locale

object BlockManager {
    private const val PREF_NAME = "blocked_apps_pref"

    // Các Key lưu trữ
    private const val KEY_TIME_BLOCKED = "blocked_packages"       // Chặn theo giờ
    private const val KEY_GEO_BLOCKED = "geo_blocked_packages"    // Chặn theo vị trí
    private const val KEY_ALWAYS_BLOCKED = "always_blocked_packages" // Chặn vĩnh viễn (Manual Block từ Group)

    private const val SEPARATOR = "|"

    // --- SAVE LOGIC ---

    fun saveBlockedPackages(context: Context, rules: List<GroupRuleDTO>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        // 1. Lọc Time Rules (Có giờ giấc)
        val timeList = rules.filter {
            it.isBlocked && !it.startTime.isNullOrEmpty() && !it.endTime.isNullOrEmpty()
        }.map { "${it.packageName}$SEPARATOR${it.startTime}$SEPARATOR${it.endTime}" }
            .toSet()

        // 2. Lọc Geo Rules (Có bán kính > 0)
        val geoList = rules.filter {
            it.isBlocked && (it.radius ?: 0.0) > 0.0
        }.map { it.packageName }
            .toSet()

        // 3. Lọc Always Block (Chặn thủ công)
        val alwaysList = rules.filter {
            it.isBlocked &&
                    (it.startTime.isNullOrEmpty() || it.endTime.isNullOrEmpty()) &&
                    ((it.radius ?: 0.0) == 0.0)
        }.map { it.packageName }.toSet()

        // Lưu tất cả
        editor.putStringSet(KEY_TIME_BLOCKED, timeList)
        editor.putStringSet(KEY_GEO_BLOCKED, geoList)
        editor.putStringSet(KEY_ALWAYS_BLOCKED, alwaysList)

        editor.apply()

        Log.d("BlockManager", "SAVED -> Time: ${timeList.size} | Geo: ${geoList.size} | Always: ${alwaysList.size}")
    }

    // 👇 ĐÃ THÊM LẠI HÀM NÀY ĐỂ FIX LỖI "Unresolved reference" TRONG HomeViewModel
    fun updateRules(context: Context, rules: List<GroupRuleDTO>) {
        saveBlockedPackages(context, rules)
    }

    // --- CHECK LOGIC ---

    fun isAppBlocked(context: Context, packageName: String, isInsideZone: Boolean): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        // 1. Ưu tiên 1: Geo Blocking
        if (isInsideZone) {
            val geoList = prefs.getStringSet(KEY_GEO_BLOCKED, emptySet()) ?: emptySet()
            if (geoList.contains(packageName)) {
                Log.d("BlockManager", "Blocking $packageName due to Location Zone")
                return true
            }
        }

        // 2. Ưu tiên 2: Always Blocking
        val alwaysList = prefs.getStringSet(KEY_ALWAYS_BLOCKED, emptySet()) ?: emptySet()
        if (alwaysList.contains(packageName)) {
            Log.d("BlockManager", "Blocking $packageName due to Manual/Always Block")
            return true
        }

        // 3. Ưu tiên 3: Time Blocking
        val timeList = prefs.getStringSet(KEY_TIME_BLOCKED, emptySet()) ?: emptySet()
        for (entry in timeList) {
            val parts = entry.split(SEPARATOR)
            if (parts.size == 3) {
                val savedPkg = parts[0]
                val startTime = parts[1]
                val endTime = parts[2]

                if (savedPkg == packageName) {
                    if (isCurrentTimeInBlockRange(startTime, endTime)) {
                        Log.d("BlockManager", "Blocking $packageName due to Time Schedule")
                        return true
                    }
                }
            }
        }

        return false
    }

    // --- SUPPORT UTILS ---

    fun saveRulesFromUI(context: Context, rules: List<BlockRule>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val blockedList = rules.filter { it.isBlocked && !it.startTime.isNullOrEmpty() && !it.endTime.isNullOrEmpty() }
            .map { "${it.packageName}$SEPARATOR${it.startTime}$SEPARATOR${it.endTime}" }
            .toSet()
        prefs.edit().putStringSet(KEY_TIME_BLOCKED, blockedList).apply()
    }

    fun isCurrentTimeInBlockRange(startTime: String?, endTime: String?): Boolean {
        if (startTime.isNullOrEmpty() || endTime.isNullOrEmpty() || startTime == "null") return false
        val current = Calendar.getInstance()
        val currentTimeString = String.format(Locale.getDefault(), "%02d:%02d", current.get(Calendar.HOUR_OF_DAY), current.get(Calendar.MINUTE))
        return try {
            if (startTime <= endTime) currentTimeString in startTime..endTime
            else currentTimeString >= startTime || currentTimeString <= endTime
        } catch (e: Exception) { false }
    }
}