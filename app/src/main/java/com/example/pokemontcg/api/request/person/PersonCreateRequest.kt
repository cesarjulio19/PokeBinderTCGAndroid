package com.example.pokemontcg.api.request.person

data class PersonCreateData(
    val user: Int,
    val adminRole: Boolean = false,
    val email: String,
    val username: String,
    val image: Int? = null  // id del media en Strapi
)
data class PersonCreateRequest(val data: PersonCreateData)
