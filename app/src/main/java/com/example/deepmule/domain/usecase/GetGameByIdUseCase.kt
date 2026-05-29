package com.example.deepmule.domain.usecase

import com.example.deepmule.domain.model.Game
import com.example.deepmule.domain.repository.GameRepository

class GetGameByIdUseCase(
    private val repository: GameRepository
) {
    suspend operator fun invoke(id: String): Game? = repository.getGameById(id)
}

