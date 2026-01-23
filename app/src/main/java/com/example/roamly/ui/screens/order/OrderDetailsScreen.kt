// OrderDetailsScreen.kt
package com.example.roamly.ui.screens.order

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.entity.DTO.establishment.EstablishmentDisplayDto
import com.example.roamly.entity.LoadState
import com.example.roamly.entity.ViewModel.EstablishmentDetailViewModel
import com.example.roamly.entity.ViewModel.OrderViewModel
import com.example.roamly.ui.screens.sealed.EstablishmentScreens
import com.example.roamly.ui.theme.AppTheme
import com.example.roamly.ui.theme.toColor
import com.example.roamly.ui.theme.toRussianText
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun OrderDetailsScreen(
    navController: NavController,
    orderId: Long,
    orderViewModel: OrderViewModel = hiltViewModel(),
) {
    val currentOrder by orderViewModel.currentOrder.collectAsState()
    val isLoading by orderViewModel.isLoading.collectAsState()
    val error by orderViewModel.error.collectAsState()  // Добавлено
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    val establishmentViewModel: EstablishmentDetailViewModel = hiltViewModel()
    val establishmentState by establishmentViewModel.establishmentState.collectAsState()

    LaunchedEffect(orderId) {
        orderViewModel.loadOrderById(orderId)
    }

    // НОВОЕ: Загружаем данные заведения после загрузки заказа
    LaunchedEffect(currentOrder) {
        currentOrder?.let { order ->
            establishmentViewModel.fetchEstablishment(order.establishmentId)
        }
    }

    Scaffold(
        Modifier.background(AppTheme.colors.MainContainer),
        topBar = {
            TopAppBar(
                title = { Text("Заказ #${currentOrder?.id ?: orderId}", color = colors.MainText) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = colors.MainText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.MainContainer)
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().background(AppTheme.colors.MainContainer).padding(paddingValues)) {
            // Если ошибка
            error?.let {
                Text(
                    text = "Ошибка: $it",
                    color = colors.MainFailure,
                    modifier = Modifier.padding(16.dp)
                )
                LaunchedEffect(Unit) {
                    orderViewModel.clearError()
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.MainSuccess)
                }
            } else if (currentOrder == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Заказ не найден", color = colors.SecondaryText)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // НОВОЕ: Карточка с заведением (используем establishmentState)
                    item {
                        when (establishmentState) {
                            is LoadState.Loading -> {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = colors.SecondaryContainer)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            "Заведение",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = colors.MainText
                                        )
                                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                                    }
                                }
                            }
                            is LoadState.Success -> {
                                val establishmentName = (establishmentState as LoadState.Success).data.name
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            navController.navigate(EstablishmentScreens.EstablishmentDetail.createRoute(currentOrder!!.establishmentId))
                                        },
                                    colors = CardDefaults.cardColors(containerColor = colors.SecondaryContainer)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            "Заведение",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = colors.MainText
                                        )
                                        Text(
                                            establishmentName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.MainText
                                        )
                                    }
                                }
                            }
                            is LoadState.Error -> {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = colors.SecondaryContainer)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            "Заведение",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = colors.MainText
                                        )
                                        Text(
                                            "Ошибка загрузки: ${(establishmentState as LoadState.Error).message}",
                                            color = colors.MainFailure
                                        )
                                        Button(onClick = { establishmentViewModel.retryEstablishment(currentOrder!!.establishmentId) }) {
                                            Text("Повторить")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Цветная карточка статуса
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = currentOrder!!.status.toColor())
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    currentOrder!!.status.toRussianText(),
                                    color = colors.MainText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Информация о заказе
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = colors.SecondaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Дата создания: ${formatDateTime(currentOrder!!.createdAt)}",
                                    color = colors.SecondaryText
                                )
                                Text(
                                    "Обновлено: ${formatDateTime(currentOrder!!.updatedAt)}",
                                    color = colors.SecondaryText
                                )
                                if (currentOrder!!.rejectionReason != null) {
                                    Text(
                                        "Причина отказа: ${currentOrder!!.rejectionReason}",
                                        color = colors.MainFailure
                                    )
                                }
                            }
                        }
                    }

                    // Адрес доставки
                    currentOrder!!.deliveryAddress?.let { address ->
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = colors.SecondaryContainer)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Адрес доставки", style = MaterialTheme.typography.titleMedium)
                                    Text("${address.street}, д. ${address.house}, кв. ${address.apartment}")
                                    address.building?.let { Text("Корпус: $it") }
                                    address.entrance?.let { Text("Подъезд: $it") }
                                    address.floor?.let { Text("Этаж: $it") }
                                    address.comment?.let { Text("Комментарий: $it") }
                                }
                            }
                        }
                    }

                    // Время доставки и оплата
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = colors.SecondaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Время доставки: ${formatDeliveryTime(currentOrder!!.deliveryTime)}",
                                    color = colors.SecondaryText
                                )
                                Text(
                                    "Способ оплаты: ${currentOrder!!.paymentMethod.name}",
                                    color = colors.SecondaryText
                                )
                                Text(
                                    "Бесконтактная доставка: ${if (currentOrder!!.contactless) "Да" else "Нет"}",  // Изменено на contactless
                                    color = colors.SecondaryText
                                )
                                currentOrder!!.comments?.let {
                                    Text("Комментарии: $it", color = colors.SecondaryText)
                                }
                            }
                        }
                    }

                    // Позиции заказа
                    items(currentOrder!!.items) { orderItem ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = colors.SecondaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    orderItem.menuItemName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.MainText
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                ItemCard(
                                    price = orderItem.pricePerUnit,
                                    quantity = orderItem.quantity,
                                    totalPrice = orderItem.totalPrice
                                )
                                // Опции, если есть (теперь Map)
                                orderItem.options?.let { opts ->
                                    Text("Опции: ${opts.entries.joinToString { "${it.key}: ${it.value}" }}")
                                }
                            }
                        }
                    }

                    // Итоговая сумма
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = colors.SecondaryContainer)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Итого к оплате",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.MainText
                                )
                                Text(
                                    "${String.format("%.2f", currentOrder!!.totalPrice)} р.",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.MainSuccess
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemCard(price: Double, quantity: Int, totalPrice: Double) {
    val colors = AppTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${String.format("%.2f", price)} р. × $quantity шт.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.SecondaryText
        )

        Text(
            "Итого: ${String.format("%.2f", totalPrice)} р.",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = colors.SecondaryText
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun formatDateTime(dateTimeString: String?): String {
    if (dateTimeString.isNullOrBlank()) return ""
    return try {
        val dateTime = LocalDateTime.parse(dateTimeString, DateTimeFormatter.ISO_DATE_TIME)
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale("ru", "RU"))
        dateTime.format(formatter)
    } catch (e: Exception) {
        dateTimeString
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun formatDeliveryTime(deliveryTimeString: String): String {
    return try {
        val dateTime = LocalDateTime.parse(deliveryTimeString, DateTimeFormatter.ISO_DATE_TIME)
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale("ru", "RU"))
        dateTime.format(formatter)
    } catch (e: Exception) {
        deliveryTimeString
    }
}