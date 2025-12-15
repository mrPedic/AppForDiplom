@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.roamly.ui.screens.establishment

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.roamly.entity.DTO.TableCreationDto
import com.example.roamly.entity.TableEditorList
import com.example.roamly.entity.TableUIModel
import com.example.roamly.entity.ViewModel.EstablishmentViewModel
import com.example.roamly.entity.ViewModel.UserViewModel
import com.example.roamly.entity.classes.*
import com.example.roamly.ui.screens.sealed.EstablishmentScreens
import com.example.roamly.ui.theme.AppTheme
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

val DAYS_OF_WEEK = listOf(
    "Понедельник", "Вторник", "Среда",
    "Четверг", "Пятница", "Суббота", "Воскресенье"
)

val DEFAULT_HOURS = mapOf(
    "Понедельник" to "08:30 - 18:00",
    "Вторник" to "08:30 - 18:00",
    "Среда" to "08:30 - 18:00",
    "Четверг" to "08:30 - 18:00",
    "Пятница" to "08:30 - 18:00",
    "Суббота" to "08:30 - 14:00",
    "Воскресенье" to "Закрыто"
)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CreateEstablishmentScreen(
    navController: NavController,
    userViewModel: UserViewModel,
    viewModel: EstablishmentViewModel = hiltViewModel()
) {
    var selectedType by rememberSaveable { mutableStateOf<TypeOfEstablishment?>(null) }
    var photoUris by rememberSaveable { mutableStateOf<List<Uri>>(emptyList()) }

    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }

    var latitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var longitude by rememberSaveable { mutableStateOf<Double?>(null) }

    val status = EstablishmentStatus.PENDING_APPROVAL
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var operatingHours by rememberSaveable { mutableStateOf(DEFAULT_HOURS) }
    var tables by rememberSaveable { mutableStateOf<List<TableUIModel>>(emptyList()) }

    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    LaunchedEffect(Unit) {
        val newLat = savedStateHandle?.get<Double>(LATITUDE_KEY)
        val newLon = savedStateHandle?.get<Double>(LONGITUDE_KEY)
        if (newLat != null && newLon != null) {
            latitude = newLat
            longitude = newLon
            savedStateHandle.remove<Double>(LATITUDE_KEY)
            savedStateHandle.remove<Double>(LONGITUDE_KEY)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.fillMaxWidth().height(15.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Название заведения", color = AppTheme.colors.SecondaryText) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppTheme.colors.MainBorder,
                unfocusedBorderColor = AppTheme.colors.SecondaryBorder,
                focusedTextColor = AppTheme.colors.MainText,
                unfocusedTextColor = AppTheme.colors.MainText
            )
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Адрес", color = AppTheme.colors.SecondaryText) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppTheme.colors.MainBorder,
                unfocusedBorderColor = AppTheme.colors.SecondaryBorder,
                focusedTextColor = AppTheme.colors.MainText,
                unfocusedTextColor = AppTheme.colors.MainText
            )
        )

        Spacer(Modifier.height(12.dp))

        EstablishmentTypeDropdown(
            selectedType = selectedType,
            onTypeSelected = { selectedType = it },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Описание", color = AppTheme.colors.SecondaryText) },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppTheme.colors.MainBorder,
                unfocusedBorderColor = AppTheme.colors.SecondaryBorder,
                focusedTextColor = AppTheme.colors.MainText,
                unfocusedTextColor = AppTheme.colors.MainText
            )
        )

        Spacer(Modifier.height(16.dp))

        OperatingHoursEditor(
            operatingHours = operatingHours,
            onHoursChange = { operatingHours = it }
        )

        LocationPickerCard(
            latitude = latitude,
            longitude = longitude,
            onClick = { navController.navigate(EstablishmentScreens.MapPicker.route) },
            onClearClick = {
                latitude = null
                longitude = null
            }
        )

        HorizontalDivider(color = AppTheme.colors.SecondaryBorder)

        TableEditorList(
            tables = tables,
            onTablesChange = { tables = it }
        )

        HorizontalDivider(color = AppTheme.colors.SecondaryBorder)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = AppTheme.colors.SecondaryContainer
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "Статус заведения",
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.MainText
                )
                Text(
                    text = "На рассмотрении администрации",
                    color = AppTheme.colors.SecondaryText
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                isLoading = true
                errorMessage = null
                val userId = userViewModel.getId() ?: run {
                    isLoading = false
                    errorMessage = "Пользователь не авторизован"
                    return@Button
                }

                val base64Photos = photoUris.mapNotNull {
                    convertUriToBase64(navController.context, it)
                }

                viewModel.createEstablishment(
                    name = name,
                    description = description,
                    address = address,
                    latitude = latitude!!,
                    longitude = longitude!!,
                    createUserId = userId,
                    type = selectedType!!,
                    photoBase64s = base64Photos,
                    operatingHoursString = convertHoursMapToString(operatingHours),
                    tables = tables.map {
                        TableCreationDto(name = it.name, maxCapacity = it.maxCapacity, description =  it.description)
                    }
                ) {
                    isLoading = false
                    if (it) navController.popBackStack()
                    else errorMessage = "Ошибка создания заведения"
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppTheme.colors.MainSuccess
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = AppTheme.colors.MainText,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text("Отправить", color = AppTheme.colors.MainText)
            }
        }

        errorMessage?.let {
            Text(
                text = it,
                color = AppTheme.colors.MainFailure,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

// --------------------------------------------------------------------------
// НОВЫЙ КОМПОНЕНТ: ВЫПАДАЮЩИЙ СПИСОК ТИПОВ
// --------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstablishmentTypeDropdown(
    selectedType: TypeOfEstablishment?,
    onTypeSelected: (TypeOfEstablishment) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = remember { TypeOfEstablishment.entries.toList() }
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedType?.let { convertTypeToWord(it) } ?: "Выберите тип заведения",
            onValueChange = {},
            readOnly = true,
            label = { Text("Тип заведения", color = AppTheme.colors.SecondaryText) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppTheme.colors.MainBorder,
                unfocusedBorderColor = AppTheme.colors.SecondaryBorder,
                focusedTextColor = AppTheme.colors.MainText,
                unfocusedTextColor = AppTheme.colors.MainText,
                focusedContainerColor = AppTheme.colors.MainContainer,
                unfocusedContainerColor = AppTheme.colors.MainContainer,
                focusedLabelColor = AppTheme.colors.SecondaryText,
                unfocusedLabelColor = AppTheme.colors.SecondaryText,
                cursorColor = AppTheme.colors.MainText
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(AppTheme.colors.SecondaryContainer)
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    text = {
                        Text(
                            convertTypeToWord(selectionOption),
                            color = AppTheme.colors.MainText
                        )
                    },
                    onClick = {
                        onTypeSelected(selectionOption)
                        expanded = false
                    },
                    modifier = Modifier.background(AppTheme.colors.SecondaryContainer)
                )
            }
        }
    }
}

