package com.example.roamly

import com.example.roamly.entity.EstablishmentDisplayDto
import com.example.roamly.entity.EstablishmentEntity
import com.example.roamly.entity.EstablishmentStatus
import com.example.roamly.entity.User
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("/auth/register")
    suspend fun createUser(@Body user: User): Long

    @POST("/auth/login")
    suspend fun loginUser(@Body user: User): User?

    @GET("/api/accounts/{id}")
    suspend fun getUser(@Path("id") id: Long): User

    @POST("/establishments/create")
    suspend fun createEstablishment(@Body establishment: EstablishmentEntity): EstablishmentEntity?

    @GET("/establishments/user/{userId}") // Пример конечной точки
    suspend fun getEstablishmentsByUserId(@Path("userId") userId: Long): List<EstablishmentDisplayDto>

    @GET("/establishments/getAll")
    suspend fun getAllEstablishments(): List<EstablishmentDisplayDto>

    @GET("/establishments/search")
    suspend fun searchEstablishments(@Query("query") query: String): List<EstablishmentDisplayDto>

    @GET("/establishments/pending")
    suspend fun getPendingEstablishments(): List<EstablishmentDisplayDto>

    @PUT("establishments/{id}/status")
    suspend fun updateEstablishmentStatus(
        @Path("id") id: Long,
        @Query("status") status: String
    ): EstablishmentDisplayDto


    @GET("/test/ping")
    suspend fun pingServer(): String
}
