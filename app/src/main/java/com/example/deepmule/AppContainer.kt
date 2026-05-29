package com.example.deepmule

import com.example.deepmule.data.GameRepository
import com.example.deepmule.data.RomSourceManager
import com.example.deepmule.data.repository.DefaultGameRepository
import com.example.deepmule.domain.repository.GameRepository as DomainGameRepository
import com.example.deepmule.domain.usecase.GetGameByIdUseCase
import com.example.deepmule.domain.usecase.MarkGameOpenedUseCase
import com.example.deepmule.domain.usecase.ObserveGamesUseCase
import com.example.deepmule.domain.usecase.ObserveRecentGamesUseCase
import com.example.deepmule.domain.usecase.SetFavoriteUseCase
import com.example.deepmule.domain.usecase.SyncLibraryUseCase

class AppContainer(
    private val legacyRepository: GameRepository,
    private val romSourceManager: RomSourceManager,
    domainRepository: DomainGameRepository
) {
    val observeGamesUseCase = ObserveGamesUseCase(domainRepository)
    val observeRecentGamesUseCase = ObserveRecentGamesUseCase(domainRepository)
    val getGameByIdUseCase = GetGameByIdUseCase(domainRepository)
    val setFavoriteUseCase = SetFavoriteUseCase(domainRepository)
    val markGameOpenedUseCase = MarkGameOpenedUseCase(domainRepository)
    val syncLibraryUseCase = SyncLibraryUseCase(legacyRepository, romSourceManager)

    companion object {
        fun from(application: DeepMuleApplication): AppContainer {
            val domainRepository = DefaultGameRepository(application.database.gameDao())
            return AppContainer(
                legacyRepository = application.repository,
                romSourceManager = application.romSourceManager,
                domainRepository = domainRepository
            )
        }
    }
}

