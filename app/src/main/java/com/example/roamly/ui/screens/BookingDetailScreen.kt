@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.roamly.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.preference.PreferenceManager
import com.example.roamly.R
import com.example.roamly.entity.DTO.booking.BookingDisplayDto
import com.example.roamly.entity.ViewModel.BookingViewModel
import com.example.roamly.entity.ViewModel.UserViewModel
import com.example.roamly.ui.theme.AppTheme
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.time.format.DateTimeFormatter
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailScreen(
    navController: NavController,
    bookingId: Long,
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

    val bookings by viewModel.bookings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val cancellationStatus by viewModel.cancellationStatus.collectAsState()

    val booking: BookingDisplayDto? = bookings.find { it.id == bookingId }

    LaunchedEffect(Unit) {
        if (bookings.isEmpty()) {
            viewModel.fetchUserBookings(userId)
        }
    }

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
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppTheme.colors.MainContainer,
                    titleContentColor = AppTheme.colors.MainText,
                    navigationIconContentColor = AppTheme.colors.MainText
                )
            )
        },
        containerColor = AppTheme.colors.MainContainer
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading && booking == null -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = AppTheme.colors.MainBorder
                    )
                }

                error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Warning, null, tint = AppTheme.colors.MainFailure, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Ошибка: $error", color = AppTheme.colors.MainFailure)
                    }
                }

                booking == null -> {
                    Text(
                        text = "Бронь не найдена",
                        modifier = Modifier.align(Alignment.Center),
                        fontSize = 18.sp,
                        color = AppTheme.colors.SecondaryText
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
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.MainText
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = booking.establishmentAddress,
            style = MaterialTheme.typography.bodyLarge,
            color = AppTheme.colors.SecondaryText
        )

        Spacer(Modifier.height(32.dp))


        InfoRow(icon = R.drawable.date_range_24px, label = "Дата и время") {
            val date = booking.startTime.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("ru")))
            val time = booking.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))
            Text("$date в $time", fontWeight = FontWeight.Medium, color = AppTheme.colors.MainText)
        }

        InfoRow(icon = R.drawable.timelapse_24px, label = "Длительность") {
            Text("${booking.durationMinutes} минут", color = AppTheme.colors.MainText)
        }

        InfoRow(icon = R.drawable.table_restaurant_24px, label = "Столик") {
            Text(booking.tableName, fontWeight = FontWeight.Medium, color = AppTheme.colors.MainText)
        }

        InfoRow(icon = R.drawable.diversity_3_24px, label = "Гостей") {
            Text(booking.tableMaxCapacity.toString(), color = AppTheme.colors.MainText)
        }

        InfoRow(icon = R.drawable.info_i_24px, label = "Статус") {
            val (text, statusColor) = when (booking.status.uppercase()) {
                "PENDING" -> "Ожидает подтверждения" to AppTheme.colors.SecondarySuccess
                "CONFIRMED" -> "Подтверждено" to AppTheme.colors.MainSuccess
                "CANCELLED", "REJECTED" -> "Отменено" to AppTheme.colors.MainFailure
                else -> booking.status to AppTheme.colors.SecondaryText
            }
            Text(text, color = statusColor, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(32.dp))

        // Мини-карта заведения
        Text(
            text = "Расположение заведения",
            style = MaterialTheme.typography.titleMedium,
            color = AppTheme.colors.MainText
        )
        Spacer(Modifier.height(8.dp))
        MiniMapSection(latitude = booking.establishmentLatitude, longitude = booking.establishmentLongitude)

        Spacer(Modifier.height(20.dp))

        if (booking.status.uppercase() in listOf("PENDING", "CONFIRMED")) {
            Button(
                onClick = onCancelClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.colors.MainFailure,
                    contentColor = AppTheme.colors.MainText
                )
            ) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Отменить бронь", fontSize = 16.sp)
            }
        } else {
            AssistChip(
                onClick = { },
                label = { Text("Бронь нельзя отменить") },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = AppTheme.colors.SecondaryContainer,
                    labelColor = AppTheme.colors.SecondaryText,
                    leadingIconContentColor = AppTheme.colors.SecondaryText
                )
            )
        }

        Spacer(Modifier.height(105.dp))
    }
}

@Composable
private fun InfoRow(
    icon: Int,  // Изменено с ImageVector на Int
    label: String,
    content: @Composable (() -> Unit)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = icon),  // Изменено с imageVector на painter
            contentDescription = null,
            tint = AppTheme.colors.MainBorder,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = AppTheme.colors.SecondaryText
            )
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
fun MiniMapSection(latitude: Double, longitude: Double) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clipToBounds()
            .clip(MaterialTheme.shapes.medium)
            .background(AppTheme.colors.SecondaryContainer)
    ) {
        AndroidView(
            factory = { ctx ->
                Configuration.getInstance()
                    .load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(false)
                    setBuiltInZoomControls(false)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

                    controller.setZoom(15.0)
                    controller.setCenter(GeoPoint(latitude, longitude))

                    overlays.add(Marker(this).apply {
                        position = GeoPoint(latitude, longitude)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    })

                    invalidate()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Невидимый слой для блокировки касаний
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { /* Блокируем все касания */ }
        )
    }
}