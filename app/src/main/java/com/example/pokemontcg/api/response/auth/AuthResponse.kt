package com.example.pokemontcg.api.response.auth

// respuesta común
data class AuthResponse(
    val jwt: String,
    val user: UserData
)

data class UserData(val id: Int, val username: String, val email: String)
