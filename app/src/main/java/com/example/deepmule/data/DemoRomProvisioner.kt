package com.example.deepmule.data

import android.content.Context
import java.io.File
import java.io.FileOutputStream

/**
 * Copia BIOS e ROMs embutidas para as pastas de execucao.
 */
object DemoRomProvisioner {

    private const val PROVISION_VERSION = 7

    fun provision(context: Context) {
        val romsDir = File(context.filesDir, "roms")
        val biosDir = File(context.filesDir, "bios")
        val coresDir = File(context.filesDir, "cores")
        val retroarchDir = File(context.filesDir, "retroarch")

        val prefs = context.getSharedPreferences("provision_prefs", Context.MODE_PRIVATE)
        if (prefs.getInt("provision_version", 0) >= PROVISION_VERSION) {
            return
        }

        clearDirectory(romsDir)
        if (!romsDir.exists()) romsDir.mkdirs()
        if (!biosDir.exists()) biosDir.mkdirs()
        clearDirectory(coresDir)
        if (!coresDir.exists()) coresDir.mkdirs()
        clearDirectory(retroarchDir)
        if (!retroarchDir.exists()) retroarchDir.mkdirs()

        // Copia BIOS dos Assets para a pasta interna
        copyAssetsFolder(context, "bios", biosDir)

        // Copia ROMs dos Assets para a pasta de ROMs
        copyAssetsFolder(context, "roms", romsDir)

        // Copia cores dos Assets para files/cores
        copyAssetsFolder(context, "cores", coresDir)

        // Copia pacotes RetroArch (info/database/thumbnails/autoconfig) para files/retroarch
        copyAssetsFolder(context, "retroarch", retroarchDir)

        prefs.edit()
            .putInt("provision_version", PROVISION_VERSION)
            .putBoolean("provisioned_v5", true)
            .apply()
    }

    private fun clearDirectory(dir: File) {
        if (!dir.exists()) {
            return
        }
        dir.listFiles()?.forEach { child ->
            child.deleteRecursively()
        }
    }

    private fun copyAssetsFolder(context: Context, assetPath: String, destinationDir: File) {
        try {
            val assets = context.assets.list(assetPath) ?: return
            if (assets.isEmpty()) {
                // É um arquivo
                copyAssetFile(context, assetPath, File(destinationDir.parentFile, File(assetPath).name))
            } else {
                // É um diretório
                if (!destinationDir.exists()) destinationDir.mkdirs()
                for (asset in assets) {
                    val fullPath = if (assetPath.isEmpty()) asset else "$assetPath/$asset"
                    val subAssets = context.assets.list(fullPath)
                    if (subAssets != null && subAssets.isNotEmpty()) {
                        copyAssetsFolder(context, fullPath, File(destinationDir, asset))
                    } else {
                        copyAssetFile(context, fullPath, File(destinationDir, asset))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun copyAssetFile(context: Context, assetPath: String, destFile: File) {
        try {
            context.assets.open(assetPath).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            // Pode ser um diretório vazio
        }
    }
}
