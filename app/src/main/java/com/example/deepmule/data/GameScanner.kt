package com.example.deepmule.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.deepmule.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameScanner(private val context: Context) {

    suspend fun scanDirectory(uri: Uri): List<GameEntity> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, uri) ?: return@withContext emptyList<GameEntity>()
        val games = mutableListOf<GameEntity>()
        scanRecursive(root, games)
        games
    }

    private fun scanRecursive(directory: DocumentFile, games: MutableList<GameEntity>) {
        directory.listFiles().forEach { file ->
            if (file.isDirectory) {
                scanRecursive(file, games)
            } else {
                val extension = file.name?.substringAfterLast('.', "") ?: ""
                val system = SupportedSystems.getByExtension(extension)
                if (system != null) {
                    games.add(
                        GameEntity(
                            path = file.uri.toString(),
                            title = file.name?.substringBeforeLast('.')
                                ?: context.getString(R.string.unknown_game),
                            system = system.name
                        )
                    )
                }
            }
        }
    }
}
