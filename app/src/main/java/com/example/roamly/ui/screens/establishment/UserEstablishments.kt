package com.example.roamly.ui.screens.establishment

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.entity.DTO.establishment.EstablishmentDisplayDto
import com.example.roamly.entity.EstablishmentStatus
import com.example.roamly.entity.ViewModel.EstablishmentViewModel
import com.example.roamly.entity.ViewModel.UserViewModel
import com.example.roamly.ui.screens.sealed.EstablishmentScreens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserEstablishmentsScreen(
    navController: NavController,
    userViewModel: UserViewModel = hiltViewModel(), // Используем hiltViewModel по умолчанию, если не передан
    // Предполагается, что EstablishmentViewModel поддерживает StateFlow для списка
    viewModel: EstablishmentViewModel = hiltViewModel()
) {
    // ⭐ ИСПРАВЛЕНО: Собираем StateFlow<User> в Compose-состояние
    val user by userViewModel.user.collectAsState()

    // ВАЖНО: Предполагается, что EstablishmentViewModel имеет поля:
    val establishments by viewModel.userEstablishments.collectAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.collectAsState(initial = false)
    val errorMessage by viewModel.errorMessage.collectAsState(initial = null)

    // ⭐ ИСПРАВЛЕНО: Получаем ID пользователя из собранного состояния
    val userId = user.id

    // Запуск загрузки данных при входе на экран
    LaunchedEffect(userId) {
        userId?.let {
            // Вызываем функцию ViewModel для загрузки заведений пользователя
            viewModel.fetchEstablishmentsByUserId(it)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        when {
            isLoading -> {
                // Индикатор загрузки
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            errorMessage != null -> {
                // Сообщение об ошибке
                Text(
                    text = "Ошибка загрузки: $errorMessage",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
                // Кнопка для повторной попытки
                Button(
                    onClick = { userId?.let { viewModel.fetchEstablishmentsByUserId(it) } },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp)
                ) {
                    Text("Повторить")
                }
            }
            establishments.isEmpty() && !isLoading -> { // Добавлена проверка !isLoading для избежания мерцания
                // Пустой список
                Text(
                    text = "У вас пока нет созданных заведений.",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            else -> {
                // Отображение списка
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(establishments) { establishment ->
                        // ⭐ ПЕРЕДАЕМ ССЫЛКУ НА VM В EstablishmentItem
                        EstablishmentItem(
                            establishment = establishment,
                            viewModel = viewModel,
                            navController = navController
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EstablishmentItem(
    establishment: EstablishmentDisplayDto,
    viewModel: EstablishmentViewModel,
    navController: NavController
) {
    val showResubmitButton = establishment.status == EstablishmentStatus.REJECTED

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                navController.navigate(
                    EstablishmentScreens.EstablishmentDetail.createRoute(establishment.id)
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = establishment.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = establishment.address,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Статус: ${formatStatus(establishment.status)}",
                        color = getStatusColor(establishment.status),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Создано: ${establishment.dateOfCreation}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Text(
                    text = "Рейтинг: ${String.format("%.1f", establishment.rating)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // ⭐ НОВЫЙ БЛОК: Кнопка повторной отправки
            if (showResubmitButton) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        // Здесь мы вызываем повторную отправку
                        viewModel.resubmitEstablishmentForReview(establishment.id)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Отправить на повторное рассмотрение")
                }
            }
        }
    }
}
/**
 * Вспомогательная функция для форматирования статуса (для UI).
 */
@Composable
private fun formatStatus(status: EstablishmentStatus): String {
    return when (status) {
        EstablishmentStatus.PENDING_APPROVAL -> "Ожидает одобрения"
        EstablishmentStatus.ACTIVE -> "Одобрено"
        EstablishmentStatus.REJECTED -> "Отклонено"
        else -> "Неизвестный"
    }
}

/**
 * Вспомогательная функция для определения цвета статуса.
 */
@Composable
private fun getStatusColor(status: EstablishmentStatus): Color {
    return when (status) {
        EstablishmentStatus.PENDING_APPROVAL -> MaterialTheme.colorScheme.tertiary
        EstablishmentStatus.ACTIVE -> MaterialTheme.colorScheme.primary
        EstablishmentStatus.REJECTED -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.error
    }
}