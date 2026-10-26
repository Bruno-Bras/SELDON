package com.seldon.fleet.domain.model

/**
 * Modelo de domínio já sanitizado. Energia chega aqui exclusivamente em mAh,
 * normalizada pela camada de dados; o domínio não conhece JSON nem unidades de origem.
 */
data class Device(
    val id: String,
    val model: String,
    val soc: String,
    val tier: DeviceTier,
    val ramMb: Int?,
    val androidApi: Int?,
    val batteryCapacityMah: Double?,
    val batteryLevelPct: Int?,
    val screenOnMinutes: Double?,
    val energyConsumedMah: Double?,
    val nominalVoltage: Double?,
    val jankFramesPct: Double?,
    val crashCount7d: Int?,
    val lastSeenEpochMs: Long?,
    val dataIssues: List<DataIssue> = emptyList(),
)
