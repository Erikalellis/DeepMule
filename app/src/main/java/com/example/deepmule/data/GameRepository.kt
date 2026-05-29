package com.example.deepmule.data

import android.net.Uri
import kotlinx.coroutines.flow.Flow

class GameRepository(
    private val gameDao: GameDao,
    private val gameScanner: GameScanner
) {
    val allGames: Flow<List<GameEntity>> = gameDao.getAllGames()

    fun searchGames(query: String): Flow<List<GameEntity>> = gameDao.searchGames(query)

    val favoriteGames: Flow<List<GameEntity>> = gameDao.getFavoriteGames()

    val recentGames: Flow<List<GameEntity>> = gameDao.getRecentGames(10)

    suspend fun scanDirectory(uri: Uri) {
        val games = gameScanner
            .scanDirectory(uri)
            .distinctBy { it.path.lowercase() }
        gameDao.insertGames(games)
    }

    /** Scan incremental: apenas arquivos ainda nao indexados sao inseridos. */
    suspend fun scanDirectoryIncremental(uri: Uri): ImportSyncReport =
        scanAllSourcesIncremental(listOf(uri.toString()))

    /** Scan incremental em multiplas fontes com relatorio final. */
    suspend fun scanAllSourcesIncremental(uris: List<String>): ImportSyncReport {
        if (uris.isEmpty()) return ImportSyncReport.Empty

        val existingPaths = gameDao.getAllPaths().map { it.lowercase() }.toHashSet()
        val existingGames = gameDao.getAllGamesSnapshot()
        val titleSystemIndex = existingGames.associateBy(
            keySelector = { keyFor(it.title, it.system) },
            valueTransform = { it.path.lowercase() }
        ).toMutableMap()

        var imported = 0
        var ignored = 0
        val conflicts = mutableListOf<ImportSyncConflict>()
        val errors = mutableListOf<String>()
        val toInsert = mutableListOf<GameEntity>()

        uris.forEach { uriStr ->
            try {
                val uri = Uri.parse(uriStr)
                val found = gameScanner.scanDirectory(uri)
                    .distinctBy { it.path.lowercase() }

                found.forEach { game ->
                    val normalizedPath = game.path.lowercase()
                    val indexKey = keyFor(game.title, game.system)
                    val existingPathForTitle = titleSystemIndex[indexKey]

                    if (normalizedPath in existingPaths) {
                        ignored++
                        return@forEach
                    }

                    if (existingPathForTitle != null && existingPathForTitle != normalizedPath) {
                        conflicts += ImportSyncConflict(
                            system = game.system,
                            title = game.title,
                            incomingPath = game.path,
                            existingPath = existingPathForTitle
                        )
                        ignored++
                        return@forEach
                    }

                    toInsert += game
                    existingPaths += normalizedPath
                    titleSystemIndex[indexKey] = normalizedPath
                    imported++
                }
            } catch (e: Exception) {
                errors += "${uriStr}: ${e.message ?: "falha desconhecida"}"
            }
        }

        if (toInsert.isNotEmpty()) {
            gameDao.insertGames(toInsert)
        }

        return ImportSyncReport(
            sourcesProcessed = uris.size,
            imported = imported,
            ignored = ignored,
            conflicts = conflicts,
            errors = errors
        )
    }

    suspend fun clearLibrary() {
        gameDao.deleteAll()
    }

    suspend fun setFavorite(path: String, favorite: Boolean) {
        gameDao.setFavorite(path, favorite)
    }

    suspend fun updateLastPlayed(path: String) {
        gameDao.updateLastPlayed(path, System.currentTimeMillis())
    }

    private fun keyFor(title: String, system: String): String =
        "${system.lowercase()}::${title.trim().lowercase()}"
}
