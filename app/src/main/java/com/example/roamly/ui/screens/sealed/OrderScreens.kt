// OrderScreens.kt
package com.example.roamly.ui.screens.sealed

sealed class OrderScreens(val route: String) {
    companion object {
        const val ESTABLISHMENT_ID_KEY = "establishmentId"
        const val ORDER_ID_KEY = "orderId"
        const val USER_ID_KEY = "userId"
        const val ADDRESS_ID_KEY = "addressId"
    }

    object OrderCreation : OrderScreens("order/create/{$ESTABLISHMENT_ID_KEY}") {
        fun createRoute(establishmentId: Long): String = "order/create/$establishmentId"
    }

    object OrderCheckout : OrderScreens("order/checkout/{$ESTABLISHMENT_ID_KEY}") {
        fun createRoute(establishmentId: Long): String = "order/checkout/$establishmentId"
    }

    object OrderDetails : OrderScreens("order/details/{$ORDER_ID_KEY}") {
        fun createRoute(orderId: Long): String = "order/details/$orderId"
    }

    object OrderList : OrderScreens("orders/list")

    object DeliveryAddresses : OrderScreens("orders/delivery-addresses/{$USER_ID_KEY}?isSelectionMode={isSelectionMode}") {
        fun createRoute(userId: Long, isSelectionMode: Boolean ): String =
            "orders/delivery-addresses/$userId?isSelectionMode=$isSelectionMode"
    }

    object CreateDeliveryAddress : OrderScreens("order/create-address/{$USER_ID_KEY}") {
        fun createRoute(userId: Long): String = "order/create-address/$userId"
    }

    object EditDeliveryAddress : OrderScreens("order/edit-address/{$USER_ID_KEY}/{$ADDRESS_ID_KEY}") {
        fun createRoute(userId: Long, addressId: Long): String = "order/edit-address/$userId/$addressId"
    }
}