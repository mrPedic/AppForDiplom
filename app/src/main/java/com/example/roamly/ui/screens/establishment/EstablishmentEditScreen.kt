@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.roamly.ui.screens.establishment

import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.roamly.entity.TypeOfEstablishment
import com.example.roamly.entity.ViewModel.EstablishmentViewModel
import com.example.roamly.entity.convertTypeToWord
import com.example.roamly.ui.screens.sealed.EstablishmentScreens

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EstablishmentEditScreen(
    navController: NavController,
    establishmentId: Long,
    viewModel: EstablishmentViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // Получение данных из ViewModel
    val establishment by viewModel.currentEstablishment.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Состояния полей
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TypeOfEstablishment.Restaurant) }
    var currentLatitude by remember { mutableStateOf(0.0) }
    var currentLongitude by remember { mutableStateOf(0.0) }

    // Состояния для фото
    // existingPhotos хранит Base64 строки, которые уже были на сервере
    var existingPhotos by remember { mutableStateOf<List<String>>(emptyList()) }
    // newPhotos хранит Uri новых выбранных изображений
    var newPhotos by remember { mutableStateOf<List<Uri>>(emptyList()) }

    var isDataLoaded by remember { mutableStateOf(false) }

    // Состояния режима редактирования текстовых полей
    var isNameEditing by remember { mutableStateOf(false) }
    var isDescriptionEditing by remember { mutableStateOf(false) }
    var isAddressEditing by remember { mutableStateOf(false) }

    // Лаунчер для выбора фото
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        newPhotos = newPhotos + uris
    }

    // Обработка возврата с карты
    val currentBackStackEntry = remember { navController.currentBackStackEntry }
    LaunchedEffect(currentBackStackEntry) {
        val savedStateHandle = currentBackStackEntry?.savedStateHandle
        val newLat = savedStateHandle?.get<Double>(LATITUDE_KEY)
        val newLon = savedStateHandle?.get<Double>(LONGITUDE_KEY)

        if (newLat != null && newLon != null) {
            currentLatitude = newLat
            currentLongitude = newLon
            savedStateHandle.remove<Double>(LATITUDE_KEY)
            savedStateHandle.remove<Double>(LONGITUDE_KEY)
        }
    }

    // Загрузка данных
    LaunchedEffect(establishmentId) {
        viewModel.fetchEstablishmentById(establishmentId)
    }

    LaunchedEffect(establishment) {
        establishment?.let {
            if (!isDataLoaded) {
                name = it.name
                description = it.description
                address = it.address
                type = it.type
                currentLatitude = it.latitude
                currentLongitude = it.longitude
                // Фильтруем пустые строки, если есть
                existingPhotos = it.photoBase64s.filter { p -> p.isNotBlank() }
                isDataLoaded = true
            }
        }
    }

    Scaffold(
        topBar = {
            // Более компактный TopAppBar
            TopAppBar(
                title = {
                    Text(
                        text = "Редактирование",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Назад"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface, // Светлый фон
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                windowInsets = WindowInsets.statusBars // Учитываем статус бар
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
                errorMessage != null -> Text(
                    text = "Ошибка: $errorMessage",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState()), // Скролл для всего экрана
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Spacer(Modifier.height(8.dp))

                        // --- СЕКЦИЯ ФОТОГРАФИЙ ---
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Фотографии",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${existingPhotos.size + newPhotos.size} шт.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(8.dp))

                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                // Кнопка добавления
                                item {
                                    Box(
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { photoPickerLauncher.launch("image/*") }
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.outlineVariant,
                                                RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Добавить фото",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Существующие фото (Base64)
                                items(existingPhotos) { base64 ->
                                    PhotoItem(
                                        model = remember(base64) { decodeBase64ToBytes(base64) },
                                        onDelete = { existingPhotos = existingPhotos - base64 }
                                    )
                                }

                                // Новые фото (Uri)
                                items(newPhotos) { uri ->
                                    PhotoItem(
                                        model = uri,
                                        onDelete = { newPhotos = newPhotos - uri }
                                    )
                                }
                            }
                        }

                        HorizontalDivider()

                        // --- ТЕКСТОВЫЕ ПОЛЯ ---

                        EditableField(
                            label = "Название",
                            value = name,
                            isEditing = isNameEditing,
                            onValueChange = { name = it },
                            onEditToggle = { isNameEditing = it }
                        )

                        EditableField(
                            label = "Описание",
                            value = description,
                            isEditing = isDescriptionEditing,
                            onValueChange = { description = it },
                            onEditToggle = { isDescriptionEditing = it },
                            singleLine = false,
                            minLines = 3
                        )

                        EditableField(
                            label = "Адрес",
                            value = address,
                            isEditing = isAddressEditing,
                            onValueChange = { address = it },
                            onEditToggle = { isAddressEditing = it }
                        )

                        // --- МЕСТОПОЛОЖЕНИЕ ---
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Местоположение", style = MaterialTheme.typography.labelMedium)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "${String.format("%.4f", currentLatitude)}, ${String.format("%.4f", currentLongitude)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                IconButton(onClick = {
                                    navController.navigate(EstablishmentScreens.MapPicker.route)
                                }) {
                                    Icon(
                                        Icons.Filled.LocationOn,
                                        contentDescription = "Изменить на карте",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // --- ТИП ЗАВЕДЕНИЯ ---
                        Box(modifier = Modifier.fillMaxWidth()) {
                            var expanded by remember { mutableStateOf(false) }
                            OutlinedTextField(
                                readOnly = true,
                                value = convertTypeToWord(type),
                                onValueChange = { },
                                label = { Text("Тип заведения") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expanded = true },
                                enabled = false, // Чтобы клик обрабатывался Box-ом или readOnly
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                            // Прозрачная кнопка поверх TextField для открытия меню
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { expanded = true }
                            )

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f) // Чуть уже экрана
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // --- КНОПКА СОХРАНЕНИЯ ---
                        Button(
                            onClick = {
                                // 1. Конвертируем новые Uri в Base64
                                val newBase64s = newPhotos.mapNotNull { uri ->
                                    uriToBase64(context, uri)
                                }
                                // 2. Объединяем списки
                                val finalPhotoList = existingPhotos + newBase64s

                                // 3. Отправляем в ViewModel
                                viewModel.updateEstablishment(
                                    establishmentId = establishmentId,
                                    name = name,
                                    description = description,
                                    address = address,
                                    latitude = currentLatitude,
                                    longitude = currentLongitude,
                                    type = type,
                                    photoBase64s = finalPhotoList, // <-- РАСКОММЕНТИРУЙТЕ, КОГДА ОБНОВИТЕ VIEWMODEL
                                    onResult = { success ->
                                        if (success) {
                                            navController.popBackStack()
                                        }
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(Icons.Filled.Done, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Сохранить изменения")
                        }

                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

// --- ВСПОМОГАТЕЛЬНЫЕ КОМПОНЕНТЫ И ФУНКЦИИ ---

@Composable
fun PhotoItem(
    model: Any?, // Может быть Uri или ByteArray
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = model),
            contentDescription = "Фото заведения",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Кнопка удаления
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(24.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                .clickable { onDelete() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Удалить",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

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
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            if (!isEditing) {
                IconButton(
                    onClick = { onEditToggle(true) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Create,
                        contentDescription = "Редактировать",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                IconButton(
                    onClick = { onEditToggle(false) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Done,
                        contentDescription = "Готово",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (isEditing) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = singleLine,
                minLines = minLines,
                shape = RoundedCornerShape(8.dp)
            )
        } else {
            Text(
                text = value.ifEmpty { "Не указано" },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// Функция декодирования Base64 в байты для отображения
fun decodeBase64ToBytes(base64String: String): ByteArray? {
    return try {
        val cleanBase64 = base64String.substringAfter(",", base64String)
        Base64.decode(cleanBase64, Base64.DEFAULT)
    } catch (e: Exception) {
        null
    }
}