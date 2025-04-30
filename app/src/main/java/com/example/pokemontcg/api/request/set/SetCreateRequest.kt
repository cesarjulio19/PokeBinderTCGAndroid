package com.example.pokemontcg.api.request.set

data class SetCreateRequest(
    val data: SetCreateData
)

data class SetCreateData(
    val name: String
)
