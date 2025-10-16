package com.example.roamly.ui.screens

import android.content.Context
import android.view.ViewGroup
import androidx.compose.foundation.layout.add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import android.graphics.Color
import android.util.Log
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon // ⬅️ Импорт для круга
import kotlin.to


@Composable
fun HomeScreen(navController: NavController) {
    OsmMapAndroidView()
}

@Composable
fun OsmMapAndroidView(modifier: Modifier = Modifier) {
    // IMPORTANT: конфиг осмдроид — нужно установить контекст (например в Application.onCreate лучше).
    // Здесь делаем локально: (если ты уже вызывал Configuration.getInstance().load(context, prefs), можно опустить)
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            // Настройка osmdroid
            Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            val mapView = MapView(ctx)
            mapView.setTileSource(TileSourceFactory.MAPNIK)
            mapView.setMultiTouchControls(true)
            mapView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            val minskPoint = GeoPoint(53.9006, 27.5590)

            // установка центра и зума
            val mapController = mapView.controller
            mapController.setZoom(14.0)
            mapController.setCenter(minskPoint)


            val circlePolygon = Polygon(mapView)

            val circlePoints = Polygon.pointsAsCircle(minskPoint, 5.0)
            circlePolygon.points = circlePoints

            // Customize the appearance of the circle (optional)
            circlePolygon.fillColor = 0xFFFF0000.toInt()
            circlePolygon.strokeColor = Color.BLACK
            circlePolygon.strokeWidth = 2f

            // Add the polygon to the map's overlays
            mapView.overlays.add(circlePolygon)

            // Refresh the map to display the new overlay
            mapView.invalidate()


//
//            // Пример маркера
//            val marker = Marker(mapView)
//            marker.position = GeoPoint(-6.2088, 106.8456)
//            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
//            marker.title = "Тут я"
//            mapView.overlays.add(marker)

            mapView
        },
        update = { view ->
            // если нужно — обновлять состояние карты из compose state
        }
    )
}
