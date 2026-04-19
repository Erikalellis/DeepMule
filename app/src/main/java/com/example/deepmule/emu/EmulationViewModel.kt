package com.example.deepmule.emu

import android.annotation.SuppressLint
import android.content.Context
import android.view.Surface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.deepmule.data.GameEntity
import com.example.deepmule.data.SupportedSystems
import com.example.deepmule.data.storage.SaveStorageManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

@SuppressLint("StaticFieldLeak")
class EmulationViewModel(
    private val context: Context
) : ViewModel() {

    private val saveStorageManager = SaveStorageManager(context)
    
    private val _currentGame = MutableStateFlow<GameEntity?>(null)
    val currentGame: StateFlow<GameEntity?> = _currentGame.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()
    
    private val _syncStatus = MutableStateFlow("Desconectado")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private var emulationJob: Job? = null
    private var hasSurface = false

    fun onSurfaceReady(surface: Surface) {
        hasSurface = LibretroDroid.attachSurface(surface)
        if (!hasSurface) {
            _syncStatus.value = "Falha ao iniciar renderizacao"
        }
    }

    fun onSurfaceDestroyed() {
        hasSurface = false
        LibretroDroid.detachSurface()
    }

    fun startGame(game: GameEntity) {
        _currentGame.value = game
        _syncStatus.value = "Preparando emulacao..."

        viewModelScope.launch {
            val system = SupportedSystems.getByName(game.system)
            if (system == null) {
                _syncStatus.value = "Sistema nao suportado"
                return@launch
            }

            BiosValidator.validate(context, system).getOrElse {
                _syncStatus.value = it.message ?: "Falha na validacao de BIOS"
                return@launch
            }

            val launchArtifacts = LaunchResolver.prepare(context, game, system).getOrElse {
                _syncStatus.value = it.message ?: "Falha ao preparar jogo"
                return@launch
            }

            if (!LibretroDroid.loadCore(launchArtifacts.corePath)) {
                _syncStatus.value = "Falha ao carregar core"
                return@launch
            }

            if (!LibretroDroid.loadGame(launchArtifacts.romPath)) {
                _syncStatus.value = "Falha ao carregar ROM"
                return@launch
            }

            if (!hasSurface) {
                _syncStatus.value = "Aguardando superficie de renderizacao"
            }

            // Carregar auto-save local se disponivel
            _syncStatus.value = "Carregando salvamento..."
            try {
                val provider = saveStorageManager.getProvider()
                val saveFile = File(context.filesDir, "current_save.state")
                provider.loadState(game.path, 0, saveFile)
                    .onSuccess {
                        val loaded = LibretroDroid.loadState(0, saveFile.absolutePath)
                        _syncStatus.value = if (loaded) "Salvo local restaurado" else "Sem save local"
                    }
                    .onFailure { _ ->
                        _syncStatus.value = "Erro ao carregar"
                    }
            } catch (e: Exception) {
                _syncStatus.value = "Desconectado"
            }
            startLoop()
        }
    }

    private fun startLoop() {
        emulationJob?.cancel()
        emulationJob = viewModelScope.launch {
            while (isActive) {
                if (!_isPaused.value) {
                    LibretroDroid.step()
                }
                delay(16) // ~60 FPS
            }
        }
    }

    fun togglePause() {
        _isPaused.value = !_isPaused.value
    }

    fun saveState(slot: Int) {
        _currentGame.value?.let { game ->
            viewModelScope.launch {
                _syncStatus.value = "Salvando..."
                try {
                    val saveFile = File(context.filesDir, "state_$slot.sav")
                    val wroteState = LibretroDroid.saveState(slot, saveFile.absolutePath)
                    if (!wroteState) {
                        _syncStatus.value = "Falha ao gerar save local"
                        return@launch
                    }
                    
                    // Sincronizar com provider configurado
                    val provider = saveStorageManager.getProvider()
                    provider.saveState(game.path, slot, saveFile)
                        .onSuccess {
                            _syncStatus.value = "Salvo com sucesso"
                        }
                        .onFailure { _ ->
                            _syncStatus.value = "Erro ao sincronizar"
                        }
                } catch (e: Exception) {
                    _syncStatus.value = "Erro: ${e.message}"
                }
            }
        }
    }

    fun loadState(slot: Int) {
        _currentGame.value?.let { game ->
            viewModelScope.launch {
                _syncStatus.value = "Carregando..."
                try {
                    val saveFile = File(context.filesDir, "state_$slot.sav")
                    val provider = saveStorageManager.getProvider()
                    provider.loadState(game.path, slot, saveFile)
                        .onSuccess {
                            val loaded = LibretroDroid.loadState(slot, saveFile.absolutePath)
                            _syncStatus.value = if (loaded) "Carregado" else "Save invalido"
                        }
                        .onFailure { _ ->
                            _syncStatus.value = "Erro ao carregar"
                        }
                } catch (e: Exception) {
                    _syncStatus.value = "Erro: ${e.message}"
                }
            }
        }
    }

    fun onExit() {
        // Auto-save com sincronização
        _currentGame.value?.let { game ->
            viewModelScope.launch {
                val saveFile = File(context.filesDir, "state_0.sav")
                try {
                    if (!LibretroDroid.saveState(0, saveFile.absolutePath)) {
                        return@launch
                    }
                    val provider = saveStorageManager.getProvider()
                    provider.saveState(game.path, 0, saveFile).getOrNull()
                } catch (e: Exception) {
                    // Falha silenciosa - já foi salvo localmente
                }
            }
        }
        emulationJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        onExit()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EmulationViewModel(context.applicationContext) as T
        }
    }
}
