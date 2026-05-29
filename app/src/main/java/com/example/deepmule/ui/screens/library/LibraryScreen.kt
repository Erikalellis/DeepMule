package com.example.deepmule.ui.screens.library

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.deepmule.domain.model.Game
import com.example.deepmule.ui.components.GameCard
import com.example.deepmule.ui.components.SystemFilterRow
import com.example.deepmule.viewmodel.LibraryScreenViewModel

@Composable
fun LibraryScreen(
    viewModel: LibraryScreenViewModel,
    onGameSelected: (Game) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
                        text = "Biblioteca",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "${state.games.size} jogos filtrados",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFBAB7CE)
                    )
                }
                IconButton(onClick = viewModel::toggleFavoritesOnly) {
                    Icon(
                        imageVector = if (state.favoritesOnly) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = null,
                        tint = if (state.favoritesOnly) Color(0xFFA6A0FF) else Color.White
                    )
                }
            }

            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.White) },
                placeholder = { Text("Buscar jogo", color = Color(0xFFBAB7CE)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFA6A0FF),
                    unfocusedBorderColor = Color(0x553F3A5F),
                    cursorColor = Color(0xFFA6A0FF)
                ),
                singleLine = true
            )

            SystemFilterRow(
                systems = state.systems,
                selected = state.selectedSystem,
                onSelect = viewModel::selectSystem
            )

            Spacer(modifier = Modifier.height(12.dp))

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFA6A0FF))
                    }
                }

                state.games.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhuma ROM encontrada com os filtros atuais.",
                            color = Color(0xFFBAB7CE),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(140.dp),
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.games, key = { it.id }) { game ->
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
}
