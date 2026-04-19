package com.example.deepmule.data.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.deepmule.data.storage.providers.LocalSaveProvider
import kotlinx.coroutines.flow.first

/**
 * Gerencia configuração de backend de saves do usuário.
 * Padrão: Local (armazenamento no próprio aparelho).
 * Futuramente: REST API, Google Drive, Dropbox, S3.
 */
class SaveStorageManager(private val context: Context) {

    private val dataStore = context.dataStore

    companion object {
        private val KEY_BACKEND = stringPreferencesKey("selected_backend")
        // Valores simples para evitar dependência de JSON na leitura
        private const val BACKEND_LOCAL = "local"
        private const val BACKEND_REST = "rest"
    }

    /**
     * Retorna o provider ativo.
     * Por enquanto sempre retorna Local.
     * Quando a API estiver pronta, basta salvar "rest" no DataStore.
     */
    suspend fun getProvider(): SaveProvider {
        val backend = dataStore.data.first()[KEY_BACKEND] ?: BACKEND_LOCAL
        return when (backend) {
            BACKEND_REST -> {
                // TODO: Carregar credenciais e criar RestSaveProvider quando API estiver pronta
                LocalSaveProvider(context)
            }
            else -> LocalSaveProvider(context)
        }
    }

    /**
     * Força o uso do backend local (padrão atual).
     */
    suspend fun useLocal() {
        dataStore.edit { it[KEY_BACKEND] = BACKEND_LOCAL }
    }

    /**
     * Ativa o backend REST quando a API estiver pronta.
     * @param baseUrl URL base da API
     * @param authToken Token de autenticação
     */
    suspend fun useRestApi(baseUrl: String, authToken: String) {
        dataStore.edit { prefs ->
            prefs[KEY_BACKEND] = BACKEND_REST
            prefs[stringPreferencesKey("rest_base_url")] = baseUrl
            prefs[stringPreferencesKey("rest_auth_token")] = authToken
        }
    }
}

private val Context.dataStore by preferencesDataStore(name = "save_storage_prefs")

