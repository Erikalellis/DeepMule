package com.example.deepmule.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.deepmule.domain.model.Game

@Composable
fun PlayerScreen(
    game: Game?,
    onLaunchEmulation: (Game) -> Unit,
    isPreparingCore: Boolean = false,
    coreStatusMessage: String? = null,
    launchError: String? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF05050D), Color(0xFF120F27), Color(0xFF0B1020))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (game == null) {
            Text("Nao foi possivel carregar o jogo", color = Color.White)
            return@Box
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "CONSOLE MODE",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFA6A0FF),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = game.title,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Black
            )
            Text(
                text = game.system.uppercase(),
                color = Color(0xFFBAB7CE),
                style = MaterialTheme.typography.titleMedium
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .border(1.dp, Color(0x553F3A5F), RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x332F2B4A))
            ) {
                if (!game.cover.isNullOrBlank()) {
                    AsyncImage(
                        model = game.cover,
                        contentDescription = game.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Sem capa", color = Color.White)
                    }
                }
            }

            Text(
                text = "ROM: ${game.romPath}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9E9BB7),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = { onLaunchEmulation(game) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isPreparingCore,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C49FF))
            ) {
                Text(
                    if (isPreparingCore) "PREPARANDO CORE..." else "INICIAR EMULACAO",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            if (isPreparingCore) {
                CircularProgressIndicator(color = Color(0xFFA6A0FF))
            }

            if (!coreStatusMessage.isNullOrBlank()) {
                Text(
                    text = coreStatusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFBAB7CE),
                    textAlign = TextAlign.Center
                )
            }

            if (!launchError.isNullOrBlank()) {
                Text(
                    text = launchError,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF8A80),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

