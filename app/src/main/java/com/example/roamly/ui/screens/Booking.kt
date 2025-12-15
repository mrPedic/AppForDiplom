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
import com.example.roamly.ui.theme.AppTheme
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

    // Получаем результат отмены из SavedStateHandle
    val cancellationResult by navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<String?>("booking_cancellation_result", null)
        ?.collectAsState() ?: remember { mutableStateOf<String?>(null) }

    // Показываем Snackbar при успешной отмене
    LaunchedEffect(cancellationResult) {
        if (cancellationResult == "success") {
            snackbarHostState.showSnackbar("Бронирование отменено успешно")
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = AppTheme.colors.MainContainer
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = AppTheme.colors.MainBorder
                )
            }

            when {
                userId == -1L || !isLoggedIn -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Требуется авторизация",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.MainText,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Для просмотра списка ваших бронирований, пожалуйста, войдите в свой аккаунт.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = AppTheme.colors.SecondaryText,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    navController.navigate(SealedButtonBar.Profile.route) {
                                        popUpTo(navController.graph.startDestinationId)
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppTheme.colors.MainSuccess
                                )
                            ) {
                                Text("Перейти к Профилю")
                            }
                        }
                    }
                }

                bookings.isEmpty() && !isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "У вас пока нет активных бронирований.",
                            style = MaterialTheme.typography.titleMedium,
                            color = AppTheme.colors.SecondaryText
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(5.dp),
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
                        item {
                            Spacer(modifier = Modifier.fillMaxWidth().height(103.dp))
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.SecondaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = booking.establishmentName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.MainText
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Дата",
                        modifier = Modifier.size(18.dp),
                        tint = AppTheme.colors.MainBorder
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = booking.startTime.format(dateFormat)
                            .replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.MainText
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${booking.startTime.format(timeFormat)} – ${endTime.format(timeFormat)} (${booking.durationMinutes} мин)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.SecondaryText
                )
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Столик: ${booking.tableName} (до ${booking.tableMaxCapacity} чел.)",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.SecondaryText
                )
            }

            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.End
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Подробнее",
                    tint = AppTheme.colors.MainBorder
                )
            }
        }
    }
}