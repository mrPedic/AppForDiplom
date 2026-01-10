// UserEstablishmentsViewModel.kt
package com.example.roamly.entity.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roamly.manager.PinnedEstablishmentsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserEstablishmentsViewModel @Inject constructor(
    private val pinnedRepository: PinnedEstablishmentsRepository
) : ViewModel() {

    private val _pinnedEstablishments = MutableStateFlow<Set<Long>>(emptySet())
    val pinnedEstablishments: StateFlow<Set<Long>> = _pinnedEstablishments.asStateFlow()

    init {
        loadPinnedEstablishments()
    }

    private fun loadPinnedEstablishments() {
        viewModelScope.launch {
            pinnedRepository.getPinnedEstablishmentsFlow().collectLatest { pinnedIds ->
                _pinnedEstablishments.value = pinnedIds
            }
        }
    }

    fun togglePin(establishmentId: Long) {
        viewModelScope.launch {
            pinnedRepository.togglePin(establishmentId)
        }
    }

    fun isPinned(establishmentId: Long): Boolean {
        return _pinnedEstablishments.value.contains(establishmentId)
    }
}