package com.seldon.fleet.domain.repository

import com.seldon.fleet.core.Resource
import com.seldon.fleet.domain.model.FleetSnapshot
import kotlinx.coroutines.flow.Flow

interface FleetRepository {
    fun observeFleet(): Flow<Resource<FleetSnapshot>>

    suspend fun toggleFavorite(deviceId: String): Result<Boolean>

    suspend fun refresh()
}
