package com.example.deepmule.emu

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.deepmule.data.GameEntity
import com.example.deepmule.ui.EmulationScreen
import kotlinx.coroutines.launch

class EmulationActivity : ComponentActivity() {

    private val viewModel: EmulationViewModel by viewModels {
        EmulationViewModel.Factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enterImmersiveMode()

        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val gamePath = intent.getStringExtra("GAME_PATH")
        val systemId = intent.getStringExtra("SYSTEM_ID")

        if (gamePath == null || systemId == null) {
            Toast.makeText(this, "Erro: Caminho do jogo não fornecido", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContent {
            EmulationScreen(
                viewModel = viewModel,
                onExit = { finish() }
            )
        }

        // Se o jogo ainda não foi carregado no ViewModel, inicia agora
        if (viewModel.currentGame.value == null) {
            val game = GameEntity(path = gamePath, title = gamePath.substringAfterLast("/"), system = systemId)
            viewModel.startGame(game)
        }
        
        // Listen for status
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.syncStatus.collect { status ->
                    if (status.contains("Erro") || status.contains("Falha")) {
                        Toast.makeText(this@EmulationActivity, status, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enterImmersiveMode()
        }
    }

    private fun enterImmersiveMode() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (viewModel.onGamepadKeyEvent(keyCode, true)) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (viewModel.onGamepadKeyEvent(keyCode, false)) return true
        return super.onKeyUp(keyCode, event)
    }
}
