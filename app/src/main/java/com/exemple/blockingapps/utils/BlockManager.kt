package com.exemple.blockingapps.utils

import android.content.Context
import com.exemple.blockingapps.model.GroupRuleDTO

object BlockManager {
    private const val PREF_NAME = "blocked_apps_pref"
    private const val KEY_BLOCKED_PACKAGES = "blocked_packages"

    fun saveBlockedPackages(context: Context, rules: List<GroupRuleDTO>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        val blockedList = rules.filter { it.isBlocked }.map { it.packageName }.toSet()

        prefs.edit().putStringSet(KEY_BLOCKED_PACKAGES, blockedList).apply()
    }

    fun isAppBlocked(context: Context, packageName: String): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val blockedList = prefs.getStringSet(KEY_BLOCKED_PACKAGES, emptySet()) ?: emptySet()

        //Debug
        android.util.Log.d("BlockService", "Checking $packageName inside: $blockedList")
        return blockedList.contains(packageName)
    }

    fun updateRules(context: Context, rules: List<GroupRuleDTO>) {
        saveBlockedPackages(context, rules)
    }
}