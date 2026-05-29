package com.example.deepmule.domain.model

data class Game(
    val id: String,
    val title: String,
    val system: String,
    val cover: String?,
    val romPath: String,
    val lastPlayed: Long?,
    val favorite: Boolean
)

