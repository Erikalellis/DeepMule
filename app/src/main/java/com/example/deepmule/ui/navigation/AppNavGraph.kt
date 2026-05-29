package com.example.deepmule.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.deepmule.AppContainer
import com.example.deepmule.ui.screens.details.DetailsScreen
import com.example.deepmule.ui.screens.home.HomeScreen
import com.example.deepmule.ui.screens.library.LibraryScreen
import com.example.deepmule.ui.screens.player.PlayerLauncher
import com.example.deepmule.ui.screens.settings.SettingsScreen
import com.example.deepmule.viewmodel.DetailsViewModel
import com.example.deepmule.viewmodel.HomeViewModel
import com.example.deepmule.viewmodel.LibraryScreenViewModel
import com.example.deepmule.viewmodel.PlayerViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    appContainer: AppContainer,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
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
            PlayerLauncher(game = game)
        }

        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}

