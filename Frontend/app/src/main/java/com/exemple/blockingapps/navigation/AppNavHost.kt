package com.exemple.blockingapps.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.lifecycle.ViewModelStoreOwner
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.exemple.blockingapps.ui.blockedapps.BlockedAppsScreen
import com.exemple.blockingapps.ui.family.FamilyManagementScreen
import com.exemple.blockingapps.ui.geoblock.GeoBlockScreen
import com.exemple.blockingapps.ui.geoblock.GeoBlockViewModel
import com.exemple.blockingapps.ui.group.GroupDetailScreen
import com.exemple.blockingapps.ui.group.GroupManagementScreen
import com.exemple.blockingapps.ui.group.GroupSettingsScreen
import com.exemple.blockingapps.ui.group.GroupViewModel
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

    // Group Routes
    const val GROUPS = "groups"
    const val GROUP_DETAIL = "group_detail/{groupId}/{groupName}"

    const val GROUP_SETTINGS = "group_settings/{groupId}/{groupName}/{joinCode}"
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
                viewModel = homeViewModel,
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
                onNavigateToGroups = { navController.navigate(Routes.GROUPS) },
                onLogout = {
                    com.exemple.blockingapps.data.local.SessionManager.clearSession(context)
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
            // Placeholder
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
            // Placeholder
        }

        composable(Routes.GEOBLOCK) {
            val geoViewModel: GeoBlockViewModel = viewModel()
            GeoBlockScreen(viewModel = geoViewModel)
        }

        // --- GROUP SCREENS ---

        composable(Routes.GROUPS) {
            val realUserId = com.exemple.blockingapps.data.local.SessionManager.getUserId(context)
            val groupViewModel: GroupViewModel = viewModel()

            if (realUserId != null && realUserId.isNotEmpty()) {
                com.exemple.blockingapps.ui.group.GroupScreen(
                    currentUserId = realUserId,
                    onGroupClick = { groupId, groupName ->
                        navController.navigate("group_detail/$groupId/$groupName")
                    },
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            }
        }

        composable(
            route = Routes.GROUP_DETAIL,
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType },
                navArgument("groupName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            val groupName = backStackEntry.arguments?.getString("groupName") ?: "Unknown Group"

            val currentUserId =
                com.exemple.blockingapps.data.local.SessionManager.getUserId(context) ?: ""

            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Routes.GROUPS)
            }
            val groupViewModel: GroupViewModel = viewModel(parentEntry)

            GroupDetailScreen(
                groupId = groupId,
                groupName = groupName,
                currentUserId = currentUserId,
                onBack = { navController.popBackStack() },
                onSettingsClick = {
                    val realJoinCode = groupViewModel.getJoinCodeForGroup(groupId)
                    navController.navigate("group_settings/$groupId/$groupName/$realJoinCode")
                },
                viewModel = groupViewModel
            )
        }

        composable(
            route = Routes.GROUP_SETTINGS,
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType },
                navArgument("groupName") { type = NavType.StringType },
                navArgument("joinCode") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val gId = backStackEntry.arguments?.getString("groupId") ?: ""
            val gName = backStackEntry.arguments?.getString("groupName") ?: ""
            val jCode = backStackEntry.arguments?.getString("joinCode") ?: ""

            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Routes.GROUPS)
            }
            val groupViewModel: GroupViewModel = viewModel(parentEntry)

            GroupSettingsScreen(
                groupId = gId,
                groupName = gName,
                joinCode = jCode,
                onBack = { navController.popBackStack() },
                viewModel = groupViewModel
            )
        }
        composable("group_management") {
            GroupManagementScreen(
                viewModel = homeViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}