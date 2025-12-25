package com.usth

import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.engine.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // 1. Khởi tạo Database
    try {
        DatabaseFactory.init()
        println("✅ Database Connected Successfully!")
    } catch (e: Exception) {
        println("❌ Database Connection Failed: ${e.message}")
    }

    // 2. Cấu hình JSON
    configureSerialization()

    // 3. Cấu hình API Routing
    configureRouting()
}