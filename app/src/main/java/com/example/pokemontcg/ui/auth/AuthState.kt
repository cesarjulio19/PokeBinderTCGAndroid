package com.example.pokemontcg.ui.auth

import com.example.pokemontcg.local.entity.PersonEntity
import com.example.pokemontcg.local.entity.UserEntity

/**
 * Representa el estado de la autenticación en la UI.
 */
sealed class AuthState {
    /** No se ha intentado (pantalla de login/registro en reposo). */
    object Idle : AuthState()

    /** Se está realizando el request (registro o login). */
    object Loading : AuthState()

    /** Login/registro exitoso. */
    data class Success(
        val user: UserEntity,
        val person: PersonEntity? = null
    ) : AuthState()

    /** Hubo un error (red, credenciales, validación...). */
    data class Error(val message: String) : AuthState()
}