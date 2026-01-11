package com.example.roamly.ui.screens.order

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.entity.DeliveryAddressDto
import com.example.roamly.entity.ViewModel.DeliveryAddressViewModel
import com.example.roamly.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryAddressScreen(
    navController: NavController,
    userId: Long,
    onAddressSelected: (DeliveryAddressDto) -> Unit,
    viewModel: DeliveryAddressViewModel = hiltViewModel()
) {
    val addresses by viewModel.addresses.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val colors = AppTheme.colors

    LaunchedEffect(userId) {
        viewModel.loadUserAddresses(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Адреса доставки", color = colors.MainText) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = colors.MainText)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            navController.navigate("order/create-address/$userId")
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить адрес", tint = colors.MainText)
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
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colors.MainSuccess)
            }
        } else if (addresses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = colors.SecondaryText
                    )
                    Text("Нет сохраненных адресов", color = colors.MainText)
                    Button(
                        onClick = {
                            navController.navigate("order/create-address/$userId")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.MainSuccess,
                            contentColor = colors.MainText
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Добавить адрес")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                items(addresses) { address ->
                    DeliveryAddressCard(
                        address = address,
                        onSelect = { onAddressSelected(address) },
                        onEdit = {
                            navController.navigate("order/edit-address/${userId}/${address.id}")
                        },
                        onDelete = {
                            viewModel.deleteAddress(userId, address.id ?: 0)
                        },
                        onSetDefault = {
                            viewModel.setDefaultAddress(userId, address.id ?: 0)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DeliveryAddressCard(
    address: DeliveryAddressDto,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit
) {
    val colors = AppTheme.colors

    Card(
        onClick = onSelect,
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
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Адрес доставки",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.MainText
                )

                if (address.isDefault) {
                    Badge(
                        containerColor = colors.MainSuccess,
                        contentColor = colors.MainText
                    ) {
                        Text("По умолчанию")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                Text("Комментарий: $it", color = colors.SecondaryText, fontStyle = MaterialTheme.typography.bodySmall.fontStyle)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!address.isDefault) {
                    OutlinedButton(
                        onClick = onSetDefault,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = colors.MainSuccess.copy(alpha = 0.1f),
                            contentColor = colors.MainSuccess
                        ),
                        border = BorderStroke(1.dp, colors.MainSuccess.copy(alpha = 0.3f)),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text("По умолчанию")
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = colors.SecondaryContainer,
                        contentColor = colors.MainText
                    ),
                    border = BorderStroke(1.dp, colors.SecondaryBorder),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Изменить")
                }

                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = colors.MainFailure.copy(alpha = 0.1f),
                        contentColor = colors.MainFailure
                    ),
                    border = BorderStroke(1.dp, colors.MainFailure.copy(alpha = 0.3f)),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Удалить")
                }
            }
        }
    }
}