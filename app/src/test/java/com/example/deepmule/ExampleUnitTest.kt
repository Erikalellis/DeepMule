package com.example.deepmule

import com.example.deepmule.data.SupportedSystems
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    // ─── Quantidade total de sistemas ─────────────────────────────────────────

    @Test
    fun totalSystems_is23() {
        assertEquals("Deve ter exatamente 23 sistemas cadastrados", 23, SupportedSystems.systems.size)
    }

    // ─── IDs únicos ───────────────────────────────────────────────────────────

    @Test
    fun allSystemIds_areUnique() {
        val ids = SupportedSystems.systems.map { it.id }
        assertEquals("Todos os IDs de sistema devem ser únicos", ids.distinct().size, ids.size)
    }

    // ─── getByExtension ───────────────────────────────────────────────────────

    @Test
    fun getByExtension_nes() = assertSystem("nes", "nes", "fceumm")

    @Test
    fun getByExtension_sfc_snes() = assertSystem("sfc", "snes", "snes9x")

    @Test
    fun getByExtension_smc_snes() = assertSystem("smc", "snes", "snes9x")

    @Test
    fun getByExtension_gba() = assertSystem("gba", "gba", "mgba")

    @Test
    fun getByExtension_gb() = assertSystem("gb", "gb", "gambatte")

    @Test
    fun getByExtension_gbc() = assertSystem("gbc", "gbc", "gambatte")

    @Test
    fun getByExtension_n64() = assertSystem("z64", "n64", "mupen64plus_next")

    @Test
    fun getByExtension_v64() = assertSystem("v64", "n64", "mupen64plus_next")

    @Test
    fun getByExtension_nds() = assertSystem("nds", "nds", "melonds")

    @Test
    fun getByExtension_3ds() = assertSystem("3ds", "3ds", "citra")

    @Test
    fun getByExtension_atari2600_a26() = assertSystem("a26", "atari2600", "stella")

    @Test
    fun getByExtension_atari7800() = assertSystem("a78", "atari7800", "prosystem")

    @Test
    fun getByExtension_lynx() = assertSystem("lnx", "lynx", "handy")

    @Test
    fun getByExtension_sms() = assertSystem("sms", "sms", "genesis_plus_gx")

    @Test
    fun getByExtension_gg() = assertSystem("gg", "gg", "genesis_plus_gx")

    @Test
    fun getByExtension_genesis_md() = assertSystem("md", "genesis", "genesis_plus_gx")

    @Test
    fun getByExtension_psp_cso() = assertSystem("cso", "psp", "ppsspp")

    @Test
    fun getByExtension_pce() = assertSystem("pce", "pce", "beetle_pce_fast")

    @Test
    fun getByExtension_ngp() = assertSystem("ngp", "ngp", "mednafen_ngp")

    @Test
    fun getByExtension_ngpc() = assertSystem("ngpc", "ngpc", "mednafen_ngp")

    @Test
    fun getByExtension_ws() = assertSystem("ws", "ws", "beetle_wswan")

    @Test
    fun getByExtension_wsc() = assertSystem("wsc", "wsc", "beetle_wswan")

    @Test
    fun getByExtension_arcade_zip() = assertSystem("zip", "arcade", "fbneo")

    @Test
    fun getByExtension_arcade_7z() = assertSystem("7z", "arcade", "fbneo")

    @Test
    fun getByExtension_caseInsensitive() {
        val system = SupportedSystems.getByExtension("NES")
        assertNotNull(system)
        assertEquals("nes", system?.id)
    }

    @Test
    fun getByExtension_returnsNull_forUnknown() {
        assertNull(SupportedSystems.getByExtension("xyz"))
    }

    // ─── getById ──────────────────────────────────────────────────────────────

    @Test
    fun getById_returnsCorrectSystem() {
        val system = SupportedSystems.getById("ps1")
        assertNotNull(system)
        assertEquals("pcsx_rearmed", system?.coreName)
    }

    @Test
    fun getById_returnsNull_forUnknown() {
        assertNull(SupportedSystems.getById("naoexiste"))
    }

    // ─── BIOS ─────────────────────────────────────────────────────────────────

    @Test
    fun biosRequired_containsExpectedSystems() {
        val biosIds = SupportedSystems.biosRequired().map { it.id }
        assertTrue("ps1 deve exigir BIOS", "ps1" in biosIds)
        assertTrue("segacd deve exigir BIOS", "segacd" in biosIds)
        assertTrue("lynx deve exigir BIOS", "lynx" in biosIds)
    }

    @Test
    fun biosRequired_systemsHaveBiosFileName() {
        SupportedSystems.biosRequired().forEach { system ->
            assertNotNull(
                "Sistema ${system.id} exige BIOS mas biosFileName é null",
                system.biosFileName
            )
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun assertSystem(ext: String, expectedId: String, expectedCore: String) {
        val system = SupportedSystems.getByExtension(ext)
        assertNotNull("Sistema não encontrado para extensão .$ext", system)
        assertEquals("ID incorreto para .$ext", expectedId, system?.id)
        assertEquals("Core incorreto para .$ext", expectedCore, system?.coreName)
    }
}