package com.exemple.blockingapps.utils

import android.location.Location
import android.util.Log
import com.exemple.blockingapps.data.common.BlockState

object GeofenceHelper {
    fun checkLocationAndBlock(currentLat: Double, currentLng: Double, targetLat: Double, targetLng: Double, radius: Float) {
        val results = FloatArray(1)

        Location.distanceBetween(currentLat, currentLng, targetLat, targetLng, results)
        val distanceInMeters = results[0]

        Log.d("GEO_DEBUG", "Khoảng cách: $distanceInMeters m. Bán kính chặn: $radius m")

        if (distanceInMeters <= radius) {
            Log.d("GEO_DEBUG", "--> TRONG VÙNG CHẶN")
            BlockState.isGeofenceActive = true
        } else {
            Log.d("GEO_DEBUG", "--> NGOÀI VÙNG CHẶN")
            BlockState.isGeofenceActive = false
        }
    }
}