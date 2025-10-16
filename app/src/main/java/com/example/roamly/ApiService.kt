package com.example.roamly

import com.example.roamly.entity.EstablishmentEntity
import com.example.roamly.entity.User
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @POST("/auth/register")
    suspend fun createUser(@Body user: User): Long

    @POST("/auth/login")
    suspend fun loginUser(@Body user: User): User?

    @GET("/api/accounts/{id}")
    suspend fun getUser(@Path("id") id: Long): User

    @POST("/establishments/create")
    suspend fun createEstablishment(@Body establishment: EstablishmentEntity): EstablishmentEntity?
}
