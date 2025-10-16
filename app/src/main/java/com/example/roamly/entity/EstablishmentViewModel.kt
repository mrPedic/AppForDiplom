package com.example.roamly.entity

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roamly.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class EstablishmentViewModel @Inject constructor(
    private val apiService: ApiService,
    // ИСПРАВЛЕНИЕ: УДАЛЕНА зависимость от UserViewModel, чтобы избежать ошибки внедрения
) : ViewModel() {

    /**
     * Отправляет запрос на сервер для создания нового заведения.
     *
     * @param name Название заведения.
     * @param description Описание.
     * @param address Адрес.
     * @param latitude Широта местоположения. ⭐ ДОБАВЛЕНО
     * @param longitude Долгота местоположения. ⭐ ДОБАВЛЕНО
     * @param createUserId ID пользователя, создавшего заведение (передается из UI).
     * @param onResult Callback: true в случае успеха, false в случае ошибки.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun createEstablishment(
        name: String,
        description: String,
        address: String,
        latitude: Double, // ⭐ ДОБАВЛЕНО
        longitude: Double, // ⭐ ДОБАВЛЕНО
        createUserId: Long,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {

            // Мы используем заглушки для полей, которые не вводит пользователь.
            val newEstablishment = EstablishmentEntity(
                id = 0, // Заглушка, реальный ID будет присвоен сервером
                name = name,
                latitude = latitude, // ⭐ Используем переданную широту
                longitude = longitude, // ⭐ Используем переданную долготу
                address = address,
                description = description,
                rating = 0.0,
                dateOfCreation = LocalDate.now(),
                status = EstablishmentStatus.PENDING_APPROVAL,
                menuId = -1,
                // ⭐ ИСПРАВЛЕНИЕ: Используем переданный ID
                createUserId = createUserId
            )

            try {
                // Предполагаем, что apiService.createEstablishment доступен
                // Если RetrofitFactory.create() используется без Hilt, это может вызвать другие проблемы
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
