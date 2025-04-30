package com.example.pokemontcg.api.request.card

data class CardUpdateRequest(
    val data: CardUpdateData
)

data class CardUpdateData(
    val name: String,
    val number: Int,
    val type: String?,
    val rarity: String?,
    val superType: String?,
    val set: Int
)
