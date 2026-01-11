package com.example.roamly.entity

import java.time.LocalDateTime

// Модель заказа
data class OrderDto(
    val id: Long? = null,
    val establishmentId: Long,
    val userId: Long,
    val status: OrderStatus,
    val deliveryAddressId: Long? = null,
    val deliveryAddress: DeliveryAddressDto? = null,
    val items: List<OrderItemDto>,
    val isContactless: Boolean = false,
    val paymentMethod: PaymentMethod,
    val deliveryTime: String, // ISO format
    val comments: String? = null,
    val totalPrice: Double,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val rejectionReason: String? = null
)

// Позиция в заказе
data class OrderItemDto(
    val id: Long? = null,
    val orderId: Long? = null,
    val menuItemId: Long,
    val menuItemName: String,
    val menuItemType: MenuItemType, // FOOD или DRINK
    val quantity: Int,
    val pricePerUnit: Double,
    val totalPrice: Double,
    val options: String? = null // JSON с выбранными опциями (для напитков)
)

// Адрес доставки
data class DeliveryAddressDto(
    val id: Long? = null,
    val userId: Long,
    val street: String,
    val house: String,
    val building: String? = null,
    val apartment: String,
    val entrance: String? = null,
    val floor: String? = null,
    val comment: String? = null,
    val isDefault: Boolean = false
)

// Создание заказа
data class CreateOrderRequest(
    val establishmentId: Long,
    val deliveryAddressId: Long? = null,
    val deliveryAddress: DeliveryAddressDto? = null,
    val items: List<CreateOrderItem>,
    val isContactless: Boolean = false,
    val paymentMethod: PaymentMethod,
    val deliveryTime: String, // ISO format
    val comments: String? = null
)

// Создание позиции заказа
data class CreateOrderItem(
    val menuItemId: Long,
    val menuItemType: MenuItemType,
    var quantity: Int,
    val selectedOptions: Map<String, String>? = null // для напитков
)

// Обновление статуса заказа
data class UpdateOrderStatusRequest(
    val status: OrderStatus,
    val rejectionReason: String? = null
)

// Перечисления
enum class OrderStatus {
    PENDING,      // На рассмотрении
    CONFIRMED,    // Подтвержден
    IN_PROGRESS,  // В процессе приготовления
    OUT_FOR_DELIVERY, // В доставке
    DELIVERED,    // Доставлен
    CANCELLED,    // Отменен
    REJECTED      // Отклонен заведением
}

enum class PaymentMethod {
    CASH,         // Наличными
    CARD,         // Картой курьеру
    ONLINE        // Онлайн оплата
}

enum class MenuItemType {
    FOOD,
    DRINK
}

fun OrderStatus.toDisplayString(): String {
    return when (this) {
        OrderStatus.PENDING -> "На рассмотрении"
        OrderStatus.CONFIRMED -> "Подтвержден"
        OrderStatus.IN_PROGRESS -> "Готовится"
        OrderStatus.OUT_FOR_DELIVERY -> "В доставке"
        OrderStatus.DELIVERED -> "Доставлен"
        OrderStatus.CANCELLED -> "Отменен"
        OrderStatus.REJECTED -> "Отклонен"
    }
}