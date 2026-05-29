package com.example.deepmule.ui.screens.player

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.deepmule.data.SupportedSystems
import com.example.deepmule.domain.model.Game
import com.example.deepmule.emu.BiosValidator
import com.example.deepmule.emu.CoreManager
import com.example.deepmule.emu.EmulationActivity
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun PlayerLauncher(game: Game?) {
    val context = LocalContext.current
    val coreManager = remember(context) { CoreManager(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var isPreparing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var launchError by remember { mutableStateOf<String?>(null) }

    PlayerScreen(
        game = game,
        onLaunchEmulation = { selectedGame ->
            scope.launch {
                isPreparing = true
                launchError = null

                statusMessage = "Validando ROM..."
                val romValidation = validateLocalRomPath(selectedGame.romPath)
                if (romValidation.isFailure) {
                    launchError = romValidation.exceptionOrNull()?.message ?: "ROM invalida"
                    statusMessage = null
                    isPreparing = false
                    return@launch
                }

                val system = SupportedSystems.getByName(selectedGame.system)
                    ?: SupportedSystems.getById(selectedGame.system)
                if (system == null) {
                    launchError = "Sistema nao suportado: ${selectedGame.system}"
                    statusMessage = null
                    isPreparing = false
                    return@launch
                }

                statusMessage = "Validando BIOS..."
                val biosValidation = BiosValidator.validate(context, system)
                if (biosValidation.isFailure) {
                    launchError = biosValidation.exceptionOrNull()?.message ?: "BIOS ausente"
                    statusMessage = null
                    isPreparing = false
                    return@launch
                }

                statusMessage = "Validando core..."
                val coreReady = if (coreManager.checkCoreInstalled(system.id)) {
                    statusMessage = "Core ja instalado"
                    true
                } else {
                    coreManager.downloadCore(system.id) { message -> statusMessage = message }.isSuccess
                }

                if (!coreReady) {
                    launchError = "Nao foi possivel preparar o core para ${system.name}."
                    statusMessage = null
                    isPreparing = false
                    return@launch
                }

                statusMessage = "Core pronto. Iniciando emulacao..."
                val intent = Intent(context, EmulationActivity::class.java).apply {
                    putExtra("GAME_PATH", selectedGame.romPath)
                    putExtra("SYSTEM_ID", selectedGame.system)
                }
                context.startActivity(intent)
                isPreparing = false
            }
        },
        isPreparingCore = isPreparing,
        coreStatusMessage = statusMessage,
        launchError = launchError
    )
}

private fun validateLocalRomPath(path: String): Result<File> = runCatching {
    val uri = Uri.parse(path)
    val file = when (uri.scheme) {
        null, "" -> File(path)
        "file" -> File(requireNotNull(uri.path) { "Path de ROM invalido" })
        else -> throw IllegalArgumentException("Apenas ROM local em arquivo e suportada")
    }
    require(file.exists() && file.canRead()) {
        "ROM nao encontrada: ${file.absolutePath}"
    }
    file
}

