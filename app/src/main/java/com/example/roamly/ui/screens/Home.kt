package com.example.roamly.ui.screens

import android.Manifest
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.roamly.PointBuilder
import com.example.roamly.entity.DTO.establishment.EstablishmentDisplayDto
import com.example.roamly.entity.EstablishmentLoadState
import com.example.roamly.entity.ViewModel.EstablishmentViewModel
import com.example.roamly.entity.classes.convertTypeToWord
import com.example.roamly.ui.screens.sealed.EstablishmentScreens
import com.example.roamly.ui.screens.sealed.SealedButtonBar
import com.example.roamly.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView


fun base64ToByteArray(base64: String): ByteArray = Base64.decode(base64, Base64.DEFAULT)

@Composable
fun HomeScreen(
    navController: NavController,
    mapRefreshKey: Boolean,
    onMapRefresh: () -> Unit
) {
    val bottomBarHeightWithPadding = 85.dp
    val viewModel: EstablishmentViewModel = hiltViewModel()
    var mapState by remember { mutableStateOf<MapView?>(null) }

    val loadState by viewModel.establishmentLoadState.collectAsState()
    val isDetailWidgetVisible by viewModel.isDetailWidgetVisible.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.initializeLocation()
    }

    mapState?.let { map ->
        LaunchedEffect(map) {
            viewModel.mapView = map
        }
        val pointBuilder = remember(map) { PointBuilder(map) }
        pointBuilder.BuildAllMarkers()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OsmMapAndroidView(
            refreshTrigger = mapRefreshKey,
            modifier = Modifier.fillMaxSize(),
            onMapCreated = { map -> mapState = map }
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 12.dp),
            horizontalAlignment = Alignment.End
        ) {
            SmallFloatingActionButton(
                onClick = onMapRefresh,
                containerColor = AppTheme.colors.SecondaryContainer,
                contentColor = AppTheme.colors.MainText,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "Обновить карту")
            }
            Spacer(Modifier.height(8.dp))
            MyLocationButton(viewModel = viewModel)
        }

        AnimatedVisibility(
            visible = isDetailWidgetVisible,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomBarHeightWithPadding + 16.dp)
                .padding(horizontal = 16.dp)
        ) {
            when (loadState) {
                is EstablishmentLoadState.Idle -> Unit
                is EstablishmentLoadState.Loading -> LoadingCard()
                is EstablishmentLoadState.Error -> ErrorCard((loadState as EstablishmentLoadState.Error).message)
                is EstablishmentLoadState.Success -> {
                    val state = loadState as EstablishmentLoadState.Success
                    DetailCard(
                        establishment = state.data,
                        isPhotoLoading = state.photosLoading,
                        onClose = { viewModel.closeDetailWidget() },
                        onViewDetails = {
                            val id = state.data.id
                            viewModel.closeDetailWidget()
                            scope.launch {
                                delay(300)
                                navController.navigate(SealedButtonBar.Searching.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                navController.navigate(EstablishmentScreens.EstablishmentDetail.createRoute(id))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = RoundedCornerShape(30.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.SecondaryContainer)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AppTheme.colors.MainBorder)
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.MainFailure.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Build,
                contentDescription = null,
                tint = AppTheme.colors.MainFailure,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.MainText
            )
        }
    }
}

@Composable
private fun DetailCard(
    establishment: EstablishmentDisplayDto,
    isPhotoLoading: Boolean,
    onClose: () -> Unit,
    onViewDetails: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        elevation = CardDefaults.cardElevation(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.MainContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                AsyncEstablishmentImage(
                    photoBase64s = establishment.photoBase64s,
                    placeholderColor = AppTheme.colors.SecondaryContainer,
                    contentDescription = "Фото заведения"
                )

                Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                    Text(
                        text = establishment.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = AppTheme.colors.MainText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Закрыть",
                        tint = AppTheme.colors.SecondaryText
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = AppTheme.colors.SecondaryBorder)
            Spacer(Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val hours = establishment.operatingHoursString ?: "Нет данных"
                val isClosed = hours.contains("Закрыто", ignoreCase = true)
                val statusColor = if (isClosed) AppTheme.colors.MainFailure else AppTheme.colors.MainSuccess
                val statusText = when {
                    hours == "Нет данных" -> "Нет данных"
                    isClosed -> "Сейчас закрыто"
                    else -> "Сейчас открыто"
                }

                InfoRow(icon = Icons.Default.Check, text = statusText, color = statusColor)
                InfoRow(icon = Icons.Filled.LocationOn, text = establishment.address)
                InfoRow(icon = Icons.Default.Menu, text = convertTypeToWord(establishment.type))
                InfoRow(icon = Icons.Filled.Star, text = "Рейтинг: ${String.format("%.1f", establishment.rating)}")
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = onViewDetails,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.colors.MainSuccess,
                    contentColor = AppTheme.colors.MainText
                )
            ) {
                Text("Смотреть детали")
            }
        }
    }
}