/**
 * Вспомогательный компонент для выбора местоположения на карте.
 * Отображает либо кнопку выбора, либо мини-карту с выбранной точкой.
 */
@Composable
fun LocationPickerCard(
    latitude: Double?,
    longitude: Double?,
    onClick: () -> Unit,
    onClearClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (latitude == null) 100.dp else 250.dp)
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (latitude == null)
                AppTheme.colors.SecondaryContainer
            else
                AppTheme.colors.MainContainer
        ),
        border = BorderStroke(1.dp, AppTheme.colors.SecondaryBorder)
    ) {
        if (latitude != null && longitude != null) {
            Box(Modifier.fillMaxSize()) {

                MiniMapView(latitude, longitude)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Координаты установлены (${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)})",
                        color = AppTheme.colors.SecondaryText,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = onClearClick) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Сбросить",
                            tint = AppTheme.colors.MainFailure
                        )
                    }
                }

                Spacer(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(onClick = onClick)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Нажмите, чтобы выбрать местоположение на карте",
                    style = MaterialTheme.typography.titleMedium,
                    color = AppTheme.colors.SecondaryText,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

/**
 * Компонент мини-карты с маркером (на основе OSMdroid).
 */
@Composable
fun MiniMapView(latitude: Double, longitude: Double) {
    val context = LocalContext.current
    val point = GeoPoint(latitude, longitude)

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            Configuration.getInstance().load(
                ctx,
                ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
            )

            MapView(ctx).apply {
                minZoomLevel = 5.0
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(false)
                controller.setZoom(14.0)
                controller.setCenter(point)

                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                overlays.add(
                    Marker(this).apply {
                        position = point
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                )
            }
        },
        update = { view ->
            view.controller.setCenter(point)
            view.overlays.removeAll { it is Marker }
            view.overlays.add(
                Marker(view).apply {
                    position = point
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
            )
            view.invalidate()
        }
    )
}

@Composable
fun OperatingHoursEditor(
    operatingHours: Map<String, String>,
    onHoursChange: (Map<String, String>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = "Время работы (ЧЧ:ММ - ЧЧ:ММ или Закрыто)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.MainText
        )

        Spacer(Modifier.height(8.dp))

        DAYS_OF_WEEK.forEach { day ->
            val currentHours = operatingHours[day] ?: "Закрыто"

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // День недели
                Text(
                    text = day,
                    modifier = Modifier.weight(0.35f),
                    color = AppTheme.colors.MainText
                )

                OutlinedTextField(
                    value = currentHours,
                    onValueChange = { newValue ->
                        onHoursChange(
                            operatingHours.toMutableMap().apply {
                                this[day] = newValue
                            }
                        )
                    },
                    placeholder = {
                        Text(
                            "08:00 - 18:00",
                            color = AppTheme.colors.SecondaryText
                        )
                    },
                    modifier = Modifier.weight(0.65f),
                    singleLine = true,
                    isError = currentHours.isNotBlank()
                            && currentHours != "Закрыто"
                            && !currentHours.matches(
                        Regex("^\\d{2}:\\d{2}\\s*-\\s*\\d{2}:\\d{2}$")
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppTheme.colors.MainBorder,
                        unfocusedBorderColor = AppTheme.colors.SecondaryBorder,
                        errorBorderColor = AppTheme.colors.MainFailure,
                        focusedTextColor = AppTheme.colors.MainText,
                        unfocusedTextColor = AppTheme.colors.MainText,
                        cursorColor = AppTheme.colors.MainText
                    )
                )
            }
        }
    }
}
