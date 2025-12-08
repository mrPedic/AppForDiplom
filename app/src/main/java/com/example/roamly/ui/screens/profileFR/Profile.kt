package com.example.roamly.ui.screens.profileFR

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row // <-- 1. Добавлен импорт
import androidx.compose.foundation.layout.Spacer // <-- 2. Добавлен импорт
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height // <-- 3. Добавлен импорт
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults // <-- 4. Добавлен импорт
import androidx.compose.material3.Card // <-- 5. Добавлен импорт
import androidx.compose.material3.CardDefaults // <-- 6. Добавлен импорт
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.roamly.entity.ViewModel.UserViewModel
import com.example.roamly.ui.screens.sealed.LogSinUpScreens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.example.roamly.entity.DTO.EstablishmentFavoriteDto
import com.example.roamly.entity.Role
import com.example.roamly.entity.ViewModel.EstablishmentViewModel
import com.example.roamly.entity.convertTypeToWord
import com.example.roamly.ui.screens.establishment.base64ToByteArray
import com.example.roamly.ui.screens.sealed.EstablishmentScreens

@Composable
fun ProfileScreen(
    navController: NavController,
    userViewModel: UserViewModel,
    establishmentViewModel: EstablishmentViewModel = hiltViewModel()
) {
    // Логика не изменилась: по-прежнему проверяем статус авторизации
    val user by userViewModel.user.collectAsState()
    val isLoggedIn = user.role != Role.UnRegistered

    if (isLoggedIn) {
        RegisteredProfileContent(navController, userViewModel, establishmentViewModel)
    } else {
        UnRegisteredProfileContent(navController, userViewModel)
    }
}

// ----------------------------------------------------
// КОМПОНЕНТ ДЛЯ АВТОРИЗОВАННОГО ПОЛЬЗОВАТЕЛЯ
// ----------------------------------------------------
@Composable
private fun RegisteredProfileContent(
    navController: NavController,
    userViewModel: UserViewModel,
    establishmentViewModel: EstablishmentViewModel
) {
    val currentUser by userViewModel.user.collectAsState()
    val favorites by establishmentViewModel.favoriteEstablishmentsList.collectAsState()

    LaunchedEffect(currentUser.id) {
        if (currentUser.id != null) {
            establishmentViewModel.fetchFavoriteEstablishmentsList(currentUser.id!!)
        }
    }

    val buttonBarHeight = 102.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Ваш Профиль",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 32.dp, bottom = 24.dp)
        )

        // Карточка информации о пользователе
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Информация о пользователе",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                Spacer(modifier = Modifier.height(12.dp))

                InfoRow(label = "Имя:", value = currentUser.name ?: "Не указано")
                InfoRow(label = "Логин:", value = currentUser.login)
                InfoRow(label = "Роль:", value = currentUser.role.toString())
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Избранные заведения ",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
        )

        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Список избранного пуст",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Горизонтальный список избранного
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

        // Кнопки управления
        Button(
            onClick = {
                navController.navigate(EstablishmentScreens.UserEstablishments.route)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ){
            Text(text = "Мои Заведения")
        }

        Button(
            onClick = {
                navController.navigate(EstablishmentScreens.CreateEstablishment.route)
            },
            modifier = Modifier.fillMaxWidth()
        ){
            Text(text = "Создать свое заведение")
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                userViewModel.logout()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
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
            .width(160.dp) // Фиксированная ширина для горизонтального скролла
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            // Фото
            val imageBytes = remember(item.photoBase64) {
                if (!item.photoBase64.isNullOrBlank()) base64ToByteArray(item.photoBase64) else null
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color.Gray)
            ) {
                if (imageBytes != null) {
                    Image(
                        painter = rememberAsyncImagePainter(model = imageBytes),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Заглушка
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Нет фото", color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Рейтинг поверх фото
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = String.format("%.1f", item.rating),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Информация
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = convertTypeToWord(item.type),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = MaterialTheme.typography.bodySmall.fontSize * 0.9f
                )
            }
        }
    }
}

// ----------------------------------------------------
// КОМПОНЕНТ ДЛЯ НЕАВТОРИЗОВАННОГО ПОЛЬЗОВАТЕЛЯ
// ----------------------------------------------------

@Composable
private fun UnRegisteredProfileContent(
    navController: NavController,
    userViewModel: UserViewModel
) {
    // ⭐ ИСПРАВЛЕНО: Убран Box с синим фоном.
    // Используем центрированный Column со стандартным фоном.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center, // Центрируем по вертикали
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Профиль",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Вы не авторизованы",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant // Мягкий цвет
        )

        Spacer(modifier = Modifier.height(48.dp))



        Spacer(modifier = Modifier.height(48.dp))

        // --- Кнопки входа/регистрации ---
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                modifier = Modifier
                    .fillMaxWidth(0.8f) // Немного шире
                    .padding(bottom = 16.dp),
                onClick = {
                    navController.navigate(route = LogSinUpScreens.SingUp.route)
                },
            ) {
                Text(text = "Создать аккаунт")
            }

            Button(
                modifier = Modifier
                    .fillMaxWidth(0.8f),
                onClick = {
                    navController.navigate(route = LogSinUpScreens.Login.route)
                },
            ) {
                Text(text = "Войти в аккаунт")
            }
        }
    }
}

// ----------------------------------------------------
// Вспомогательные компоненты
// ----------------------------------------------------


/**
 * Вспомогательный Composable для отображения строки "Метка: Значение"
 */
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}