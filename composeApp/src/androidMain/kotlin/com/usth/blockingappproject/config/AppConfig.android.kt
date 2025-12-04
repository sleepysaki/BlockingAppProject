package com.usth.blockingappproject.config

// Android implementation
actual object AppConfig {
    // Android Emulator special IP for localhost
    actual val BASE_URL: String = "http://10.0.2.2:8080"
}