@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.roamly.ui.screens.establishment

import android.content.Context
import android.os.Build
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.roamly.entity.EstablishmentStatus
import com.example.roamly.entity.EstablishmentViewModel
import com.example.roamly.entity.UserViewModel
import com.example.roamly.ui.screens.sealed.LogSinUpScreens
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

// Константы для получения координат из MapPickerScreen
// Эти константы должны быть объявлены в MapPickerScreen.kt или другом общем месте
// Дублируем их здесь для самодостаточности, если они не импортируются напрямую:
// const val LATITUDE_KEY = "latitude"
// const val LONGITUDE_KEY = "longitude"


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CreateEstablishmentScreen(
    navController: NavController,
    userViewModel: UserViewModel,
    viewModel: EstablishmentViewModel = hiltViewModel()
) {
    // Состояние полей ввода
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    // ⭐ НОВОЕ СОСТОЯНИЕ ДЛЯ КООРДИНАТ
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }

    // Состояние статуса (на старте всегда PENDING_APPROVAL)
    val status = EstablishmentStatus.PENDING_APPROVAL
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // ⭐ ОБРАБОТКА РЕЗУЛЬТАТА ИЗ MAP PICKER
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    LaunchedEffect(savedStateHandle?.get<Double>(LATITUDE_KEY), savedStateHandle?.get<Double>(LONGITUDE_KEY)) {
        val newLat = savedStateHandle?.get<Double>(LATITUDE_KEY)
        val newLon = savedStateHandle?.get<Double>(LONGITUDE_KEY)
        if (newLat != null && newLon != null) {
            latitude = newLat
            longitude = newLon
            // Очищаем savedStateHandle, чтобы не получить те же данные при следующей композиции
            savedStateHandle.remove<Double>(LATITUDE_KEY)
            savedStateHandle.remove<Double>(LONGITUDE_KEY)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Создать Новое Заведение") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()), // Добавим скроллинг
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

            // 3. Поле для Описания
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Описание") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(bottom = 16.dp)
            )

            // ⭐ 4. Компонент выбора местоположения / Мини-карта
            LocationPickerCard(
                latitude = latitude,
                longitude = longitude,
                onClick = {
                    // Предполагаем, что маршрут для MapPickerScreen - "map_picker_route"
                    navController.navigate(LogSinUpScreens.MapPicker.route)
                }
            )

            // 5. Отображение статуса
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

            // 6. Кнопка создания
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

                    // Передаем ID пользователя и координаты в ViewModel
                    viewModel.createEstablishment(
                        name = name,
                        description = description,
                        address = address,
                        latitude = latitude!!,
                        longitude = longitude!!,
                        createUserId = currentUserId
                    ) { isSuccess ->
                        isLoading = false
                        if (isSuccess) {
                            // Успех: навигация назад
                            navController.popBackStack()
                        } else {
                            // Ошибка: отображаем сообщение
                            errorMessage = "Не удалось создать заведение. Повторите попытку."
                        }
                    }
                },
                enabled = name.isNotBlank() && address.isNotBlank() && latitude != null && longitude != null && !isLoading,
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

            // 7. Отображение сообщения об ошибке
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
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
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (latitude == null) 100.dp else 200.dp)
            .padding(bottom = 16.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (latitude == null) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        if (latitude != null && longitude != null) {
            MiniMapView(latitude, longitude)
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
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
            // Обновление центра карты и маркера при изменении координат (хотя в мини-карте это редко нужно)
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
@Preview(showBackground = true)
fun CreateEstablishmentScreenPreview(){
    CreateEstablishmentScreen(
        navController = rememberNavController(),
        userViewModel = hiltViewModel(),
    )
}
