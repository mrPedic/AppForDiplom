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
import javax.inject.Inject

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

    // --- StateFlow для выбранного/редактируемого заведения ---
    private val _currentEstablishment = MutableStateFlow<EstablishmentDisplayDto?>(null)
    val currentEstablishment: StateFlow<EstablishmentDisplayDto?> = _currentEstablishment


    /**
     * Загружает список заведений, созданных указанным пользователем.
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

    fun fetchAllEstablishments() {
        // Если уже идет загрузка, игнорируем запрос
        if (_isLoading.value) return

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // ⭐ НОВЫЙ ВЫЗОВ API: Предполагаем, что apiService имеет метод для получения списка
                // Метод apiService.getEstablishmentsByUserId должен быть реализован в ApiService
                val list = apiService.getAllEstablishments()

                withContext(Dispatchers.Main) {
                    _userEstablishments.value = list
                    Log.i("EstViewModel", "Загружено заведений: ${list.size}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val msg = "Ошибка загрузки заведений : ${e.message}"
                    Log.e("EstViewModel", msg)
                    _errorMessage.value = "Не удалось загрузить заведения."
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
     * Выполняет поиск заведений по названию или адресу.
     *
     * @param query Строка поиска (название или адрес).
     */
    fun searchEstablishments(query: String) {
        // Мы НЕ используем _isLoading.value здесь, чтобы позволить пользователю
        // искать, пока другие фоновые операции могут выполняться.
        // Но мы устанавливаем _isLoading для этой конкретной операции.

        if (query.isBlank()) {
            // Если запрос пустой, очищаем список, чтобы не показывать старые результаты
            _userEstablishments.value = emptyList()
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val results = apiService.searchEstablishments(query)

                withContext(Dispatchers.Main) {
                    _userEstablishments.value = results // Обновляем список
                    Log.i("EstViewModel", "Найдено заведений по запросу '$query': ${results.size}")
                    _errorMessage.value = null
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val msg = "Ошибка поиска заведений: ${e.message}"
                    Log.e("EstViewModel", msg)
                    _errorMessage.value = "Ошибка при поиске заведений. Проверьте соединение."
                    _userEstablishments.value = emptyList()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    private val _pendingEstablishments = MutableStateFlow<List<EstablishmentDisplayDto>>(emptyList())
    val pendingEstablishments: StateFlow<List<EstablishmentDisplayDto>> = _pendingEstablishments

    fun fetchPendingEstablishments() {
        if (_isLoading.value) return

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = apiService.getPendingEstablishments()

                withContext(Dispatchers.Main) {
                    _pendingEstablishments.value = list
                    Log.i("AdminVM", "Загружено неодобренных заявок: ${list.size}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("AdminVM", "Ошибка загрузки неодобренных заявок: ${e.message}")
                    _errorMessage.value = "Не удалось загрузить заявки на одобрение."
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    /**
     * Изменяет статус заведения (например, одобряет или отклоняет).
     */
    fun updateEstablishmentStatus(id: Long, newStatus: EstablishmentStatus) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Вызываем API для изменения статуса
                val updated = apiService.updateEstablishmentStatus(id, newStatus.name) // Проверить сигнатуру apiService

                withContext(Dispatchers.Main) {
                    Log.i("AdminVM", "Статус заведения ${updated.name} изменен на ${updated.status}")

                    // Обновляем список, удаляя или изменяя элемент
                    _pendingEstablishments.value = _pendingEstablishments.value.filter { it.id != id }

                    // Если статус APPROVED, можно также обновить список для карты (опционально)
                    // if (newStatus == EstablishmentStatus.APPROVED) fetchAllEstablishments()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("AdminVM", "Ошибка изменения статуса: ${e.message}")
                    _errorMessage.value = "Не удалось изменить статус заведения."
                }
            }
        }
    }

    /**
     * Отправляет заведение на повторное рассмотрение, устанавливая статус PENDING_APPROVAL.
     * @param id ID заведения.
     */
    fun resubmitEstablishmentForReview(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Используем существующий метод API, передавая статус PENDING_APPROVAL
                val updated = apiService.updateEstablishmentStatus(id, EstablishmentStatus.PENDING_APPROVAL.name)

                withContext(Dispatchers.Main) {
                    Log.i("EstViewModel", "Заведение ${updated.name} отправлено на повторное рассмотрение.")

                    // Обновляем список, чтобы UI отразил новый статус (опционально, зависит от вашего общего UI/Admin flow)
                    // Если экран пользователя отображает только его заведения, лучше обновить только этот список
                    val currentList = _userEstablishments.value.map {
                        if (it.id == id) it.copy(status = EstablishmentStatus.PENDING_APPROVAL) else it
                    }
                    _userEstablishments.value = currentList
                    _errorMessage.value = null
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("EstViewModel", "Ошибка повторной отправки заведения $id: ${e.message}")
                    _errorMessage.value = "Не удалось отправить заведение на повторное рассмотрение."
                }
            }
        }
    }

    /**
     * Загружает данные заведения по его ID.
     * @param id ID заведения.
     */
    fun fetchEstablishmentById(id: Long) {
        // Мы НЕ устанавливаем _isLoading, чтобы не блокировать другие экраны,
        // но можем использовать отдельный StateFlow для UI загрузки этого экрана.
        // Для простоты будем использовать общий _isLoading.
        _isLoading.value = true
        _errorMessage.value = null
        _currentEstablishment.value = null // Очищаем предыдущее

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val entity = apiService.getEstablishmentById(id)

                withContext(Dispatchers.Main) {
                    _currentEstablishment.value = entity
                    Log.i("EstViewModel", "Загружено заведение: ${entity.name}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val msg = "Ошибка загрузки заведения $id: ${e.message}"
                    Log.e("EstViewModel", msg)
                    _errorMessage.value = "Не удалось загрузить данные заведения."
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    /**
     * Отправляет запрос на сервер для обновления существующего заведения.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun updateEstablishment(
        establishmentId: Long,
        name: String,
        description: String,
        address: String,
        latitude: Double,
        longitude: Double,
        type: TypeOfEstablishment,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {

            // Используем текущий объект EstablishmentDisplayDto для заполнения всех полей,
            // которые не меняются на экране редактирования (rating, menuId, createdUserId, status, dateOfCreation)
            val existing = _currentEstablishment.value
            if (existing == null) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Ошибка: Нет данных для обновления."
                    onResult(false)
                }
                return@launch
            }

            // Создаем сущность для отправки (EstablishmentEntity)
            val updatedEntity = EstablishmentEntity(
                id = establishmentId,
                name = name,
                latitude = latitude,
                longitude = longitude,
                address = address,
                description = description,
                rating = existing.rating,
                status = existing.status, // Оставляем текущий статус
                menuId = existing.menuId,
                createdUserId = existing.createdUserId,
                dateOfCreation = existing.dateOfCreation,
                type = type
            )

            try {
                // Вызываем новый метод API для PUT-запроса
                val updatedEstablishment = apiService.updateEstablishment(establishmentId, updatedEntity)

                withContext(Dispatchers.Main) {
                    Log.i("EstUpdateVM", "Заведение ${updatedEstablishment.name} успешно обновлено.")
                    _currentEstablishment.value = updatedEstablishment // Обновляем локальное состояние
                    _errorMessage.value = null
                    onResult(true)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("EstUpdateVM", "Ошибка обновления заведения: ${e.message}")
                    _errorMessage.value = "Не удалось обновить заведение. ${e.message}"
                    onResult(false)
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
        type: TypeOfEstablishment,
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
                dateOfCreation = "dsa",
                type = type
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
