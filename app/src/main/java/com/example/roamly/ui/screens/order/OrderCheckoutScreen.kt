package com.example.roamly.ui.screens.order

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.entity.DTO.order.CreateOrderItem
import com.example.roamly.entity.DTO.order.CreateOrderRequest
import com.example.roamly.entity.DTO.order.DeliveryAddressDto
import com.example.roamly.entity.DTO.order.PaymentMethod
import com.example.roamly.entity.LoadState
import com.example.roamly.entity.ViewModel.DeliveryAddressViewModel
import com.example.roamly.entity.ViewModel.EstablishmentDetailViewModel
import com.example.roamly.entity.ViewModel.OrderCreationViewModel
import com.example.roamly.entity.ViewModel.OrderViewModel
import com.example.roamly.ui.screens.sealed.OrderScreens
import com.example.roamly.ui.screens.establishment.toMap
import com.example.roamly.ui.screens.sealed.SealedButtonBar
import com.example.roamly.ui.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun OrderCheckoutScreen(
    navController: NavController,
    establishmentId: Long,
    userId: Long,
    orderViewModel: OrderViewModel,
    deliveryAddressViewModel: DeliveryAddressViewModel = hiltViewModel(),
    establishmentDetailViewModel: EstablishmentDetailViewModel = hiltViewModel(),
    orderCreationViewModel: OrderCreationViewModel
) {
    val currentSavedState = navController.currentBackStackEntry?.savedStateHandle
    val savedSelectedAddress: DeliveryAddressDto? by remember {
        currentSavedState?.getStateFlow<DeliveryAddressDto?>("selectedAddress", null) ?: MutableStateFlow(null)
    }.collectAsState()

    var selectedAddress by remember { mutableStateOf<DeliveryAddressDto?>(null) }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var selectedTime by remember { mutableStateOf<String?>(null) }
    var comments by remember { mutableStateOf("") }

    val colors = AppTheme.colors

    // Получаем корзину и меню из OrderCreationViewModel
    val cartItems by orderCreationViewModel.cartItems.collectAsState()
    val menu by orderCreationViewModel.menu.collectAsState()
    val totalPrice by remember(cartItems) {
        derivedStateOf {
            val total = orderCreationViewModel.calculateTotal()
            Log.d("OrderCheckoutScreen", "Корзина на экране оформления: ${cartItems.size} позиций, сумма: $total")
            total
        }
    }

    // Получаем данные заведения
    val establishmentState by establishmentDetailViewModel.establishmentState.collectAsState()

    // Состояния из OrderViewModel
    val isOrderLoading by orderViewModel.isLoading.collectAsState()
    val orderError by orderViewModel.error.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Показываем ошибку в Snackbar
    LaunchedEffect(orderError) {
        orderError?.let { errorMsg ->
            scope.launch {
                snackbarHostState.showSnackbar(errorMsg)
                orderViewModel.clearError()
            }
        }
    }

    LaunchedEffect(Unit) {
        Log.d("OrderCheckoutScreen", "Загружаем адреса для пользователя $userId")
        deliveryAddressViewModel.loadUserAddresses(userId)

        if (menu == null) {
            Log.d("OrderCheckoutScreen", "Загружаем меню для заведения $establishmentId")
            orderCreationViewModel.loadMenu(establishmentId)
        }

        establishmentDetailViewModel.fetchEstablishment(establishmentId)
    }

    val addresses by deliveryAddressViewModel.addresses.collectAsState()

    LaunchedEffect(addresses) {
        if (selectedAddress == null) {
            selectedAddress = addresses.find { it.isDefault } ?: addresses.firstOrNull()
            Log.d("OrderCheckoutScreen", "Выбран адрес: ${selectedAddress?.street ?: "не выбран"}")
        }
    }

    LaunchedEffect(cartItems) {
        Log.d("OrderCheckoutScreen", "Корзина обновилась: ${cartItems.size} позиций")
        cartItems.forEachIndexed { index, item ->
            Log.d("OrderCheckoutScreen", "  Позиция $index: ID=${item.menuItemId}, тип=${item.menuItemType}, кол-во=${item.quantity}")
        }
    }

    LaunchedEffect(savedSelectedAddress) {
        savedSelectedAddress?.let { address ->
            selectedAddress = address
            currentSavedState?.remove<DeliveryAddressDto>("selectedAddress")
            Log.d("OrderCheckoutScreen", "Выбран новый адрес из savedStateHandle: ${address.street}")
        }
    }

    val establishment = if (establishmentState is LoadState.Success) {
        (establishmentState as LoadState.Success).data
    } else null

    // Получаем сегодняшние часы работы
    val todayDay = LocalDate.now().dayOfWeek.name.lowercase(Locale.getDefault()).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
    val hoursStr = establishment?.operatingHoursString?.toMap()?.get(todayDay) ?: "10:00-22:00"
    val parts = hoursStr.split("-")
    val openingTime = if (parts.size >= 2) LocalTime.parse(parts[0]) else LocalTime.of(10, 0)
    val closingTime = if (parts.size >= 2) LocalTime.parse(parts[1]) else LocalTime.of(22, 0)

    // Генерируем слоты времени
    val timeSlots = remember(establishment) {
        generateTimeSlots(
            openingTime = openingTime,
            closingTime = closingTime
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Оформление заказа", color = colors.MainText) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Назад",
                            tint = colors.MainText
                        )
                    }
                },
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        selectedTime?.let { time ->
                            Log.d("OrderCheckoutScreen", "Подтверждение заказа: время=$time, адрес=${selectedAddress?.id}, позиций=${cartItems.size}")

                            val orderItems: List<CreateOrderItem> = cartItems.map { item ->
                                CreateOrderItem(
                                    menuItemId = item.menuItemId,
                                    menuItemType = item.menuItemType,
                                    quantity = item.quantity,
                                    selectedOptions = item.selectedOptions
                                )
                            }

                            // Парсим время в ISO формат
                            val parsedTime = parseTimeSlotToIso(time)

                            val orderRequest = CreateOrderRequest(
                                establishmentId = establishmentId,
                                deliveryAddressId = selectedAddress?.id,
                                deliveryAddress = selectedAddress,
                                items = orderItems,
                                isContactless = false,
                                paymentMethod = paymentMethod,
                                deliveryTime = parsedTime,
                                comments = comments
                            )

                            orderViewModel.createOrder(orderRequest) { order ->
                                Log.d("OrderCheckoutScreen", "Заказ создан: ID=${order.id}")
                                orderCreationViewModel.clearCart()
                                navController.navigate(OrderScreens.OrderDetails.createRoute(order.id!!)) {
                                    popUpTo(SealedButtonBar.Home.route) { inclusive = false }
                                }
                            }
                        }
                    },
                    enabled = selectedAddress != null && selectedTime != null && cartItems.isNotEmpty() && !isOrderLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.MainSuccess,
                        contentColor = colors.MainText,
                        disabledContainerColor = colors.SecondaryBorder,
                        disabledContentColor = colors.SecondaryText
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (isOrderLoading) {
                        CircularProgressIndicator(
                            color = colors.MainText,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text("Оформить заказ на ${String.format("%.2f", totalPrice)} р.")
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = colors.MainContainer
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Корзина
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.SecondaryContainer,
                    contentColor = colors.MainText
                ),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Корзина",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = colors.MainText
                    )

                    cartItems.forEach { item ->
                        val (name, price) = orderCreationViewModel.getCartItemDetails(item)
                        val itemTotal = price * item.quantity

                        OrderItemRow(
                            name = name,
                            quantity = item.quantity,
                            price = price,
                            totalPrice = itemTotal
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = colors.SecondaryBorder
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Итого:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.MainText
                        )
                        Text(
                            "${String.format("%.2f", totalPrice)} р.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.MainText
                        )
                    }
                }
            }

            // Адрес доставки
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable {
                        navController.navigate(
                            OrderScreens.DeliveryAddresses.createRoute(
                                userId = userId,
                                isSelectionMode = true
                            )
                        )
                    },
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = colors.MainText
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Адрес доставки",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.MainText
                        )
                        Text(
                            selectedAddress?.let { addr ->
                                "${addr.street}, д. ${addr.house}${addr.building?.let { ", к.$it" } ?: ""}, кв. ${addr.apartment}"
                            } ?: "Выберите адрес",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selectedAddress != null) colors.MainText else colors.SecondaryText
                        )
                    }
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colors.MainText
                    )
                }
            }

            // Способ оплаты
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                                .clickable { paymentMethod = method }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = paymentMethod == method,
                                onClick = { paymentMethod = method },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = colors.MainSuccess,
                                    unselectedColor = colors.SecondaryText
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                when (method) {
                                    PaymentMethod.CASH -> "Наличными курьеру"
                                    PaymentMethod.CARD -> "Картой курьеру"
                                    else -> method.name.replace("_", " ").lowercase()
                                        .replaceFirstChar { it.uppercase() }
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                                    contentColor = colors.MainText
                                ),
                                shape = MaterialTheme.shapes.small,
                                elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 1.dp),
                                border = if (isSelected) BorderStroke(
                                    2.dp,
                                    colors.MainSuccess
                                ) else null
                            ) {
                                Text(
                                    slot,
                                    modifier = Modifier.padding(16.dp, 12.dp),
                                    color = colors.MainText
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                                "Например: позвонить за 5 минут до доставки, не звонить в домофон, оставить у двери и т.д.",
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

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun OrderItemRow(
    name: String,
    quantity: Int,
    price: Double,
    totalPrice: Double,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.MainText,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${String.format("%.2f", totalPrice)} р.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = colors.MainText
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${price} р. × $quantity шт.",
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
}

@RequiresApi(Build.VERSION_CODES.O)
private fun generateTimeSlots(
    openingTime: LocalTime,
    closingTime: LocalTime
): List<String> {
    val slots = mutableListOf<String>()
    val formatter = DateTimeFormatter.ofPattern("HH:mm")

    val now = LocalTime.now()
    var startTime = if (now.isBefore(openingTime)) openingTime else now

    // Округляем startTime до следующего 15-минутного интервала
    val minutes = startTime.minute
    val remainder = minutes % 15
    if (remainder != 0) {
        startTime = startTime.plusMinutes((15 - remainder).toLong())
    }

    var current = startTime
    while (current.isBefore(closingTime.minusMinutes(30))) {
        val end = current.plusMinutes(30)
        slots.add("${current.format(formatter)} - ${end.format(formatter)}")
        current = current.plusMinutes(15)
    }

    return slots
}

@RequiresApi(Build.VERSION_CODES.O)
private fun parseTimeSlotToIso(timeSlot: String): String {
    // Пример: "19:00 - 19:30" -> берём начало как время доставки
    val startTimeStr = timeSlot.split(" - ")[0]
    val today = LocalDate.now()
    val startTime = LocalTime.parse(startTimeStr)
    val dateTime = LocalDateTime.of(today, startTime)
    return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
}