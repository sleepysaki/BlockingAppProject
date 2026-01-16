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
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.exemple.blockingapps.data.common.BlockState
import com.exemple.blockingapps.data.local.FakeLocalDatabase
import com.exemple.blockingapps.data.repo.UserRepository
import com.exemple.blockingapps.di.LocalUserRepository
import com.exemple.blockingapps.model.GroupRuleDTO
import com.exemple.blockingapps.model.network.RetrofitClient
import com.exemple.blockingapps.navigation.AppNavHost
import com.exemple.blockingapps.ui.home.HomeViewModel
import com.exemple.blockingapps.ui.theme.BlockingAppsTheme
import com.exemple.blockingapps.utils.BlockManager
import com.exemple.blockingapps.worker.SyncRulesWorker
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Hardcode ID để test (như đã thống nhất), sau này thay bằng ID thật từ Login
    private val currentUserId = "36050457-f112-4762-a7f7-24daab6986ce"

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

        // Xin quyền (Lưu ý: Hỏi dồn dập thế này UX hơi kém, nhưng OK để test)
        askLocationPermissions()
        askBatteryOptimizationPermission()
        askOverlayPermission()
        askAccessibilityPermission()

        val userRepository = UserRepository(FakeLocalDatabase)

        // 👇 KHỞI ĐỘNG SYNC NGAY LẬP TỨC (Update UI + Service)
        //fetchRulesFromServer()

        // 👇 KÍCH HOẠT WORKER CHẠY NGẦM (15 phút/lần)
        //setupPeriodicSyncWorker()

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

    // 👇 CẤU HÌNH WORKER (Phần bạn thiếu)
    private fun setupPeriodicSyncWorker() {
        val syncRequest = PeriodicWorkRequestBuilder<SyncRulesWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SyncRulesWorker",
            ExistingPeriodicWorkPolicy.KEEP, // Giữ worker cũ nếu đang chạy
            syncRequest
        )
        Log.d("MainActivity", "✅ Background Sync Worker Scheduled")
    }

    // 👇 CẬP NHẬT LOGIC FETCH ĐỂ KHỚP VỚI GROUP VIEW MODEL
    private fun fetchRulesFromServer() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Lấy danh sách nhóm của user
                val groupsResponse = RetrofitClient.apiService.getUserGroups(currentUserId)
                val allRules = mutableListOf<GroupRuleDTO>()

                if (groupsResponse.isSuccessful) {
                    val groups = groupsResponse.body() ?: emptyList()
                    // 2. Lấy luật của từng nhóm
                    for (group in groups) {
                        val rulesResponse = RetrofitClient.apiService.getGroupRules(group.groupId)
                        if (rulesResponse.isSuccessful) {
                            allRules.addAll(rulesResponse.body() ?: emptyList())
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    // 3. QUAN TRỌNG: Lưu vào BlockManager để AccessibilityService đọc được
                    BlockManager.saveBlockedPackages(this@MainActivity, allRules)

                    // (Tùy chọn) Cập nhật BlockState cũ nếu code UI cũ vẫn dùng
                    BlockState.blockedPackages = allRules.filter { it.isBlocked }.map { it.packageName }.toSet()

                    Log.i("API_SYNC", "Manual Sync: Loaded ${allRules.size} rules")
                }
            } catch (e: Exception) {
                Log.e("API_SYNC", "Failed to fetch rules: ${e.message}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        // Chỉ chạy update UI, logic chặn chính nằm ở Service
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build()

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    // Logic update UI nếu cần
                }
            }, Looper.getMainLooper())
        } catch (e: Exception) {
            Log.e("Location", "Error starting updates: ${e.message}")
        }
    }

    // --- CÁC HÀM XIN QUYỀN GIỮ NGUYÊN ---
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
                // Mở setting app để user tự bật
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