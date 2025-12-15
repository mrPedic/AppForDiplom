package com.example.roamly.ui.screens.profileFR

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.roamly.entity.DTO.establishment.EstablishmentFavoriteDto
import com.example.roamly.entity.Role
import com.example.roamly.entity.ViewModel.EstablishmentViewModel
import com.example.roamly.entity.ViewModel.UserViewModel
import com.example.roamly.entity.classes.convertTypeToWord
import com.example.roamly.ui.screens.base64ToByteArray
import com.example.roamly.ui.screens.sealed.EstablishmentScreens
import com.example.roamly.ui.screens.sealed.LogSinUpScreens
import com.example.roamly.ui.theme.AppTheme

@Composable
fun ProfileScreen(
    navController: NavController,
    userViewModel: UserViewModel,
    establishmentViewModel: EstablishmentViewModel = hiltViewModel()
) {
    val user by userViewModel.user.collectAsState()
    val isLoggedIn = user.role != Role.UnRegistered

    // Base background matching Booking.kt
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppTheme.colors.MainContainer
    ) {
        if (isLoggedIn) {
            RegisteredProfileContent(navController, userViewModel, establishmentViewModel)
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

        // Action Buttons
        Button(
            onClick = {
                navController.navigate(EstablishmentScreens.UserEstablishments.route)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppTheme.colors.SecondaryContainer,
                contentColor = AppTheme.colors.MainText
            )
        ){
            Text(text = "Мои Заведения")
        }

        Button(
            onClick = {
                navController.navigate(EstablishmentScreens.CreateEstablishment.route)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppTheme.colors.MainSuccess, // Primary Action
                contentColor = AppTheme.colors.MainText
            )
        ){
            Text(text = "Создать свое заведение")
        }

        Spacer(modifier = Modifier.weight(1f))

        // Logout -> Destructive Action
        Button(
            onClick = {
                userViewModel.logout()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = AppTheme.colors.MainFailure,
                contentColor = AppTheme.colors.MainText // Or white, depending on theme, but MainText is safe
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
                    .background(AppTheme.colors.MainContainer) // Fallback background
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
                    containerColor = AppTheme.colors.MainSuccess, // Action color
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
                    containerColor = AppTheme.colors.MainBorder, // Secondary Action
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