package com.seldon.fleet.domain.usecase

import com.seldon.fleet.core.Resource
import com.seldon.fleet.domain.model.FleetTelemetry
import com.seldon.fleet.domain.repository.FleetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetFleetTelemetryUseCase @Inject constructor(
    private val repository: FleetRepository,
    private val classifier: ClassifyDeviceHealthUseCase,
) {
    operator fun invoke(): Flow<Resource<FleetTelemetry>> = repository.observeFleet().map { resource ->
        when (resource) {
            is Resource.Loading -> Resource.Loading
            is Resource.Failure -> Resource.Failure(resource.failure)
            is Resource.Success -> {
                val snapshot = resource.data
                val analyses = classifier.classify(snapshot.fleet.devices)
                Resource.Success(
                    FleetTelemetry(
                        fleetId = snapshot.fleet.fleetId,
                        exportedAtEpochMs = snapshot.fleet.exportedAtEpochMs,
                        devices = analyses,
                        favorites = snapshot.favorites,
                        warnings = snapshot.fleet.warnings,
                    ),
                )
            }
        }
    }
}
