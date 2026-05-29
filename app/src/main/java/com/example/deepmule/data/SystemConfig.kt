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
    val coreAlternatives: List<String> = emptyList(),
    val biosRequired: Boolean = false,
    val biosFileName: String? = null,
    val folderAliases: List<String> = emptyList()
)

object SupportedSystems {

    val systems = listOf(

        // ─── Atari ────────────────────────────────────────────────────────────
        SystemConfig(
            id = "atari2600",
            name = "Atari 2600 (A26)",
            extensions = listOf("a26", "bin", "rom"),
            coreName = "stella",
            folderAliases = listOf("atari2600", "roms atari", "roms atari 2600", "atari 2600")
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
            coreName = "mgba",
            folderAliases = listOf("gba", "roms gameboy advance", "gameboy advance")
        ),
        SystemConfig(
            id = "nds",
            name = "Nintendo DS (NDS)",
            extensions = listOf("nds"),
            coreName = "melonds",
            coreAlternatives = listOf("desmume")
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
            coreName = "fceumm",
            folderAliases = listOf("nes", "roms nintendo nes", "nintendo nes")
        ),
        SystemConfig(
            id = "snes",
            name = "Super Nintendo (SNES)",
            extensions = listOf("sfc", "smc"),
            coreName = "snes9x",
            folderAliases = listOf("snes", "rom snes", "roms snes", "super nintendo")
        ),
        SystemConfig(
            id = "n64",
            name = "Nintendo 64 (N64)",
            extensions = listOf("n64", "z64", "v64", "zip"),
            coreName = "mupen64plus_next",
            coreAlternatives = listOf("mupen64plus_next_gles3", "mupen64plus_next_gles2"),
            folderAliases = listOf("n64", "roms nintendo 64", "nintendo 64")
        ),

        // ─── Sega ─────────────────────────────────────────────────────────────
        SystemConfig(
            id = "sms",
            name = "Sega Master System (SMS)",
            extensions = listOf("sms"),
            coreName = "genesis_plus_gx",
            folderAliases = listOf("sms", "roms sega master system", "sega master system")
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
            coreName = "genesis_plus_gx",
            folderAliases = listOf(
                "genesis",
                "mega drive",
                "sega genesis",
                "roms sega mega drive sega genesis",
                "roms sega mega drive",
                "roms sega genesis"
            )
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
            extensions = listOf("cue", "iso", "pbp", "chd", "zip"),
            coreName = "pcsx_rearmed",
            coreAlternatives = listOf("pcsx_rearmed_gles", "pcsx-rearmed"),
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
            coreName = "mednafen_pce_fast",
            coreAlternatives = listOf("mednafen_pce")
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

        // ─── Bandai ──────────────────────────────────────────────────────────────
        SystemConfig(
            id = "ws",
            name = "WonderSwan (WS)",
            extensions = listOf("ws"),
            coreName = "mednafen_wswan"
        ),
        SystemConfig(
            id = "wsc",
            name = "WonderSwan Color (WSC)",
            extensions = listOf("wsc", "pc2"),
            coreName = "mednafen_wswan"
        ),


        // ─── Arcade ───────────────────────────────────────────────────────────
        SystemConfig(
            id = "arcade",
            name = "Arcade (FinalBurn Neo)",
            extensions = listOf("zip", "7z"),
            coreName = "fbneo",
            folderAliases = listOf("arcade", "fbneo", "roms neo geo", "neo geo")
        )
    )

    /** Retorna o sistema pelo id. */
    fun getById(id: String): SystemConfig? =
        systems.find { it.id == id }

    /** Retorna o sistema pelo nome de exibicao. */
    fun getByName(name: String): SystemConfig? =
        systems.find { it.name == name }

    /** Retorna o sistema compatível com a extensão (sem ponto, case-insensitive). */
    fun getByExtension(ext: String): SystemConfig? {
        val normalizedExt = ext.lowercase()
        if (normalizedExt == "zip" || normalizedExt == "7z") {
            return getById("arcade")
        }
        return systems.find { it.extensions.contains(normalizedExt) }
    }

    /** Resolve sistema com base no caminho da ROM e, opcionalmente, na extensao. */
    fun getByPathHint(pathHint: String, extension: String? = null): SystemConfig? {
        val normalizedPath = normalizeHint(pathHint)
        val normalizedExt = extension?.lowercase()
        val genericExtensions = setOf("zip", "7z", "bin", "rom", "img")

        val bestMatch = systems.mapNotNull { system ->
            val extensionMatches = normalizedExt == null ||
                normalizedExt in system.extensions ||
                normalizedExt in genericExtensions

            if (!extensionMatches) {
                return@mapNotNull null
            }

            val matchedHint = system.searchHints()
                .map(::normalizeHint)
                .filter { hint -> hint.isNotBlank() && normalizedPath.contains(hint) }
                .maxByOrNull { it.length }
                ?: return@mapNotNull null

            system to matchedHint.length
        }.maxByOrNull { it.second }

        return bestMatch?.first ?: normalizedExt?.let(::getByExtension)
    }

    /** Retorna lista de sistemas que precisam de BIOS. */
    fun biosRequired(): List<SystemConfig> =
        systems.filter { it.biosRequired }

    private fun SystemConfig.searchHints(): List<String> = buildList {
        if (id.length >= 3) add(id)
        addAll(folderAliases)
    }

    private fun normalizeHint(value: String): String =
        value.lowercase()
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
}
