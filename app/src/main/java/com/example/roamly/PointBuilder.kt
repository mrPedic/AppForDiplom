package com.example.roamly

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.roamly.entity.EstablishmentDisplayDto
import com.example.roamly.entity.EstablishmentViewModel
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import android.util.Log // Добавлен Log для отладки
import com.example.roamly.factory.RetrofitFactory

/**
 * Класс для управления созданием маркеров на карте osmdroid.
 * Принимает MapView в конструкторе.
 */
class PointBuilder(
    val mapView: MapView
) {
    /**
     * Создает и добавляет все маркеры заведений, полученные из ViewModel, на карту.
     */
    @Composable
    fun BuildAllMarkers(
        viewModel: EstablishmentViewModel = hiltViewModel()
    ) {
        // Получаем состояние списка заведений из ViewModel
        val establishments by viewModel.userEstablishments.collectAsState(initial = emptyList())
        val isLoading by viewModel.isLoading.collectAsState(initial = false)
        val errorMessage by viewModel.errorMessage.collectAsState(initial = null)

        // Запускаем загрузку данных при первом входе на экран/при первой композиции
        LaunchedEffect(Unit) {
            // Вызываем функцию ViewModel для загрузки ВСЕХ заведений
            viewModel.fetchAllEstablishments()
        }

        // Если данные загружены и ошибок нет, строим маркеры
        if (!isLoading && errorMessage == null) {
            // Используем LaunchedEffect с ключом 'establishments'
            // чтобы пересоздать/обновить маркеры только при изменении списка
            LaunchedEffect(establishments) {
                // Очистка старых маркеров перед добавлением новых,
                // чтобы избежать дублирования, если список обновляется
                mapView.overlays.removeAll { it is Marker }

                if (establishments.isNotEmpty()) {
                    Log.d("PointBuilder", "Найдено ${establishments.size} заведений для отображения.")
                    Log.i("PointBuilder", "Используемый адрес сервера: ${RetrofitFactory.BASE_URL}")
                    establishments.forEach { establishment ->
                        // Вызываем функцию для создания одного маркера
                        // NOTE: Вложенный Composable внутри LaunchedEffect/forEach не является
                        // стандартным подходом, но для osmdroid оверлеев это приемлемо,
                        // если создание маркера не зависит от Compose State
                        createEstablishmentMarker(establishment)
                    }
                    // Важно: обновить карту после добавления всех оверлеев
                    mapView.invalidate()
                } else {
                    Log.d("PointBuilder", "Список заведений пуст.")
                    Log.i("PointBuilder", "Используемый адрес сервера: ${RetrofitFactory.BASE_URL}")
                }
            }
        }

        // TODO: Возможно, добавить отображение индикатора загрузки или ошибки здесь,
        // но это может конфликтовать с AndroidView, поэтому чаще это делают
        // в родительском Composable (например, в HomeScreen)
    }

    /**
     * Не-Composable функция для создания одного маркера osmdroid.
     * Должна вызываться только внутри LaunchedEffect или не-Composable блока.
     */
    private fun createEstablishmentMarker(establishment: EstablishmentDisplayDto) {
        val geoPoint = GeoPoint(establishment.latitude, establishment.longitude)
        val marker = Marker(mapView).apply {
            position = geoPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = establishment.name
            subDescription = "Рейтинг: ${String.format("%.1f", establishment.rating)}\n" +
                    "Статус: ${establishment.status}\n" +
                    "Адрес: ${establishment.address}"
            // TODO: Установка иконки/цвета в зависимости от TypeOfEstablishment
            // marker.icon = getMarkerIcon(establishment.typeOfEstablishment)
            // marker.color = getMarkerColor(establishment.typeOfEstablishment)
            setOnMarkerClickListener { m, _ ->
                // TODO: Логика при клике на маркер (например, показать BottomSheet или перейти)
                Log.d("MarkerClick", "Клик по заведению: ${m.title}")
                true // true означает, что событие обработано
            }
        }
        mapView.overlays.add(marker)
    }
}
