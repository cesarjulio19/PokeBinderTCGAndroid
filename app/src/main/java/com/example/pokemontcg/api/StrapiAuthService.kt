package com.example.pokemontcg.api

import com.example.pokemontcg.api.request.auth.RegisterRequest
import com.example.pokemontcg.api.request.person.PersonCreateRequest
import com.example.pokemontcg.api.response.auth.AuthResponse
import com.example.pokemontcg.api.response.auth.UserData
import com.example.pokemontcg.api.response.person.PersonResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface StrapiAuthService {
    @POST("auth/local/register")
    suspend fun register(@Body body: RegisterRequest): Response<AuthResponse>

    @POST("auth/local")
    suspend fun login(@Body body: RegisterRequest /* usa email+password */): Response<AuthResponse>

    // crea tu Person
    @POST("people")
    suspend fun createPerson(
        @Header("Authorization") bearer: String,
        @Body person: PersonCreateRequest
    ): Response<PersonResponse>

    @GET("users/me")
    suspend fun me(@Header("Authorization") bearer: String): Response<UserData>
}