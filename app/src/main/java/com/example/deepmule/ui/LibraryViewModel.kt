package com.example.deepmule.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.deepmule.data.GameEntity
import com.example.deepmule.data.GameRepository
import com.example.deepmule.data.ImportSyncReport
import com.example.deepmule.data.RomSourceManager
import com.example.deepmule.data.SystemConfig
import com.example.deepmule.data.SystemPreset
import com.example.deepmule.data.SystemPresetStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LibraryViewMode {
    Grid, List
}

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    private val repository: GameRepository,
    private val romSourceManager: RomSourceManager,
    private val systemPresetStore: SystemPresetStore
) : ViewModel() {

    private val _viewMode = MutableStateFlow(LibraryViewMode.Grid)
    val viewMode: StateFlow<LibraryViewMode> = _viewMode.asStateFlow()

    private val _selectedSystem = MutableStateFlow<String?>(null)
    val selectedSystem: StateFlow<String?> = _selectedSystem.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showSearch = MutableStateFlow(false)
    val showSearch: StateFlow<Boolean> = _showSearch.asStateFlow()

    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly.asStateFlow()

    val games: StateFlow<List<GameEntity>> = combine(
        _searchQuery.flatMapLatest { query ->
            if (query.isBlank()) repository.allGames else repository.searchGames(query)
        },
        _selectedSystem,
        _showFavoritesOnly
    ) { allGames, system, favOnly ->
        var result = if (system == null) allGames else allGames.filter { it.system == system }
        if (favOnly) result = result.filter { it.favorite }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentGames: StateFlow<List<GameEntity>> = repository.recentGames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _lastImportReport = MutableStateFlow(ImportSyncReport.Empty)
    val lastImportReport: StateFlow<ImportSyncReport> = _lastImportReport.asStateFlow()

    private val _lastScanError = MutableStateFlow<String?>(null)
    val lastScanError: StateFlow<String?> = _lastScanError.asStateFlow()

    // ── ROM Sources ──────────────────────────────────────────────────────────

    private val _romSources = MutableStateFlow(romSourceManager.getSources())
    val romSources: StateFlow<List<String>> = _romSources.asStateFlow()

    private val _publicRomPath = MutableStateFlow(romSourceManager.getPublicRomPath())
    val publicRomPath: StateFlow<String?> = _publicRomPath.asStateFlow()

    init {
        romSourceManager.ensureDefaultSource()
        _romSources.value = romSourceManager.getSources()
        if (_romSources.value.isNotEmpty()) {
            rescanAllSources()
        }
    }

    fun addRomSource(uri: Uri) {
        val uriStr = uri.toString()
        romSourceManager.addSource(uriStr)
        _romSources.value = romSourceManager.getSources()
        // Scan incremental apenas para a nova fonte
        viewModelScope.launch {
            _isScanning.value = true
            _lastScanError.value = null
            try {
                _lastImportReport.value = repository.scanDirectoryIncremental(uri)
            } catch (e: Exception) {
                _lastScanError.value = e.message ?: "Falha ao escanear fonte"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun removeRomSource(uri: String) {
        romSourceManager.removeSource(uri)
        _romSources.value = romSourceManager.getSources()
    }

    /** Verifica se as permissões das fontes ainda são válidas. */
    fun validateRomSources(contentResolver: android.content.ContentResolver) {
        val currentSources = _romSources.value
        val invalidSources = mutableListOf<String>()
        
        currentSources.forEach { uriStr ->
            val uri = Uri.parse(uriStr)
            if (uri.scheme == "content") {
                val hasPermission = contentResolver.persistedUriPermissions.any { 
                    it.uri.toString() == uriStr && it.isReadPermission 
                }
                if (!hasPermission) {
                    invalidSources.add(uriStr)
                }
            }
        }
        
        if (invalidSources.isNotEmpty()) {
            _lastScanError.value = "Algumas fontes perderam acesso. Por favor, re-adicione-as."
        }
    }

    fun rescanAllSources() {
        android.util.Log.d("DeepMule", "Iniciando scan de todas as fontes...")
        viewModelScope.launch {
            _isScanning.value = true
            _lastScanError.value = null
            try {
                val report = repository.scanAllSourcesIncremental(_romSources.value)
                _lastImportReport.value = report
                android.util.Log.d("DeepMule", "Scan finalizado: ${report.imported} novos jogos encontrados.")
            } catch (e: Exception) {
                _lastScanError.value = e.message ?: "Falha ao re-escanear fontes"
                android.util.Log.e("DeepMule", "Erro no scan: ${e.message}", e)
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun clearScanFeedback() {
        _lastScanError.value = null
        _lastImportReport.value = ImportSyncReport.Empty
    }

    // ── System Presets ───────────────────────────────────────────────────────

    private val _editingPreset = MutableStateFlow<Pair<SystemConfig, SystemPreset>?>(null)
    val editingPreset: StateFlow<Pair<SystemConfig, SystemPreset>?> = _editingPreset.asStateFlow()

    fun openPresetEditor(system: SystemConfig) {
        _editingPreset.value = system to systemPresetStore.getPreset(system.id)
    }

    fun closePresetEditor() {
        _editingPreset.value = null
    }

    fun savePreset(preset: SystemPreset) {
        systemPresetStore.savePreset(preset)
        closePresetEditor()
    }

    fun resetPreset(systemId: String) {
        systemPresetStore.resetPreset(systemId)
        closePresetEditor()
    }

    fun getPreset(systemId: String): SystemPreset = systemPresetStore.getPreset(systemId)

    // ── Existing actions ─────────────────────────────────────────────────────

    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == LibraryViewMode.Grid) LibraryViewMode.List else LibraryViewMode.Grid
    }

    fun selectSystem(system: String?) {
        _selectedSystem.value = system
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearch() {
        _showSearch.value = !_showSearch.value
        if (!_showSearch.value) _searchQuery.value = ""
    }

    fun toggleFavoritesOnly() {
        _showFavoritesOnly.value = !_showFavoritesOnly.value
    }

    /** Scan completo (substitui/atualiza todos os arquivos encontrados). */
    private fun scanDirectory(uri: Uri) {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                repository.scanDirectory(uri)
            } catch (e: Exception) {
                _lastScanError.value = e.message
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun rescanSource(uri: Uri) {
        scanDirectory(uri)
    }


    fun clearLibrary() {
        viewModelScope.launch {
            repository.clearLibrary()
        }
    }

    /** Limpa o cache de cores e thumbnails para liberar espaço. */
    fun clearCache(context: android.content.Context) {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                // Limpar Cores
                val coresDir = java.io.File(context.filesDir, "cores")
                if (coresDir.exists()) coresDir.deleteRecursively()
                
                // Limpar Thumbnails
                val thumbDir = java.io.File(context.filesDir, "retroarch/thumbnails")
                if (thumbDir.exists()) thumbDir.deleteRecursively()
                
                // Limpar Staging de ROMs
                val stagingDir = java.io.File(context.cacheDir, "rom_staging")
                if (stagingDir.exists()) stagingDir.deleteRecursively()
                
                _lastScanError.value = "Cache limpo com sucesso!"
            } catch (e: Exception) {
                _lastScanError.value = "Falha ao limpar cache: ${e.message}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun setFavorite(game: GameEntity, favorite: Boolean) {
        viewModelScope.launch {
            repository.setFavorite(game.path, favorite)
        }
    }

    fun onGameOpened(game: GameEntity) {
        viewModelScope.launch {
            repository.updateLastPlayed(game.path)
        }
    }

    class Factory(
        private val repository: GameRepository,
        private val romSourceManager: RomSourceManager,
        private val systemPresetStore: SystemPresetStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LibraryViewModel(repository, romSourceManager, systemPresetStore) as T
        }
    }
}
