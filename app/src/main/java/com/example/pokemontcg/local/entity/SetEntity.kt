package com.example.pokemontcg.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sets")
data class SetEntity(
    @PrimaryKey val id: Int,
    val name: String
)
