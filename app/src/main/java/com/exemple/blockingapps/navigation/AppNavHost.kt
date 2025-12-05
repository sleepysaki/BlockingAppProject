package com.exemple.blockingapps.navigation

import LoginScreen
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.exemple.blockingapps.ui.home.HomeScreen

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
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {

        // LOGIN
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToFamily = { navController.navigate(Routes.FAMILY) },
                onNavigateToBlockedApps = { navController.navigate(Routes.BLOCKED) },
                onNavigateToTimeLimit = { navController.navigate(Routes.TIMELIMIT) },
                onNavigateToHistory = { navController.navigate(Routes.HISTORY) },
                onNavigateToRecommend = { navController.navigate(Routes.RECOMMEND) },
                onNavigateToFace = { navController.navigate(Routes.FACE) },
                onNavigateToGeoBlock = { navController.navigate(Routes.GEOBLOCK) },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.BLOCKED) {
            BlockedAppsScreen()
        }

        composable(Routes.FAMILY) {  }
        composable(Routes.TIMELIMIT) { }
        composable(Routes.HISTORY) { }
        composable(Routes.RECOMMEND) { }
        composable(Routes.FACE) {  }
        composable(Routes.GEOBLOCK) {  }
    }
}

@Composable
fun BlockedAppsScreen() {
}
