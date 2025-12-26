package com.exemple.blockingapps.data.local

import android.content.Context
import com.exemple.blockingapps.ui.home.TimePreset

// Bạn có thể đổi tên object này thành "LocalDataManager" cho chuyên nghiệp hơn
// Nếu đổi tên, nhớ bấm Refactor -> Rename để Android Studio tự sửa các chỗ đang gọi nó
object FakeLocalDatabase {
    private const val PREFS_NAME = "BlockedAppsPrefs"
    private const val KEY_PACKAGES = "blocked_packages"
    private const val PREFS_TIME = "TimePresets"

    // --- XÓA PHẦN USERS MOCK (VÌ ĐÃ CÓ API) ---

    // Giữ lại phần này để lưu danh sách chặn dưới máy (tránh mất khi tắt app)
    fun saveBlockedPackages(context: Context, packages: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_PACKAGES, packages).apply()
    }

    fun loadBlockedPackages(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_PACKAGES, emptySet()) ?: emptySet()
    }

    // Giữ lại phần này để lưu cài đặt thời gian
    fun saveTimePresets(context: Context, presets: List<TimePreset>) {
        val prefs = context.getSharedPreferences(PREFS_TIME, Context.MODE_PRIVATE)
        val data = presets.joinToString(";") { "${it.id}|${it.label}|${it.startTime}|${it.endTime}" }
        prefs.edit().putString("presets_data", data).apply()
    }

    fun loadTimePresets(context: Context): List<TimePreset> {
        val prefs = context.getSharedPreferences(PREFS_TIME, Context.MODE_PRIVATE)
        val raw = prefs.getString("presets_data", "") ?: ""
        if (raw.isEmpty()) return emptyList()

        return raw.split(";").map {
            val p = it.split("|")
            if (p.size >= 4) {
                TimePreset(p[0], p[1], p[2], p[3])
            } else {
                TimePreset("error", "Error", "00:00", "00:00") // Fallback
            }
        }
    }
}