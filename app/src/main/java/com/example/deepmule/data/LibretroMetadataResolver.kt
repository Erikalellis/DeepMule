package com.example.deepmule.data

import android.content.Context
import java.io.File

/**
 * Resolve metadados básicos (nome canônico) a partir dos .dat oficiais do libretro-database.
 * Implementação leve por filename de ROM para evitar dependência de schema sqlite.
 */
class LibretroMetadataResolver(context: Context) {
    private val databaseRoot = File(context.filesDir, "retroarch/database")
    private val romNameToTitle by lazy { loadIndex() }

    fun resolveTitleByFileName(fileName: String, fallbackTitle: String): String {
        val normalized = normalizeRomName(fileName)
        if (normalized.isBlank()) return fallbackTitle

        return romNameToTitle[normalized]
            ?: romNameToTitle[normalized.substringAfterLast('/')]
            ?: fallbackTitle
    }

    private fun loadIndex(): Map<String, String> {
        if (!databaseRoot.exists()) return emptyMap()

        val index = HashMap<String, String>()
        val datFiles = databaseRoot.walkTopDown()
            .maxDepth(3)
            .filter { it.isFile && it.extension.equals("dat", ignoreCase = true) }

        datFiles.forEach { dat ->
            dat.inputStream().bufferedReader().useLines { lines ->
                LibretroDatParser.parse(lines).forEach { (romName, title) ->
                    index.putIfAbsent(romName, title)
                }
            }
        }

        return index
    }
}

internal object LibretroDatParser {
    fun parse(lines: Sequence<String>): Map<String, String> {
        val out = HashMap<String, String>()
        var inGame = false
        var gameTitle: String? = null

        lines.forEach { raw ->
            val line = raw.trim()

            if (line.startsWith("game (", ignoreCase = true)) {
                inGame = true
                gameTitle = null
                return@forEach
            }

            if (!inGame) return@forEach

            if (line == ")") {
                inGame = false
                gameTitle = null
                return@forEach
            }

            if (gameTitle == null && line.startsWith("name \"", ignoreCase = true)) {
                gameTitle = line.substringAfter("\"").substringBeforeLast("\"").trim()
                return@forEach
            }

            if (!line.startsWith("rom", ignoreCase = true)) return@forEach

            val romName = extractQuotedValue(line, "name") ?: return@forEach
            if (!romName.contains('.')) return@forEach

            val key = normalizeRomName(romName)
            if (key.isBlank()) return@forEach
            out.putIfAbsent(key, gameTitle ?: romName)
        }

        return out
    }

    private fun extractQuotedValue(line: String, field: String): String? {
        val pattern = Regex("$field\\s+\"([^\"]+)\"", RegexOption.IGNORE_CASE)
        return pattern.find(line)?.groupValues?.getOrNull(1)?.trim()
    }
}

internal fun normalizeRomName(name: String): String =
    name.trim()
        .replace('\\', '/')
        .substringAfterLast('/')
        .lowercase()
