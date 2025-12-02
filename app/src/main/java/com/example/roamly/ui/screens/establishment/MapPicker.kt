@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.roamly.ui.screens.establishment

import android.content.Context
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
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
                    controller.setZoom(14.0)
                    controller.setCenter(defaultCenter)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    // Добавляем оверлей для вращения (опционально)
//                        overlays.add(RotationGestureOverlay(this))
                }

                // Оверлей для обработки нажатий
                val clickOverlay = object : Overlay(ctx) {
                    override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
                        val projection = mapView.projection
                        val geoPoint = projection.fromPixels(e.x.toInt(), e.y.toInt())
                        selectedPoint = geoPoint as GeoPoint?
                        return true
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

        // Кнопка подтверждения
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
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text(if (selectedPoint != null) "Подтвердить Место (${String.format("%.4f", selectedPoint!!.latitude)}, ${String.format("%.4f", selectedPoint!!.longitude)})" else "Нажмите на карту, чтобы выбрать место")
        }
    }
}