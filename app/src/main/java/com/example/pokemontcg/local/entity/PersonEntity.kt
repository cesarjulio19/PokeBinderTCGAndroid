package com.example.pokemontcg.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "persons")
data class PersonEntity(
    @PrimaryKey val id: Int,
    val userId: Int,
    val username: String,
    val email: String,
    val adminRole: Boolean,
    val imageUrl: String?
)
