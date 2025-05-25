package com.example.pokemontcg.ui.auth

import android.content.SharedPreferences
import com.example.pokemontcg.api.StrapiAuthService
import com.example.pokemontcg.api.request.auth.RegisterRequest
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val service: StrapiAuthService,
    private val prefs: SharedPreferences
) {
    suspend fun register(req: RegisterRequest): Boolean {
        val resp = service.register(req)
        if (resp.isSuccessful) {
            val auth = resp.body()!!
            saveToken(auth.jwt)
            return true
        }
        return false
    }

    suspend fun login(req: RegisterRequest): Boolean {
        val resp = service.login(req)
        if (resp.isSuccessful) {
            saveToken(resp.body()!!.jwt)
            return true
        }
        return false
    }

    fun logout() {
        prefs.edit().remove("jwt").apply()
    }

    private fun saveToken(jwt: String) {
        prefs.edit().putString("jwt", jwt).apply()
    }

    fun getToken(): String? = prefs.getString("jwt", null)
}