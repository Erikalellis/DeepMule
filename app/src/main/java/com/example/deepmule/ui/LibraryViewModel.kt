package com.example.deepmule.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.deepmule.data.GameEntity
import com.example.deepmule.data.GameRepository
import com.example.deepmule.data.SupportedSystems
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LibraryViewMode {
    Grid, List
}

class LibraryViewModel(private val repository: GameRepository) : ViewModel() {

    private val _viewMode = MutableStateFlow(LibraryViewMode.Grid)
    val viewMode: StateFlow<LibraryViewMode> = _viewMode.asStateFlow()

    private val _selectedSystem = MutableStateFlow<String?>(null)
    val selectedSystem: StateFlow<String?> = _selectedSystem.asStateFlow()

    val games: StateFlow<List<GameEntity>> = combine(
        repository.allGames,
        _selectedSystem
    ) { allGames, system ->
        if (system == null) allGames else allGames.filter { it.system == system }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == LibraryViewMode.Grid) LibraryViewMode.List else LibraryViewMode.Grid
    }

    fun selectSystem(system: String?) {
        _selectedSystem.value = system
    }

    fun scanDirectory(uri: Uri) {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                repository.scanDirectory(uri)
            } catch (e: Exception) {
                // Log or handle error
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun clearLibrary() {
        viewModelScope.launch {
            repository.clearLibrary()
        }
    }

    class Factory(private val repository: GameRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LibraryViewModel(repository) as T
        }
    }
}
