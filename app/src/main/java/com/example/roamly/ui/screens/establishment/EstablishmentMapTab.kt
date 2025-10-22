package com.example.roamly.ui.screens.establishment

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker


@Composable
fun EstablishmentMapTab(
    name: String,
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val point = remember(latitude, longitude) { GeoPoint(latitude, longitude) }

    // Функция для открытия внешнего приложения навигации
    val startNavigation = {
        // Формируем URI для запроса маршрута, например, используя Google Maps:
        // geo:lat,lng?q=query (Просто указывает точку)
        // https://www.google.com/maps/dir/?api=1&destination=lat,lng (Строит маршрут)
        val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude&travelmode=driving")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri)

        // Попытка запустить внешнее приложение
        try {
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            // В случае ошибки (например, нет браузера или навигационного приложения)
            // В реальном приложении здесь нужен Toast/Snackbar
            e.printStackTrace()
        }
    }


    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Занимает большую часть пространства
                    .padding(8.dp),
                factory = { ctx ->
                    // Настройка osmdroid
                    Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(14.0)
                        controller.setCenter(point)
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        // ⭐ ИЗМЕНЕНИЕ: БЛОКИРОВКА ПЕРЕМЕЩЕНИЯ (ТОЛЬКО ЗУМ)
                        this.setOnTouchListener { v, event ->
                            // Разрешаем только Pinch-to-Zoom (2 пальца)
                            if (event.pointerCount == 2) {
                                false // Вернуть false, чтобы позволить осмдроид обрабатывать мультитач
                            } else {
                                // Блокируем одиночные касания и перетаскивание
                                true // Вернуть true, чтобы потреблять событие и не давать ему двигать карту
                            }
                        }
                    }
                },
                update = { mapView ->
                    // Обновление маркера при смене координат
                    mapView.overlays.removeAll { it is Marker }

                    val marker = Marker(mapView).apply {
                        position = point
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = name
                        subDescription = "Координаты: ${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)}"
                    }
                    mapView.overlays.add(marker)
                    mapView.controller.animateTo(point)
                    mapView.invalidate()
                }
            )

            // ⭐ ДОБАВЛЕНО: Кнопка для построения маршрута
            Button(
                onClick = startNavigation,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 16.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(text = "Построить маршрут")
            }
        }
    }
}
