package com.example.deepmule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.deepmule.R
import com.example.deepmule.emu.EmulationViewModel
import android.view.SurfaceHolder
import android.view.SurfaceView

@Composable
fun EmulationScreen(
    viewModel: EmulationViewModel,
    onExit: () -> Unit
) {
    val currentGame by viewModel.currentGame.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { context ->
                SurfaceView(context).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            viewModel.onSurfaceReady(holder.surface)
                        }

                        override fun surfaceChanged(
                            holder: SurfaceHolder,
                            format: Int,
                            width: Int,
                            height: Int
                        ) {
                            viewModel.onSurfaceReady(holder.surface)
                        }

                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            viewModel.onSurfaceDestroyed()
                        }
                    })
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        val nomeJogo = currentGame?.title ?: stringResource(R.string.unknown_game)
        Text(
            text = stringResource(R.string.emulating_now, nomeJogo),
            color = Color.White.copy(alpha = 0.5f),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.align(Alignment.Center)
        )

        // Overlay Controls
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { 
                    viewModel.onExit()
                    onExit() 
                }) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack, 
                        contentDescription = stringResource(R.string.exit_emulation), 
                        tint = Color.White
                    )
                }
                
                Text(
                    text = currentGame?.title ?: "",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )

                IconButton(onClick = { viewModel.togglePause() }) {
                    Icon(
                        if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        contentDescription = if (isPaused) stringResource(R.string.resume_emulation) else stringResource(R.string.pause_emulation),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom Controls (Quick Save/Load)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ControlAction(
                    Icons.Rounded.Save, 
                    stringResource(R.string.save_state)
                ) { viewModel.saveState(1) }
                
                ControlAction(
                    Icons.Rounded.SettingsBackupRestore, 
                    stringResource(R.string.load_state)
                ) { viewModel.loadState(1) }
            }
        }

        // Virtual Pad Overlay (Simplified)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        ) {
            Text(
                stringResource(R.string.virtual_controls_overlay),
                color = Color.White.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun ControlAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.2f),
            contentColor = Color.White
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.padding(12.dp).size(24.dp)
            )
        }
        Text(text = label, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}
