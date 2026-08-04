package com.thoughtcapture.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.thoughtcapture.app.ui.capture.CaptureScreen
import com.thoughtcapture.app.ui.inbox.InboxScreen
import com.thoughtcapture.app.ui.plan.PlanScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Capture : Screen("capture", "捕捉", Icons.Default.Edit)
    data object Inbox : Screen("inbox", "收件箱", Icons.Default.Inbox)
    data object Plan : Screen("plan", "计划", Icons.Default.CalendarToday)
}

val bottomNavItems = listOf(Screen.Capture, Screen.Inbox, Screen.Plan)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(startTab: String? = null) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startTab ?: Screen.Capture.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Capture.route) { CaptureScreen() }
            composable(Screen.Inbox.route) { InboxScreen() }
            composable(Screen.Plan.route) { PlanScreen() }
        }
    }
}
