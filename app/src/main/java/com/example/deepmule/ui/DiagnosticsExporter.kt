package com.example.deepmule.ui

import android.content.Context
import com.example.deepmule.data.GameEntity
import com.example.deepmule.data.SupportedSystems
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DiagnosticsFiles(
    val jsonFile: File,
    val txtFile: File
)

object DiagnosticsExporter {
    fun export(
        context: Context,
        games: List<GameEntity>,
        selectedSystem: String?,
        isScanning: Boolean
    ): DiagnosticsFiles {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val jsonFile = File(reportsDir, "deepmule-diagnostic-$timestamp.json")
        val txtFile = File(reportsDir, "deepmule-diagnostic-$timestamp.txt")

        val biosDir = File(context.filesDir, "bios")
        val coresDir = File(context.filesDir, "cores")

        val biosArray = JSONArray()
        SupportedSystems.biosRequired().forEach { system ->
            val biosName = system.biosFileName ?: ""
            val biosFile = File(biosDir, biosName)
            val biosJson = JSONObject()
                .put("systemId", system.id)
                .put("systemName", system.name)
                .put("biosFile", biosName)
                .put("present", biosFile.exists())
                .put("size", if (biosFile.exists()) biosFile.length() else 0)
            biosArray.put(biosJson)
        }

        val bySystem = games.groupBy { it.system }
        val systemsJson = JSONObject()
        bySystem.forEach { (system, list) ->
            systemsJson.put(system, list.size)
        }

        val root = JSONObject()
            .put("app", "DeepMule")
            .put("generatedAt", timestamp)
            .put("selectedSystem", selectedSystem ?: "ALL")
            .put("isScanning", isScanning)
            .put("gamesTotal", games.size)
            .put("gamesBySystem", systemsJson)
            .put("bios", biosArray)
            .put("biosDir", biosDir.absolutePath)
            .put("biosDirExists", biosDir.exists())
            .put("coresDir", coresDir.absolutePath)
            .put("coresDirExists", coresDir.exists())
            .put("coresCount", coresDir.listFiles()?.size ?: 0)

        jsonFile.writeText(root.toString(2))

        val txt = buildString {
            appendLine("DeepMule - Diagnostico")
            appendLine("Gerado em: $timestamp")
            appendLine("Sistema selecionado: ${selectedSystem ?: "ALL"}")
            appendLine("Escaneando: $isScanning")
            appendLine("Jogos totais: ${games.size}")
            appendLine("")
            appendLine("Jogos por sistema:")
            if (bySystem.isEmpty()) {
                appendLine("- nenhum jogo indexado")
            } else {
                bySystem.toSortedMap().forEach { (system, list) ->
                    appendLine("- $system: ${list.size}")
                }
            }
            appendLine("")
            appendLine("BIOS obrigatorias:")
            SupportedSystems.biosRequired().forEach { system ->
                val biosName = system.biosFileName ?: ""
                val biosFile = File(biosDir, biosName)
                val status = if (biosFile.exists()) "OK" else "MISSING"
                appendLine("- ${system.name}: $biosName -> $status")
            }
            appendLine("")
            appendLine("Pastas internas:")
            appendLine("- bios: ${biosDir.absolutePath} (exists=${biosDir.exists()})")
            appendLine("- cores: ${coresDir.absolutePath} (exists=${coresDir.exists()}, count=${coresDir.listFiles()?.size ?: 0})")
        }

        txtFile.writeText(txt)
        return DiagnosticsFiles(jsonFile = jsonFile, txtFile = txtFile)
    }
}

