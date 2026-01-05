package com.example.roamly.websocket

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Определяем DataStore как extension property (для удобства)
val Context.notificationsDataStore: DataStore<Preferences> by preferencesDataStore(name = "notifications_prefs")

@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {

    @Provides
    @Singleton
    fun provideNotificationHelper(@ApplicationContext context: Context): NotificationHelper {
        return NotificationHelper(context)
    }

    @Provides
    @Singleton
    fun provideNotificationsDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.notificationsDataStore
    }
}