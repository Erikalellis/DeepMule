package com.example.deepmule.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.security.MessageDigest

/**
 * Resolve capa local a partir do pacote de thumbnails oficial do libretro.
 * Retorna path absoluto para ser usado direto pelo Coil.
 */
class GameArtworkResolver(context: Context) {
    private val thumbnailsRoot = File(context.filesDir, "retroarch/thumbnails")
    private val coverCacheDir = File(context.filesDir, "covers").apply { if (!exists()) mkdirs() }
    private val fallbackIndex by lazy { buildFallbackIndex() }

    suspend fun resolveBoxArt(system: SystemConfig, title: String, gameId: String): String? = withContext(Dispatchers.IO) {
        val cachedCover = File(coverCacheDir, "${hashedId(gameId)}.jpg")
        if (cachedCover.exists() && cachedCover.canRead()) {
            return@withContext cachedCover.absolutePath
        }

        val cleanTitle = normalizeTitle(title)
        if (cleanTitle.isBlank()) return@withContext null

        val playlistCandidates = playlistDirsFor(system)
        val namedBoxarts = listOf("Named_Boxarts", "Named_Snaps", "Named_Titles")
        val fileCandidates = listOf("$cleanTitle.png", "$cleanTitle.jpg", "$cleanTitle.jpeg")

        playlistCandidates.forEach { playlist ->
            namedBoxarts.forEach { bucket ->
                fileCandidates.forEach { fileName ->
                    val candidate = File(thumbnailsRoot, "$playlist/$bucket/$fileName")
                    if (candidate.exists()) return@withContext candidate.absolutePath
                }
            }
        }

        fallbackIndex[cleanTitle]?.let { localFallback ->
            return@withContext localFallback
        }

        val remote = tryDownloadRemoteCover(system, cleanTitle, cachedCover)
        return@withContext remote?.absolutePath
    }

    private fun buildFallbackIndex(): Map<String, String> {
        if (!thumbnailsRoot.exists()) return emptyMap()

        val map = HashMap<String, String>()
        thumbnailsRoot.walkTopDown()
            .maxDepth(5)
            .filter { it.isFile }
            .filter {
                val ext = it.extension.lowercase()
                ext == "png" || ext == "jpg" || ext == "jpeg"
            }
            .forEach { file ->
                val key = normalizeTitle(file.nameWithoutExtension)
                if (key.isNotBlank() && !map.containsKey(key)) {
                    map[key] = file.absolutePath
                }
            }
        return map
    }

    private fun playlistDirsFor(system: SystemConfig): List<String> {
        val aliases = mapOf(
            "nes" to listOf("Nintendo - Nintendo Entertainment System"),
            "snes" to listOf("Nintendo - Super Nintendo Entertainment System"),
            "n64" to listOf("Nintendo - Nintendo 64"),
            "gb" to listOf("Nintendo - Game Boy"),
            "gbc" to listOf("Nintendo - Game Boy Color"),
            "gba" to listOf("Nintendo - Game Boy Advance"),
            "nds" to listOf("Nintendo - Nintendo DS"),
            "sms" to listOf("Sega - Master System - Mark III"),
            "gg" to listOf("Sega - Game Gear"),
            "genesis" to listOf("Sega - Mega Drive - Genesis"),
            "segacd" to listOf("Sega - Mega-CD - Sega CD"),
            "ps1" to listOf("Sony - PlayStation"),
            "psp" to listOf("Sony - PlayStation Portable"),
            "lynx" to listOf("Atari - Lynx"),
            "atari2600" to listOf("Atari - 2600"),
            "atari7800" to listOf("Atari - 7800"),
            "pce" to listOf("NEC - PC Engine - TurboGrafx 16"),
            "arcade" to listOf("FBNeo - Arcade Games", "MAME")
        )

        val byId = aliases[system.id].orEmpty()
        return (byId + listOf(system.name)).distinct()
    }

    private fun normalizeTitle(title: String): String =
        title.trim()
            .replace(Regex("\\[[^\\]]*]"), " ")
            .replace(Regex("\\([^)]*(usa|europe|japan|rev|beta|proto|hack)[^)]*\\)", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("\\.(zip|7z|smc|sfc|gba|gb|gbc|nes|n64|z64|v64|iso|cue|chd|pbp)$", RegexOption.IGNORE_CASE), " ")
            .replace(':', ' ')
            .replace('/', ' ')
            .replace('\\', ' ')
            .replace('*', ' ')
            .replace('?', ' ')
            .replace('"', ' ')
            .replace('<', ' ')
            .replace('>', ' ')
            .replace('|', ' ')
            .replace(Regex("\\s+"), " ")

    private fun tryDownloadRemoteCover(system: SystemConfig, title: String, destination: File): File? {
        val remoteCandidates = playlistDirsFor(system).flatMap { playlist ->
            listOf(
                "https://thumbnails.libretro.com/$playlist/Named_Boxarts/$title.png",
                "https://thumbnails.libretro.com/$playlist/Named_Boxarts/$title.jpg",
                "https://thumbnails.libretro.com/$playlist/Named_Titles/$title.png"
            )
        }

        for (url in remoteCandidates) {
            val downloaded = runCatching {
                val connection = URL(url).openConnection().apply {
                    connectTimeout = 2000
                    readTimeout = 4000
                }
                connection.getInputStream().use { input ->
                    val bitmap = BitmapFactory.decodeStream(input) ?: return@runCatching null
                    destination.outputStream().use { output ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output)
                    }
                }
                destination
            }.getOrNull()

            if (downloaded != null && downloaded.exists() && downloaded.length() > 0) {
                return downloaded
            }
        }
        return null
    }

    private fun hashedId(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(value.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }.take(16)
    }
}

