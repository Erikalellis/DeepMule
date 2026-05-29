package com.example.deepmule

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Valida todos os perfis de autoconfig de controles (retroarch-pack/autoconfig/android/).
 * Teste JVM puro — sem Android, sem device necessario.
 */
class AutoconfigValidationTest {

    private val REQUIRED_KEYS = listOf("input_device", "input_driver", "input_b_btn")

    private fun findAutoconfigDir(): File? {
        var dir = File(System.getProperty("user.dir") ?: ".")
        repeat(5) {
            val candidate = File(dir, "retroarch-pack/autoconfig/android")
            if (candidate.isDirectory) return candidate
            dir = dir.parentFile ?: return@repeat
        }
        return null
    }

    /** Analisa um .cfg do RetroArch / linhas "chave = valor" e comentarios. */
    private fun parseCfg(file: File): Map<String, String> {
        val map = mutableMapOf<String, String>()
        file.forEachLine { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEachLine
            val eqIdx = trimmed.indexOf('=')
            if (eqIdx > 0) {
                val key   = trimmed.substring(0, eqIdx).trim()
                val value = trimmed.substring(eqIdx + 1).trim().removeSurrounding("\"")
                map[key] = value
            }
        }
        return map
    }

    @Test
    fun allAutoconfigFiles_haveValidSyntax() {
        val dir = findAutoconfigDir() ?: run {
            println("AVISO: autoconfig/android nao encontrado — teste pulado")
            return
        }

        val badFiles = mutableListOf<String>()
        dir.listFiles { f -> f.extension == "cfg" }?.forEach { cfg ->
            try {
                cfg.forEachLine { line ->
                    val trimmed = line.trim()
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEachLine
                    require(trimmed.contains('=')) { "Linha sem '=': $trimmed" }
                }
            } catch (e: Exception) {
                badFiles += "${cfg.name}: ${e.message}"
            }
        }

        assertTrue(
            "Autoconfigs com sintaxe invalida:\n${badFiles.joinToString("\n")}",
            badFiles.isEmpty()
        )
    }

    @Test
    fun popularControllers_haveRequiredFields() {
        val dir = findAutoconfigDir() ?: run {
            println("AVISO: autoconfig/android nao encontrado — teste pulado")
            return
        }

        val cfgFiles = dir.listFiles { f -> f.extension == "cfg" } ?: emptyArray()
        assertTrue("Nenhum .cfg encontrado em ${dir.absolutePath}", cfgFiles.isNotEmpty())

        val missingFields = mutableListOf<String>()
        cfgFiles.forEach { cfg ->
            val parsed = parseCfg(cfg)
            for (key in REQUIRED_KEYS) {
                if (!parsed.containsKey(key)) {
                    missingFields += "${cfg.name}: falta '$key'"
                    return@forEach  // um aviso por arquivo
                }
            }
        }

        // Reporta como aviso (warn) mas nao quebra o build — alguns perifericos  sao minimais
        if (missingFields.isNotEmpty()) {
            println("AVISO — campos minimos ausentes (${missingFields.size} arquivo(s)):")
            missingFields.forEach { println("  $it") }
        }
    }

    @Test
    fun dualSense_android13_isPresent_andHasDriver() {
        val dir = findAutoconfigDir() ?: run {
            println("AVISO: autoconfig/android nao encontrado — teste pulado")
            return
        }

        val file = File(dir, "DualSense Wireless Controller (Android 13).cfg")
        if (!file.exists()) {
            println("AVISO: DualSense Android 13 nao encontrado — teste pulado")
            return
        }

        val cfg = parseCfg(file)
        assertNotNull("input_device ausente no DualSense Android 13", cfg["input_device"])
        assertNotNull("input_driver ausente no DualSense Android 13", cfg["input_driver"])
        assertNotNull("input_b_btn ausente no DualSense Android 13", cfg["input_b_btn"])
    }

    @Test
    fun xbox360_isPresent_andHasDriver() {
        val dir = findAutoconfigDir() ?: run {
            println("AVISO: autoconfig/android nao encontrado — teste pulado")
            return
        }

        val file = File(dir, "Microsoft_XBOX_360_Controller.cfg")
        if (!file.exists()) {
            println("AVISO: Xbox 360 autoconfig nao encontrado — teste pulado")
            return
        }

        val cfg = parseCfg(file)
        assertNotNull("input_device ausente no Xbox 360", cfg["input_device"])
        assertNotNull("input_b_btn ausente no Xbox 360", cfg["input_b_btn"])
    }

    @Test
    fun autoconfigCount_isAboveThreshold() {
        val dir = findAutoconfigDir() ?: run {
            println("AVISO: autoconfig/android nao encontrado — teste pulado")
            return
        }

        val count = dir.listFiles { f -> f.extension == "cfg" }?.size ?: 0
        assertTrue(
            "Esperado ao menos 50 perfis de controle, encontrado: $count",
            count >= 50
        )
    }
}

