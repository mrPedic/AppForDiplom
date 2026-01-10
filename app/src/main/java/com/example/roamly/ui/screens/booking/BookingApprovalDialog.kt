package com.example.roamly.ui.screens.booking

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.roamly.entity.DTO.booking.OwnerBookingDisplayDto
import com.example.roamly.ui.theme.AppTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BookingApprovalDialog(
    booking: OwnerBookingDisplayDto,
    onDismiss: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    isVisible: Boolean = true
) {
    if (!isVisible) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            colors = CardDefaults.cardColors(
                containerColor = AppTheme.colors.MainContainer
            ),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Заголовок
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Новая бронь",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.MainText
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = AppTheme.colors.SecondaryText
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Информация о брони
                BookingInfoSection(booking)

                Spacer(Modifier.height(24.dp))

                // Кнопки действий
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppTheme.colors.MainFailure,
                            contentColor = AppTheme.colors.MainText
                        )
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Отклонить")
                    }

                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppTheme.colors.MainSuccess,
                            contentColor = AppTheme.colors.MainText
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Одобрить")
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun BookingInfoSection(booking: OwnerBookingDisplayDto) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Информация о заведении
        InfoRow(
            icon = Icons.Default.Build,
            label = "Заведение",
            value = booking.establishmentName
        )

        // Информация о госте
        InfoRow(
            icon = Icons.Default.Person,
            label = "Гость",
            value = booking.userName
        )

        booking.userPhone?.let { phone ->
            InfoRow(
                icon = Icons.Default.Phone,
                label = "Телефон",
                value = phone
            )
        }

        // Детали брони
        InfoRow(
            icon = Icons.Default.Build,
            label = "Столик",
            value = "№${booking.tableNumber}"
        )

        InfoRow(
            icon = Icons.Default.Build,
            label = "Количество гостей",
            value = booking.numberOfGuests.toString()
        )

        // Время
        InfoRow(
            icon = Icons.Default.Build,
            label = "Дата и время",
            value = formatDateTime(booking.startTime)
        )

        InfoRow(
            icon = Icons.Default.Build,
            label = "Продолжительность",
            value = "${calculateDuration(booking.startTime, booking.endTime)} мин"
        )

        // Статус
        InfoRow(
            icon = Icons.Default.Info,
            label = "Статус",
            value = when (booking.status) {
                com.example.roamly.entity.DTO.booking.BookingStatus.PENDING -> "Ожидает подтверждения"
                com.example.roamly.entity.DTO.booking.BookingStatus.CONFIRMED -> "Подтверждено"
                com.example.roamly.entity.DTO.booking.BookingStatus.CANCELLED -> "Отменено"
                com.example.roamly.entity.DTO.booking.BookingStatus.COMPLETED -> "Завершено"
                com.example.roamly.entity.DTO.booking.BookingStatus.NO_SHOW -> "Неявка"
                else -> "Неизвестно"
            }
        )
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppTheme.colors.MainBorder,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = AppTheme.colors.SecondaryText
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.MainText,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun formatDateTime(dateTime: LocalDateTime): String {
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    return "${dateTime.format(dateFormatter)} в ${dateTime.format(timeFormatter)}"
}

@RequiresApi(Build.VERSION_CODES.O)
private fun calculateDuration(start: LocalDateTime, end: LocalDateTime): Int {
    return java.time.Duration.between(start, end).toMinutes().toInt()
}