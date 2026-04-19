package com.example.deepmule.emu

import android.content.Context
import com.example.deepmule.data.SystemConfig
import java.io.File

/**
 * Resolve e valida o core nativo esperado para o sistema.
 */
object CoreProvisioning {

    private fun coreCandidates(context: Context, system: SystemConfig): List<File> =
        (listOf(system.coreName) + system.coreAlternatives)
            .distinct()
            .map { File(context.filesDir, "cores/${it}.so") }

    fun resolveCoreFile(context: Context, system: SystemConfig): File? =
        coreCandidates(context, system).firstOrNull { it.exists() && it.canRead() }

    fun ensureCoreAvailable(context: Context, system: SystemConfig): Result<File> = runCatching {
        val coreFile = resolveCoreFile(context, system)
        requireNotNull(coreFile) {
            val expected = coreCandidates(context, system).joinToString(" ou ") { it.name }
            "Core ausente para ${system.name}. Esperado: ${expected} em ${File(context.filesDir, "cores").absolutePath}"
        }
        coreFile
    }
}

