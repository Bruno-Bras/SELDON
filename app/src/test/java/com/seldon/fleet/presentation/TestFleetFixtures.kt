package com.seldon.fleet.presentation

import com.seldon.fleet.core.Resource
import com.seldon.fleet.domain.model.DataIssue
import com.seldon.fleet.domain.model.Device
import com.seldon.fleet.domain.model.DeviceTier
import com.seldon.fleet.domain.model.Fleet
import com.seldon.fleet.domain.model.FleetFailure
import com.seldon.fleet.domain.model.FleetSnapshot
import com.seldon.fleet.domain.repository.FleetRepository
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

class FakeFleetRepository(
    initialFleet: Resource<FleetSnapshot> = Resource.Loading,
    initialFavorites: Set<String> = emptySet(),
    var failToggle: Boolean = false,
) : FleetRepository {

    val fleet = MutableStateFlow(initialFleet)
    val favoriteIds = MutableStateFlow(initialFavorites)
    var refreshCount: Int = 0
        private set

    override fun observeFleet(): Flow<Resource<FleetSnapshot>> =
        combine(fleet, favoriteIds) { resource, favorites ->
            when (resource) {
                is Resource.Loading -> Resource.Loading
                is Resource.Failure -> Resource.Failure(resource.failure)
                is Resource.Success -> Resource.Success(
                    resource.data.copy(favorites = favorites),
                )
            }
        }

    override suspend fun toggleFavorite(deviceId: String): Result<Boolean> {
        if (failToggle) return Result.failure(IOException("preferences unavailable"))
        val current = favoriteIds.value.toMutableSet()
        val added = if (current.contains(deviceId)) {
            current.remove(deviceId)
            false
        } else {
            current.add(deviceId)
            true
        }
        favoriteIds.value = current
        return Result.success(added)
    }

    override suspend fun refresh() {
        refreshCount += 1
    }
}

fun testSnapshot(
    vararg devices: Device,
    favorites: Set<String> = emptySet(),
): FleetSnapshot = FleetSnapshot(
    fleet = Fleet(
        fleetId = "TEST-FLEET",
        exportedAtEpochMs = 1_760_000_000_000,
        devices = devices.toList(),
    ),
    favorites = favorites,
)

fun testDevice(
    id: String,
    model: String = "Device $id",
    capacityMah: Double? = 1000.0,
    screenMinutes: Double? = 60.0,
    energyMah: Double? = 10.0,
    jank: Double? = 0.0,
    crashes: Int? = 0,
    issues: List<DataIssue> = emptyList(),
): Device = Device(
    id = id,
    model = model,
    soc = "Test SoC",
    tier = DeviceTier.MID,
    ramMb = 4096,
    androidApi = 34,
    batteryCapacityMah = capacityMah,
    batteryLevelPct = 50,
    screenOnMinutes = screenMinutes,
    energyConsumedMah = energyMah,
    nominalVoltage = 3.85,
    jankFramesPct = jank,
    crashCount7d = crashes,
    lastSeenEpochMs = 1_760_000_000_000,
    dataIssues = issues,
)

fun testFailure(): FleetFailure = FleetFailure.MalformedSource("JSON inválido no teste")
