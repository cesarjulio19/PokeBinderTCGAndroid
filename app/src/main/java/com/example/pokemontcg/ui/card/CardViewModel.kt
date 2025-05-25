package com.example.pokemontcg.ui.card


import com.example.pokemontcg.dto.CardDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.pokemontcg.api.request.card.CardCreateData
import com.example.pokemontcg.api.request.card.CardCreateRequest
import com.example.pokemontcg.api.request.card.CardUpdateData
import com.example.pokemontcg.api.request.card.CardUpdateRequest
import okhttp3.MultipartBody


@HiltViewModel
class CardViewModel @Inject constructor(
    private val repository: CardRepository
) : ViewModel() {

    val cards: StateFlow<List<CardDto>> = repository.getAllCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCard = MutableStateFlow<CardDto?>(null)
    val selectedCard: StateFlow<CardDto?> = _selectedCard

    private val _isSuccess = MutableSharedFlow<Boolean>()
    val isSuccess: SharedFlow<Boolean> = _isSuccess

    private val _filteredCards = MutableStateFlow<List<CardDto>>(emptyList())
    val filteredCards: StateFlow<List<CardDto>> = _filteredCards

    private val _setId = MutableStateFlow<Int?>(null)

    // este flujo emitirá los PagingData tal y como los cargue el API
    val pagedCards: Flow<PagingData<CardDto>> = _setId
        .filterNotNull()
        .flatMapLatest { setId ->
            repository.getPagedCardsBySetApi(setId)
        }
        .cachedIn(viewModelScope)

    //Llama desde el Fragmento cuando el usuario elija un set.
    fun onSetSelected(setId: Int) {
        _setId.value = setId
        // si quieres sincronizar en background a Room:
        viewModelScope.launch { repository.syncCardsBySet(setId) }
    }



    fun fetchCardsBySet(setId: Int) {
        viewModelScope.launch {
            repository.getCardsBySet(setId).collect {
                _filteredCards.value = it
            }
        }
    }

    // Llamada para sincronizar con Strapi
    fun syncCardsBySet(setId: Int) {
        viewModelScope.launch {
            try {
                repository.syncCardsBySet(setId)
                fetchCardsBySet(setId)
            } catch (e: Exception) {

            }
        }
    }



    fun createCard(request: CardCreateRequest, imagePart: MultipartBody.Part?) =
        viewModelScope.launch {
            _isSuccess.emit(repository.createCardWithImage(request, imagePart))
        }

    fun updateCard(id: Int, request: CardUpdateRequest, imagePart: MultipartBody.Part?) =
        viewModelScope.launch {
            _isSuccess.emit(repository.updateCardWithImage(id, request, imagePart))
        }

    fun deleteCard(id: Int) {
        viewModelScope.launch {
            _isSuccess.emit(repository.deleteCard(id))
        }
    }
}