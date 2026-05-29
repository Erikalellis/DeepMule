package com.example.deepmule.emu

import android.content.Context
import com.example.deepmule.data.SystemConfig
import java.io.File

/**
 * Resolve e valida o core nativo esperado para o sistema.
 */
object CoreProvisioning {

    private val coreAliases = mapOf(
        "mupen64plus_next" to listOf("mupen64plus_next_gles3", "mupen64plus_next_gles2", "mupen64plus"),
        "mednafen_pce_fast" to listOf("beetle_pce_fast", "mednafen_pce"),
        "pcsx_rearmed_gles" to listOf("pcsx_rearmed", "pcsx-rearmed")
    )

    private fun coreNameVariants(coreName: String): List<String> =
        listOf(coreName, coreName.replace("-", "_"), coreName.replace("_", "-"))
            .filter { it.isNotBlank() }
            .distinct()

    private fun coreLookupNames(system: SystemConfig): List<String> {
        val names = linkedSetOf<String>()
        val requested = (listOf(system.coreName) + system.coreAlternatives).distinct()
        requested.forEach { core ->
            names += core
            coreAliases[core].orEmpty().forEach { names += it }
        }

        val expanded = linkedSetOf<String>()
        names.forEach { name ->
            coreNameVariants(name).forEach { expanded += it }
        }
        return expanded.toList()
    }

    private fun coreCandidates(context: Context, system: SystemConfig): List<File> {
        val nativeDir = File(context.applicationInfo.nativeLibraryDir)
        return coreLookupNames(system).flatMap { coreName ->
            listOf(
                File(context.filesDir, "cores/${coreName}.so"),
                File(context.filesDir, "cores/${coreName}_libretro_android.so"),
                File(context.filesDir, "cores/${coreName}_libretro.so"),
                File(nativeDir, "lib${coreName}.so"),
                File(nativeDir, "${coreName}.so")
            )
        }.distinct()
    }

    fun resolveCoreFile(context: Context, system: SystemConfig): File? =
        coreCandidates(context, system).firstOrNull { it.exists() && it.canRead() }

    fun ensureCoreAvailable(context: Context, system: SystemConfig): Result<File> = runCatching {
        val coreFile = resolveCoreFile(context, system)
        requireNotNull(coreFile) {
            val expected = coreCandidates(context, system).joinToString(" ou ") { it.name }
            "Core ausente para ${system.name}. Esperado: $expected em ${File(context.filesDir, "cores").absolutePath}"
        }
        coreFile
    }
}

