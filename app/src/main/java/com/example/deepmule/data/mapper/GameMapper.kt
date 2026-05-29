package com.example.deepmule.data.mapper

import com.example.deepmule.data.GameEntity
import com.example.deepmule.domain.model.Game

fun GameEntity.toDomain(): Game = Game(
    id = path,
    title = title,
    system = system,
    cover = boxArtUrl,
    romPath = path,
    lastPlayed = lastPlayed,
    favorite = favorite
)

