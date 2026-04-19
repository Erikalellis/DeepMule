package com.example.deepmule

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import com.example.deepmule.analytics.DeepMuleReporter
import com.example.deepmule.data.GameDatabase
import com.example.deepmule.data.GameRepository
import com.example.deepmule.data.GameScanner
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

    val database by lazy { GameDatabase.getDatabase(this) }
    val repository by lazy {
        GameRepository(
            database.gameDao(),
            GameScanner(this)
        )
    }
}

private fun Context.withPtBrLocale(): Context {
    val locale = Locale.Builder()
        .setLanguage("pt")
        .setRegion("BR")
        .build()
    Locale.setDefault(locale)

    val config = Configuration(resources.configuration)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))
    } else {
        config.setLocale(locale)
    }

    return createConfigurationContext(config)
}

