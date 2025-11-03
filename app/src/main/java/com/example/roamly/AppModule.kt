package com.example.roamly

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    private const val BASE_URL = "http://10.52.115.228:8080/"

    @RequiresApi(Build.VERSION_CODES.O)
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            // Регистрируем адаптер для типа LocalDateTime
            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
            .create()
    }

    // Инструкция №1: Как создавать Retrofit (теперь использует настроенный Gson)
    @Provides
    @Singleton
    fun provideRetrofit(gson: Gson): Retrofit { // Gson теперь инжектируется
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            // Используем настроенный Gson
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}