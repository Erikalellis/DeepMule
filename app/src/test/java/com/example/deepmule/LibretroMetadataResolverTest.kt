package com.example.deepmule

import com.example.deepmule.data.LibretroDatParser
import com.example.deepmule.data.normalizeRomName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibretroMetadataResolverTest {

    @Test
    fun parseDat_extractsGameTitleByRomName() {
        val lines = sequenceOf(
            "clrmamepro (", ")",
            "game (",
            "  name \"Super Mario World\"",
            "  rom ( name \"Super Mario World (USA).sfc\" size 524288 crc deadbeef )",
            ")"
        )

        val parsed = LibretroDatParser.parse(lines)

        assertEquals(
            "Super Mario World",
            parsed["super mario world (usa).sfc"]
        )
    }

    @Test
    fun parseDat_ignoresRomEntriesWithoutExtension() {
        val lines = sequenceOf(
            "game (",
            "  name \"Arcade Sample\"",
            "  rom ( name \"rom_without_ext\" size 123 )",
            ")"
        )

        val parsed = LibretroDatParser.parse(lines)

        assertTrue(parsed.isEmpty())
    }

    @Test
    fun normalizeRomName_keepsOnlyFilenameLowercase() {
        val normalized = normalizeRomName("C:/ROMS/NES/SUPER MARIO.NES")
        assertEquals("super mario.nes", normalized)
    }
}

