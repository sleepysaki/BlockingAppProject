package com.exemple.blockingapps.data.common
object BlockState {
    var isBlocking: Boolean = false

    var blockedPackages: Set<String> = emptySet()
}
