package com.example.deepmule.analytics

import android.content.Context
import android.os.Build
import com.example.deepmule.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reporter automático de análises e crashes para o GitHub.
 *
 * Envia:
 * - Issues automáticos para crashes não tratados
 * - Relatórios de uso (anônimos) para o repositório
 *
 * Configurar o secret GITHUB_REPORTER_TOKEN no repositório:
 * GitHub → Settings → Secrets → Actions → New secret
 */
object DeepMuleReporter {

    // Configurar com seu repositório
    private const val GITHUB_OWNER = "Erikalellis"
    private const val GITHUB_REPO = "DeepMule"

    // Token configurado via BuildConfig (vem de secrets do GitHub Actions / local secrets.properties)
    private val GITHUB_TOKEN get() = BuildConfig.GITHUB_REPORTER_TOKEN

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("pt", "BR"))

    /**
     * Inicializa o reporter e registra o handler de crashes.
     * Chamar em DeepMuleApplication.onCreate()
     */
    fun init(context: Context) {
        if (GITHUB_TOKEN.isBlank() || GITHUB_TOKEN == "none") return

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Reportar crash para o GitHub
            reportCrash(context, throwable)

            // Continuar com o handler padrão do Android
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    /**
     * Reporta um crash como Issue no GitHub.
     */
    private fun reportCrash(context: Context, throwable: Throwable) {
        val stackTrace = StringWriter().also {
            throwable.printStackTrace(PrintWriter(it))
        }.toString()

        val deviceInfo = buildDeviceInfo(context)
        val timestamp = dateFormat.format(Date())

        val issueTitle = "💥 Crash: ${throwable.javaClass.simpleName} - ${throwable.message?.take(60) ?: "sem mensagem"}"

        val issueBody = """
## 💥 Relatório de Crash Automático

**Data/Hora:** $timestamp  
**Versão App:** ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})  
**Ambiente:** ${if (BuildConfig.DEBUG) "Debug" else "Release"}

---

## 📱 Dispositivo

$deviceInfo

---

## 📋 Stack Trace

```
$stackTrace
```

---

## 🔖 Labels
- `crash`
- `automated`
- `${if (BuildConfig.DEBUG) "debug" else "release"}`

---
*Issue criado automaticamente pelo DeepMule CrashReporter*
        """.trimIndent()

        scope.launch {
            try {
                createGitHubIssue(
                    title = issueTitle,
                    body = issueBody,
                    labels = listOf("crash", "bug", "automated")
                )
            } catch (e: Exception) {
                // Falha silenciosa - não deve interferir no crash handler
            }
        }
    }

    /**
     * Reporta um evento de uso/análise como comentário em Issue dedicada.
     */
    fun reportEvent(eventName: String, properties: Map<String, String> = emptyMap()) {
        if (GITHUB_TOKEN.isBlank() || GITHUB_TOKEN == "none") return

        scope.launch {
            try {
                val timestamp = dateFormat.format(Date())
                val propsText = if (properties.isNotEmpty()) {
                    properties.entries.joinToString("\n") { "- **${it.key}:** ${it.value}" }
                } else {
                    "_sem propriedades_"
                }

                val body = """
**Evento:** `$eventName`  
**Versão:** ${BuildConfig.VERSION_NAME}  
**Horário:** $timestamp

$propsText
                """.trimIndent()

                createGitHubIssue(
                    title = "📊 Análise: $eventName - $timestamp",
                    body = body,
                    labels = listOf("analytics", "automated")
                )
            } catch (e: Exception) {
                // Silencioso
            }
        }
    }

    /**
     * Cria um Issue no GitHub via API REST.
     */
    private fun createGitHubIssue(
        title: String,
        body: String,
        labels: List<String> = emptyList()
    ) {
        val json = JSONObject().apply {
            put("title", title)
            put("body", body)
            put("labels", org.json.JSONArray(labels))
        }.toString()

        val request = Request.Builder()
            .url("https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/issues")
            .addHeader("Authorization", "Bearer $GITHUB_TOKEN")
            .addHeader("Accept", "application/vnd.github+json")
            .addHeader("X-GitHub-Api-Version", "2022-11-28")
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("GitHub API error: ${response.code}")
            }
        }
    }

    /**
     * Monta string com informações do dispositivo.
     */
    private fun buildDeviceInfo(context: Context): String {
        val displayMetrics = context.resources.displayMetrics
        return """
| Campo | Valor |
|-------|-------|
| Fabricante | ${Build.MANUFACTURER} |
| Modelo | ${Build.MODEL} |
| Android | ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) |
| Tela | ${displayMetrics.widthPixels}x${displayMetrics.heightPixels} px |
| Densidade | ${displayMetrics.densityDpi} dpi |
| Idioma | ${Locale.getDefault().toLanguageTag()} |
        """.trimIndent()
    }
}

