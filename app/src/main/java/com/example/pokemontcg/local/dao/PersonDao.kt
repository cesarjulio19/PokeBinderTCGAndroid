package com.example.pokemontcg.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pokemontcg.local.entity.PersonEntity

@Dao
interface PersonDao {
    //inserta persona
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(person: PersonEntity)
    // busca por userId
    @Query("SELECT * FROM persons WHERE userId = :userId LIMIT 1")
    suspend fun findByUserId(userId: Int): PersonEntity?
    //busca por id
    @Query("SELECT * FROM persons WHERE id = :userId LIMIT 1")
    suspend fun findById(userId: Int): PersonEntity?
}