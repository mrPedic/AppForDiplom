package com.example.roamly.ui.screens.order

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.entity.*
import com.example.roamly.entity.ViewModel.DeliveryAddressViewModel
import com.example.roamly.entity.ViewModel.EstablishmentDetailViewModel
import com.example.roamly.entity.ViewModel.OrderViewModel
import com.example.roamly.ui.theme.AppTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun OrderCheckoutScreen(
    navController: NavController,
    establishmentId: Long,
    userId: Long,
    orderViewModel: OrderViewModel = hiltViewModel(),
    deliveryAddressViewModel: DeliveryAddressViewModel = hiltViewModel()
) {
    var selectedAddress by remember { mutableStateOf<DeliveryAddressDto?>(null) }
    var isContactless by remember { mutableStateOf(false) }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var selectedTime by remember { mutableStateOf<String?>(null) }
    var comments by remember { mutableStateOf("") }

    val timeSlots = remember { generateTimeSlots() }
    val colors = AppTheme.colors

    // Загружаем адреса пользователя
    LaunchedEffect(userId) {
        deliveryAddressViewModel.loadUserAddresses(userId)
    }

    val addresses by deliveryAddressViewModel.addresses.collectAsState()

    // Устанавливаем адрес по умолчанию
    LaunchedEffect(addresses) {
        if (selectedAddress == null) {
            selectedAddress = addresses.find { it.isDefault } ?: addresses.firstOrNull()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Оформление заказа", color = colors.MainText) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.MainContainer.copy(alpha = 0.95f),
                    scrolledContainerColor = colors.MainContainer,
                    navigationIconContentColor = colors.MainText,
                    titleContentColor = colors.MainText,
                    actionIconContentColor = colors.MainText
                )
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    selectedTime?.let { time ->
                        val orderRequest = CreateOrderRequest(
                            establishmentId = establishmentId,
                            deliveryAddressId = selectedAddress?.id,
                            deliveryAddress = selectedAddress,
                            items = emptyList(), // TODO: Получить из корзины
                            isContactless = isContactless,
                            paymentMethod = paymentMethod,
                            deliveryTime = time,
                            comments = comments
                        )

                        orderViewModel.createOrder(orderRequest) { order ->
                            navController.navigate("order/details/${order.id}")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = selectedAddress != null && selectedTime != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.MainSuccess,
                    contentColor = colors.MainText
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Подтвердить заказ")
            }
        },
        containerColor = colors.MainContainer
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
        ) {
            // Выбор адреса доставки
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.SecondaryContainer,
                    contentColor = colors.MainText
                ),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Адрес доставки",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = colors.MainText
                    )

                    // Показать текущий выбранный адрес
                    selectedAddress?.let { address ->
                        Column {
                            Text(
                                "${address.street}, д. ${address.house}${address.building?.let { ", корп. $it" } ?: ""}",
                                color = colors.MainText
                            )
                            Text("Квартира: ${address.apartment}", color = colors.SecondaryText)
                            address.entrance?.let {
                                Text("Подъезд: $it", color = colors.SecondaryText)
                            }
                            address.floor?.let {
                                Text("Этаж: $it", color = colors.SecondaryText)
                            }
                            address.comment?.let {
                                Text("Комментарий: $it", color = colors.SecondaryText)
                            }
                        }
                    } ?: run {
                        Text("Адрес не выбран", color = colors.SecondaryText)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                navController.navigate("orders/delivery-addresses/$userId")
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.MainSuccess,
                                contentColor = colors.MainText
                            ),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(Icons.Default.List, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Выбрать адрес")
                        }

                        OutlinedButton(
                            onClick = {
                                navController.navigate("order/create-address/$userId")
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = colors.MainSuccess.copy(alpha = 0.1f),
                                contentColor = colors.MainSuccess
                            ),
                            border = BorderStroke(1.dp, colors.MainSuccess.copy(alpha = 0.3f)),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Новый адрес")
                        }
                    }
                }
            }

            // Бесконтактная доставка
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.SecondaryContainer,
                    contentColor = colors.MainText
                ),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Бесконтактная доставка", color = colors.MainText)
                    Switch(
                        checked = isContactless,
                        onCheckedChange = { isContactless = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.MainSuccess,
                            checkedTrackColor = colors.MainSuccess.copy(alpha = 0.5f),
                            uncheckedThumbColor = colors.UnSelectedItem,
                            uncheckedTrackColor = colors.UnSelectedItem.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            // Способ оплаты
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.SecondaryContainer,
                    contentColor = colors.MainText
                ),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Способ оплаты",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = colors.MainText
                    )

                    PaymentMethod.values().forEach { method ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = paymentMethod == method,
                                onClick = { paymentMethod = method },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = colors.MainSuccess,
                                    unselectedColor = colors.UnSelectedItem
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                when (method) {
                                    PaymentMethod.CASH -> "Наличными курьеру"
                                    PaymentMethod.CARD -> "Картой курьеру"
                                    PaymentMethod.ONLINE -> "Онлайн оплата"
                                },
                                color = colors.MainText
                            )
                        }
                    }
                }
            }

            // Время доставки
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.SecondaryContainer,
                    contentColor = colors.MainText
                ),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Время доставки",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = colors.MainText
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(timeSlots.size) { index ->
                            val slot = timeSlots[index]
                            val isSelected = selectedTime == slot

                            Card(
                                onClick = { selectedTime = slot },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) {
                                        colors.MainSuccess
                                    } else {
                                        colors.SecondaryContainer
                                    },
                                    contentColor = if (isSelected) colors.MainText else colors.MainText
                                ),
                                shape = MaterialTheme.shapes.small,
                                elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 1.dp)
                            ) {
                                Text(
                                    slot,
                                    modifier = Modifier.padding(16.dp, 12.dp),
                                    color = if (isSelected) colors.MainText else colors.MainText
                                )
                            }
                        }
                    }
                }
            }

            // Комментарий
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.SecondaryContainer,
                    contentColor = colors.MainText
                ),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Комментарий к заказу",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = colors.MainText
                    )

                    OutlinedTextField(
                        value = comments,
                        onValueChange = { comments = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Например: позвонить за 5 минут до доставки",
                                color = colors.SecondaryText
                            )
                        },
                        maxLines = 3,
                        shape = MaterialTheme.shapes.small,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.MainBorder,
                            unfocusedBorderColor = colors.SecondaryBorder,
                            focusedTextColor = colors.MainText,
                            unfocusedTextColor = colors.MainText
                        )
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun generateTimeSlots(): List<String> {
    val slots = mutableListOf<String>()
    val formatter = DateTimeFormatter.ofPattern("HH:mm")

    val startTime = LocalTime.of(10, 0)
    val endTime = LocalTime.of(22, 0)

    var current = startTime
    while (current.isBefore(endTime.minusMinutes(30))) {
        val end = current.plusMinutes(30)
        slots.add("${current.format(formatter)} - ${end.format(formatter)}")
        current = current.plusMinutes(15)
    }

    return slots
}