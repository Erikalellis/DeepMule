package com.example.deepmule.ui.screens.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.deepmule.domain.model.Game
import com.example.deepmule.viewmodel.DetailsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    viewModel: DetailsViewModel,
    onBack: () -> Unit,
    onPlay: (Game) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val game = state.game

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(game?.title ?: "Detalhes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (game != null) {
                        IconButton(onClick = viewModel::toggleFavorite) {
                            Icon(
                                imageVector = if (game.favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = null
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Text("Carregando...", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }

        if (game == null) {
            Text("Jogo nao encontrado", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = game.cover,
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(18.dp))
            )
            Text(game.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(game.system.uppercase(), color = MaterialTheme.colorScheme.primary)
            Text("ROM: ${game.romPath}", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { onPlay(game) }, modifier = Modifier.fillMaxWidth()) {
                Text("Jogar")
            }
            FilledTonalButton(onClick = viewModel::toggleFavorite, modifier = Modifier.fillMaxWidth()) {
                Text(if (game.favorite) "Remover dos favoritos" else "Adicionar aos favoritos")
            }
        }
    }
}
