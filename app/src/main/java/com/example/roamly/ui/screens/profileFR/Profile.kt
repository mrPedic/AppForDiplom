package com.example.roamly.ui.screens.profileFR

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.roamly.entity.DTO.establishment.EstablishmentFavoriteDto
import com.example.roamly.entity.DTO.order.OrderDto
import com.example.roamly.entity.DTO.order.OrderStatus
import com.example.roamly.entity.Role
import com.example.roamly.entity.ViewModel.EstablishmentViewModel
import com.example.roamly.entity.ViewModel.NotificationViewModel
import com.example.roamly.entity.ViewModel.OrderViewModel
import com.example.roamly.entity.ViewModel.UserViewModel
import com.example.roamly.entity.classes.convertTypeToWord
import com.example.roamly.entity.DTO.order.toDisplayString
import com.example.roamly.ui.screens.base64ToByteArray
import com.example.roamly.ui.screens.sealed.EstablishmentScreens
import com.example.roamly.ui.screens.sealed.ProfileScreens
import com.example.roamly.ui.screens.sealed.NotificationScreens
import com.example.roamly.ui.screens.sealed.OrderScreens
import com.example.roamly.ui.theme.AppTheme
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    userViewModel: UserViewModel,
    orderViewModel: OrderViewModel,
    establishmentViewModel: EstablishmentViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel()
) {
    val user by userViewModel.user.collectAsState()
    val isLoggedIn = user.role != Role.UnRegistered
    val unreadCountState by notificationViewModel.unreadCount.collectAsState()

    LaunchedEffect(Unit) {
        if (isLoggedIn) {
            notificationViewModel.refresh()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppTheme.colors.MainContainer
    ) {
        if (isLoggedIn) {
            RegisteredProfileContent(
                navController = navController,
                userViewModel = userViewModel,
                establishmentViewModel = establishmentViewModel,
                notificationViewModel = notificationViewModel,
                orderViewModel = orderViewModel,
                unreadCount = unreadCountState
            )
        } else {
            UnRegisteredProfileContent(navController, userViewModel)
        }
    }
}

// ----------------------------------------------------
// LOGGED IN USER CONTENT
// ----------------------------------------------------
@Composable
private fun RegisteredProfileContent(
    navController: NavController,
    userViewModel: UserViewModel,
    orderViewModel: OrderViewModel,
    establishmentViewModel: EstablishmentViewModel,
    notificationViewModel: NotificationViewModel,
    unreadCount: Int
) {
    val currentUser by userViewModel.user.collectAsState()
    val favorites by establishmentViewModel.favoriteEstablishmentsList.collectAsState()
    val orders by orderViewModel.userOrders.collectAsState()
    val buttonBarHeight = 102.dp
    val error by orderViewModel.error.collectAsState()

    // Получаем цвета внутри композиции
    val colors = AppTheme.colors

    LaunchedEffect(currentUser.id) {
        if (currentUser.id != null) {
            establishmentViewModel.fetchFavoriteEstablishmentsList(currentUser.id!!)
            orderViewModel.loadUserOrders(currentUser.id!!)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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

        // Заголовок
        Text(
            text = "Ваш Профиль",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = colors.MainText,
            modifier = Modifier.padding(top = 32.dp, bottom = 24.dp)
        )

        // Карточка информации о пользователе (сделаем её кликабельной для редактирования)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    navController.navigate(ProfileScreens.EditProfile.route)
                },
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = colors.SecondaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Информация о пользователе",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.MainText
                    )
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Редактировать профиль",
                        tint = colors.MainBorder
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(
                    thickness = 1.dp,
                    color = colors.MainBorder.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(12.dp))

                InfoRow(label = "Имя:", value = currentUser.name ?: "Не указано")
                InfoRow(label = "Логин:", value = currentUser.login)
                InfoRow(label = "Роль:", value = currentUser.role.toString())
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Раздел избранных заведений
        SectionHeader(
            title = "Избранные заведения",
            showAllButton = false
        )

        if (favorites.isEmpty()) {
            EmptyStateCard(
                text = "Список избранного пуст",
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(favorites) { item ->
                    FavoriteEstablishmentCard(
                        item = item,
                        onClick = {
                            navController.navigate(EstablishmentScreens.EstablishmentDetail.createRoute(item.id))
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Раздел заказов
        SectionHeader(
            title = "Мои заказы",
            showAllButton = true,
            onAllClick = {
                navController.navigate(OrderScreens.OrderList.route)
            }
        )

        val userOrders = orders
        val activeOrders = userOrders.filter {
            it.status == OrderStatus.PENDING ||
                    it.status == OrderStatus.CONFIRMED ||
                    it.status == OrderStatus.IN_PROGRESS ||
                    it.status == OrderStatus.OUT_FOR_DELIVERY
        }

        val completedOrders = userOrders.filter {
            it.status == OrderStatus.DELIVERED ||
                    it.status == OrderStatus.CANCELLED ||
                    it.status == OrderStatus.REJECTED
        }

        if (activeOrders.isEmpty() && completedOrders.isEmpty()) {
            EmptyStateCard(
                text = "У вас пока нет заказов",
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (activeOrders.isNotEmpty()) {
                    Text(
                        "Активные заказы",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.MainSuccess,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(activeOrders) { order ->
                            OrderPreviewCard(
                                order = order,
                                onClick = {
                                    order.id?.let {
                                        navController.navigate("order/details/$it")
                                    }
                                }
                            )
                        }
                    }
                }

                if (completedOrders.isNotEmpty()) {
                    Text(
                        "Завершенные заказы",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.SecondaryText,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(completedOrders) { order ->
                            OrderPreviewCard(
                                order = order,
                                onClick = {
                                    order.id?.let {
                                        navController.navigate("order/details/$it")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Панель действий с кнопкой редактирования профиля
        ActionGrid(
            items = remember(colors, unreadCount) {
                listOf(
                    ActionItem(
                        title = "Редактировать профиль",
                        icon = Icons.Default.Edit,
                        onClick = { navController.navigate(ProfileScreens.EditProfile.route) },
                        containerColor = colors.SecondaryContainer,
                        contentColor = colors.MainText
                    ),
                    ActionItem(
                        title = "Уведомления",
                        icon = Icons.Default.Notifications,
                        badgeCount = unreadCount,
                        onClick = { navController.navigate(NotificationScreens.Notifications.route) },
                        containerColor = colors.MainBorder,
                        contentColor = colors.MainText
                    ),
                    ActionItem(
                        title = "Управление заведениями",
                        onClick = { navController.navigate(EstablishmentScreens.UserEstablishments.route) },
                        containerColor = colors.SecondaryContainer,
                        contentColor = colors.MainText
                    ),
                    ActionItem(
                        title = "Создать заведение",
                        onClick = { navController.navigate(EstablishmentScreens.CreateEstablishment.route) },
                        containerColor = colors.MainSuccess,
                        contentColor = colors.MainText
                    ),
                    ActionItem(
                        title = "Адреса доставки",
                        onClick = {
                            currentUser.id?.let { userId ->
                                navController.navigate(OrderScreens.DeliveryAddresses.createRoute(userId, false))
                            }
                        },
                        containerColor = colors.SecondaryContainer,
                        contentColor = colors.MainText
                    )
                )
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Кнопка выхода
        OutlinedButton(
            onClick = {
                userViewModel.logout()
                notificationViewModel.disconnect()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = buttonBarHeight),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = colors.MainFailure
            ),
            border = ButtonDefaults.outlinedButtonBorder
        ) {
            Text(
                text = "Выйти из аккаунта",
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoriteEstablishmentCard(
    item: EstablishmentFavoriteDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .height(200.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.SecondaryContainer)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Изображение
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val imageBytes = remember(item.photoBase64) {
                    if (!item.photoBase64.isNullOrBlank()) base64ToByteArray(item.photoBase64) else null
                }

                if (imageBytes != null) {
                    Image(
                        painter = rememberAsyncImagePainter(model = imageBytes),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AppTheme.colors.MainContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Нет фото",
                            color = AppTheme.colors.SecondaryText,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Рейтинг
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = AppTheme.colors.MainContainer.copy(alpha = 0.9f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = AppTheme.colors.MainSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f", item.rating),
                            color = AppTheme.colors.MainText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Информация
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.MainText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = convertTypeToWord(item.type),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppTheme.colors.SecondaryText,
                        maxLines = 1
                    )
                }

                Text(
                    text = item.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.SecondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ----------------------------------------------------
// NOT LOGGED IN CONTENT
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnRegisteredProfileContent(
    navController: NavController,
    userViewModel: UserViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Иконка профиля
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(AppTheme.colors.SecondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = AppTheme.colors.MainText,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Войдите в аккаунт",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.MainText
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Чтобы получить доступ ко всем функциям приложения",
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.SecondaryText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Кнопка входа
            Button(
                onClick = {
                    navController.navigate(route = ProfileScreens.Login.route)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.colors.MainSuccess
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Text(
                    text = "Войти",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Кнопка регистрации
            OutlinedButton(
                onClick = {
                    navController.navigate(route = ProfileScreens.SingUp.route)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AppTheme.colors.MainBorder
                ),
                border = ButtonDefaults.outlinedButtonBorder
            ) {
                Text(
                    text = "Создать аккаунт",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

// ----------------------------------------------------
// ВСПОМОГАТЕЛЬНЫЕ КОМПОНЕНТЫ
// ----------------------------------------------------
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.SecondaryText
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.colors.MainText
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderPreviewCard(order: OrderDto, onClick: () -> Unit) {
    val colors = AppTheme.colors

    Card(
        onClick = onClick,
        modifier = Modifier
            .width(200.dp)
            .height(120.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.SecondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Заказ #${order.id ?: "???"}",
                    fontWeight = FontWeight.Bold,
                    color = colors.MainText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${order.totalPrice} р.",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.MainSuccess
                )
            }

            Column {
                Text(
                    text = "${order.items?.size ?: 0} позиций",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.SecondaryText
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Статус заказа
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (order.status) {
                        OrderStatus.DELIVERED -> colors.MainSuccess.copy(alpha = 0.2f)
                        OrderStatus.CANCELLED, OrderStatus.REJECTED -> colors.MainFailure.copy(alpha = 0.2f)
                        else -> colors.MainBorder.copy(alpha = 0.2f)
                    }
                ) {
                    Text(
                        text = order.status.toDisplayString(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.MainText,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    showAllButton: Boolean = false,
    onAllClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.MainText
        )

        if (showAllButton && onAllClick != null) {
            TextButton(
                onClick = onAllClick,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = AppTheme.colors.MainBorder
                )
            ) {
                Text(
                    text = "Все →",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmptyStateCard(
    text: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.SecondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = AppTheme.colors.SecondaryText,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

data class ActionItem(
    val title: String,
    val icon: ImageVector? = null,
    val badgeCount: Int = 0,
    val onClick: () -> Unit,
    val containerColor: Color,
    val contentColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionGrid(
    items: List<ActionItem>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.forEach { item ->
            Button(
                onClick = item.onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = item.containerColor
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item.icon?.let { icon ->
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (item.badgeCount > 0) {
                        Badge(
                            modifier = Modifier
                        ) {
                            Text(
                                text = min(item.badgeCount, 99).toString(),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}