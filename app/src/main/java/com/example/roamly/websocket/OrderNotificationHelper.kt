package com.example.roamly.websocket

import android.content.Context
import com.example.roamly.entity.*
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class OrderNotificationHelper(private val context: Context) {

    private val notificationHelper = NotificationHelper(context)

    fun handleOrderNotification(json: JSONObject) {
        try {
            val type = json.getString("type")
            val orderJson = json.getJSONObject("order")
            val order = parseOrderFromJson(orderJson)

            when (type) {
                "ORDER_CREATED" -> {
                    showOrderCreatedNotification(order)
                }
                "ORDER_STATUS_CHANGED" -> {
                    showOrderStatusChangedNotification(order)
                }
                "ORDER_CANCELLED" -> {
                    showOrderCancelledNotification(order)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showOrderCreatedNotification(order: OrderDto) {
        notificationHelper.showNotification(
            title = "Новый заказ #${order.id}",
            message = "Заказ создан и ожидает подтверждения",
            notificationId = "order_${order.id}"
        )
    }

    private fun showOrderStatusChangedNotification(order: OrderDto) {
        val statusText = when (order.status) {
            OrderStatus.CONFIRMED -> "подтвержден"
            OrderStatus.IN_PROGRESS -> "готовится"
            OrderStatus.OUT_FOR_DELIVERY -> "в доставке"
            OrderStatus.DELIVERED -> "доставлен"
            OrderStatus.REJECTED -> "отклонен"
            else -> "обновлен"
        }

        notificationHelper.showNotification(
            title = "Заказ #${order.id} $statusText",
            message = order.rejectionReason ?: "Статус вашего заказа изменен",
            notificationId = "order_${order.id}_status"
        )
    }

    private fun showOrderCancelledNotification(order: OrderDto) {
        notificationHelper.showNotification(
            title = "Заказ #${order.id} отменен",
            message = "Ваш заказ был отменен",
            notificationId = "order_${order.id}_cancelled"
        )
    }

    private fun parseOrderFromJson(json: JSONObject): OrderDto {
        // Парсинг обязательных полей
        val id = json.getLong("id")
        val establishmentId = json.getLong("establishmentId")
        val userId = json.getLong("userId")
        val status = OrderStatus.valueOf(json.getString("status"))
        val paymentMethod = PaymentMethod.valueOf(json.getString("paymentMethod"))
        val deliveryTime = json.getString("deliveryTime")
        val totalPrice = json.getDouble("totalPrice")
        val isContactless = json.optBoolean("isContactless", false)

        // Парсинг необязательных полей
        val deliveryAddressId = if (json.has("deliveryAddressId")) {
            json.getLong("deliveryAddressId")
        } else null

        val comments = if (json.has("comments")) {
            json.getString("comments")
        } else null

        val createdAt = if (json.has("createdAt")) {
            json.getString("createdAt")
        } else null

        val updatedAt = if (json.has("updatedAt")) {
            json.getString("updatedAt")
        } else null

        val rejectionReason = if (json.has("rejectionReason")) {
            json.getString("rejectionReason")
        } else null

        // Парсинг списка items
        val items = mutableListOf<OrderItemDto>()
        if (json.has("items")) {
            val itemsArray = json.getJSONArray("items")
            for (i in 0 until itemsArray.length()) {
                val itemJson = itemsArray.getJSONObject(i)
                val item = OrderItemDto(
                    id = if (itemJson.has("id")) itemJson.getLong("id") else null,
                    orderId = if (itemJson.has("orderId")) itemJson.getLong("orderId") else null,
                    menuItemId = itemJson.getLong("menuItemId"),
                    menuItemName = itemJson.getString("menuItemName"),
                    menuItemType = MenuItemType.valueOf(itemJson.getString("menuItemType")),
                    quantity = itemJson.getInt("quantity"),
                    pricePerUnit = itemJson.getDouble("pricePerUnit"),
                    totalPrice = itemJson.getDouble("totalPrice"),
                    options = if (itemJson.has("options")) itemJson.getString("options") else null
                )
                items.add(item)
            }
        }

        // Парсинг deliveryAddress (если есть)
        var deliveryAddress: DeliveryAddressDto? = null
        if (json.has("deliveryAddress")) {
            val addressJson = json.getJSONObject("deliveryAddress")
            deliveryAddress = DeliveryAddressDto(
                id = if (addressJson.has("id")) addressJson.getLong("id") else null,
                userId = addressJson.getLong("userId"),
                street = addressJson.getString("street"),
                house = addressJson.getString("house"),
                building = if (addressJson.has("building")) addressJson.getString("building") else null,
                apartment = addressJson.getString("apartment"),
                entrance = if (addressJson.has("entrance")) addressJson.getString("entrance") else null,
                floor = if (addressJson.has("floor")) addressJson.getString("floor") else null,
                comment = if (addressJson.has("comment")) addressJson.getString("comment") else null,
                isDefault = addressJson.optBoolean("isDefault", false)
            )
        }

        return OrderDto(
            id = id,
            establishmentId = establishmentId,
            userId = userId,
            status = status,
            deliveryAddressId = deliveryAddressId,
            deliveryAddress = deliveryAddress,
            items = items,
            isContactless = isContactless,
            paymentMethod = paymentMethod,
            deliveryTime = deliveryTime,
            comments = comments,
            totalPrice = totalPrice,
            createdAt = createdAt,
            updatedAt = updatedAt,
            rejectionReason = rejectionReason
        )
    }
}