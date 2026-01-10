// PinnedEstablishmentsRepository.kt
package com.example.roamly.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.pinnedEstablishmentsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "pinned_establishments"
)

@Singleton
class PinnedEstablishmentsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.pinnedEstablishmentsDataStore
    private val PINNED_ESTABLISHMENTS_KEY = stringSetPreferencesKey("pinned_establishments_ids")

    suspend fun getPinnedEstablishments(): Set<Long> {
        return dataStore.data
            .map { preferences ->
                val stringSet = preferences[PINNED_ESTABLISHMENTS_KEY] ?: emptySet()
                stringSet.mapNotNull { it.toLongOrNull() }.toSet()
            }
            .first()
    }

    suspend fun pinEstablishment(establishmentId: Long) {
        dataStore.edit { preferences ->
            val currentSet = preferences[PINNED_ESTABLISHMENTS_KEY] ?: emptySet()
            preferences[PINNED_ESTABLISHMENTS_KEY] = currentSet + establishmentId.toString()
        }
    }

    suspend fun unpinEstablishment(establishmentId: Long) {
        dataStore.edit { preferences ->
            val currentSet = preferences[PINNED_ESTABLISHMENTS_KEY] ?: emptySet()
            preferences[PINNED_ESTABLISHMENTS_KEY] = currentSet - establishmentId.toString()
        }
    }

    suspend fun isPinned(establishmentId: Long): Boolean {
        return dataStore.data
            .map { preferences ->
                val stringSet = preferences[PINNED_ESTABLISHMENTS_KEY] ?: emptySet()
                stringSet.contains(establishmentId.toString())
            }
            .first()
    }

    fun getPinnedEstablishmentsFlow(): Flow<Set<Long>> {
        return dataStore.data.map { preferences ->
            val stringSet = preferences[PINNED_ESTABLISHMENTS_KEY] ?: emptySet()
            stringSet.mapNotNull { it.toLongOrNull() }.toSet()
        }
    }

    suspend fun togglePin(establishmentId: Long) {
        dataStore.edit { preferences ->
            val currentSet = preferences[PINNED_ESTABLISHMENTS_KEY] ?: emptySet()
            val idString = establishmentId.toString()
            preferences[PINNED_ESTABLISHMENTS_KEY] = if (currentSet.contains(idString)) {
                currentSet - idString
            } else {
                currentSet + idString
            }
        }
    }
}