package com.exemple.blockingapps.utils

import android.content.Context

object LocationPrefs {
    private const val PREFS_NAME = "GeofencePrefs"

    fun saveTargetLocation(context: Context, lat: Double, lng: Double, radius: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putFloat("target_lat", lat.toFloat())
            putFloat("target_lng", lng.toFloat())
            putFloat("target_radius", radius)
            apply()
        }
    }

    fun getTargetLocation(context: Context): Triple<Double, Double, Float>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lat = prefs.getFloat("target_lat", 0f).toDouble()
        val lng = prefs.getFloat("target_lng", 0f).toDouble()
        val radius = prefs.getFloat("target_radius", 0f)
        return if (lat != 0.0) Triple(lat, lng, radius) else null
    }
}