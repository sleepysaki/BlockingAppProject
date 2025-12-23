package com.exemple.blockingapps.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.exemple.blockingapps.ui.blockedapps.BlockedAppsScreen // Ensure this import exists
import com.exemple.blockingapps.ui.family.FamilyManagementScreen
import com.exemple.blockingapps.ui.geoblock.GeoBlockScreen
import com.exemple.blockingapps.ui.geoblock.GeoBlockViewModel
import com.exemple.blockingapps.ui.group.GroupScreen // 👈 Import your GroupScreen
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
    const val GROUPS = "groups" // 👈 Add Groups route
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavHost(navController: NavHostController, homeViewModel: HomeViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // Mock ID for testing Groups (matches MainActivity logic)
    val testUserId = "3fa85f64-5717-4562-b3fc-2c963f66afa6"

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
                viewModel = homeViewModel, // Pass viewModel if needed by Home
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
                // ADD YOUR GROUP NAVIGATION HERE
                onNavigateToGroups = { navController.navigate(Routes.GROUPS) },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.BLOCKED) {
            // Using logic from Input 3 (The most complete one)
            BlockedAppsScreen(
                viewModel = homeViewModel, // Uncomment if BlockedAppsScreen needs it
                onBack = { navController.popBackStack() } // Uncomment if BlockedAppsScreen needs it
            )
        }

        composable(Routes.FAMILY) {
            FamilyManagementScreen(viewModel = homeViewModel)
        }

        composable(Routes.TIMELIMIT) {
            // Placeholder for TimeLimit screen
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

        composable(Routes.FACE) {
            // Placeholder for Face screen
        }

        composable(Routes.GEOBLOCK) {
            val geoViewModel: GeoBlockViewModel = viewModel()
            GeoBlockScreen(viewModel = geoViewModel)
        }

        // ADD YOUR GROUP SCREEN COMPOSABLE
        composable(Routes.GROUPS) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val userId = com.exemple.blockingapps.data.local.SessionManager.getUserId(context)

            if (userId != null) {
                com.exemple.blockingapps.ui.group.GroupScreen(currentUserId = userId)
            } else {
                // If session expired or missing, force logout
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            }
        }
    }
}