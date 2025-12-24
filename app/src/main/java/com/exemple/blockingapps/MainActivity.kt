package com.exemple.blockingapps

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.exemple.blockingapps.data.common.BlockState
import com.exemple.blockingapps.data.local.FakeLocalDatabase
import com.exemple.blockingapps.data.repo.UserRepository
import com.exemple.blockingapps.di.LocalUserRepository
import com.exemple.blockingapps.model.network.RetrofitClient
import com.exemple.blockingapps.navigation.AppNavHost
import com.exemple.blockingapps.ui.home.HomeViewModel
import com.exemple.blockingapps.ui.theme.BlockingAppsTheme
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)
        val coarseLocationGranted = permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)

        if (fineLocationGranted || coarseLocationGranted) {
            Log.d("GEO", "Location permission granted")
            askBackgroundLocationPermission()
        } else {
            Log.e("GEO", "User denied location permission")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        enableEdgeToEdge()

        askLocationPermissions()
        askBatteryOptimizationPermission()
        askOverlayPermission()
        askAccessibilityPermission()

        val userRepository = UserRepository(FakeLocalDatabase)
        BlockState.blockedPackages = FakeLocalDatabase.loadBlockedPackages(this)

        fetchRulesFromServer()
        startLocationUpdates()

        setContent {
            BlockingAppsTheme {
                val navController = rememberNavController()
                val homeViewModel: HomeViewModel = viewModel()

                CompositionLocalProvider(
                    LocalUserRepository provides userRepository
                ) {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        AppNavHost(
                            navController = navController,
                            homeViewModel = homeViewModel
                        )
                    }
                }
            }
        }
    }

    private fun fetchRulesFromServer() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val rules = RetrofitClient.api.getBlockRules()
                val serverBlockedList = rules.filter { it.isBlocked }.map { it.packageName }.toSet()
                val firstRule = rules.firstOrNull()

                withContext(Dispatchers.Main) {
                    BlockState.blockedPackages = serverBlockedList

                    if (firstRule != null) {
                        BlockState.targetLatitude = firstRule.latitude ?: 0.0
                        BlockState.targetLongitude = firstRule.longitude ?: 0.0
                        BlockState.targetRadius = firstRule.radius ?: 100.0
                    }
                    Log.i("API_SYNC", "Synced ${serverBlockedList.size} rules")
                }
            } catch (e: Exception) {
                Log.e("API_SYNC", "Failed to fetch rules: ${e.message}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000).build()

        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return

                if (BlockState.targetLatitude != 0.0) {
                    val results = FloatArray(1)
                    android.location.Location.distanceBetween(
                        location.latitude,
                        location.longitude,
                        BlockState.targetLatitude,
                        BlockState.targetLongitude,
                        results
                    )
                    BlockState.isInStudyZone = results[0] <= BlockState.targetRadius
                }
            }
        }, Looper.getMainLooper())
    }

    private fun askLocationPermissions() {
        locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    private fun askBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasBackgroundLocation = checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasBackgroundLocation) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }

    @SuppressLint("ServiceCast")
    private fun askBatteryOptimizationPermission() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            try { startActivity(intent) } catch (e: Exception) { Log.e("BLOCKER", "Battery Opt Error: ${e.message}") }
        }
    }

    private fun askOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabledServices?.contains(packageName) == true
    }

    private fun askAccessibilityPermission() {
        if (!isAccessibilityServiceEnabled()) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }
}