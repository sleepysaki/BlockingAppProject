package com.exemple.blockingapps.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.exemple.blockingapps.ui.family.FamilyManagementScreen
import com.exemple.blockingapps.ui.geoblock.GeoBlockScreen
import com.exemple.blockingapps.ui.geoblock.GeoBlockViewModel
import com.exemple.blockingapps.ui.history.RecommendationScreen
import com.exemple.blockingapps.ui.history.UsageHistoryScreen
import com.exemple.blockingapps.ui.home.HomeScreen
import com.exemple.blockingapps.ui.home.HomeViewModel
import com.exemple.blockingapps.ui.login.LoginScreen

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val BLOCKED = "blocked"
    const val FAMILY = "family"
    const val TIMELIMIT = "timelimit"
    const val HISTORY = "history"
    const val RECOMMEND = "recommend"
    const val FACE = "face"
    const val GEOBLOCK = "geoblock"
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavHost(
    navController: NavHostController,
    homeViewModel: HomeViewModel
) {
    NavHost(navController = navController, startDestination = "login") { // Đổi start thành login

        // Màn hình Login
        composable("login") {
            LoginScreen(onLoginSuccess = {
                // Khi login xong thì chuyển sang Home và xóa Login khỏi BackStack (để back không quay lại login)
                navController.navigate("home") {
                    popUpTo("login") { inclusive = true }
                }
            })
        }

        // Màn hình Home
        composable("home") {
            HomeScreen(viewModel = homeViewModel)
        }
    }
}
@Composable
fun BlockedAppsScreen() {
}
