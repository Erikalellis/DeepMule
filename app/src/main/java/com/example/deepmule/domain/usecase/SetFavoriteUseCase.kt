package com.example.deepmule.domain.usecase

import com.example.deepmule.domain.repository.GameRepository

class SetFavoriteUseCase(
    private val repository: GameRepository
) {
    suspend operator fun invoke(id: String, favorite: Boolean) {
        repository.setFavorite(id, favorite)
    }
}

