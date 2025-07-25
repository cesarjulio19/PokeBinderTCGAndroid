package com.example.pokemontcg.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pokemontcg.local.entity.UserEntity

@Dao
interface UserDao {

    //inserta user
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)
    // busca por username
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun findByUsername(username: String): UserEntity?
    //busca por email
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?
    //busca por id
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findById(id: Int): UserEntity?


}