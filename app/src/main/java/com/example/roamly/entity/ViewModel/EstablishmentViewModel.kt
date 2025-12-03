package com.example.roamly.entity.ViewModel

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roamly.ApiService
import com.example.roamly.classes.cl_menu.Drink
import com.example.roamly.classes.cl_menu.Food
import com.example.roamly.classes.cl_menu.MenuOfEstablishment
import com.example.roamly.entity.BookingCreationDto
import com.example.roamly.entity.DTO.EstablishmentDisplayDto
import com.example.roamly.entity.DTO.EstablishmentFavoriteDto
import com.example.roamly.entity.DTO.EstablishmentMarkerDto
import com.example.roamly.entity.DTO.EstablishmentSearchResultDto
import com.example.roamly.entity.DTO.EstablishmentUpdateRequest
import com.example.roamly.entity.DTO.TableCreationDto
import com.example.roamly.entity.EstablishmentEntity
import com.example.roamly.entity.EstablishmentStatus
import com.example.roamly.entity.ReviewEntity
import com.example.roamly.entity.TableEntity
import com.example.roamly.entity.TypeOfEstablishment
import com.example.roamly.entity.toDisplayDto
import com.example.roamly.manager.SearchHistoryManager
import com.example.roamly.ui.screens.establishment.toJsonString
import com.example.roamly.ui.screens.establishment.toMap
import com.example.roamly.ui.screens.sealed.SaveStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import javax.inject.Inject


