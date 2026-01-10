@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.roamly.ui.screens.booking

import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.roamly.ui.theme.AppTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

private const val TAG = "CreateBookingScreen"
private val BOOKING_DURATIONS_MINUTES = listOf(30L, 60L, 90L, 120L, 150L, 180L)

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
    var guestContact by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val establishmentState by viewModel.establishmentDetailState.collectAsState()
    val availableTables by viewModel.availableTables.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()

    // Загружаем данные заведения и доступные столики
    LaunchedEffect(Unit) {
        viewModel.fetchEstablishmentDetails(establishmentId)
    }

    // Получаем рабочие часы для выбранной даты
    val workingHours by remember(selectedDate, establishmentState) {
        derivedStateOf {
            when (establishmentState) {
                is EstablishmentLoadState.Success -> {
                    val establishment = (establishmentState as EstablishmentLoadState.Success).data
                    getWorkingHoursForDay(establishment.operatingHoursString, selectedDate.dayOfWeek)
                }
                else -> null
            }
        }
    }

    // Используем вычисляемое значение для определения, находится ли выбранное время в будущем
    val selectedDateTime = LocalDateTime.of(selectedDate, selectedTime)
    val now = LocalDateTime.now()
    val isSelectedTimeInFuture = selectedDateTime.isAfter(now)

    // Проверяем, что бронь укладывается в рабочие часы
    val isDurationValid by remember(selectedTime, selectedDuration, workingHours) {
        derivedStateOf {
            if (workingHours == null) false
            else {
                val (open, close) = workingHours!!
                val isOvernight = close.isBefore(open) || close == LocalTime.MIDNIGHT
                val openMinutes = open.toSecondOfDay() / 60L
                var closeMinutes = close.toSecondOfDay() / 60L
                if (isOvernight) closeMinutes += 1440
                var startMinutes = selectedTime.toSecondOfDay() / 60L
                if (isOvernight && selectedTime.isBefore(open)) startMinutes += 1440
                val endMinutes = startMinutes + selectedDuration
                startMinutes >= openMinutes && endMinutes <= closeMinutes
            }
        }
    }

    // Проверяем, что выбранное время доступно (не в прошлом и в рабочих часах)
    val isTimeValid = isSelectedTimeInFuture && (workingHours == null ||
            (!selectedTime.isBefore(workingHours!!.first) &&
                    !selectedTime.isAfter(workingHours!!.second.minusMinutes(30))))

    LaunchedEffect(selectedDate, selectedTime) {
        if (isTimeValid) {
            val iso = selectedDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            viewModel.fetchAvailableTables(establishmentId, iso)
        } else {
            // Сбрасываем список столиков, если время недоступно
            viewModel._availableTables.value = emptyList()
        }
    }

    Scaffold(
        modifier = Modifier.background(AppTheme.colors.MainContainer),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Бронирование",
                        color = AppTheme.colors.MainText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            "Назад",
                            tint = AppTheme.colors.MainText
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppTheme.colors.SecondaryContainer,
                    navigationIconContentColor = AppTheme.colors.MainText,
                    titleContentColor = AppTheme.colors.MainText,
                    actionIconContentColor = AppTheme.colors.MainText
                )
            )
        }
    ) { padding ->
        if (isLoading && establishmentState is EstablishmentLoadState.Idle) {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = AppTheme.colors.MainText
                )
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
                        .background(AppTheme.colors.MainContainer)
                ) {
                    // Заголовок
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 5.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.SecondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                establishment.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.MainText
                            )
                            Text(
                                establishment.address,
                                style = MaterialTheme.typography.bodyLarge,
                                color = AppTheme.colors.SecondaryText
                            )

                            // Отображаем рабочие часы для выбранного дня
                            workingHours?.let { (open, close) ->
                                Text(
                                    "Рабочие часы: ${open.format(DateTimeFormatter.ofPattern("HH:mm"))} - ${close.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppTheme.colors.SecondaryText,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    // Кастомный выбор даты
                    Text(
                        "Выберите дату:",
                        fontWeight = FontWeight.SemiBold,
                        color = AppTheme.colors.MainText,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                    )
                    CustomDatePicker(
                        selectedDate = selectedDate,
                        onDateSelected = { newDate -> selectedDate = newDate }
                    )

                    Spacer(Modifier.height(16.dp))

                    // Кастомный выбор времени с учетом рабочих часов
                    Text(
                        "Выберите время:",
                        fontWeight = FontWeight.SemiBold,
                        color = AppTheme.colors.MainText,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                    CustomTimePicker(
                        selectedDate = selectedDate,
                        selectedTime = selectedTime,
                        onTimeSelected = { newTime -> selectedTime = newTime },
                        workingHours = workingHours
                    )

                    // Предупреждения
                    if (!isSelectedTimeInFuture) {
                        Text(
                            "Вы выбрали прошедшее время. Пожалуйста, выберите время в будущем.",
                            color = AppTheme.colors.MainFailure,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 8.dp)
                        )
                    }

                    if (workingHours != null && !isDurationValid && isSelectedTimeInFuture) {
                        Text(
                            "Выбранная длительность не укладывается в рабочие часы заведения. Максимально возможное время окончания: ${workingHours!!.second.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                            color = AppTheme.colors.MainFailure,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 8.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Длительность бронирования
                    BookingDurationSelector(selectedDuration) { newDuration -> selectedDuration = newDuration }
                    Spacer(Modifier.height(16.dp))

                    // Количество гостей
                    GuestCountSelector(numberOfGuests) { newCount -> numberOfGuests = newCount }
                    Spacer(Modifier.height(16.dp))

                    // Доступные столики
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 5.dp),
                        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.SecondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Доступные столы:",
                                fontWeight = FontWeight.SemiBold,
                                color = AppTheme.colors.MainText,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            if (!isTimeValid) {
                                Text(
                                    if (workingHours == null) "Часы работы не указаны"
                                    else "Выберите время в будущем и в пределах рабочих часов, чтобы увидеть доступные столы",
                                    color = AppTheme.colors.MainFailure,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else if (availableTables.isEmpty()) {
                                Text(
                                    "Нет доступных столов на выбранное время",
                                    color = AppTheme.colors.MainFailure,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                TableSelector(
                                    availableTables,
                                    selectedTable,
                                    numberOfGuests
                                ) { table -> selectedTable = table }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Контактная информация
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 5.dp),
                        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.SecondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Контактная информация",
                                fontWeight = FontWeight.SemiBold,
                                color = AppTheme.colors.MainText,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = guestContact,
                                onValueChange = { guestContact = it },
                                label = {
                                    Text(
                                        "Телефон для связи",
                                        color = AppTheme.colors.SecondaryText
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AppTheme.colors.MainBorder,
                                    unfocusedBorderColor = AppTheme.colors.SecondaryBorder,
                                    focusedTextColor = AppTheme.colors.MainText,
                                    unfocusedTextColor = AppTheme.colors.MainText,
                                    focusedLabelColor = AppTheme.colors.SecondaryText,
                                    unfocusedLabelColor = AppTheme.colors.SecondaryText,
                                    cursorColor = AppTheme.colors.MainText
                                )
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = {
                                    Text(
                                        "Комментарий (по желанию)",
                                        color = AppTheme.colors.SecondaryText
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AppTheme.colors.MainBorder,
                                    unfocusedBorderColor = AppTheme.colors.SecondaryBorder,
                                    focusedTextColor = AppTheme.colors.MainText,
                                    unfocusedTextColor = AppTheme.colors.MainText,
                                    focusedLabelColor = AppTheme.colors.SecondaryText,
                                    unfocusedLabelColor = AppTheme.colors.SecondaryText,
                                    cursorColor = AppTheme.colors.MainText
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Кнопка бронирования
                    val isBookingEnabled = selectedTable != null &&
                            isSelectedTimeInFuture &&
                            (workingHours == null || isDurationValid) &&
                            !isLoading

                    Button(
                        onClick = {
                            if (!isSelectedTimeInFuture) {
                                Toast.makeText(context, "Выберите время в будущем", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            if (workingHours != null && !isDurationValid) {
                                Toast.makeText(context, "Выбранная длительность не укладывается в рабочие часы", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            selectedTable?.let { table ->
                                val userId = user?.id ?: return@let
                                val startTimeStr = selectedDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                val dto = BookingCreationDto(
                                    establishmentId = establishmentId,
                                    userId = userId,
                                    tableId = table.id,
                                    startTime = startTimeStr,
                                    durationMinutes = selectedDuration,
                                    numPeople = numberOfGuests,
                                    notes = notes.takeIf { it.isNotBlank() },
                                    guestPhone = guestContact
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
                            .padding(horizontal = 5.dp)
                            .height(56.dp),
                        enabled = isBookingEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppTheme.colors.MainSuccess,
                            contentColor = AppTheme.colors.MainText,
                            disabledContainerColor = AppTheme.colors.SecondaryContainer,
                            disabledContentColor = AppTheme.colors.SecondaryText
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = AppTheme.colors.MainText,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Подтвердить бронирование",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
            is EstablishmentLoadState.Error -> {
                Box(Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Build,
                            contentDescription = null,
                            tint = AppTheme.colors.MainFailure,
                            modifier = Modifier.size(64.dp).padding(bottom = 16.dp)
                        )
                        Text(
                            "Ошибка загрузки заведения",
                            style = MaterialTheme.typography.headlineSmall,
                            color = AppTheme.colors.MainFailure,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            (establishmentState as EstablishmentLoadState.Error).message ?: "Неизвестная ошибка",
                            color = AppTheme.colors.MainText,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { navController.popBackStack() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppTheme.colors.MainSuccess,
                                contentColor = AppTheme.colors.MainText
                            )
                        ) {
                            Text("Вернуться назад")
                        }
                    }
                }
            }
            else -> {}
        }

        error?.let {
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = {
                    Text(
                        "Ошибка",
                        color = AppTheme.colors.MainText
                    )
                },
                text = {
                    Text(
                        it,
                        color = AppTheme.colors.MainText
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.clearError() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppTheme.colors.MainSuccess,
                            contentColor = AppTheme.colors.MainText
                        )
                    ) {
                        Text("OK")
                    }
                },
                containerColor = AppTheme.colors.MainContainer
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CustomDatePicker(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = LocalDate.now()

    // Используем remember для кэширования списка дат
    val dates = remember {
        (0..30).map { today.plusDays(it.toLong()) }
    }

    val lazyListState = rememberLazyListState()

    LaunchedEffect(selectedDate) {
        val index = dates.indexOf(selectedDate)
        if (index >= 0) {
            lazyListState.animateScrollToItem(index)
        }
    }

    LazyRow(
        state = lazyListState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(dates, key = { it.toString() }) { date -> // Добавляем ключ для оптимизации
            val isSelected = date == selectedDate
            val isToday = date == today
            val isDisabled = date < today // Предотвращаем выбор прошедших дат

            Card(
                onClick = {
                    if (!isDisabled) {
                        onDateSelected(date)
                    }
                },
                modifier = Modifier.width(70.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AppTheme.colors.SecondaryContainer
                ),
                border = when {
                    isSelected -> BorderStroke(2.dp, AppTheme.colors.MainBorder)
                    isToday -> BorderStroke(1.dp, AppTheme.colors.SecondarySuccess)
                    else -> BorderStroke(1.dp, AppTheme.colors.SecondaryBorder)
                },
                enabled = !isDisabled
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale("ru")),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDisabled) AppTheme.colors.SecondaryText else AppTheme.colors.MainText,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Text(
                        date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isDisabled) AppTheme.colors.SecondaryText else AppTheme.colors.MainText,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    Text(
                        date.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale("ru")),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDisabled) AppTheme.colors.SecondaryText else AppTheme.colors.MainText,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CustomTimePicker(
    selectedDate: LocalDate,
    selectedTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    workingHours: Pair<LocalTime, LocalTime>? = null
) {
    val today = LocalDate.now()
    val now = LocalTime.now()
    val isToday = selectedDate == today

    val timeSlots = remember(workingHours, selectedDate) {
        if (workingHours == null) return@remember emptyList<LocalTime>()

        val (open, close) = workingHours
        val isOvernight = close.isBefore(open) || close == LocalTime.MIDNIGHT
        val openMinutes = open.toSecondOfDay() / 60L
        var closeMinutes = close.toSecondOfDay() / 60L
        if (isOvernight) closeMinutes += 1440

        val slots = mutableListOf<LocalTime>()
        var currentMinutes = openMinutes
        val maxSlots = 96

        while (currentMinutes < closeMinutes && slots.size < maxSlots) {
            val endMinutes = currentMinutes + 30
            if (endMinutes <= closeMinutes) {
                val time = LocalTime.ofSecondOfDay((currentMinutes % 1440) * 60)
                slots.add(time)
            }
            currentMinutes += 30
        }
        slots
    }

    val lazyListState = rememberLazyListState()

    LaunchedEffect(selectedTime) {
        val index = timeSlots.indexOfFirst { it == selectedTime }
        if (index >= 0) {
            lazyListState.animateScrollToItem(index)
        } else if (timeSlots.isNotEmpty()) {
            // Если выбранное время не в списке слотов, выбираем первый доступный слот
            onTimeSelected(timeSlots[0])
        }
    }

    LazyRow(
        state = lazyListState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (workingHours == null) {
            item {
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .padding(vertical = 12.dp)
                ) {
                    Text(
                        "Часы работы не указаны",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.SecondaryText
                    )
                }
            }
        } else if (timeSlots.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .padding(vertical = 12.dp)
                ) {
                    Text(
                        "Нет доступного времени",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.MainFailure
                    )
                }
            }
        } else {
            items(timeSlots) { time ->
                val isSelected = time == selectedTime
                val isDisabled = isToday && time.isBefore(now)
                val isValid = workingHours?.let { isValidBookingTime(time, it, 30L) } ?: false

                Card(
                    onClick = {
                        if (!isDisabled && isValid) {
                            onTimeSelected(time)
                        }
                    },
                    modifier = Modifier.width(70.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AppTheme.colors.SecondaryContainer
                    ),
                    border = if (isSelected) {
                        BorderStroke(2.dp, AppTheme.colors.MainBorder)
                    } else {
                        BorderStroke(1.dp, AppTheme.colors.SecondaryBorder)
                    },
                    enabled = !isDisabled && isValid
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            time.format(DateTimeFormatter.ofPattern("HH:mm")),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = when {
                                isDisabled || !isValid -> AppTheme.colors.SecondaryText
                                else -> AppTheme.colors.MainText
                            }
                        )
                    }
                }
            }
        }
    }
}

// Функция для проверки, что бронь поместится в рабочие часы
@RequiresApi(Build.VERSION_CODES.O)
private fun isValidBookingTime(
    startTime: LocalTime,
    workingHours: Pair<LocalTime, LocalTime>,
    durationMinutes: Long = 30L
): Boolean {
    val (open, close) = workingHours
    val isOvernight = close.isBefore(open) || close == LocalTime.MIDNIGHT
    val openMinutes = open.toSecondOfDay() / 60L
    var closeMinutes = close.toSecondOfDay() / 60L
    if (isOvernight) closeMinutes += 1440
    var startMinutes = startTime.toSecondOfDay() / 60L
    if (isOvernight && startTime.isBefore(open)) startMinutes += 1440
    val endMinutes = startMinutes + durationMinutes
    return startMinutes >= openMinutes && endMinutes <= closeMinutes
}

@Composable
fun BookingDurationSelector(
    selectedDuration: Long,
    onDurationSelected: (Long) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 5.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.SecondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Длительность:",
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.colors.MainText,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(BOOKING_DURATIONS_MINUTES, key = { it.toString() }) { minutes ->
                    val isSelected = minutes == selectedDuration
                    Card(
                        onClick = { onDurationSelected(minutes) },
                        modifier = Modifier.width(100.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = AppTheme.colors.SecondaryContainer
                        ),
                        border = if (isSelected) {
                            BorderStroke(2.dp, AppTheme.colors.MainBorder)
                        } else {
                            BorderStroke(1.dp, AppTheme.colors.SecondaryBorder)
                        }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = formatDuration(minutes),
                                fontWeight = FontWeight.Medium,
                                color = AppTheme.colors.MainText
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GuestCountSelector(
    numPeople: Int,
    onGuestCountChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 5.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.SecondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Количество гостей:",
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.colors.MainText,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Кнопка уменьшения
                Card(
                    onClick = { if (numPeople > 1) onGuestCountChange(numPeople - 1) },
                    modifier = Modifier.size(48.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (numPeople > 1) AppTheme.colors.SecondaryContainer
                        else AppTheme.colors.SecondaryContainer.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, AppTheme.colors.SecondaryBorder),
                    enabled = numPeople > 1
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Build,
                            contentDescription = "Уменьшить количество гостей",
                            modifier = Modifier.size(24.dp),
                            tint = if (numPeople > 1) AppTheme.colors.MainText
                            else AppTheme.colors.SecondaryText
                        )
                    }
                }

                // Отображение количества
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "$numPeople",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.MainText
                    )
                    Text(
                        text = when (numPeople) {
                            1 -> "гость"
                            in 2..4 -> "гостя"
                            else -> "гостей"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.SecondaryText
                    )
                }

                // Кнопка увеличения
                Card(
                    onClick = { onGuestCountChange(numPeople + 1) },
                    modifier = Modifier.size(48.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AppTheme.colors.SecondaryContainer
                    ),
                    border = BorderStroke(1.dp, AppTheme.colors.SecondaryBorder)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Увеличить количество гостей",
                            modifier = Modifier.size(24.dp),
                            tint = AppTheme.colors.MainText
                        )
                    }
                }
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
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        availableTables.forEach { table ->
            val isSelected = table == selectedTable
            val fitsCapacity = table.maxCapacity >= requiredCapacity

            Card(
                onClick = { if (fitsCapacity) onTableSelected(table) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = AppTheme.colors.SecondaryContainer
                ),
                border = if (isSelected) {
                    BorderStroke(2.dp, AppTheme.colors.MainBorder)
                } else {
                    BorderStroke(0.5.dp, AppTheme.colors.SecondaryBorder)
                }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                table.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = AppTheme.colors.MainText,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    "Выбран",
                                    tint = AppTheme.colors.MainSuccess,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Build,
                                "Вместимость",
                                tint = if (fitsCapacity) AppTheme.colors.MainText else AppTheme.colors.MainFailure,
                                modifier = Modifier.size(16.dp).padding(end = 4.dp)
                            )
                            Text(
                                "${table.maxCapacity} чел.",
                                color = if (fitsCapacity) AppTheme.colors.MainText else AppTheme.colors.MainFailure
                            )
                        }
                    }
                    if (!fitsCapacity) {
                        Text(
                            "Мал для ${requiredCapacity} чел.",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppTheme.colors.MainFailure
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(minutes: Long): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours > 0 && mins > 0 -> "$hours ч $mins мин"
        hours > 0 -> "$hours ч"
        else -> "$mins мин"
    }
}

private fun parseOperatingHours(operatingHoursString: String?): Map<String, String> {
    if (operatingHoursString.isNullOrBlank()) {
        Log.d(TAG, "parseOperatingHours: operatingHoursString is null or blank")
        return emptyMap()
    }

    Log.d(TAG, "parseOperatingHours: raw string = $operatingHoursString")

    return try {
        // Пробуем парсить как JSON массив
        val result = parseOperatingHoursArray(operatingHoursString)
        Log.d(TAG, "parseOperatingHours: parsed successfully, size = ${result.size}")
        result
    } catch (e: Exception) {
        Log.e(TAG, "parseOperatingHours: error parsing", e)
        emptyMap()
    }
}

private fun parseOperatingHoursArray(jsonArrayString: String): Map<String, String> {
    val result = mutableMapOf<String, String>()
    val gson = Gson()

    try {
        // Убираем пробелы и парсим как JSON массив строк
        val cleanString = jsonArrayString.trim()
        Log.d(TAG, "parseOperatingHoursArray: cleanString = $cleanString")

        val type = object : TypeToken<List<String>>() {}.type
        val list = gson.fromJson<List<String>>(cleanString, type)

        Log.d(TAG, "parseOperatingHoursArray: parsed list size = ${list.size}")
        Log.d(TAG, "parseOperatingHoursArray: list = $list")

        list.forEach { item ->
            Log.d(TAG, "parseOperatingHoursArray: processing item = $item")
            // Формат может быть: "Пн: 8:00-24:00" или "Понедельник: 8:00-24:00"
            // Ищем первое вхождение ":" которое разделяет день и время
            val colonIndex = item.indexOf(':')
            if (colonIndex > 0) {
                val day = item.substring(0, colonIndex).trim()
                val time = item.substring(colonIndex + 1).trim()
                Log.d(TAG, "parseOperatingHoursArray: day='$day', time='$time'")

                // Нормализуем названия дней
                val normalizedDay = normalizeDayName(day)
                Log.d(TAG, "parseOperatingHoursArray: normalizedDay='$normalizedDay'")

                if (time.isNotBlank() && time != "Закрыто") {
                    result[normalizedDay] = time
                    Log.d(TAG, "parseOperatingHoursArray: added to result")
                }
            } else {
                Log.d(TAG, "parseOperatingHoursArray: item doesn't contain ':', skipping")
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "parseOperatingHoursArray: error parsing JSON", e)
        // Игнорируем ошибки парсинга
    }

    Log.d(TAG, "parseOperatingHoursArray: final result size = ${result.size}")
    Log.d(TAG, "parseOperatingHoursArray: result = $result")
    return result
}

private fun normalizeDayName(day: String): String {
    val lowerDay = day.trim().lowercase(Locale.getDefault())
    val result = when {
        lowerDay.startsWith("пн") -> "Понедельник"
        lowerDay.startsWith("вт") -> "Вторник"
        lowerDay.startsWith("ср") -> "Среда"
        lowerDay.startsWith("чт") -> "Четверг"
        lowerDay.startsWith("пт") -> "Пятница"
        lowerDay.startsWith("сб") -> "Суббота"
        lowerDay.startsWith("вс") -> "Воскресенье"
        lowerDay == "понедельник" -> "Понедельник"
        lowerDay == "вторник" -> "Вторник"
        lowerDay == "среда" -> "Среда"
        lowerDay == "четверг" -> "Четверг"
        lowerDay == "пятница" -> "Пятница"
        lowerDay == "суббота" -> "Суббота"
        lowerDay == "воскресенье" -> "Воскресенье"
        else -> day.trim()
    }
    Log.d(TAG, "normalizeDayName: '$day' -> '$result'")
    return result
}

// Функция для получения часов работы на конкретный день
@RequiresApi(Build.VERSION_CODES.O)
private fun getWorkingHoursForDay(
    operatingHoursString: String?,
    dayOfWeek: DayOfWeek
): Pair<LocalTime, LocalTime>? {
    Log.d(TAG, "getWorkingHoursForDay: called with dayOfWeek = $dayOfWeek")
    Log.d(TAG, "getWorkingHoursForDay: operatingHoursString = $operatingHoursString")

    val operatingHours = parseOperatingHours(operatingHoursString)
    Log.d(TAG, "getWorkingHoursForDay: parsed operatingHours = $operatingHours")

    val dayMap = mapOf(
        DayOfWeek.MONDAY to "Понедельник",
        DayOfWeek.TUESDAY to "Вторник",
        DayOfWeek.WEDNESDAY to "Среда",
        DayOfWeek.THURSDAY to "Четверг",
        DayOfWeek.FRIDAY to "Пятница",
        DayOfWeek.SATURDAY to "Суббота",
        DayOfWeek.SUNDAY to "Воскресенье"
    )

    val dayName = dayMap[dayOfWeek] ?: return null
    val hoursString = operatingHours[dayName] ?: return null

    val times = hoursString.split("-")
    if (times.size != 2) return null

    return try {
        val openTimeStr = times[0].trim()
        var closeTimeStr = times[1].trim()

        val formatter = DateTimeFormatter.ofPattern("H:mm")

        val openTime = LocalTime.parse(openTimeStr, formatter)

        // Обрабатываем 24:00 и подобные случаи
        val closeTime = when {
            closeTimeStr == "24:00" || closeTimeStr == "00:00" -> LocalTime.MIDNIGHT
            closeTimeStr == "24" || closeTimeStr == "00" -> LocalTime.MIDNIGHT
            else -> LocalTime.parse(closeTimeStr, formatter)
        }

        Pair(openTime, closeTime)
    } catch (e: Exception) {
        Log.e(TAG, "getWorkingHoursForDay: error parsing time", e)
        null
    }
}