package com.seldon.fleet.presentation.fleet

import com.seldon.fleet.domain.model.DeviceAnalysis
import com.seldon.fleet.domain.model.DeviceStatus
import com.seldon.fleet.domain.model.DeviceTier
import com.seldon.fleet.domain.model.FleetTelemetry
import com.seldon.fleet.ui.theme.visual

fun FleetTelemetry.toDeviceUiModels(): List<DeviceUiModel> = devices.map { analysis ->
    analysis.toUiModel(favorites = favorites)
}

fun DeviceAnalysis.toUiModel(favorites: Set<String>): DeviceUiModel = DeviceUiModel(
    id = device.id,
    model = device.model,
    soc = device.soc,
    tierLabel = device.tier.label(),
    status = status,
    statusLabel = status.visual().label,
    batteryLevelPct = device.batteryLevelPct,
    relativeDrainPctPerHour = relativeDrainPctPerHour,
    riskScore = diagnosis.riskScore,
    isFavorite = device.id in favorites,
    hasDataIssues = device.dataIssues.isNotEmpty(),
    headline = diagnosis.headline,
)

fun List<DeviceUiModel>.filtered(query: String, filter: FleetFilter): List<DeviceUiModel> {
    val normalizedQuery = query.trim().lowercase()
    return filter { device ->
        val matchesFilter = when (filter) {
            FleetFilter.ALL -> true
            FleetFilter.OK -> device.status == DeviceStatus.OK
            FleetFilter.ATENCAO -> device.status == DeviceStatus.ATENCAO
            FleetFilter.CRITICO -> device.status == DeviceStatus.CRITICO
            FleetFilter.INDETERMINADO -> device.status == DeviceStatus.INDETERMINADO
            FleetFilter.FAVORITES -> device.isFavorite
        }
        val matchesQuery = normalizedQuery.isEmpty() ||
            device.id.lowercase().contains(normalizedQuery) ||
            device.model.lowercase().contains(normalizedQuery) ||
            device.soc.lowercase().contains(normalizedQuery)
        matchesFilter && matchesQuery
    }.sortedWith(
        compareBy<DeviceUiModel> { it.status.sortRank() }
            .thenByDescending { it.riskScore }
            .thenBy { it.id },
    )
}

fun List<DeviceUiModel>.toStats(): FleetStats = FleetStats(
    total = size,
    ok = count { it.status == DeviceStatus.OK },
    attention = count { it.status == DeviceStatus.ATENCAO },
    critical = count { it.status == DeviceStatus.CRITICO },
    unknown = count { it.status == DeviceStatus.INDETERMINADO },
    favorites = count { it.isFavorite },
)

fun DeviceTier.label(): String = when (this) {
    DeviceTier.LOW -> "LOW"
    DeviceTier.MID -> "MID"
    DeviceTier.HIGH -> "HIGH"
    DeviceTier.UNKNOWN -> "—"
}

private fun DeviceStatus.sortRank(): Int = when (this) {
    DeviceStatus.CRITICO -> 0
    DeviceStatus.ATENCAO -> 1
    DeviceStatus.INDETERMINADO -> 2
    DeviceStatus.OK -> 3
}
