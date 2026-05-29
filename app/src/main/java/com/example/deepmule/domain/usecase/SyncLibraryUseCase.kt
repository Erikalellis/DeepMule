package com.example.deepmule.domain.usecase

import com.example.deepmule.data.GameRepository
import com.example.deepmule.data.ImportSyncReport
import com.example.deepmule.data.RomSourceManager

class SyncLibraryUseCase(
    private val legacyRepository: GameRepository,
    private val romSourceManager: RomSourceManager
) {
    suspend operator fun invoke(): ImportSyncReport {
        romSourceManager.ensureDefaultSource()
        val sources = romSourceManager.getSources()
        if (sources.isEmpty()) return ImportSyncReport.Empty
        return legacyRepository.scanAllSourcesIncremental(sources)
    }
}

