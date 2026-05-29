package com.example.deepmule.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.deepmule.AppContainer

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun AppNavScaffold(appContainer: AppContainer) {
    val navController = rememberNavController()
    val bottomDestinations = listOf(
        BottomDestination(Screen.Home.route, "Inicio", Icons.Rounded.Home),
        BottomDestination(Screen.Library.route, "Biblioteca", Icons.Rounded.Folder),
        BottomDestination(Screen.Settings.route, "Configuracoes", Icons.Rounded.Settings)
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val showBottomBar = bottomDestinations.any { destination ->
                currentDestination?.hierarchy?.any { it.route == destination.route } == true
            }
            if (showBottomBar) {
                NavigationBar {
                    bottomDestinations.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        AppNavGraph(
            navController = navController,
            appContainer = appContainer,
            modifier = Modifier.padding(padding)
        )
    }
}

