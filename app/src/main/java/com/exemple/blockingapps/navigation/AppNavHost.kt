package com.exemple.blockingapps.navigation

import LoginScreen
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.exemple.blockingapps.ui.blockedapps.BlockedAppsScreen
import com.exemple.blockingapps.ui.family.FamilyManagementScreen
import com.exemple.blockingapps.ui.geoblock.GeoBlockScreen
import com.exemple.blockingapps.ui.geoblock.GeoBlockViewModel
import com.exemple.blockingapps.ui.history.RecommendationScreen
import com.exemple.blockingapps.ui.history.UsageHistoryScreen
import com.exemple.blockingapps.ui.home.HomeScreen
import com.exemple.blockingapps.ui.home.HomeViewModel
import com.exemple.blockingapps.ui.timelimits.TimeLimitScreen

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
fun AppNavHost(navController: NavHostController, homeViewModel: HomeViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {

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
                onNavigateToRecommend = {
                    homeViewModel.loadRealUsageAndGenerateRecs(context)
                    navController.navigate(Routes.RECOMMEND)
                },
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
            BlockedAppsScreen(
                viewModel = homeViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.FAMILY) {
            FamilyManagementScreen(viewModel = homeViewModel)
        }
        composable(Routes.TIMELIMIT) {
            TimeLimitScreen(viewModel = homeViewModel, onBack = { navController.popBackStack() })
        }
        composable(Routes.HISTORY) {
            UsageHistoryScreen(viewModel = homeViewModel)
        }
        composable(Routes.RECOMMEND) {
            RecommendationScreen(
                viewModel = homeViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.FACE) {  }
        composable(Routes.GEOBLOCK) {
            val geoViewModel: GeoBlockViewModel = viewModel()
            GeoBlockScreen(viewModel = geoViewModel)
        }
    }
}

@Composable
fun BlockedAppsScreen() {
}
