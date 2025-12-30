package com.exemple.blockingapps.data.repo

import com.exemple.blockingapps.data.local.FakeLocalDatabase
import com.exemple.blockingapps.data.model.User

class UserRepository(private val db: FakeLocalDatabase) {

    // Note: We are currently handling Login directly in LoginScreen + SessionManager.
    // So we just return 'true' here to fix the compilation error.

    fun login(email: String, password: String): Boolean {
        // Placeholder: Always success
        return true
    }

    fun register(user: User): Boolean {
        // Placeholder: Always success
        return true
    }
}