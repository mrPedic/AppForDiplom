package com.example.roamly.entity.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roamly.ApiService
import com.example.roamly.classes.cl_menu.MenuOfEstablishment
import com.example.roamly.entity.CreateOrderItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class OrderCreationViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _menu = MutableStateFlow<MenuOfEstablishment?>(null)
    val menu: StateFlow<MenuOfEstablishment?> = _menu.asStateFlow()

    private val _cartItems = MutableStateFlow<List<CreateOrderItem>>(emptyList())
    val cartItems: StateFlow<List<CreateOrderItem>> = _cartItems.asStateFlow()

    private val _selectedFoodGroup = MutableStateFlow<String?>(null)
    val selectedFoodGroup: StateFlow<String?> = _selectedFoodGroup.asStateFlow()

    private val _selectedDrinkGroup = MutableStateFlow<String?>(null)
    val selectedDrinkGroup: StateFlow<String?> = _selectedDrinkGroup.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadMenu(establishmentId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val menuData = withContext(Dispatchers.IO) {
                    apiService.getMenuByEstablishmentId(establishmentId)
                }
                _menu.value = menuData
            } catch (e: Exception) {
                // Обработка ошибки
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addToCart(item: CreateOrderItem) {
        val currentItems = _cartItems.value.toMutableList()
        val existingItem = currentItems.find { it.menuItemId == item.menuItemId }

        if (existingItem != null) {
            existingItem.quantity += item.quantity
        } else {
            currentItems.add(item)
        }

        _cartItems.value = currentItems
    }

    fun removeFromCart(item: CreateOrderItem) {
        val currentItems = _cartItems.value.toMutableList()
        val existingItem = currentItems.find { it.menuItemId == item.menuItemId }

        if (existingItem != null) {
            existingItem.quantity -= 1
            if (existingItem.quantity <= 0) {
                currentItems.remove(existingItem)
            }
        }

        _cartItems.value = currentItems
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    fun calculateTotal(): Double {
        // TODO: Реализовать расчет суммы на основе цен из меню
        // Это нужно синхронизировать с сервером
        return _cartItems.value.sumOf { 100.0 * it.quantity } // временная заглушка
    }

    fun setSelectedFoodGroup(group: String?) {
        _selectedFoodGroup.value = group
    }

    fun setSelectedDrinkGroup(group: String?) {
        _selectedDrinkGroup.value = group
    }
}