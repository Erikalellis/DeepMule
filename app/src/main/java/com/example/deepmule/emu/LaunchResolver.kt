package com.example.deepmule.emu

import android.content.Context
import androidx.core.net.toUri
import com.example.deepmule.data.GameEntity
import com.example.deepmule.data.SystemConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

import java.util.zip.ZipInputStream

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
        system: SystemConfig,
        onProgress: (String) -> Unit = {}
    ): Result<LaunchArtifacts> = withContext(Dispatchers.IO) {
        runCatching {
            onProgress("Verificando core...")
            val coreFile = CoreProvisioning.ensureCoreAvailable(context, system).getOrElse {
                throw it
            }

            onProgress("Preparando ROM...")
            var romFile = stageRomIfNeeded(context, game)
            
            // Se for ZIP, extrair tudo (necessário para multi-arquivos como PS1)
            if (romFile.extension.lowercase() == "zip" && system.id != "arcade") {
                onProgress("Extraindo arquivos...")
                romFile = extractZipContent(context, romFile, system) ?: romFile
            }

            // Sanitizar CUE se necessário
            if (romFile.extension.lowercase() == "cue") {
                onProgress("Validando referências CUE...")
                romFile = sanitizeCue(romFile)
            }

            LaunchArtifacts(
                corePath = coreFile.absolutePath,
                romPath = romFile.absolutePath
            )
        }
    }

    private fun extractZipContent(context: Context, zipFile: File, system: SystemConfig): File? {
        val baseDir = File(context.cacheDir, "rom_extracted")
        val targetDir = File(baseDir, stableId(zipFile))
        
        if (targetDir.exists() && targetDir.listFiles()?.isNotEmpty() == true) {
            return findBestFile(targetDir, system)
        }
        
        targetDir.mkdirs()
        
        try {
            ZipInputStream(zipFile.inputStream().buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val outFile = File(targetDir, entry.name)
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().use { output ->
                            zip.copyTo(output)
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (e: Exception) {
            return null
        }
        
        return findBestFile(targetDir, system)
    }

    private fun findBestFile(directory: File, system: SystemConfig): File? {
        val files = directory.listFiles() ?: return null
        // Tenta extensões na ordem definida no SystemConfig (ex: cue antes de bin)
        for (ext in system.extensions) {
            val match = files.find { it.extension.lowercase() == ext.lowercase() }
            if (match != null) return match
        }
        return files.firstOrNull { !it.isDirectory }
    }

    private fun sanitizeCue(cueFile: File): File {
        val lines = try { cueFile.readLines() } catch (e: Exception) { return cueFile }
        val parentDir = cueFile.parentFile ?: return cueFile
        var modified = false
        
        val newLines = lines.map { line ->
            if (line.trim().startsWith("FILE", ignoreCase = true)) {
                // Regex para capturar: FILE "filename" TYPE
                val regex = Regex("""FILE\s+"([^"]+)"\s+(.*)""", RegexOption.IGNORE_CASE)
                val match = regex.find(line)
                if (match != null) {
                    val originalName = match.groupValues[1]
                    val type = match.groupValues[2]
                    val referencedFile = File(parentDir, originalName)
                    
                    if (!referencedFile.exists()) {
                        // Busca case-insensitive no diretório
                        val actualFile = parentDir.listFiles()?.find { it.name.equals(originalName, ignoreCase = true) }
                        if (actualFile != null) {
                            modified = true
                            return@map "FILE \"${actualFile.name}\" $type"
                        }
                    }
                }
            }
            line
        }
        
        if (modified) {
            try {
                val sanitizedFile = File(parentDir, "fixed_${cueFile.name}")
                sanitizedFile.writeText(newLines.joinToString("\n"))
                return sanitizedFile
            } catch (e: Exception) {}
        }
        
        return cueFile
    }

    private fun stageRomIfNeeded(context: Context, game: GameEntity): File {
        val uri = game.path.toUri()
        if (uri.scheme == "content") {
            val extension = game.path.substringAfterLast('.', "rom")
            // Para URIs de content, usamos o stableId do path string, pois não temos o arquivo local ainda
            val stagedFile = File(context.cacheDir, "rom_staging/${stableId(game.path)}.$extension")
            
            if (stagedFile.exists() && stagedFile.length() > 0) return stagedFile

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

    private fun stableId(file: File): String {
        val input = "${file.absolutePath}:${file.length()}:${file.lastModified()}"
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }.take(16)
    }

    private fun stableId(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(value.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }.take(16)
    }
}

