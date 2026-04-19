package com.example.deepmule.data

import android.net.Uri
import kotlinx.coroutines.flow.Flow

class GameRepository(
    private val gameDao: GameDao,
    private val gameScanner: GameScanner
) {
    val allGames: Flow<List<GameEntity>> = gameDao.getAllGames()

    suspend fun scanDirectory(uri: Uri) {
        val games = gameScanner.scanDirectory(uri)
        gameDao.insertGames(games)
    }

    suspend fun clearLibrary() {
        gameDao.deleteAll()
    }
}
