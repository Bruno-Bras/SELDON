package com.seldon.fleet.data.local

import android.content.Context
import com.seldon.fleet.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface FleetDataSource {
    suspend fun readFleet(): String
}

@Singleton
class AssetFleetDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : FleetDataSource {

    override suspend fun readFleet(): String = withContext(ioDispatcher) {
        context.assets.open(ASSET_NAME).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private companion object {
        const val ASSET_NAME = "frota.json"
    }
}
