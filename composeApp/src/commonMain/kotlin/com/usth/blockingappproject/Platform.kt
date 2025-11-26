package com.usth.blockingappproject

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform