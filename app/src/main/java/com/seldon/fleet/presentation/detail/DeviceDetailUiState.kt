package com.seldon.fleet.presentation.detail

import androidx.compose.runtime.Immutable
import com.seldon.fleet.domain.model.DataQuality
import com.seldon.fleet.domain.model.DeviceAnalysis
import com.seldon.fleet.domain.model.DeviceStatus
import com.seldon.fleet.domain.model.RiskFactor
import com.seldon.fleet.presentation.common.UiFailure
import com.seldon.fleet.presentation.fleet.label
import com.seldon.fleet.ui.theme.visual
import com.seldon.fleet.ui.theme.visualLabel

@Immutable
data class IssueUiModel(
    val message: String,
)

@Immutable
data class FactorUiModel(
    val factor: RiskFactor,
    val label: String,
    val points: Double,
    val normalized: Double,
)

@Immutable
data class DeviceDetailUiModel(
    val id: String,
    val model: String,
    val soc: String,
    val tierLabel: String,
    val status: DeviceStatus,
    val statusLabel: String,
    val riskScore: Double,
    val headline: String,
    val detail: String,
    val observedValue: String?,
    val thresholdValue: String?,
    val dataQuality: DataQuality,
    val batteryLevelPct: Int?,
    val absoluteDrainMahPerHour: Double?,
    val relativeDrainPctPerHour: Double?,
    val batteryCapacityMah: Double?,
    val screenOnMinutes: Double?,
    val jankFramesPct: Double?,
    val crashCount7d: Int?,
    val androidApi: Int?,
    val ramMb: Int?,
    val nominalVoltage: Double?,
    val lastSeenEpochMs: Long?,
    val isFavorite: Boolean,
    val factors: List<FactorUiModel>,
    val issues: List<IssueUiModel>,
)

sealed interface DeviceDetailUiState {
    data object Loading : DeviceDetailUiState
    data class Success(val device: DeviceDetailUiModel) : DeviceDetailUiState
    data class Error(val failure: UiFailure) : DeviceDetailUiState
}

sealed interface DetailEvent {
    data object FavoriteToggleFailed : DetailEvent
}

fun DeviceAnalysis.toDetailUiModel(isFavorite: Boolean): DeviceDetailUiModel = DeviceDetailUiModel(
    id = device.id,
    model = device.model,
    soc = device.soc,
    tierLabel = device.tier.label(),
    status = status,
    statusLabel = status.visual().label,
    riskScore = diagnosis.riskScore,
    headline = diagnosis.headline,
    detail = diagnosis.detail,
    observedValue = diagnosis.observedValue,
    thresholdValue = diagnosis.thresholdValue,
    dataQuality = diagnosis.dataQuality,
    batteryLevelPct = device.batteryLevelPct,
    absoluteDrainMahPerHour = absoluteDrainMahPerHour,
    relativeDrainPctPerHour = relativeDrainPctPerHour,
    batteryCapacityMah = device.batteryCapacityMah,
    screenOnMinutes = device.screenOnMinutes,
    jankFramesPct = device.jankFramesPct,
    crashCount7d = device.crashCount7d,
    androidApi = device.androidApi,
    ramMb = device.ramMb,
    nominalVoltage = device.nominalVoltage,
    lastSeenEpochMs = device.lastSeenEpochMs,
    isFavorite = isFavorite,
    factors = diagnosis.contributions.map {
        FactorUiModel(
            factor = it.factor,
            label = it.factor.visualLabel(),
            points = it.points,
            normalized = it.normalized,
        )
    },
    issues = device.dataIssues.map { IssueUiModel(it.message) },
)
