@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.roamly.ui.screens.booking

import android.os.Build
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

    // Используем вычисляемое значение для определения, находится ли выбранное время в будущем
    val selectedDateTime = LocalDateTime.of(selectedDate, selectedTime)
    val now = LocalDateTime.now()
    val isSelectedTimeInFuture = selectedDateTime.isAfter(now)

    LaunchedEffect(selectedDate, selectedTime) {
        if (isSelectedTimeInFuture) {
            val iso = selectedDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            viewModel.fetchAvailableTables(establishmentId, iso)
        } else {
            // Сбрасываем список столиков, если время в прошлом
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

                    // Кастомный выбор времени
                    Text(
                        "Выберите время:",
                        fontWeight = FontWeight.SemiBold,
                        color = AppTheme.colors.MainText,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                    CustomTimePicker(
                        selectedDate = selectedDate,
                        selectedTime = selectedTime,
                        onTimeSelected = { newTime -> selectedTime = newTime }
                    )

                    // Предупреждение, если время в прошлом
                    if (!isSelectedTimeInFuture) {
                        Text(
                            "Вы выбрали прошедшее время. Пожалуйста, выберите время в будущем.",
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

                            if (!isSelectedTimeInFuture) {
                                Text(
                                    "Выберите время в будущем, чтобы увидеть доступные столы",
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
                    Button(
                        onClick = {
                            if (!isSelectedTimeInFuture) {
                                Toast.makeText(context, "Выберите время в будущем", Toast.LENGTH_SHORT).show()
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
                        enabled = selectedTable != null && isSelectedTimeInFuture && !isLoading,
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
    val maxDate = today.plusDays(30) // Ограничение: 1 месяц вперед

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
        items(dates) { date ->
            val isSelected = date == selectedDate
            val isToday = date == today
            val isDisabled = date > maxDate

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
                }
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
    onTimeSelected: (LocalTime) -> Unit
) {
    val today = LocalDate.now()
    val now = LocalTime.now()
    val isToday = selectedDate == today

    val startHour = 8
    val endHour = 22
    val intervalMinutes = 30

    val timeSlots = remember {
        val slots = mutableListOf<LocalTime>()
        for (hour in startHour..endHour) {
            for (minute in listOf(0, 30)) {
                if (hour == endHour && minute > 0) continue
                val time = LocalTime.of(hour, minute)
                slots.add(time)
            }
        }
        slots
    }

    val lazyListState = rememberLazyListState()

    LaunchedEffect(selectedTime) {
        val index = timeSlots.indexOfFirst { it == selectedTime }
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
        items(timeSlots) { time ->
            val isSelected = time == selectedTime
            // Время в прошлом только если это сегодня и время уже прошло
            val isDisabled = isToday && time.isBefore(now)

            Card(
                onClick = {
                    if (!isDisabled) {
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
                        time.format(DateTimeFormatter.ofPattern("HH:mm")),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (isDisabled) AppTheme.colors.SecondaryText else AppTheme.colors.MainText
                    )
                }
            }
        }
    }
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
                items(BOOKING_DURATIONS_MINUTES) { minutes ->
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
                    BorderStroke(1.dp, AppTheme.colors.SecondaryBorder)
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