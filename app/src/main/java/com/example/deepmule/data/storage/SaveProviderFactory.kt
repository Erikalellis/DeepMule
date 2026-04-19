package com.example.deepmule.data.storage

import android.content.Context
import com.example.deepmule.data.storage.providers.LocalSaveProvider
import com.example.deepmule.data.storage.providers.RestSaveProvider
import com.example.deepmule.data.storage.providers.SaveStateApi
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Factory para criar instâncias de SaveProvider
 * Usa enum para configurar qual backend usar
 */
sealed class SaveBackendConfig {
    object Local : SaveBackendConfig()
    data class RestAPI(val baseUrl: String, val authToken: String) : SaveBackendConfig()
    data class GoogleDrive(val accessToken: String) : SaveBackendConfig()
    data class Dropbox(val accessToken: String) : SaveBackendConfig()
    data class S3(val bucketName: String, val region: String, val accessKey: String, val secretKey: String) : SaveBackendConfig()
}

/**
 * Factory para instanciar SaveProviders
 */
object SaveProviderFactory {
    
    fun createProvider(
        config: SaveBackendConfig,
        context: Context
    ): SaveProvider = when (config) {
        
        SaveBackendConfig.Local -> {
            LocalSaveProvider(context)
        }
        
        is SaveBackendConfig.RestAPI -> {
            val retrofit = Retrofit.Builder()
                .baseUrl(config.baseUrl)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
            
            val apiClient = retrofit.create(SaveStateApi::class.java)
            RestSaveProvider(apiClient, config.authToken)
        }
        
        is SaveBackendConfig.GoogleDrive -> {
            // TODO: Implementar GoogleDriveSaveProvider
            LocalSaveProvider(context) // Fallback temporário
        }
        
        is SaveBackendConfig.Dropbox -> {
            // TODO: Implementar DropboxSaveProvider
            LocalSaveProvider(context) // Fallback temporário
        }
        
        is SaveBackendConfig.S3 -> {
            // TODO: Implementar S3SaveProvider
            LocalSaveProvider(context) // Fallback temporário
        }
    }
    
    /**
     * Retorna lista de backends disponíveis com seus nomes
     */
    fun getAvailableBackends(): List<Pair<SaveBackendConfig, String>> = listOf(
        SaveBackendConfig.Local to "Local (Dispositivo)",
        SaveBackendConfig.GoogleDrive("") to "Google Drive",
        SaveBackendConfig.Dropbox("") to "Dropbox",
        SaveBackendConfig.S3("", "", "", "") to "AWS S3",
        SaveBackendConfig.RestAPI("", "") to "Backend Customizado"
    )
}

