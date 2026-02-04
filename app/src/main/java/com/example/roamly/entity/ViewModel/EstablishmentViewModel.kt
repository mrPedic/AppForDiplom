package com.example.roamly.entity.ViewModel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roamly.ApiService
import com.example.roamly.PureLocationManager
import com.example.roamly.R
import com.example.roamly.classes.cl_menu.Drink
import com.example.roamly.classes.cl_menu.Food
import com.example.roamly.classes.cl_menu.MenuOfEstablishment
import com.example.roamly.entity.DTO.establishment.EstablishmentDisplayDto
import com.example.roamly.entity.DTO.establishment.EstablishmentFavoriteDto
import com.example.roamly.entity.DTO.establishment.EstablishmentMarkerDto
import com.example.roamly.entity.DTO.establishment.EstablishmentSearchResultDto
import com.example.roamly.entity.DTO.TableCreationDto
import com.example.roamly.entity.DTO.establishment.EstablishmentWithCountsDto
import com.example.roamly.entity.classes.EstablishmentEntity
import com.example.roamly.entity.EstablishmentLoadState
import com.example.roamly.entity.classes.EstablishmentStatus
import com.example.roamly.entity.classes.ReviewDisplayDto
import com.example.roamly.entity.classes.ReviewEntity
import com.example.roamly.entity.classes.TableEntity
import com.example.roamly.entity.classes.TypeOfEstablishment
import com.example.roamly.manager.SearchHistoryManager
import com.example.roamly.ui.screens.sealed.SaveStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import retrofit2.HttpException
import javax.inject.Inject


