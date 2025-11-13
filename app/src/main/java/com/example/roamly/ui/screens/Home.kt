package com.example.roamly.ui.screens

import android.content.Context
import android.util.Base64 // ⭐ Импорт для Base64
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.* // ⭐ Импорт Box и fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.roamly.PointBuilder
import com.example.roamly.entity.DTO.EstablishmentDisplayDto
import com.example.roamly.entity.ViewModel.EstablishmentViewModel
import com.example.roamly.entity.convertTypeToWord
import com.example.roamly.ui.screens.establishment.base64ToByteArray
import com.example.roamly.ui.screens.sealed.EstablishmentScreens
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

@Composable
fun HomeScreen(navController: NavController, mapRefreshKey: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        OsmMapAndroidView(refreshTrigger = mapRefreshKey, modifier = Modifier.fillMaxSize())

        EstablishmentDetailWidget(
            navController = navController,
            // ⭐ ИСПРАВЛЕНИЕ: Modifier.align применяется здесь, к AnimatedVisibility
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
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
        pointBuilder.BuildAllMarkers(mapRefreshKey = refreshTrigger)
    }
}

// =================================================================
// UI ВИДЖЕТА
// =================================================================
@Composable
fun EstablishmentDetailWidget(
    navController: NavController,
    viewModel: EstablishmentViewModel = hiltViewModel(),
    modifier: Modifier // Принимаем modifier из HomeScreen
) {
    val isVisible by viewModel.isDetailWidgetVisible.collectAsState()
    // 'establishment' - это полный DTO (EstablishmentDisplayDto)
    val establishment by viewModel.selectedEstablishment.collectAsState()

    val currentEstablishment = establishment ?: return

    AnimatedVisibility(
        visible = isVisible,
        // ⭐ ИСПРАВЛЕНИЕ: Применяем modifier (с .align()) к AnimatedVisibility
        modifier = modifier,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        // ⭐ UI: Используем Surface вместо Card для M3-вида
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh, // Цвет M3
            tonalElevation = 6.dp,
            shadowElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                // 1. Заголовок и кнопка закрытия
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top // Выравнивание по верху
                ) {
                    // Группируем фото и название
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f) // Занимаем место, отодвигая кнопку
                    ) {

                        // ⭐ ИСПРАВЛЕНИЕ 1: Фото отображается, только если оно есть
                        val photoBase64 = currentEstablishment.photoBase64s.firstOrNull()
                        if (photoBase64 != null) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = base64ToByteArray(photoBase64)
                                ),
                                contentDescription = currentEstablishment.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                            Spacer(Modifier.width(12.dp))
                        }

                        Text(
                            text = currentEstablishment.name,
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

                // 2. ⭐ UI: Блок информации с иконками
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    // Логика статуса
                    val statusText = currentEstablishment.operatingHoursString ?: "Нет данных"
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
                        text = currentEstablishment.address
                    )
                    InfoRow(
                        icon = Icons.Default.Menu,
                        text = convertTypeToWord(currentEstablishment.type)
                    )
                    InfoRow(
                        icon = Icons.Filled.Star,
                        text = "Рейтинг: ${String.format("%.1f", currentEstablishment.rating)}"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Кнопка перехода к деталям
                Button(
                    onClick = {
                        viewModel.closeDetailWidget()
                        navController.navigate(
                            EstablishmentScreens.EstablishmentDetail.createRoute(currentEstablishment.id)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Смотреть детали")
                }
            }
        }
    }
}

/**
 * Вспомогательный Composable для отображения строки "Иконка + Текст"
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
            tint = MaterialTheme.colorScheme.primary, // Цвет иконки
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color // Цвет текста (особый для статуса)
        )
    }
}

// TODO: Оптимизировать загрузку путем создания более легковестного DTO