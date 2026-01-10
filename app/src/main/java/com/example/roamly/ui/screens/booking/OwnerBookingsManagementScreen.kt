package com.example.roamly.ui.screens.booking

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.entity.ViewModel.BookingViewModel
import com.example.roamly.entity.ViewModel.UserViewModel
import com.example.roamly.ui.theme.AppTheme

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerBookingsManagementScreen(
    navController: NavController,
    establishmentId: Long,
    userViewModel: UserViewModel = hiltViewModel(),
    bookingViewModel: BookingViewModel = hiltViewModel()
) {
    val user by userViewModel.user.collectAsState()
    val pendingBookings by bookingViewModel.ownerPendingBookings.collectAsState()
    val isLoading by bookingViewModel.isLoading.collectAsState()
    val error by bookingViewModel.errorMessage.collectAsState()

    // Получаем название заведения для отображения
    var establishmentName by remember { mutableStateOf("") }

    LaunchedEffect(user.id) {
        user.id?.let { bookingViewModel.fetchPendingBookingsForOwner(it) }
    }

    // Получаем название заведения из списка бронирований
    LaunchedEffect(pendingBookings) {
        val booking = pendingBookings.firstOrNull { it.establishmentId == establishmentId }
        establishmentName = booking?.establishmentName ?: "Заведение #$establishmentId"
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Бронирования: $establishmentName",
                        color = AppTheme.colors.MainText
                    )
                },
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
                    containerColor = AppTheme.colors.SecondaryContainer, // ФОН ДОБАВЛЕН
                    titleContentColor = AppTheme.colors.MainText,
                    navigationIconContentColor = AppTheme.colors.MainText,
                    actionIconContentColor = AppTheme.colors.MainText
                ),
                actions = {
                    IconButton(
                        onClick = {
                            navController.navigate(
                                "owner/approved/$establishmentId"
                            )
                        }
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Одобренные брони",
                            tint = AppTheme.colors.MainText
                        )
                    }
                    IconButton(
                        onClick = { user.id?.let { bookingViewModel.fetchPendingBookingsForOwner(it) } }
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Обновить",
                            tint = AppTheme.colors.MainText
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(AppTheme.colors.MainContainer)
        ) {
            when {
                isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = AppTheme.colors.MainText
                )

                error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Ошибка: $error",
                            color = AppTheme.colors.MainFailure
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { user.id?.let { bookingViewModel.fetchPendingBookingsForOwner(it) } },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppTheme.colors.MainSuccess
                            )
                        ) {
                            Text("Повторить")
                        }
                    }
                }

                pendingBookings.filter { it.establishmentId == establishmentId }.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Build,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = AppTheme.colors.SecondaryText
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Нет новых броней на рассмотрение",
                            style = MaterialTheme.typography.titleLarge,
                            color = AppTheme.colors.SecondaryText
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Все брони обработаны",
                            color = AppTheme.colors.SecondaryText
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(pendingBookings.filter { it.establishmentId == establishmentId }) { booking ->
                            OwnerBookingCard(
                                booking = booking,
                                onApprove = { bookingViewModel.approveBooking(booking.id, user.id!!) },
                                onReject = { bookingViewModel.rejectBooking(booking.id, user.id!!) }
                            )
                        }
                    }
                }
            }
        }
    }
}