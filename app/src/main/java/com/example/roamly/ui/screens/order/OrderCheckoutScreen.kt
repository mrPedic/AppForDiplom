package com.example.roamly.ui.screens.order

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.roamly.ui.screens.sealed.EstablishmentScreens
import com.example.roamly.ui.screens.sealed.SealedButtonBar
import com.example.roamly.ui.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
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

    // --- ИЗМЕНЕНИЕ 1: Список доступных дат (2 недели) ---
    val availableDates = remember {
        val today = LocalDate.now()
        (0..14).map { today.plusDays(it.toLong()) }
    }

    // Выбранная дата (по умолчанию сегодня)
    var orderDate by remember { mutableStateOf(LocalDate.now()) }

    val colors = AppTheme.colors

    // Get Cart and Menu
    val cartItems by orderCreationViewModel.cartItems.collectAsState()
    val menu by orderCreationViewModel.menu.collectAsState()
    val totalPrice by remember(cartItems) {
        derivedStateOf {
            val total = orderCreationViewModel.calculateTotal()
            total
        }
    }

    // Get Establishment Data
    val establishmentState by establishmentDetailViewModel.establishmentState.collectAsState()

    // OrderViewModel States
    val isOrderLoading by orderViewModel.isLoading.collectAsState()
    val orderError by orderViewModel.error.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Show Error Snackbar
    LaunchedEffect(orderError) {
        orderError?.let { errorMsg ->
            scope.launch {
                snackbarHostState.showSnackbar(errorMsg)
                orderViewModel.clearError()
            }
        }
    }

    LaunchedEffect(Unit) {
        deliveryAddressViewModel.loadUserAddresses(userId)

        if (menu == null) {
            orderCreationViewModel.loadMenu(establishmentId)
        }

        establishmentDetailViewModel.fetchEstablishment(establishmentId)
    }

    val addresses by deliveryAddressViewModel.addresses.collectAsState()

    LaunchedEffect(addresses) {
        if (selectedAddress == null) {
            selectedAddress = addresses.find { it.isDefault } ?: addresses.firstOrNull()
        }
    }

    LaunchedEffect(savedSelectedAddress) {
        savedSelectedAddress?.let { address ->
            selectedAddress = address
            currentSavedState?.remove<DeliveryAddressDto>("selectedAddress")
        }
    }

    val establishment = if (establishmentState is LoadState.Success) {
        (establishmentState as LoadState.Success).data
    } else null

    // 1. Helper to map English DayOfWeek to Russian keys in JSON
    fun getRussianDayKey(day: DayOfWeek): String {
        return when (day) {
            DayOfWeek.MONDAY -> "Пн"
            DayOfWeek.TUESDAY -> "Вт"
            DayOfWeek.WEDNESDAY -> "Ср"
            DayOfWeek.THURSDAY -> "Чт"
            DayOfWeek.FRIDAY -> "Пт"
            DayOfWeek.SATURDAY -> "Сб"
            DayOfWeek.SUNDAY -> "Вс"
        }
    }

    // 2. Generate Time Slots logic
    val timeSlots = remember(establishment, orderDate) {
        if (establishment == null) return@remember emptyList<String>()

        val russianDayKey = getRussianDayKey(orderDate.dayOfWeek)
        // Получаем часы работы для конкретного дня недели
        val hoursStr = establishment.operatingHoursString?.toMap()?.get(russianDayKey) ?: "10:00-22:00"

        val parts = hoursStr.split("-")
        // Если заведение закрыто в этот день (например, "Выходной" или пустая строка), возвращаем пустой список
        if (parts.size < 2) return@remember emptyList<String>()

        val openingTime = try { LocalTime.parse(parts[0]) } catch (e: Exception) { LocalTime.of(10, 0) }
        val closingTime = try { LocalTime.parse(parts[1]) } catch (e: Exception) { LocalTime.of(22, 0) }

        // Determine "Start Calculation Time"
        val isToday = orderDate.isEqual(LocalDate.now())
        val startCalculationTime = if (isToday) LocalTime.now().plusMinutes(45) else openingTime

        generateTimeSlots(
            startCalculationTime = startCalculationTime,
            openingTime = openingTime,
            closingTime = closingTime
        )
    }

    // 3. Auto-switch logic (если сегодня уже поздно, переключаем на завтра)
    // Изменено: срабатывает только если выбрана дата "Сегодня" и слотов нет
    LaunchedEffect(establishment, timeSlots) {
        if (establishment != null && timeSlots.isEmpty() && orderDate.isEqual(LocalDate.now())) {
            Log.d("OrderCheckout", "No slots for today, switching to tomorrow")
            orderDate = LocalDate.now().plusDays(1)
            selectedTime = null
        }
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
                            val orderItems: List<CreateOrderItem> = cartItems.map { item ->
                                CreateOrderItem(
                                    menuItemId = item.menuItemId,
                                    menuItemType = item.menuItemType,
                                    quantity = item.quantity,
                                    selectedOptions = item.selectedOptions
                                )
                            }

                            val parsedTime = parseTimeSlotToIso(time, orderDate)

                            val orderRequest = CreateOrderRequest(
                                userId = userId,
                                establishmentId = establishmentId,
                                deliveryAddressId = selectedAddress?.id,
                                deliveryAddress = selectedAddress,
                                items = orderItems,
                                contactless = false,
                                paymentMethod = paymentMethod,
                                deliveryTime = parsedTime,
                                comments = comments
                            )

                            orderViewModel.createOrder(orderRequest) { order ->
                                orderCreationViewModel.clearCart()
                                navController.navigate(OrderScreens.OrderDetails.createRoute(order.id!!)) {
                                    popUpTo(EstablishmentScreens.EstablishmentDetail.createRoute(establishmentId)) { inclusive = false }
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
                        disabledContainerColor = colors.SecondaryContainer,
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
            // Basket (Code remains same)
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
                        OrderItemRow(name, item.quantity, price, itemTotal)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = colors.SecondaryBorder)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Итого:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.MainText)
                        Text("${String.format("%.2f", totalPrice)} р.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.MainText)
                    }
                }
            }

            // Address Card (Code remains same)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { navController.navigate(OrderScreens.DeliveryAddresses.createRoute(userId = userId, isSelectionMode = true)) },
                colors = CardDefaults.cardColors(containerColor = colors.SecondaryContainer, contentColor = colors.MainText),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = colors.MainText)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Адрес доставки", style = MaterialTheme.typography.titleMedium, color = colors.MainText)
                        Text(
                            selectedAddress?.let { addr -> "${addr.street}, д. ${addr.house}${addr.building?.let { ", к.$it" } ?: ""}, кв. ${addr.apartment}" } ?: "Выберите адрес",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selectedAddress != null) colors.MainText else colors.SecondaryText
                        )
                    }
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = colors.MainText)
                }
            }

            // --- ИЗМЕНЕНИЕ 2: Карточка выбора Даты и Времени ---
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
                Column(modifier = Modifier.padding(vertical = 16.dp)) {
                    Text(
                        "Время доставки",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.MainText,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Выбор Даты (Горизонтальный список)
                    Text(
                        "Дата",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.SecondaryText,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableDates) { date ->
                            val isSelected = orderDate.isEqual(date)
                            val isToday = date.isEqual(LocalDate.now())
                            val isTomorrow = date.isEqual(LocalDate.now().plusDays(1))

                            val dateText = when {
                                isToday -> "Сегодня"
                                isTomorrow -> "Завтра"
                                else -> date.format(DateTimeFormatter.ofPattern("d MMM", Locale("ru")))
                            }

                            val dayOfWeekText = if (isToday || isTomorrow) {
                                date.format(DateTimeFormatter.ofPattern("d.MM"))
                            } else {
                                date.format(DateTimeFormatter.ofPattern("EEE", Locale("ru")))
                                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                            }

                            Card(
                                onClick = {
                                    if (!isSelected) {
                                        orderDate = date
                                        selectedTime = null // Сброс времени при смене даты
                                    }
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) colors.MainSuccess else colors.MainContainer,
                                    contentColor = colors.MainText
                                ),
                                border = if (isSelected) BorderStroke(1.dp, colors.MainSuccess) else BorderStroke(1.dp, colors.SecondaryBorder),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = dateText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = dayOfWeekText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) colors.MainText.copy(alpha = 0.8f) else colors.SecondaryText
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Выбор Времени (Слоты)
                    Text(
                        "Время",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.SecondaryText,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    if (timeSlots.isEmpty()) {
                        Text(
                            "Нет доступных слотов на выбранную дату",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.SecondaryText,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(timeSlots.size) { index ->
                                val slot = timeSlots[index]
                                val isSelected = selectedTime == slot

                                Card(
                                    onClick = { selectedTime = slot },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) colors.MainSuccess else colors.MainContainer,
                                        contentColor = colors.MainText
                                    ),
                                    shape = MaterialTheme.shapes.small,
                                    elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 0.dp),
                                    border = if (isSelected) BorderStroke(1.dp, colors.MainSuccess) else BorderStroke(1.dp, colors.SecondaryBorder)
                                ) {
                                    Text(
                                        slot,
                                        modifier = Modifier.padding(16.dp, 12.dp),
                                        color = colors.MainText,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Payment Method (Code remains same)
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
                    Text("Способ оплаты", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp), color = colors.MainText)
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
                                colors = RadioButtonDefaults.colors(selectedColor = colors.MainSuccess, unselectedColor = colors.SecondaryText)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                when (method) {
                                    PaymentMethod.CASH -> "Наличными курьеру"
                                    PaymentMethod.CARD -> "Картой курьеру"
                                    else -> method.name
                                },
                                color = colors.MainText
                            )
                        }
                    }
                }
            }

            // Comments (Code remains same)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = colors.SecondaryContainer, contentColor = colors.MainText),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Комментарий к заказу", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp), color = colors.MainText)
                    OutlinedTextField(
                        value = comments,
                        onValueChange = { comments = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Например: позвонить за 5 минут до доставки...", color = colors.SecondaryText) },
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
    startCalculationTime: LocalTime,
    openingTime: LocalTime,
    closingTime: LocalTime
): List<String> {
    val slots = mutableListOf<String>()
    val formatter = DateTimeFormatter.ofPattern("HH:mm")

    var startTime = startCalculationTime

    // Ensure we don't start before opening
    if (startTime.isBefore(openingTime)) {
        startTime = openingTime
    }

    // Round up to next 15 minutes
    val minutes = startTime.minute
    val remainder = minutes % 15
    if (remainder != 0) {
        startTime = startTime.plusMinutes((15 - remainder).toLong())
    }

    var current = startTime
    // Logic: check if slot ENDS before closing
    // We stop generating if the slot would end after closing - 30 mins
    while (current.isBefore(closingTime.minusMinutes(30)) || current.equals(closingTime.minusMinutes(30))) {
        val end = current.plusMinutes(30)
        // Double check strict closing time
        if (end.isAfter(closingTime)) break

        slots.add("${current.format(formatter)} - ${end.format(formatter)}")
        current = current.plusMinutes(15)
    }

    return slots
}

@RequiresApi(Build.VERSION_CODES.O)
private fun parseTimeSlotToIso(timeSlot: String, date: LocalDate): String {
    val startTimeStr = timeSlot.split(" - ")[0]
    val startTime = LocalTime.parse(startTimeStr)
    val dateTime = LocalDateTime.of(date, startTime)
    return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
}