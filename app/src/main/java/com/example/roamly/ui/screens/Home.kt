package com.example.roamly.ui.screens

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.roamly.PointBuilder
import com.example.roamly.entity.TypeOfEstablishment
import com.example.roamly.entity.ViewModel.EstablishmentViewModel
import com.example.roamly.entity.convertTypeToWord
import com.example.roamly.ui.screens.establishment.base64ToByteArray
import com.example.roamly.ui.screens.sealed.EstablishmentScreens
import com.example.roamly.ui.screens.sealed.SealedButtonBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import kotlin.apply
@Composable
fun HomeScreen(
    navController: NavController,
    mapRefreshKey: Boolean,
    onMapRefresh: () -> Unit
) {
    val bottomBarHeightWithPadding = 85.dp
    val viewModel: EstablishmentViewModel = hiltViewModel()
    var mapState by remember { mutableStateOf<MapView?>(null) }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.initializeLocation()
    }
    mapState?.let { map ->
        LaunchedEffect(map) {
            viewModel.mapView = map  // Установи ссылку на mapView
        }
        val pointBuilder = remember(map) { PointBuilder(map) }  // Если PointBuilder нужен
        pointBuilder.BuildAllMarkers()  // Если маркеры строятся здесь
    }
    Box {
        OsmMapAndroidView(
            refreshTrigger = mapRefreshKey,
            modifier = Modifier.fillMaxSize(),
            onMapCreated = { map -> mapState = map }
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 12.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Кнопка обновления карты
            SmallFloatingActionButton(
                onClick = onMapRefresh,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "Обновить карту")
            }
            Spacer(Modifier.height(8.dp))
                // НОВАЯ КНОПКА: Моё местоположение
            MyLocationButton(  // Без onClick
                viewModel = viewModel
            )
        }
            // 3. Виджет деталей заведения (нижний)
        val currentEstablishment by viewModel.currentEstablishment.collectAsState()
        val scope = rememberCoroutineScope()
        val isDetailWidgetVisible by viewModel.isDetailWidgetVisible.collectAsState()

        AnimatedVisibility(
            visible = isDetailWidgetVisible && currentEstablishment != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomBarHeightWithPadding + 16.dp)
                .padding(horizontal = 16.dp)
        ) {
            currentEstablishment?.let { establishment ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val imageBytes = base64ToByteArray(
                                    establishment.photoBase64s.firstOrNull() ?: "")
                                if (imageBytes != null) {
                                    Image(
                                        painter = rememberAsyncImagePainter(imageBytes),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                }
                                Text(
                                    text = establishment.name ?: "Error",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 2
                                )
                            }
                            IconButton(
                                onClick = { viewModel.closeDetailWidget() },
                                modifier = Modifier.size(24.dp) // Уменьшаем кнопку
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Закрыть")
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val statusText = establishment.operatingHoursString ?: "Нет данных"
                            val isClosed = statusText.contains("Закрыто", ignoreCase = true)
                            val statusColor = if (isClosed) MaterialTheme.colorScheme.error else Color(0xFF388E3C) // Зеленый
                            val statusMsg = if (statusText == "Нет данных") "Нет данных" else if (isClosed) "Сейчас закрыто" else "Сейчас открыто"
                            InfoRow(
                                icon = Icons.Default.Check,
                                text = statusMsg,
                                color = statusColor
                            )
                            InfoRow(
                                icon = Icons.Filled.LocationOn,
                                text = establishment.address ?: "Error"
                            )
                            InfoRow(
                                icon = Icons.Default.Menu,
                                text = convertTypeToWord(establishment.type ?: TypeOfEstablishment.Error)
                            )
                            InfoRow(
                                icon = Icons.Filled.Star,
                                text = "Рейтинг: ${String.format("%.1f", establishment.rating)}"
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                            // 3. Кнопка перехода к деталям
                        Button(
                            onClick = {
                                val establishmentId = currentEstablishment?.id // Может быть null

                                // ⭐ Добавляем явную проверку на null
                                if (establishmentId != null) {
                                    viewModel.closeDetailWidget()
                                    scope.launch {
                                        delay(300)

                                        // Переключение на вкладку "Searching"
                                        navController.navigate(SealedButtonBar.Searching.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }

                                        // Навигация к деталям с гарантированно не-null ID
                                        navController.navigate(
                                            EstablishmentScreens.EstablishmentDetail.createRoute(establishmentId)
                                        )
                                    }
                                } else {
                                    // Опционально: обработать ошибку, например, вывести Toast "Данные не найдены"
                                    Log.e("Home", "Ошибка: ID заведения для навигации оказался null.")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Смотреть детали")
                        }
                    }
                }
            }
        }
    }
}

/**
Вспомогательный Composable для отображения строки "Иконка + Текст"
 */
@Composable
private fun InfoRow(
    icon: ImageVector,
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
    }
}

@Composable
fun MyLocationButton(
    viewModel: EstablishmentViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val isTracking by viewModel.isLocationTracking.collectAsState()
    val hasLocation by viewModel.hasUserLocation.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            viewModel.toggleLocationTracking()
        } else {
// TODO: Показать Snackbar или диалог "Разрешения не даны"
        }
    }
    val backgroundColor = when {
        isTracking -> MaterialTheme.colorScheme.primary
        hasLocation -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val icon = when {
        isTracking -> Icons.Filled.LocationOn
        hasLocation -> Icons.Filled.Place
        else -> Icons.Filled.LocationOn // Изменено на LocationOn для лучшей визуализации (серый цвет)
    }
    SmallFloatingActionButton(
        onClick = {
            if (viewModel.hasLocationPermission()) {
                viewModel.toggleLocationTracking()
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        },
        containerColor = backgroundColor,
        contentColor = if (isTracking) Color.White else MaterialTheme.colorScheme.onSurface,
        modifier = modifier.size(48.dp)
    ) {
        Icon(icon, contentDescription = "Моё местоположение")
    }
}
@Composable
fun OsmMapAndroidView(
    refreshTrigger: Boolean,
    modifier: Modifier = Modifier,
    onMapCreated: (MapView) -> Unit = {}
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            Configuration.getInstance().load(context, androidx.preference.PreferenceManager.getDefaultSharedPreferences(context))
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                minZoomLevel = 5.0  // Лимит максимального удаления (минимальный зум)
                controller.setZoom(15.0)
                controller.setCenter(GeoPoint(53.9006, 27.5590)) // Центр Минска
            }
        },
        update = { mapView ->
            onMapCreated(mapView)
            if (refreshTrigger) {
                mapView.invalidate() // Обновление карты при refreshTrigger
            }
        }
    )
}