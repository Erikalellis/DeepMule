package com.example.deepmule.data

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Persiste URIs de pastas de ROM no SharedPreferences.
 * Permite adicionar/remover/listar fontes de forma persistente.
 */
class RomSourceManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("rom_sources_v1", Context.MODE_PRIVATE)

    fun getSources(): List<String> =
        (prefs.getStringSet("uris", emptySet()) ?: emptySet()).toList().sorted()

    fun addSource(uri: String) {
        val current = getSources().toMutableSet().apply { add(uri) }
        prefs.edit().putStringSet("uris", current).apply()
    }

    fun removeSource(uri: String) {
        val current = getSources().toMutableSet().apply { remove(uri) }
        prefs.edit().putStringSet("uris", current).apply()
    }

    fun clear() {
        prefs.edit().remove("uris").apply()
    }

    /**
     * Garante fonte local padrao de ROMs em /files/roms.
     */
    fun ensureDefaultSource(): String? {
        val internalRomDir = File(context.filesDir, "roms").apply {
            if (!exists()) mkdirs()
        }
        val internalUri = Uri.fromFile(internalRomDir).toString()

        // Local-only policy: keep a single source pointing to /files/roms.
        prefs.edit().putStringSet("uris", setOf(internalUri)).apply()

        return internalRomDir.absolutePath
    }

    fun getPublicRomPath(): String? {
        return File(context.filesDir, "roms").absolutePath
    }

    fun defaultSourcePath(): String = File(context.filesDir, "roms").absolutePath
}

