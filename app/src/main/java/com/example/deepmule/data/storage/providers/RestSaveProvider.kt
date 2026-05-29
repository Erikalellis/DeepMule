package com.example.deepmule.data.storage.providers

import com.example.deepmule.data.storage.SaveMetadata
import com.example.deepmule.data.storage.SaveProvider
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import java.io.File

/**
 * Implementação REST API de SaveProvider
 * Conecta com um backend customizado do desenvolvedor
 */
class RestSaveProvider(
    private val apiClient: SaveStateApi,
    private val authToken: String
) : SaveProvider {

    override suspend fun saveState(
        gameId: String,
        slotNumber: Int,
        stateFile: File
    ): Result<Unit> = runCatching {
        val requestBody = stateFile.asRequestBody("application/octet-stream".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", stateFile.name, requestBody)
        val response = apiClient.uploadSave(
            gameId = gameId,
            slotNumber = slotNumber,
            authToken = "Bearer $authToken",
            file = part
        )

        if (!response.success) {
            throw Exception("Erro ao salvar: ${response.message}")
        }
    }

    override suspend fun loadState(
        gameId: String,
        slotNumber: Int,
        destinationFile: File
    ): Result<Unit> = runCatching {
        val response = apiClient.downloadSave(
            gameId = gameId,
            slotNumber = slotNumber,
            authToken = "Bearer $authToken"
        )

        if (response.isSuccessful) {
            destinationFile.writeBytes(response.body() ?: byteArrayOf())
        } else {
            throw Exception("Erro ao carregar save: ${response.code()}")
        }
    }

    override suspend fun listSaves(gameId: String): Result<List<SaveMetadata>> = runCatching {
        val response = apiClient.listSaves(
            gameId = gameId,
            authToken = "Bearer $authToken"
        )
        response.saves ?: emptyList()
    }

    override suspend fun deleteSave(
        gameId: String,
        slotNumber: Int
    ): Result<Unit> = runCatching {
        val response = apiClient.deleteSave(
            gameId = gameId,
            slotNumber = slotNumber,
            authToken = "Bearer $authToken"
        )

        if (!response.success) {
            throw Exception("Erro ao deletar: ${response.message}")
        }
    }

    override suspend fun getSaveMetadata(
        gameId: String,
        slotNumber: Int
    ): Result<SaveMetadata> = runCatching {
        val response = apiClient.getSaveMetadata(
            gameId = gameId,
            slotNumber = slotNumber,
            authToken = "Bearer $authToken"
        )
        response ?: throw Exception("Save não encontrado")
    }

    override suspend fun testConnection(): Result<Boolean> = runCatching {
        val response = apiClient.testConnection(authToken = "Bearer $authToken")
        response.connected
    }

    override fun getProviderName(): String = "Backend Customizado (REST API)"
}

/**
 * Interface Retrofit para API REST do backend
 */
interface SaveStateApi {

    @Multipart
    @POST("saves/{gameId}/{slotNumber}")
    suspend fun uploadSave(
        @Path("gameId") gameId: String,
        @Path("slotNumber") slotNumber: Int,
        @Header("Authorization") authToken: String,
        @Part file: MultipartBody.Part
    ): UploadResponse

    @GET("saves/download/{gameId}/{slotNumber}")
    suspend fun downloadSave(
        @Path("gameId") gameId: String,
        @Path("slotNumber") slotNumber: Int,
        @Header("Authorization") authToken: String
    ): retrofit2.Response<ByteArray>

    @GET("saves/{gameId}")
    suspend fun listSaves(
        @Path("gameId") gameId: String,
        @Header("Authorization") authToken: String
    ): ListSavesResponse

    @DELETE("saves/{gameId}/{slotNumber}")
    suspend fun deleteSave(
        @Path("gameId") gameId: String,
        @Path("slotNumber") slotNumber: Int,
        @Header("Authorization") authToken: String
    ): DeleteResponse

    @GET("saves/{gameId}/{slotNumber}/metadata")
    suspend fun getSaveMetadata(
        @Path("gameId") gameId: String,
        @Path("slotNumber") slotNumber: Int,
        @Header("Authorization") authToken: String
    ): SaveMetadata?

    @GET("health")
    suspend fun testConnection(
        @Header("Authorization") authToken: String
    ): HealthResponse
}

// Response DTOs
data class UploadResponse(
    val success: Boolean,
    val message: String = "",
    val saveId: String? = null
)

data class ListSavesResponse(
    val success: Boolean,
    val saves: List<SaveMetadata>?
)

data class DeleteResponse(
    val success: Boolean,
    val message: String = ""
)

data class HealthResponse(
    val connected: Boolean,
    val timestamp: Long
)

