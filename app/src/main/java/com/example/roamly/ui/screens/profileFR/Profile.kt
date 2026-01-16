package com.example.roamly.ui.screens.profileFR

import android.util.Log
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.roamly.ui.screens.sealed.LogSinUpScreens
import com.example.roamly.ui.screens.sealed.NotificationScreens
import com.example.roamly.ui.screens.sealed.OrderScreens
import com.example.roamly.ui.theme.AppTheme
import com.example.roamly.websocket.SockJSManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

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

    // Получаем StateFlow значения как State
    val unreadCountState by notificationViewModel.unreadCount.collectAsState()

    LaunchedEffect(unreadCountState) {
        Log.d("ProfileScreen", "🔄 Unread count updated: $unreadCountState")
    }

    // 🔥 НОВОЕ: Обновляем уведомления при каждом открытии экрана профиля
    LaunchedEffect(Unit) {
        if (isLoggedIn) {
            notificationViewModel.refresh()
        }
    }
    val connectionState by notificationViewModel.connectionState.collectAsState()

    // Base background matching Booking.kt
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

    // 🔥 НОВОЕ: Обновляем избранные заведения при каждом открытии
    LaunchedEffect(currentUser.id) {
        if (currentUser.id != null) {
            establishmentViewModel.fetchFavoriteEstablishmentsList(currentUser.id!!)
        }
    }

    // 🔥 НОВОЕ: Обновляем уведомления при каждом открытии
    LaunchedEffect(Unit) {
        notificationViewModel.refresh()
    }

    val connectionState by notificationViewModel.connectionState.collectAsState()
    val lastMessage = notificationViewModel.lastMessage

    val buttonBarHeight = 102.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Ваш Профиль",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.MainText,
            modifier = Modifier.padding(top = 32.dp, bottom = 24.dp)
        )

        // User Info Card -> Matches BookingItemCard style
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.SecondaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Информация о пользователе",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.MainText,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                HorizontalDivider(
                    color = AppTheme.colors.MainBorder.copy(alpha = 0.5f),
                    thickness = DividerDefaults.Thickness
                )
                Spacer(modifier = Modifier.height(12.dp))

                InfoRow(label = "Имя:", value = currentUser.name ?: "Не указано")
                InfoRow(label = "Логин:", value = currentUser.login)
                InfoRow(label = "Роль:", value = currentUser.role.toString())
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Избранные заведения",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.MainText,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
        )

        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(AppTheme.colors.SecondaryContainer, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Список избранного пуст",
                    color = AppTheme.colors.SecondaryText
                )
            }
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

        // Заказы


        Text(
            text = "Мои заказы",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.MainText,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
        )

        val userOrders by orderViewModel.userOrders.collectAsState()
        val user = userViewModel.user.collectAsState()

        LaunchedEffect(user.value.id) {
            user.value.id?.let {
                orderViewModel.loadUserOrders(it)
            }
        }

// Активные заказы (PENDING, CONFIRMED, IN_PROGRESS, OUT_FOR_DELIVERY)
        val activeOrders = userOrders.filter {
            it.status == OrderStatus.PENDING ||
                    it.status == OrderStatus.CONFIRMED ||
                    it.status == OrderStatus.IN_PROGRESS ||
                    it.status == OrderStatus.OUT_FOR_DELIVERY
        }

