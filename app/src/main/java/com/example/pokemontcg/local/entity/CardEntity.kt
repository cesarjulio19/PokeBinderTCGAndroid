package com.example.pokemontcg.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val number: Int,
    val type: String?,
    val rarity: String?,
    val superType: String?,
    val illustration: String?,
    val image: String?,
    val setId: Int
)
