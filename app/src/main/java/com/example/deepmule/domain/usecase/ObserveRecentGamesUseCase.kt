package com.example.deepmule.domain.usecase

import com.example.deepmule.domain.model.Game
import com.example.deepmule.domain.repository.GameRepository
import kotlinx.coroutines.flow.Flow

class ObserveRecentGamesUseCase(
    private val repository: GameRepository
) {
    operator fun invoke(limit: Int = 10): Flow<List<Game>> = repository.observeRecentGames(limit)
}

