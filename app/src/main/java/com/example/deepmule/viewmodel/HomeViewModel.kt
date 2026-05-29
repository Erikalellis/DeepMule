package com.example.deepmule.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.deepmule.domain.model.Game
import com.example.deepmule.domain.usecase.MarkGameOpenedUseCase
import com.example.deepmule.domain.usecase.ObserveGamesUseCase
import com.example.deepmule.domain.usecase.ObserveRecentGamesUseCase
import com.example.deepmule.domain.usecase.SetFavoriteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val allGames: List<Game> = emptyList(),
    val recentGames: List<Game> = emptyList(),
    val systems: List<String> = emptyList(),
    val isLoading: Boolean = true
)

class HomeViewModel(
    observeGamesUseCase: ObserveGamesUseCase,
    observeRecentGamesUseCase: ObserveRecentGamesUseCase,
    private val setFavoriteUseCase: SetFavoriteUseCase,
    private val markGameOpenedUseCase: MarkGameOpenedUseCase
) : ViewModel() {

    private val _selectedSystem = MutableStateFlow<String?>(null)
    val selectedSystem: StateFlow<String?> = _selectedSystem.asStateFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        observeGamesUseCase(),
        observeRecentGamesUseCase(10)
    ) { games, recent ->
        HomeUiState(
            allGames = games,
            recentGames = recent,
            systems = games.map { it.system }.distinct().sorted(),
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun selectSystem(system: String?) {
        _selectedSystem.value = system
    }

    fun setFavorite(game: Game, favorite: Boolean) {
        viewModelScope.launch {
            setFavoriteUseCase(game.id, favorite)
        }
    }

    fun onGameOpened(game: Game) {
        viewModelScope.launch {
            markGameOpenedUseCase(game.id)
        }
    }

    class Factory(
        private val observeGamesUseCase: ObserveGamesUseCase,
        private val observeRecentGamesUseCase: ObserveRecentGamesUseCase,
        private val setFavoriteUseCase: SetFavoriteUseCase,
        private val markGameOpenedUseCase: MarkGameOpenedUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(
                observeGamesUseCase = observeGamesUseCase,
                observeRecentGamesUseCase = observeRecentGamesUseCase,
                setFavoriteUseCase = setFavoriteUseCase,
                markGameOpenedUseCase = markGameOpenedUseCase
            ) as T
        }
    }
}

