package com.example.deepmule.data

/**
 * Configuração de um sistema de emulação.
 *
 * Inspirado no Lemuroid (Swordfish90/Lemuroid), adaptado para pt-BR.
 *
 * @param id           Identificador único interno (snake_case)
 * @param name         Nome de exibição em português brasileiro
 * @param extensions   Extensões de arquivo reconhecidas pelo scanner
 * @param coreName     Nome do core Libretro
 * @param biosRequired Indica se o sistema exige arquivo de BIOS
 * @param biosFileName Nome do arquivo de BIOS esperado (quando aplicável)
 */
data class SystemConfig(
    val id: String,
    val name: String,
    val extensions: List<String>,
    val coreName: String,
    val biosRequired: Boolean = false,
    val biosFileName: String? = null
)

object SupportedSystems {

    val systems = listOf(

        // ─── Atari ────────────────────────────────────────────────────────────
        SystemConfig(
            id = "atari2600",
            name = "Atari 2600 (A26)",
            extensions = listOf("a26", "bin", "rom"),
            coreName = "stella"
        ),
        SystemConfig(
            id = "atari7800",
            name = "Atari 7800 (A78)",
            extensions = listOf("a78"),
            coreName = "prosystem"
        ),
        SystemConfig(
            id = "lynx",
            name = "Atari Lynx",
            extensions = listOf("lnx", "lynx"),
            coreName = "handy",
            biosRequired = true,
            biosFileName = "lynxboot.img"
        ),

        // ─── Nintendo portátil ────────────────────────────────────────────────
        SystemConfig(
            id = "gb",
            name = "Game Boy (GB)",
            extensions = listOf("gb"),
            coreName = "gambatte"
        ),
        SystemConfig(
            id = "gbc",
            name = "Game Boy Color (GBC)",
            extensions = listOf("gbc"),
            coreName = "gambatte"
        ),
        SystemConfig(
            id = "gba",
            name = "Game Boy Advance (GBA)",
            extensions = listOf("gba"),
            coreName = "mgba"
        ),
        SystemConfig(
            id = "nds",
            name = "Nintendo DS (NDS)",
            extensions = listOf("nds"),
            coreName = "melonds"
        ),
        SystemConfig(
            id = "3ds",
            name = "Nintendo 3DS",
            extensions = listOf("3ds", "3dsx", "cia"),
            coreName = "citra"
        ),

        // ─── Nintendo console ─────────────────────────────────────────────────
        SystemConfig(
            id = "nes",
            name = "Nintendo (NES)",
            extensions = listOf("nes"),
            coreName = "fceumm"
        ),
        SystemConfig(
            id = "snes",
            name = "Super Nintendo (SNES)",
            extensions = listOf("sfc", "smc"),
            coreName = "snes9x"
        ),
        SystemConfig(
            id = "n64",
            name = "Nintendo 64 (N64)",
            extensions = listOf("n64", "z64", "v64"),
            coreName = "mupen64plus_next"
        ),

        // ─── Sega ─────────────────────────────────────────────────────────────
        SystemConfig(
            id = "sms",
            name = "Sega Master System (SMS)",
            extensions = listOf("sms"),
            coreName = "genesis_plus_gx"
        ),
        SystemConfig(
            id = "gg",
            name = "Sega Game Gear (GG)",
            extensions = listOf("gg"),
            coreName = "genesis_plus_gx"
        ),
        SystemConfig(
            id = "genesis",
            name = "Sega Genesis / Mega Drive",
            extensions = listOf("md", "gen", "smd"),
            coreName = "genesis_plus_gx"
        ),
        SystemConfig(
            id = "segacd",
            name = "Sega CD / Mega CD",
            extensions = listOf("cue", "iso", "chd"),
            coreName = "genesis_plus_gx",
            biosRequired = true,
            biosFileName = "bios_CD_U.bin"
        ),

        // ─── Sony ─────────────────────────────────────────────────────────────
        SystemConfig(
            id = "ps1",
            name = "PlayStation (PSX)",
            extensions = listOf("cue", "iso", "pbp", "chd"),
            coreName = "pcsx_rearmed",
            biosRequired = true,
            biosFileName = "scph1001.bin"
        ),
        SystemConfig(
            id = "psp",
            name = "PlayStation Portable (PSP)",
            extensions = listOf("iso", "cso", "pbp"),
            coreName = "ppsspp"
        ),

        // ─── NEC ──────────────────────────────────────────────────────────────
        SystemConfig(
            id = "pce",
            name = "PC Engine / TurboGrafx-16 (PCE)",
            extensions = listOf("pce", "cue", "chd"),
            coreName = "beetle_pce_fast"
        ),

        // ─── SNK ──────────────────────────────────────────────────────────────
        SystemConfig(
            id = "ngp",
            name = "Neo Geo Pocket (NGP)",
            extensions = listOf("ngp"),
            coreName = "mednafen_ngp"
        ),
        SystemConfig(
            id = "ngpc",
            name = "Neo Geo Pocket Color (NGPC)",
            extensions = listOf("ngc", "ngpc"),
            coreName = "mednafen_ngp"
        ),

        // ─── Bandai ───────────────────────────────────────────────────────────
        SystemConfig(
            id = "ws",
            name = "WonderSwan (WS)",
            extensions = listOf("ws"),
            coreName = "beetle_wswan"
        ),
        SystemConfig(
            id = "wsc",
            name = "WonderSwan Color (WSC)",
            extensions = listOf("wsc"),
            coreName = "beetle_wswan"
        ),

        // ─── Arcade ───────────────────────────────────────────────────────────
        SystemConfig(
            id = "arcade",
            name = "Arcade (FinalBurn Neo)",
            extensions = listOf("zip", "7z"),
            coreName = "fbneo"
        )
    )

    /** Retorna o sistema pelo id. */
    fun getById(id: String): SystemConfig? =
        systems.find { it.id == id }

    /** Retorna o sistema pelo nome de exibicao. */
    fun getByName(name: String): SystemConfig? =
        systems.find { it.name == name }

    /** Retorna o sistema compatível com a extensão (sem ponto, case-insensitive). */
    fun getByExtension(ext: String): SystemConfig? =
        systems.find { it.extensions.contains(ext.lowercase()) }

    /** Retorna lista de sistemas que precisam de BIOS. */
    fun biosRequired(): List<SystemConfig> =
        systems.filter { it.biosRequired }
}
