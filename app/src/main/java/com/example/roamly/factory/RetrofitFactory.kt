package com.example.roamly.factory

import com.example.roamly.ApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitFactory {
    //IP при подключении через мобильную точку доступа
//    private const val BASE_URL = "http://10.52.115.133:8080"

    const val BASE_URL = "http://10.52.115.228:8080"

    //IP при подключении через wifi
    //private const val BASE_URL = "http://192.168.100.5:8080"

    // ГЛОБАЛЬНЫЙ ВАРИАНТ (для справки):
    // Если бы вы использовали глобальный адрес, он выглядел бы так:
    // private const val BASE_URL = "https://api.your-production-server.com"


    fun create(): ApiService {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
