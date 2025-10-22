package com.example.roamly.ui.screens

import android.content.Context
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.roamly.PointBuilder // Предполагается, что этот класс существует
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView


@Composable
fun HomeScreen(navController: NavController, mapRefreshKey: Boolean) { // ⭐ Принимаем ключ обновления
    OsmMapAndroidView(refreshTrigger = mapRefreshKey) // ⭐ Передаем его дальше
}

@Composable
fun OsmMapAndroidView(modifier: Modifier = Modifier, refreshTrigger: Boolean) { // ⭐ Принимаем ключ
    var mapState by remember { mutableStateOf<MapView?>(null) }

    AndroidView(
        modifier = modifier,
        // ⭐ Добавляем refreshTrigger в key, чтобы при его изменении запускался update блок
        factory = { ctx ->
            Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            val mapView = MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                this.minZoomLevel = 6.0

                val minskPoint = GeoPoint(53.9006, 27.5590)
                controller.setZoom(14.0)
                controller.setCenter(minskPoint)

                invalidate()
            }
            mapState = mapView
            mapView
        },
        update = { view ->
            // ⭐ НОВОЕ: Этот блок будет перезапускаться при изменении refreshTrigger
            // Мы используем его для принудительного обновления карты (перерисовки тайлов и маркеров)
            if (refreshTrigger) {
                view.invalidate() // Принудительно обновляем вид карты
                // Можно добавить логику, которая принудительно перецентрирует карту,
                // если обновление подразумевает возврат к исходному виду:
                // view.controller.animateTo(GeoPoint(53.9006, 27.5590))
            }
        }
    )

    mapState?.let { mapView ->
        // Здесь PointBuilder будет запускаться каждый раз при изменении mapState или refreshTrigger
        // благодаря тому, что AndroidView с mapRefreshKey в update вызывает рекомпозицию.
        // Чтобы быть уверенным в обновлении маркеров, PointBuilder должен иметь возможность
        // очищать и перестраивать их.
        val pointBuilder = remember(mapView) { PointBuilder(mapView) }
        pointBuilder.BuildAllMarkers()
        // Если PointBuilder зависит от refreshTrigger, его нужно добавить в remember/LaunchedEffect
    }
}
