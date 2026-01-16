package com.example.roamly.ui.screens.order

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.entity.DTO.order.DeliveryAddressDto
import com.example.roamly.entity.ViewModel.DeliveryAddressViewModel
import com.example.roamly.ui.screens.sealed.OrderScreens
import com.example.roamly.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryAddressScreen(
    navController: NavController,
    userId: Long,
    onAddressSelected: (DeliveryAddressDto) -> Unit = {},
    isSelectionMode: Boolean,
    viewModel: DeliveryAddressViewModel = hiltViewModel()
) {
    val addresses by viewModel.addresses.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    val showSetDefaultDialog by viewModel.showSetDefaultDialog.collectAsState()
    val colors = AppTheme.colors

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Автоматическая обработка ошибок и успехов
    LaunchedEffect(error) {
        error?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
                viewModel.clearError()
            }
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
                viewModel.clearSuccessMessage()
                delay(500) // Небольшая задержка перед перезагрузкой
                viewModel.loadUserAddresses(userId)
            }
        }
    }

    // Автоматический выбор адреса при возврате из создания/редактирования (если режим выбора)
    val savedSelectedAddress = navController.previousBackStackEntry?.savedStateHandle?.get<DeliveryAddressDto>("selectedAddress")
    LaunchedEffect(savedSelectedAddress) {
        if (isSelectionMode && savedSelectedAddress != null) {
            onAddressSelected(savedSelectedAddress)
            navController.previousBackStackEntry?.savedStateHandle?.remove<DeliveryAddressDto>("selectedAddress")
        }
    }

    LaunchedEffect(userId) {
        viewModel.loadUserAddresses(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isSelectionMode) "Выберите адрес" else "Адреса доставки", color = colors.MainText) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = colors.MainText)
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(OrderScreens.CreateDeliveryAddress.createRoute(userId)) }) {
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = colors.MainContainer
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.MainSuccess)
            }
        } else if (addresses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(64.dp), tint = colors.SecondaryText)
                    Text("Нет сохраненных адресов", color = colors.MainText)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(addresses) { address ->
                    AddressCard(
                        address = address,
                        onClick = {
                            if (isSelectionMode) {
                                onAddressSelected(address)
                            } else {
                                address.id?.let { navController.navigate(OrderScreens.EditDeliveryAddress.createRoute(userId, it)) }
                            }
                        },
                        onEdit = { address.id?.let { navController.navigate(OrderScreens.EditDeliveryAddress.createRoute(userId, it)) } },
                        onDelete = { address.id?.let { viewModel.showDeleteConfirmation(it) } },
                        onSetDefault = { address.id?.let { viewModel.showSetDefaultConfirmation(it) } },
                        isSelectionMode = isSelectionMode
                    )
                }
            }
        }
    }

    // Диалог подтверждения удаления
    showDeleteDialog?.let { addressId ->
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteConfirmation() },
            title = { Text("Удалить адрес?") },
            text = { Text("Вы уверены, что хотите удалить этот адрес? Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.confirmDeleteAddress(userId, addressId)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.MainFailure)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteConfirmation() }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Диалог подтверждения установки по умолчанию
    showSetDefaultDialog?.let { addressId ->
        AlertDialog(
            onDismissRequest = { viewModel.hideSetDefaultConfirmation() },
            title = { Text("Сделать основным?") },
            text = { Text("Сделать этот адрес адресом по умолчанию для будущих заказов?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.confirmSetDefaultAddress(userId, addressId)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.MainSuccess)
                ) {
                    Text("Да")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideSetDefaultConfirmation() }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun AddressCard(
    address: DeliveryAddressDto,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit,
    isSelectionMode: Boolean
) {
    val colors = AppTheme.colors

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (address.isDefault) colors.SecondarySuccess.copy(alpha = 0.15f) else colors.SecondaryContainer,
            contentColor = colors.MainText
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = if (address.isDefault) colors.MainSuccess else colors.SecondaryText
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${address.street}, д. ${address.house}${address.building?.let { ", корп. $it" } ?: ""}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.MainText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            buildString {
                                append("кв. ${address.apartment}")
                                address.entrance?.let { append(", под. $it") }
                                address.floor?.let { append(", эт. $it") }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.SecondaryText
                        )
                        if (!address.comment.isNullOrBlank()) {
                            Text(
                                address.comment,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.SecondaryText,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (address.isDefault) {
                        Icon(Icons.Default.Star, "По умолчанию", tint = colors.MainSuccess)
                    }
                }
                // Иконка редактирования в правом верхнем углу - всегда видна
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Редактировать", tint = colors.MainText)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isSelectionMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        if (address.isDefault) "По умолчанию" else "Выбрать",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (address.isDefault) colors.MainSuccess else colors.MainText,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}