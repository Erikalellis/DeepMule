package com.example.deepmule

import com.example.deepmule.data.SupportedSystems
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Testa se cada sistema de SupportedSystems tem pelo menos um core .so
 * disponivel em cores-pack/ (executado localmente, sem device).
 *
 * O caminho e resolvido a partir do modulo :app usando "../cores-pack".
 */
class CoreFilesCoverageTest {

    /** Localiza a raiz do projeto navegando para cima a partir do dir de trabalho. */
    private fun findCoresPackDir(): File {
        var dir = File(System.getProperty("user.dir") ?: ".")
        repeat(5) {
            val candidate = File(dir, "cores-pack")
            if (candidate.isDirectory) return candidate
            dir = dir.parentFile ?: return@repeat
        }
        error("Diretorio cores-pack nao encontrado. user.dir=${System.getProperty("user.dir")}")
    }

    private fun soExists(coresDir: File, coreName: String): Boolean {
        return File(coresDir, "${coreName}_libretro_android.so").exists()
            || File(coresDir, "${coreName}.so").exists()
    }

    @Test
    fun allSystems_haveAtLeastOneCoreFile() {
        val coresDir = try { findCoresPackDir() } catch (e: IllegalStateException) {
            // Ambiente CI sem os artefatos — pula silenciosamente com aviso
            println("AVISO: $e — teste de cobertura de cores pulado")
            return
        }

        val missing = mutableListOf<String>()
        for (system in SupportedSystems.systems) {
            val allCores = listOf(system.coreName) + system.coreAlternatives
            val found = allCores.any { soExists(coresDir, it) }
            if (!found) {
                missing += "${system.id} [esperava: ${allCores.joinToString("|")}]"
            }
        }

        assertTrue(
            "Sistemas sem core .so em cores-pack:\n${missing.joinToString("\n")}",
            missing.isEmpty()
        )
    }

    @Test
    fun coresPackDir_isNotEmpty() {
        val coresDir = try { findCoresPackDir() } catch (e: IllegalStateException) {
            println("AVISO: $e — teste pulado")
            return
        }

        val soFiles = coresDir.listFiles { f -> f.extension == "so" } ?: emptyArray()
        assertTrue("Nenhum .so encontrado em ${coresDir.absolutePath}", soFiles.isNotEmpty())
    }
}

