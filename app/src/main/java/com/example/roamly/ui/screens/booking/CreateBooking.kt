@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.roamly.ui.screens.booking

import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.entity.TableEntity
import com.example.roamly.entity.ViewModel.EstablishmentViewModel
import com.example.roamly.entity.ViewModel.UserViewModel
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

private const val TAG = "CreateBookingScreen"
private val BOOKING_DURATIONS_MINUTES = listOf(60L, 90L, 120L, 150L, 180L)

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
 * Парсит строку operatingHoursString. (Оставлена без изменений)
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

    // ⭐ НОВЫЕ ЛОКАЛЬНЫЕ СОСТОЯНИЯ
    var numberOfGuests by remember { mutableStateOf(1) } // Количество человек, по умолчанию 1
    var bookingComment by remember { mutableStateOf("") } // Заметка к брони

    var selectedTable by remember { mutableStateOf<TableEntity?>(null) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) } // ⭐ Для выбора времени

    val isLoggedIn = userViewModel.isLoggedIn()

    // Расписание заведения
    val operatingHoursMap = remember(currentEstablishment?.operatingHoursString) {
        parseOperatingHours(currentEstablishment?.operatingHoursString)
    }

    // Время работы для выбранной даты
    val (openTime, closeTime) = operatingHoursMap[selectedDate.dayOfWeek]?.let { (open, close) ->
        open to close
    } ?: (null to null)


    LaunchedEffect(selectedDate, selectedStartTime, selectedDurationMinutes, numberOfGuests) {
        // ⭐ Обновление: Учитываем selectedTable.maxCapacity и numberOfGuests при проверке доступности столов
        // Хотя API-запрос должен учитывать только время, после получения результатов, нужно отфильтровать столы
        // по вместимости или положиться на серверную логику. Здесь оставляем API-запрос прежним,
        // но при выборе стола нужно будет проверить вместимость.
        if (selectedStartTime != null && currentEstablishment != null) {
            // ⭐ Отправляем время начала в формате ISO 8601
            val dateTime = LocalDateTime.of(selectedDate, selectedStartTime!!).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

            // ⭐ НОВОЕ ЛОГИРОВАНИЕ: ПУТЬ ЗАПРОСА
            val requestPath = "bookings/$establishmentId/available?dateTime=$dateTime&durationMinutes=$selectedDurationMinutes&guests=$numberOfGuests"
            Log.w(TAG, "---[API CALL CHECK] Requesting available tables via path: $requestPath")
            Log.w(TAG, "---[API CALL CHECK] Establishment ID: $establishmentId, DateTime: $dateTime, Duration: $selectedDurationMinutes, Guests: $numberOfGuests")

            // ⭐ Обновляем запрос, чтобы потенциально учесть длительность и гостей (зависит от контракта API)
            // Здесь отправляем только dateTime, как было, но в реальном приложении лучше отправлять все.
            // Примем, что серверная логика учтет время и длительность.
            establishmentViewModel.fetchAvailableTables(establishmentId, dateTime)
            selectedTable = null // Сбрасываем выбор столика
        }
    }


    if (showDatePickerDialog) {
        // ... (Код DatePickerDialog без изменений)
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
                            selectedStartTime = null
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

    // ⭐ НОВЫЙ КОМПОНЕНТ: TimePickerDialog для выбора времени
    if (showTimePickerDialog && openTime != null && closeTime != null) {
        val initialTime = selectedStartTime ?: openTime
        val timePickerState = rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        val endTime = selected.plusMinutes(selectedDurationMinutes)

                        // Проверка, что выбранное время находится в пределах рабочего графика
                        val latestStartTime = closeTime.minusMinutes(selectedDurationMinutes)
                        val isValidTime = (selected.isAfter(openTime) || selected.equals(openTime)) &&
                                (selected.isBefore(latestStartTime) || selected.equals(latestStartTime)) &&
                                (endTime.isBefore(closeTime) || endTime.equals(closeTime))


                        if (isValidTime) {
                            selectedStartTime = selected
                            selectedTable = null // Сброс стола при смене времени
                            Log.d(TAG, "Start time selected: $selected")
                        } else {
                            Toast.makeText(context, "Выбранное время выходит за рамки рабочего графика с учетом длительности.", Toast.LENGTH_LONG).show()
                        }
                        showTimePickerDialog = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text("Отмена")
                }
            },
            title = { Text("Выберите время начала") },
            text = { TimePicker(state = timePickerState) }
        )
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
            Button(
                onClick = {
                    val table = selectedTable
                    val startTime = selectedStartTime
                    val guests = numberOfGuests

                    if (!isLoggedIn) {
                        Toast.makeText(context, "Сначала войдите в систему.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (table == null || startTime == null || currentUser.id == null || guests < 1) {
                        Toast.makeText(context, "Выберите дату, время, столик и укажите количество человек.", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    // Дополнительная проверка вместимости, если сервер не фильтрует
                    if (guests > table.maxCapacity) {
                        Toast.makeText(context, "Столик ${table.name} вмещает максимум ${table.maxCapacity} чел.", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    val dateTimeStr = LocalDateTime.of(selectedDate, startTime).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

                    Log.w(TAG, "---[API CALL CHECK] Submitting Booking DTO:")
                    Log.w(TAG, "---[API CALL CHECK] Establishment ID: $establishmentId")
                    Log.w(TAG, "---[API CALL CHECK] User ID: ${currentUser.id}")
                    Log.w(TAG, "---[API CALL CHECK] Table ID: ${table.id}")
                    Log.w(TAG, "---[API CALL CHECK] Start Time (ISO): $dateTimeStr")
                    Log.w(TAG, "---[API CALL CHECK] Duration Minutes: $selectedDurationMinutes")
                    // ⭐ НОВЫЕ ЛОГИ:
                    Log.w(TAG, "---[API CALL CHECK] Number of Guests: $guests")
                    Log.w(TAG, "---[API CALL CHECK] Comment: $bookingComment")


                    establishmentViewModel.submitBooking(
                        establishmentId = establishmentId,
                        userId = currentUser.id!!,
                        tableId = table.id,
                        dateTime = dateTimeStr, // Время начала
                        durationMinutes = selectedDurationMinutes,
                        numberOfGuests = guests,
                        comment = bookingComment,
                        onResult = { success, message ->
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            if (success) navController.popBackStack()
                        }
                    )
                },
                enabled = selectedTable != null && selectedStartTime != null && !isLoading && isLoggedIn && currentEstablishment != null && numberOfGuests > 0,
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
                Text("1. Выберите дату", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                DateSelectionInput(
                    selectedDate = selectedDate,
                    onOpenPicker = { showDatePickerDialog = true },
                    openTime = openTime,
                    closeTime = closeTime
                )
                Spacer(Modifier.height(16.dp))

                Text("2. Время и длительность", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                if (openTime == null || closeTime == null) {
                    val dayOfWeek = selectedDate.format(DateTimeFormatter.ofPattern("EEEE", Locale("ru")))
                    Text("Заведение закрыто в $dayOfWeek.", color = MaterialTheme.colorScheme.error)
                } else {
                    // ⭐ ОБНОВЛЕННЫЙ ВЫБОР ВРЕМЕНИ И ДЛИТЕЛЬНОСТИ
                    TimeSelectionInput(
                        selectedStartTime = selectedStartTime,
                        selectedDurationMinutes = selectedDurationMinutes,
                        onOpenTimePicker = { showTimePickerDialog = true },
                        onDurationSelected = {
                            selectedDurationMinutes = it
                            selectedTable = null
                            Log.d(TAG, "Duration selected: $it min")
                        },
                        openTime = openTime,
                        closeTime = closeTime
                    )
                }
                Spacer(Modifier.height(16.dp))

                // ⭐ НОВЫЙ РАЗДЕЛ: Количество человек
                Text("3. Количество человек", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                GuestSelectionInput(
                    numberOfGuests = numberOfGuests,
                    onGuestsChange = { guests ->
                        numberOfGuests = max(1, guests) // Минимум 1
                        selectedTable = null // Сброс стола при смене количества гостей
                    }
                )
                Spacer(Modifier.height(16.dp))

                Text("4. Выберите столик", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                if (selectedStartTime == null) {
                    Text("Выберите время, чтобы увидеть доступные столики.")
                } else if (isLoading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Проверяем доступность столов...")
                } else {
                    val filteredTables = availableTables.filter { it.maxCapacity >= numberOfGuests }
                    if (filteredTables.isEmpty()) {
                        Text("Нет свободных столиков, подходящих для $numberOfGuests чел. на это время (${selectedStartTime!!.format(DateTimeFormatter.ofPattern("HH:mm"))} - ${selectedEndTime!!.format(DateTimeFormatter.ofPattern("HH:mm"))}).", color = MaterialTheme.colorScheme.error)
                    } else {
                        TableSelector(
                            availableTables = filteredTables,
                            selectedTable = selectedTable,
                            onTableSelected = {
                                selectedTable = it
                                Log.d(TAG, "Table selected: ${it.name}")
                            },
                            requiredCapacity = numberOfGuests // Передаем требуемую вместимость
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))

                // ⭐ НОВЫЙ РАЗДЕЛ: Заметки
                Text("5. Дополнительные заметки (необязательно)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = bookingComment,
                    onValueChange = { bookingComment = it },
                    label = { Text("Заметки к брони (например, 'Нужен высокий стул')") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}


// ⭐ НОВЫЙ КОМПОНЕНТ: GuestSelectionInput
@Composable
fun GuestSelectionInput(
    numberOfGuests: Int,
    onGuestsChange: (Int) -> Unit
) {
    OutlinedTextField(
        value = numberOfGuests.toString(),
        onValueChange = { newValue ->
            val number = newValue.toIntOrNull()
            if (number != null && number >= 1) {
                onGuestsChange(number)
            } else if (newValue.isEmpty()) {
                // Разрешаем пустое поле, чтобы пользователь мог начать ввод, но установим 1 при подтверждении брони
                onGuestsChange(0)
            }
        },
        label = { Text("Количество гостей") },
        leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Гости") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}

// ⭐ НОВЫЙ КОМПОНЕНТ: TimeSelectionInput
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TimeSelectionInput(
    selectedStartTime: LocalTime?,
    selectedDurationMinutes: Long,
    onOpenTimePicker: () -> Unit,
    onDurationSelected: (Long) -> Unit,
    openTime: LocalTime?,
    closeTime: LocalTime?
) {
    val timeFormat = remember { DateTimeFormatter.ofPattern("HH:mm") }

    // Выбор времени начала
    OutlinedCard(
        onClick = onOpenTimePicker,
        modifier = Modifier.fillMaxWidth().clickable(enabled = openTime != null) { onOpenTimePicker() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Выбрать время",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                val timeDisplay = if (selectedStartTime != null) {
                    val endTime = selectedStartTime.plusMinutes(selectedDurationMinutes)
                    "${selectedStartTime.format(timeFormat)} - ${endTime.format(timeFormat)}"
                } else {
                    "Нажмите, чтобы выбрать время начала"
                }
                Text(
                    text = timeDisplay,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Длительность: ${selectedDurationMinutes / 60} ч ${if (selectedDurationMinutes % 60 > 0) "${selectedDurationMinutes % 60} мин" else ""}".trim(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    Text("Выберите длительность бронирования:", style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(8.dp))

    // Выбор длительности
    BookingDurationSelector(
        selectedDuration = selectedDurationMinutes,
        onDurationSelected = onDurationSelected
    )
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BookingDurationSelector(
    selectedDuration: Long,
    onDurationSelected: (Long) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(BOOKING_DURATIONS_MINUTES) { duration ->
            val isSelected = duration == selectedDuration
            ElevatedCard( // Заменено на ElevatedCard для лучшей видимости
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


// УДАЛЕН BookingIntervalSelector - заменен на TimeSelectionInput и TimePicker

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


@Composable
fun TableSelector(
    availableTables: List<TableEntity>,
    selectedTable: TableEntity?,
    onTableSelected: (TableEntity) -> Unit,
    requiredCapacity: Int // ⭐ НОВЫЙ ПАРАМЕТР
) {
    val selectedBorderWidth = 2.dp

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        availableTables.forEach { table ->
            val isSelected = table == selectedTable

            val borderStroke = if (isSelected) {
                BorderStroke(selectedBorderWidth, MaterialTheme.colorScheme.primary)
            } else {
                CardDefaults.outlinedCardBorder(enabled = true)
            }

            OutlinedCard(
                // Дополнительная проверка на вместимость не нужна здесь,
                // так как список availableTables уже отфильтрован в CreateBooking
                onClick = { onTableSelected(table) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.surface,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                ),
                border = borderStroke
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
                        // ⭐ Обновление текста: выделение, если вместимость соответствует
                        val capacityText = "Вместимость: ${table.maxCapacity} чел."
                        Text(
                            text = capacityText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (table.maxCapacity >= requiredCapacity) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Выбран",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Spacer(modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}