package com.example.roamly.entity.ViewModel

import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roamly.ApiService
import com.example.roamly.classes.cl_menu.MenuOfEstablishment
import com.example.roamly.entity.DTO.establishment.EstablishmentDisplayDto
import com.example.roamly.entity.DTO.forDispalyEstablishmentDetails.DescriptionDTO
import com.example.roamly.entity.DTO.forDispalyEstablishmentDetails.MapDTO
import com.example.roamly.entity.LoadState
import com.example.roamly.entity.ReviewEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class EstablishmentDetailViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _descriptionState = MutableStateFlow<LoadState<DescriptionDTO>>(LoadState.Loading)
    val descriptionState: StateFlow<LoadState<DescriptionDTO>> = _descriptionState.asStateFlow()

    private val _reviewsState = MutableStateFlow<LoadState<List<ReviewEntity>>>(LoadState.Loading)
    val reviewsState: StateFlow<LoadState<List<ReviewEntity>>> = _reviewsState.asStateFlow()

    private val _mapState = MutableStateFlow<LoadState<MapDTO>>(LoadState.Loading)
    val mapState: StateFlow<LoadState<MapDTO>> = _mapState.asStateFlow()

    private val _menuState = MutableStateFlow<LoadState<MenuOfEstablishment>>(LoadState.Loading)
    val menuState: StateFlow<LoadState<MenuOfEstablishment>> = _menuState.asStateFlow()

    private val _photosState = MutableStateFlow<LoadState<List<ByteArray>>>(LoadState.Loading)
    val photosState: StateFlow<LoadState<List<ByteArray>>> = _photosState.asStateFlow()

    private val _favoriteState = MutableStateFlow<LoadState<Boolean>>(LoadState.Loading)
    val favoriteState: StateFlow<LoadState<Boolean>> = _favoriteState.asStateFlow()

    private val _establishmentState = MutableStateFlow<LoadState<EstablishmentDisplayDto>>(LoadState.Loading)
    val establishmentState: StateFlow<LoadState<EstablishmentDisplayDto>> = _establishmentState.asStateFlow()

    fun fetchAllDetails(establishmentId: Long, userId: Long? = null) {
        viewModelScope.launch {
            coroutineScope {
                async { fetchDescription(establishmentId) }
                async { fetchReviews(establishmentId) }
                async { fetchMap(establishmentId) }
                async { fetchMenu(establishmentId) }
                async { fetchPhotos(establishmentId) }
                async { fetchEstablishment(establishmentId) }
                if (userId != null) {
                    async { fetchFavorite(establishmentId, userId) }
                }
            }
        }
    }

    private suspend fun fetchDescription(establishmentId: Long) {
        Log.d("EstablishmentDetailViewModel", "Starting fetchDescription for ID: $establishmentId")
        try {
            val description = withContext(Dispatchers.IO) {
                apiService.getDescription(establishmentId)
            }
            _descriptionState.value = LoadState.Success(description)
            Log.d("EstablishmentDetailViewModel", "Finished fetchDescription successfully")
        } catch (e: Exception) {
            _descriptionState.value = LoadState.Error(e.message ?: "Unknown error")
            Log.e("EstablishmentDetailViewModel", "Finished fetchDescription with error: ${e.message}", e)
        }
    }

    private suspend fun fetchReviews(establishmentId: Long) {
        Log.d("EstablishmentDetailViewModel", "Starting fetchReviews for ID: $establishmentId")
        try {
            val reviews = withContext(Dispatchers.IO) {
                apiService.getReviewsByEstablishmentId(establishmentId)
            }
            _reviewsState.value = LoadState.Success(reviews)
            Log.d("EstablishmentDetailViewModel", "Finished fetchReviews successfully")
        } catch (e: Exception) {
            _reviewsState.value = LoadState.Error(e.message ?: "Unknown error")
            Log.e("EstablishmentDetailViewModel", "Finished fetchReviews with error: ${e.message}", e)
        }
    }

    private suspend fun fetchMap(establishmentId: Long) {
        Log.d("EstablishmentDetailViewModel", "Starting fetchMap for ID: $establishmentId")
        try {
            val mapData = withContext(Dispatchers.IO) {
                apiService.getMapData(establishmentId)
            }
            _mapState.value = LoadState.Success(mapData)
            Log.d("EstablishmentDetailViewModel", "Finished fetchMap successfully")
        } catch (e: Exception) {
            _mapState.value = LoadState.Error(e.message ?: "Unknown error")
            Log.e("EstablishmentDetailViewModel", "Finished fetchMap with error: ${e.message}", e)
        }
    }

    private suspend fun fetchMenu(establishmentId: Long) {
        Log.d("EstablishmentDetailViewModel", "Starting fetchMenu for ID: $establishmentId")
        try {
            val menu = withContext(Dispatchers.IO) {
                apiService.getMenuByEstablishmentId(establishmentId)
            }
            _menuState.value = LoadState.Success(menu)
            Log.d("EstablishmentDetailViewModel", "Finished fetchMenu successfully")
        } catch (e: Exception) {
            _menuState.value = LoadState.Error(e.message ?: "Unknown error")
            Log.e("EstablishmentDetailViewModel", "Finished fetchMenu with error: ${e.message}", e)
        }
    }

    private suspend fun fetchPhotos(establishmentId: Long) {
        Log.d("EstablishmentDetailViewModel", "Starting fetchPhotos for ID: $establishmentId")
        try {
            val photosBase64 = withContext(Dispatchers.IO) {
                apiService.getPhotos(establishmentId)
            }
            val preloadedPhotos = photosBase64.map { base64 ->
                Base64.decode(base64, Base64.DEFAULT)
            }
            _photosState.value = LoadState.Success(preloadedPhotos)
            Log.d("EstablishmentDetailViewModel", "Finished fetchPhotos successfully")
        } catch (e: Exception) {
            _photosState.value = LoadState.Error(e.message ?: "Unknown error")
            Log.e("EstablishmentDetailViewModel", "Finished fetchPhotos with error: ${e.message}", e)
        }
    }

    private suspend fun fetchFavorite(establishmentId: Long, userId: Long) {
        Log.d("EstablishmentDetailViewModel", "Starting fetchFavorite for ID: $establishmentId, user: $userId")
        try {
            val isFavorite = withContext(Dispatchers.IO) {
                apiService.checkFavorite(userId, establishmentId)
            }
            _favoriteState.value = LoadState.Success(isFavorite)
            Log.d("EstablishmentDetailViewModel", "Finished fetchFavorite successfully")
        } catch (e: Exception) {
            _favoriteState.value = LoadState.Error(e.message ?: "Unknown error")
            Log.e("EstablishmentDetailViewModel", "Finished fetchFavorite with error: ${e.message}", e)
        }
    }

    private suspend fun fetchEstablishment(establishmentId: Long) {
        Log.d("EstablishmentDetailViewModel", "Starting fetchEstablishment for ID: $establishmentId")
        try {
            val establishment = withContext(Dispatchers.IO) {
                apiService.getEstablishmentById(establishmentId)
            }
            _establishmentState.value = LoadState.Success(establishment)
            Log.d("EstablishmentDetailViewModel", "Finished fetchEstablishment successfully")
        } catch (e: Exception) {
            _establishmentState.value = LoadState.Error(e.message ?: "Unknown error")
            Log.e("EstablishmentDetailViewModel", "Finished fetchEstablishment with error: ${e.message}", e)
        }
    }

    // Retry methods
    fun retryDescription(establishmentId: Long) = viewModelScope.launch { fetchDescription(establishmentId) }
    fun retryReviews(establishmentId: Long) = viewModelScope.launch { fetchReviews(establishmentId) }
    fun retryMap(establishmentId: Long) = viewModelScope.launch { fetchMap(establishmentId) }
    fun retryMenu(establishmentId: Long) = viewModelScope.launch { fetchMenu(establishmentId) }
    fun retryPhotos(establishmentId: Long) = viewModelScope.launch { fetchPhotos(establishmentId) }
    fun retryFavorite(establishmentId: Long, userId: Long) = viewModelScope.launch { fetchFavorite(establishmentId, userId) }
    fun retryEstablishment(establishmentId: Long) = viewModelScope.launch { fetchEstablishment(establishmentId) }

    // Additional methods for favorite toggling (from original)
    fun addFavoriteEstablishment(userId: Long, establishmentId: Long) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    apiService.addFavoriteEstablishment(userId, establishmentId)
                }
                // Update state after success
                _favoriteState.value = LoadState.Success(true)
            } catch (e: Exception) {
                Log.e("EstablishmentDetailViewModel", "Error adding favorite: ${e.message}", e)
            }
        }
    }



    fun removeFavoriteEstablishment(userId: Long, establishmentId: Long) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    apiService.removeFavoriteEstablishment(userId, establishmentId)
                }
                // Update state after success
                _favoriteState.value = LoadState.Success(false)
            } catch (e: Exception) {
                Log.e("EstablishmentDetailViewModel", "Error removing favorite: ${e.message}", e)
            }
        }
    }
}