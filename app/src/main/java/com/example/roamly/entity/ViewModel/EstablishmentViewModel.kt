package com.example.roamly.entity.ViewModel

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roamly.ApiService
import com.example.roamly.classes.cl_menu.Drink
import com.example.roamly.classes.cl_menu.DrinkOption
import com.example.roamly.classes.cl_menu.Food
import com.example.roamly.classes.cl_menu.MenuOfEstablishment
import com.example.roamly.entity.BookingCreationDto
import com.example.roamly.entity.DTO.EstablishmentDisplayDto
import com.example.roamly.entity.DTO.EstablishmentMarkerDto
import com.example.roamly.entity.DTO.TableCreationDto
import com.example.roamly.entity.EstablishmentEntity
import com.example.roamly.entity.EstablishmentStatus
import com.example.roamly.entity.ReviewEntity
import com.example.roamly.entity.TableEntity
import com.example.roamly.entity.TypeOfEstablishment
import com.example.roamly.ui.screens.sealed.SaveStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    // --- StateFlow для индикатора основной загрузки (заведение, списки) ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // --- StateFlow для сообщений об ошибках ---
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // --- StateFlow для выбранного/редактируемого заведения ---
    private val _currentEstablishment = MutableStateFlow<EstablishmentDisplayDto?>(null)
    val currentEstablishment: StateFlow<EstablishmentDisplayDto?> = _currentEstablishment

    // --- StateFlow для неодобренных заведений (Admin) ---
    private val _pendingEstablishments =
        MutableStateFlow<List<EstablishmentDisplayDto>>(emptyList())
    val pendingEstablishments: StateFlow<List<EstablishmentDisplayDto>> = _pendingEstablishments

    // ================================================ //
    // ===== ПОЛЯ ДЛЯ РАБОТЫ С ОТЗЫВАМИ (Reviews) ===== //
    // ================================================ //

    private val _reviews = MutableStateFlow<List<ReviewEntity>>(emptyList())
    val reviews: StateFlow<List<ReviewEntity>> = _reviews

    private val _isReviewsLoading = MutableStateFlow(false)
    val isReviewsLoading: StateFlow<Boolean> = _isReviewsLoading

    private val _establishmentMarkers = MutableStateFlow<List<EstablishmentMarkerDto>>(emptyList())
    val establishmentMarkers: StateFlow<List<EstablishmentMarkerDto>> = _establishmentMarkers

    // ===================================================== //
    // ===== ПОЛЯ ДЛЯ РАБОТЫ С БРОНИРОВАНИЕМ (Booking) ===== //
    // ===================================================== //

    private val _tables = MutableStateFlow<List<TableEntity>>(emptyList())
    // Список всех столов заведения
    val tables: StateFlow<List<TableEntity>> = _tables

    // Список столов, доступных для бронирования на выбранную дату/время
    private val _availableTables = MutableStateFlow<List<TableEntity>>(emptyList())
    val availableTables: StateFlow<List<TableEntity>> = _availableTables

    private val _isBookingLoading = MutableStateFlow(false)
    val isBookingLoading: StateFlow<Boolean> = _isBookingLoading

    // =========================================== //
    // =========================================== //


    /**
     * Загружает список заведений, созданных указанным пользователем.
     * @param userId ID пользователя, чьи заведения нужно загрузить.
     */
    fun fetchEstablishmentsByUserId(userId: Long) {
        if (_isLoading.value) return

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
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
                    _userEstablishments.value = emptyList()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    fun fetchAllEstablishments() {
        if (_isLoading.value) return

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = apiService.getAllEstablishments()

                withContext(Dispatchers.Main) {
                    _userEstablishments.value = list
                    Log.i("EstViewModel", "Загружено всех заведений: ${list.size}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val msg = "Ошибка загрузки всех заведений: ${e.message}"
                    Log.e("EstViewModel", msg)
                    _errorMessage.value = "Не удалось загрузить заведения."
                    _userEstablishments.value = emptyList()
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
     * @param query Строка поиска (название или адрес).
     */
    fun searchEstablishments(query: String) {
        if (query.isBlank()) {
            _userEstablishments.value = emptyList()
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val results = apiService.searchEstablishments(query)

                withContext(Dispatchers.Main) {
                    _userEstablishments.value = results
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
                val updated = apiService.updateEstablishmentStatus(id, newStatus.name)

                withContext(Dispatchers.Main) {
                    Log.i(
                        "AdminVM",
                        "Статус заведения ${updated.name} изменен на ${updated.status}"
                    )
                    _pendingEstablishments.value =
                        _pendingEstablishments.value.filter { it.id != id }
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
     * Отправляет заведение на повторное рассмотрение.
     * @param id ID заведения.
     */
    fun resubmitEstablishmentForReview(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updated = apiService.updateEstablishmentStatus(id, EstablishmentStatus.PENDING_APPROVAL.name)

                withContext(Dispatchers.Main) {
                    Log.i(
                        "EstViewModel",
                        "Заведение ${updated.name} отправлено на повторное рассмотрение."
                    )
                    val currentList = _userEstablishments.value.map {
                        if (it.id == id) it.copy(status = EstablishmentStatus.PENDING_APPROVAL) else it
                    }
                    _userEstablishments.value = currentList
                    _errorMessage.value = null
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("EstViewModel", "Ошибка повторной отправки заведения $id: ${e.message}")
                    _errorMessage.value =
                        "Не удалось отправить заведение на повторное рассмотрение."
                }
            }
        }
    }

    /**
     * Загружает данные заведения по его ID.
     * @param id ID заведения.
     */
    fun fetchEstablishmentById(id: Long) {
        _isLoading.value = true
        _errorMessage.value = null
        _currentEstablishment.value = null

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
        photoBase64s: List<String> = emptyList(),
        operatingHoursString: String? = null,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {

            val existing = _currentEstablishment.value
            if (existing == null) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Ошибка: Нет данных для обновления."
                    onResult(false)
                }
                return@launch
            }

            val updatedEntity = EstablishmentEntity(
                id = establishmentId,
                name = name,
                latitude = latitude,
                longitude = longitude,
                address = address,
                description = description,
                rating = existing.rating,
                status = existing.status,
                menuId = existing.menuId,
                createdUserId = existing.createdUserId,
                dateOfCreation = existing.dateOfCreation,
                type = type,
                photoBase64s = photoBase64s,
                operatingHoursString = operatingHoursString
            )

            try {
                // Предполагается, что ваш EstablishmentEntity обновлен для использования operatingHoursString
                val updatedEstablishment = apiService.updateEstablishment(establishmentId, updatedEntity)

                withContext(Dispatchers.Main) {
                    Log.i(
                        "EstUpdateVM",
                        "Заведение ${updatedEstablishment.name} успешно обновлено."
                    )
                    // Здесь нужно преобразовать EstablishmentEntity в EstablishmentDisplayDto,
                    // если ваш API возвращает Entity, но StateFlow ждет DTO. (Предполагаем, что EstablishmentEntity - это уже DTO)
                    _currentEstablishment.value = updatedEstablishment
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
        photoBase64s: List<String> = emptyList(),
        operatingHoursString: String? = null,
        tables: List<TableCreationDto> = emptyList(),
        onResult: (Boolean) -> Unit
    ) {
        // Весь код выполняется в фоновом потоке
        viewModelScope.launch(Dispatchers.IO) {

            var creationSuccessful = false
            var savedId: Long? = null

            // --- ШАГ 1: Создание заведения ---
            try {
                // Создание DTO внутри try-блока для поимки ошибок сериализации
                val newEstablishment = EstablishmentEntity(
                    id = 0,
                    name = name,
                    latitude = latitude,
                    longitude = longitude,
                    address = address,
                    description = description,
                    rating = 0.0,
                    status = EstablishmentStatus.PENDING_APPROVAL,
                    menuId = -1,
                    createdUserId = createUserId,
                    dateOfCreation = "", // Исправлено: пустая строка, чтобы избежать ошибки маппинга даты
                    type = type,
                    photoBase64s = photoBase64s,
                    operatingHoursString = operatingHoursString,
                )

                val createdEstablishment = apiService.createEstablishment(newEstablishment)

                if (createdEstablishment?.id != null) {
                    savedId = createdEstablishment.id
                    creationSuccessful = true
                    Log.i("EstCreationVM", "Заведение создано с ID: $savedId")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("EstCreationVM", "Ошибка создания заведения (Шаг 1): ${e.message}", e)
                    onResult(false)
                }
                return@launch
            }

            // --- ШАГ 2: Сохранение столиков (если они есть) ---
            if (creationSuccessful && savedId != null) {
                if (tables.isNotEmpty()) {
                    try {
                        Log.d("EstCreationVM", "Попытка сохранить ${tables.size} столиков для ID: $savedId")
                        tables.forEachIndexed { index, dto ->
                            Log.d("EstCreationVM", "Столик ${index + 1}: ${dto.name}, Capacity: ${dto.maxCapacity}")
                        }

                        val tablesResult = saveTables(savedId, tables)

                        if (tablesResult.isSuccess) {
                            Log.i("EstCreationVM", "Столики успешно сохранены для ID: $savedId")
                            withContext(Dispatchers.Main) { onResult(true) }
                        } else {
                            val errorMsg = tablesResult.exceptionOrNull()?.message
                            // ⭐ КРИТИЧЕСКИЙ ЛОГ
                            Log.e("EstCreationVM", "ОШИБКА ШАГА 2 (Tables): $errorMsg", tablesResult.exceptionOrNull())
                            withContext(Dispatchers.Main) {
                                _errorMessage.value = "Ошибка сохранения столиков: ${errorMsg ?: "Неизвестно"}"
                                onResult(false)
                            }
                        }
                    } catch (e: Exception) {
                        // Ловим любые необработанные исключения внутри Шага 2 (например, корутины)
                        withContext(Dispatchers.Main) {
                            Log.e("EstCreationVM", "КРИТИЧЕСКАЯ ОШИБКА ШАГА 2: ${e.message}", e)
                            _errorMessage.value = "Критическая ошибка при сохранении столиков."
                            onResult(false)
                        }
                    }
                } else {
                    // Успешно создано заведение без столиков
                    withContext(Dispatchers.Main) { onResult(true) }
                }
            } else {
                // Если Шаг 1 не удался
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    /**
     * Отправляет новый отзыв.
     */
    fun submitReview(
        establishmentId: Long,
        userId: Long,
        rating: Float,
        reviewText: String,
        photoBase64: String? = null,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (rating < 1f || rating > 5f || reviewText.isBlank()) {
            onResult(false, "Пожалуйста, укажите оценку (1-5) и текст отзыва.")
            return
        }

        if (userId < 1) {
            onResult(false, "Пользователь не авторизован или имеет неверный ID.")
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        val reviewEntity = ReviewEntity(
            establishmentId = establishmentId,
            createdUserId = userId,
            rating = rating,
            reviewText = reviewText,
            photoBase64 = photoBase64
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiService.createReview(reviewEntity)

                withContext(Dispatchers.Main) {
                    Log.i("ReviewVM", "Отзыв успешно создан.")
                    // Перезагружаем отзывы, чтобы обновить вкладку
                    fetchReviewsForEstablishment(establishmentId)
                    onResult(true, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMsg =
                        "Не удалось отправить отзыв: ${e.message ?: "Неизвестная ошибка"}"
                    Log.e("ReviewVM", errorMsg)
                    val displayMsg =
                        e.message?.substringAfter("HTTP 400 ") ?: "Ошибка при отправке отзыва."
                    _errorMessage.value = displayMsg
                    onResult(false, displayMsg)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    /**
     * Загружает список отзывов для заведения.
     */
    fun fetchReviewsForEstablishment(establishmentId: Long) {
        if (_isReviewsLoading.value) return

        _isReviewsLoading.value = true
        // Очищаем ошибки, связанные с отзывами
        if (_errorMessage.value == "Не удалось загрузить отзывы.") {
            _errorMessage.value = null
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Предполагаем, что apiService.getReviewsByEstablishmentId реализован
                val list = apiService.getReviewsByEstablishmentId(establishmentId)

                withContext(Dispatchers.Main) {
                    _reviews.value = list
                    Log.i("ReviewVM", "Загружено отзывов для $establishmentId: ${list.size}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("ReviewVM", "Ошибка загрузки отзывов: ${e.message}")
                    _errorMessage.value = "Не удалось загрузить отзывы."
                    _reviews.value = emptyList()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isReviewsLoading.value = false
                }
            }
        }
    }

    suspend fun saveTables(
        establishmentId: Long,
        tables: List<TableCreationDto>
    ): Result<List<TableEntity>> {
        return try {
            // ⭐ Шаг 1: Вызов API
            val response = apiService.createTables(establishmentId, tables)

            // ⭐ Шаг 2: Проверка успеха
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Log.i("TableVM", "Столики успешно получены (Count: ${body.size})")
                    Result.success(body)
                } else {
                    // Сервер вернул 200 OK, но тело пустое (проблема с сериализацией на сервере?)
                    val errorMsg = "HTTP ${response.code()}: Тело ответа успешно, но пустое."
                    Log.e("TableVM", errorMsg)
                    Result.failure(Exception(errorMsg))
                }
            } else {
                // Сервер вернул код ошибки (4xx или 5xx)
                val errorBody = response.errorBody()?.string() ?: "Неизвестная ошибка тела"
                val errorMsg = "HTTP ${response.code()}: $errorBody"

                // ⭐ ЭТО ВАЖНО: Выводим полный ответ сервера в лог
                Log.e("TableVM", "Ошибка сервера при создании столиков: $errorMsg")

                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            // ⭐ Ловим IOException, SocketTimeoutException и ошибки парсинга JSON
            Log.e("TableVM", "Критическая ошибка корутины/сети/JSON при сохранении столиков: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Загружает облегченный список заведений для отображения на карте в качестве маркеров.
     */
    fun fetchEstablishmentMarkers() {
        if (_isLoading.value) return // Используем основной индикатор загрузки

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // ⭐ Предполагается, что apiService.getAllEstablishmentMarkers() реализован
                val list = apiService.getAllEstablishmentMarkers()

                withContext(Dispatchers.Main) {
                    _establishmentMarkers.value = list
                    Log.i("EstViewModel", "Загружено маркеров заведений: ${list.size}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val msg = "Ошибка загрузки маркеров заведений: ${e.message}"
                    Log.e("EstViewModel", msg)
                    _errorMessage.value = "Не удалось загрузить данные для карты."
                    _establishmentMarkers.value = emptyList()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    /**
     * Загружает все столики для указанного заведения.
     * @param establishmentId ID заведения.
     */
    fun fetchTablesByEstablishmentId(establishmentId: Long) {
        if (_isBookingLoading.value) return

        _isBookingLoading.value = true
        _errorMessage.value = null // Используем общий errorMessage

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // ⭐ Предполагаем, что в ApiService есть этот метод
                val list = apiService.getTablesByEstablishmentId(establishmentId)

                withContext(Dispatchers.Main) {
                    _tables.value = list
                    Log.i("BookingVM", "Загружено столов для $establishmentId: ${list.size}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("BookingVM", "Ошибка загрузки столов: ${e.message}")
                    _errorMessage.value = "Не удалось загрузить столики."
                    _tables.value = emptyList()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isBookingLoading.value = false
                }
            }
        }
    }

    /**
     * Загружает список доступных столов на указанную дату и время.
     * На стороне сервера нужно реализовать логику фильтрации.
     * @param establishmentId ID заведения.
     * @param dateTime Строка в формате ISO 8601 (например, "2025-10-31T18:30:00").
     */
    fun fetchAvailableTables(establishmentId: Long, dateTime: String) {
        if (_isBookingLoading.value) return

        _isBookingLoading.value = true
        _availableTables.value = emptyList()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // ⭐ Предполагаем, что в ApiService есть этот метод
                val list = apiService.getAvailableTables(establishmentId, dateTime)

                withContext(Dispatchers.Main) {
                    _availableTables.value = list
                    Log.i("BookingVM", "Найдено доступных столов: ${list.size}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("BookingVM", "Ошибка проверки доступности столов: ${e.message}")
                    _errorMessage.value = "Ошибка проверки доступности столов."
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isBookingLoading.value = false
                }
            }
        }
    }

    /**
     * Отправляет запрос на создание бронирования.
     */
    fun submitBooking(
        establishmentId: Long,
        userId: Long,
        tableId: Long,
        dateTime: String, // ISO 8601 (Время начала)
        durationMinutes: Long, // ⭐ ДОБАВЛЕН НОВЫЙ ПАРАМЕТР: Длительность брони
        onResult: (Boolean, String?) -> Unit
    ) {
        if (userId < 1 || tableId < 1) {
            onResult(false, "Неверный ID пользователя или столика.")
            return
        }

        _isBookingLoading.value = true
        _errorMessage.value = null

        // ⭐ 1. СОЗДАНИЕ DTO ДЛЯ ОТПРАВКИ
        // Используем BookingCreationDto (или аналогичный), чтобы включить длительность.
        val bookingDto = BookingCreationDto(
            establishmentId = establishmentId,
            userId = userId,
            tableId = tableId,
            startTime = dateTime, // Передаем время начала
            durationMinutes = durationMinutes // Передаем выбранную длительность
        )

        // ЛОГИРОВАНИЕ ОТПРАВЛЯЕМЫХ ДАННЫХ
        Log.d("BookingVM", "Отправка брони: Start=${bookingDto.startTime}, Duration=${bookingDto.durationMinutes}")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // ⭐ 2. ИСПОЛЬЗУЕМ DTO ВЫЗОВ В API
                // Предполагаем, что apiService.createBooking теперь принимает BookingCreationDto
                apiService.createBooking(bookingDto)

                withContext(Dispatchers.Main) {
                    Log.i("BookingVM", "Бронирование успешно создано.")
                    onResult(true, "Столик успешно забронирован.")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Улучшенная обработка для вывода сообщения
                    val baseError = e.message ?: "Неизвестная ошибка"
                    val detailedError = if (baseError.contains("HTTP 400")) {
                        baseError.substringAfter("HTTP 400 ").trim()
                    } else {
                        "Ошибка при бронировании. Проверьте подключение или логи сервера."
                    }

                    Log.e("BookingVM", "Не удалось создать бронь: $baseError")
                    _errorMessage.value = "Ошибка при бронировании."

                    onResult(false, detailedError)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isBookingLoading.value = false
                }
            }
        }
    }

    private val _saveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Idle)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus.asStateFlow()

    // 2. Функция для сброса статуса (чтобы Snackbar не висел вечно)
    fun clearSaveStatus() {
        _saveStatus.value = SaveStatus.Idle
    }

    private val deletedFoodGroupIds = mutableStateListOf<Long>()
    private val deletedFoodItemIds = mutableStateListOf<Long>()
    private val deletedDrinksGroupIds = mutableStateListOf<Long>()
    private val deletedDrinkItemIds = mutableStateListOf<Long>()

    fun processMenuChanges(menu: MenuOfEstablishment) {

        _saveStatus.value = SaveStatus.Loading

        // 1. Преобразуем SnapshotStateList в MutableList (Исправляет ClassCastException)
        val safeFoodGroups = menu.foodGroups.map { foodGroup ->
            foodGroup.copy(items = foodGroup.items.toMutableList())
        }
        val safeDrinksGroups = menu.drinksGroups.map { drinkGroup ->
            drinkGroup.copy(items = drinkGroup.items.map { drink ->
                drink.copy(options = drink.options.toMutableList())
            }.toMutableList())
        }
        val safeMenu = menu.copy(
            foodGroups = safeFoodGroups.toMutableList(),
            drinksGroups = safeDrinksGroups.toMutableList()
        )

        viewModelScope.launch {
            try {
                // -----------------------------------------------------------
                // 1. Обработка групп Еды (FoodGroup)
                // -----------------------------------------------------------
                safeMenu.foodGroups.forEach { group ->
                    // Фиктивные ID (101, 102) считаются новыми
                    val isNew = group.id == null || group.id == 101L || group.id == 102L

                    var groupCopy = group.copy()
                    val itemsToProcess: List<Food> = groupCopy.items.toList()
                    groupCopy.items = mutableListOf()


                    val processedGroup = if (isNew) {
                        // --- CREATE GROUP (POST) ---
                        groupCopy = groupCopy.copy(id = null)
                        val groupForApi = groupCopy.copy(items = mutableListOf())

                        try {
                            println("DEBUG: Sending Food Group POST: ${groupForApi.name}")
                            val newGroup = apiService.createFoodGroup(groupForApi)
                            println("DEBUG: Received Food Group ID: ${newGroup.id}")
                            group.id = newGroup.id
                            println("Created Food Group: ${newGroup.id}")
                            newGroup
                        } catch (e: Exception) {
                            println("CRITICAL ERROR IN STEP 1 (Food Group Creation): ${e.message}")
                            throw e
                        }
                    } else {
                        // --- UPDATE GROUP (PUT) ---
                        val updatedGroup = apiService.updateFoodGroup(group.id!!, groupCopy)
                        println("Updated Food Group: ${updatedGroup.id}")
                        updatedGroup
                    }

                    // -----------------------------------------------------------
                    // 2. Обработка компонентов Еды (Food)
                    // -----------------------------------------------------------
                    itemsToProcess.forEach { item ->
                        var itemCopy = item.copy()
                        itemCopy = itemCopy.copy(foodGroupId = processedGroup.id)

                        // Фиктивный ID (1) считается новым
                        val itemIsNew = item.id == null || item.id == 1L

                        if (itemIsNew) {
                            // --- CREATE ITEM (POST) ---
                            itemCopy = itemCopy.copy(id = null)
                            val newItem = apiService.createFood(itemCopy)
                            item.id = newItem.id
                            println("Created Food Item: ${newItem.id}")
                        } else {
                            // --- UPDATE ITEM (PUT) ---
                            val updatedItem = apiService.updateFood(item.id!!, itemCopy)
                            println("Updated Food Item: ${updatedItem.id}")
                        }
                    }
                }

                // -----------------------------------------------------------
                // 3. Обработка групп Напитков (DrinksGroup)
                // -----------------------------------------------------------
                safeMenu.drinksGroups.forEach { group ->
                    // Фиктивный ID: 201
                    val isNew = group.id == null || group.id == 201L

                    var groupCopy = group.copy()
                    val itemsToProcess: List<Drink> = groupCopy.items.toList()

                    groupCopy.items = mutableListOf()
                    println("DEBUG: Attemting to process Drink Group: ${group.name}, isNew: $isNew")

                    val processedGroup = if (isNew) {
                        // --- CREATE DRINK GROUP (POST) ---
                        groupCopy = groupCopy.copy(id = null)
                        val groupForApi = groupCopy.copy(items = mutableListOf())

                        try {
                            val newGroup = apiService.createDrinksGroup(groupForApi)
                            group.id = newGroup.id
                            println("Created Drink Group: ${newGroup.id}")
                            newGroup
                        } catch (e: Exception) {
                            println("CRITICAL ERROR IN STEP 3 (Drink Group Creation): ${e.message}")
                            throw e
                        }
                    } else {
                        // --- UPDATE DRINK GROUP (PUT) ---
                        val updatedGroup = apiService.updateDrinksGroup(group.id!!, groupCopy)
                        println("Updated Drink Group: ${updatedGroup.id}")
                        updatedGroup
                    }

                    // -----------------------------------------------------------
                    // 4. Обработка компонентов Напитков (Drink)
                    // -----------------------------------------------------------
                    itemsToProcess.forEach { item ->
                        println("DEBUG: Attemting to process Drink Item: ${item.name}")
                        var itemCopy = item.copy()
                        itemCopy = itemCopy.copy(drinkGroupId = processedGroup.id)

                        // Фиктивный ID: 2 (и опции 301, 302)
                        val itemIsNew = item.id == null || item.id == 2L

                        if (itemIsNew) {
                            // --- CREATE DRINK ITEM (POST) ---
                            itemCopy = itemCopy.copy(id = null)

                            val newOptions = itemCopy.options.map { option ->
                                option.copy(id = null, drinkId = null)
                            }.toMutableList()

                            itemCopy = itemCopy.copy(options = newOptions)

                            val newItem = apiService.createDrink(itemCopy)
                            item.id = newItem.id
                            println("Created Drink Item: ${newItem.id}")
                        } else {
                            // --- UPDATE DRINK ITEM (PUT) ---
                            val updatedItem = apiService.updateDrink(item.id!!, itemCopy)
                            println("Updated Drink Item: ${updatedItem.id}")
                        }
                    }
                }

                // -----------------------------------------------------------
                // 5. ⭐ ОБРАБОТКА УДАЛЕНИЙ (DELETE) ⭐
                // -----------------------------------------------------------

                // Удаляем компоненты (в первую очередь, чтобы избежать ошибок внешнего ключа)
                deletedFoodItemIds.forEach { itemId ->
                    println("DEBUG: Deleting Food Item: $itemId")
                    apiService.deleteItem(itemId, isFood = true)
                }
                deletedDrinkItemIds.forEach { itemId ->
                    println("DEBUG: Deleting Drink Item: $itemId")
                    apiService.deleteItem(itemId, isFood = false)
                }

                // Удаляем группы
                deletedFoodGroupIds.forEach { groupId ->
                    println("DEBUG: Deleting Food Group: $groupId")
                    apiService.deleteGroup(groupId, isFood = true)
                }
                deletedDrinksGroupIds.forEach { groupId ->
                    println("DEBUG: Deleting Drink Group: $groupId")
                    apiService.deleteGroup(groupId, isFood = false)
                }

                // Очищаем списки после успешной отправки
                deletedFoodGroupIds.clear()
                deletedFoodItemIds.clear()
                deletedDrinksGroupIds.clear()
                deletedDrinkItemIds.clear()

                _saveStatus.value = SaveStatus.Success

            } catch (e: Exception) {
                println("Error saving menu: HTTP 500 - ${e.message}")
            }
        }
    }

    fun trackAndDeleteGroup(groupId: Long?, isFood: Boolean, menu: MenuOfEstablishment) {
        if (groupId == null) return

        // 1. Отслеживаем реальный ID для отправки на сервер
        if (groupId > 0) { // Игнорируем временные ID (например, < 100), их не нужно удалять с сервера
            if (isFood) {
                deletedFoodGroupIds.add(groupId)
            } else {
                deletedDrinksGroupIds.add(groupId)
            }
        }

        // 2. Удаляем из локального состояния (чтобы UI обновился)
        if (isFood) {
            menu.foodGroups.removeAll { it.id == groupId }
        } else {
            menu.drinksGroups.removeAll { it.id == groupId }
        }
    }

    // Вы должны вызывать ЭТУ функцию из UI для удаления блюда/напитка
    fun trackAndDeleteItem(groupId: Long?, itemId: Long?, isFood: Boolean, menu: MenuOfEstablishment) {
        if (groupId == null || itemId == null) return

        // 1. Отслеживаем реальный ID
        if (itemId > 0) { // Игнорируем временные ID
            if (isFood) {
                deletedFoodItemIds.add(itemId)
            } else {
                deletedDrinkItemIds.add(itemId)
            }
        }

        // 2. Удаляем из локального состояния
        if (isFood) {
            menu.foodGroups.find { it.id == groupId }?.items?.removeAll { it.id == itemId }
        } else {
            menu.drinksGroups.find { it.id == groupId }?.items?.removeAll { it.id == itemId }
        }
    }

    fun deleteGroupById(groupId: Long, isFood: Boolean) {
        viewModelScope.launch {
            try {
                apiService.deleteGroup(groupId, isFood)
            } catch (e: Exception) { /* Обработка */ }
        }
    }

    fun deleteItemById(itemId: Long, isFood: Boolean) {
        viewModelScope.launch {
            try {
                apiService.deleteItem(itemId, isFood)
            } catch (e: Exception) { /* Обработка */ }
        }
    }

    private val _isMenuLoading = MutableStateFlow(false)
    val isMenuLoading: StateFlow<Boolean> = _isMenuLoading

    private val _menuOfEstablishment = MutableStateFlow<MenuOfEstablishment?>(null)
    val menuOfEstablishment: StateFlow<MenuOfEstablishment?> = _menuOfEstablishment

    private val _menuErrorMessage = MutableStateFlow<String?>(null) // Добавляем обработчик ошибок
    val menuErrorMessage: StateFlow<String?> = _menuErrorMessage

    fun fetchMenuForEstablishment(establishmentId: Long) {
        if (_isMenuLoading.value) return

        // Сбрасываем предыдущие ошибки перед началом новой загрузки
        _menuErrorMessage.value = null
        _isMenuLoading.value = true

        viewModelScope.launch {
            try {
                // ⭐ ИСПРАВЛЕНИЕ 1: Использование инжектированного члена (apiService)
                val menu = apiService.getMenuForEstablishment(establishmentId)
                _menuOfEstablishment.value = menu
            } catch (e: Exception) {
                // ⭐ ИСПРАВЛЕНИЕ 2: Обработка ошибок
                val message = "Ошибка загрузки меню: ${e.localizedMessage ?: "Неизвестная ошибка"}"
                Log.e("EstablishmentVM", message, e)

                _menuErrorMessage.value = "Не удалось загрузить меню." // Сообщение для UI
                _menuOfEstablishment.value = null
            } finally {
                _isMenuLoading.value = false
            }
        }
    }
}