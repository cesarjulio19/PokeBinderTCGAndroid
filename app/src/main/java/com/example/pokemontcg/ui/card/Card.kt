package com.example.pokemontcg.ui.card

data class Card(
    val id: String,
    val name: String,
    val number: Int,
    val illustration: String?,
    val image: String?,
    val type: String?,
    val rarity: String?,
    val superType: String?,
    val set: Set?
)

data class Set(
    val id: String,
    val name: String
)

