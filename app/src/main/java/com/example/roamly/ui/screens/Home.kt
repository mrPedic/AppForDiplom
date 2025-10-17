package com.example.roamly.ui.screens

import android.content.Context
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import android.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.roamly.PointBuilder
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon // ⬅️ Импорт для круга


@Composable
fun HomeScreen(navController: NavController) {
    OsmMapAndroidView()
}

@Composable
fun OsmMapAndroidView(modifier: Modifier = Modifier) {
    // MapView должен быть доступен для BuildAllMarkers
    var mapState by remember { mutableStateOf<MapView?>(null) } // <-- Сохраняем ссылку на MapView

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            val mapView = MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                val minskPoint = GeoPoint(53.9006, 27.5590)
                controller.setZoom(14.0)
                controller.setCenter(minskPoint)

                // Добавление круга (если нужно)
                val circlePolygon = Polygon(this)
                circlePolygon.points = Polygon.pointsAsCircle(minskPoint, 5000.0) // 5000м радиус
                circlePolygon.fillColor = 0x20FF0000 // Полупрозрачный красный
                circlePolygon.strokeColor = Color.RED
                circlePolygon.strokeWidth = 2f
                overlays.add(circlePolygon)

                invalidate()
            }
            mapState = mapView // <-- Сохраняем MapView в состоянии Compose
            mapView
        },
        update = { view ->
            // Обновления
        }
    )

    // Вызываем Composable для построения маркеров, когда MapView готов
    mapState?.let { mapView ->
        val pointBuilder = remember(mapView) { PointBuilder(mapView) }
        pointBuilder.BuildAllMarkers()
    }
}