@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.roamly.ui.screens.booking

import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.entity.TableEntity
import com.example.roamly.entity.ViewModel.EstablishmentViewModel
import com.example.roamly.entity.ViewModel.UserViewModel
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale
// kotlin.time.Duration.Companion.minutes - Удалено, так как не используется

// Уникальный тег для логирования
private const val TAG = "CreateBookingScreen"

// ⭐ Конфигурация: Доступные длительности бронирования в минутах
private val BOOKING_DURATIONS_MINUTES = listOf(60L, 90L, 120L, 150L, 180L) // 1ч, 1.5ч, 2ч, 2.5ч, 3ч

// ------------------------------------------------------------------
// ⭐ УТИЛИТЫ ДЛЯ ВРЕМЕНИ РАБОТЫ (Operating Hours)
// ------------------------------------------------------------------

// Карта для преобразования ПОЛНЫХ названий дней на русском в DayOfWeek
@RequiresApi(Build.VERSION_CODES.O)
private val DAY_NAME_TO_DAY_OF_WEEK = mapOf(
    "ПОНЕДЕЛЬНИК" to DayOfWeek.MONDAY,
    "ВТОРНИК" to DayOfWeek.TUESDAY,
    "СРЕДА" to DayOfWeek.WEDNESDAY,
    "ЧЕТВЕРГ" to DayOfWeek.THURSDAY,
    "ПЯТНИЦА" to DayOfWeek.FRIDAY,
    "СУББОТА" to DayOfWeek.SATURDAY,
    "ВОСКРЕСЕНЬЕ" to DayOfWeek.SUNDAY
)

/**
 * Парсит строку operatingHoursString.
 */
@RequiresApi(Build.VERSION_CODES.O)
fun parseOperatingHours(hoursStr: String?): Map<DayOfWeek, Pair<LocalTime?, LocalTime?>> {
    if (hoursStr.isNullOrBlank()) {
        Log.d(TAG, "Operating hours string is null or blank. Returning empty map.")
        return emptyMap()
    }

    Log.d(TAG, "Parsing operating hours string: $hoursStr")

    return hoursStr.split("|")
        .mapNotNull { dayEntry ->
            val parts = dayEntry.split(":", limit = 2)

            if (parts.size != 2) {
                Log.w(TAG, "Skipping malformed day entry: $dayEntry")
                return@mapNotNull null
            }

            val dayName = parts[0].trim().uppercase(Locale.ROOT)
            val hours = parts[1].trim()
            val dayOfWeek = DAY_NAME_TO_DAY_OF_WEEK[dayName]
            if (dayOfWeek == null) {
                Log.w(TAG, "Unknown day name: $dayName")
                return@mapNotNull null
            }

            if (hours.contains("Закрыто", ignoreCase = true) || hours.isBlank()) {
                Log.v(TAG, "$dayName is CLOSED.")
                return@mapNotNull dayOfWeek to (null to null) // Закрыто
            }

            val timeParts = hours.split("-", limit = 2)
            if (timeParts.size == 2) {
                try {
                    val openTime = LocalTime.parse(timeParts[0].trim())
                    val closeTime = LocalTime.parse(timeParts[1].trim())
                    Log.v(TAG, "$dayName is open from $openTime to $closeTime")
                    return@mapNotNull dayOfWeek to (openTime to closeTime)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse time for $dayName ($hours): ${e.message}")
                    return@mapNotNull dayOfWeek to (null to null)
                }
            } else {
                Log.w(TAG, "Malformed time format for $dayName: $hours")
                return@mapNotNull dayOfWeek to (null to null)
            }
        }
        .toMap()
}

