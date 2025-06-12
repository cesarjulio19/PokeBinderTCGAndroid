package com.example.pokemontcg.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.pokemontcg.local.entity.SetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SetDao {
    //obtiene todos los sets
    @Query("SELECT * FROM sets")
    fun getAllSets(): Flow<List<SetEntity>>
   //obtiene todos los ids
    @Query("SELECT id FROM sets")
    suspend fun getAllSetIdsOnce(): List<Int>
    @Query("SELECT MAX(id) FROM sets")
    suspend fun getMaxId(): Int?
    //inserta set
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: SetEntity)
    // inserta una lista de sets
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSets(sets: List<SetEntity>)
    //modifica un set
    @Update
    suspend fun updateSet(set: SetEntity)
    //elimina un set
    @Delete
    suspend fun deleteSet(set: SetEntity)
    //elimina todos los sets
    @Query("DELETE FROM sets")
    suspend fun deleteAllSets()
    //elimina set por id
    @Query("DELETE FROM sets WHERE id = :id")
    suspend fun deleteSetById(id: Int)
}