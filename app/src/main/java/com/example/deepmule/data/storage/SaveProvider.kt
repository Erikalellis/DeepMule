package com.example.deepmule.data.storage

import java.io.File

/**
 * Interface abstrata para sincronização de SaveStates
 * Permite múltiplos backends (Local, Google Drive, Dropbox, AWS S3, Backend REST)
 */
interface SaveProvider {
    
    /**
     * Salva um state em um slot específico
     * @param gameId ID do jogo
     * @param slotNumber Número do slot (0-9)
     * @param stateFile Arquivo do state a ser salvo
     */
    suspend fun saveState(gameId: String, slotNumber: Int, stateFile: File): Result<Unit>
    
    /**
     * Carrega um state de um slot específico
     * @param gameId ID do jogo
     * @param slotNumber Número do slot (0-9)
     * @param destinationFile Local para salvar o arquivo carregado
     */
    suspend fun loadState(gameId: String, slotNumber: Int, destinationFile: File): Result<Unit>
    
    /**
     * Lista todos os saves disponíveis para um jogo
     * @param gameId ID do jogo
     */
    suspend fun listSaves(gameId: String): Result<List<SaveMetadata>>
    
    /**
     * Deleta um save específico
     * @param gameId ID do jogo
     * @param slotNumber Número do slot
     */
    suspend fun deleteSave(gameId: String, slotNumber: Int): Result<Unit>
    
    /**
     * Obtém metadados de um save
     */
    suspend fun getSaveMetadata(gameId: String, slotNumber: Int): Result<SaveMetadata>
    
    /**
     * Testa conexão com o provider (para cloud)
     */
    suspend fun testConnection(): Result<Boolean>
    
    /**
     * Nome/tipo do provider
     */
    fun getProviderName(): String
}

/**
 * Metadados de um SaveState
 */
data class SaveMetadata(
    val gameId: String,
    val slotNumber: Int,
    val timestamp: Long,
    val sizeBytes: Long,
    val fileName: String,
    val syncedWithCloud: Boolean = false,
    val lastCloudSync: Long? = null
)

/**
 * Resultado genérico para operações async
 */
typealias SaveOperationResult<T> = Result<T>