// ------------------------------------------------------------------
// ⭐ ОСНОВНОЙ КОМПОНЕНТ CreateBooking
// ------------------------------------------------------------------

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CreateBooking(
    navController: NavController,
    establishmentId: Long,
    establishmentViewModel: EstablishmentViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    // Получение данных
    val currentEstablishment by establishmentViewModel.currentEstablishment.collectAsState()
    val availableTables by establishmentViewModel.availableTables.collectAsState()
    val isLoading by establishmentViewModel.isBookingLoading.collectAsState()
    val currentUser by userViewModel.user.collectAsState()

    LaunchedEffect(Unit) {
        if (currentEstablishment == null) {
            establishmentViewModel.fetchEstablishmentById(establishmentId)
            Log.d(TAG, "Fetching establishment details for ID: $establishmentId")
        }
    }

    val context = LocalContext.current

    // Локальное состояние для ИНТЕРВАЛА
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedStartTime by remember { mutableStateOf<LocalTime?>(null) }
    var selectedDurationMinutes by remember { mutableStateOf(BOOKING_DURATIONS_MINUTES.first()) } // ⭐ Длительность
    val selectedEndTime = selectedStartTime?.plusMinutes(selectedDurationMinutes) // ⭐ Конец = Начало + Длительность

    var selectedTable by remember { mutableStateOf<TableEntity?>(null) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    val isLoggedIn = userViewModel.isLoggedIn()

    // Расписание заведения
    val operatingHoursMap = remember(currentEstablishment?.operatingHoursString) {
        parseOperatingHours(currentEstablishment?.operatingHoursString)
    }

    // Время работы для выбранной даты
    val (openTime, closeTime) = operatingHoursMap[selectedDate.dayOfWeek]?.let { (open, close) ->
        open to close
    } ?: (null to null)


    // ------------------------------------------------------------------
    // ⭐ Effect: Запрос доступных столов при изменении даты/времени/длительности
    // ------------------------------------------------------------------
    LaunchedEffect(selectedDate, selectedStartTime, selectedDurationMinutes) {
        if (selectedStartTime != null && currentEstablishment != null) {
            // ⭐ Отправляем время начала в формате ISO 8601
            val dateTime = LocalDateTime.of(selectedDate, selectedStartTime!!).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

            // ⭐ НОВОЕ ЛОГИРОВАНИЕ: ПУТЬ ЗАПРОСА
            val requestPath = "bookings/$establishmentId/available?dateTime=$dateTime"
            Log.w(TAG, "---[API CALL CHECK] Requesting available tables via path: $requestPath")
            Log.w(TAG, "---[API CALL CHECK] Establishment ID: $establishmentId, DateTime: $dateTime")

            establishmentViewModel.fetchAvailableTables(establishmentId, dateTime)
            selectedTable = null // Сбрасываем выбор столика
        }
    }

    // ------------------------------------------------------------------
    // ⭐ Компонент DatePickerDialog (Без изменений)
    // ------------------------------------------------------------------
    if (showDatePickerDialog) {
        val today = LocalDate.now()
        val twoWeeks = today.plusDays(14)
        val initialDateMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDateMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                    val isOpen = operatingHoursMap[date.dayOfWeek]?.first != null

                    val isWithinRange = (date.isEqual(today) || date.isAfter(today)) &&
                            (date.isEqual(twoWeeks) || date.isBefore(twoWeeks))

                    return isWithinRange && isOpen
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            selectedStartTime = null // Сброс времени при смене даты
                        }
                        showDatePickerDialog = false
                    },
                    enabled = datePickerState.selectedDateMillis != null
                ) {
                    Text("Выбрать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Отмена")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Бронирование: ${currentEstablishment?.name ?: "Загрузка..."}") },
                actions = {
                    if (currentEstablishment == null) {
                        CircularProgressIndicator(Modifier.size(24.dp).padding(end = 8.dp), strokeWidth = 2.dp)
                    }
                }
            )
        },
        bottomBar = {
            // Кнопка бронирования
            Button(
                onClick = {
                    val table = selectedTable
                    val startTime = selectedStartTime
                    if (!isLoggedIn) {
                        Toast.makeText(context, "Сначала войдите в систему.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (table == null || startTime == null || currentUser.id == null) {
                        Toast.makeText(context, "Выберите дату, время и столик.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // ⭐ Время начала
                    val dateTimeStr = LocalDateTime.of(selectedDate, startTime).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

                    // ⭐ НОВОЕ ЛОГИРОВАНИЕ: ДАННЫЕ ДЛЯ БРОНИ
                    Log.w(TAG, "---[API CALL CHECK] Submitting Booking DTO:")
                    Log.w(TAG, "---[API CALL CHECK] Establishment ID: $establishmentId")
                    Log.w(TAG, "---[API CALL CHECK] User ID: ${currentUser.id}")
                    Log.w(TAG, "---[API CALL CHECK] Table ID: ${table.id}")
                    Log.w(TAG, "---[API CALL CHECK] Start Time (ISO): $dateTimeStr")
                    Log.w(TAG, "---[API CALL CHECK] Duration Minutes: $selectedDurationMinutes")


                    // ⭐ ИСПРАВЛЕНИЕ: Передача durationMinutes
                    establishmentViewModel.submitBooking(
                        establishmentId = establishmentId,
                        userId = currentUser.id!!,
                        tableId = table.id,
                        dateTime = dateTimeStr, // Время начала
                        durationMinutes = selectedDurationMinutes, // ⭐ Длительность
                        onResult = { success, message ->
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            if (success) navController.popBackStack()
                        }
                    )
                },
                enabled = selectedTable != null && selectedStartTime != null && !isLoading && isLoggedIn && currentEstablishment != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Подтвердить бронирование")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (currentEstablishment == null) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("Загрузка информации о заведении...", modifier = Modifier.padding(top = 16.dp))
            } else {
                // ------------------------------------------------------------------
                // ⭐ ШАГ 1: Выбор даты
                // ------------------------------------------------------------------
                Text("1. Выберите дату", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                DateSelectionInput(
                    selectedDate = selectedDate,
                    onOpenPicker = { showDatePickerDialog = true },
                    openTime = openTime,
                    closeTime = closeTime
                )
                Spacer(Modifier.height(16.dp))

                // ------------------------------------------------------------------
                // ⭐ ШАГ 2: Выбор времени и длительности (НОВЫЕ КОМПОНЕНТЫ)
                // ------------------------------------------------------------------
                Text("2. Выберите время и длительность", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                if (openTime == null || closeTime == null) {
                    val dayOfWeek = selectedDate.format(DateTimeFormatter.ofPattern("EEEE", Locale("ru")))
                    Text("Заведение закрыто в $dayOfWeek.", color = MaterialTheme.colorScheme.error)
                } else {
                    // ⭐ Выбор длительности
                    BookingDurationSelector(
                        selectedDuration = selectedDurationMinutes,
                        onDurationSelected = {
                            selectedDurationMinutes = it
                            selectedTable = null // Сброс столика при смене длительности
                            Log.d(TAG, "Duration selected: $it min")
                        }
                    )
                    Spacer(Modifier.height(8.dp))

                    // ⭐ Выбор времени начала (улучшенный UX)
                    BookingIntervalSelector(
                        selectedStartTime = selectedStartTime,
                        onTimeSelected = {
                            selectedStartTime = it
                            Log.d(TAG, "Start time selected: $it")
                        },
                        openTime = openTime,
                        closeTime = closeTime,
                        durationMinutes = selectedDurationMinutes,
                        timeStepMinutes = 10
                    )
                }
                Spacer(Modifier.height(16.dp))

                // ------------------------------------------------------------------
                // ⭐ ШАГ 3: Выбор столика
                // ------------------------------------------------------------------
                Text("3. Выберите столик", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                if (selectedStartTime == null) {
                    Text("Выберите время, чтобы увидеть доступные столики.")
                } else if (isLoading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Проверяем доступность столов...")
                } else if (availableTables.isEmpty()) {
                    Text("Нет свободных столиков на это время (${selectedStartTime!!.format(DateTimeFormatter.ofPattern("HH:mm"))} - ${selectedEndTime!!.format(DateTimeFormatter.ofPattern("HH:mm"))}).", color = MaterialTheme.colorScheme.error)
                } else {
                    TableSelector(
                        availableTables = availableTables,
                        selectedTable = selectedTable,
                        onTableSelected = {
                            selectedTable = it
                            Log.d(TAG, "Table selected: ${it.name}")
                        }
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// ⭐ BookingDurationSelector (Выбор длительности)
// ------------------------------------------------------------------

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BookingDurationSelector(
    selectedDuration: Long,
    onDurationSelected: (Long) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(BOOKING_DURATIONS_MINUTES) { duration ->
            val isSelected = duration == selectedDuration
            Card(
                onClick = { onDurationSelected(duration) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text(
                    text = "${duration / 60} ч ${if (duration % 60 > 0) "${duration % 60} мин" else ""}".trim(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}


// ------------------------------------------------------------------
// ⭐ BookingIntervalSelector (Выбор времени начала + отображение конца)
// ------------------------------------------------------------------

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BookingIntervalSelector(
    selectedStartTime: LocalTime?,
    onTimeSelected: (LocalTime) -> Unit,
    openTime: LocalTime,
    closeTime: LocalTime,
    durationMinutes: Long, // Длительность брони
    timeStepMinutes: Long
) {
    val timeSlots = remember(openTime, closeTime, timeStepMinutes, durationMinutes) {
        val slots = mutableListOf<LocalTime>()
        var currentTime = openTime

        // Время, после которого нельзя начинать бронь (CloseTime минус длительность)
        val latestStartTime = closeTime.minusMinutes(durationMinutes)

        // Генерируем слоты, пока время начала + длительность не превысит время закрытия
        while (currentTime.isBefore(closeTime) && (currentTime.isBefore(latestStartTime) || currentTime.equals(latestStartTime))) { // ⭐ ИСПРАВЛЕНО: .equals()
            slots.add(currentTime)
            currentTime = currentTime.plusMinutes(timeStepMinutes)

            if (slots.size > 200) break
        }
        Log.v(TAG, "Generated ${slots.size} interval slots up to $latestStartTime.")
        slots
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(timeSlots) { startTime ->
            val endTime = startTime.plusMinutes(durationMinutes)
            val isSelected = startTime == selectedStartTime

            // Проверяем, не выходит ли конец брони за рамки рабочего времени (должно быть проверено в цикле, но для двойной проверки)
            val isValidSlot = endTime.isBefore(closeTime) || endTime.equals(closeTime) // ⭐ ИСПРАВЛЕНО: .equals()

            if (isValidSlot) {
                Card(
                    onClick = { onTimeSelected(startTime) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "- ${endTime.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// ⭐ DateSelectionInput (Отображение выбранной даты и кнопка)
// ------------------------------------------------------------------

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DateSelectionInput(
    selectedDate: LocalDate,
    onOpenPicker: () -> Unit,
    openTime: LocalTime?,
    closeTime: LocalTime?
) {
    val dateFormat = remember { DateTimeFormatter.ofPattern("EEEE, dd MMMM", Locale("ru")) }
    val timeFormat = remember { DateTimeFormatter.ofPattern("HH:mm") }

    OutlinedCard(
        onClick = onOpenPicker,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "Выбрать дату",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = selectedDate.format(dateFormat).replaceFirstChar { it.uppercase(Locale("ru")) },
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge
                )
                val statusText = if (openTime != null && closeTime != null) {
                    "Открыто: ${openTime.format(timeFormat)} - ${closeTime.format(timeFormat)}"
                } else {
                    "Закрыто"
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (openTime != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}


// ------------------------------------------------------------------
// ⭐ TableSelector (Выбор столика)
// ------------------------------------------------------------------

@Composable
fun TableSelector(
    availableTables: List<TableEntity>,
    selectedTable: TableEntity?,
    onTableSelected: (TableEntity) -> Unit
) {
    // Определяем толщину границы
    val selectedBorderWidth = 2.dp

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        availableTables.forEach { table ->
            val isSelected = table == selectedTable

            // ⭐ Ручное создание BorderStroke, чтобы установить цвет при выборе
            val borderStroke = if (isSelected) {
                BorderStroke(selectedBorderWidth, MaterialTheme.colorScheme.primary)
            } else {
                CardDefaults.outlinedCardBorder(enabled = true)
            }

            OutlinedCard(
                onClick = { onTableSelected(table) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.surface,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                ),
                border = borderStroke // Используем вручную созданный BorderStroke
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = table.name,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Вместимость: ${table.maxCapacity} чел.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Выбран",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        // Пустое место для выравнивания
                        Spacer(modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}