package com.example.deepmule.ui.navigation

import android.content.Context
import com.example.deepmule.AppContainer
import com.example.deepmule.data.DemoRomProvisioner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppInitializer {
    suspend fun initialize(context: Context, appContainer: AppContainer) {
        withContext(Dispatchers.IO) {
            DemoRomProvisioner.provision(context)
            appContainer.syncLibraryUseCase()
        }
    }
}

