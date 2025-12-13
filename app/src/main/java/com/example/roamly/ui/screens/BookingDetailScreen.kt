// BookingDetailScreen.kt — полностью исправленная версия
@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.roamly.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.entity.DTO.booking.BookingDisplayDto
import com.example.roamly.entity.ViewModel.BookingViewModel
import com.example.roamly.entity.ViewModel.UserViewModel
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailScreen(
    navController: NavController,
    bookingId: Long,  // ← Обязательно передаём ID!
    viewModel: BookingViewModel = hiltViewModel()
) {
    val userViewModel: UserViewModel = hiltViewModel()
    val user by userViewModel.user.collectAsState()

    val userId = user.id ?: run {
        navController.popBackStack()
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Состояния
    val bookings by viewModel.bookings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val cancellationStatus by viewModel.cancellationStatus.collectAsState()

    // Находим бронь в списке или показываем заглушку
    val booking: BookingDisplayDto? = bookings.find { it.id == bookingId }

    // Загружаем брони пользователя при открытии экрана (если не загружены)
    LaunchedEffect(Unit) {
        if (bookings.isEmpty()) {
            viewModel.fetchUserBookings(userId)
        }
    }

    // Обработка отмены
    LaunchedEffect(cancellationStatus) {
        if (cancellationStatus == true) {
            Toast.makeText(context, "Бронь отменена", Toast.LENGTH_LONG).show()
            navController.popBackStack()
        } else if (cancellationStatus == false) {
            Toast.makeText(context, "Ошибка при отмене", Toast.LENGTH_SHORT).show()
        }
        if (cancellationStatus != null) {
            viewModel.clearCancellationStatus()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Детали брони") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
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
            when {
                isLoading && booking == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Ошибка: $error", color = MaterialTheme.colorScheme.error)
                    }
                }

                booking == null -> {
                    Text(
                        text = "Бронь не найдена",
                        modifier = Modifier.align(Alignment.Center),
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    BookingDetailContent(
                        booking = booking,
                        onCancelClick = {
                            scope.launch {
                                viewModel.cancelBooking(booking.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun BookingDetailContent(
    booking: BookingDisplayDto,
    onCancelClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = booking.establishmentName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = booking.establishmentAddress,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        InfoRow(icon = Icons.Default.Info, label = "Дата и время") {
            val date = booking.startTime.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("ru")))
            val time = booking.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))
            Text("$date в $time", fontWeight = FontWeight.Medium)
        }

        InfoRow(icon = Icons.Default.Info, label = "Длительность") {
            Text("${booking.durationMinutes} минут")
        }

        InfoRow(icon = Icons.Default.Info, label = "Столик") {
            Text(booking.tableName, fontWeight = FontWeight.Medium)
        }

        InfoRow(icon = Icons.Default.Info, label = "Гостей") {
            Text(booking.tableMaxCapacity.toString())
        }

        InfoRow(icon = Icons.Default.Info, label = "Статус") {
            val (text, color) = when (booking.status.uppercase()) {
                "PENDING" -> "Ожидает подтверждения" to Color(0xFFFFA000)
                "CONFIRMED" -> "Подтверждено" to Color(0xFF4CAF50)
                "CANCELLED", "REJECTED" -> "Отменено" to Color(0xFFF44336)
                else -> booking.status to MaterialTheme.colorScheme.onSurface
            }
            Text(text, color = color, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(40.dp))

        // Кнопка отмены
        if (booking.status.uppercase() in listOf("PENDING", "CONFIRMED")) {
            Button(
                onClick = onCancelClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Отменить бронь", fontSize = 16.sp, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        } else {
            AssistChip(
                onClick = { },
                label = { Text("Бронь нельзя отменить") },
                leadingIcon = { Icon(Icons.Default.Info, null) }
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}