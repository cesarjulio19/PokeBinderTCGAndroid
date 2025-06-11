package com.example.pokemontcg.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pokemontcg.local.entity.CardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {

    //obtiene todas las cartas
    @Query("SELECT * FROM cards")
    fun getAllCards(): Flow<List<CardEntity>>
    //inserta una lista de cartas
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<CardEntity>)

    //elimina todas las cartas
    @Query("DELETE FROM cards")
    suspend fun clearCards()
    //obtiene carta por id
    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getCardById(id: Int): CardEntity?
    //elimina las cartas de un set
    @Query("DELETE FROM cards WHERE setId = :setId")
    suspend fun deleteCardsBySet(setId: Int)
    //obtiene cartas por set
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

    @Query(" SELECT * FROM cards WHERE setId = :setId ORDER BY id DESC ")
    fun pagingSourceBySet(setId: Int): PagingSource<Int, CardEntity>
}