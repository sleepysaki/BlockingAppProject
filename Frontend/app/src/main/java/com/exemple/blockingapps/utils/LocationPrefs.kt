package com.exemple.blockingapps.utils

import android.content.Context

object LocationPrefs {
    private const val PREF_NAME = "location_prefs"
    private const val KEY_LAT = "target_lat"
    private const val KEY_LONG = "target_long"
    private const val KEY_RADIUS = "target_radius"

    // Lưu tọa độ vùng cấm
    fun saveTargetLocation(context: Context, lat: Double, long: Double, radius: Double) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putFloat(KEY_LAT, lat.toFloat())
            putFloat(KEY_LONG, long.toFloat())
            putFloat(KEY_RADIUS, radius.toFloat())
            apply()
        }
    }

    // Xóa tọa độ (khi không có rule nào)
    fun clearTargetLocation(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    // Lấy tọa độ để Service dùng
    // Trả về Triple(Lat, Long, Radius) hoặc null nếu chưa set
    fun getTargetLocation(context: Context): Triple<Double, Double, Float>? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_LAT)) return null

        val lat = prefs.getFloat(KEY_LAT, 0f).toDouble()
        val long = prefs.getFloat(KEY_LONG, 0f).toDouble()
        val radius = prefs.getFloat(KEY_RADIUS, 0f)

        return Triple(lat, long, radius)
    }
}