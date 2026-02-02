// New file: NotificationsScreen.kt
package com.example.roamly.ui.screens.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.roamly.entity.ViewModel.AdminNotificationsViewModel
import com.example.roamly.ui.theme.AppTheme

@Composable
fun NotificationsScreen(
    viewModel: AdminNotificationsViewModel = hiltViewModel()
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("all_users") } // "all_users", "all_establishments", "specific_user", etc.
    var specificId by remember { mutableStateOf("") }
    val isSending by viewModel.isSending.collectAsState()
    val sendResult by viewModel.sendResult.collectAsState()

    Scaffold(
        containerColor = AppTheme.colors.MainContainer
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Глобальные уведомления",
                style = MaterialTheme.typography.headlineMedium,
                color = AppTheme.colors.MainText // Этот цвет корректен
            )

            // --- ИСПРАВЛЕНИЕ 1: Поле заголовка ---
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Заголовок уведомления", color = AppTheme.colors.SecondaryText) }, // Цвет лейбла
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppTheme.colors.MainBorder,
                    unfocusedBorderColor = AppTheme.colors.SecondaryBorder,
                    cursorColor = AppTheme.colors.MainSuccess,
                    // ДОБАВЛЕНЫ ЦВЕТА ТЕКСТА:
                    focusedTextColor = AppTheme.colors.MainText,
                    unfocusedTextColor = AppTheme.colors.MainText,
                    focusedContainerColor = AppTheme.colors.SecondaryContainer, // Опционально: фон
                    unfocusedContainerColor = AppTheme.colors.SecondaryContainer // Опционально: фон
                )
            )

            // --- ИСПРАВЛЕНИЕ 2: Поле сообщения ---
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Сообщение", color = AppTheme.colors.SecondaryText) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppTheme.colors.MainBorder,
                    unfocusedBorderColor = AppTheme.colors.SecondaryBorder,
                    cursorColor = AppTheme.colors.MainSuccess,
                    // ДОБАВЛЕНЫ ЦВЕТА ТЕКСТА:
                    focusedTextColor = AppTheme.colors.MainText,
                    unfocusedTextColor = AppTheme.colors.MainText,
                    focusedContainerColor = AppTheme.colors.SecondaryContainer,
                    unfocusedContainerColor = AppTheme.colors.SecondaryContainer
                )
            )

            // Выбор цели
            Column {
                Text("Целевая аудитория", color = AppTheme.colors.MainText)
                RadioButtonGroup(
                    options = listOf(
                        "Все пользователи" to "all_users",
                        "Все заведения" to "all_establishments",
                        "Конкретный пользователь" to "specific_user",
                        "Конкретное заведение" to "specific_establishment"
                    ),
                    selected = target,
                    onSelected = { target = it }
                )
            }

            if (target.startsWith("specific_")) {
                OutlinedTextField(
                    value = specificId,
                    onValueChange = { specificId = it },
                    label = { Text("ID получателя") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppTheme.colors.MainText,
                        unfocusedTextColor = AppTheme.colors.SecondaryText,
                        focusedBorderColor = AppTheme.colors.MainBorder,
                        unfocusedBorderColor = AppTheme.colors.SecondaryBorder,
                        cursorColor = AppTheme.colors.MainSuccess
                    )
                )
            }

            Button(
                onClick = {
                    viewModel.sendGlobalNotification(
                        title = title,
                        message = message,
                        target = target,
                        specificId = specificId.toLongOrNull()
                    )
                },
                enabled = !isSending && title.isNotBlank() && message.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.colors.MainSuccess,
                    contentColor = AppTheme.colors.MainText
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSending) {
                    CircularProgressIndicator(color = AppTheme.colors.MainText, modifier = Modifier.size(24.dp))
                } else {
                    Text("Отправить")
                }
            }

            sendResult?.let { result ->
                Text(
                    if (result.success) "Уведомление отправлено успешно!" else "Ошибка: ${result.error}",
                    color = if (result.success) AppTheme.colors.MainSuccess else AppTheme.colors.MainFailure
                )
            }
        }
    }
}

@Composable
private fun RadioButtonGroup(
    options: List<Pair<String, String>>,
    selected: String,
    onSelected: (String) -> Unit
) {
    LazyColumn {
        items(options) { (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelected(value) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected == value,
                    onClick = { onSelected(value) },
                    colors = RadioButtonDefaults.colors(selectedColor = AppTheme.colors.MainSuccess)
                )
                Spacer(Modifier.width(8.dp))
                Text(label, color = AppTheme.colors.MainText)
            }
        }
    }
}