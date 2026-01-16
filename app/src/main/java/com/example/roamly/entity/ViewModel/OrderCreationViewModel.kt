package com.example.roamly.entity.ViewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roamly.ApiService
import com.example.roamly.classes.cl_menu.MenuOfEstablishment
import com.example.roamly.entity.DTO.order.CreateOrderItem
import com.example.roamly.entity.DTO.order.MenuItemType
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

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun loadMenu(establishmentId: Long) {
        Log.d("OrderCreationViewModel", "loadMenu вызван для заведения $establishmentId")
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val menuData = withContext(Dispatchers.IO) {
                    apiService.getMenuByEstablishmentId(establishmentId)
                }
                _menu.value = menuData
                Log.d("OrderCreationViewModel", "Меню загружено: ${menuData.foodGroups?.size} групп еды, ${menuData.drinksGroups?.size} групп напитков")
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки меню: ${e.message}"
                Log.e("OrderCreationViewModel", "Ошибка загрузки меню", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addToCart(item: CreateOrderItem) {
        Log.d("OrderCreationViewModel", "Добавление в корзину: ${item.menuItemType} ID=${item.menuItemId}, количество=${item.quantity}")
        val currentItems = _cartItems.value.toMutableList()
        val existingItemIndex = currentItems.indexOfFirst {
            it.menuItemId == item.menuItemId &&
                    it.menuItemType == item.menuItemType &&
                    it.selectedOptions == item.selectedOptions
        }

        if (existingItemIndex != -1) {
            // Обновляем существующий элемент
            val existingItem = currentItems[existingItemIndex]
            currentItems[existingItemIndex] = existingItem.copy(
                quantity = existingItem.quantity + item.quantity
            )
            Log.d("OrderCreationViewModel", "Обновлен существующий элемент, новое количество: ${existingItem.quantity + item.quantity}")
        } else {
            // Добавляем новый элемент
            currentItems.add(item)
            Log.d("OrderCreationViewModel", "Добавлен новый элемент")
        }

        _cartItems.value = currentItems
        _successMessage.value = "${getItemName(item)} добавлен в корзину"
        Log.d("OrderCreationViewModel", "В корзине теперь ${currentItems.size} позиций")
    }

    fun removeFromCart(item: CreateOrderItem) {
        Log.d("OrderCreationViewModel", "Удаление из корзины: ${item.menuItemType} ID=${item.menuItemId}, количество=${item.quantity}")
        val currentItems = _cartItems.value.toMutableList()
        val existingItemIndex = currentItems.indexOfFirst {
            it.menuItemId == item.menuItemId &&
                    it.menuItemType == item.menuItemType &&
                    it.selectedOptions == item.selectedOptions
        }

        if (existingItemIndex != -1) {
            val existingItem = currentItems[existingItemIndex]
            val newQuantity = existingItem.quantity - item.quantity

            if (newQuantity <= 0) {
                // Полностью удаляем элемент
                currentItems.removeAt(existingItemIndex)
                _successMessage.value = "${getItemName(item)} удален из корзины"
                Log.d("OrderCreationViewModel", "Элемент полностью удален")
            } else {
                // Уменьшаем количество
                currentItems[existingItemIndex] = existingItem.copy(
                    quantity = newQuantity
                )
                _successMessage.value = "Количество ${getItemName(item)} уменьшено"
                Log.d("OrderCreationViewModel", "Количество уменьшено до $newQuantity")
            }

            _cartItems.value = currentItems
            Log.d("OrderCreationViewModel", "В корзине теперь ${currentItems.size} позиций")
        }
    }

    // Метод для полного удаления элемента из корзины
    fun removeCartItem(item: CreateOrderItem) {
        val currentItems = _cartItems.value.toMutableList()
        val existingItemIndex = currentItems.indexOfFirst {
            it.menuItemId == item.menuItemId &&
                    it.menuItemType == item.menuItemType &&
                    it.selectedOptions == item.selectedOptions
        }

        if (existingItemIndex != -1) {
            currentItems.removeAt(existingItemIndex)
            _cartItems.value = currentItems
            _successMessage.value = "${getItemName(item)} удален из корзины"
        }
    }

    // Метод для удаления элемента по ID и типу
    fun removeCartItemById(menuItemId: Long, menuItemType: MenuItemType) {
        val currentItems = _cartItems.value.toMutableList()
        val itemsToRemove = currentItems.filter {
            it.menuItemId == menuItemId && it.menuItemType == menuItemType
        }

        if (itemsToRemove.isNotEmpty()) {
            currentItems.removeAll(itemsToRemove)
            _cartItems.value = currentItems
            _successMessage.value = "Позиция удалена из корзины"
        }
    }

    // Метод для обновления количества элемента
    fun updateCartItemQuantity(item: CreateOrderItem, newQuantity: Int) {
        val currentItems = _cartItems.value.toMutableList()
        val existingItemIndex = currentItems.indexOfFirst {
            it.menuItemId == item.menuItemId &&
                    it.menuItemType == item.menuItemType &&
                    it.selectedOptions == item.selectedOptions
        }

        if (existingItemIndex != -1) {
            if (newQuantity <= 0) {
                // Удаляем элемент, если количество 0 или меньше
                currentItems.removeAt(existingItemIndex)
                _successMessage.value = "${getItemName(item)} удален из корзины"
            } else {
                // Обновляем количество
                val existingItem = currentItems[existingItemIndex]
                currentItems[existingItemIndex] = existingItem.copy(
                    quantity = newQuantity
                )
                _successMessage.value = "Количество ${getItemName(item)} обновлено"
            }

            _cartItems.value = currentItems
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _successMessage.value = "Корзина очищена"
    }

    // Метод для получения общего количества позиций в корзине
    fun getTotalItemCount(): Int {
        return _cartItems.value.sumOf { it.quantity }
    }

    fun calculateTotal(): Double {
        val menu = _menu.value ?: return 0.0
        var total = 0.0

        _cartItems.value.forEach { item ->
            when (item.menuItemType) {
                MenuItemType.FOOD -> {
                    menu.foodGroups.forEach { group ->
                        group.items.find { it.id == item.menuItemId }?.let { food ->
                            total += food.cost * item.quantity
                        }
                    }
                }
                MenuItemType.DRINK -> {
                    menu.drinksGroups.forEach { group ->
                        group.items.find { it.id == item.menuItemId }?.let { drink ->
                            // Получаем выбранную цену из selectedOptions
                            val price = item.selectedOptions?.get("price")?.toDoubleOrNull()
                                ?: drink.options.firstOrNull()?.cost ?: 0.0
                            total += price * item.quantity
                        }
                    }
                }
            }
        }

        Log.d("OrderCreationViewModel", "calculateTotal: ${_cartItems.value.size} позиций, итого: $total")
        return total
    }

    fun setSelectedFoodGroup(group: String?) {
        _selectedFoodGroup.value = group
    }

    fun setSelectedDrinkGroup(group: String?) {
        _selectedDrinkGroup.value = group
    }

    // Вспомогательный метод для получения названия элемента
    private fun getItemName(item: CreateOrderItem): String {
        val menu = _menu.value ?: return "Позиция"

        return when (item.menuItemType) {
            MenuItemType.FOOD -> {
                menu.foodGroups.flatMap { it.items }
                    .find { it.id == item.menuItemId }?.name ?: "Блюдо"
            }
            MenuItemType.DRINK -> {
                val drink = menu.drinksGroups.flatMap { it.items }
                    .find { it.id == item.menuItemId }
                val size = item.selectedOptions?.get("size")?.let { " (${it} мл)" } ?: ""
                (drink?.name ?: "Напиток") + size
            }
        }
    }

    // Метод для получения деталей элемента из корзины
    fun getCartItemDetails(item: CreateOrderItem): Pair<String, Double> {
        val menu = _menu.value ?: return ("Неизвестная позиция" to 0.0)

        return when (item.menuItemType) {
            MenuItemType.FOOD -> {
                val food = menu.foodGroups.flatMap { it.items }
                    .find { it.id == item.menuItemId }
                (food?.name ?: "Блюдо") to (food?.cost ?: 0.0)
            }
            MenuItemType.DRINK -> {
                val drink = menu.drinksGroups.flatMap { it.items }
                    .find { it.id == item.menuItemId }
                val price = item.selectedOptions?.get("price")?.toDoubleOrNull()
                    ?: drink?.options?.firstOrNull()?.cost ?: 0.0
                val size = item.selectedOptions?.get("size")?.let { " (${it} мл)" } ?: ""
                ((drink?.name ?: "Напиток") + size) to price
            }
        }
    }

    // Метод для проверки, есть ли элемент в корзине
    fun isItemInCart(menuItemId: Long, menuItemType: MenuItemType, selectedOptions: Map<String, String>? = null): Boolean {
        return _cartItems.value.any {
            it.menuItemId == menuItemId &&
                    it.menuItemType == menuItemType &&
                    it.selectedOptions == selectedOptions
        }
    }

    // Метод для получения количества конкретного элемента
    fun getItemQuantity(menuItemId: Long, menuItemType: MenuItemType, selectedOptions: Map<String, String>? = null): Int {
        return _cartItems.value
            .find {
                it.menuItemId == menuItemId &&
                        it.menuItemType == menuItemType &&
                        it.selectedOptions == selectedOptions
            }?.quantity ?: 0
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }
}