package com.example.roamly.entity.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roamly.entity.DTO.ReservationDto
import com.example.roamly.entity.TableEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

// Заглушка репозитория, который нужно будет реализовать
interface ReservationRepository {
    suspend fun getTablesForEstablishment(establishmentId: Long): List<TableEntity>
    suspend fun getReservationsForTableAndDay(tableId: Long, date: LocalDate): List<ReservationDto>
    suspend fun createReservation(reservation: ReservationDto): ReservationDto
}

// Заглушка для инъекции
// @HiltViewModel
class ReservationViewModel(
    private val reservationRepository: ReservationRepository
) : ViewModel() {

    // Состояние загрузки
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Список столиков для текущего заведения
    private val _tables = MutableStateFlow<List<TableEntity>>(emptyList())
    val tables: StateFlow<List<TableEntity>> = _tables.asStateFlow()

    // Карта занятых временных слотов для выбранного столика и даты
    // Key: LocalTime (начало слота, например, 14:30), Value: Boolean (true = занято)
    private val _bookedSlots = MutableStateFlow<Map<LocalTime, Boolean>>(emptyMap())
    val bookedSlots: StateFlow<Map<LocalTime, Boolean>> = _bookedSlots.asStateFlow()

    // --- Параметры экрана бронирования ---
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _selectedTable = MutableStateFlow<TableEntity?>(null)
    val selectedTable: StateFlow<TableEntity?> = _selectedTable.asStateFlow()


    /**
     * Загружает все столики для данного заведения.
     */
    fun loadTables(establishmentId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _tables.value = reservationRepository.getTablesForEstablishment(establishmentId)
            } catch (e: Exception) {
                // Обработка ошибок
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Загружает занятые слоты для выбранного столика и даты.
     */
    fun loadBookedSlots(tableId: Long, date: LocalDate) {
        viewModelScope.launch {
            _isLoading.value = true
            _bookedSlots.value = emptyMap() // Сброс
            try {
                val reservations = reservationRepository.getReservationsForTableAndDay(tableId, date)

                // ⭐ Генерируем занятые слоты с шагом в 15 минут
                val slotsMap = mutableMapOf<LocalTime, Boolean>()

                // (Здесь потребуется сложная логика для генерации слотов,
                // но пока сосредоточимся на загрузке данных)

                // Простая заглушка:
                reservations.forEach { reservation ->
                    // Для примера, помечаем начало бронирования как занятое
                    slotsMap[reservation.startTime] = true
                }

                _bookedSlots.value = slotsMap

            } catch (e: Exception) {
                // Обработка ошибок
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Обновляет выбранную дату и перезагружает слоты, если столик выбран.
     */
    fun updateSelectedDate(date: LocalDate) {
        _selectedDate.value = date
        _selectedTable.value?.let { table ->
            loadBookedSlots(table.id, date)
        }
    }

    /**
     * Обновляет выбранный столик и перезагружает слоты.
     */
    fun selectTable(table: TableEntity) {
        _selectedTable.value = table
        loadBookedSlots(table.id, _selectedDate.value)
    }


    /**
     * Отправляет запрос на бронирование.
     */
    fun attemptReservation(
        tableId: Long,
        userId: Long,
        startTime: LocalTime,
        durationMinutes: Int = 90, // Например, бронь на 1.5 часа
        guests: Int,
        comment: String? = null
    ) {
        viewModelScope.launch {
            // ⭐ Здесь нужно проверить, что слот свободен (сложная логика)

            _isLoading.value = true
            try {
                val reservationDto = ReservationDto(
                    tableId = tableId,
                    userId = userId,
                    reservationDate = _selectedDate.value,
                    startTime = startTime,
                    // Расчет времени окончания (EndTime)
                    endTime = startTime.plusMinutes(durationMinutes.toLong()),
                    numberOfGuests = guests,
                    comment = comment
                )

                val result = reservationRepository.createReservation(reservationDto)

                // Успешная бронь!
                // Вывести сообщение пользователю и перезагрузить слоты

            } catch (e: Exception) {
                // Обработка ошибок бронирования (например, "Слот только что заняли")
            } finally {
                _isLoading.value = false
            }
        }
    }
}