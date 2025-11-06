@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.roamly.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.entity.DTO.BookingDisplayDto
import com.example.roamly.entity.ViewModel.BookingViewModel
import com.example.roamly.entity.ViewModel.UserViewModel
import com.example.roamly.ui.screens.sealed.BookingScreens
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun UserBookingsScreen(
    navController: NavController,
    bookingViewModel: BookingViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    val bookings by bookingViewModel.userBookings.collectAsState()
    val isLoading by bookingViewModel.isLoading.collectAsState()
    val userId = userViewModel.getId()

    // Перезагрузка при необходимости
    LaunchedEffect(userId) {
        userId?.let {
            bookingViewModel.fetchUserBookings(it)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (userId == null || userId == -1L || !userViewModel.isLoggedIn()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Для просмотра бронирований необходимо авторизоваться.", color = MaterialTheme.colorScheme.error)
            }
        } else if (!isLoading && bookings.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("У вас пока нет активных бронирований.")
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(bookings) { booking ->
                    BookingItemCard(
                        booking = booking,
                        onClick = {
                            // Переход на экран деталей
                            navController.navigate(BookingScreens.BookingDetail.createRoute(booking.id))
                        }
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BookingItemCard(booking: BookingDisplayDto, onClick: () -> Unit) {
    val timeFormat = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val dateFormat = remember { DateTimeFormatter.ofPattern("dd MMMM, EEE", Locale("ru")) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ⭐ Название заведения и статус
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = booking.establishmentName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))

            // ⭐ Дата и время
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Дата",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = booking.startTime.format(dateFormat).replaceFirstChar { it.uppercase(Locale("ru")) },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(4.dp))

            // ⭐ Время и длительность
            val endTime = booking.startTime.plus(booking.durationMinutes, ChronoUnit.MINUTES)
            Text(
                text = "${booking.startTime.format(timeFormat)} - ${endTime.format(timeFormat)} (${booking.durationMinutes} мин)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))

            // ⭐ Столик
            Text(
                text = "Столик: ${booking.tableName} (до ${booking.tableMaxCapacity} чел.)",
                style = MaterialTheme.typography.bodySmall
            )
        }

        // ⭐ Индикатор перехода
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Детали",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}