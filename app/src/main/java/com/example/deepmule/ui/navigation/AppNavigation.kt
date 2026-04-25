package com.example.deepmule.ui.navigation

import android.net.Uri
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.deepmule.AppContainer
import com.example.deepmule.domain.model.Game
import com.example.deepmule.ui.screens.details.DetailsScreen
import com.example.deepmule.ui.screens.home.HomeScreen
import com.example.deepmule.ui.screens.library.LibraryScreen
import com.example.deepmule.ui.screens.player.PlayerScreen
import com.example.deepmule.ui.screens.settings.SettingsScreen
import com.example.deepmule.viewmodel.DetailsViewModel
import com.example.deepmule.viewmodel.HomeViewModel
import com.example.deepmule.viewmodel.LibraryScreenViewModel
import com.example.deepmule.viewmodel.PlayerViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun AppNavigation(
    appContainer: AppContainer,
    onLaunchGame: (Game) -> Unit
) {
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
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Home.route) {
                val viewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.Factory(
                        observeGamesUseCase = appContainer.observeGamesUseCase,
                        observeRecentGamesUseCase = appContainer.observeRecentGamesUseCase,
                        setFavoriteUseCase = appContainer.setFavoriteUseCase,
                        markGameOpenedUseCase = appContainer.markGameOpenedUseCase
                    )
                )
                HomeScreen(
                    viewModel = viewModel,
                    onGameSelected = { game ->
                        navController.navigate(Screen.Details.createRoute(Uri.encode(game.id)))
                    }
                )
            }

            composable(Screen.Library.route) {
                val viewModel: LibraryScreenViewModel = viewModel(
                    factory = LibraryScreenViewModel.Factory(
                        observeGamesUseCase = appContainer.observeGamesUseCase,
                        setFavoriteUseCase = appContainer.setFavoriteUseCase,
                        markGameOpenedUseCase = appContainer.markGameOpenedUseCase
                    )
                )
                LibraryScreen(
                    viewModel = viewModel,
                    onGameSelected = { game ->
                        navController.navigate(Screen.Details.createRoute(Uri.encode(game.id)))
                    }
                )
            }

            composable(
                route = Screen.Details.route,
                arguments = listOf(navArgument("gameId") { type = NavType.StringType })
            ) { entry ->
                val encodedId = entry.arguments?.getString("gameId").orEmpty()
                val gameId = Uri.decode(encodedId)
                val viewModel: DetailsViewModel = viewModel(
                    key = "details-$encodedId",
                    factory = DetailsViewModel.Factory(
                        gameId = gameId,
                        getGameByIdUseCase = appContainer.getGameByIdUseCase,
                        setFavoriteUseCase = appContainer.setFavoriteUseCase
                    )
                )
                DetailsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onPlay = { game ->
                        navController.navigate(Screen.Player.createRoute(Uri.encode(game.id)))
                    }
                )
            }

            composable(
                route = Screen.Player.route,
                arguments = listOf(navArgument("gameId") { type = NavType.StringType })
            ) { entry ->
                val encodedId = entry.arguments?.getString("gameId").orEmpty()
                val gameId = Uri.decode(encodedId)
                val viewModel: PlayerViewModel = viewModel(
                    key = "player-$encodedId",
                    factory = PlayerViewModel.Factory(
                        gameId = gameId,
                        getGameByIdUseCase = appContainer.getGameByIdUseCase
                    )
                )
                val game by viewModel.game.collectAsStateWithLifecycle()
                PlayerScreen(
                    game = game,
                    onLaunchEmulation = onLaunchGame
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}

