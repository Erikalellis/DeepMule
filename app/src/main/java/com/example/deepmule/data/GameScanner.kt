package com.example.deepmule.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.deepmule.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class GameScanner(private val context: Context) {

    private val artworkResolver = GameArtworkResolver(context)
    private val metadataResolver = LibretroMetadataResolver(context)

    suspend fun scanDirectory(uri: Uri): List<GameEntity> = withContext(Dispatchers.IO) {
        val games = mutableListOf<GameEntity>()

        if (uri.scheme == "file") {
            val rootFile = File(requireNotNull(uri.path) { "URI de arquivo invalida" })
            if (!rootFile.exists() || !rootFile.isDirectory) return@withContext emptyList<GameEntity>()
            scanRecursiveFile(rootFile, games)
            return@withContext games
        }

        val root = DocumentFile.fromTreeUri(context, uri) ?: return@withContext emptyList<GameEntity>()
        scanRecursive(root, games)
        games
    }

    private suspend fun scanRecursive(directory: DocumentFile, games: MutableList<GameEntity>) {
        directory.listFiles().forEach { file ->
            if (file.isDirectory) {
                scanRecursive(file, games)
            } else {
                val extension = file.name?.substringAfterLast('.', "") ?: ""
                val system = resolveSystem(extension, file.uri.toString())
                if (system != null) {
                    val rawName = file.name ?: ""
                    val fallbackTitle = rawName.substringBeforeLast('.')
                        .ifBlank { context.getString(R.string.unknown_game) }
                    val title = metadataResolver.resolveTitleByFileName(rawName, fallbackTitle)
                    val cover = artworkResolver.resolveBoxArt(
                        system = system,
                        title = title,
                        gameId = file.uri.toString()
                    )

                    games.add(
                        GameEntity(
                            path = file.uri.toString(),
                            title = title,
                            system = system.name,
                            boxArtUrl = cover
                        )
                    )
                }
            }
        }
    }

    private suspend fun scanRecursiveFile(directory: File, games: MutableList<GameEntity>) {
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                scanRecursiveFile(file, games)
            } else {
                val extension = file.extension
                val system = resolveSystem(extension, file.absolutePath)
                if (system != null) {
                    val rawName = file.name
                    val fallbackTitle = file.nameWithoutExtension.ifBlank {
                        context.getString(R.string.unknown_game)
                    }
                    val title = metadataResolver.resolveTitleByFileName(rawName, fallbackTitle)
                    val cover = artworkResolver.resolveBoxArt(
                        system = system,
                        title = title,
                        gameId = file.toURI().toString()
                    )

                    games.add(
                        GameEntity(
                            path = file.toURI().toString(),
                            title = title,
                            system = system.name,
                            boxArtUrl = cover
                        )
                    )
                }
            }
        }
    }

    private fun resolveSystem(extension: String, pathHint: String): SystemConfig? {
        val normalizedExt = extension.lowercase()
        val byFolder = SupportedSystems.getByPathHint(pathHint, normalizedExt)
        val extMapped = SupportedSystems.getByExtension(normalizedExt)
        if (normalizedExt != "zip" && normalizedExt != "7z") {
            return byFolder ?: extMapped
        }

        return byFolder ?: extMapped
    }
}
