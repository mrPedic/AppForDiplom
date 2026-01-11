package com.example.roamly.ui.screens.order

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.entity.DeliveryAddressDto
import com.example.roamly.entity.ViewModel.DeliveryAddressViewModel
import com.example.roamly.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDeliveryAddressScreen(
    navController: NavController,
    userId: Long,
    addressId: Long,
    viewModel: DeliveryAddressViewModel = hiltViewModel()
) {
    var street by remember { mutableStateOf("") }
    var house by remember { mutableStateOf("") }
    var building by remember { mutableStateOf("") }
    var apartment by remember { mutableStateOf("") }
    var entrance by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var isDefault by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val addresses by viewModel.addresses.collectAsState()
    val colors = AppTheme.colors

    // Загружаем адрес при инициализации
    LaunchedEffect(addressId, addresses) {
        val address = addresses.find { it.id == addressId }
        address?.let {
            street = it.street
            house = it.house
            building = it.building ?: ""
            apartment = it.apartment
            entrance = it.entrance ?: ""
            floor = it.floor ?: ""
            comment = it.comment ?: ""
            isDefault = it.isDefault
        }
    }

    // Если адрес не найден, загружаем список адресов
    LaunchedEffect(userId) {
        if (addresses.isEmpty()) {
            viewModel.loadUserAddresses(userId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Редактирование адреса", color = colors.MainText) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = colors.MainText)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val address = DeliveryAddressDto(
                                id = addressId,
                                userId = userId,
                                street = street,
                                house = house,
                                building = if (building.isNotEmpty()) building else null,
                                apartment = apartment,
                                entrance = if (entrance.isNotEmpty()) entrance else null,
                                floor = if (floor.isNotEmpty()) floor else null,
                                comment = if (comment.isNotEmpty()) comment else null,
                                isDefault = isDefault
                            )

                            viewModel.updateAddress(userId, addressId, address) {
                                navController.popBackStack()
                            }
                        },
                        enabled = street.isNotEmpty() && house.isNotEmpty() && apartment.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Сохранить", tint = colors.MainText)
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
        containerColor = colors.MainContainer
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
        ) {
            if (error != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.MainFailure.copy(alpha = 0.1f)
                    ),
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Text(
                        error!!,
                        modifier = Modifier.padding(16.dp),
                        color = colors.MainFailure
                    )
                }
            }

            // Основная информация
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.SecondaryContainer,
                    contentColor = colors.MainText
                ),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Основная информация",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp),
                        color = colors.MainText
                    )

                    OutlinedTextField(
                        value = street,
                        onValueChange = { street = it },
                        label = { Text("Улица *", color = colors.SecondaryText) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.MainBorder,
                            unfocusedBorderColor = colors.SecondaryBorder,
                            focusedTextColor = colors.MainText,
                            unfocusedTextColor = colors.MainText
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = house,
                            onValueChange = { house = it },
                            label = { Text("Дом *", color = colors.SecondaryText) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = MaterialTheme.shapes.small,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.MainBorder,
                                unfocusedBorderColor = colors.SecondaryBorder,
                                focusedTextColor = colors.MainText,
                                unfocusedTextColor = colors.MainText
                            )
                        )

                        OutlinedTextField(
                            value = building,
                            onValueChange = { building = it },
                            label = { Text("Корпус", color = colors.SecondaryText) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = MaterialTheme.shapes.small,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.MainBorder,
                                unfocusedBorderColor = colors.SecondaryBorder,
                                focusedTextColor = colors.MainText,
                                unfocusedTextColor = colors.MainText
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = apartment,
                            onValueChange = { apartment = it },
                            label = { Text("Квартира *", color = colors.SecondaryText) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = MaterialTheme.shapes.small,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.MainBorder,
                                unfocusedBorderColor = colors.SecondaryBorder,
                                focusedTextColor = colors.MainText,
                                unfocusedTextColor = colors.MainText
                            )
                        )

                        OutlinedTextField(
                            value = entrance,
                            onValueChange = { entrance = it },
                            label = { Text("Подъезд", color = colors.SecondaryText) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = MaterialTheme.shapes.small,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.MainBorder,
                                unfocusedBorderColor = colors.SecondaryBorder,
                                focusedTextColor = colors.MainText,
                                unfocusedTextColor = colors.MainText
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = floor,
                        onValueChange = { floor = it },
                        label = { Text("Этаж", color = colors.SecondaryText) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
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

            // Дополнительная информация
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
                        "Дополнительная информация",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp),
                        color = colors.MainText
                    )

                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Комментарий для курьера", color = colors.SecondaryText) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = MaterialTheme.shapes.small,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.MainBorder,
                            unfocusedBorderColor = colors.SecondaryBorder,
                            focusedTextColor = colors.MainText,
                            unfocusedTextColor = colors.MainText
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Использовать по умолчанию", color = colors.MainText)
                        Switch(
                            checked = isDefault,
                            onCheckedChange = { isDefault = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.MainSuccess,
                                checkedTrackColor = colors.MainSuccess.copy(alpha = 0.5f),
                                uncheckedThumbColor = colors.UnSelectedItem,
                                uncheckedTrackColor = colors.UnSelectedItem.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }

            // Кнопка удаления
            OutlinedButton(
                onClick = {
                    viewModel.deleteAddress(userId, addressId)
                    navController.popBackStack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = colors.MainFailure.copy(alpha = 0.1f),
                    contentColor = colors.MainFailure
                ),
                border = BorderStroke(1.dp, colors.MainFailure.copy(alpha = 0.3f)),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Удалить адрес")
            }

            // Кнопка сохранения
            Button(
                onClick = {
                    val address = DeliveryAddressDto(
                        id = addressId,
                        userId = userId,
                        street = street,
                        house = house,
                        building = if (building.isNotEmpty()) building else null,
                        apartment = apartment,
                        entrance = if (entrance.isNotEmpty()) entrance else null,
                        floor = if (floor.isNotEmpty()) floor else null,
                        comment = if (comment.isNotEmpty()) comment else null,
                        isDefault = isDefault
                    )

                    viewModel.updateAddress(userId, addressId, address) {
                        navController.popBackStack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = street.isNotEmpty() && house.isNotEmpty() && apartment.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.MainSuccess,
                    contentColor = colors.MainText
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = colors.MainText
                    )
                } else {
                    Text("Сохранить изменения")
                }
            }
        }
    }
}