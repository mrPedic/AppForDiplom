package com.example.roamly

import android.R.attr.radius
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.roamly.entity.EstablishmentDisplayDto
import com.example.roamly.entity.EstablishmentViewModel
import com.example.roamly.entity.convertTypeToColor
import com.example.roamly.factory.RetrofitFactory
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer // Убедитесь, что это импортировано!
import org.osmdroid.bonuspack.clustering.StaticCluster
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Класс для управления созданием маркеров на карте osmdroid.
 * Принимает MapView в конструкторе.
 */
class PointBuilder(
    private val mapView: MapView
) {
    private val markerClusterer: RadiusMarkerClusterer

    init {
        requireNotNull(mapView.context) { "MapView context must not be null" }

        // Инициализация кластеризатора в init блоке
        markerClusterer = object : RadiusMarkerClusterer(mapView.context) {
            // Override buildClusterMarker to customize the cluster icon and its click listener
            override fun buildClusterMarker(cluster: StaticCluster, mapView: MapView): Marker {
                val clusterMarker = super.buildClusterMarker(cluster, mapView)

                // Create the text paint object
                val textPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 12.dpToPx().toFloat()
                    textAlign = Paint.Align.CENTER
                    isFakeBoldText = true
                    isAntiAlias = true
                }

                val clusterIconBitmap = createClusterIconBitmap(mapView.context).copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(clusterIconBitmap)

                val text = cluster.size.toString()
                val x = clusterIconBitmap.width / 2f
                val y = (clusterIconBitmap.height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
                canvas.drawText(text, x, y, textPaint)

                clusterMarker.icon = BitmapDrawable(mapView.context.resources, clusterIconBitmap)

                // --- START OF THE FIX ---
                // Set the click listener directly on the marker being built
                clusterMarker.setOnMarkerClickListener { marker, map ->
                    Log.d("PointBuilder", "Клик по кластеру: ${marker.title}")
                    try {
                        map?.controller?.animateTo(marker.position)
                        map?.controller?.zoomIn()
                    } catch (e: IllegalStateException) {
                        Log.w("PointBuilder", "Ошибка при анимации к кластеру: ${e.message}")
                    }
                    true // Event handled
                }
                // --- END OF THE FIX ---

                return clusterMarker
            }
        }.apply {
            // Радиус кластеризации в пикселях
            setRadius(100)

            // Create a base bitmap for the cluster icon.
            // This is still useful as a fallback or for the initial state.
            val clusterIconBitmap = createClusterIconBitmap(mapView.context)
            setIcon(clusterIconBitmap)
        }
        // Добавляем кластеризатор в оверлеи карты
        mapView.overlays.add(markerClusterer)
    }

    /**
     * Создает Bitmap для иконки кластера.
     * ✅ Теперь возвращает Bitmap, а не Drawable
     */
    private fun createClusterIconBitmap(context: Context): Bitmap {
        try {
            val sizePx = 40.dpToPx()

            // Создание фона (фиолетовый круг)
            val clusterBackground = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#FF6200EE"))
                setBounds(0, 0, sizePx, sizePx)
            }

            return createBitmapFromDrawable(clusterBackground)
        } catch (e: Exception) {
            Log.e("PointBuilder", "Ошибка создания иконки кластера: ${e.message}", e)
            // Возвращаем запасной Bitmap, чтобы избежать краша
            return Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888)
        }
    }

    /**
     * Компонент Compose, который заполняет карту маркерами из ViewModel.
     */
    @Composable
    fun BuildAllMarkers(
        viewModel: EstablishmentViewModel = hiltViewModel()
    ) {
        val establishments by viewModel.userEstablishments.collectAsState(initial = emptyList())
        val isLoading by viewModel.isLoading.collectAsState(initial = false)
        val errorMessage by viewModel.errorMessage.collectAsState(initial = null)

        LaunchedEffect(key1 = viewModel) {
            if (establishments.isEmpty() && !isLoading && errorMessage == null) {
                viewModel.fetchAllEstablishments()
            }
        }

        if (!isLoading && errorMessage == null) {
            LaunchedEffect(establishments) {
                try {
                    // Очищаем только маркеры кластеризатора
                    markerClusterer.items.clear()

                    if (establishments.isNotEmpty()) {
                        Log.d("PointBuilder", "Найдено ${establishments.size} заведений для отображения.")
                        Log.i("PointBuilder", "Используемый адрес сервера: ${RetrofitFactory.BASE_URL}")

                        establishments.forEach { establishment ->
                            val marker = createEstablishmentMarker(establishment)
                            markerClusterer.add(marker)
                        }

                        // Запускаем кластеризацию
                        markerClusterer.clusterer(mapView)
                        mapView.invalidate()
                    } else {
                        Log.d("PointBuilder", "Список заведений пуст.")
                    }
                } catch (e: Exception) {
                    Log.e("PointBuilder", "Ошибка при обновлении маркеров: ${e.message}", e)
                    Toast.makeText(
                        mapView.context,
                        "Ошибка загрузки маркеров: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } else if (errorMessage != null) {
            LaunchedEffect(errorMessage) {
                Toast.makeText(
                    mapView.context,
                    "Ошибка: $errorMessage",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ... (Остальные вспомогательные функции, включая getMarkerDrawableWithColor, createEstablishmentMarker, dpToPx, createBitmapFromDrawable остаются без изменений)

    /**
     * Преобразует XML-макет в Drawable.
     */
    @SuppressLint("InflateParams", "ResourceType")
    private fun getMarkerDrawableWithColor(
        @ColorInt dotColor: Int
    ): Drawable? {
        return try {
            val inflater = LayoutInflater.from(mapView.context)
            val markerView = inflater.inflate(R.drawable.ic_map_flag, null)
            val flagRoot = markerView.findViewById<View>(R.id.flag_root)
                ?: throw IllegalStateException("flag_root not found in ic_map_flag")

            val backgroundDrawable = flagRoot.background
            if (backgroundDrawable is GradientDrawable) {
                // ✅ FIX: Use PorterDuffColorFilter to apply the tint
                backgroundDrawable.mutate().colorFilter =
                    PorterDuffColorFilter(dotColor, PorterDuff.Mode.SRC_IN)
            } else {
                // This is a good fallback
                flagRoot.setBackgroundColor(dotColor)
            }

            val sizePx = 20.dpToPx()
            markerView.measure(
                View.MeasureSpec.makeMeasureSpec(sizePx, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(sizePx, View.MeasureSpec.EXACTLY)
            )
            markerView.layout(0, 0, markerView.measuredWidth, markerView.measuredHeight)

            val bitmap = Bitmap.createBitmap(
                markerView.measuredWidth,
                markerView.measuredHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            markerView.draw(canvas)

            BitmapDrawable(mapView.context.resources, bitmap)
        } catch (e: Exception) {
            Log.e("PointBuilder", "Ошибка создания кастомной иконки из XML: ${e.message}", e)
            null
        }
    }

    /**
     * Создает один маркер osmdroid.
     */
    private fun createEstablishmentMarker(establishment: EstablishmentDisplayDto): Marker {
        val geoPoint = GeoPoint(establishment.latitude, establishment.longitude)
        val marker = Marker(mapView)

        marker.apply {
            position = geoPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)

            val customIcon = getMarkerDrawableWithColor(convertTypeToColor(establishment.type))
            if (customIcon != null) {
                icon = customIcon
            }

            title = establishment.name
            subDescription = "Рейтинг: ${String.format("%.1f", establishment.rating)}\n" +
                    "Статус: ${establishment.status}\n" +
                    "Адрес: ${establishment.address}"

            // Устанавливаем слушатель клика для одиночного маркера
            setOnMarkerClickListener { m, _ ->
                Toast.makeText(
                    mapView.context,
                    "ID заведения: ${establishment.name}",
                    Toast.LENGTH_SHORT
                ).show()

                if (m.isInfoWindowShown) {
                    m.closeInfoWindow()
                } else {
                    m.showInfoWindow()
                }
                true
            }
        }
        return marker
    }

    // Вспомогательные утилиты
    private fun Int.dpToPx(): Int {
        return (this * mapView.context.resources.displayMetrics.density).toInt()
    }

    private fun createBitmapFromDrawable(drawable: Drawable): Bitmap {
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 100
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 100

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}