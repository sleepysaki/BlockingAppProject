
package com.exemple.blockingapps.data.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object BlockState {
    var isBlocking: Boolean = true
    var blockedPackages: Set<String> = emptySet()
    var targetLatitude: Double = 0.0
    var targetLongitude: Double = 0.0
    var targetRadius: Double = 100.0
    val remainingTimeSeconds: ConcurrentHashMap<String, Long> = ConcurrentHashMap()

    private val _instantLockRemaining = MutableStateFlow(0L)
    val instantLockRemaining: StateFlow<Long> = _instantLockRemaining
    var isGeofenceActive by mutableStateOf(false)
    var isInStudyZone: Boolean = false
    var restrictedApps = setOf(
        "com.google.android.youtube",
        "com.android.settings",
        "com.android.vending"
    )
    fun setInstantLockTime(seconds: Long) {
        _instantLockRemaining.value = seconds
    }
}