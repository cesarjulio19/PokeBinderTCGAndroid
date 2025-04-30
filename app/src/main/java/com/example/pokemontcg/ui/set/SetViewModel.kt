package com.example.pokemontcg.ui.set

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokemontcg.local.entity.SetEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetViewModel @Inject constructor(
    private val repository: SetRepository
) : ViewModel() {

    val sets: StateFlow<List<SetEntity>> = repository.getAllSets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun refresh() {
        viewModelScope.launch {
            repository.refreshSetsFromApi()
        }
    }

    fun createSet(name: String) {
        viewModelScope.launch {
            repository.createSet(name)
        }
    }

    fun updateSet(id: Int, name: String) {
        viewModelScope.launch {
            repository.updateSet(id, name)
        }
    }

    fun deleteSet(id: Int) {
        viewModelScope.launch {
            repository.deleteSet(id)
        }
    }
}