package com.example.pokemontcg.api

import com.example.pokemontcg.api.request.card.CardCreateRequest
import com.example.pokemontcg.api.response.card.CardListResponse
import com.example.pokemontcg.api.response.card.CardResponse
import com.example.pokemontcg.api.request.card.CardUpdateRequest
import com.example.pokemontcg.api.request.set.SetCreateRequest
import com.example.pokemontcg.api.request.set.SetUpdateRequest
import com.example.pokemontcg.api.response.card.UploadResponse
import com.example.pokemontcg.api.response.set.SetItemResponse
import com.example.pokemontcg.api.response.set.SetListResponse
import com.example.pokemontcg.api.response.set.SetResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface StrapiApiService {

    // Cards
    @GET("cards?populate=*")
    suspend fun getAllCards(): Response<CardListResponse>

    @GET("cards/{id}")
    suspend fun getCardById(@Path("id") id: Int): Response<CardResponse>

    @GET("cards")
    suspend fun getCardsBySetPaged(
        @Query("filters[set][id][\$eq]") setId: Int,
        @Query("populate") populate: String = "*",
        @Query("pagination[page]") page: Int,
        @Query("pagination[pageSize]") pageSize: Int,
        @Query("sort") sort: String = "id:desc"
    ): Response<CardListResponse>

    @POST("cards")
    suspend fun createCard(
        @Query("populate") populate: String = "*",
        @Body card: CardCreateRequest
    ): Response<CardResponse>

    @PUT("cards/{id}")
    suspend fun updateCard(
        @Path("id") id: Int,
        @Query("populate") populate: String = "*",
        @Body card: CardUpdateRequest
    ): Response<CardResponse>

    @DELETE("cards/{id}")
    suspend fun deleteCard(@Path("id") id: Int): Response<Unit>

    @GET("cards")
    suspend fun getCardsBySetId(
        @Query("populate") populate: String = "*",
        @Query("filters[set][id][\$eq]") setId: Int
    ): Response<CardListResponse>

    // Sets
    @GET("sets")
    suspend fun getAllSets(): Response<SetListResponse>

    @POST("sets")
    suspend fun createSet(@Body request: SetCreateRequest): Response<SetItemResponse>

    @PUT("sets/{id}")
    suspend fun updateSet(@Path("id") id: Int, @Body request: SetUpdateRequest): Response<SetItemResponse>

    @DELETE("sets/{id}")
    suspend fun deleteSet(@Path("id") id: Int): Response<Unit>

    @Multipart
    @POST("upload")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part
    ): Response<List<UploadResponse>>

    }
