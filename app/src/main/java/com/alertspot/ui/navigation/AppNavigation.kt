package com.alertspot.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.alertspot.ui.screen.*
import com.alertspot.viewmodel.AlertViewModel

sealed class Screen(val route: String) {
    data object Map : Screen("map")
    data object AlertList : Screen("alerts")
    data object Settings : Screen("settings")
    data object AddLocation : Screen("add_location")
    data object History : Screen("history")
    data object EditLocation : Screen("edit_location/{locationId}") {
        fun createRoute(locationId: String) = "edit_location/$locationId"
    }
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: @Composable () -> Unit
)

@Composable
fun AppNavigation(viewModel: AlertViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem(Screen.Map, "Map") {
            Icon(Icons.Default.Map, contentDescription = "Map")
        },
        BottomNavItem(Screen.AlertList, "Alerts") {
            Icon(Icons.Default.Notifications, contentDescription = "Alerts")
        },
        BottomNavItem(Screen.Settings, "Settings") {
            Icon(Icons.Default.Settings, contentDescription = "Settings")
        }
    )

    val showBottomBar = currentRoute in bottomNavItems.map { it.screen.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = item.icon,
                            label = { Text(item.label) },
                            selected = currentRoute == item.screen.route,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Map.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Map.route) {
                MapScreen(
                    viewModel = viewModel,
                    onAddAlert = { navController.navigate(Screen.AddLocation.route) }
                )
            }

            composable(Screen.AlertList.route) {
                AlertListScreen(
                    viewModel = viewModel,
                    onAddAlert = { navController.navigate(Screen.AddLocation.route) },
                    onEditAlert = { locationId ->
                        navController.navigate(Screen.EditLocation.createRoute(locationId))
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToHistory = { navController.navigate(Screen.History.route) }
                )
            }

            composable(Screen.AddLocation.route) {
                AddLocationScreen(
                    viewModel = viewModel,
                    onDismiss = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.EditLocation.route,
                arguments = listOf(navArgument("locationId") { type = NavType.StringType })
            ) { backStackEntry ->
                val locationId = backStackEntry.arguments?.getString("locationId") ?: return@composable
                EditLocationScreen(
                    viewModel = viewModel,
                    locationId = locationId,
                    onDismiss = { navController.popBackStack() }
                )
            }

            composable(Screen.History.route) {
                HistoryScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
