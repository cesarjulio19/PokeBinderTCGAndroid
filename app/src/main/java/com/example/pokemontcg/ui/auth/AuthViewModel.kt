package com.example.pokemontcg.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokemontcg.api.request.auth.LoginRequest
import com.example.pokemontcg.api.request.auth.RegisterRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepo: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun login(request: LoginRequest) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading


            val result = authRepo.login(request)

            if (result.isSuccess) {
                val user = authRepo.getLoggedInUser()
                if (user != null) {
                    // Si existe, cargamos también la persona asociada (o null si no existe)
                    val person = authRepo.getPersonByUserId(user.id)
                    _authState.value = AuthState.Success(user, person)
                } else {
                    _authState.value = AuthState.Error("No se encontró usuario local.")
                }
            } else {

                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Error al iniciar sesión")
            }
        }
    }

    fun register(request: RegisterRequest) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            val result = authRepo.register(request)
            if (result.isSuccess) {
                viewModelScope.launch {
                    val user = authRepo.getLoggedInUser()!!
                    val person = authRepo.getPersonByUserId(user.id)
                    _authState.value = AuthState.Success(user, person)
                }
            } else {
                val message = result.exceptionOrNull()?.message ?: "Error inesperado"
                _authState.value = AuthState.Error(message)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepo.logout()
            _authState.value = AuthState.Idle
        }
    }
}