@Composable
fun MyLocationButton(
    viewModel: EstablishmentViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val isTracking by viewModel.isLocationTracking.collectAsState()
    val hasLocation by viewModel.hasUserLocation.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            viewModel.toggleLocationTracking()
        }
    }

    val backgroundColor = when {
        isTracking -> AppTheme.colors.MainSuccess           // Яркий акцент, когда активно
        hasLocation -> AppTheme.colors.SecondarySuccess     // Вторичный успех, когда есть позиция
        else -> AppTheme.colors.SecondaryContainer         // Нейтральный контейнер по умолчанию
    }

    val contentColor = if (isTracking) AppTheme.colors.MainContainer else AppTheme.colors.MainText

    val icon = when {
        isTracking -> Icons.Filled.LocationOn
        hasLocation -> Icons.Filled.Place
        else -> Icons.Filled.Build
    }

    SmallFloatingActionButton(
        onClick = {
            if (viewModel.hasLocationPermission()) {
                viewModel.toggleLocationTracking()
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        },
        containerColor = backgroundColor,
        contentColor = contentColor,
        modifier = modifier.size(48.dp)
    ) {
        Icon(icon, contentDescription = "Моё местоположение")
    }
}

@Composable
fun OsmMapAndroidView(
    refreshTrigger: Boolean,
    modifier: Modifier = Modifier,
    onMapCreated: (MapView) -> Unit = {}
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            Configuration.getInstance().load(
                context,
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
            )
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                minZoomLevel = 5.0
                controller.setZoom(15.0)
                controller.setCenter(GeoPoint(53.9006, 27.5590))
            }
        },
        update = { mapView ->
            onMapCreated(mapView)
            if (refreshTrigger) mapView.invalidate()
        }
    )
}

@Composable
fun AsyncEstablishmentImage(
    photoBase64s: List<String>,
    modifier: Modifier = Modifier,
    placeholderColor: Color = AppTheme.colors.SecondaryContainer,
    contentDescription: String? = null
) {
    var bytes by remember { mutableStateOf<ByteArray?>(null) }
    val firstBase64 = photoBase64s.firstOrNull()

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(placeholderColor),
        contentAlignment = Alignment.Center
    ) {
        LaunchedEffect(firstBase64) {
            if (!firstBase64.isNullOrBlank()) {
                bytes = withContext(Dispatchers.IO) {
                    try {
                        base64ToByteArray(firstBase64)
                    } catch (e: Exception) {
                        Log.e("AsyncImage", "Decode error: ${e.message}")
                        null
                    }
                }
            }
        }

        bytes?.let {
            Image(
                painter = rememberAsyncImagePainter(it),
                contentDescription = contentDescription,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } ?: Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
            tint = AppTheme.colors.SecondaryText
        )
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color = AppTheme.colors.MainText
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppTheme.colors.MainBorder,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}