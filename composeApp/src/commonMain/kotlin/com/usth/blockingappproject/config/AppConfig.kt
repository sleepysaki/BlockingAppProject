package com.usth.blockingappproject.config

// Define the expectation (mechanism expect/actual of KPM)
// Handle platform divergence

// Expect a variable called BaseUrl to exist
// Define actual value in androidMain and webMain
expect object AppConfig {
    val BASE_URL: String
}