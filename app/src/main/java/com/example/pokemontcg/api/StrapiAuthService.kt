package com.example.pokemontcg.api

import com.example.pokemontcg.api.request.auth.LoginRequest
import com.example.pokemontcg.api.request.auth.RegisterRequest
import com.example.pokemontcg.api.request.person.PersonCreateRequest
import com.example.pokemontcg.api.request.person.PersonUpdateRequest
import com.example.pokemontcg.api.response.auth.AuthResponse
import com.example.pokemontcg.api.response.person.PersonListResponse
import com.example.pokemontcg.api.response.person.PersonResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface StrapiAuthService {

    //registro
    @POST("auth/local/register")
    suspend fun register(@Body data: RegisterRequest): Response<AuthResponse>

    //login
    @POST("auth/local")
    suspend fun login(@Body data: LoginRequest): Response<AuthResponse>

    // crea Person
    @POST("people?populate=user")
    suspend fun createPerson(
        @Header("Authorization") bearer: String,
        @Body person: PersonCreateRequest
    ): Response<PersonResponse>

    // obtiene una persona por el userId
    @GET("people")
    suspend fun getPersonByUserId(
        @Header("Authorization") bearer: String,
        @Query("filters[user][id][\$eq]") userId: Int,
        @Query("populate") populate: String = "*"
    ): Response<PersonListResponse>

    // edita una persona
    @PUT("people/{id}")
    suspend fun updatePerson(
        @Header("Authorization") bearer: String,
        @Path("id") id: Int,
        @Body request: PersonUpdateRequest
    ): Response<PersonResponse>
}