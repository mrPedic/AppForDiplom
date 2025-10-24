@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.roamly.ui.screens.establishment

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.entity.EstablishmentStatus
import com.example.roamly.entity.EstablishmentViewModel
import com.example.roamly.entity.TypeOfEstablishment // Предполагаем, что этот импорт теперь доступен
import com.example.roamly.entity.UserViewModel
import com.example.roamly.entity.convertTypeToWord
import com.example.roamly.ui.screens.sealed.EstablishmentScreens
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CreateEstablishmentScreen(
    navController: NavController,
    userViewModel: UserViewModel,
    viewModel: EstablishmentViewModel = hiltViewModel()
) {
    var selectedType by rememberSaveable { mutableStateOf<TypeOfEstablishment?>(null) }
    var photoUris by rememberSaveable { mutableStateOf<List<Uri>>(emptyList()) }

    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }

    // Состояние для координат
    var latitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var longitude by rememberSaveable { mutableStateOf<Double?>(null) }

    // Состояние статуса (на старте всегда PENDING_APPROVAL)
    val status = EstablishmentStatus.PENDING_APPROVAL
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // ⭐ ОБРАБОТКА РЕЗУЛЬТАТА ИЗ MAP PICKER
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    LaunchedEffect(Unit) { // Запускаем только один раз
        // Проверяем наличие ключей
        val newLat = savedStateHandle?.get<Double>(LATITUDE_KEY)
        val newLon = savedStateHandle?.get<Double>(LONGITUDE_KEY)

        if (newLat != null && newLon != null) {
            latitude = newLat
            longitude = newLon
            // Очищаем savedStateHandle
            savedStateHandle.remove<Double>(LATITUDE_KEY)
            savedStateHandle.remove<Double>(LONGITUDE_KEY)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // 1. Поле для Названия
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Название заведения") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        // 2. Поле для Адреса
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Адрес (Улица, дом и т.д.)") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        // ⭐ 3. Выбор типа заведения
        EstablishmentTypeDropdown(
            selectedType = selectedType,
            onTypeSelected = { selectedType = it },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        // 4. Поле для Описания
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Описание") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(bottom = 16.dp)
        )

        // ⭐ 5. Компонент выбора местоположения / Мини-карта
        LocationPickerCard(
            latitude = latitude,
            longitude = longitude,
            onClick = {
                // Предполагаем, что MapPicker.route ждет ключи
                navController.navigate(EstablishmentScreens.MapPicker.route)
            },
            onClearClick = { // ⭐ ОБРАБОТЧИК ДЛЯ СБРОСА
                latitude = null
                longitude = null
            }
        )

        HorizontalDivider(Modifier.padding(vertical = 16.dp))

        // ⭐ 6. Выбор и обрезка фотографий
        PhotoPickerAndCropper(
            photoUris = photoUris,
            onUrisChange = { photoUris = it },
            navController = navController
        )

        HorizontalDivider(Modifier.padding(vertical = 16.dp))

        // 6. Отображение статуса
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "Статус Заведения:",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    // Отображаем статус, который будет PENDING_APPROVAL
                    text = "На рассмотрении администрации (Текущий статус: ${status.name})",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // 7. Кнопка создания
        Button(
            onClick = {
                isLoading = true
                errorMessage = null

                val currentUserId = userViewModel.getId()

                if (currentUserId == null) {
                    isLoading = false
                    errorMessage = "Ошибка: Пользователь не авторизован или ID не доступен."
                    return@Button
                }
                if (latitude == null || longitude == null) {
                    isLoading = false
                    errorMessage = "Ошибка: Пожалуйста, укажите местоположение на карте."
                    return@Button
                }

                // ⭐ ПРОВЕРКА ВЫБРАННОГО ТИПА
                if (selectedType == null) {
                    isLoading = false
                    errorMessage = "Ошибка: Пожалуйста, выберите тип заведения."
                    return@Button
                }

                Log.e("CreateEstablishment", "userId = ${userViewModel.getId()}")

                // Передаем ID пользователя, координаты И ТИП в ViewModel
                val base64List = try {
                    photoUris.mapNotNull { uri -> convertUriToBase64(navController.context, uri) }
                } catch (e: Exception) {
                    isLoading = false
                    errorMessage = "Ошибка обработки фото: ${e.message}"
                    Log.e("CreateEstablishment", "Photo conversion error", e)
                    return@Button
                }

                viewModel.createEstablishment(
                    name = name,
                    description = description,
                    address = address,
                    latitude = latitude!!,
                    longitude = longitude!!,
                    createUserId = currentUserId,
                    type = selectedType!!,
                    // ⭐ ПЕРЕДАЕМ СПИСОК BASE64
                    photoBase64s = base64List
                ) { isSuccess ->
                    isLoading = false
                    if (isSuccess) {
                        navController.popBackStack()
                    } else {
                        errorMessage = "Не удалось создать заведение. Повторите попытку."
                    }
                }
            },
            enabled = name.isNotBlank() && address.isNotBlank() && latitude != null && longitude != null && selectedType != null && !isLoading,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text("Отправить на рассмотрение")
            }
        }

        // 8. Отображение сообщения об ошибке
        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

// --------------------------------------------------------------------------
// НОВЫЙ КОМПОНЕНТ: ВЫПАДАЮЩИЙ СПИСОК ТИПОВ
// --------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstablishmentTypeDropdown(
    selectedType: TypeOfEstablishment?,
    onTypeSelected: (TypeOfEstablishment) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = remember { TypeOfEstablishment.entries.toList() }
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            // ⭐ Отображаем название на русском, используя convertTypeToWord
            value = selectedType?.let { convertTypeToWord(it) } ?: "Выберите тип заведения",
            onValueChange = {},
            readOnly = true,
            label = { Text("Тип заведения") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    // ⭐ Отображаем название на русском
                    text = { Text(convertTypeToWord(selectionOption)) },
                    onClick = {
                        onTypeSelected(selectionOption)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}


/**
 * Вспомогательный компонент для выбора местоположения на карте.
 * Отображает либо кнопку выбора, либо мини-карту с выбранной точкой.
 */
@Composable
fun LocationPickerCard(
    latitude: Double?,
    longitude: Double?,
    onClick: () -> Unit,
    onClearClick: () -> Unit // ⭐ ДОБАВЛЕН КОЛБЭК ДЛЯ СБРОСА
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (latitude == null) 100.dp else 250.dp) // Увеличим высоту карты, если выбрана
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (latitude == null) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        if (latitude != null && longitude != null) {
            Box(Modifier.fillMaxSize()) {
                // 1. Мини-карта
                MiniMapView(latitude, longitude)

                // 2. Кнопка сброса (отображается поверх карты)
                Row(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Координаты установлены (${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onClearClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Сбросить местоположение",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // 3. Слой для перехода (чтобы нажать можно было в любом месте, кроме кнопки сброса)
                Spacer(
                    modifier = Modifier.matchParentSize().clickable(onClick = onClick)
                )
            }
        } else {
            // Если координаты не выбраны
            Box(
                modifier = Modifier.fillMaxSize().clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Нажмите, чтобы выбрать местоположение на карте",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

/**
 * Компонент мини-карты с маркером (на основе OSMdroid).
 */
@Composable
fun MiniMapView(latitude: Double, longitude: Double) {
    val context = LocalContext.current
    val point = GeoPoint(latitude, longitude)

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            // Настройка osmdroid: MUST BE CALLED ONCE
            Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))

            MapView(ctx).apply {

                this.minZoomLevel = 5.0

                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(false) // Отключаем мультитач для мини-карты
                controller.setZoom(14.0)
                controller.setCenter(point)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                // Добавляем маркер
                val marker = Marker(this).apply {
                    position = point
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                overlays.add(marker)
                invalidate()
            }
        },
        update = { view ->
            // Обновление центра карты и маркера при изменении координат
            view.controller.setCenter(point)
            view.overlays.removeAll { it is Marker }
            val marker = Marker(view).apply {
                position = point
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            view.overlays.add(marker)
            view.invalidate()
        }
    )
}
