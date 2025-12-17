package com.exemple.blockingapps.data.repo

import com.exemple.blockingapps.data.local.FakeLocalDatabase
import com.exemple.blockingapps.data.model.User

class UserRepository(private val db: FakeLocalDatabase) {

    fun login(email: String, password: String): Boolean {
        return db.users.any { it.email == email && it.password == password }
    }

    fun register(user: User): Boolean {
        if (db.users.any { it.email == user.email }) return false
        db.users.add(user)
        return true
    }
}


