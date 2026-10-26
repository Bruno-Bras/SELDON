package com.seldon.fleet.domain.model

data class FleetTelemetry(
    val fleetId: String?,
    val exportedAtEpochMs: Long?,
    val devices: List<DeviceAnalysis>,
    val favorites: Set<String>,
    val warnings: List<DataIssue>,
)
