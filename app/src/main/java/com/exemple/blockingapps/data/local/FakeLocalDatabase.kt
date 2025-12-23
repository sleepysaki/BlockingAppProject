package com.exemple.blockingapps.data.local

import android.content.Context
import com.exemple.blockingapps.data.model.User
import com.exemple.blockingapps.ui.home.TimePreset

object FakeLocalDatabase {
    private const val PREFS_NAME = "BlockedAppsPrefs"
    private const val KEY_PACKAGES = "blocked_packages"

    val users = mutableListOf(
        User(email="test@gmail.com", name="Test", password="123456", role="Parent")
    )

    fun saveBlockedPackages(context: Context, packages: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_PACKAGES, packages).apply()
    }

    fun loadBlockedPackages(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_PACKAGES, emptySet()) ?: emptySet()
    }

    fun saveTimePresets(context: Context, presets: List<TimePreset>) {
        val prefs = context.getSharedPreferences("TimePresets", Context.MODE_PRIVATE)
        val data = presets.joinToString(";") { "${it.id}|${it.label}|${it.startTime}|${it.endTime}" }
        prefs.edit().putString("presets_data", data).apply()
    }

    fun loadTimePresets(context: Context): List<TimePreset> {
        val prefs = context.getSharedPreferences("TimePresets", Context.MODE_PRIVATE)
        val raw = prefs.getString("presets_data", "") ?: ""
        if (raw.isEmpty()) return emptyList()

        return raw.split(";").map {
            val p = it.split("|")
            TimePreset(p[0], p[1], p[2], p[3])
        }
    }
}