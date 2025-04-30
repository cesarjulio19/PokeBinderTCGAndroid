package com.example.pokemontcg.dto

data class CardDto(
    val id: Int,
    val name: String,
    val number: Int,
    val type: String?,
    val rarity: String?,
    val superType: String?,
    val illustration: String?,
    val image: String?,
    val setId: Int?
)
