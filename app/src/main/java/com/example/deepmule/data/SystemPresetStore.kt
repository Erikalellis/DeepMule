package com.example.deepmule.data

import android.content.Context

/** Filtro de vídeo por sistema. */
enum class VideoFilter(val label: String) {
    NONE("Nenhum"),
    SCANLINES("Scanlines"),
    SMOOTH("Suavizado")
}

/** Preset de configuração para um sistema de emulação. */
data class SystemPreset(
    val systemId: String,
    val coreOverride: String? = null,
    val audioEnabled: Boolean = true,
    val videoFilter: VideoFilter = VideoFilter.NONE,
    val frameSkip: Int = 0,
    val enableRewind: Boolean = false
)

/**
 * Persiste presets por sistema no SharedPreferences.
 */
class SystemPresetStore(private val context: Context) {

    private val prefs = context.getSharedPreferences("system_presets_v1", Context.MODE_PRIVATE)

    fun getPreset(systemId: String): SystemPreset = SystemPreset(
        systemId = systemId,
        coreOverride = prefs.getString("${systemId}_core", null),
        audioEnabled = prefs.getBoolean("${systemId}_audio", true),
        videoFilter = prefs.getString("${systemId}_video_filter", null)
            ?.let { runCatching { VideoFilter.valueOf(it) }.getOrDefault(VideoFilter.NONE) }
            ?: VideoFilter.NONE,
        frameSkip = prefs.getInt("${systemId}_frame_skip", 0),
        enableRewind = prefs.getBoolean("${systemId}_rewind", false)
    )

    fun savePreset(preset: SystemPreset) {
        with(prefs.edit()) {
            if (preset.coreOverride != null)
                putString("${preset.systemId}_core", preset.coreOverride)
            else
                remove("${preset.systemId}_core")
            putBoolean("${preset.systemId}_audio", preset.audioEnabled)
            putString("${preset.systemId}_video_filter", preset.videoFilter.name)
            putInt("${preset.systemId}_frame_skip", preset.frameSkip)
            putBoolean("${preset.systemId}_rewind", preset.enableRewind)
            apply()
        }
    }

    fun resetPreset(systemId: String) {
        with(prefs.edit()) {
            listOf("_core", "_audio", "_video_filter", "_frame_skip", "_rewind")
                .forEach { remove("$systemId$it") }
            apply()
        }
    }
}

