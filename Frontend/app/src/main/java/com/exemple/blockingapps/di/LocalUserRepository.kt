package com.exemple.blockingapps.di

import androidx.compose.runtime.staticCompositionLocalOf
import com.exemple.blockingapps.data.repo.UserRepository

val LocalUserRepository = staticCompositionLocalOf<UserRepository> {
    error("UserRepository not provided")
}