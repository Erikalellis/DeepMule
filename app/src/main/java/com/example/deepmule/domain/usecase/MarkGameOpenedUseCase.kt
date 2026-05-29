package com.example.deepmule.domain.usecase

import com.example.deepmule.domain.repository.GameRepository

class MarkGameOpenedUseCase(
    private val repository: GameRepository
) {
    suspend operator fun invoke(id: String) {
        repository.markGameOpened(id)
    }
}

