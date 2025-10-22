package com.example.roamly.ui.screens.establishment

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.entity.EstablishmentViewModel
import com.example.roamly.entity.TypeOfEstablishment
import com.example.roamly.entity.convertTypeToWord
import com.example.roamly.ui.screens.sealed.EstablishmentScreens

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstablishmentEditScreen(
    navController: NavController,
    establishmentId: Long,
    viewModel: EstablishmentViewModel = hiltViewModel()
) {
    // Получение данных из ViewModel
    val establishment by viewModel.currentEstablishment.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Состояния для полей ввода
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TypeOfEstablishment.Restaurant) }
    var currentLatitude by remember { mutableStateOf(0.0) }
    var currentLongitude by remember { mutableStateOf(0.0) }
    var isDataLoaded by remember { mutableStateOf(false) }

    // ⭐ НОВЫЕ СОСТОЯНИЯ: Режим редактирования для каждого поля
    var isNameEditing by remember { mutableStateOf(false) }
    var isDescriptionEditing by remember { mutableStateOf(false) }
    var isAddressEditing by remember { mutableStateOf(false) }

    val currentBackStackEntry = remember { navController.currentBackStackEntry }

    // 1. Загрузка данных
    LaunchedEffect(establishmentId) {
        viewModel.fetchEstablishmentById(establishmentId)
    }

    // 2. Инициализация полей после загрузки данных
    LaunchedEffect(establishment) {
        establishment?.let {
            if (!isDataLoaded) {
                name = it.name
                description = it.description
                address = it.address
                type = it.type
                currentLatitude = it.latitude
                currentLongitude = it.longitude
                isDataLoaded = true
            }
        }
    }

    // ⭐ 3. Получение результата с MapPickerScreen
    LaunchedEffect(currentBackStackEntry) {
        val savedStateHandle = currentBackStackEntry?.savedStateHandle
        val newLat = savedStateHandle?.get<Double>(LATITUDE_KEY)
        val newLon = savedStateHandle?.get<Double>(LONGITUDE_KEY)

        if (newLat != null && newLon != null) {
            currentLatitude = newLat
            currentLongitude = newLon
            // Очищаем, чтобы избежать повторного срабатывания
            savedStateHandle.remove<Double>(LATITUDE_KEY)
            savedStateHandle.remove<Double>(LONGITUDE_KEY)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Редактирование: ${name.ifEmpty { "Заведение" }}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading || !isDataLoaded -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                errorMessage != null -> Text(text = "Ошибка: $errorMessage", color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // ----------------------------------------------------
                        // ⭐ РЕДАКТИРОВАНИЕ НАЗВАНИЯ
                        // ----------------------------------------------------
                        EditableField(
                            label = "Название",
                            value = name,
                            isEditing = isNameEditing,
                            onValueChange = { name = it },
                            onEditToggle = { isNameEditing = it }
                        )

                        // ----------------------------------------------------
                        // ⭐ РЕДАКТИРОВАНИЕ ОПИСАНИЯ
                        // ----------------------------------------------------
                        EditableField(
                            label = "Описание",
                            value = description,
                            isEditing = isDescriptionEditing,
                            onValueChange = { description = it },
                            onEditToggle = { isDescriptionEditing = it },
                            singleLine = false,
                            minLines = 3
                        )

                        // ----------------------------------------------------
                        // ⭐ РЕДАКТИРОВАНИЕ АДРЕСА
                        // ----------------------------------------------------
                        EditableField(
                            label = "Адрес",
                            value = address,
                            isEditing = isAddressEditing,
                            onValueChange = { address = it },
                            onEditToggle = { isAddressEditing = it }
                        )

                        // ----------------------------------------------------
                        // ⭐ БЛОК РЕДАКТИРОВАНИЯ МЕСТОПОЛОЖЕНИЯ (ОСТАВЛЕН БЕЗ ИЗМЕНЕНИЙ)
                        // ----------------------------------------------------
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Местоположение:", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = "Lat: ${String.format("%.4f", currentLatitude)}, Lon: ${String.format("%.4f", currentLongitude)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(onClick = {
                                // Навигация на MapPickerScreen
                                navController.navigate(EstablishmentScreens.MapPicker.route)
                            }) {
                                Icon(Icons.Filled.LocationOn, contentDescription = "Выбрать на карте", modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Изменить")
                            }
                        }

                        // ----------------------------------------------------
                        // ⭐ Выбор типа заведения (оставлен Dropdown)
                        // ----------------------------------------------------
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                readOnly = true,
                                value = convertTypeToWord(type),
                                onValueChange = { },
                                label = { Text("Тип заведения") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                TypeOfEstablishment.entries.forEach { selectionOption ->
                                    DropdownMenuItem(
                                        text = { Text(convertTypeToWord(selectionOption)) },
                                        onClick = {
                                            type = selectionOption
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                viewModel.updateEstablishment(
                                    establishmentId = establishmentId,
                                    name = name,
                                    description = description,
                                    address = address,
                                    latitude = currentLatitude,
                                    longitude = currentLongitude,
                                    type = type,
                                    onResult = { success ->
                                        if (success) { navController.popBackStack() }
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Done, contentDescription = "Сохранить", modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Сохранить все изменения")
                        }
                    }
                }
            }
        }
    }
}

// ⭐ НОВЫЙ КОМПОНЕНТ: Для переключения между просмотром и редактированием
@Composable
fun EditableField(
    label: String,
    value: String,
    isEditing: Boolean,
    onValueChange: (String) -> Unit,
    onEditToggle: (Boolean) -> Unit,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (isEditing) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label) },
                modifier = Modifier.weight(1f),
                singleLine = singleLine,
                minLines = minLines
            )
            IconButton(onClick = { onEditToggle(false) }) {
                Icon(Icons.Filled.Done, contentDescription = "Сохранить поле")
            }
        } else {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = value.ifEmpty { "Не указано" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Normal
                )
            }
            IconButton(onClick = { onEditToggle(true) }) {
                Icon(Icons.Filled.Create, contentDescription = "Редактировать поле")
            }
        }
    }
    // Добавление разделителя для лучшей визуальной структуры
    if (!isEditing) {
        Divider(Modifier.fillMaxWidth().padding(top = 8.dp))
    }
}
