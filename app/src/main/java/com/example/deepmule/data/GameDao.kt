package com.example.deepmule.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY title ASC")
    fun getAllGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE system = :system ORDER BY title ASC")
    fun getGamesBySystem(system: String): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE title LIKE '%' || :query || '%' ORDER BY title ASC")
    fun searchGames(query: String): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE favorite = 1 ORDER BY title ASC")
    fun getFavoriteGames(): Flow<List<GameEntity>>

    @Query("UPDATE games SET lastPlayed = :timestamp WHERE path = :path")
    suspend fun updateLastPlayed(path: String, timestamp: Long)

    @Query("SELECT * FROM games WHERE lastPlayed IS NOT NULL ORDER BY lastPlayed DESC LIMIT :limit")
    fun getRecentGames(limit: Int = 10): Flow<List<GameEntity>>

    @Query("SELECT path FROM games")
    suspend fun getAllPaths(): List<String>

    @Query("SELECT * FROM games")
    suspend fun getAllGamesSnapshot(): List<GameEntity>

    @Query("UPDATE games SET favorite = :favorite WHERE path = :path")
    suspend fun setFavorite(path: String, favorite: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGames(games: List<GameEntity>)

    @Query("DELETE FROM games")
    suspend fun deleteAll()

    @Query("SELECT * FROM games WHERE path = :path LIMIT 1")
    suspend fun getGameByPath(path: String): GameEntity?
}
