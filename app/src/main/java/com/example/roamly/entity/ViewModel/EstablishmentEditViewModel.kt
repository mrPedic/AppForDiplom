package com.example.roamly.entity.ViewModel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roamly.ApiService
import com.example.roamly.entity.DTO.establishment.EstablishmentDisplayDto
import com.example.roamly.entity.DTO.establishment.EstablishmentUpdateRequest
import com.example.roamly.entity.classes.TypeOfEstablishment
import com.example.roamly.ui.screens.establishment.toJsonString
import com.example.roamly.ui.screens.establishment.toMap
import com.example.roamly.ui.screens.establishment.uriToBase64
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "EstablishmentEditVM"

@HiltViewModel
class EstablishmentEditViewModel @Inject constructor(
    private val apiService: ApiService,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    // Редактируемые поля
    private val _editedName = MutableStateFlow("")
    val editedName: StateFlow<String> = _editedName.asStateFlow()

    private val _editedDescription = MutableStateFlow("")
    val editedDescription: StateFlow<String> = _editedDescription.asStateFlow()

    private val _editedAddress = MutableStateFlow("")
    val editedAddress: StateFlow<String> = _editedAddress.asStateFlow()

    private val _editedLatitude = MutableStateFlow(0.0)
    val editedLatitude: StateFlow<Double> = _editedLatitude.asStateFlow()

    private val _editedLongitude = MutableStateFlow(0.0)
    val editedLongitude: StateFlow<Double> = _editedLongitude.asStateFlow()

    private val _editedType = MutableStateFlow(TypeOfEstablishment.Restaurant)
    val editedType: StateFlow<TypeOfEstablishment> = _editedType.asStateFlow()

    private val _editedOperatingHours = MutableStateFlow<Map<String, String>>(emptyMap())
    val editedOperatingHours: StateFlow<Map<String, String>> = _editedOperatingHours.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    // Фотографии
    sealed class PhotoItem {
        data class Remote(val base64: String) : PhotoItem()
        data class Local(val uri: Uri) : PhotoItem()
    }

    private val _editedPhotos = MutableStateFlow<List<PhotoItem>>(emptyList())
    val editedPhotos: StateFlow<List<PhotoItem>> = _editedPhotos.asStateFlow()

    // Состояния загрузки/ошибки
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Инициализация из загруженных данных
    fun initWithData(dto: EstablishmentDisplayDto) {
        Log.d(TAG, "initWithData() START | dto.id=${dto.id}, name='${dto.name}', photosCount=${dto.photoBase64s.size}")
        _editedName.value = dto.name
        _editedDescription.value = dto.description ?: ""
        _editedAddress.value = dto.address
        _editedLatitude.value = dto.latitude
        _editedLongitude.value = dto.longitude
        _editedType.value = dto.type
        _editedOperatingHours.value = dto.operatingHoursString?.toMap() ?: emptyMap()
        _isInitialized.value = true
        Log.d(TAG, "initWithData() END | Инициализация завершена успешно")
    }

    // Обновление полей
    fun updateName(name: String) = _editedName.update { name }
    fun updateDescription(desc: String) = _editedDescription.update { desc }
    fun updateAddress(addr: String) = _editedAddress.update { addr }
    fun updateType(type: TypeOfEstablishment) = _editedType.update { type }
    fun updateLatitude(lat: Double) = _editedLatitude.update { lat }
    fun updateLongitude(lon: Double) = _editedLongitude.update { lon }
    fun updateOperatingHours(hours: Map<String, String>) = _editedOperatingHours.update { hours }

    // Фото
    fun addPhotos(uris: List<Uri>) {
        Log.d(TAG, "addPhotos() START | Получено URI: ${uris.size} шт.")
        _editedPhotos.update { current -> current + uris.map { PhotoItem.Local(it) } }

        viewModelScope.launch(Dispatchers.IO) {
            uris.forEachIndexed { index, uri ->
                Log.d(TAG, "addPhotos() Конвертация фото $index из ${uris.size} | uri=$uri")
                val base64 = uriToBase64(appContext, uri)
                if (base64 == null) {
                    Log.e(TAG, "addPhotos() ОШИБКА: Не удалось конвертировать URI в Base64 | uri=$uri")
                    return@forEachIndexed
                }
                Log.d(TAG, "addPhotos() Успешно конвертировано | длина Base64: ${base64.length}")

                withContext(Dispatchers.Main) {
                    _editedPhotos.update { list ->
                        list.map { item ->
                            if (item is PhotoItem.Local && item.uri == uri) {
                                PhotoItem.Remote(base64)
                            } else item
                        }
                    }
                }
            }
        }
        Log.d(TAG, "addPhotos() END | Добавление запущено в фоне")
    }

    fun removePhoto(photo: PhotoItem) {
        Log.d(TAG, "removePhoto() START | Удаляется фото: ${if (photo is PhotoItem.Remote) "Remote (Base64 len=${photo.base64.length})" else "Local (uri=${(photo as PhotoItem.Local).uri})"}")
        _editedPhotos.update { current ->
            current.filter { it !== photo }
        }
        Log.d(TAG, "removePhoto() END | Фото удалено, осталось: ${_editedPhotos.value.size}")
    }

    // Сохранение
    fun saveChanges(
        establishmentId: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        Log.d(TAG, "saveChanges() START | establishmentId=$establishmentId | Фото для отправки: ${_editedPhotos.value.size} шт.")

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val photoBase64s = withContext(Dispatchers.IO) {
                    val list = _editedPhotos.value.mapNotNull { item ->
                        when (item) {
                            is PhotoItem.Remote -> {
                                Log.d(TAG, "saveChanges() Отправка Remote фото, длина: ${item.base64.length}")
                                item.base64
                            }
                            is PhotoItem.Local -> {
                                val base64 = uriToBase64(appContext, item.uri)
                                if (base64 != null) Log.d(TAG, "saveChanges() Конвертировано Local → Base64, длина: ${base64.length}")
                                else Log.e(TAG, "saveChanges() ОШИБКА конвертации Local фото: ${item.uri}")
                                base64
                            }
                        }
                    }
                    Log.d(TAG, "saveChanges() Подготовлено фото для отправки: ${list.size} шт.")
                    list
                }

                val request = EstablishmentUpdateRequest(
                    name = _editedName.value,
                    description = _editedDescription.value,
                    address = _editedAddress.value,
                    latitude = _editedLatitude.value,
                    longitude = _editedLongitude.value,
                    type = _editedType.value,
                    photoBase64s = photoBase64s,
                    operatingHoursString = _editedOperatingHours.value.toJsonString()
                )

                Log.d(TAG, "saveChanges() Отправка запроса на сервер...")

                val response = withContext(Dispatchers.IO) {
                    apiService.updateEstablishment(establishmentId, request)
                }

                if (response.isSuccessful) {
                    Log.d(TAG, "saveChanges() SUCCESS | Ответ сервера: ${response.code()}")
                    onSuccess()
                } else {
                    val errorMsg = "Ошибка сервера: ${response.code()}"
                    Log.e(TAG, "saveChanges() ERROR | $errorMsg | body=${response.errorBody()?.string()}")
                    onError(errorMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "saveChanges() EXCEPTION | ${e.message}", e)
                onError(e.message ?: "Неизвестная ошибка")
            } finally {
                _isLoading.value = false
                Log.d(TAG, "saveChanges() END")
            }
        }
    }

    fun fetchEstablishment(id: Long) {
        Log.d(TAG, "fetchEstablishment() START | id=$id")

        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(TAG, "fetchEstablishment() Загрузка основных данных...")
                val dto = withContext(Dispatchers.IO) { apiService.getEstablishmentById(id) }
                Log.d(TAG, "fetchEstablishment() Получено DTO | id=${dto.id}, name='${dto.name}', photoBase64s в DTO: ${dto.photoBase64s.size}")
                initWithData(dto)

                Log.d(TAG, "fetchEstablishment() Загрузка фото отдельно...")
                val photos = withContext(Dispatchers.IO) { apiService.getPhotos(id) }
                Log.d(TAG, "fetchEstablishment() УСПЕШНО загружено фото с сервера: ${photos.size} шт.")
                photos.forEachIndexed { i, s ->
                    val preview = if (s.length > 100) s.take(100) + "..." else s
                    Log.d(TAG, "fetchEstablishment() Фото $i | длина=${s.length} | preview: $preview")
                }

                _editedPhotos.value = photos.map { PhotoItem.Remote(it) }
                Log.d(TAG, "fetchEstablishment() Фото установлены в StateFlow: ${_editedPhotos.value.size} шт.")

            } catch (e: Exception) {
                Log.e(TAG, "fetchEstablishment() EXCEPTION | ${e.message}", e)
                _errorMessage.value = e.message ?: "Ошибка загрузки"
            } finally {
                _isLoading.value = false
                Log.d(TAG, "fetchEstablishment() END")
            }
        }
    }
}