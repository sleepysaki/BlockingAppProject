package com.usth.blockingappproject.server.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import com.usth.blockingappproject.model.api.*
import com.usth.blockingappproject.server.service.AuthService

fun Route.authRoutes(authService: AuthService) {
    route("/api/auth") {
        
        // POST http://localhost:8080/api/auth/register
        post("/register") {
            val request = call.receive<RegisterRequest>()
            val response = authService.registerUser(request)
            
            if (response != null) {
                call.respond(HttpStatusCode.Created, response)
            } else {
                call.respond(HttpStatusCode.Conflict, "Email already exists")
            }
        }

        // POST http://localhost:8080/api/auth/login
        post("/login") {
            val request = call.receive<LoginRequest>()
            val response = authService.loginUser(request)

            if (response != null) {
                call.respond(HttpStatusCode.OK, response)
            } else {
                call.respond(HttpStatusCode.Unauthorized, "Invalid credentials")
            }
        }
    }
}