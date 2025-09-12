package com.example.roamly.factory

import com.example.roamly.PingService
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

object RetrofitFactory {
    fun create(baseUrl: String): PingService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(PingService::class.java)
    }
}
