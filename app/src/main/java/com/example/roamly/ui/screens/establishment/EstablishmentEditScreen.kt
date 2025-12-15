@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.roamly.ui.screens.establishment

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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.example.roamly.entity.classes.TypeOfEstablishment
import com.example.roamly.entity.ViewModel.EstablishmentEditViewModel
import com.example.roamly.entity.classes.convertTypeToWord
import com.example.roamly.ui.screens.sealed.EstablishmentScreens
import com.example.roamly.ui.theme.AppTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EstablishmentEditScreen(
    navController: NavController,
    establishmentId: Long,
    viewModel: EstablishmentEditViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // Инициализация OSMDroid
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, androidx.preference.PreferenceManager.getDefaultSharedPreferences(context))
    }

    // Состояния из ViewModel
    val editedName by viewModel.editedName.collectAsState()
    val editedDescription by viewModel.editedDescription.collectAsState()
    val editedAddress by viewModel.editedAddress.collectAsState()
    val editedType by viewModel.editedType.collectAsState()
    val editedLatitude by viewModel.editedLatitude.collectAsState()
    val editedLongitude by viewModel.editedLongitude.collectAsState()
    val editedPhotos by viewModel.editedPhotos.collectAsState()
    val editedOperatingHours by viewModel.editedOperatingHours.collectAsState()

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isInitialized by viewModel.isInitialized.collectAsState()

    // Локальные состояния редактирования
    var isNameEditing by remember { mutableStateOf(false) }
    var isDescriptionEditing by remember { mutableStateOf(false) }
    var isAddressEditing by remember { mutableStateOf(false) }

    // Лаунчер выбора фото
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.takeIf { it.isNotEmpty() }?.let { viewModel.addPhotos(it) }
    }

    // Загрузка данных (один раз)
    LaunchedEffect(Unit) {
        if (!isInitialized) {
            viewModel.fetchEstablishment(establishmentId)
        }
    }

    // Обработка возврата с экрана выбора карты
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.get<Double>("latitude")?.let { newLat ->
            viewModel.updateLatitude(newLat)
            savedStateHandle.remove<Double>("latitude")
        }
        savedStateHandle?.get<Double>("longitude")?.let { newLon ->
            viewModel.updateLongitude(newLon)
            savedStateHandle.remove<Double>("longitude")
        }
    }

    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier.background(AppTheme.colors.MainContainer),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Редактирование заведения",
                        color = AppTheme.colors.MainText
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.colors.SecondaryContainer,
                    navigationIconContentColor = AppTheme.colors.MainText,
                    titleContentColor = AppTheme.colors.MainText,
                    actionIconContentColor = AppTheme.colors.MainText
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Назад",
                            tint = AppTheme.colors.MainText
                        )
                    }
                },
                actions = {
                    if (!isLoading) {
                        IconButton(
                            onClick = {
                                viewModel.saveChanges(
                                    establishmentId = establishmentId,
                                    onSuccess = { navController.popBackStack() },
                                    onError = { /* Ошибка отобразится через errorMessage */ }
                                )
                            }
                        ) {
                            Icon(
                                Icons.Filled.Done,
                                contentDescription = "Сохранить",
                                tint = AppTheme.colors.MainText
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    color = AppTheme.colors.MainText
                )
            }
        } else if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Ошибка: $errorMessage",
                    color = AppTheme.colors.MainFailure,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .background(AppTheme.colors.MainContainer)
                    .padding(16.dp)
            ) {
                // Основная информация
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = AppTheme.colors.SecondaryContainer
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(AppTheme.colors.MainBorder)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        EditableTextField(
                            label = "Название",
                            value = editedName,
                            onValueChange = viewModel::updateName,
                            isEditing = isNameEditing,
                            onEditToggle = { isNameEditing = it }
                        )
                        Spacer(Modifier.height(16.dp))

                        EditableTextField(
                            label = "Описание",
                            value = editedDescription,
                            onValueChange = viewModel::updateDescription,
                            isEditing = isDescriptionEditing,
                            onEditToggle = { isDescriptionEditing = it },
                            singleLine = false,
                            minLines = 3
                        )
                        Spacer(Modifier.height(16.dp))

                        EditableTextField(
                            label = "Адрес",
                            value = editedAddress,
                            onValueChange = viewModel::updateAddress,
                            isEditing = isAddressEditing,
                            onEditToggle = { isAddressEditing = it }
                        )
                        Spacer(Modifier.height(8.dp))

                        // Карта
                        Text(
                            "Местоположение",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.MainText
                        )
                        Spacer(Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    1.dp,
                                    AppTheme.colors.SecondaryBorder,
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    MapView(ctx).apply {
                                        setTileSource(TileSourceFactory.MAPNIK)
                                        controller.setZoom(15.0)
                                        controller.setCenter(GeoPoint(editedLatitude, editedLongitude))
                                        overlays.add(Marker(this).apply {
                                            position = GeoPoint(editedLatitude, editedLongitude)
                                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                        })
                                        setMultiTouchControls(false)
                                        isClickable = false
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                                update = { mapView ->
                                    mapView.controller.setCenter(GeoPoint(editedLatitude, editedLongitude))
                                    mapView.overlays.filterIsInstance<Marker>().forEach {
                                        it.position = GeoPoint(editedLatitude, editedLongitude)
                                    }
                                    mapView.invalidate()
                                }
                            )

                            // Прозрачная кнопка поверх карты для перехода к выбору
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { navController.navigate(EstablishmentScreens.MapPicker.route) }
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { navController.navigate(EstablishmentScreens.MapPicker.route) }
                                .padding(4.dp)
                        ) {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = AppTheme.colors.MainSuccess
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Изменить на карте",
                                color = AppTheme.colors.MainSuccess
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // Тип заведения
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextField(
                                readOnly = true,
                                value = convertTypeToWord(editedType),
                                onValueChange = { },
                                label = {
                                    Text(
                                        "Тип заведения",
                                        color = AppTheme.colors.SecondaryText
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp  else Icons.Filled.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = AppTheme.colors.MainText
                                    )
                                },
                                colors = ExposedDropdownMenuDefaults.textFieldColors(
                                    focusedContainerColor = AppTheme.colors.MainContainer,
                                    unfocusedContainerColor = AppTheme.colors.MainContainer,
                                    disabledContainerColor = AppTheme.colors.MainContainer,
                                    focusedTextColor = AppTheme.colors.MainText,
                                    unfocusedTextColor = AppTheme.colors.MainText,
                                    focusedLabelColor = AppTheme.colors.SecondaryText,
                                    unfocusedLabelColor = AppTheme.colors.SecondaryText,
                                    focusedIndicatorColor = AppTheme.colors.MainBorder,
                                    unfocusedIndicatorColor = AppTheme.colors.SecondaryBorder,
                                    cursorColor = AppTheme.colors.MainText
                                ),
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(AppTheme.colors.MainContainer)
                            ) {
                                TypeOfEstablishment.entries.forEach { type ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                convertTypeToWord(type),
                                                color = AppTheme.colors.MainText
                                            )
                                        },
                                        onClick = {
                                            viewModel.updateType(type)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Фотографии
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = AppTheme.colors.SecondaryContainer
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(AppTheme.colors.MainBorder)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Фотографии",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.MainText
                        )
                        Spacer(Modifier.height(12.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.height(140.dp)
                        ) {
                            items(editedPhotos) { item ->
                                Box(
                                    modifier = Modifier
                                        .size(140.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AppTheme.colors.SecondaryBorder.copy(alpha = 0.3f))
                                ) {
                                    // Подготовка модели изображения
                                    val model = remember(item) {
                                        when (item) {
                                            is EstablishmentEditViewModel.PhotoItem.Local -> item.uri
                                            is EstablishmentEditViewModel.PhotoItem.Remote -> {
                                                try {
                                                    Base64.decode(item.base64, Base64.DEFAULT)
                                                } catch (e: Exception) {
                                                    null
                                                }
                                            }
                                        }
                                    }

                                    val painter = rememberAsyncImagePainter(model = model)

                                    // Отображение
                                    Image(
                                        painter = painter,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // Состояния
                                    when (painter.state) {
                                        is AsyncImagePainter.State.Loading -> {
                                            CircularProgressIndicator(
                                                modifier = Modifier.align(Alignment.Center),
                                                strokeWidth = 2.dp,
                                                color = AppTheme.colors.MainText
                                            )
                                        }
                                        is AsyncImagePainter.State.Error -> {
                                            Icon(
                                                Icons.Filled.Warning,
                                                contentDescription = "Ошибка",
                                                tint = AppTheme.colors.MainFailure,
                                                modifier = Modifier.align(Alignment.Center)
                                            )
                                        }
                                        else -> { /* Успех - ничего не рисуем поверх */ }
                                    }

                                    // Кнопка удаления
                                    IconButton(
                                        onClick = { viewModel.removePhoto(item) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(28.dp)
                                            .background(AppTheme.colors.MainContainer.copy(alpha = 0.8f), CircleShape)
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "Удалить",
                                            tint = AppTheme.colors.MainFailure,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Кнопка добавления
                            item {
                                Box(
                                    modifier = Modifier
                                        .size(140.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            2.dp,
                                            AppTheme.colors.MainSuccess.copy(alpha = 0.5f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { photoPickerLauncher.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = "Добавить фото",
                                        tint = AppTheme.colors.MainSuccess,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // График работы
                ScheduleEditBlock(
                    operatingHours = editedOperatingHours,
                    onHoursChange = viewModel::updateOperatingHours
                )
            }
        }
    }
}

@Composable
fun EditableTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isEditing: Boolean,
    onEditToggle: (Boolean) -> Unit,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                color = AppTheme.colors.MainText
            )
            IconButton(onClick = { onEditToggle(!isEditing) }) {
                Icon(
                    imageVector = if (isEditing) Icons.Filled.Done else Icons.Filled.Create,
                    contentDescription = null,
                    tint = AppTheme.colors.MainSuccess
                )
            }
        }

        if (isEditing) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = singleLine,
                minLines = minLines,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppTheme.colors.MainBorder,
                    unfocusedBorderColor = AppTheme.colors.SecondaryBorder,
                    focusedTextColor = AppTheme.colors.MainText,
                    unfocusedTextColor = AppTheme.colors.MainText,
                    focusedLabelColor = AppTheme.colors.SecondaryText,
                    unfocusedLabelColor = AppTheme.colors.SecondaryText,
                    cursorColor = AppTheme.colors.MainText
                )
            )
        } else {
            Text(
                text = value.ifEmpty { "Не указано" },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                color = if (value.isEmpty()) AppTheme.colors.SecondaryText else AppTheme.colors.MainText
            )
        }
    }
}

fun Map<String, String>.toJsonString(): String? {
    return if (this.isEmpty()) null else Gson().toJson(this)
}

fun String?.toMap(): Map<String, String> {
    return try {
        if (this.isNullOrBlank() || this.trim() == "null") {
            emptyMap()
        } else {
            val type = object : TypeToken<Map<String, String>>() {}.type
            Gson().fromJson<Map<String, String>>(this, type).filterValues { it.isNotBlank() }
        }
    } catch (e: Exception) {
        Log.e("ScheduleHelper", "Ошибка парсинга: $this", e)
        emptyMap()
    }
}

@Composable
fun ScheduleEditBlock(
    operatingHours: Map<String, String>,
    onHoursChange: (Map<String, String>) -> Unit
) {
    val days = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.SecondaryContainer
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(AppTheme.colors.MainBorder)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Часы работы",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.MainText
            )
            Spacer(Modifier.height(12.dp))

            days.forEach { day ->
                val hours = operatingHours[day] ?: "Закрыто"
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = day,
                        modifier = Modifier.weight(0.4f),
                        color = AppTheme.colors.MainText
                    )
                    OutlinedTextField(
                        value = hours,
                        onValueChange = { newHours ->
                            val map = operatingHours.toMutableMap()
                            map[day] = newHours.ifBlank { "Закрыто" }
                            onHoursChange(map)
                        },
                        modifier = Modifier.weight(0.6f),
                        singleLine = true,
                        placeholder = {
                            Text(
                                "09:00 - 22:00",
                                color = AppTheme.colors.SecondaryText
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppTheme.colors.MainBorder,
                            unfocusedBorderColor = AppTheme.colors.SecondaryBorder,
                            focusedTextColor = AppTheme.colors.MainText,
                            unfocusedTextColor = AppTheme.colors.MainText,
                            focusedLabelColor = AppTheme.colors.SecondaryText,
                            unfocusedLabelColor = AppTheme.colors.SecondaryText,
                            cursorColor = AppTheme.colors.MainText
                        )
                    )
                }
            }
        }
    }
}