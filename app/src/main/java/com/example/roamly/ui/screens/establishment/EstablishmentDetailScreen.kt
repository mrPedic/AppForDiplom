// EstablishmentDetailScreen.kt
@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.roamly.ui.screens.establishment

import android.os.Build
import android.util.Base64
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialogDefaults.containerColor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.preference.PreferenceManager
import coil.compose.rememberAsyncImagePainter
import com.example.roamly.classes.cl_menu.Drink
import com.example.roamly.classes.cl_menu.Food
import com.example.roamly.classes.cl_menu.MenuOfEstablishment
import com.example.roamly.entity.DTO.forDispalyEstablishmentDetails.DescriptionDTO
import com.example.roamly.entity.DTO.forDispalyEstablishmentDetails.MapDTO
import com.example.roamly.entity.LoadState
import com.example.roamly.entity.ReviewEntity
import com.example.roamly.entity.ViewModel.EstablishmentDetailViewModel
import com.example.roamly.entity.ViewModel.UserViewModel
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EstablishmentDetailScreen(establishmentId: Long, navController: NavController) {
    val viewModel: EstablishmentDetailViewModel = hiltViewModel()
    val userViewModel: UserViewModel = hiltViewModel()
    val coroutineScope = rememberCoroutineScope()

    val descriptionState by viewModel.descriptionState.collectAsState()
    val reviewsState by viewModel.reviewsState.collectAsState()
    val mapState by viewModel.mapState.collectAsState()
    val menuState by viewModel.menuState.collectAsState()
    val photosState by viewModel.photosState.collectAsState()
    val favoriteState by viewModel.favoriteState.collectAsState()

    val user by userViewModel.user.collectAsState()
    val userId = user.id ?: 0L

    LaunchedEffect(Unit) {
        viewModel.fetchAllDetails(establishmentId, user.id)
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(
            initialHeightOffset = 0f,
            initialContentOffset = 0f
        ),
        flingAnimationSpec = null
    )

    val pagerState = rememberPagerState(pageCount = { 4 })
    val tabs = listOf("Описание", "Меню", "Отзывы", "Карта")

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 3) {
            scrollBehavior.state.heightOffset = 0f
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    (descriptionState as? LoadState.Success)?.data?.let { desc ->
                        Text(desc.name)
                    } ?: Text("Заведение")
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Назад")
                    }
                },
                actions = {
                    val isFavorite = (favoriteState as? LoadState.Success)?.data ?: false
                    if (user.id != null) {
                        IconButton(onClick = {
                            if (isFavorite) {
                                viewModel.removeFavoriteEstablishment(userId, establishmentId)
                            } else {
                                viewModel.addFavoriteEstablishment(userId, establishmentId)
                            }
                        }) {
                            Icon(
                                if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Избранное",
                                tint = if (isFavorite) Color.Red else LocalContentColor.current
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                windowInsets = WindowInsets(top = 0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        contentWindowInsets = WindowInsets(top = 0.dp)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // innerPadding теперь содержит только отступ от TopAppBar
        ) {
            // Фото-карусель
            val collapsedFraction by animateFloatAsState(scrollBehavior.state.collapsedFraction)

            (photosState as? LoadState.Success)?.data?.let { photos ->
                if (photos.isNotEmpty()) {
                    val decodedPhotos = remember(photos) { photos.map { Base64.decode(it, Base64.DEFAULT) } }
                    val photoPagerState = rememberPagerState(pageCount = { photos.size })

                    Box(
                        modifier = Modifier
                            .height(280.dp * (1f - collapsedFraction))
                            .fillMaxWidth()
                    ) {
                        HorizontalPager(state = photoPagerState) { page ->
                            Image(
                                painter = rememberAsyncImagePainter(decodedPhotos[page]),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            } ?: Box(
                modifier = Modifier
                    .height(280.dp * (1f - collapsedFraction))
                    .background(Color.LightGray)
                    .fillMaxWidth()
            ) {
                Text("Нет фото", modifier = Modifier.align(Alignment.Center))
            }

            TabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(title) }
                    )
                }
            }

            // Остальной код HorizontalPager остаётся без изменений
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val listState = rememberLazyListState()
                LaunchedEffect(page) { listState.animateScrollToItem(0) }

                val enableNestedScroll = page != 3

                when (page) {
                    0 -> LazyContent(
                        state = listState,
                        scrollBehavior = scrollBehavior,
                        enableNestedScroll = enableNestedScroll
                    ) {
                        item {
                            when (descriptionState) {
                                is LoadState.Loading -> Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                                is LoadState.Success -> DescriptionSection((descriptionState as LoadState.Success).data)
                                is LoadState.Error -> ErrorItem((descriptionState as LoadState.Error).message) {
                                    viewModel.retryDescription(establishmentId)
                                }
                            }
                        }
                    }

                    1 -> LazyContent(
                        state = listState,
                        scrollBehavior = scrollBehavior,
                        enableNestedScroll = enableNestedScroll
                    ) {
                        item {
                            when (menuState) {
                                is LoadState.Loading -> Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                                is LoadState.Success -> MenuSection((menuState as LoadState.Success).data)
                                is LoadState.Error -> ErrorItem((menuState as LoadState.Error).message) {
                                    viewModel.retryMenu(establishmentId)
                                }
                            }
                        }
                    }

                    2 -> LazyContent(
                        state = listState,
                        scrollBehavior = scrollBehavior,
                        enableNestedScroll = enableNestedScroll
                    ) {
                        item {
                            when (reviewsState) {
                                is LoadState.Loading -> Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                                is LoadState.Success -> ReviewsSection((reviewsState as LoadState.Success).data)
                                is LoadState.Error -> ErrorItem((reviewsState as LoadState.Error).message) {
                                    viewModel.retryReviews(establishmentId)
                                }
                            }
                        }
                    }

                    3 -> Column(modifier = Modifier.padding(8.dp)) {
                        when (mapState) {
                            is LoadState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                            is LoadState.Success -> MapSection((mapState as LoadState.Success).data)
                            is LoadState.Error -> ErrorItem((mapState as LoadState.Error).message) {
                                viewModel.retryMap(establishmentId)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Исправленная функция — теперь content имеет тип LazyListScope.() -> Unit
@Composable
private fun LazyContent(
    state: LazyListState,
    scrollBehavior: TopAppBarScrollBehavior,
    enableNestedScroll: Boolean,
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        state = state,
        modifier = Modifier
            .fillMaxSize()
            .then(if (enableNestedScroll) Modifier.nestedScroll(scrollBehavior.nestedScrollConnection) else Modifier)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        content = content
    )
}


@Composable
private fun ErrorItem(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("Ошибка: $message")
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRetry) { Text("Повторить") }
    }
}

// Остальные composables без изменений
@Composable
fun DescriptionSection(desc: DescriptionDTO) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(desc.description, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Адрес: ${desc.address}", style = MaterialTheme.typography.bodyMedium)
            Text("Рейтинг: ${desc.rating}", style = MaterialTheme.typography.bodyMedium)
            Text("Тип: ${desc.type}", style = MaterialTheme.typography.bodyMedium)
            desc.operatingHoursString?.let { Text("Часы работы: $it", style = MaterialTheme.typography.bodyMedium) }
            Text("Дата создания: ${desc.dateOfCreation}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun MenuSection(menu: MenuOfEstablishment) {
    Column {
        Text("Еда", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        menu.foodGroups.forEach { group ->
            Text(group.name ?: "Группа", fontWeight = FontWeight.Bold)
            LazyRow {
                items(group.items) { food ->
                    FoodCard(food = food, onClick = {})
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        Text("Напитки", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        menu.drinksGroups.forEach { group ->
            Text(group.name ?: "Группа", fontWeight = FontWeight.Bold)
            LazyRow {
                items(group.items) { drink ->
                    DrinkCard(drink = drink, onClick = {})
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ReviewsSection(reviews: List<ReviewEntity>) {
    if (reviews.isEmpty()) {
        Text("Нет отзывов", style = MaterialTheme.typography.bodyLarge)
    } else {
        reviews.forEach { review ->
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Рейтинг: ${review.rating}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(review.reviewText, style = MaterialTheme.typography.bodyMedium)
                    review.photoBase64?.let { photo ->
                        Image(
                            painter = rememberAsyncImagePainter(Base64.decode(photo, Base64.DEFAULT)),
                            contentDescription = null,
                            modifier = Modifier.size(100.dp).padding(top = 8.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Дата: ${review.dateOfCreation}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun MapSection(mapData: MapDTO) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
            .clipToBounds()
            .clip(RoundedCornerShape(12.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(450.dp)
                .clip(RoundedCornerShape(12.dp)) // красивые углы (опционально)
        ) {
            // Сама карта
            AndroidView(
                factory = { ctx ->
                    Configuration.getInstance()
                        .load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(false)
                        setBuiltInZoomControls(false)
                        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

                        controller.setZoom(15.0)
                        controller.setCenter(GeoPoint(mapData.latitude, mapData.longitude))

                        overlays.add(Marker(this).apply {
                            position = GeoPoint(mapData.latitude, mapData.longitude)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        })

                        invalidate()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Невидимый слой, который перехватывает ВСЕ касания
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { /* ничего не делаем */ }
                        detectDragGestures { _, _ -> /* блокируем драг */ }
                    }
            )
        }
    }
}