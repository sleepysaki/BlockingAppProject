package com.exemple.blockingapps.data.local

import android.content.Context
import com.exemple.blockingapps.data.model.User

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
}