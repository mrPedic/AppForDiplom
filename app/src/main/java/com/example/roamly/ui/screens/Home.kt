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
fun HomeScreen(navController: NavController, mapRefreshKey: Boolean) {
    OsmMapAndroidView(refreshTrigger = mapRefreshKey)
}

@Composable
fun OsmMapAndroidView(modifier: Modifier = Modifier, refreshTrigger: Boolean) {
    var mapState by remember { mutableStateOf<MapView?>(null) }

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
            if (refreshTrigger) {
                view.invalidate()
            }
        }
    )

    mapState?.let { mapView ->
        val pointBuilder = remember(mapView) { PointBuilder(mapView) }
        pointBuilder.BuildAllMarkers()
    }
}
