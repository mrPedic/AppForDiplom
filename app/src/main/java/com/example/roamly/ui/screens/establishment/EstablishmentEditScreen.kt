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

    // Collect currentEstablishment для отслеживания загрузки
    val currentEstablishment by viewModel.currentEstablishment.collectAsState()

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
        newPhotoUris += uris // Для отображения
    }

    // Загрузка данных (только fetch, init - ниже)
    LaunchedEffect(establishmentId) {
        viewModel.fetchEstablishmentById(establishmentId)
    }

    // Инициализация edited состояний ПОСЛЕ успешной загрузки currentEstablishment
    LaunchedEffect(currentEstablishment) {
        if (currentEstablishment != null && editedName.isEmpty()) { // Проверка, чтобы init только раз
            viewModel.initEditedStates()
        }
    }

    // Обработка возврата с MapPicker
    val navBackStackEntry = remember { navController.currentBackStackEntry }
    LaunchedEffect(navBackStackEntry) {
        val savedStateHandle = navBackStackEntry?.savedStateHandle
        val newLat = savedStateHandle?.get<Double>(LATITUDE_KEY)
        val newLon = savedStateHandle?.get<Double>(LONGITUDE_KEY)
        if (newLat != null && newLon != null) {
            Log.i("EditScreenMap", "Получены новые координаты: Lat: $newLat, Lon: $newLon")
            viewModel.updateEditedLatitude(newLat)
            viewModel.updateEditedLongitude(newLon)
            savedStateHandle.remove<Double>(LATITUDE_KEY)
            savedStateHandle.remove<Double>(LONGITUDE_KEY)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Редактирование", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Box(Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else if (errorMessage != null) {
                Text("Ошибка: $errorMessage", color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Spacer(Modifier.height(8.dp))

                    // СЕКЦИЯ ФОТОГРАФИЙ (используем editedPhotoBase64s + newPhotoUris для отображения)
                    Column(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Фотографии", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${editedPhotoBase64s.size} шт.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(8.dp))
                        LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                            item {
                                Box(
                                    Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { photoPickerLauncher.launch("image/*") }
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, "Добавить фото", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            items(editedPhotoBase64s) { base64 ->
                                PhotoItem(model = decodeBase64ToBytes(base64)) { viewModel.updateEditedPhotos(editedPhotoBase64s - base64) }
                            }
                            items(newPhotoUris) { uri ->
                                PhotoItem(model = uri) {
                                    newPhotoUris = newPhotoUris - uri
                                    viewModel.updateEditedPhotos(editedPhotoBase64s - uriToBase64(context, uri)!!)
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // ТЕКСТОВЫЕ ПОЛЯ (используем ViewModel)
                    EditableField("Название", editedName, isNameEditing, { viewModel.updateEditedName(it) }, { isNameEditing = it })
                    EditableField("Описание", editedDescription, isDescriptionEditing, { viewModel.updateEditedDescription(it) }, { isDescriptionEditing = it }, singleLine = false, minLines = 3)
                    EditableField("Адрес", editedAddress, isAddressEditing, { viewModel.updateEditedAddress(it) }, { isAddressEditing = it })

                    // МЕСТОПОЛОЖЕНИЕ (из ViewModel)
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text("Местоположение", style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(4.dp))
                                Text("${String.format("%.4f", editedLatitude)}, ${String.format("%.4f", editedLongitude)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { navController.navigate(EstablishmentScreens.MapPicker.route) }) {
                                Icon(Icons.Filled.LocationOn, "Изменить на карте", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    // ТИП ЗАВЕДЕНИЯ
                    var expanded by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        readOnly = true,
                        value = convertTypeToWord(editedType),
                        onValueChange = {},
                        label = { Text("Тип заведения") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
                        TypeOfEstablishment.entries.forEach { option ->
                            DropdownMenuItem(text = { Text(convertTypeToWord(option)) }, onClick = {
                                viewModel.updateEditedType(option)
                                expanded = false
                            })
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    ScheduleEditBlock(editedOperatingHours) { viewModel.updateEditedOperatingHours(it) }

                    // КНОПКА СОХРАНЕНИЯ
                    Button(onClick = {
                        viewModel.updateEstablishment(establishmentId) { success ->
                            if (success) navController.popBackStack()
                        }
                    }, Modifier.fillMaxWidth().height(50.dp)) {
                        Icon(Icons.Filled.Done, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Сохранить изменения")
                    }

                    Spacer(Modifier.height(32.dp))
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