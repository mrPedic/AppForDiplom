package com.example.roamly.factory

import com.example.roamly.ApiService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitFactory {
    //IP при подключении через мобильную точку доступа
    //const val BASE_URL = "http://10.39.189.228:8080"

    const val BASE_URL = "https://x8tsh9tc-8080.euw.devtunnels.ms"

    //const val BASE_URL = "http://192.168.1.136:8080"

    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS) // Время на соединение с сервером
        .readTimeout(120, TimeUnit.SECONDS)    // Время ожидания ответа (чтения данных)
        .writeTimeout(120, TimeUnit.SECONDS)   // Время на отправку запроса
        .build()

    fun create(): ApiService {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}