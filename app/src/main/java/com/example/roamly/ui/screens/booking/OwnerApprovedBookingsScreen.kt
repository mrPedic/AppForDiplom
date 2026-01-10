package com.example.roamly.ui.screens.booking

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.entity.DTO.booking.OwnerBookingDisplayDto
import com.example.roamly.entity.ViewModel.BookingViewModel
import com.example.roamly.entity.ViewModel.UserViewModel
import com.example.roamly.ui.theme.AppTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun OwnerApprovedBookingsScreen(
    navController: NavController,
    establishmentId: Long,
    userViewModel: UserViewModel = hiltViewModel(),
    bookingViewModel: BookingViewModel = hiltViewModel()
) {
    val user by userViewModel.user.collectAsState()
    val approvedBookings by bookingViewModel.ownerApprovedBookings.collectAsState()
    val isLoading by bookingViewModel.isLoading.collectAsState()
    val error by bookingViewModel.errorMessage.collectAsState()

    var selectedStatus by remember { mutableStateOf("CONFIRMED") }
    var dateFilter by remember { mutableStateOf("сегодня") }

    val statuses = listOf("CONFIRMED", "COMPLETED", "CANCELLED", "NO_SHOW")
    val dateFilters = listOf("сегодня", "завтра", "неделя", "все")

    val statusLabels = mapOf(
        "CONFIRMED" to "Подтверждено",
        "COMPLETED" to "Завершено",
        "CANCELLED" to "Отменено",
        "NO_SHOW" to "Неявка"
    )

    val dateLabels = mapOf(
        "сегодня" to "Сегодня",
        "завтра" to "Завтра",
        "неделя" to "Неделя",
        "все" to "Все"
    )

    LaunchedEffect(user.id, establishmentId) {
        user.id?.let {
            bookingViewModel.fetchApprovedBookingsForOwner(it, establishmentId)
        }
    }

    val filteredBookings = approvedBookings.filter { booking ->
        booking.status.name == selectedStatus && filterByDate(booking.startTime, dateFilter)
    }

    Scaffold(
        containerColor = AppTheme.colors.MainContainer,  // Фон экрана под тему
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Одобренные брони", color = AppTheme.colors.MainText) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Назад",
                            tint = AppTheme.colors.MainText
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppTheme.colors.SecondaryContainer,  // Фон TopAppBar
                    titleContentColor = AppTheme.colors.MainText,
                    navigationIconContentColor = AppTheme.colors.MainText
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(AppTheme.colors.MainContainer)  // Фон контента
        ) {
            // Фильтры по статусу с горизонтальным скроллом
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                statuses.forEach { status ->
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { selectedStatus = status },
                        label = { Text(statusLabels[status] ?: status, color = if (selectedStatus == status) AppTheme.colors.MainText else AppTheme.colors.SecondaryText) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = AppTheme.colors.SecondaryContainer,
                            selectedContainerColor = AppTheme.colors.MainSuccess.copy(alpha = 0.2f),
                            labelColor = AppTheme.colors.SecondaryText,
                            selectedLabelColor = AppTheme.colors.MainText
                        )
                    )
                }
            }

            // Фильтры по дате с горизонтальным скроллом
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dateFilters.forEach { filter ->
                    FilterChip(
                        selected = dateFilter == filter,
                        onClick = { dateFilter = filter },
                        label = { Text(dateLabels[filter] ?: filter.capitalize(), color = if (dateFilter == filter) AppTheme.colors.MainText else AppTheme.colors.SecondaryText) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = AppTheme.colors.SecondaryContainer,
                            selectedContainerColor = AppTheme.colors.MainSuccess.copy(alpha = 0.2f),
                            labelColor = AppTheme.colors.SecondaryText,
                            selectedLabelColor = AppTheme.colors.MainText
                        )
                    )
                }
            }

            // Список бронирований
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = AppTheme.colors.MainSuccess  // Цвет индикатора
                    )
                } else if (error != null) {
                    Text(
                        "Ошибка: $error",
                        modifier = Modifier.align(Alignment.Center),
                        color = AppTheme.colors.MainFailure,  // Цвет ошибки
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else if (filteredBookings.isEmpty()) {
                    Text(
                        "Нет броней",
                        modifier = Modifier.align(Alignment.Center),
                        color = AppTheme.colors.SecondaryText,  // Цвет пустого списка
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AppTheme.colors.MainContainer),  // Дополнительный фон для списка
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(filteredBookings) { booking ->
                            OwnerBookingCard(
                                booking = booking,
                                onApprove = {}, // Уже одобрено, скрыть кнопку или обработать по-другому
                                onReject = {}  // Уже одобрено, скрыть кнопку или обработать по-другому
                            )
                        }
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun filterByDate(startTime: LocalDateTime, filter: String): Boolean {
    val today = LocalDate.now()
    return when (filter) {
        "сегодня" -> startTime.toLocalDate() == today
        "завтра" -> startTime.toLocalDate() == today.plusDays(1)
        "неделя" -> startTime.toLocalDate().isAfter(today) && startTime.toLocalDate().isBefore(today.plusDays(7))
        "все" -> true
        else -> true
    }
}