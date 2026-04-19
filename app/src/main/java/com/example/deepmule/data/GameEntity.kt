package com.example.deepmule.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val path: String,
    val title: String,
    val system: String,
    val boxArtUrl: String? = null,
    val lastPlayed: Long? = null,
    val favorite: Boolean = false
)
