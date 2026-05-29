package com.example.deepmule.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.deepmule.domain.model.Game
import com.example.deepmule.domain.usecase.GetGameByIdUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val gameId: String,
    private val getGameByIdUseCase: GetGameByIdUseCase
) : ViewModel() {

    private val _game = MutableStateFlow<Game?>(null)
    val game: StateFlow<Game?> = _game.asStateFlow()

    init {
        viewModelScope.launch {
            _game.value = getGameByIdUseCase(gameId)
        }
    }

    class Factory(
        private val gameId: String,
        private val getGameByIdUseCase: GetGameByIdUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlayerViewModel(gameId, getGameByIdUseCase) as T
        }
    }
}