@HiltViewModel
class EstablishmentViewModel @Inject constructor(
    private val apiService: ApiService,
    private val searchHistoryManager: SearchHistoryManager,
    @ApplicationContext private val appContext: Context  // Добавь это
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



    private val _isDetailWidgetVisible = MutableStateFlow(false)
    val isDetailWidgetVisible: StateFlow<Boolean> = _isDetailWidgetVisible.asStateFlow()




    // --- StateFlow для неодобренных заведений (Admin) ---
    private val _pendingEstablishments =
        MutableStateFlow<List<EstablishmentDisplayDto>>(emptyList())
    val pendingEstablishments: StateFlow<List<EstablishmentDisplayDto>> = _pendingEstablishments

    // ================================================ //
    // ===== ПОЛЯ ДЛЯ РАБОТЫ С ОТЗЫВАМИ (Reviews) ===== //
    // ================================================ //

    private val _reviews = MutableStateFlow<List<ReviewEntity>>(emptyList())

    private val _isReviewsLoading = MutableStateFlow(false)

    // =========================================== //
    // ===== ПОЛЯ ДЛЯ РАБОТЫ С КАРТОЙ (Map) ===== //
    // =========================================== //

    private val _establishmentMarkers = MutableStateFlow<List<EstablishmentMarkerDto>>(emptyList())
    val establishmentMarkers: StateFlow<List<EstablishmentMarkerDto>> = _establishmentMarkers

    // Потоки для виджета деталей на карте

    // ===================================================== //
    // ===== ПОЛЯ ДЛЯ РАБОТЫ С БРОНИРОВАНИЕМ (Booking) ===== //
    // ===================================================== //


    // Список столов, доступных для бронирования на выбранную дату/время


    // ============================================== //
    // ===== ПОЛЯ ДЛЯ РАБОТЫ С ПОИСКОМ (Search) ===== //
    // ============================================== //


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

    private val _selectedReview = MutableStateFlow<ReviewEntity?>(null)
    val selectedReview: StateFlow<ReviewEntity?> = _selectedReview.asStateFlow()


    // =========================================== //
    // ===== ИНИЦИАЛИЗАЦИЯ ViewModel (init) ===== //
    // =========================================== //

    init {
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
                    // ВЫПОЛНЯЕМ ПОИСК ПРИ ИЗМЕНЕНИИ ФИЛЬТРОВ
                    performSearch(query, types)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun performSearch(query: String, types: Set<TypeOfEstablishment>) {
        viewModelScope.launch {
            _isSearchLoading.value = true
            try {
                val typeNames = if (types.isNotEmpty()) {
                    types.map { it.name }
                } else null

                val results = apiService.searchEstablishments(
                    query = if (query.isNotBlank()) query else null,
                    types = typeNames
                )

                _establishmentSearchResults.value = results
                Log.i("EstViewModel", "Найдено заведений: ${results.size}")
            } catch (e: Exception) {
                Log.e("EstViewModel", "Ошибка поиска: ${e.message}")
                _errorMessage.value = "Ошибка при поиске заведений"
                _establishmentSearchResults.value = emptyList()
            } finally {
                _isSearchLoading.value = false
            }
        }
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




    // ================================================= //
    // ===== МЕТОДЫ ДЛЯ КАРТЫ (Map Detail Widget) ===== //
    // ================================================= //

    private var loadJob: Job? = null

    fun loadEstablishmentDetails(establishmentId: Long) {
        loadJob?.cancel()  // Отменяем предыдущий запрос
        loadJob = viewModelScope.launch {
            _isDetailWidgetVisible.value = true  // Показываем виджет сразу (с loading)
            _establishmentLoadState.value = EstablishmentLoadState.Loading
            try {
                val details = apiService.getEstablishmentById(establishmentId)
                _establishmentLoadState.value = EstablishmentLoadState.Success(details, photosLoading = true)
                val photos = apiService.getEstablishmentPhotos(establishmentId)
                _establishmentLoadState.value = EstablishmentLoadState.Success(details.copy(photoBase64s = photos), photosLoading = false)
                Log.d("EstViewModel", "Полные данные заведения ID $establishmentId загружены: ${details.name}")
            } catch (e: Exception) {
                _establishmentLoadState.value = EstablishmentLoadState.Error("Ошибка: ${e.message}")
                _isDetailWidgetVisible.value = false  // Скрываем на ошибке
                Log.e("EstViewModel", "Ошибка: ${e.message}")
            }
        }
    }

    fun closeDetailWidget() {
        _isDetailWidgetVisible.value = false
        _establishmentLoadState.value = EstablishmentLoadState.Idle
    }

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

    private val _establishmentLoadState = MutableStateFlow<EstablishmentLoadState>(EstablishmentLoadState.Idle)
    val establishmentLoadState: StateFlow<EstablishmentLoadState> = _establishmentLoadState.asStateFlow()

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
                    .sortedBy { it.dateOfCreation }

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
            val response = apiService.createTables(establishmentId, tables)

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

                Log.e("TableVM", "Ошибка сервера при создании столиков: $errorMsg")

                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
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

    private val _establishment = MutableStateFlow<EstablishmentDisplayDto?>(null)
    val establishment: StateFlow<EstablishmentDisplayDto?> = _establishment.asStateFlow()



    /**
     * Загружает меню с сервера.
     */
    fun fetchMenuForEstablishment(establishmentId: Long) {
        Log.d("MenuVM", "Начинаем загрузку меню для ID: $establishmentId")

        _isMenuLoading.value = true
        _menuOfEstablishment.value = null // Очистка старых данных

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Используем соответствующий метод из ApiService
                val menu = apiService.getMenuForEstablishment(establishmentId)

                withContext(Dispatchers.Main) {
                    _menuOfEstablishment.value = menu
                    Log.d("MenuVM", "Меню успешно загружено. Групп еды: ${menu.foodGroups.size}")
                }
            } catch (e: HttpException) {
                val errorCode = e.code()
                val errorMsg = e.response()?.errorBody()?.string() ?: "Ошибка $errorCode"
                Log.e("MenuVM", "Ошибка загрузки меню: HTTP $errorCode. Сообщение: $errorMsg")
            } catch (e: Exception) {
                Log.e("MenuVM", "Ошибка подключения к серверу или парсинга меню: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    _isMenuLoading.value = false
                }
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
            val finalHistory = newList.take(3)

            // СОХРАНЯЕМ В ХРАНИЛИЩЕ ПОСЛЕ ОБНОВЛЕНИЯ
            viewModelScope.launch {
                searchHistoryManager.saveHistory(finalHistory)
            }

            finalHistory
        }
    }

    private val _favoriteEstablishmentsList = MutableStateFlow<List<EstablishmentFavoriteDto>>(emptyList())
    val favoriteEstablishmentsList: StateFlow<List<EstablishmentFavoriteDto>> = _favoriteEstablishmentsList.asStateFlow()
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

    // ============================================= //
// ===== ПОЛЯ И МЕТОДЫ ДЛЯ GPS (Location) ===== //
// ============================================= //
    private val _isLocationTracking = MutableStateFlow(false)
    val isLocationTracking: StateFlow<Boolean> = _isLocationTracking.asStateFlow()
    private val _hasUserLocation = MutableStateFlow(false)
    val hasUserLocation: StateFlow<Boolean> = _hasUserLocation.asStateFlow()
    private var locationManager: PureLocationManager? = null
    private var userMarker: Marker? = null
    var mapView: MapView? = null  // Ссылка на MapView (установи в HomeScreen)
    fun initializeLocation() {
        locationManager = PureLocationManager(appContext)
    }
    fun hasLocationPermission(): Boolean = locationManager?.hasLocationPermission() ?: false
    fun toggleLocationTracking() {
        if (_isLocationTracking.value) {
// Выключаем (3-е нажатие)
            stopLocationTracking()
        } else if (_hasUserLocation.value) {
// Центрируем (2-е нажатие)
            centerOnUserLocation()
        } else {
// Включаем (1-е нажатие)
            startLocationTracking()
        }
    }
    private fun startLocationTracking() {
        if (!locationManager!!.hasLocationPermission()) {
            _errorMessage.value = "Нет разрешения на геолокацию. Включите в настройках."
            return
        }
        if (!isGpsEnabled()) {
            _errorMessage.value = "GPS выключен. Включите в настройках устройства."
            return
        }
        _isLocationTracking.value = true
        _hasUserLocation.value = false
        locationManager!!.startLocationUpdates { location ->
            _hasUserLocation.value = true
            updateUserMarker(location)
            if (_isLocationTracking.value) {
                centerOnUserLocation()  // Автоцентрирование при обновлении
            }
        }
// Первое местоположение
        locationManager!!.getCurrentLocation { location ->
            location?.let {
                _hasUserLocation.value = true
                updateUserMarker(it)
                centerOnUserLocation()
            }
        }
    }
    private fun stopLocationTracking() {
        locationManager?.stopLocationUpdates()
        _isLocationTracking.value = false
        _hasUserLocation.value = false
        removeUserMarker()
    }
    private fun centerOnUserLocation() {
        userMarker?.position?.let { geoPoint ->
            val currentZoom = mapView?.zoomLevelDouble ?: 16.0  // Получаем текущий zoom или fallback на 16.0
            mapView?.controller?.animateTo(geoPoint, currentZoom, 1000L)  // Используем текущий zoom для анимации
        }
    }

    private fun updateUserMarker(location: android.location.Location) {
        val geoPoint = GeoPoint(location.latitude, location.longitude)
        if (userMarker == null) {
            userMarker = Marker(mapView).apply {
                position = geoPoint
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = ContextCompat.getDrawable(appContext, R.drawable.ic_my_location_blue)
                    ?: createBlueDotIcon()
                title = "Вы здесь"
                mapView?.overlays?.add(this)
            }
        } else {
            userMarker!!.position = geoPoint
        }
        mapView?.invalidate()
    }
    private fun removeUserMarker() {
        userMarker?.let {
            mapView?.overlays?.remove(it)
            userMarker = null
            mapView?.invalidate()
        }
    }
    private fun createBlueDotIcon(): BitmapDrawable {
        val size = 48
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLUE
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 3f, paint)
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(size / 2f, size / 2f, size / 6f, paint)
        return BitmapDrawable(appContext.resources, bitmap)
    }

    fun isGpsEnabled(): Boolean {
        return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false ||
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ?: false
    }



    private val _userEstablishmentsWithCounts = MutableStateFlow<List<EstablishmentWithCountsDto>>(emptyList())
    val userEstablishmentsWithCounts: StateFlow<List<EstablishmentWithCountsDto>> = _userEstablishmentsWithCounts.asStateFlow()

    private val _countsLoading = MutableStateFlow(false)
    val countsLoading: StateFlow<Boolean> = _countsLoading.asStateFlow()

    suspend fun fetchEstablishmentsWithCountsByUserId(userId: Long) {
        _countsLoading.value = true
        try {
            val establishments = apiService.getEstablishmentsWithCountsByUserId(userId)
            _userEstablishmentsWithCounts.value = establishments
        } catch (e: Exception) {
            Log.e("EstablishmentViewModel", "Error fetching establishments with counts", e)
        } finally {
            _countsLoading.value = false
        }
    }


    fun loadReview(reviewId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val review = apiService.getReviewById(reviewId)
                _selectedReview.value = review
            } catch (e: Exception) {
                Log.e("EstablishmentViewModel", "Error loading review", e)
                _errorMessage.value = "Ошибка загрузки отзыва"
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun updateReview(
        reviewId: Long,
        establishmentId: Long,
        userId: Long,
        rating: Float,
        reviewText: String,
        photoBase64: String?,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val reviewEntity = ReviewEntity(
                    id = reviewId, // Важно передать ID
                    establishmentId = establishmentId,
                    createdUserId = userId,
                    rating = rating,
                    reviewText = reviewText,
                    photoBase64 = photoBase64
                )

                val response = withContext(Dispatchers.IO) {
                    apiService.updateReview(reviewId, reviewEntity)
                }

                if (response.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, "Ошибка сервера: ${response.code()}")
                }
            } catch (e: Exception) {
                onResult(false, e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }
}