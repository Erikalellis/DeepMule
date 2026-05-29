package com.example.deepmule

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import com.example.deepmule.analytics.DeepMuleReporter
import com.example.deepmule.data.GameDatabase
import com.example.deepmule.data.GameRepository
import com.example.deepmule.data.GameScanner
import com.example.deepmule.data.RomSourceManager
import com.example.deepmule.data.SystemPresetStore
import java.util.Locale

class DeepMuleApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base.withPtBrLocale())
    }

    override fun onCreate() {
        super.onCreate()
        // Inicializar reporter de crashes/análises → GitHub Issues
        DeepMuleReporter.init(this)
    }

    internal val database by lazy { GameDatabase.getDatabase(this) }
    internal val repository by lazy {
        GameRepository(
            database.gameDao(),
            GameScanner(this)
        )
    }
    internal val romSourceManager by lazy { RomSourceManager(this) }
    internal val systemPresetStore by lazy { SystemPresetStore(this) }
    internal val appContainer by lazy { AppContainer.from(this) }
}

private fun Context.withPtBrLocale(): Context {
    val locale = Locale.Builder()
        .setLanguage("pt")
        .setRegion("BR")
        .build()
    Locale.setDefault(locale)

    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    config.setLocales(LocaleList(locale))

    return createConfigurationContext(config)
}
