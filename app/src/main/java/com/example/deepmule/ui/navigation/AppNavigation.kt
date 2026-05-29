package com.example.deepmule.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.example.deepmule.AppContainer
import com.example.deepmule.DeepMuleApplication

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val app = context.applicationContext as DeepMuleApplication

    AppRoot(
        appContainer = app.appContainer,
        onInitialize = {
            AppInitializer.initialize(context = app.applicationContext, appContainer = app.appContainer)
        }
    )
}

@Composable
private fun AppRoot(
    appContainer: AppContainer,
    onInitialize: suspend () -> Unit
) {
    LaunchedEffect(appContainer) {
        onInitialize()
    }

    AppNavScaffold(appContainer = appContainer)
}

