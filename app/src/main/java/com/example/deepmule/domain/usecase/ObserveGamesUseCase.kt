package com.example.deepmule.domain.usecase

import com.example.deepmule.domain.model.Game
import com.example.deepmule.domain.repository.GameRepository
import kotlinx.coroutines.flow.Flow

class ObserveGamesUseCase(
    private val repository: GameRepository
) {
    operator fun invoke(): Flow<List<Game>> = repository.observeGames()
}

