package com.seldon.fleet.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.seldon.fleet.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface FleetPreferencesDataSource {
    val favorites: Flow<Set<String>>

    suspend fun toggleFavorite(deviceId: String): Result<Boolean>
}

@Singleton
class DataStoreFleetPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : FleetPreferencesDataSource {

    override val favorites: Flow<Set<String>> = dataStore.data
        .map { preferences -> preferences[FAVORITES_KEY] ?: emptySet() }

    override suspend fun toggleFavorite(deviceId: String): Result<Boolean> = runCatching {
        withContext(ioDispatcher) {
            var isFavorite = false
            dataStore.edit { preferences ->
                val current = preferences[FAVORITES_KEY]?.toMutableSet() ?: mutableSetOf()
                isFavorite = if (current.contains(deviceId)) {
                    current.remove(deviceId)
                    false
                } else {
                    current.add(deviceId)
                    true
                }
                preferences[FAVORITES_KEY] = current
            }
            isFavorite
        }
    }

    private companion object {
        val FAVORITES_KEY = stringSetPreferencesKey("favorite_device_ids")
    }
}
