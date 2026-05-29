package com.example.deepmule.emu

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.view.KeyEvent
import android.view.Surface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.deepmule.data.GameEntity
import com.example.deepmule.data.SupportedSystems
import com.example.deepmule.data.SystemConfig
import com.example.deepmule.data.SystemPresetStore
import com.example.deepmule.data.storage.SaveStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.coroutineContext


@SuppressLint("StaticFieldLeak")
class EmulationViewModel(
    private val context: Context
) : ViewModel() {

    enum class VirtualButton(val id: Int) {
        B(0), Y(1), SELECT(2), START(3), UP(4), DOWN(5), LEFT(6), RIGHT(7), A(8), X(9), L(10), R(11)
    }

    private val saveStorageManager = SaveStorageManager(context)
    private val systemPresetStore = SystemPresetStore(context)

    private val _currentGame = MutableStateFlow<GameEntity?>(null)
    val currentGame: StateFlow<GameEntity?> = _currentGame.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _syncStatus = MutableStateFlow("Desconectado")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private var emulationJob: Job? = null
    private var audioJob: Job? = null
    private var audioTrack: AudioTrack? = null
    private var hasSurface = false

    // ─── Audio ───────────────────────────────────────────────────────────────

    private fun createAudioTrack(): AudioTrack {
        val sampleRate = 44100
        val minBufSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(minBufSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    private fun startAudioDrain() {
        audioJob?.cancel()
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = createAudioTrack().also { it.play() }

        audioJob = viewModelScope.launch(Dispatchers.IO) {
            val buffer = ShortArray(2048)
            while (isActive) {
                if (!_isPaused.value) {
                    val n = LibretroDroid.drainAudio(buffer, buffer.size)
                    if (n > 0) {
                        audioTrack?.write(buffer, 0, n)
                    } else {
                        delay(4)
                    }
                } else {
                    delay(16)
                }
            }
        }
    }

    private fun stopAudio() {
        audioJob?.cancel()
        audioJob = null
        runCatching {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        }
        audioTrack = null
    }

    // ─── Surface lifecycle ────────────────────────────────────────────────────

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

    fun startGame(game: GameEntity, onLaunchResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        _currentGame.value = game
        _syncStatus.value = "Preparando emulacao..."

        viewModelScope.launch {
            val system = SupportedSystems.getByName(game.system)
            if (system == null) {
                _syncStatus.value = "Sistema nao suportado"
                onLaunchResult(false, _syncStatus.value)
                return@launch
            }

            val preset = systemPresetStore.getPreset(system.id)
            val effectiveSystem = applyCoreOverride(system, preset.coreOverride)

            _syncStatus.value = "Verificando core..."
            var coreFile = CoreProvisioning.resolveCoreFile(context, effectiveSystem)
            
            if (coreFile == null) {
                val coresToTry = (listOf(effectiveSystem.coreName) + effectiveSystem.coreAlternatives).distinct()
                for (name in coresToTry) {
                    val result = CoreDownloader.downloadCoreIfNeeded(context, name) { _syncStatus.value = it }
                    if (result.isSuccess) {
                        coreFile = result.getOrNull()
                        break
                    }
                }
            }

            if (coreFile == null) {
                _syncStatus.value = "Falha ao obter core para ${effectiveSystem.name}"
                onLaunchResult(false, _syncStatus.value)
                return@launch
            }

            BiosValidator.validate(context, effectiveSystem).getOrElse {
                _syncStatus.value = it.message ?: "Falha na validacao de BIOS"
                onLaunchResult(false, _syncStatus.value)
                return@launch
            }

            val launchArtifacts = LaunchResolver.prepare(context, game, effectiveSystem) {
                _syncStatus.value = it
            }.getOrElse {
                _syncStatus.value = it.message ?: "Falha ao preparar jogo"
                onLaunchResult(false, _syncStatus.value)
                return@launch
            }

            _syncStatus.value = "Configurando caminhos..."
            val biosDir = File(context.filesDir, "bios")
            if (!biosDir.exists()) biosDir.mkdirs()
            val savesDir = File(context.filesDir, "saves")
            if (!savesDir.exists()) savesDir.mkdirs()
            LibretroDroid.setDirectories(biosDir.absolutePath, savesDir.absolutePath)

            if (!LibretroDroid.loadCore(coreFile.absolutePath)) {
                _syncStatus.value = "Falha ao carregar core: ${coreFile.name}"
                onLaunchResult(false, _syncStatus.value)
                return@launch
            }

            if (!LibretroDroid.loadGame(launchArtifacts.romPath)) {
                _syncStatus.value = "Falha ao carregar ROM"
                onLaunchResult(false, _syncStatus.value)
                return@launch
            }

            onLaunchResult(true, null)

            val presetSummary = buildString {
                append("Preset aplicado")
                if (preset.coreOverride != null) append(" | core=${preset.coreOverride}")
                if (!preset.audioEnabled) append(" | audio=off")
                if (preset.frameSkip > 0) append(" | frameSkip=${preset.frameSkip}")
                if (preset.enableRewind) append(" | rewind=on")
            }
            _syncStatus.value = presetSummary

            if (!hasSurface) {
                _syncStatus.value = "Aguardando superficie de renderizacao"
            }

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
            startAudioDrain()
        }
    }

    private fun applyCoreOverride(system: SystemConfig, overrideCore: String?): SystemConfig {
        if (overrideCore.isNullOrBlank()) return system
        val alternatives = (listOf(system.coreName) + system.coreAlternatives)
            .filterNot { it == overrideCore }
            .distinct()
        return system.copy(coreName = overrideCore, coreAlternatives = alternatives)
    }

    private fun startLoop() {
        emulationJob?.cancel()
        emulationJob = viewModelScope.launch(Dispatchers.Default) {
            while (coroutineContext[Job]?.isActive == true) {
                if (!_isPaused.value) {
                    LibretroDroid.step()
                }
                delay(16) // ~60 FPS
            }
        }
    }

    fun togglePause() {
        _isPaused.value = !_isPaused.value
        if (_isPaused.value) {
            audioTrack?.pause()
        } else {
            audioTrack?.play()
        }
    }

    fun setVirtualButton(button: VirtualButton, pressed: Boolean) {
        LibretroDroid.setInputState(button.id, pressed)
    }

    fun onGamepadKeyEvent(keyCode: Int, pressed: Boolean): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_BUTTON_MODE || keyCode == KeyEvent.KEYCODE_ESCAPE) {
            if (pressed) togglePause()
            return true
        }

        val button = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> VirtualButton.UP
            KeyEvent.KEYCODE_DPAD_DOWN -> VirtualButton.DOWN
            KeyEvent.KEYCODE_DPAD_LEFT -> VirtualButton.LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> VirtualButton.RIGHT
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_2 -> VirtualButton.A
            KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_1 -> VirtualButton.B
            KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_4 -> VirtualButton.X
            KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_BUTTON_3 -> VirtualButton.Y
            KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_L2 -> VirtualButton.L
            KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_BUTTON_R2 -> VirtualButton.R
            KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> VirtualButton.START
            KeyEvent.KEYCODE_BUTTON_SELECT -> VirtualButton.SELECT
            else -> null
        }
        if (button == null) return false
        setVirtualButton(button, pressed)
        return true
    }

    fun saveState(slot: Int) {
        _currentGame.value?.let { game ->
            viewModelScope.launch {
                _syncStatus.value = "Salvando slot $slot…"
                try {
                    val saveFile = File(context.filesDir, "state_$slot.sav")
                    val wroteState = LibretroDroid.saveState(slot, saveFile.absolutePath)
                    if (!wroteState) {
                        _syncStatus.value = "Falha ao gerar save local"
                        return@launch
                    }
                    val provider = saveStorageManager.getProvider()
                    provider.saveState(game.path, slot, saveFile)
                        .onSuccess { _syncStatus.value = "Slot $slot salvo" }
                        .onFailure { _ -> _syncStatus.value = "Erro ao sincronizar" }
                } catch (e: Exception) {
                    _syncStatus.value = "Erro: ${e.message}"
                }
            }
        }
    }

    fun loadState(slot: Int) {
        _currentGame.value?.let { game ->
            viewModelScope.launch {
                _syncStatus.value = "Carregando slot $slot…"
                try {
                    val saveFile = File(context.filesDir, "state_$slot.sav")
                    val provider = saveStorageManager.getProvider()
                    provider.loadState(game.path, slot, saveFile)
                        .onSuccess {
                            val loaded = LibretroDroid.loadState(slot, saveFile.absolutePath)
                            _syncStatus.value = if (loaded) "Slot $slot carregado" else "Save invalido"
                        }
                        .onFailure { _ -> _syncStatus.value = "Erro ao carregar" }
                } catch (e: Exception) {
                    _syncStatus.value = "Erro: ${e.message}"
                }
            }
        }
    }

    fun onExit() {
        _currentGame.value?.let { game ->
            viewModelScope.launch {
                val saveFile = File(context.filesDir, "state_0.sav")
                try {
                    if (!LibretroDroid.saveState(0, saveFile.absolutePath)) return@launch
                    val provider = saveStorageManager.getProvider()
                    provider.saveState(game.path, 0, saveFile).getOrNull()
                } catch (e: Exception) {
                    // Falha silenciosa
                }
            }
        }
        emulationJob?.cancel()
        stopAudio()
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
