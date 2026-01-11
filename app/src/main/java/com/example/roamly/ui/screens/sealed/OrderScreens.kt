// OrderScreens.kt
package com.example.roamly.ui.screens.sealed

sealed class OrderScreens(val route: String) {
    companion object {
        const val ESTABLISHMENT_ID_KEY = "establishmentId"
        const val ORDER_ID_KEY = "orderId"
    }

    object OrderCreation : OrderScreens("order/create/{$ESTABLISHMENT_ID_KEY}") {
        fun createRoute(establishmentId: Long): String = "order/create/$establishmentId"
    }

    object OrderDetails : OrderScreens("order/details/{$ORDER_ID_KEY}") {
        fun createRoute(orderId: Long): String = "order/details/$orderId"
    }

    object OrderList : OrderScreens("orders/list")

    object DeliveryAddresses : OrderScreens("orders/delivery-addresses")

    object CreateDeliveryAddress : OrderScreens("orders/create-address")
}