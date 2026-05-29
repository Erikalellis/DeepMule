package com.example.deepmule.emu

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

import java.util.zip.ZipInputStream

/**
 * Utilitário para baixar cores (núcleos) de emulação do buildbot oficial do Libretro.
 */
object CoreDownloader {

    private const val BUILDBOT_BASE_URL = "https://buildbot.libretro.com/nightly/android/latest/"

    /**
     * Tenta baixar o core para o sistema especificado se ele ainda não existir localmente.
     * @param context Contexto da aplicação
     * @param coreName Nome do core (ex: "snes9x")
     * @param abi Arquitetura do Android (ex: "arm64-v8a", "armeabi-v7a")
     */
    suspend fun downloadCoreIfNeeded(
        context: Context,
        coreName: String,
        abi: String = getAbi(),
        onProgress: (String) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        val coresDir = File(context.filesDir, "cores")
        if (!coresDir.exists()) coresDir.mkdirs()

        val destinationFile = File(coresDir, "${coreName}.so")

        if (destinationFile.exists() && destinationFile.length() > 0) {
            return@withContext Result.success(destinationFile)
        }

        runCatching {
            onProgress("Baixando $coreName...")
            // Buildbot agora serve arquivos .so.zip
            val coreZipName = "${coreName}_libretro_android.so.zip"
            val downloadUrl = "$BUILDBOT_BASE_URL$abi/$coreZipName"
            
            android.util.Log.i("DeepMule", "Baixando core: $downloadUrl")
            
            URL(downloadUrl).openConnection().apply {
                connectTimeout = 10000
                readTimeout = 10000
            }.getInputStream().use { input ->
                ZipInputStream(input).use { zip ->
                    val entry = zip.nextEntry
                    if (entry != null) {
                        onProgress("Extraindo...")
                        destinationFile.outputStream().use { output ->
                            zip.copyTo(output)
                        }
                        android.util.Log.i("DeepMule", "Core extraído: ${entry.name}")
                    } else {
                        throw Exception("Zip vazio ou inválido: $downloadUrl")
                    }
                }
            }
            
            // Garantir permissão de execução após o download
            try {
                destinationFile.setExecutable(true, false)
            } catch (e: Exception) {
                // Falha não crítica
            }
            destinationFile
        }
    }

    private fun getAbi(): String {
        val abi = android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        return when {
            abi.contains("arm64") -> "arm64-v8a"
            abi.contains("armeabi") -> "armeabi-v7a"
            abi.contains("x86_64") -> "x86_64"
            abi.contains("x86") -> "x86"
            else -> "arm64-v8a"
        }
    }
}
