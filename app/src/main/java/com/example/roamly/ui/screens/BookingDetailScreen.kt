@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.roamly.ui.screens

import android.content.Context
import android.os.Build
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.entity.DTO.BookingDisplayDto
import com.example.roamly.entity.ViewModel.BookingViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BookingDetailScreen(
    navController: NavController,
    bookingId: Long,
    bookingViewModel: BookingViewModel = hiltViewModel()
) {
    // Получаем бронь из ViewModel (предполагая, что она уже загружена в UserBookingsScreen)
    val booking = bookingViewModel.getBookingById(bookingId)

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(booking?.establishmentName ?: "Детали бронирования") })
        }
    ) { paddingValues ->
        if (booking == null) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Бронирование не найдено.", color = MaterialTheme.colorScheme.error)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                BookingInfoSection(booking)

                // ⭐ Вставляем секцию с картой
                MapSection(booking)

                // Кнопка отмены (заглушка)
                Button(
                    onClick = { /* TODO: Логика отмены брони */ },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Отменить бронирование", color = Color.White)
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// ⭐ Вспомогательные Composable
// ------------------------------------------------------------------

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BookingInfoSection(booking: BookingDisplayDto) {
    val timeFormat = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val dateFormat = remember { DateTimeFormatter.ofPattern("EEEE, dd MMMM", Locale("ru")) }
    val endTime = booking.startTime.plus(booking.durationMinutes, ChronoUnit.MINUTES)

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Бронирование # ${booking.id}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = booking.establishmentName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            // Используем компонент из UserBookingsScreen
            BookingStatusBadge(status = booking.status)
        }
        HorizontalDivider(Modifier.padding(vertical = 12.dp))

        BookingDetailRow(
            icon = Icons.Default.LocationOn,
            label = "Адрес",
            value = booking.establishmentAddress
        )

        BookingDetailRow(
            icon = Icons.Default.Info,
            label = "Дата и время",
            value = booking.startTime.format(dateFormat).replaceFirstChar { it.uppercase(Locale("ru")) }
        )

        BookingDetailRow(
            icon = Icons.Default.Info,
            label = "Продолжительность",
            value = "${booking.startTime.format(timeFormat)} - ${endTime.format(timeFormat)} (${booking.durationMinutes} мин)"
        )

        BookingDetailRow(
            icon = Icons.Default.Info,
            label = "Столик",
            value = "${booking.tableName} (вместимость: ${booking.tableMaxCapacity} чел.)"
        )
    }
}

@Composable
fun BookingDetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun MapSection(booking: BookingDisplayDto) {
    val context = LocalContext.current
    val establishmentLocation = GeoPoint(booking.establishmentLatitude, booking.establishmentLongitude)

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = "Местоположение",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp) // Фиксированная высота для карты
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(16.0)
                        controller.setCenter(establishmentLocation)
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        val marker = Marker(this).apply {
                            position = establishmentLocation
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = booking.establishmentName
                        }
                        this.overlays.add(marker)
                        this.invalidate()
                    }
                },
                update = { mapView ->
                    mapView.controller.setCenter(establishmentLocation)
                    mapView.invalidate()
                }
            )
        }
    }
}