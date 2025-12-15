@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.roamly.ui.screens.establishment

import android.content.Context
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.roamly.ui.theme.AppTheme
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay

// Константы для передачи данных через NavController
const val LOCATION_RESULT_KEY = "location_result"
const val LATITUDE_KEY = "latitude"
const val LONGITUDE_KEY = "longitude"
const val CROPPED_IMAGE_URI_KEY = "cropped_uri"
const val MAX_PHOTOS = 5 // Максимальное количество фотографий

/**
 * Экран для выбора географических координат на карте.
 */
@Composable
fun MapPickerScreen(navController: NavController) {
    val context = LocalContext.current
    var selectedPoint by remember { mutableStateOf<GeoPoint?>(null) }
    val defaultCenter = GeoPoint(53.9006, 27.5590) // Минск

    // Состояния для управления видимостью инструкции
    var showInstruction by remember { mutableStateOf(true) }
    var mapTouched by remember { mutableStateOf(false) }
    var mapMoved by remember { mutableStateOf(false) }

    // Запускаем таймер для скрытия инструкции
    LaunchedEffect(Unit) {
        delay(3000) // По умолчанию 3 секунды
        if (showInstruction && !mapMoved) {
            showInstruction = false
        }
    }

    // Если карта двигалась, скрываем инструкцию через 1.5 секунды
    LaunchedEffect(mapMoved) {
        if (mapMoved && showInstruction) {
            delay(1500)
            showInstruction = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Компонент карты
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
                val mapView = MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)

                    // Отключаем кнопки зума
                    zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)

                    controller.setZoom(14.0)
                    controller.setCenter(defaultCenter)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                // Оверлей для обработки нажатий (выбора точки)
                val clickOverlay = object : Overlay(ctx) {
                    private var lastMoveTime = System.currentTimeMillis()

                    override fun onScroll(
                        event1: MotionEvent?,
                        event2: MotionEvent?,
                        distanceX: Float,
                        distanceY: Float,
                        mapView: MapView?
                    ): Boolean {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastMoveTime > 100) { // Debounce 100ms
                            lastMoveTime = currentTime
                            mapMoved = true
                        }
                        return super.onScroll(event1, event2, distanceX, distanceY, mapView)
                    }

                    override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
                        val projection = mapView.projection
                        val geoPoint = projection.fromPixels(e.x.toInt(), e.y.toInt())
                        selectedPoint = geoPoint as GeoPoint?
                        mapTouched = true
                        return true
                    }

                    override fun onTouchEvent(event: MotionEvent, mapView: MapView?): Boolean {
                        if (event.action == MotionEvent.ACTION_DOWN) {
                            mapTouched = true
                        }
                        return super.onTouchEvent(event, mapView)
                    }
                }
                mapView.overlays.add(clickOverlay)

                mapView
            },
            update = { mapView ->
                // Обновление маркера при смене selectedPoint
                mapView.overlays.removeAll { it is Marker }
                selectedPoint?.let { point ->
                    val marker = Marker(mapView).apply {
                        position = point
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Выбранное место"
                    }
                    mapView.overlays.add(marker)
                    mapView.controller.animateTo(point)
                }
                mapView.invalidate()
            }
        )

        // Верхний индикатор выбранных координат
        if (selectedPoint != null) {
            AnimatedVisibility(
                visible = selectedPoint != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -50 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -50 }),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = AppTheme.colors.SecondaryContainer.copy(alpha = 0.9f) // Было 0.3f, стало 0.9f (уменьшили прозрачность в 3 раза)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(AppTheme.colors.MainBorder)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Выбраны координаты: ",
                            color = AppTheme.colors.SecondaryText
                        )
                        Text(
                            text = "${String.format("%.4f", selectedPoint!!.latitude)}, ${String.format("%.4f", selectedPoint!!.longitude)}",
                            color = AppTheme.colors.MainText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Инструкция в центре, если точка не выбрана
        AnimatedVisibility(
            visible = showInstruction && selectedPoint == null,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300)),
            modifier = Modifier
                .align(Alignment.Center)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = AppTheme.colors.SecondaryContainer.copy(alpha = 0.9f) // Было 0.3f, стало 0.9f
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(AppTheme.colors.MainBorder)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Выбор местоположения",
                        tint = AppTheme.colors.MainText,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Нажмите на карту, чтобы выбрать местоположение",
                        color = AppTheme.colors.MainText,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // Кнопка подтверждения
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = AppTheme.colors.MainContainer.copy(alpha = 0.9f) // Было 0.3f, стало 0.9f
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            selectedPoint?.let {
                                // Передаем координаты обратно в предыдущий экран
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set(LATITUDE_KEY, it.latitude)
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set(LONGITUDE_KEY, it.longitude)

                                navController.popBackStack()
                            }
                        },
                        enabled = selectedPoint != null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppTheme.colors.MainSuccess.copy(alpha = 0.9f), // Было 0.3f, стало 0.9f
                            contentColor = AppTheme.colors.MainText,
                            disabledContainerColor = AppTheme.colors.MainSuccess.copy(alpha = 0.5f), // Было 0.1f, стало 0.5f
                            disabledContentColor = AppTheme.colors.MainText.copy(alpha = 0.7f) // Было 0.5f, стало 0.7f
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = if (selectedPoint != null) {
                                "Подтвердить выбор местоположения"
                            } else {
                                "Сначала выберите точку на карте"
                            },
                            color = if (selectedPoint != null) {
                                AppTheme.colors.MainText
                            } else {
                                AppTheme.colors.MainText.copy(alpha = 0.7f) // Было 0.7f, стало 0.9f
                            }
                        )
                    }

                    // Дополнительная кнопка отмены
                    if (selectedPoint != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                selectedPoint = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = AppTheme.colors.MainFailure
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(AppTheme.colors.MainFailure.copy(alpha = 0.9f)) // Было 0.5f, стало 0.9f
                            )
                        ) {
                            Text("Сбросить выбор")
                        }
                    }

                    // Кнопка возврата
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            navController.popBackStack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AppTheme.colors.SecondaryText
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(AppTheme.colors.SecondaryBorder.copy(alpha = 0.9f)) // Было 0.5f, стало 0.9f
                        )
                    ) {
                        Text("Вернуться без выбора")
                    }
                }
            }
        }
    }
}