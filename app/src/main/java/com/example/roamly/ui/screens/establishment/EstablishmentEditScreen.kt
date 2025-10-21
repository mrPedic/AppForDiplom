@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.roamly.ui.screens.establishment

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.entity.*

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EstablishmentEditScreen(
    navController: NavController,
    establishmentId: Long,
    viewModel: EstablishmentViewModel = hiltViewModel()
) {
    val establishment by viewModel.currentEstablishment.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Состояния для полей ввода
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TypeOfEstablishment.Restaurant) }

    // ⭐ Загружаем данные при входе, если их нет
    LaunchedEffect(establishmentId) {
        if (establishment?.id != establishmentId) {
            viewModel.fetchEstablishmentById(establishmentId)
        }
    }

    // ⭐ Инициализация полей ввода после загрузки данных
    LaunchedEffect(establishment) {
        establishment?.let {
            name = it.name
            description = it.description
            address = it.address
            type = it.type
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Редактирование: ${name}") }) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                errorMessage != null -> Text(
                    text = "Ошибка: $errorMessage",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                establishment != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Поле "Название"
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Название") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Поле "Описание"
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Описание") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )

                        // Поле "Адрес"
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Адрес") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // TODO: Добавить DropdownMenu для выбора TypeOfEstablishment
                        Text("Тип заведения: ${convertTypeToWord(type)}")

                        // TODO: Добавить редактирование координат (сложно, лучше отдельный экран с картой)
                        // Text("Координаты: ${establishment.latitude}, ${establishment.longitude}")

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                // Вызов метода обновления в ViewModel
                                viewModel.updateEstablishment(
                                    establishmentId = establishmentId,
                                    name = name,
                                    description = description,
                                    address = address,
                                    latitude = establishment!!.latitude, // Используем старые координаты
                                    longitude = establishment!!.longitude,
                                    type = type,
                                    onResult = { success ->
                                        if (success) {
                                            navController.popBackStack() // Вернуться на экран деталей
                                        } else {
                                            // Ошибка уже установлена в ViewModel, можно показать Snackbar
                                        }
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading && establishment != null
                        ) {
                            Text("Сохранить изменения")
                        }
                    }
                }
            }
        }
    }
}