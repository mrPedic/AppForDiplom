package com.example.roamly.ui.theme

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.roamly.entity.DTO.order.OrderStatus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun OrderStatus.toColor(): Color {
    return when (this) {
        OrderStatus.PENDING -> AppTheme.colors.SecondarySuccess  // Ожидание
        OrderStatus.CONFIRMED -> AppTheme.colors.MainSuccess     // Подтвержден
        OrderStatus.IN_PROGRESS -> AppTheme.colors.MainBorder    // В процессе
        OrderStatus.OUT_FOR_DELIVERY -> AppTheme.colors.SecondarySuccess // Доставка
        OrderStatus.DELIVERED -> AppTheme.colors.SecondaryText   // Доставлен
        OrderStatus.CANCELLED -> AppTheme.colors.MainFailure     // Отменен
        OrderStatus.REJECTED -> AppTheme.colors.MainFailure      // Отклонен
    }
}

fun OrderStatus.toRussianText(): String {
    return when (this) {
        OrderStatus.PENDING -> "На рассмотрении"
        OrderStatus.CONFIRMED -> "Принят"
        OrderStatus.IN_PROGRESS -> "Готовится"
        OrderStatus.OUT_FOR_DELIVERY -> "В пути"
        OrderStatus.DELIVERED -> "Доставлен"
        OrderStatus.CANCELLED -> "Отменен"
        OrderStatus.REJECTED -> "Отклонен"
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun formatIsoTime(isoTime: String?): String {
    if (isoTime == null) return ""
    return try {
        val parsed = LocalDateTime.parse(isoTime)
        parsed.format(DateTimeFormatter.ofPattern("dd MMM, HH:mm"))
    } catch (e: Exception) {
        isoTime
    }
}