package com.example.pokemontcg.api.request.auth

// request para registro
data class RegisterRequest(val username: String,
                           val email: String,
                           val password: String)