@HiltViewModel
class EstablishmentViewModel @Inject constructor(
    private val apiService: ApiService,
    private val searchHistoryManager: SearchHistoryManager,
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

    // =========================================== //
    // ===== ПОЛЯ ДЛЯ РАБОТЫ С КАРТОЙ (Map) ===== //
    // =========================================== //

    private val _establishmentMarkers = MutableStateFlow<List<EstablishmentMarkerDto>>(emptyList())
    val establishmentMarkers: StateFlow<List<EstablishmentMarkerDto>> = _establishmentMarkers

    // Потоки для виджета деталей на карте
    private val _selectedEstablishment = MutableStateFlow<EstablishmentDisplayDto?>(null)
    val selectedEstablishment: StateFlow<EstablishmentDisplayDto?> = _selectedEstablishment.asStateFlow()

    private val _isDetailWidgetVisible = MutableStateFlow(false)
    val isDetailWidgetVisible: StateFlow<Boolean> = _isDetailWidgetVisible.asStateFlow()


    // ===================================================== //
    // ===== ПОЛЯ ДЛЯ РАБОТЫ С БРОНИРОВАНИЕМ (Booking) ===== //
    // ===================================================== //

    private val _tables = MutableStateFlow<List<TableEntity>>(emptyList())

    // Список столов, доступных для бронирования на выбранную дату/время
    private val _availableTables = MutableStateFlow<List<TableEntity>>(emptyList())
    val availableTables: StateFlow<List<TableEntity>> = _availableTables

    private val _isBookingLoading = MutableStateFlow(false)
    val isBookingLoading: StateFlow<Boolean> = _isBookingLoading

    // ============================================== //
    // ===== ПОЛЯ ДЛЯ РАБОТЫ С ПОИСКОМ (Search) ===== //
    // ============================================== //

    // ⭐ 1. ОПРЕДЕЛЯЕМ ПОЛЯ ДЛЯ ПОИСКА (ОДИН РАЗ)

    // Поток для текстового запроса
    private val _searchQueryFlow = MutableStateFlow("")

    // Поток для выбранных фильтров (типов)
    private val _selectedTypesFlow = MutableStateFlow(emptySet<TypeOfEstablishment>())
    val selectedTypes: StateFlow<Set<TypeOfEstablishment>> = _selectedTypesFlow.asStateFlow()

    // Поток для результатов поиска
    private val _establishmentSearchResults = MutableStateFlow<List<EstablishmentSearchResultDto>>(emptyList())
    val establishmentSearchResults: StateFlow<List<EstablishmentSearchResultDto>> = _establishmentSearchResults.asStateFlow()

    // Поток для индикатора загрузки поиска
    private val _isSearchLoading = MutableStateFlow(false)
    val isSearchLoading: StateFlow<Boolean> = _isSearchLoading.asStateFlow()


    // =========================================== //
    // ===== ИНИЦИАЛИЗАЦИЯ ViewModel (init) ===== //
    // =========================================== //

    init {
        // ⭐ 2. ОБЪЕДИНЕННЫЙ INIT БЛОК ДЛЯ ПОИСКА (ОДИН РАЗ)
        // (Удалены все конфликтующие/дублирующиеся init блоки)

        combine(_searchQueryFlow, _selectedTypesFlow) { query, types ->
            query.trim() to types // Обрезаем пробелы и объединяем в Pair
        }
            .debounce(300L) // Ждем 300 мс
            .distinctUntilChanged() // Игнорируем, если ничего не изменилось
            .onEach { (query, types) ->

                // Выполняем поиск, только если запрос не пуст ИЛИ выбраны фильтры
                if (query.isBlank() && types.isEmpty()) {
                    _establishmentSearchResults.value = emptyList()
                    _isSearchLoading.value = false
                    Log.i("EstViewModel", "Запрос пуст, фильтры не выбраны.")
                } else {
                    // Если запрос пуст, но есть фильтры, query будет "" (пустая строка)
                    performSearch(query, types) // Вызов API
                }
            }
            .launchIn(viewModelScope)
    }

    // =========================================== //
    // ===== МЕТОДЫ ПОИСКА (Search Methods) ===== //
    // =========================================== //

    /**
     * Вызывается из UI (SearchScreen) при каждом изменении поля.
     * Обновляет поток запроса.
     * @param query Строка поиска.
     */
    fun searchEstablishments(query: String) {
        _searchQueryFlow.value = query
    }

    /**
     * Вызывается из UI (SearchScreen) при применении фильтров.
     * @param newTypes Новый набор выбранных типов.
     */
    fun updateFilters(newTypes: Set<TypeOfEstablishment>) {
        _selectedTypesFlow.value = newTypes
    }

    /**
     * (Private) Фактическое выполнение API-запроса поиска с фильтрами.
     */
    private fun performSearch(query: String, types: Set<TypeOfEstablishment>) {
        _isSearchLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Конвертируем Set<Enum> в List<String> для Retrofit
                val typeStrings = types.map { it.name }

                // ⭐ ВЫЗЫВАЕМ API С ФИЛЬТРАМИ
                val results = apiService.searchEstablishments(query, typeStrings)

                withContext(Dispatchers.Main) {
                    _establishmentSearchResults.value = results
                    Log.i("EstViewModel", "Найдено заведений по запросу '$query' с фильтрами [${typeStrings.joinToString()}]: ${results.size}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val msg = "Ошибка поиска заведений: ${e.message}"
                    Log.e("EstViewModel", msg)
                    _establishmentSearchResults.value = emptyList()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isSearchLoading.value = false
                }
            }
        }
    }


    // ================================================= //
    // ===== МЕТОДЫ ДЛЯ КАРТЫ (Map Detail Widget) ===== //
    // ================================================= //

    /**
     * Загружает полные данные заведения при клике на маркер.
     */
    fun loadEstablishmentDetails(id: Long) {
        _isDetailWidgetVisible.value = false
        _selectedEstablishment.value = null

        // (Можно добавить _isDetailsLoading.value = true, если есть отдельный индикатор)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Предполагаем, что apiService.getEstablishmentById возвращает EstablishmentDisplayDto
                val details: EstablishmentDisplayDto = apiService.getEstablishmentById(id)

                withContext(Dispatchers.Main) {
                    _selectedEstablishment.value = details
                    _isDetailWidgetVisible.value = true
                    Log.d("EstViewModel", "Полные данные заведения ID $id загружены: ${details.name}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("EstViewModel", "Ошибка загрузки деталей заведения ID $id: ${e.message}")
                }
            }
        }
    }

    /**
     * Закрывает виджет деталей на карте.
     */
    fun closeDetailWidget() {
        _isDetailWidgetVisible.value = false
        _selectedEstablishment.value = null
        Log.d("EstViewModel", "Виджет деталей закрыт.")
    }


    //авыэфлашлщвыфроашщплвфырпарощуцфвралвщдыфпарофывоалщзфыврапргвыфьшсщвцйтифгнам втыфшс твыфргаствлыщфраовырфшщгарвшыщгфаошвщыфрзха





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

            // 1. Создаем DTO для отправки (вместо Entity)
            val updateRequest = EstablishmentUpdateRequest(
                name = name,
                description = description,
                address = address,
                latitude = latitude,
                longitude = longitude,
                type = type,
                photoBase64s = photoBase64s,
                operatingHoursString = operatingHoursString
            )

            Log.i("EstUpdateVM", "Отправка обновления ID: $establishmentId. Lat: $latitude, Lon: $longitude. Тип: $type. Фото: ${photoBase64s.size} шт.")

            try {
                // 2. Вызываем API, ожидая Response<EstablishmentEntity>
                val response: Response<EstablishmentEntity> = apiService.updateEstablishment(
                    establishmentId,
                    updateRequest
                )

                if (response.isSuccessful) {
                    // СЛУЧАЙ 1: УСПЕХ (200 OK)
                    val updatedEntity = response.body()

                    withContext(Dispatchers.Main) {
                        if (updatedEntity != null) {
                            // ⭐ ИСПРАВЛЕНИЕ: Конвертация EstablishmentEntity в EstablishmentDisplayDto
                            val updatedDisplayDto = updatedEntity.toDisplayDto()

                            Log.i("EstUpdateVM", "Заведение ${updatedDisplayDto.name} успешно обновлено.")

                            // Присваиваем ожидаемый StateFlow тип
                            _currentEstablishment.value = updatedDisplayDto
                            _errorMessage.value = null
                            onResult(true)
                        } else {
                            // Редкий случай: 200 OK, но тело пустое
                            _errorMessage.value = "Ошибка: Сервер вернул пустой ответ при успехе."
                            onResult(false)
                        }
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val serverErrorMsg = if (errorBody != null) {
                        "Ошибка (Code ${response.code()}): $errorBody"
                    } else {
                        "Ошибка сервера (Code ${response.code()})"
                    }

                    Log.e("EstUpdateVM", serverErrorMsg)

                    withContext(Dispatchers.Main) {
                        val displayError = if (serverErrorMsg.contains("error\":")) {
                            serverErrorMsg.substringAfter("\"error\":\"").substringBefore("\"")
                        } else {
                            "Не удалось обновить. Код ошибки: ${response.code()}"
                        }
                        _errorMessage.value = displayError
                        onResult(false)
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("EstUpdateVM", "Сетевая ошибка обновления: ${e.message}")
                    _errorMessage.value = "Ошибка сети. Проверьте соединение. ${e.message}"
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

    // --- Списки для отслеживания удалений ---
    private val deletedFoodGroupIds = mutableStateListOf<Long>()
    private val deletedFoodItemIds = mutableStateListOf<Long>()
    private val deletedDrinksGroupIds = mutableStateListOf<Long>()
    private val deletedDrinkItemIds = mutableStateListOf<Long>()

    // --- Статус сохранения ---
    private val _saveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Idle)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus.asStateFlow()

    fun clearSaveStatus() {
        _saveStatus.value = SaveStatus.Idle
    }

    // --- Статус загрузки меню ---
    private val _isMenuLoading = MutableStateFlow(false)
    val isMenuLoading: StateFlow<Boolean> = _isMenuLoading

    private val _menuOfEstablishment = MutableStateFlow<MenuOfEstablishment?>(null)
    val menuOfEstablishment: StateFlow<MenuOfEstablishment?> = _menuOfEstablishment

    private val _menuErrorMessage = MutableStateFlow<String?>(null)
    val menuErrorMessage: StateFlow<String?> = _menuErrorMessage

    /**
     * Загружает меню с сервера.
     */
    fun fetchMenuForEstablishment(establishmentId: Long) {
        if (_isMenuLoading.value) return

        _menuErrorMessage.value = null
        _isMenuLoading.value = true

        viewModelScope.launch {
            try {
                Log.d("MenuVM", "Начинаем загрузку меню для ID: $establishmentId")
                val menu = apiService.getMenuForEstablishment(establishmentId)
                Log.d("MenuVM", "Меню успешно загружено. Групп еды: ${menu.foodGroups.size}")
                _menuOfEstablishment.value = menu
            } catch (e: Exception) {
                val message = "Ошибка загрузки меню: ${e.localizedMessage ?: "Неизвестная ошибка"}"
                Log.e("MenuVM", message, e)

                _menuErrorMessage.value = "Не удалось загрузить меню."
                _menuOfEstablishment.value = null
            } finally {
                _isMenuLoading.value = false
            }
        }
    }

    /**
     * Отслеживает группу для удаления.
     */
    fun trackAndDeleteGroup(groupId: Long?, isFood: Boolean, menu: MenuOfEstablishment) {
        if (groupId == null) return
        if (groupId > 0) { // Игнорируем временные ID
            if (isFood) {
                deletedFoodGroupIds.add(groupId)
            } else {
                deletedDrinksGroupIds.add(groupId)
            }
        }
        if (isFood) {
            menu.foodGroups.removeAll { it.id == groupId }
        } else {
            menu.drinksGroups.removeAll { it.id == groupId }
        }
    }

    /**
     * Отслеживает компонент для удаления.
     */
    fun trackAndDeleteItem(groupId: Long?, itemId: Long?, isFood: Boolean, menu: MenuOfEstablishment) {
        if (groupId == null || itemId == null) return
        if (itemId > 0) { // Игнорируем временные ID
            if (isFood) {
                deletedFoodItemIds.add(itemId)
            } else {
                deletedDrinkItemIds.add(itemId)
            }
        }
        if (isFood) {
            menu.foodGroups.find { it.id == groupId }?.items?.removeAll { it.id == itemId }
        } else {
            menu.drinksGroups.find { it.id == groupId }?.items?.removeAll { it.id == itemId }
        }
    }

    /**
     * Обрабатывает все изменения (Create, Update, Delete) в меню.
     */
    fun processMenuChanges(menu: MenuOfEstablishment) {

        _saveStatus.value = SaveStatus.Loading

        // Преобразуем SnapshotStateList в MutableList с помощью .toMutableList()
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
                // -----------------------------------------------------------
                deletedFoodItemIds.forEach { itemId ->
                    println("DEBUG: Deleting Food Item: $itemId")
                    apiService.deleteItem(itemId, isFood = true)
                }
                deletedDrinkItemIds.forEach { itemId ->
                    println("DEBUG: Deleting Drink Item: $itemId")
                    apiService.deleteItem(itemId, isFood = false)
                }
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

                // После сохранения, принудительно обновляем _menuOfEstablishment
                // чтобы UI (MenuDetailScreen) отобразил новые данные,
                // а MenuEditScreen в следующий раз загрузил актуальные.
                fetchMenuForEstablishment(menu.establishmentId)

                _saveStatus.value = SaveStatus.Success

            } catch (e: Exception) {
                println("Error saving menu: HTTP 500 - ${e.message}")
                // Передаем ошибку в UI
                _saveStatus.value = SaveStatus.Error(e.message ?: "Неизвестная ошибка сети")
            }
        }
    }

    private val _recentEstablishmentsFlow = MutableStateFlow<List<EstablishmentSearchResultDto>>(
        searchHistoryManager.loadHistory()
    )
    val recentEstablishments: StateFlow<List<EstablishmentSearchResultDto>> = _recentEstablishmentsFlow.asStateFlow()

    fun addRecentEstablishment(establishment: EstablishmentSearchResultDto) {
        _recentEstablishmentsFlow.update { currentList ->
            val newList = currentList.toMutableList().apply {
                removeAll { it.id == establishment.id }
                add(0, establishment)
            }
            val finalHistory = newList.take(5)

            // СОХРАНЯЕМ В ХРАНИЛИЩЕ ПОСЛЕ ОБНОВЛЕНИЯ
            viewModelScope.launch {
                searchHistoryManager.saveHistory(finalHistory)
            }

            finalHistory
        }
    }

    private val _favoriteEstablishmentsList = MutableStateFlow<List<EstablishmentFavoriteDto>>(emptyList())
    val favoriteEstablishmentsList: StateFlow<List<EstablishmentFavoriteDto>> = _favoriteEstablishmentsList.asStateFlow()

    // ----------------------------------------------------------------------
    /**
     * ✅ ИСПРАВЛЕННАЯ ЛОГИКА: Реактивно извлекает ID из полного списка избранных заведений.
     * Этот StateFlow будет автоматически обновляться, когда обновится favoriteEstablishmentsList.
     * Именно здесь происходит "проверка на наличие ID в списке" для UI.
     */
    val favoriteEstablishmentIds: StateFlow<Set<Long>> = favoriteEstablishmentsList
        .map { list ->
            // Преобразуем List<EstablishmentDisplayDto> в Set<Long> с ID заведений.
            list.map { it.id }.toSet()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )
    // ----------------------------------------------------------------------

    /**
     * Загружает список избранных заведений (DTO) для профиля.
     */
    fun fetchFavoriteEstablishmentsList(userId: Long) {
        if (userId < 1) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = apiService.getFavoriteEstablishmentsList(userId)
                withContext(Dispatchers.Main) {
                    _favoriteEstablishmentsList.value = list
                }
            } catch (e: Exception) {
                Log.e("EstViewModel", "Ошибка загрузки списка избранного: ${e.message}")
            }
        }
    }

    /**
     * Проверяет, является ли заведение избранным (локальная проверка).
     */
    fun isFavorite(establishmentId: Long): Boolean {
        Log.d("EstViewModel", "$Long")
        return favoriteEstablishmentIds.value.contains(establishmentId)
    }

    // EstablishmentViewModel.kt

    /**
     * Переключает состояние избранного и вызывает API-запрос.
     * @param establishment EstablishmentDisplayDto заведения для оптимистического обновления
     * @param userId ID текущего пользователя (передается из UI)
     */
    fun toggleFavorite(establishment: EstablishmentDisplayDto, userId: Long) {
        if (userId < 1) {
            Log.e("EstViewModel", "Пользователь не авторизован (ID: $userId).")
            _errorMessage.value = "Для добавления в избранное нужно авторизоваться."
            return
        }
        val establishmentId = establishment.id
        // Проверяем статус до оптимистичного обновления
        val isCurrentlyFavorite = isFavorite(establishmentId)
        Log.i("EstViewModel", "Текущий статус избранного: $isCurrentlyFavorite")

        val removedDto: EstablishmentFavoriteDto? = if (isCurrentlyFavorite) {
            // 1. Запоминаем объект для отката, если удаление не удастся
            _favoriteEstablishmentsList.value.find { it.id == establishmentId }
        } else null

        // 2. Оптимистичное обновление списка (меняем UI сразу)
        _favoriteEstablishmentsList.update { current ->
            if (isCurrentlyFavorite) {
                Log.i("EstViewModel", "Оптимистичное УДАЛЕНИЕ: ID $establishmentId")
                current.filter { it.id != establishmentId } // Оптимистичное УДАЛЕНИЕ
            } else {
                Log.i("EstViewModel", "Оптимистичное ДОБАВЛЕНИЕ: ID $establishmentId")
                val newDto = EstablishmentFavoriteDto(
                    establishment.id,
                    establishment.name,
                    establishment.address,
                    establishment.rating,
                    establishment.type,
                    establishment.photoBase64s.firstOrNull()
                )
                current + newDto // Оптимистичное ДОБАВЛЕНИЕ
            }
        }

        // 3. API-запрос и логика отката
        viewModelScope.launch(Dispatchers.IO) {
            val wasFavorite = isCurrentlyFavorite // Запоминаем исходный статус для отката
            try {
                if (wasFavorite) {
                    // ⭐ КЛЮЧЕВОЙ ВЫЗОВ ДЛЯ УДАЛЕНИЯ
                    apiService.removeFavoriteEstablishment(userId,establishmentId)
                    withContext(Dispatchers.Main) {
                        Log.i("EstViewModel", "Удалено из избранного ID $establishmentId (API success)")
                        _errorMessage.value = null
                    }
                } else {
                    // КЛЮЧЕВОЙ ВЫЗОВ ДЛЯ ДОБАВЛЕНИЯ
                    apiService.addFavoriteEstablishment(userId,establishmentId)
                    withContext(Dispatchers.Main) {
                        Log.i("EstViewModel", "Добавлено в избранное ID $establishmentId (API success)")
                        _errorMessage.value = null
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // ⭐ ЛОГИКА ОТКАТА (Rollback) при ошибке API
                    val action = if (wasFavorite) "удаления" else "добавления"
                    Log.e("EstViewModel", "Ошибка API при $action избранного (ID $establishmentId): ${e.message}")
                    _errorMessage.value = "Ошибка сервера. Не удалось изменить избранное."

                    _favoriteEstablishmentsList.update { current ->
                        if (wasFavorite) { // Если пытались УДАЛИТЬ, но не удалось -> ВОССТАНОВИТЬ
                            Log.d("EstViewModel", "Откат: Восстановление ID $establishmentId")
                            removedDto?.let { current + it } ?: current
                        } else { // Если пытались ДОБАВИТЬ, но не удалось -> УДАЛИТЬ оптимистично добавленный
                            Log.d("EstViewModel", "Откат: Удаление ID $establishmentId")
                            current.filter { it.id != establishmentId }
                        }
                    }
                }
            }
        }
    }

    private fun rollbackFavoriteState(establishmentId: Long, wasFavorite: Boolean, removedDto: EstablishmentFavoriteDto?) {
        _favoriteEstablishmentsList.update { current ->
            if (wasFavorite && removedDto != null) {
                current + removedDto
            } else if (!wasFavorite) {
                current.filter { it.id != establishmentId }
            } else {
                current
            }
        }
    }

    // --- Временные состояния для редактирования (чтобы избежать сброса при навигации) ---
    private val _editedName = MutableStateFlow("")
    val editedName: StateFlow<String> = _editedName.asStateFlow()

    private val _editedDescription = MutableStateFlow("")
    val editedDescription: StateFlow<String> = _editedDescription.asStateFlow()

    private val _editedAddress = MutableStateFlow("")
    val editedAddress: StateFlow<String> = _editedAddress.asStateFlow()

    private val _editedType = MutableStateFlow(TypeOfEstablishment.Restaurant)
    val editedType: StateFlow<TypeOfEstablishment> = _editedType.asStateFlow()

    private val _editedLatitude = MutableStateFlow(0.0)
    val editedLatitude: StateFlow<Double> = _editedLatitude.asStateFlow()

    private val _editedLongitude = MutableStateFlow(0.0)
    val editedLongitude: StateFlow<Double> = _editedLongitude.asStateFlow()

    private val _editedPhotoBase64s = MutableStateFlow<List<String>>(emptyList())
    val editedPhotoBase64s: StateFlow<List<String>> = _editedPhotoBase64s.asStateFlow()

    private val _editedOperatingHours = MutableStateFlow<Map<String, String>>(emptyMap())
    val editedOperatingHours: StateFlow<Map<String, String>> = _editedOperatingHours.asStateFlow()

    // Метод для инициализации edited состояний из currentEstablishment (вызывается при загрузке экрана редактирования)
    fun initEditedStates() {
        currentEstablishment.value?.let { est ->
            if (_editedName.value.isEmpty()) { // Проверка, чтобы init только если не инициализировано
                _editedName.value = est.name
                _editedDescription.value = est.description
                _editedAddress.value = est.address
                _editedType.value = est.type
                _editedLatitude.value = est.latitude
                _editedLongitude.value = est.longitude
                _editedPhotoBase64s.value = est.photoBase64s.filter { it.isNotBlank() }
                _editedOperatingHours.value = est.operatingHoursString.toMap()
                Log.i("EstViewModel", "Инициализация edited состояний для ID ${est.id}")
            }
        }
    }

    // Методы для обновления edited состояний (вызываются из UI)
    fun updateEditedName(newName: String) { _editedName.value = newName }
    fun updateEditedDescription(newDesc: String) { _editedDescription.value = newDesc }
    fun updateEditedAddress(newAddr: String) { _editedAddress.value = newAddr }
    fun updateEditedType(newType: TypeOfEstablishment) { _editedType.value = newType }
    fun updateEditedLatitude(newLat: Double) { _editedLatitude.value = newLat }
    fun updateEditedLongitude(newLon: Double) { _editedLongitude.value = newLon }
    fun updateEditedPhotos(newPhotos: List<String>) { _editedPhotoBase64s.value = newPhotos }
    fun updateEditedOperatingHours(newHours: Map<String, String>) { _editedOperatingHours.value = newHours }

    // В updateEstablishment: используйте edited состояния для отправки на сервер
// (Ваш существующий метод updateEstablishment - обновите параметры, чтобы использовать edited*)
    fun updateEstablishment(
        establishmentId: Long,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = EstablishmentUpdateRequest(
                    name = editedName.value,
                    description = editedDescription.value,
                    address = editedAddress.value,
                    latitude = editedLatitude.value,
                    longitude = editedLongitude.value,
                    type = editedType.value,
                    photoBase64s = editedPhotoBase64s.value,
                    operatingHoursString = editedOperatingHours.value.toJsonString()
                )
                val response: Response<EstablishmentEntity> = apiService.updateEstablishment(establishmentId, request)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Log.i("EstViewModel", "Заведение ID $establishmentId обновлено.")
                        fetchEstablishmentById(establishmentId) // Перезагрузка после сохранения
                        onResult(true)
                    } else {
                        Log.e("EstViewModel", "Ошибка обновления: ${response.code()}")
                        onResult(false)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("EstViewModel", "Сетевая ошибка: ${e.message}")
                    onResult(false)
                }
            }
        }
    }

    fun checkIfFavorite(id: Long): Boolean {
        return favoriteEstablishmentIds.value.contains(id)
    }

}