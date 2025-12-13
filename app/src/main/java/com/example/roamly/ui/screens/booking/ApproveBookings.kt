// ApproveBookingsScreen.kt
package com.example.roamly.ui.screens.booking

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApproveBookingsScreen(
    navController: NavController,
    userViewModel: UserViewModel = hiltViewModel(),
    bookingViewModel: BookingViewModel = hiltViewModel()
) {
    val user by userViewModel.user.collectAsState()
    val bookings by bookingViewModel.ownerPendingBookings.collectAsState()
    val isLoading by bookingViewModel.isLoading.collectAsState()
    val error by bookingViewModel.errorMessage.collectAsState()

    LaunchedEffect(user.id) {
        user.id?.let { bookingViewModel.fetchPendingBookingsForOwner(it) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Одобрение броней") },
                actions = {
                    IconButton(onClick = { bookingViewModel.fetchPendingBookingsForOwner(user.id!!) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Ошибка: $error", color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { user.id?.let { bookingViewModel.fetchPendingBookingsForOwner(it) } }) {
                            Text("Повторить")
                        }
                    }
                }

                bookings.isEmpty() -> {
                    Text(
                        text = "Нет новых броней на рассмотрении",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(bookings, key = { it.id }) { booking ->
                            OwnerBookingCard(
                                booking = booking,
                                onApprove = { bookingViewModel.approveBooking(booking.id, user.id!!) },
                                onReject = { bookingViewModel.rejectBooking(booking.id,user.id!!) }
                            )
                        }
                        item{
                            Spacer(modifier = Modifier.fillMaxWidth().height(83.dp))
                        }
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerBookingCard(
    booking: OwnerBookingDisplayDto,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = booking.establishmentName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatDate(booking.startTime),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Гость: ${booking.userName}", style = MaterialTheme.typography.bodyLarge)
                booking.userPhone?.let { Text("Телефон: $it") }
                Text("Столик: №${booking.tableNumber} • Гостей: ${booking.numberOfGuests}")
                Text(
                    text = "Время: ${formatTime(booking.startTime)} – ${formatTime(booking.endTime)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Одобрить")
                }
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Отклонить")
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun formatDate(dateTime: LocalDateTime): String {
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    val date = dateTime.toLocalDate()

    return when (date) {
        today -> "Сегодня"
        tomorrow -> "Завтра"
        else -> dateTime.format(DateTimeFormatter.ofPattern("d MMMM", Locale("ru")))
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun formatTime(dateTime: LocalDateTime): String {
    return dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
}