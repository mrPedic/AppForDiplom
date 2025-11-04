package com.example.roamly

import com.example.roamly.entity.BookingCreationDto
import com.example.roamly.entity.BookingEntity
import com.example.roamly.entity.DTO.EstablishmentDisplayDto
import com.example.roamly.entity.DTO.EstablishmentMarkerDto
import com.example.roamly.entity.DTO.TableCreationDto
import com.example.roamly.entity.EstablishmentEntity
import com.example.roamly.entity.EstablishmentStatus
import com.example.roamly.entity.ReviewEntity
import com.example.roamly.entity.TableEntity
import com.example.roamly.entity.User
import retrofit2.Response
import retrofit2.http.Body
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

      // =================================== //
     // ===== Все точки для заведений ===== //
    // =================================== //
    @POST("establishments/create")
    suspend fun createEstablishment(@Body establishment: EstablishmentEntity): EstablishmentEntity?

    @GET("establishments/user/{userId}") // Пример конечной точки
    suspend fun getEstablishmentsByUserId(@Path("userId") userId: Long): List<EstablishmentDisplayDto>

    @GET("establishments/getAll")
    suspend fun getAllEstablishments(): List<EstablishmentDisplayDto>

    // ⭐ НОВАЯ ТОЧКА: Загрузка облегченных данных для карты
    @GET("establishments/markers")
    suspend fun getAllEstablishmentMarkers(): List<EstablishmentMarkerDto>

    @GET("establishments/search")
    suspend fun searchEstablishments(@Query("query") query: String): List<EstablishmentDisplayDto>

    @GET("establishments/pending")
    suspend fun getPendingEstablishments(): List<EstablishmentDisplayDto>

    @PUT("establishments/{id}/status")
    suspend fun updateEstablishmentStatus(
        @Path("id") id: Long,
        @Query("status") status: String
    ): EstablishmentDisplayDto

    @GET("establishments/{id}")
    suspend fun getEstablishmentById(@Path("id") id: Long): EstablishmentDisplayDto

    @PUT("establishments/{id}")
    suspend fun updateEstablishment(
        @Path("id") id: Long,
        @Body establishment: EstablishmentEntity
    ): EstablishmentDisplayDto

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

    @GET("bookings/{establishmentId}/available")
    suspend fun getAvailableTables(
        @Path("establishmentId") establishmentId: Long,
        @Query("dateTime") dateTime: String
    ): List<TableEntity>

    @POST("bookings")
    suspend fun createBooking(
        @Body booking: BookingCreationDto
    ): BookingEntity

    // ⭐ НОВАЯ ТОЧКА: Получение всех бронирований пользователя
    @GET("bookings/user/{userId}")
    suspend fun getUserBookings(
        @Path("userId") userId: Long
    ): List<com.example.roamly.entity.DTO.BookingDisplayDto>

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
}