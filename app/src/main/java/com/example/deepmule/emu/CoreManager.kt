package com.example.deepmule.emu

import android.content.Context
import com.example.deepmule.data.SupportedSystems
import com.example.deepmule.data.SystemConfig
import java.io.File

/**
 * Local core orchestration used by the player flow before launching emulation.
 */
class CoreManager(private val context: Context) {

    fun checkCoreInstalled(systemId: String): Boolean {
        val system = resolveSystem(systemId) ?: return false
        return CoreProvisioning.resolveCoreFile(context, system) != null
    }

    suspend fun downloadCore(
        systemId: String,
        onProgress: (String) -> Unit = {}
    ): Result<File> {
        val system = resolveSystem(systemId)
            ?: return Result.failure(IllegalArgumentException("Sistema nao suportado: $systemId"))

        val coreNames = (listOf(system.coreName) + system.coreAlternatives).distinct()
        var lastError: Throwable? = null

        for (coreName in coreNames) {
            val result = CoreDownloader.downloadCoreIfNeeded(
                context = context,
                coreName = coreName,
                onProgress = onProgress
            )
            if (result.isSuccess) {
                val resolved = CoreProvisioning.resolveCoreFile(context, system)
                if (resolved != null) {
                    return Result.success(resolved)
                }
            } else {
                lastError = result.exceptionOrNull()
            }
        }

        return Result.failure(lastError ?: IllegalStateException("Falha ao baixar core para ${system.name}"))
    }

    suspend fun updateCore(
        systemId: String,
        onProgress: (String) -> Unit = {}
    ): Result<File> {
        val system = resolveSystem(systemId)
            ?: return Result.failure(IllegalArgumentException("Sistema nao suportado: $systemId"))

        val coreDir = java.io.File(context.filesDir, "cores")
        (listOf(system.coreName) + system.coreAlternatives)
            .distinct()
            .forEach { coreName ->
                listOf(
                    java.io.File(coreDir, "$coreName.so"),
                    java.io.File(coreDir, "${coreName}_libretro.so"),
                    java.io.File(coreDir, "${coreName}_libretro_android.so")
                ).forEach { candidate ->
                    if (candidate.exists()) candidate.delete()
                }
            }

        return downloadCore(systemId = systemId, onProgress = onProgress)
    }

    fun getCorePath(systemId: String): String? {
        val system = resolveSystem(systemId) ?: return null
        return CoreProvisioning.resolveCoreFile(context, system)?.absolutePath
    }

    private fun resolveSystem(systemId: String): SystemConfig? {
        return SupportedSystems.getById(systemId)
            ?: SupportedSystems.getByName(systemId)
    }
}

