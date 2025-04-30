package com.example.pokemontcg.api.request.card

data class CardCreateRequest(
    val data: CardCreateData
)

data class CardCreateData(
    val name: String,
    val number: Int,
    val image: String?,
    val type: String?,
    val rarity: String?,
    val superType: String?,
    val set: Int
)
