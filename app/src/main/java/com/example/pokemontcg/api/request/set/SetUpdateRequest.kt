package com.example.pokemontcg.api.request.set

data class SetUpdateRequest(
    val data: SetUpdateData
)

data class SetUpdateData(
    val name: String
)
