package com.example.deepmule.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.deepmule.domain.model.Game
import com.example.deepmule.domain.usecase.GetGameByIdUseCase
import com.example.deepmule.domain.usecase.SetFavoriteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetailsUiState(
    val game: Game? = null,
    val isLoading: Boolean = true
)

class DetailsViewModel(
    private val gameId: String,
    private val getGameByIdUseCase: GetGameByIdUseCase,
    private val setFavoriteUseCase: SetFavoriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    init {
        loadGame()
    }

    private fun loadGame() {
        viewModelScope.launch {
            _uiState.value = DetailsUiState(game = getGameByIdUseCase(gameId), isLoading = false)
        }
    }

    fun toggleFavorite() {
        val current = _uiState.value.game ?: return
        viewModelScope.launch {
            val next = !current.favorite
            setFavoriteUseCase(current.id, next)
            _uiState.value = _uiState.value.copy(game = current.copy(favorite = next))
        }
    }

    class Factory(
        private val gameId: String,
        private val getGameByIdUseCase: GetGameByIdUseCase,
        private val setFavoriteUseCase: SetFavoriteUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DetailsViewModel(
                gameId = gameId,
                getGameByIdUseCase = getGameByIdUseCase,
                setFavoriteUseCase = setFavoriteUseCase
            ) as T
        }
    }
}

