package com.example.deepmule.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.deepmule.domain.model.Game
import com.example.deepmule.domain.usecase.MarkGameOpenedUseCase
import com.example.deepmule.domain.usecase.ObserveGamesUseCase
import com.example.deepmule.domain.usecase.SetFavoriteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val games: List<Game> = emptyList(),
    val systems: List<String> = emptyList(),
    val searchQuery: String = "",
    val selectedSystem: String? = null,
    val favoritesOnly: Boolean = false,
    val isLoading: Boolean = true
)

class LibraryScreenViewModel(
    observeGamesUseCase: ObserveGamesUseCase,
    private val setFavoriteUseCase: SetFavoriteUseCase,
    private val markGameOpenedUseCase: MarkGameOpenedUseCase
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedSystem = MutableStateFlow<String?>(null)
    private val favoritesOnly = MutableStateFlow(false)

    val uiState: StateFlow<LibraryUiState> = combine(
        observeGamesUseCase(),
        searchQuery,
        selectedSystem,
        favoritesOnly
    ) { games, query, system, favOnly ->
        val filtered = games
            .asSequence()
            .filter { query.isBlank() || it.title.contains(query, ignoreCase = true) }
            .filter { system == null || it.system.equals(system, ignoreCase = true) }
            .filter { !favOnly || it.favorite }
            .toList()

        LibraryUiState(
            games = filtered,
            systems = games.map { it.system }.distinct().sorted(),
            searchQuery = query,
            selectedSystem = system,
            favoritesOnly = favOnly,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryUiState())

    fun setSearchQuery(value: String) {
        searchQuery.value = value
    }

    fun selectSystem(system: String?) {
        selectedSystem.value = system
    }

    fun toggleFavoritesOnly() {
        favoritesOnly.value = !favoritesOnly.value
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
        private val setFavoriteUseCase: SetFavoriteUseCase,
        private val markGameOpenedUseCase: MarkGameOpenedUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LibraryScreenViewModel(
                observeGamesUseCase = observeGamesUseCase,
                setFavoriteUseCase = setFavoriteUseCase,
                markGameOpenedUseCase = markGameOpenedUseCase
            ) as T
        }
    }
}

