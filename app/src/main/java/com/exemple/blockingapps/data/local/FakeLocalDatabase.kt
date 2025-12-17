package com.exemple.blockingapps.data.local

import com.exemple.blockingapps.data.model.User

object FakeLocalDatabase {
    val users = mutableListOf(
        User(email="test@gmail.com", name="Test", password="123456", role="Parent")
    )

    val blockedAppsPackages = mutableSetOf<String>()
    fun loadBlockedPackages(): Set<String> {
        blockedAppsPackages.add("com.google.android.youtube")
        return blockedAppsPackages.toSet()
    }
}
