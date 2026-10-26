package com.seldon.fleet.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.seldon.fleet.data.local.AssetFleetDataSource
import com.seldon.fleet.data.local.FleetDataSource
import com.seldon.fleet.data.prefs.DataStoreFleetPreferencesDataSource
import com.seldon.fleet.data.prefs.FleetPreferencesDataSource
import com.seldon.fleet.data.repository.FleetRepositoryImpl
import com.seldon.fleet.domain.repository.FleetRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindFleetDataSource(implementation: AssetFleetDataSource): FleetDataSource

    @Binds
    @Singleton
    abstract fun bindFleetPreferences(
        implementation: DataStoreFleetPreferencesDataSource,
    ): FleetPreferencesDataSource

    @Binds
    @Singleton
    abstract fun bindFleetRepository(implementation: FleetRepositoryImpl): FleetRepository

    companion object {
        @Provides
        @Singleton
        fun providePreferencesDataStore(
            @ApplicationContext context: Context,
            @ApplicationScope scope: CoroutineScope,
        ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            scope = scope,
            produceFile = { context.preferencesDataStoreFile("seldon_fleet.preferences_pb") },
        )
    }
}
