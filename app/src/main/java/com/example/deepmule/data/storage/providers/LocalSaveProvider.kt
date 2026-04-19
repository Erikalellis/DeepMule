package com.example.deepmule.data.storage.providers

import android.content.Context
import com.example.deepmule.data.storage.SaveMetadata
import com.example.deepmule.data.storage.SaveProvider
import java.io.File
import java.security.MessageDigest

/**
 * Implementação local de SaveProvider
 * Salva states no armazenamento interno do dispositivo
 */
class LocalSaveProvider(private val context: Context) : SaveProvider {
    
    private val savesDir = File(context.filesDir, "saves").apply { mkdirs() }
    
    override suspend fun saveState(
        gameId: String,
        slotNumber: Int,
        stateFile: File
    ): Result<Unit> = runCatching {
        if (!stateFile.exists()) {
            throw IllegalArgumentException("Arquivo de state não encontrado: ${stateFile.absolutePath}")
        }
        
        val destFile = File(savesDir, "${normalizedGameId(gameId)}_slot_${slotNumber}.sav")
        stateFile.copyTo(destFile, overwrite = true)
    }
    
    override suspend fun loadState(
        gameId: String,
        slotNumber: Int,
        destinationFile: File
    ): Result<Unit> = runCatching {
        val sourceFile = File(savesDir, "${normalizedGameId(gameId)}_slot_${slotNumber}.sav")

        if (!sourceFile.exists()) {
            throw IllegalArgumentException("Save não encontrado: $gameId slot $slotNumber")
        }
        
        sourceFile.copyTo(destinationFile, overwrite = true)
    }
    
    override suspend fun listSaves(gameId: String): Result<List<SaveMetadata>> = runCatching {
        val gamePrefix = "${normalizedGameId(gameId)}_slot_"
        savesDir.listFiles { file ->
            file.name.startsWith(gamePrefix) && file.name.endsWith(".sav")
        }?.map { file ->
            val slotNumber = file.name
                .removePrefix(gamePrefix)
                .removeSuffix(".sav")
                .toIntOrNull() ?: return@map null
            
            SaveMetadata(
                gameId = gameId,
                slotNumber = slotNumber,
                timestamp = file.lastModified(),
                sizeBytes = file.length(),
                fileName = file.name,
                syncedWithCloud = false
            )
        }?.filterNotNull() ?: emptyList()
    }
    
    override suspend fun deleteSave(
        gameId: String,
        slotNumber: Int
    ): Result<Unit> = runCatching {
        val file = File(savesDir, "${normalizedGameId(gameId)}_slot_${slotNumber}.sav")
        if (file.exists()) {
            file.delete()
        }
    }
    
    override suspend fun getSaveMetadata(
        gameId: String,
        slotNumber: Int
    ): Result<SaveMetadata> = runCatching {
        val file = File(savesDir, "${normalizedGameId(gameId)}_slot_${slotNumber}.sav")

        if (!file.exists()) {
            throw IllegalArgumentException("Save não encontrado")
        }
        
        SaveMetadata(
            gameId = gameId,
            slotNumber = slotNumber,
            timestamp = file.lastModified(),
            sizeBytes = file.length(),
            fileName = file.name,
            syncedWithCloud = false
        )
    }
    
    override suspend fun testConnection(): Result<Boolean> = runCatching {
        savesDir.exists() && savesDir.canWrite()
    }

    private fun normalizedGameId(gameId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(gameId.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(24)
    }

    override fun getProviderName(): String = "Local (Dispositivo)"
}

