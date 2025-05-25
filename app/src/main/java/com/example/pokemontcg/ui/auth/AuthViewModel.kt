package com.example.pokemontcg.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokemontcg.api.request.auth.RegisterRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val personRepo: PersonRepository
): ViewModel() {

    private val _authResult = MutableLiveData<Boolean>()
    val authResult: LiveData<Boolean> = _authResult

    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            val ok = authRepo.register(RegisterRequest(username, email, password))
            if (ok) {
                // creamos también la Person
                personRepo.createPersonForCurrentUser(username, email)
            }
            _authResult.value = ok
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            val ok = authRepo.login(RegisterRequest("", email, password))
            _authResult.value = ok
        }
    }

    fun logout() {
        authRepo.logout()
    }
}