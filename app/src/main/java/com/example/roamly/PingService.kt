package com.example.roamly

import retrofit2.http.GET

interface PingService {
    @GET("/test/ping")
    suspend fun ping(): String
}