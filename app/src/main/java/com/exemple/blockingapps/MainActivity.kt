package com.exemple.blockingapps

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.compose.rememberNavController
import com.exemple.blockingapps.data.common.BlockState
import com.exemple.blockingapps.data.local.FakeLocalDatabase
import com.exemple.blockingapps.data.repo.UserRepository
import com.exemple.blockingapps.di.LocalUserRepository
import com.exemple.blockingapps.navigation.AppNavHost
import com.exemple.blockingapps.ui.theme.BlockingAppsTheme

class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        askOverlayPermission()

        askAccessibilityPermission()

        val userRepository = UserRepository(FakeLocalDatabase)
        BlockState.blockedPackages = FakeLocalDatabase.loadBlockedPackages()
        Log.e("BLOCKER", "Loaded blocked packages: ${BlockState.blockedPackages}")

        setContent {
            BlockingAppsTheme {
                CompositionLocalProvider(
                    LocalUserRepository provides userRepository
                ) {
                    val navController = rememberNavController()

                    Surface(color = MaterialTheme.colorScheme.background) {
                        AppNavHost(navController = navController)
                    }
                }
            }
        }
    }

    private fun askOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val serviceId = packageName + "/" + com.exemple.blockingapps.service.AppBlockerAccessibilityService::class.java.name

        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )

        return enabledServices?.contains(serviceId) == true
    }

    private fun askAccessibilityPermission() {
        if (!isAccessibilityServiceEnabled()) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }
    }
}
