package com.example.pokemontcg.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pokemontcg.local.entity.CardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM cards")
    fun getAllCards(): Flow<List<CardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<CardEntity>)

    @Query("DELETE FROM cards")
    suspend fun clearCards()

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getCardById(id: Int): CardEntity?

    @Query("DELETE FROM cards WHERE setId = :setId")
    suspend fun deleteCardsBySet(setId: Int)

    @Query("SELECT * FROM cards WHERE setId = :setId")
    fun getCardsBySet(setId: Int): Flow<List<CardEntity>>

    @Query("DELETE FROM cards WHERE setId = :setId")
    suspend fun deleteCardsBySetId(setId: Int)

    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun deleteCardById(id: Int)

    @Query("SELECT * FROM cards WHERE setId = :setId")
    suspend fun getCardsBySetOnce(setId: Int): List<CardEntity>

    @Delete
    suspend fun deleteCards(cards: List<CardEntity>)
}