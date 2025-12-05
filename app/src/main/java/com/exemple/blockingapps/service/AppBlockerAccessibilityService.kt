package com.exemple.blockingapps.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import com.exemple.blockingapps.data.common.BlockState
import com.exemple.blockingapps.data.local.FakeLocalDatabase


class AppBlockerAccessibilityService : AccessibilityService() {
    private val CHANNEL_ID = "AppBlockerChannel"
    private val NOTIFICATION_ID = 101

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            pkg != packageName &&
            pkg in com.exemple.blockingapps.data.common.BlockState.blockedPackages) {

            Log.e("BLOCKER", "App event: ${event.eventType} | PKG: $pkg | Blocked: true. Attempting to go Home.")

            val success = performGlobalAction(GLOBAL_ACTION_HOME)

            if (success) {
                Log.e("BLOCKER", "Successfully performed GLOBAL_ACTION_HOME.")
            } else {
                Log.e("BLOCKER", "Failed to perform GLOBAL_ACTION_HOME.")
            }

        }
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
    }

    @SuppressLint("ForegroundServiceType")
    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "App Blocker"
            val descriptionText = "App Blocker is running to monitor and block selected apps."
            val importance = NotificationManager.IMPORTANCE_LOW // Dùng LOW để ít gây phiền nhiễu
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("App Blocker Active")
            .setContentText("Monitoring apps in the background.")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.e("BLOCKER", "AppBlockerAccessibilityService IS NOW CONNECTED AND READY.")

        BlockState.blockedPackages = FakeLocalDatabase.loadBlockedPackages()
        Log.e("BLOCKER", "Service re-loaded blocked packages: ${BlockState.blockedPackages}")
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        Log.e("BLOCKER", "AppBlockerAccessibilityService DESTROYED.")
        com.exemple.blockingapps.ui.overlay.BlockOverlay.hide(this)
        super.onDestroy()
    }
}