package com.example.deepmule.domain.repository

import com.example.deepmule.domain.model.Game
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    fun observeGames(): Flow<List<Game>>
    fun observeRecentGames(limit: Int = 10): Flow<List<Game>>
    suspend fun getGameById(id: String): Game?
    suspend fun setFavorite(id: String, favorite: Boolean)
    suspend fun markGameOpened(id: String)
}

