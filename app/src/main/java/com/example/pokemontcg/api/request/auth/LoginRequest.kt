package com.example.pokemontcg.api.request.auth

data class LoginRequest(
    val identifier: String,
    val password: String
)
