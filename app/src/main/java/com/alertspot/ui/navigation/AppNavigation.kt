package com.alertspot.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.alertspot.ui.screen.*
import com.alertspot.ui.theme.Blue
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
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun AppNavigation(viewModel: AlertViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem(Screen.Map, "Map", Icons.Filled.Map, Icons.Outlined.Map),
        BottomNavItem(Screen.AlertList, "Alerts", Icons.Filled.Notifications, Icons.Outlined.Notifications),
        BottomNavItem(Screen.Settings, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    val showBottomBar = currentRoute in bottomNavItems.map { it.screen.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                IosBottomBar(
                    items = bottomNavItems,
                    currentRoute = currentRoute,
                    onItemClick = { item ->
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

/* ─── iOS-style frosted-glass bottom bar with animated pill indicator ────── */

@Composable
private fun IosBottomBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemClick: (BottomNavItem) -> Unit
) {
    val selectedIndex = items.indexOfFirst { it.screen.route == currentRoute }.coerceAtLeast(0)

    // Frosted glass container
    Column {
        // Subtle top hair-line separator
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        )

        // Top fade gradient — mimics iOS translucency edge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                .navigationBarsPadding()
                .padding(top = 6.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                IosTabItem(
                    item = item,
                    isSelected = index == selectedIndex,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@Composable
private fun IosTabItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Spring-based scale pop on selection
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tabScale"
    )

    // Animated pill background height & alpha
    val pillAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pillAlpha"
    )

    val iconTint by animateColorAsState(
        targetValue = if (isSelected) Blue
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "iconTint"
    )

    val labelColor by animateColorAsState(
        targetValue = if (isSelected) Blue
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "labelTint"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null   // no ripple — iOS feel
            ) { onClick() }
            .padding(horizontal = 20.dp, vertical = 4.dp)
    ) {
        // Icon with pill background
        Box(
            modifier = Modifier
                .height(30.dp)
                .widthIn(min = 56.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(Blue.copy(alpha = (pillAlpha * 0.12f).coerceIn(0f, 1f))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.label,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = item.label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = labelColor,
            maxLines = 1
        )
    }
}

