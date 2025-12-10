package com.usth.blockingappproject.server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database
import com.usth.blockingappproject.server.service.AuthService
import com.usth.blockingappproject.server.routes.authRoutes

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // 1. Serialization (JSON)
    install(ContentNegotiation) {
        json()
    }

    // 2. Database Connection
    // Ensure you have a Postgres container running on port 5432
    Database.connect(
        url = "jdbc:postgresql://localhost:5432/blockingapp_db", 
        driver = "org.postgresql.Driver",
        user = "postgres", 
        password = "password"
    )

    // 3. Initialize Services
    val authService = AuthService()

    // 4. Register Routes
    routing {
        authRoutes(authService)
    }
}