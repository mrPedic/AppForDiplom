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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.roamly.entity.EstablishmentLoadState
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

@RequiresApi(Build.VERSION_CODES.O)
fun parseOperatingHours(hoursStr: String?): Map<DayOfWeek, Pair<LocalTime?, LocalTime?>> {
    if (hoursStr.isNullOrBlank()) return emptyMap()

    return hoursStr.split("|")
        .mapNotNull { dayEntry ->
            val parts = dayEntry.split(":", limit = 2)
            if (parts.size != 2) return@mapNotNull null

            val dayName = parts[0].trim().uppercase(Locale.ROOT)
            val hours = parts[1].trim()
            val dayOfWeek = DAY_NAME_TO_DAY_OF_WEEK[dayName] ?: return@mapNotNull null

            if (hours.contains("Закрыто", ignoreCase = true) || hours.isBlank()) {
                dayOfWeek to (null to null)
            } else {
                val timeParts = hours.split("-", limit = 2)
                if (timeParts.size == 2) {
                    val start = LocalTime.parse(timeParts[0].trim())
                    val end = LocalTime.parse(timeParts[1].trim())
                    dayOfWeek to (start to end)
                } else null
            }
        }.toMap()
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CreateBookingScreen(
    navController: NavController,
    establishmentId: Long,
    viewModel: EstablishmentViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val establishmentState by viewModel.establishmentDetailState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val user by userViewModel.user.collectAsState()

    // Локальные состояния
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedTime by remember { mutableStateOf(LocalTime.of(18, 0)) }
    var selectedDuration by remember { mutableStateOf(120L) }
    var guestCount by remember { mutableStateOf(2) }
    var selectedTable by remember { mutableStateOf<TableEntity?>(null) }

    // Загрузка заведения
    LaunchedEffect(establishmentId) {
        viewModel.fetchEstablishmentDetails(establishmentId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Бронирование") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = establishmentState) {
                is EstablishmentLoadState.Idle -> {}
                is EstablishmentLoadState.Loading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                is EstablishmentLoadState.Error -> {
                    Text(
                        text = "Ошибка: ${state.message}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is EstablishmentLoadState.Success -> {
                    val establishment = state.data

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text(
                            text = establishment.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Адрес: ${establishment.address}", style = MaterialTheme.typography.bodyMedium)

                        Spacer(Modifier.height(24.dp))

                        // Дата
                        DateSelector(
                            selectedDate = selectedDate,
                            onDateSelected = { selectedDate = it },
                            operatingHours = parseOperatingHours(establishment.operatingHoursString)
                        )

                        Spacer(Modifier.height(16.dp))

                        // Время
                        TimeSelector(
                            selectedTime = selectedTime,
                            onTimeSelected = { selectedTime = it },
                            selectedDate = selectedDate,
                            operatingHours = parseOperatingHours(establishment.operatingHoursString)
                        )

                        Spacer(Modifier.height(16.dp))

                        // Количество гостей
                        GuestCountSelector(
                            guestCount = guestCount,
                            onGuestCountChange = { guestCount = it.coerceIn(1, 20) }
                        )

                        Spacer(Modifier.height(16.dp))

                        // Длительность
                        DurationSelector(
                            selectedDuration = selectedDuration,
                            onDurationSelected = { selectedDuration = it }
                        )

                        Spacer(Modifier.height(24.dp))

                        // Доступные столики (заглушка — в реальности нужно отдельный запрос)
                        // Пока просто показываем пример
                        val mockTables = listOf(
                            TableEntity(id = 1, name = "Столик у окна", maxCapacity = 4, description = "", establishmentId = -1L),
                            TableEntity(id = 2, name = "Большой столик", maxCapacity = 8, description = "", establishmentId = -1L),
                            TableEntity(id = 3, name = "VIP-зал", maxCapacity = 12, description = "", establishmentId = -1L),
                        )

                        val availableTables = mockTables.filter { it.maxCapacity >= guestCount }

                        if (availableTables.isEmpty()) {
                            Text(
                                text = "Нет столиков на $guestCount человек(а)",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        } else {
                            Text("Выберите столик:", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            TableSelector(
                                availableTables = availableTables,
                                selectedTable = selectedTable,
                                onTableSelected = { selectedTable = it },
                                requiredCapacity = guestCount
                            )
                        }

                        Spacer(Modifier.height(32.dp))

                        // Кнопка бронирования
                        Button(
                            onClick = {
                                if (user.id == null) {
                                    Toast.makeText(context, "Войдите в аккаунт", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (selectedTable == null) {
                                    Toast.makeText(context, "Выберите столик", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                // Здесь будет реальный вызов API
                                Toast.makeText(context, "Бронь отправлена!", Toast.LENGTH_LONG).show()
                                navController.popBackStack()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = selectedTable != null && user.id != null
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Забронировать")
                        }
                    }
                }
            }

            // Глобальный индикатор загрузки
            if (isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DateSelector(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    operatingHours: Map<DayOfWeek, Pair<LocalTime?, LocalTime?>>
) {
    val today = LocalDate.now()
    val dates = (0L..30L).map { today.plusDays(it) }

    Column {
        Text("Выберите дату:", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(dates) { date ->
                val dayOfWeek = date.dayOfWeek
                val isClosed = operatingHours[dayOfWeek]?.first == null
                val isSelected = date == selectedDate

                FilterChip(
                    selected = isSelected,
                    onClick = { if (!isClosed) onDateSelected(date) },
                    label = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(date.dayOfMonth.toString(), fontWeight = FontWeight.Bold)
                            Text(date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale("ru")))
                        }
                    },
                    enabled = !isClosed,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (isClosed) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TimeSelector(
    selectedTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    selectedDate: LocalDate,
    operatingHours: Map<DayOfWeek, Pair<LocalTime?, LocalTime?>>
) {
    val dayOfWeek = selectedDate.dayOfWeek
    val (openTime, closeTime) = operatingHours[dayOfWeek] ?: (null to null)

    if (openTime == null || closeTime == null) {
        Text("Заведение закрыто в этот день", color = MaterialTheme.colorScheme.error)
        return
    }

    val times = generateSequence(openTime) { time ->
        time.plusMinutes(30).takeIf { it < closeTime }
    }.toList()

    Column {
        Text("Выберите время:", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(times) { time ->
                val isSelected = time == selectedTime
                FilterChip(
                    selected = isSelected,
                    onClick = { onTimeSelected(time) },
                    label = { Text(time.format(DateTimeFormatter.ofPattern("HH:mm"))) }
                )
            }
        }
    }
}

@Composable
fun DurationSelector(
    selectedDuration: Long,
    onDurationSelected: (Long) -> Unit
) {
    Column {
        Text("Длительность:", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(BOOKING_DURATIONS_MINUTES) { minutes ->
                val isSelected = minutes == selectedDuration
                FilterChip(
                    selected = isSelected,
                    onClick = { onDurationSelected(minutes) },
                    label = { Text("${minutes}мин") }
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
    requiredCapacity: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        availableTables.forEach { table ->
            val isSelected = table == selectedTable
            val borderStroke = if (isSelected) {
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            } else {
                CardDefaults.outlinedCardBorder()
            }

            OutlinedCard(
                onClick = { onTableSelected(table) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                    else MaterialTheme.colorScheme.surface
                ),
                border = borderStroke
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(table.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Вместимость: ${table.maxCapacity} чел.",
                            color = if (table.maxCapacity >= requiredCapacity) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.error
                        )
                    }
                    if (isSelected) {
                        Icon(Icons.Default.DateRange, "Выбран", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
@Composable
fun GuestCountSelector(
    guestCount: Int,
    onGuestCountChange: (Int) -> Unit
) {
    Column {
        Text("Количество гостей:", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onGuestCountChange(guestCount - 1) }, enabled = guestCount > 1) {
                Icon(Icons.Filled.Person, "Меньше")
            }
            Text(
                text = "$guestCount",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.width(60.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            IconButton(onClick = { onGuestCountChange(guestCount + 1) }) {
                Icon(Icons.Filled.Person, "Больше")
            }
        }
    }
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
    val dateFormat =
        remember { DateTimeFormatter.ofPattern("EEEE, dd MMMM", Locale("ru")) }
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
                    text = selectedDate.format(dateFormat)
                        .replaceFirstChar { it.uppercase(Locale("ru")) },
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge
                )
                val statusText = if (openTime != null && closeTime != null) {
                    "Открыто: ${openTime.format(timeFormat)} - ${
                        closeTime.format(
                            timeFormat
                        )
                    }"
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