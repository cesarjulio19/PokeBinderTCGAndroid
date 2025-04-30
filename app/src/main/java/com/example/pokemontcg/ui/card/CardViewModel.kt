package com.example.pokemontcg.ui.card


import com.example.pokemontcg.dto.CardDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokemontcg.api.request.card.CardCreateRequest
import com.example.pokemontcg.api.request.card.CardUpdateRequest


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

    fun syncFromApi() {
        viewModelScope.launch {
            try {
                repository.syncCardsFromApi()
            } catch (e: Exception) {
                // log o manejo de error opcional
            }
        }
    }

    fun fetchCardById(id: Int) {
        viewModelScope.launch {
            val card = repository.getCardById(id)
            _selectedCard.value = card
        }
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

    fun createCard(request: CardCreateRequest) {
        viewModelScope.launch {
            _isSuccess.emit(repository.createCard(request))
        }
    }

    fun updateCard(id: Int, request: CardUpdateRequest) {
        viewModelScope.launch {
            _isSuccess.emit(repository.updateCard(id, request))
        }
    }

    fun deleteCard(id: Int) {
        viewModelScope.launch {
            _isSuccess.emit(repository.deleteCard(id))
        }
    }
}