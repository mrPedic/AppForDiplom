@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.roamly.ui.screens.establishment

import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
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
import com.example.roamly.entity.EstablishmentLoadState
import com.example.roamly.entity.TypeOfEstablishment
import com.example.roamly.entity.ViewModel.EstablishmentViewModel
import com.example.roamly.entity.convertTypeToWord
import com.example.roamly.ui.screens.sealed.EstablishmentScreens
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EstablishmentEditScreen(
    navController: NavController,
    establishmentId: Long,
    viewModel: EstablishmentViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // Состояния из ViewModel (collectAsState)
    val editedName by viewModel.editedName.collectAsState()
    val editedDescription by viewModel.editedDescription.collectAsState()
    val editedAddress by viewModel.editedAddress.collectAsState()
    val editedType by viewModel.editedType.collectAsState()
    val editedLatitude by viewModel.editedLatitude.collectAsState()
    val editedLongitude by viewModel.editedLongitude.collectAsState()
    val editedPhotoBase64s by viewModel.editedPhotoBase64s.collectAsState()
    val editedOperatingHours by viewModel.editedOperatingHours.collectAsState()

    // Collect establishmentDetailState вместо currentEstablishment
    val establishmentState by viewModel.establishmentDetailState.collectAsState()

    // Локальные состояния (только для UI, не edited data)
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var isNameEditing by remember { mutableStateOf(false) }
    var isDescriptionEditing by remember { mutableStateOf(false) }
    var isAddressEditing by remember { mutableStateOf(false) }

    // Для новых фото (Uri, потом конвертируем в Base64 и добавляем в ViewModel)
    var newPhotoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // Лаунчер для фото
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val newBase64s = uris.mapNotNull { uriToBase64(context, it) }
        viewModel.updateEditedPhotos(editedPhotoBase64s + newBase64s)
        newPhotoUris += uris // Для отображения, если нужно, но поскольку используем Base64, опционально
    }

    // Загрузка данных (только fetch, init - ниже)
    LaunchedEffect(establishmentId) {
        viewModel.fetchEstablishmentDetails(establishmentId)
    }

    // Инициализация edited состояний ПОСЛЕ успешной загрузки establishmentState
    LaunchedEffect(establishmentState) {
        if (establishmentState is EstablishmentLoadState.Success && editedName.isEmpty()) { // Проверка, чтобы init только раз
            viewModel.initEditedStates()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Редактировать заведение") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Назад")
                    }
                },
                actions = {
                    Button(onClick = { viewModel.saveChanges(establishmentId, navController) }) {
                        Text("Сохранить")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = establishmentState) {
                is EstablishmentLoadState.Idle -> {}
                is EstablishmentLoadState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is EstablishmentLoadState.Error -> Text("Ошибка: ${state.message}", color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                is EstablishmentLoadState.Success -> {
                    val establishment = state.data  // Теперь данные из state.data

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // Название
                        EditableField(
                            label = "Название",
                            value = editedName,
                            onValueChange = { viewModel.updateEditedName(it) },
                            isEditing = isNameEditing,
                            onEditToggle = { isNameEditing = it },
                            singleLine = true
                        )

                        Spacer(Modifier.height(16.dp))

                        // Описание
                        EditableField(
                            label = "Описание",
                            value = editedDescription,
                            onValueChange = { viewModel.updateEditedDescription(it) },
                            isEditing = isDescriptionEditing,
                            onEditToggle = { isDescriptionEditing = it },
                            singleLine = false,
                            minLines = 3
                        )

                        Spacer(Modifier.height(16.dp))

                        // Адрес
                        EditableField(
                            label = "Адрес",
                            value = editedAddress,
                            onValueChange = { viewModel.updateEditedAddress(it) },
                            isEditing = isAddressEditing,
                            onEditToggle = { isAddressEditing = it },
                            singleLine = true
                        )

                        Spacer(Modifier.height(16.dp))

                        // Тип заведения
                        Text("Тип заведения: ${convertTypeToWord(editedType)}")
                        // Можно добавить Dropdown для изменения типа, если нужно

                        Spacer(Modifier.height(16.dp))

                        // Координаты
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Широта: $editedLatitude, Долгота: $editedLongitude")
                            Spacer(Modifier.width(16.dp))
                            Button(onClick = {
                                // Навигатор к выбору на карте
                            }) {
                                Text("Выбрать на карте")
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Фото
                        Text("Фото:")
                        LazyRow(
                            modifier = Modifier.height(120.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(editedPhotoBase64s) { photo ->
                                Box {
                                    val bytes = decodeBase64ToBytes(photo)
                                    if (bytes != null) {
                                        Image(
                                            painter = rememberAsyncImagePainter(bytes),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(120.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.removePhoto(editedPhotoBase64s.indexOf(photo)) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(24.dp)
                                            .background(Color.Black.copy(0.5f), CircleShape)
                                    ) {
                                        Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            item {
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .background(Color.LightGray)
                                        .clickable { photoPickerLauncher.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Add, null, tint = Color.Gray)
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Расписание
                        ScheduleEditBlock(
                            operatingHours = editedOperatingHours,
                            onHoursChange = { viewModel.updateEditedOperatingHours(it) }
                        )
                    }
                }
            }
        }
    }
}


// EditableField
@Composable
fun EditableField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isEditing: Boolean,
    onEditToggle: (Boolean) -> Unit,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )

        if (isEditing) {
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
        } else {
            IconButton(
                onClick = { onEditToggle(true) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Filled.Create,
                    contentDescription = "Редактировать",
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

// Функция декодирования Base64 в байты для отображения
fun decodeBase64ToBytes(base64String: String): ByteArray? {
    return try {
        val cleanBase64 = base64String.substringAfter(",", base64String)
        Base64.decode(cleanBase64, Base64.DEFAULT)
    } catch (e: Exception) {
        null
    }
}

fun Map<String, String>.toJsonString(): String? {
    return if (this.isEmpty()) null else Gson().toJson(this)
}

// Конвертация JSON String в Map (для инициализации в Compose)
fun String?.toMap(): Map<String, String> {
    return try {
        if (this.isNullOrBlank() || this.trim() == "null") {
            emptyMap()
        } else {
            val type = object : TypeToken<Map<String, String>>() {}.type
            Gson().fromJson<Map<String, String>>(this, type).filterValues { it.isNotBlank() }
        }
    } catch (e: Exception) {
        Log.e("ScheduleHelper", "Ошибка парсинга расписания: $this", e)
        emptyMap()
    }
}

@Composable
fun ScheduleEditBlock(
    operatingHours: Map<String, String>,
    onHoursChange: (Map<String, String>) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Часы работы (редактирование)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            DAYS_OF_WEEK.forEach { day ->
                val currentHours = operatingHours[day] ?: "Закрыто"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = day,
                        modifier = Modifier.weight(0.35f)
                    )

                    OutlinedTextField(
                        value = currentHours,
                        onValueChange = { newValue ->
                            onHoursChange(operatingHours.toMutableMap().apply {
                                this[day] = newValue.trim().ifEmpty { "Закрыто" }
                            })
                        },
                        placeholder = { Text("08:00 - 18:00 / Закрыто") },
                        modifier = Modifier.weight(0.65f),
                        singleLine = true,
                    )
                }
            }
        }
    }
}