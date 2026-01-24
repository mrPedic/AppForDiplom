// OwnerOrdersManagementScreen.kt
package com.example.roamly.ui.screens.order

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.entity.DTO.order.OrderDto
import com.example.roamly.entity.DTO.order.OrderStatus
import com.example.roamly.entity.ViewModel.OwnerOrdersViewModel
import com.example.roamly.ui.screens.sealed.OrderScreens
import com.example.roamly.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerOrdersManagementScreen(
    navController: NavController,
    establishmentId: Long,
    viewModel: OwnerOrdersViewModel = hiltViewModel()
) {
    val orders by viewModel.orders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()

    // Используем строковые значения для совместимости с API
    val statuses = OrderStatus.values().map { it.name }

    val statusLabels = mapOf(
        "PENDING" to "Ожидает",
        "CONFIRMED" to "Подтверждено",
        "IN_PROGRESS" to "Готовится",
        "OUT_FOR_DELIVERY" to "В пути",
        "DELIVERED" to "Доставлено",
        "CANCELLED" to "Отменено",
        "REJECTED" to "Отклонено"
    )

    LaunchedEffect(establishmentId, selectedStatus) {
        viewModel.fetchEstablishmentOrders(establishmentId, selectedStatus)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Управление заказами",
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
                    containerColor = AppTheme.colors.SecondaryContainer,
                    titleContentColor = AppTheme.colors.MainText,
                    navigationIconContentColor = AppTheme.colors.MainText
                ),
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.fetchEstablishmentOrders(establishmentId, selectedStatus)
                        }
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(AppTheme.colors.MainContainer)
        ) {
            // Фильтры по статусу
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(state = rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedStatus == null,
                    onClick = { viewModel.setStatusFilter(null) },
                    label = { Text("Все") },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = AppTheme.colors.SecondaryContainer,
                        selectedContainerColor = AppTheme.colors.MainSuccess.copy(alpha = 0.2f),
                        labelColor = AppTheme.colors.SecondaryText,
                        selectedLabelColor = AppTheme.colors.MainText
                    )
                )

                statuses.forEach { status ->
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { viewModel.setStatusFilter(status) },
                        label = { Text(statusLabels[status] ?: status) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = AppTheme.colors.SecondaryContainer,
                            selectedContainerColor = AppTheme.colors.MainSuccess.copy(alpha = 0.2f),
                            labelColor = AppTheme.colors.SecondaryText,
                            selectedLabelColor = AppTheme.colors.MainText
                        )
                    )
                }
            }

            // Контент
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = AppTheme.colors.MainSuccess
                        )
                    }

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
                                onClick = {
                                    viewModel.fetchEstablishmentOrders(establishmentId, selectedStatus)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppTheme.colors.MainSuccess
                                )
                            ) {
                                Text("Повторить")
                            }
                        }
                    }

                    orders.isEmpty() -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = AppTheme.colors.SecondaryText
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Нет заказов",
                                style = MaterialTheme.typography.titleLarge,
                                color = AppTheme.colors.SecondaryText
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Заказов по выбранному фильтру не найдено",
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
                            items(orders) { order ->
                                OwnerOrderCard(
                                    order = order,
                                    onStatusChange = { newStatus ->
                                        viewModel.updateOrderStatus(order.id ?: 0L, newStatus)
                                    },
                                    onViewDetails = {
                                        order.id?.let { orderId ->
                                            navController.navigate(
                                                OrderScreens.OrderDetails.createRoute(orderId)
                                            )
                                        }
                                    }
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
fun OwnerOrderCard(
    order: OrderDto,
    onStatusChange: (String) -> Unit,
    onViewDetails: () -> Unit
) {
    val statusColors = mapOf(
        OrderStatus.PENDING to AppTheme.colors.MainBorder,
        OrderStatus.CONFIRMED to AppTheme.colors.MainSuccess,
        OrderStatus.IN_PROGRESS to AppTheme.colors.MainBorder,
        OrderStatus.OUT_FOR_DELIVERY to AppTheme.colors.SecondaryText,
        OrderStatus.DELIVERED to AppTheme.colors.SecondarySuccess,
        OrderStatus.CANCELLED to AppTheme.colors.MainFailure,
        OrderStatus.REJECTED to AppTheme.colors.MainFailure
    )

    val statusLabels = mapOf(
        "PENDING" to "Ожидает",
        "CONFIRMED" to "Подтверждено",
        "IN_PROGRESS" to "Готовится",
        "OUT_FOR_DELIVERY" to "В пути",
        "DELIVERED" to "Доставлено",
        "CANCELLED" to "Отменено",
        "REJECTED" to "Отклонено"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.SecondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Заказ #${order.id}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.MainText
                )

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = statusColors[order.status] ?: AppTheme.colors.SecondaryContainer,
                    contentColor = AppTheme.colors.MainText
                ) {
                    Text(
                        text = statusLabels[order.status.name] ?: order.status.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Клиент: ${order.userName ?: "Неизвестно"}",
                color = AppTheme.colors.SecondaryText,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Адрес: ${order.deliveryAddress?.let { "${it.street}, ${it.house}" } ?: "Самовывоз"}",
                color = AppTheme.colors.SecondaryText,
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Сумма: ${order.totalPrice} р.",
                color = AppTheme.colors.MainText,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(12.dp))

            // Кнопки изменения статуса
            if (order.status != OrderStatus.DELIVERED &&
                order.status != OrderStatus.CANCELLED &&
                order.status != OrderStatus.REJECTED) {

                val nextStatus = when (order.status) {
                    OrderStatus.PENDING -> "CONFIRMED"
                    OrderStatus.CONFIRMED -> "IN_PROGRESS"
                    OrderStatus.IN_PROGRESS -> "OUT_FOR_DELIVERY"
                    OrderStatus.OUT_FOR_DELIVERY -> "DELIVERED"
                    else -> null
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (nextStatus != null) {
                        Button(
                            onClick = { onStatusChange(nextStatus) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppTheme.colors.MainSuccess
                            )
                        ) {
                            Text("Принять → ${statusLabels[nextStatus] ?: nextStatus}")
                        }
                    }

                    OutlinedButton(
                        onClick = { onStatusChange("CANCELLED") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AppTheme.colors.MainFailure
                        )
                    ) {
                        Text("Отменить")
                    }
                }
            }

            // Кнопка просмотра деталей
            OutlinedButton(
                onClick = onViewDetails,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Подробности")
            }
        }
    }
}