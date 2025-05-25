package com.example.pokemontcg.ui.auth

import com.example.pokemontcg.api.StrapiAuthService
import com.example.pokemontcg.api.request.person.PersonCreateData
import com.example.pokemontcg.api.request.person.PersonCreateRequest
import javax.inject.Inject

class PersonRepository @Inject constructor(
    private val auth: AuthRepository,
    private val service: StrapiAuthService
) {
    suspend fun createPersonForCurrentUser(username: String, email: String): Boolean {
        val userResp = service.me("Bearer ${auth.getToken()}")
        if (!userResp.isSuccessful) return false
        val user = userResp.body()!!

        val personReq = PersonCreateRequest(
            data = PersonCreateData(
                user = user.id,
                email = email,
                username = username
            )
        )
        val resp = service.createPerson("Bearer ${auth.getToken()}", personReq)
        return resp.isSuccessful
    }
}