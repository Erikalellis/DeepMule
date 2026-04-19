package com.example.deepmule.emu

import android.content.Context
import android.net.Uri
import com.example.deepmule.data.GameEntity
import com.example.deepmule.data.SystemConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Prepara artefatos de inicializacao para a sessao de emulacao.
 */
object LaunchResolver {

    data class LaunchArtifacts(
        val corePath: String,
        val romPath: String
    )

    suspend fun prepare(
        context: Context,
        game: GameEntity,
        system: SystemConfig
    ): Result<LaunchArtifacts> = withContext(Dispatchers.IO) {
        runCatching {
            val coreFile = File(context.filesDir, "cores/${system.coreName}.so")
            if (!coreFile.exists()) {
                error("Core ausente: ${coreFile.absolutePath}")
            }

            val romFile = stageRomIfNeeded(context, game)
            LaunchArtifacts(
                corePath = coreFile.absolutePath,
                romPath = romFile.absolutePath
            )
        }
    }

    private fun stageRomIfNeeded(context: Context, game: GameEntity): File {
        val uri = Uri.parse(game.path)
        if (uri.scheme == "content") {
            val extension = game.path.substringAfterLast('.', "rom")
            val stagedFile = File(context.cacheDir, "rom_staging/${stableId(game.path)}.$extension")
            stagedFile.parentFile?.mkdirs()

            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Nao foi possivel abrir ROM: ${game.path}" }
                stagedFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            return stagedFile
        }

        if (uri.scheme == "file") {
            val path = requireNotNull(uri.path) { "Path de arquivo invalido: ${game.path}" }
            return File(path)
        }

        return File(game.path)
    }

    private fun stableId(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(value.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }.take(16)
    }
}

