package com.example.deepmule.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Library : Screen("library")
    data object Settings : Screen("settings")
    data object Details : Screen("details/{gameId}") {
        fun createRoute(gameId: String): String = "details/$gameId"
    }
    data object Player : Screen("player/{gameId}") {
        fun createRoute(gameId: String): String = "player/$gameId"
    }
}

