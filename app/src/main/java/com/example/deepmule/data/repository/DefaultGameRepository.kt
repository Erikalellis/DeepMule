package com.example.deepmule.data.repository

import com.example.deepmule.data.GameDao
import com.example.deepmule.data.mapper.toDomain
import com.example.deepmule.domain.model.Game
import com.example.deepmule.domain.repository.GameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultGameRepository(
    private val gameDao: GameDao
) : GameRepository {

    override fun observeGames(): Flow<List<Game>> =
        gameDao.getAllGames().map { list -> list.map { it.toDomain() } }

    override fun observeRecentGames(limit: Int): Flow<List<Game>> =
        gameDao.getRecentGames(limit).map { list -> list.map { it.toDomain() } }

    override suspend fun getGameById(id: String): Game? =
        gameDao.getGameByPath(id)?.toDomain()

    override suspend fun setFavorite(id: String, favorite: Boolean) {
        gameDao.setFavorite(id, favorite)
    }

    override suspend fun markGameOpened(id: String) {
        gameDao.updateLastPlayed(id, System.currentTimeMillis())
    }
}

