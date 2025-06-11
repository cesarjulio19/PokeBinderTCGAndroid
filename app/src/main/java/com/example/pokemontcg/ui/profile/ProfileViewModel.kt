package com.example.pokemontcg.ui.profile

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokemontcg.R
import com.example.pokemontcg.api.request.person.PersonUpdateData
import com.example.pokemontcg.api.request.person.PersonUpdateRequest
import com.example.pokemontcg.dto.PersonDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repo: ProfileRepository
): ViewModel() {

    private val _person = MutableStateFlow<PersonDto?>(null)
    val person: StateFlow<PersonDto?> = _person

    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents
    //obtiene el person por userId
    fun loadProfile(id: Int) = viewModelScope.launch {
        repo.fetchMyPerson(id)?.let { _person.value = it }

    }
    //edita el perfil
    fun updateProfile(id: Int, username: String, imagePart: MultipartBody.Part?) =
        viewModelScope.launch {
            val ok = repo.updatePerson(id,
                PersonUpdateRequest(PersonUpdateData(username, null)), imagePart
            )
            if (ok) {
                _uiEvents.emit(UiEvent.ShowMessage(R.string.perfil_actualizado))
                loadProfile(id)
            } else {
                _uiEvents.emit(UiEvent.ShowMessage(R.string.error_perfil_actualizado))
            }
        }

    sealed class UiEvent {
        data class ShowMessage(@StringRes val resId: Int): UiEvent()
    }
}