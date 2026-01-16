package com.exemple.blockingapps.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.exemple.blockingapps.data.common.BlockState
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return

        when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                BlockState.isInStudyZone = true
                Log.d("GEO", "Bé đã vào vùng học tập")
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                BlockState.isInStudyZone = false
                Log.d("GEO", "Bé đã ra khỏi vùng học tập")
            }
        }
    }
}