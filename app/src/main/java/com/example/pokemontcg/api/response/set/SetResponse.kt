package com.example.pokemontcg.api.response.set

data class SetResponse(
    val data: List<SetData>
)

data class SetListResponse(
    val data: List<SetData>,
    val meta: Meta?
)

data class SetItemResponse(
    val data: SetData
)

data class SetData(
    val id: Int,
    val attributes: SetAttributes
)

data class SetAttributes(
    val name: String
)

data class Meta(
    val pagination: Pagination?
)

data class Pagination(
    val page: Int,
    val pageSize: Int,
    val pageCount: Int,
    val total: Int
)
