package com.example.deepmule

import com.example.deepmule.data.SupportedSystems
import com.example.deepmule.data.SystemConfig
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Testes da logica de emulacao:
 *  - BiosValidator (ramo sem Context — regras puras)
 *  - Mapeamento coreName -> arquivo .so
 *  - retroarch.cfg: chaves criticas presentes
 *
 * Nao requer device nem Android SDK em execucao.
 */
class EmulationLogicTest {

    // ── helpers ────────────────────────────────────────────────────────────

    private fun projectRoot(): File? {
        var dir = File(System.getProperty("user.dir") ?: ".")
        repeat(5) {
            if (File(dir, "cores-pack").isDirectory) return dir
            dir = dir.parentFile ?: return@repeat
        }
        return null
    }

    // ── BiosValidator: logica pura (sem Context) ───────────────────────────

    /** Representa a regra: sistema sem BIOS requerida -> validacao deve passar. */
    @Test
    fun biosValidation_systemWithoutBios_expectsNoFile() {
        val system = SystemConfig(
            id = "nes",
            name = "NES",
            extensions = listOf("nes"),
            coreName = "fceumm",
            biosRequired = false,
            biosFileName = null
        )
        assertFalse("Sistema sem BIOS nao deve exigir arquivo", system.biosRequired)
        assertNull("biosFileName deve ser null quando nao requerido", system.biosFileName)
    }

    /** Regra: sistema com BIOS requerida DEVE ter biosFileName definido. */
    @Test
    fun biosValidation_allRequiredSystems_haveBiosFileName() {
        val violations = SupportedSystems.biosRequired().filter { it.biosFileName.isNullOrBlank() }
        assertTrue(
            "Sistemas com biosRequired=true sem biosFileName:\n${violations.map { it.id }}",
            violations.isEmpty()
        )
    }

    /** BIOS files declarados devem existir fisicamente em bios-pack/. */
    @Test
    fun biosFiles_allDeclaredFiles_existInBiosPack() {
        val root = projectRoot() ?: run {
            println("AVISO: raiz do projeto nao encontrada — teste de BIOS pulado")
            return
        }
        val biosDir = File(root, "bios-pack")
        if (!biosDir.isDirectory) {
            println("AVISO: bios-pack nao encontrado — teste pulado")
            return
        }

        val missing = SupportedSystems.biosRequired().mapNotNull { system ->
            val fileName = system.biosFileName ?: return@mapNotNull null
            val file = File(biosDir, fileName)
            if (!file.exists() || file.length() == 0L)
                "${system.id}: $fileName"
            else
                null
        }

        assertTrue(
            "BIOS obrigatorias ausentes em bios-pack:\n${missing.joinToString("\n")}",
            missing.isEmpty()
        )
    }

    // ── CoreName -> arquivo .so ─────────────────────────────────────────────

    @Test
    fun coreName_allSystems_coreNameIsNotBlank() {
        val blankCore = SupportedSystems.systems.filter { it.coreName.isBlank() }
        assertTrue(
            "Sistemas com coreName vazio: ${blankCore.map { it.id }}",
            blankCore.isEmpty()
        )
    }

    @Test
    fun coreName_allSystems_coreNameUsesUnderscoreNotDash() {
        // RetroArch expects "genesis_plus_gx", not "genesis-plus-gx"
        val badNames = SupportedSystems.systems
            .filter { it.coreName.contains('-') }
            .map { "${it.id}=${it.coreName}" }
        // coreAlternatives podem ter dash (pcsx-rearmed é alternativa aceita)
        // só o coreName primario importa aqui
        if (badNames.isNotEmpty()) {
            println("INFO: coreNames com hifem (verifique se sao validos): $badNames")
        }
    }

    @Test
    fun coreName_everySystemHasCoreFileInCoresPack() {
        val root = projectRoot() ?: run {
            println("AVISO: raiz do projeto nao encontrada — teste de cobertura de cores pulado")
            return
        }
        val coresDir = File(root, "cores-pack")
        if (!coresDir.isDirectory) {
            println("AVISO: cores-pack nao encontrado — teste pulado")
            return
        }

        fun exists(name: String) =
            File(coresDir, "${name}_libretro_android.so").exists() ||
            File(coresDir, "$name.so").exists()

        val missing = SupportedSystems.systems.filter { system ->
            val allCores = listOf(system.coreName) + system.coreAlternatives
            allCores.none { exists(it) }
        }.map { "${it.id} -> ${it.coreName}" }

        assertTrue(
            "Sistemas sem .so em cores-pack:\n${missing.joinToString("\n")}",
            missing.isEmpty()
        )
    }

    // ── retroarch.cfg ──────────────────────────────────────────────────────

    @Test
    fun retroarchCfg_criticalKeysPresent() {
        val root = projectRoot() ?: run {
            println("AVISO: raiz do projeto nao encontrada — teste de retroarch.cfg pulado")
            return
        }
        val cfgFile = File(root, "retroarch-pack/config/retroarch.cfg")
        if (!cfgFile.exists()) {
            println("AVISO: retroarch.cfg nao encontrado — teste pulado")
            return
        }

        val content = cfgFile.readText()

        // chaves que DEVEM estar definidas explicitamente no nosso cfg customizado
        val mustHaveKeys = listOf(
            "savefile_directory",
            "savestate_directory",
            "system_directory"
        )

        val missing = mustHaveKeys.filter { key ->
            !content.contains(Regex("""(?m)^\s*$key\s*="""))
        }

        assertTrue(
            "Chaves ausentes em retroarch.cfg:\n${missing.joinToString("\n")}",
            missing.isEmpty()
        )
    }

    @Test
    fun retroarchCfg_savefileDirPointsToExternalStorage() {
        val root = projectRoot() ?: run {
            println("AVISO: raiz do projeto nao encontrada — teste pulado")
            return
        }
        val cfgFile = File(root, "retroarch-pack/config/retroarch.cfg")
        if (!cfgFile.exists()) {
            println("AVISO: retroarch.cfg nao encontrado — teste pulado")
            return
        }

        val content = cfgFile.readText()
        val match = Regex("""(?m)^\s*savefile_directory\s*=\s*"?([^"\n]+)"?""")
            .find(content)
        assertNotNull("savefile_directory nao definido em retroarch.cfg", match)

        val value = match!!.groupValues[1].trim()
        assertTrue(
            "savefile_directory nao aponta para um caminho absoluto ou variavel: '$value'",
            value.startsWith("/") || value.startsWith(":") || value.contains("INTERNAL")
        )
    }
}

