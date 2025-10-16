package com.example.roamly.entity

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roamly.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

// --- ОПРЕДЕЛЕНИЯ, НЕОБХОДИМЫЕ ДЛЯ СОСТОЯНИЯ VIEWMODEL ---

// DTO для отображения списка заведений (должен соответствовать ответу сервера)
data class EstablishmentDisplayDto(
    val id: Long,
    val name: String,
    val description: String,
    val address: String,
    val status: EstablishmentStatus,
    val rating: Double,
    val dateOfCreation: String // Сервер возвращает дату как строку ISO
)

// --------------------------------------------------------

@HiltViewModel
class EstablishmentViewModel @Inject constructor(
    private val apiService: ApiService,
) : ViewModel() {

    // --- StateFlow для списка заведений пользователя ---
    private val _userEstablishments = MutableStateFlow<List<EstablishmentDisplayDto>>(emptyList())
    val userEstablishments: StateFlow<List<EstablishmentDisplayDto>> = _userEstablishments

    // --- StateFlow для индикатора загрузки ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // --- StateFlow для сообщений об ошибках ---
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage


    /**
     * Загружает список заведений, созданных указанным пользователем.
     *
     * @param userId ID пользователя, чьи заведения нужно загрузить.
     */
    fun fetchEstablishmentsByUserId(userId: Long) {
        // Если уже идет загрузка, игнорируем запрос
        if (_isLoading.value) return

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // ⭐ НОВЫЙ ВЫЗОВ API: Предполагаем, что apiService имеет метод для получения списка
                // Метод apiService.getEstablishmentsByUserId должен быть реализован в ApiService
                val list = apiService.getEstablishmentsByUserId(userId)

                withContext(Dispatchers.Main) {
                    _userEstablishments.value = list
                    Log.i("EstViewModel", "Загружено заведений: ${list.size}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val msg = "Ошибка загрузки заведений пользователя $userId: ${e.message}"
                    Log.e("EstViewModel", msg)
                    _errorMessage.value = "Не удалось загрузить ваши заведения."
                    _userEstablishments.value = emptyList() // Очищаем старый список при ошибке
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }


    /**
     * Отправляет запрос на сервер для создания нового заведения.
     *
     * @param name Название заведения.
     * @param description Описание.
     * @param address Адрес.
     * @param latitude Широта местоположения.
     * @param longitude Долгота местоположения.
     * @param createUserId ID пользователя, создавшего заведение (передается из UI).
     * @param onResult Callback: true в случае успеха, false в случае ошибки.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun createEstablishment(
        name: String,
        description: String,
        address: String,
        latitude: Double,
        longitude: Double,
        createUserId: Long,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {

            // Мы используем заглушки для полей, которые не вводит пользователь.
            val newEstablishment = EstablishmentEntity(
                id = 0, // Заглушка, реальный ID будет присвоен сервером
                name = name,
                latitude = latitude,
                longitude = longitude,
                address = address,
                description = description,
                rating = 0.0,
                status = EstablishmentStatus.PENDING_APPROVAL,
                menuId = -1,
                createdUserId = createUserId,
                dateOfCreation = "dsa"
            )

            try {
                val createdEstablishment = apiService.createEstablishment(newEstablishment)

                withContext(Dispatchers.Main) {
                    if (createdEstablishment != null) {
                        Log.i("EstCreationVM", "Заведение создано: ${createdEstablishment.name}")
                        onResult(true)
                    } else {
                        onResult(false)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("EstCreationVM", "Ошибка создания заведения: ${e.message}")
                    onResult(false)
                }
            }
        }
    }
}
