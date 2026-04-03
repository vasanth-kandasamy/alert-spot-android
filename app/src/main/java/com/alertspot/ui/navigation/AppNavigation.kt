package com.alertspot.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.alertspot.ui.screen.*
import com.alertspot.ui.theme.Blue
import com.alertspot.viewmodel.AlertViewModel
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.roundToInt

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

    val bottomNavItems = remember {
        listOf(
            BottomNavItem(Screen.Map, "Map", Icons.Filled.Map, Icons.Outlined.Map),
            BottomNavItem(Screen.AlertList, "Alerts", Icons.Filled.Notifications, Icons.Outlined.Notifications),
            BottomNavItem(Screen.Settings, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
        )
    }
    val bottomRoutes = remember(bottomNavItems) { bottomNavItems.map { it.screen.route }.toSet() }
    var selectedBottomRoute by rememberSaveable { mutableStateOf(Screen.Map.route) }
    var pendingBottomRoute by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(currentRoute) {
        val route = currentRoute ?: return@LaunchedEffect
        if (route !in bottomRoutes) return@LaunchedEffect

        val pendingRoute = pendingBottomRoute
        if (pendingRoute == null) {
            selectedBottomRoute = route
        } else if (route == pendingRoute) {
            selectedBottomRoute = route
            pendingBottomRoute = null
        }
    }

    val showBottomBar = currentRoute in bottomRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                IosBottomBar(
                    items = bottomNavItems,
                    selectedRoute = selectedBottomRoute,
                    onItemClick = { item ->
                        selectedBottomRoute = item.screen.route
                        pendingBottomRoute = item.screen.route
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
    selectedRoute: String,
    onItemClick: (BottomNavItem) -> Unit
) {
    val selectedIndex = items.indexOfFirst { it.screen.route == selectedRoute }.coerceAtLeast(0)
    val darkTheme = isSystemInDarkTheme()
    val density = LocalDensity.current
    var settledIndex by remember { mutableIntStateOf(selectedIndex) }

    LaunchedEffect(selectedIndex) {
        settledIndex = selectedIndex
    }

    Column {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        )

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

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .height(64.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = if (darkTheme) 0.96f else 0.98f),
                            MaterialTheme.colorScheme.surface.copy(alpha = if (darkTheme) 0.88f else 0.94f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (darkTheme) 0.08f else 0.05f),
                    shape = RoundedCornerShape(30.dp)
                )
        ) {
            val slotWidth = maxWidth / items.size
            val bubbleInset = 6.dp
            val bubbleWidth = slotWidth - (bubbleInset * 2)
            val slotWidthPx = with(density) { slotWidth.toPx() }
            val bubbleInsetPx = with(density) { bubbleInset.toPx() }
            val bubbleWidthPx = with(density) { bubbleWidth.toPx() }
            val selectedOffsetPx = (slotWidthPx * settledIndex) + bubbleInsetPx
            val maxOffsetPx = (slotWidthPx * (items.lastIndex)) + bubbleInsetPx

            var isDragging by remember { mutableStateOf(false) }
            val bubbleOffset = remember { Animatable(selectedOffsetPx) }

            // When settledIndex changes (tap or external nav), animate to target
            LaunchedEffect(selectedOffsetPx) {
                if (!isDragging) {
                    bubbleOffset.animateTo(
                        targetValue = selectedOffsetPx,
                        animationSpec = spring(
                            dampingRatio = 0.82f,
                            stiffness = 420f
                        )
                    )
                }
            }

            val bubbleOffsetPx = bubbleOffset.value
            val previewIndex = if (slotWidthPx > 0f) {
                ((bubbleOffsetPx - bubbleInsetPx) / slotWidthPx).roundToInt().coerceIn(0, items.lastIndex)
            } else {
                settledIndex
            }

            fun indexFromOffsetX(offsetX: Float): Int {
                if (slotWidthPx <= 0f) return settledIndex
                return floor(offsetX / slotWidthPx)
                    .toInt()
                    .coerceIn(0, items.lastIndex)
            }

            val coroutineScope = rememberCoroutineScope()

            fun snapToNearestItem() {
                val currentOffset = bubbleOffset.value
                val targetIndex = if (slotWidthPx > 0f) {
                    ((currentOffset - bubbleInsetPx) / slotWidthPx)
                        .roundToInt()
                        .coerceIn(0, items.lastIndex)
                } else {
                    settledIndex
                }
                isDragging = false
                settledIndex = targetIndex
                val targetOffset = (slotWidthPx * targetIndex) + bubbleInsetPx
                coroutineScope.launch {
                    bubbleOffset.animateTo(
                        targetValue = targetOffset,
                        animationSpec = spring(
                            dampingRatio = 0.82f,
                            stiffness = 420f
                        )
                    )
                }
                onItemClick(items[targetIndex])
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset { IntOffset(x = bubbleOffsetPx.roundToInt(), y = 0) }
                        .width(bubbleWidth)
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    if (darkTheme) {
                                        Color.White.copy(alpha = 0.14f)
                                    } else {
                                        Color.White.copy(alpha = 0.96f)
                                    },
                                    Blue.copy(alpha = if (darkTheme) 0.22f else 0.14f),
                                    if (darkTheme) {
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
                                    } else {
                                        Color.White.copy(alpha = 0.82f)
                                    }
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = if (darkTheme) {
                                Color.White.copy(alpha = 0.08f)
                            } else {
                                Blue.copy(alpha = 0.08f)
                            },
                            shape = RoundedCornerShape(24.dp)
                        )
                )
            }

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(items, slotWidthPx) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                isDragging = true
                                coroutineScope.launch {
                                    bubbleOffset.snapTo(
                                        (offset.x - (bubbleWidthPx / 2f))
                                            .coerceIn(bubbleInsetPx, maxOffsetPx)
                                    )
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                val newOffset = (bubbleOffset.value + dragAmount)
                                    .coerceIn(bubbleInsetPx, maxOffsetPx)
                                coroutineScope.launch {
                                    bubbleOffset.snapTo(newOffset)
                                }
                            },
                            onDragEnd = { snapToNearestItem() },
                            onDragCancel = {
                                isDragging = false
                                coroutineScope.launch {
                                    bubbleOffset.animateTo(
                                        targetValue = selectedOffsetPx,
                                        animationSpec = spring(
                                            dampingRatio = 0.82f,
                                            stiffness = 420f
                                        )
                                    )
                                }
                            }
                        )
                    }
                    .pointerInput(items, slotWidthPx) {
                        detectTapGestures { offset ->
                            val targetIndex = indexFromOffsetX(offset.x)
                            settledIndex = targetIndex
                            val targetOffset = (slotWidthPx * targetIndex) + bubbleInsetPx
                            coroutineScope.launch {
                                bubbleOffset.animateTo(
                                    targetValue = targetOffset,
                                    animationSpec = spring(
                                        dampingRatio = 0.82f,
                                        stiffness = 420f
                                    )
                                )
                            }
                            onItemClick(items[targetIndex])
                        }
                    }
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    IosTabItem(
                        item = item,
                        isSelected = index == previewIndex,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun IosTabItem(
    item: BottomNavItem,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "iconScale"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.72f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "contentAlpha"
    )

    val iconTint by animateColorAsState(
        targetValue = if (isSelected) Blue
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.46f),
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
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
            contentDescription = item.label,
            tint = iconTint,
            modifier = Modifier
                .size(22.dp)
                .scale(iconScale)
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = item.label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = labelColor.copy(alpha = contentAlpha),
            maxLines = 1
        )
    }
}

