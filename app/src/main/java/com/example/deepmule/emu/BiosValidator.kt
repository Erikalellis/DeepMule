package com.example.deepmule.emu

import android.content.Context
import com.example.deepmule.data.SystemConfig
import java.io.File

/**
 * Valida BIOS obrigatoria antes de iniciar a sessao de emulacao.
 */
object BiosValidator {

    private fun biosDir(context: Context): File = File(context.filesDir, "bios")

    fun validate(context: Context, system: SystemConfig): Result<Unit> = runCatching {
        if (!system.biosRequired) {
            return@runCatching Unit
        }

        val biosName = requireNotNull(system.biosFileName) {
            "Sistema ${system.name} exige BIOS, mas sem nome de arquivo configurado"
        }

        val file = File(biosDir(context), biosName)
        require(file.exists()) {
            "BIOS ausente para ${system.name}: coloque ${biosName} em ${biosDir(context).absolutePath}"
        }
        require(file.canRead()) { "BIOS sem permissao de leitura: ${file.absolutePath}" }
    }
}

