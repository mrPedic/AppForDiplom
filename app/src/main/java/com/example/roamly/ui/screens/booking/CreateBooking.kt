@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.roamly.ui.screens.booking

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
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
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
import com.example.roamly.entity.EstablishmentLoadState
import com.example.roamly.entity.classes.TableEntity
import com.example.roamly.entity.ViewModel.BookingViewModel
import com.example.roamly.entity.ViewModel.UserViewModel
import com.example.roamly.entity.DTO.booking.BookingCreationDto
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val TAG = "CreateBookingScreen"
private val BOOKING_DURATIONS_MINUTES = listOf(30L, 60L, 90L, 120L, 150L, 180L)

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
    viewModel: BookingViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val user by userViewModel.user.collectAsState()

    // Состояния формы
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedTime by remember { mutableStateOf(LocalTime.now().plusHours(1).withMinute(0)) }
    var selectedDuration by remember { mutableStateOf(90L) }
    var selectedTable by remember { mutableStateOf<TableEntity?>(null) }
    var numberOfGuests by remember { mutableStateOf(2) }
    var guestContact by remember { mutableStateOf("") } // Изменено: guestPhone -> guestContact
    var notes by remember { mutableStateOf("") }

    val establishmentState by viewModel.establishmentDetailState.collectAsState()
    val availableTables by viewModel.availableTables.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()

    // Загружаем данные заведения и столики
    LaunchedEffect(Unit) {
        viewModel.fetchEstablishmentDetails(establishmentId)
    }

    val operatingHours = (establishmentState as? EstablishmentLoadState.Success)?.data?.let {
        parseOperatingHours(it.operatingHoursString)
    } ?: emptyMap()

    LaunchedEffect(selectedDate, selectedTime) {
        val dayOfWeek = selectedDate.dayOfWeek
        val (open, close) = operatingHours[dayOfWeek] ?: return@LaunchedEffect
        if (open != null && close != null && selectedTime >= open && selectedTime < close) {
            val dateTime = LocalDateTime.of(selectedDate, selectedTime)
            val iso = dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            viewModel.fetchAvailableTables(establishmentId, iso)
        } else {
            viewModel._availableTables.value = emptyList()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Бронирование") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Назад")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading && establishmentState is EstablishmentLoadState.Idle) {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            return@Scaffold
        }

        when (establishmentState) {
            is EstablishmentLoadState.Success -> {
                val establishment = (establishmentState as EstablishmentLoadState.Success).data

                Column(
                    modifier = Modifier
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(establishment.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(establishment.address, style = MaterialTheme.typography.bodyLarge)
                    DateSelector(selectedDate) { newDate -> selectedDate = newDate }
                    TimeSelector(selectedTime, operatingHours, selectedDate) { newTime -> selectedTime = newTime }
                    BookingDurationSelector(selectedDuration) { newDuration -> selectedDuration = newDuration }
                    GuestCountSelector(numberOfGuests) { newCount -> numberOfGuests = newCount }
                    Text("Доступные столы:", fontWeight = FontWeight.SemiBold)
                    if (availableTables.isEmpty()) {
                        Text("Нет доступных столов на выбранное время", color = Color.Red)
                    } else {
                        TableSelector(availableTables, selectedTable, numberOfGuests) { table -> selectedTable = table }
                    }

                    // Измененное поле: Как с вами связаться
                    OutlinedTextField(
                        value = guestContact,
                        onValueChange = { guestContact = it },
                        label = { Text("Как с вами связаться (номер телефона, как обращаться)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )

                    // Поле для заметок
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Заметки (по желанию)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        minLines = 3
                    )

                    Spacer(Modifier.height(24.dp))

                    // Кнопка бронирования
                    Button(
                        onClick = {
                            selectedTable?.let { table ->
                                val userId = user?.id ?: return@let
                                val startTimeStr = LocalDateTime.of(selectedDate, selectedTime).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                val dto = BookingCreationDto(
                                    establishmentId = establishmentId,
                                    userId = userId,
                                    tableId = table.id,
                                    startTime = startTimeStr,
                                    durationMinutes = selectedDuration,
                                    numPeople = numberOfGuests,
                                    notes = notes.takeIf { it.isNotBlank() },
                                    guestPhone = guestContact  // Теперь это может быть любой текст, сервер примет String
                                )
                                viewModel.createBooking(dto) { success ->
                                    if (success) {
                                        Toast.makeText(context, "Бронирование создано!", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    } else {
                                        Toast.makeText(context, "Ошибка создания бронирования", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } ?: Toast.makeText(context, "Выберите стол", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        enabled = selectedTable != null && !isLoading
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Забронировать")
                    }
                }
            }
            is EstablishmentLoadState.Error -> {
                Box(Modifier.fillMaxSize()) {
                    Text("Ошибка: ${(establishmentState as EstablishmentLoadState.Error).message}", Modifier.align(Alignment.Center))
                }
            }
            else -> {}
        }

        error?.let {
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text("Ошибка") },
                text = { Text(it) },
                confirmButton = { Button(onClick = { viewModel.clearError() }) { Text("OK") } }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DateSelector(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
                showDialog = false
            },
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth
        ).show()
    }

    OutlinedTextField(
        value = selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
        onValueChange = {},
        label = { Text("Дата") },
        readOnly = true,
        trailingIcon = {
            Icon(
                Icons.Default.DateRange,
                contentDescription = null,
                Modifier.clickable { showDialog = true }
            )
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TimeSelector(
    selectedTime: LocalTime,
    operatingHours: Map<DayOfWeek, Pair<LocalTime?, LocalTime?>>,
    selectedDate: LocalDate,
    onTimeSelected: (LocalTime) -> Unit
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val newTime = LocalTime.of(hourOfDay, minute)
                onTimeSelected(newTime)
                showDialog = false
            },
            selectedTime.hour,
            selectedTime.minute,
            true
        ).show()
    }

    OutlinedTextField(
        value = selectedTime.format(DateTimeFormatter.ofPattern("HH:mm")),
        onValueChange = {},
        label = { Text("Время") },
        readOnly = true,
        trailingIcon = {
            Icon(
                Icons.Default.DateRange,
                contentDescription = null,
                Modifier.clickable { showDialog = true }
            )
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun DurationSelector(
    selectedDuration: Long,
    onDurationSelected: (Long) -> Unit
) {
    Column {
        Text("Длительность:", fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(BOOKING_DURATIONS_MINUTES) { minutes ->
                val isSelected = minutes == selectedDuration
                FilterChip(
                    selected = isSelected,
                    onClick = { onDurationSelected(minutes) },
                    label = { Text(formatDuration(minutes)) }
                )
            }
        }
    }
}

private fun formatDuration(minutes: Long): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return when{
        mins > 0 && hours > 0 ->  "$hours ч $mins мин"
        mins > 0 && hours == 0L -> "$mins мин"
        mins == 0L && hours > 0 -> "$hours ч "
        else -> "Error"
    }
}

@Composable
fun GuestCountSelector(
    numPeople: Int,
    onGuestCountChange: (Int) -> Unit
) {
    Column {
        Text("Количество гостей:", fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onGuestCountChange(numPeople - 1) }, enabled = numPeople > 1) {
                Icon(Icons.Filled.KeyboardArrowLeft, "Меньше")
            }
            Text(
                text = "$numPeople",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.width(60.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            IconButton(onClick = { onGuestCountChange(numPeople + 1) }) {
                Icon(Icons.Filled.KeyboardArrowRight, "Больше")
            }
        }
    }
}

@Composable
fun TableSelector(
    availableTables: List<TableEntity>,
    selectedTable: TableEntity?,
    requiredCapacity: Int,
    onTableSelected: (TableEntity) -> Unit
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BookingDurationSelector(
    selectedDuration: Long,
    onDurationSelected: (Long) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(BOOKING_DURATIONS_MINUTES) { duration ->
            val isSelected = duration == selectedDuration
            ElevatedCard(
                onClick = { onDurationSelected(duration) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text(
                    text = formatDuration(duration),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}