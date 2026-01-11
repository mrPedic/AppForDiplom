package com.example.roamly.entity.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roamly.ApiService
import com.example.roamly.entity.DeliveryAddressDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class DeliveryAddressViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _addresses = MutableStateFlow<List<DeliveryAddressDto>>(emptyList())
    val addresses: StateFlow<List<DeliveryAddressDto>> = _addresses.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // UI состояние для подтверждения удаления
    private val _showDeleteDialog = MutableStateFlow<Long?>(null)
    val showDeleteDialog: StateFlow<Long?> = _showDeleteDialog.asStateFlow()

    // UI состояние для выбора адреса по умолчанию
    private val _showSetDefaultDialog = MutableStateFlow<Long?>(null)
    val showSetDefaultDialog: StateFlow<Long?> = _showSetDefaultDialog.asStateFlow()

    // Успешные операции
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun loadUserAddresses(userId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val addressesList = withContext(Dispatchers.IO) {
                    apiService.getUserDeliveryAddresses(userId)
                }
                _addresses.value = addressesList
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки адресов: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createAddress(userId: Long, address: DeliveryAddressDto, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _successMessage.value = null
            try {
                val newAddress = withContext(Dispatchers.IO) {
                    apiService.createDeliveryAddress(userId, address)
                }
                _addresses.value = _addresses.value + newAddress
                _successMessage.value = "Адрес успешно добавлен"
                onSuccess()
            } catch (e: Exception) {
                _error.value = "Ошибка создания адреса: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateAddress(userId: Long, addressId: Long, address: DeliveryAddressDto, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _successMessage.value = null
            try {
                val updatedAddress = withContext(Dispatchers.IO) {
                    apiService.updateDeliveryAddress(userId, addressId, address)
                }
                _addresses.value = _addresses.value.map {
                    if (it.id == addressId) updatedAddress else it
                }
                _successMessage.value = "Адрес успешно обновлен"
                onSuccess()
            } catch (e: Exception) {
                _error.value = "Ошибка обновления адреса: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Показать диалог подтверждения удаления
    fun showDeleteConfirmation(addressId: Long) {
        _showDeleteDialog.value = addressId
    }

    // Скрыть диалог удаления
    fun hideDeleteConfirmation() {
        _showDeleteDialog.value = null
    }

    // Удалить адрес после подтверждения
    fun confirmDeleteAddress(userId: Long, addressId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _successMessage.value = null
            try {
                withContext(Dispatchers.IO) {
                    apiService.deleteDeliveryAddress(userId, addressId)
                }
                _addresses.value = _addresses.value.filter { it.id != addressId }
                _successMessage.value = "Адрес успешно удален"
            } catch (e: Exception) {
                _error.value = "Ошибка удаления адреса: ${e.message}"
            } finally {
                _isLoading.value = false
                _showDeleteDialog.value = null
            }
        }
    }

    // Показать диалог подтверждения установки по умолчанию
    fun showSetDefaultConfirmation(addressId: Long) {
        _showSetDefaultDialog.value = addressId
    }

    // Скрыть диалог установки по умолчанию
    fun hideSetDefaultConfirmation() {
        _showSetDefaultDialog.value = null
    }

    // Установить адрес по умолчанию после подтверждения
    fun confirmSetDefaultAddress(userId: Long, addressId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _successMessage.value = null
            try {
                withContext(Dispatchers.IO) {
                    apiService.setDefaultDeliveryAddress(userId, addressId)
                }
                _addresses.value = _addresses.value.map {
                    it.copy(isDefault = it.id == addressId)
                }
                _successMessage.value = "Адрес установлен по умолчанию"
            } catch (e: Exception) {
                _error.value = "Ошибка установки адреса по умолчанию: ${e.message}"
            } finally {
                _isLoading.value = false
                _showSetDefaultDialog.value = null
            }
        }
    }

    // Старый метод для прямой установки адреса по умолчанию (без подтверждения)
    fun setDefaultAddress(userId: Long, addressId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _successMessage.value = null
            try {
                withContext(Dispatchers.IO) {
                    apiService.setDefaultDeliveryAddress(userId, addressId)
                }
                _addresses.value = _addresses.value.map {
                    it.copy(isDefault = it.id == addressId)
                }
                _successMessage.value = "Адрес установлен по умолчанию"
            } catch (e: Exception) {
                _error.value = "Ошибка установки адреса по умолчанию: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Старый метод для прямого удаления (без подтверждения)
    fun deleteAddress(userId: Long, addressId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _successMessage.value = null
            try {
                withContext(Dispatchers.IO) {
                    apiService.deleteDeliveryAddress(userId, addressId)
                }
                _addresses.value = _addresses.value.filter { it.id != addressId }
                _successMessage.value = "Адрес успешно удален"
            } catch (e: Exception) {
                _error.value = "Ошибка удаления адреса: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }
}