package com.example.roamly

import com.example.roamly.classes.cl_menu.Drink
import com.example.roamly.classes.cl_menu.DrinksGroup
import com.example.roamly.classes.cl_menu.Food
import com.example.roamly.classes.cl_menu.FoodGroup
import com.example.roamly.classes.cl_menu.MenuOfEstablishment
import com.example.roamly.entity.CreateOrderRequest
import com.example.roamly.entity.DTO.OrderNotificationDto
import com.example.roamly.entity.DTO.booking.BookingCreationDto
import com.example.roamly.entity.DTO.establishment.EstablishmentDisplayDto
import com.example.roamly.entity.DTO.establishment.EstablishmentFavoriteDto
import com.example.roamly.entity.DTO.establishment.EstablishmentMarkerDto
import com.example.roamly.entity.DTO.establishment.EstablishmentSearchResultDto
import com.example.roamly.entity.DTO.establishment.EstablishmentUpdateRequest
import com.example.roamly.entity.DTO.forDispalyEstablishmentDetails.MapDTO
import com.example.roamly.entity.DTO.TableCreationDto
import com.example.roamly.entity.DTO.booking.BookingDisplayDto
import com.example.roamly.entity.DTO.booking.OwnerBookingDisplayDto
import com.example.roamly.entity.DTO.forDispalyEstablishmentDetails.DescriptionDTO
import com.example.roamly.entity.DeliveryAddressDto
import com.example.roamly.entity.OrderDto
import com.example.roamly.entity.UpdateOrderStatusRequest
import com.example.roamly.entity.classes.EstablishmentEntity
import com.example.roamly.entity.classes.ReviewEntity
import com.example.roamly.entity.classes.TableEntity
import com.example.roamly.entity.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

      // ======================================= //
     // ===== Все точки для пользователей ===== //
    // ======================================= //
    @POST("auth/register")
    suspend fun createUser(@Body user: User): Long

    @POST("auth/login")
    suspend fun loginUser(@Body user: User): User?

    @GET("users/{userId}/favorites/list")
    suspend fun getFavoriteEstablishmentsList(
        @Path("userId") userId: Long
    ): List<EstablishmentFavoriteDto>

    @POST("users/{userId}/favorites/{establishmentId}")
    suspend fun addFavoriteEstablishment(
        @Path("userId") userId: Long,
        @Path("establishmentId") establishmentId: Long
    ): Response<Unit>

    @DELETE("users/{userId}/favorites/{establishmentId}")
    suspend fun removeFavoriteEstablishment(
        @Path("userId") userId: Long,
        @Path("establishmentId") establishmentId: Long
    ): Response<Unit>

      // =================================== //
     // ===== Все точки для заведений ===== //
    // =================================== //
    @POST("establishments/create")
    suspend fun createEstablishment(@Body establishment: EstablishmentEntity): EstablishmentEntity?

    @GET("establishments/user/{userId}") // Пример конечной точки
    suspend fun getEstablishmentsByUserId(@Path("userId") userId: Long): List<EstablishmentDisplayDto>

    @GET("establishments/getAll")
    suspend fun getAllEstablishments(): List<EstablishmentDisplayDto>

    @GET("establishments/markers")
    suspend fun getAllEstablishmentMarkers(): List<EstablishmentMarkerDto>

    @GET("establishments/search")
    suspend fun searchEstablishments(
        @Query("query") query: String?,
        @Query("types") types: List<String>?
    ): List<EstablishmentSearchResultDto>

    // New endpoints for separate loading
    @GET("establishments/{id}/description")
    suspend fun getDescription(@Path("id") id: Long): DescriptionDTO

    @GET("establishments/{id}/map")
    suspend fun getMapData(@Path("id") id: Long): MapDTO

    @GET("establishments/{id}/photos")
    suspend fun getPhotos(@Path("id") id: Long): List<String>

    @GET("users/{userId}/favorites/check/{establishmentId}")
    suspend fun checkFavorite(
        @Path("userId") userId: Long,
        @Path("establishmentId") establishmentId: Long
    ): Boolean

    @GET("establishments/pending")
    suspend fun getPendingEstablishments(): List<EstablishmentDisplayDto>

    @PUT("establishments/{id}")
    suspend fun updateEstablishmentStatus(
        @Path("id") id: Long,
        @Query("status") status: String
    ): EstablishmentDisplayDto

    @GET("establishments/{id}")
    suspend fun getEstablishmentById(
        @Path("id") id: Long,
    ): EstablishmentDisplayDto

    @PUT("establishments/{id}")
    suspend fun updateEstablishment(
        @Path("id") id: Long,
        @Body request: EstablishmentUpdateRequest
    ): Response<EstablishmentEntity>

    @GET("establishments/{id}/photos")
    suspend fun getEstablishmentPhotos(
        @Path("id") id: Long
    ): List<String>

      // ================================== //
     // ===== Все точки для столиков ===== //
    // ================================== //

    @POST("tables/establishment/{establishmentId}/create")
    suspend fun createTables(
        @Path("establishmentId") establishmentId: Long,
        @Body tables: List<TableCreationDto>
    ): Response<List<TableEntity>>

    @GET("tables/establishment/{establishmentId}")
    suspend fun getTablesByEstablishmentId(@Path("establishmentId") establishmentId: Long): List<TableEntity>

    // =============================== //
    // ===== Все точки для брони ===== //
    // =============================== //

    @GET("bookings/{establishmentId}/available")
    suspend fun getAvailableTables(
        @Path("establishmentId") establishmentId: Long,
        @Query("dateTime") dateTime: String
    ): List<TableEntity>

    @POST("bookings")
    suspend fun createBooking(
        @Body booking: BookingCreationDto
    ): Response<Unit>  // Изменено на Response<Unit>, т.к. сервер возвращает 201 Created без тела (или с BookingEntity, но для простоты)

    @GET("bookings/user/{userId}")
    suspend fun getUserBookings(
        @Path("userId") userId: Long
    ): List<BookingDisplayDto>

    @DELETE("bookings/{bookingId}")
    suspend fun cancelBooking(@Path("bookingId") bookingId: Long): Response<Unit>

      // ================================ //
     // ===== Все точки для тестов ===== //
    // ================================ //
    @GET("test/ping")
    suspend fun pingServer(): String

      // ================================= //
     // ===== Все точки для отзывов ===== //
    // ================================= //
    @POST("reviews/create")
    suspend fun createReview(@Body review: ReviewEntity): ReviewEntity

    @GET("reviews/establishment/{establishmentId}") // Соответствует Spring @GetMapping("/establishment/{establishmentId}")
    suspend fun getReviewsByEstablishmentId(@Path("establishmentId") id: Long): List<ReviewEntity>

      // =================================== //
     // ===== Все точки для меню (NEW)===== //
    // =================================== //

    // --- Получение всего меню по ID заведения ---
    @GET("menu/establishment/{establishmentId}")
    suspend fun getMenuByEstablishmentId(@Path("establishmentId") establishmentId: Long): MenuOfEstablishment

    @GET("menu/establishment/{establishmentId}")
    suspend fun getMenuForEstablishment(@Path("establishmentId") establishmentId: Long): MenuOfEstablishment

    @POST("menu/processChanges")
    suspend fun processMenuChanges(@Body menu: MenuOfEstablishment): MenuOfEstablishment

    // --- Группы (FoodGroup / DrinksGroup) ---

    // Добавление новой группы еды/напитков
    @POST("menu/group/food")
    suspend fun createFoodGroup(@Body group: FoodGroup): FoodGroup

    @POST("menu/drink/group") // Путь согласован
    suspend fun createDrinksGroup(@Body group: DrinksGroup): DrinksGroup

    // Обновление группы
    @PUT("menu/group/food/{groupId}")
    suspend fun updateFoodGroup(@Path("groupId") groupId: Long, @Body group: FoodGroup): FoodGroup

    @PUT("menu/drink/group/{groupId}") // Путь согласован. УБРАНО ДУБЛИРОВАНИЕ!
    suspend fun updateDrinksGroup(@Path("groupId") groupId: Long, @Body group: DrinksGroup): DrinksGroup

    // Удаление группы (и всех ее компонентов)
    @DELETE("menu/group/{groupId}")
    suspend fun deleteGroup(@Path("groupId") groupId: Long, @Query("isFood") isFood: Boolean): Response<Unit>
    // --- Компоненты (Food / Drink) ---

    // Создание блюда/напитка
    @POST("menu/item/food")
    suspend fun createFood(@Body food: Food): Food

    @POST("menu/item/drink")
    suspend fun createDrink(@Body drink: Drink): Drink // Drink включает DrinkOption

    // Обновление блюда/напитка
    @PUT("menu/item/food/{itemId}")
    suspend fun updateFood(@Path("itemId") itemId: Long, @Body food: Food): Food

    @PUT("menu/item/drink/{itemId}")
    suspend fun updateDrink(@Path("itemId") itemId: Long, @Body drink: Drink): Drink

    // Удаление блюда/напитка
    @DELETE("menu/item/{itemId}")
    suspend fun deleteItem(@Path("itemId") itemId: Long, @Query("isFood") isFood: Boolean): Response<Unit>




    @GET("bookings/owner/{ownerId}/pending")
    suspend fun getPendingBookingsForOwner(@Path("ownerId") ownerId: Long): List<OwnerBookingDisplayDto>

    @PUT("bookings/{bookingId}/status")
    suspend fun updateBookingStatus(
        @Path("bookingId") bookingId: Long,
        @Query("status") status: String,
        @Query("ownerId") ownerId: Long
    ): Response<Unit>

    // Добавим в ApiService.kt
    @GET("bookings/owner/{ownerId}/approved")
    suspend fun getApprovedBookingsForOwner(
        @Path("ownerId") ownerId: Long,
        @Query("establishmentId") establishmentId: Long? = null
    ): List<OwnerBookingDisplayDto>

    // В ApiService.kt добавьте:
    @GET("bookings/{bookingId}/details")
    suspend fun getBookingDetails(@Path("bookingId") bookingId: Long): OwnerBookingDisplayDto


    // Добавить в ApiService.kt
    @POST("orders/create")
    suspend fun createOrder(@Body request: CreateOrderRequest): OrderDto

    @GET("orders/user/{userId}")
    suspend fun getUserOrders(@Path("userId") userId: Long): List<OrderDto>

    @GET("orders/establishment/{establishmentId}")
    suspend fun getEstablishmentOrders(
        @Path("establishmentId") establishmentId: Long,
        @Query("status") status: String? = null
    ): List<OrderDto>

    @PUT("orders/{orderId}/status")
    suspend fun updateOrderStatus(
        @Path("orderId") orderId: Long,
        @Body request: UpdateOrderStatusRequest
    ): OrderDto

    data class OrderNotification(
        val orderId: Long,
        val userId: Long,
        val establishmentId: Long,
        val notificationType: String, // "ORDER_CREATED", "ORDER_STATUS_CHANGED"
        val message: String
    )

    // Добавьте эти методы в существующий ApiService.kt

    @GET("users/{userId}/delivery-addresses")
    suspend fun getUserDeliveryAddresses(@Path("userId") userId: Long): List<DeliveryAddressDto>

    @POST("users/{userId}/delivery-addresses")
    suspend fun createDeliveryAddress(@Path("userId") userId: Long, @Body address: DeliveryAddressDto): DeliveryAddressDto

    @DELETE("users/{userId}/delivery-addresses/{addressId}")
    suspend fun deleteDeliveryAddress(
        @Path("userId") userId: Long,
        @Path("addressId") addressId: Long
    ): Response<Unit>

    @PUT("users/{userId}/delivery-addresses/{addressId}")
    suspend fun updateDeliveryAddress(
        @Path("userId") userId: Long,
        @Path("addressId") addressId: Long,
        @Body address: DeliveryAddressDto
    ): DeliveryAddressDto

    @PUT("users/{userId}/delivery-addresses/{addressId}/set-default")
    suspend fun setDefaultDeliveryAddress(
        @Path("userId") userId: Long,
        @Path("addressId") addressId: Long
    ): Response<Unit>

    // Эндпоинты для уведомлений о заказах
    @POST("notifications/order")
    suspend fun sendOrderNotification(@Body notification: OrderNotification): Response<Unit>

    @GET("notifications/order/user/{userId}")
    suspend fun getOrderNotifications(@Path("userId") userId: Long): List<OrderNotificationDto>
}