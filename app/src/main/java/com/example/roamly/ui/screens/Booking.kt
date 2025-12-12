@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.roamly.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.entity.DTO.booking.BookingDisplayDto
import com.example.roamly.entity.ViewModel.BookingViewModel
import com.example.roamly.entity.ViewModel.UserViewModel
import com.example.roamly.ui.screens.sealed.BookingScreens
import com.example.roamly.ui.screens.sealed.SealedButtonBar
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
    val bookings by bookingViewModel.bookings.collectAsState()
    val isLoading by bookingViewModel.isLoading.collectAsState()
    val user by userViewModel.user.collectAsState()
    val userId = user.id ?: -1L
    val isLoggedIn = userViewModel.isLoggedIn()

    val snackbarHostState = remember { SnackbarHostState() }

    // Получаем результат отмены из SavedStateHandle (как String)
    val cancellationResult by navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<String?>("booking_cancellation_result", null)
        ?.collectAsState() ?: remember { mutableStateOf<String?>(null) }

    // Показываем Snackbar при успешной отмене
    LaunchedEffect(cancellationResult) {
        if (cancellationResult == "success") {
            snackbarHostState.showSnackbar("Бронирование отменено успешно")
            // Очищаем значение, чтобы не показывалось повторно
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.set("booking_cancellation_result", null)
        }
    }

    // Загружаем бронирования пользователя
    LaunchedEffect(userId) {
        if (userId != -1L) {
            bookingViewModel.fetchUserBookings(userId)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            when {
                userId == -1L || !isLoggedIn -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Требуется авторизация",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Для просмотра списка ваших бронирований, пожалуйста, войдите в свой аккаунт.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = {
                                navController.navigate(SealedButtonBar.Profile.route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }) {
                                Text("Перейти к Профилю")
                            }
                        }
                    }
                }

                bookings.isEmpty() && !isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "У вас пока нет активных бронирований.",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(bookings, key = { it.id }) { booking ->
                            BookingItemCard(
                                booking = booking,
                                onClick = {
                                    navController.navigate(BookingScreens.BookingDetail.createRoute(booking.id))
                                }
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
@Composable
fun BookingItemCard(booking: BookingDisplayDto, onClick: () -> Unit) {
    val timeFormat = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val dateFormat = remember { DateTimeFormatter.ofPattern("dd MMMM, EEE", Locale("ru")) }
    val endTime = remember(booking.startTime, booking.durationMinutes) {
        booking.startTime.plus(booking.durationMinutes, ChronoUnit.MINUTES)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        // Главный Row для разделения контента и иконки
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp) // Общий padding для Row
        ) {

            // Column для всего текстового контента
            Column(
                modifier = Modifier.weight(1f) // Занимает всё доступное место, кроме места для иконки
            ) {
                Text(
                    text = booking.establishmentName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Дата",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = booking.startTime.format(dateFormat)
                            .replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${booking.startTime.format(timeFormat)} – ${endTime.format(timeFormat)} (${booking.durationMinutes} мин)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Столик: ${booking.tableName} (до ${booking.tableMaxCapacity} чел.)",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Column для иконки, которая будет прижата к низу
            Column(
                modifier = Modifier.fillMaxHeight(), // Важно, чтобы этот Column занимал всю высоту Row
                verticalArrangement = Arrangement.Bottom, // Прижимает содержимое (иконку) к низу
                horizontalAlignment = Alignment.End // Выравнивает содержимое (иконку) по правому краю (необходимо, если Column не .fillMaxWidth(), как в данном случае)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Подробнее",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}