// Завершенные заказы
        val completedOrders = userOrders.filter {
            it.status == OrderStatus.DELIVERED ||
                    it.status == OrderStatus.CANCELLED ||
                    it.status == OrderStatus.REJECTED
        }

        if (activeOrders.isEmpty() && completedOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(AppTheme.colors.SecondaryContainer, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("У вас пока нет заказов")
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (activeOrders.isNotEmpty()) {
                    Text(
                        "Активные заказы",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.MainSuccess
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(activeOrders) { order ->
                            OrderPreviewCard(order = order, onClick = {
                                order.id?.let {
                                    navController.navigate("order/details/$it")
                                }
                            })
                        }
                    }
                }

                if (completedOrders.isNotEmpty()) {
                    Text(
                        "Завершенные заказы",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.SecondaryText
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(completedOrders) { order ->
                            OrderPreviewCard(order = order, onClick = {
                                order.id?.let {
                                    navController.navigate("order/details/$it")
                                }
                            })
                        }
                    }
                }
            }

            Button(
                onClick = {
                    navController.navigate(OrderScreens.OrderList.route)
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text("Все заказы")
            }
        }

        // Action Buttons
        Button(
            onClick = {
                navController.navigate(EstablishmentScreens.UserEstablishments.route)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppTheme.colors.SecondaryContainer,
                contentColor = AppTheme.colors.MainText
            )
        ) {
            Text(text = "Мои Заведения")
        }

        // 🆕 Кнопка уведомлений
        Box(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    navController.navigate(NotificationScreens.Notifications.route)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.colors.MainBorder,
                    contentColor = AppTheme.colors.MainText
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Уведомления",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Уведомления")
                }
            }

            // Бейдж с количеством непрочитанных
            if (unreadCount > 0) {
                Badge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-8).dp, y = 8.dp)
                ) {
                    Text(min(unreadCount, 99).toString())
                }
            }
        }

        Button(
            onClick = {
                navController.navigate(EstablishmentScreens.CreateEstablishment.route)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppTheme.colors.MainSuccess,
                contentColor = AppTheme.colors.MainText
            )
        ) {
            Text(text = "Создать свое заведение")
        }

        // Кнопка управления адресами доставки
        Button(
            onClick = {
                currentUser.id?.let { userId ->
                    navController.navigate(OrderScreens.DeliveryAddresses.createRoute(userId, false))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppTheme.colors.SecondaryContainer,
                contentColor = AppTheme.colors.MainText
            )
        ) {
            Text(text = "Мои адреса доставки")
        }

        Spacer(modifier = Modifier.height(50.dp))

        // Logout -> Destructive Action
        Button(
            onClick = {
                userViewModel.logout()
                notificationViewModel.disconnect()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = AppTheme.colors.MainFailure,
                contentColor = AppTheme.colors.MainText
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = buttonBarHeight)
        ) {
            Text(text = "Выйти из аккаунта")
        }
    }
}

@Composable
fun FavoriteEstablishmentCard(
    item: EstablishmentFavoriteDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.SecondaryContainer)
    ) {
        Column {
            val imageBytes = remember(item.photoBase64) {
                if (!item.photoBase64.isNullOrBlank()) base64ToByteArray(item.photoBase64) else null
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(AppTheme.colors.MainContainer)
            ) {
                if (imageBytes != null) {
                    Image(
                        painter = rememberAsyncImagePainter(model = imageBytes),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Нет фото",
                            color = AppTheme.colors.SecondaryText,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Rating Badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = AppTheme.colors.MainContainer.copy(alpha = 0.8f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = AppTheme.colors.MainSuccess,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = String.format("%.1f", item.rating),
                            color = AppTheme.colors.MainText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Info Section
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.MainText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = convertTypeToWord(item.type),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTheme.colors.SecondaryText,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.SecondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = MaterialTheme.typography.bodySmall.fontSize * 0.9f
                )
            }
        }
    }
}

// ----------------------------------------------------
// NOT LOGGED IN CONTENT
// ----------------------------------------------------

@Composable
private fun UnRegisteredProfileContent(
    navController: NavController,
    userViewModel: UserViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Профиль",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.MainText
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Вы не авторизованы",
            style = MaterialTheme.typography.titleMedium,
            color = AppTheme.colors.SecondaryText
        )

        Spacer(modifier = Modifier.height(48.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(bottom = 16.dp),
                onClick = {
                    navController.navigate(route = LogSinUpScreens.SingUp.route)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.colors.MainSuccess,
                    contentColor = AppTheme.colors.MainText
                )
            ) {
                Text(text = "Создать аккаунт")
            }

            Button(
                modifier = Modifier.fillMaxWidth(0.8f),
                onClick = {
                    navController.navigate(route = LogSinUpScreens.Login.route)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.colors.MainBorder,
                    contentColor = AppTheme.colors.MainText
                )
            ) {
                Text(text = "Войти в аккаунт")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = AppTheme.colors.SecondaryText
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.colors.MainText
        )
    }
}
// Исправленный OrderPreviewCard в Profile.kt
@Composable
fun OrderPreviewCard(order: OrderDto, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(200.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                "Заказ #${order.id}",
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "${order.items.size} позиций",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                "${order.totalPrice} ₽",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = order.status.toDisplayString(),
                style = MaterialTheme.typography.labelSmall,
                color = when (order.status) {
                    OrderStatus.DELIVERED -> Color.Green
                    OrderStatus.REJECTED -> Color.Red
                    else -> AppTheme.colors.MainText
                }
            )
        }
    }
}
