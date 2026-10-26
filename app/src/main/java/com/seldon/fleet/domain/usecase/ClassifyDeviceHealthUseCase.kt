package com.seldon.fleet.domain.usecase

import com.seldon.fleet.di.DefaultDispatcher
import com.seldon.fleet.domain.engine.FleetAnomalyEngine
import com.seldon.fleet.domain.model.Device
import com.seldon.fleet.domain.model.DeviceAnalysis
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ClassifyDeviceHealthUseCase @Inject constructor(
    private val engine: FleetAnomalyEngine,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) {
    suspend fun classify(devices: List<Device>): List<DeviceAnalysis> = withContext(dispatcher) {
        devices.map(engine::analyze)
    }

    suspend fun classify(device: Device): DeviceAnalysis = withContext(dispatcher) {
        engine.analyze(device)
    }
}
