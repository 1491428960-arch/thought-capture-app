package com.thoughtcapture.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.thoughtcapture.app.ui.capture.CaptureScreen
import com.thoughtcapture.app.ui.detail.DetailScreen
import com.thoughtcapture.app.ui.fitness.FitnessScreen
import com.thoughtcapture.app.ui.inbox.InboxScreen
import com.thoughtcapture.app.ui.plan.PlanScreen
import com.thoughtcapture.app.ui.review.ReviewScreen
import com.thoughtcapture.app.ui.setup.SetupScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Capture : Screen("capture", "捕捉", Icons.Default.Edit)
    data object Inbox : Screen("inbox", "收件箱", Icons.Default.Inbox)
    data object Review : Screen("review", "复习", Icons.Default.MenuBook)
    data object Plan : Screen("plan", "计划", Icons.Default.CalendarToday)
    data object Fitness : Screen("fitness", "运动", Icons.Default.FitnessCenter)
}

val bottomNavItems = listOf(Screen.Capture, Screen.Inbox, Screen.Review, Screen.Plan, Screen.Fitness)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(startTab: String? = null) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                bottomNavItems.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = {
                            if (selected) {
                                Icon(screen.icon, contentDescription = screen.label)
                            } else {
                                Icon(
                                    screen.icon,
                                    contentDescription = screen.label,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        label = {
                            Text(
                                screen.label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startTab ?: Screen.Capture.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = androidx.compose.animation.core.tween(200)) },
            exitTransition = { fadeOut(animationSpec = androidx.compose.animation.core.tween(200)) }
        ) {
            composable(Screen.Capture.route) { CaptureScreen() }
            composable(Screen.Inbox.route) {
                InboxScreen(
                    onNavigateToCapture = {
                        navController.navigate(Screen.Capture.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    },
                    onNavigateToDetail = { entryId ->
                        navController.navigate("detail/$entryId")
                    }
                )
            }
            composable(Screen.Review.route) { ReviewScreen() }
            composable(Screen.Plan.route) { PlanScreen() }
            composable(Screen.Fitness.route) { FitnessScreen() }
            composable("settings") {
                SetupScreen(onComplete = { navController.popBackStack() })
            }
            composable("detail/{entryId}") { backStackEntry ->
                val entryId = backStackEntry.arguments?.getString("entryId") ?: ""
                DetailScreen(
                    entryId = entryId,
                    onBack = { navController.popBackStack() },
                    onDelete = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
