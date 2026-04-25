package com.example.deepmule.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.deepmule.domain.model.Game
import com.example.deepmule.ui.components.GameCard
import com.example.deepmule.ui.components.SystemFilterRow
import com.example.deepmule.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onGameSelected: (Game) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedSystem by viewModel.selectedSystem.collectAsStateWithLifecycle()

    val games = if (selectedSystem == null) {
        state.allGames
    } else {
        state.allGames.filter { it.system == selectedSystem }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F0F1A), Color(0xFF1A1033))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DeepMule",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "${state.allGames.size} jogos disponiveis",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFBAB7CE)
                    )
                }
                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }

            if (state.recentGames.isNotEmpty()) {
                Text(
                    text = "Jogados recentemente",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.recentGames.size) { index ->
                        val game = state.recentGames[index]
                        Card(
                            onClick = {
                                viewModel.onGameOpened(game)
                                onGameSelected(game)
                            },
                            modifier = Modifier.size(width = 220.dp, height = 72.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0x332F2B4A)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FolderOpen,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Column {
                                    Text(game.title, maxLines = 1, color = Color.White)
                                    Text(
                                        game.system.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFA6A0FF)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            SystemFilterRow(
                systems = state.systems,
                selected = selectedSystem,
                onSelect = viewModel::selectSystem
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(140.dp),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(games, key = { it.id }) { game ->
                    GameCard(
                        game = game,
                        onClick = {
                            viewModel.onGameOpened(game)
                            onGameSelected(game)
                        },
                        onToggleFavorite = { favorite -> viewModel.setFavorite(game, favorite) }
                    )
                }
            }
        }
    }
}

