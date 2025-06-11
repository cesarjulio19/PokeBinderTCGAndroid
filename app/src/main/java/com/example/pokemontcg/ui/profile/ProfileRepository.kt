package com.example.pokemontcg.ui.profile

import android.content.SharedPreferences
import android.util.Log
import com.example.pokemontcg.api.StrapiApiService
import com.example.pokemontcg.api.StrapiAuthService
import com.example.pokemontcg.api.request.person.PersonUpdateRequest
import com.example.pokemontcg.dto.PersonDto
import com.example.pokemontcg.local.dao.PersonDao
import com.example.pokemontcg.mapper.PersonMapper.toDomain
import com.example.pokemontcg.mapper.PersonMapper.toEntity
import okhttp3.MultipartBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val apiAuth: StrapiAuthService,
    private val apiStrapi: StrapiApiService,
    private val prefs: SharedPreferences,
    private val personDao: PersonDao
) {
    suspend fun fetchMyPerson(userId: Int): PersonDto? {
        val token = prefs.getString("jwt", "") ?: ""
        val bearer = "Bearer $token"
        return try {
            val resp = apiAuth.getPersonByUserId(bearer, userId)
            if (resp.isSuccessful) {
                val dto = resp.body()?.data?.firstOrNull()?.toDomain()
                // Guarda en caché local
                dto?.let { personDao.insert(it.toEntity()) }
                dto
            } else {
                // HTTP error → fallback a local
                personDao.findByUserId(userId)?.toDomain()
            }
        } catch (io: IOException) {
            // No hay red → fallback a local
            personDao.findByUserId(userId)?.toDomain()
        }
    }

    suspend fun updatePerson(id:Int,
                             request: PersonUpdateRequest,
                             imagePart: MultipartBody.Part?): Boolean {
        val photoId = imagePart?.let { part ->
            apiStrapi.uploadImage(part).let { if (!it.isSuccessful) return false; it.body()!![0].id }
        }
        val reqWithPhoto = request.data.copy(image = photoId).let(::PersonUpdateRequest)
        val token = prefs.getString("jwt", "") ?: ""
        val bearer = "Bearer $token"
        val resp = apiAuth.updatePerson(bearer,id, reqWithPhoto)
        return resp.isSuccessful
    }
}