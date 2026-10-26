package com.seldon.fleet.presentation.fleet

import androidx.compose.runtime.Immutable
import com.seldon.fleet.domain.model.DeviceStatus
import com.seldon.fleet.presentation.common.UiFailure

enum class FleetFilter {
    ALL,
    OK,
    ATENCAO,
    CRITICO,
    INDETERMINADO,
    FAVORITES;

    companion object {
        fun fromStorage(value: String?): FleetFilter =
            entries.firstOrNull { it.name == value } ?: ALL
    }
}

@Immutable
data class FleetStats(
    val total: Int = 0,
    val ok: Int = 0,
    val attention: Int = 0,
    val critical: Int = 0,
    val unknown: Int = 0,
    val favorites: Int = 0,
)

@Immutable
data class DeviceUiModel(
    val id: String,
    val model: String,
    val soc: String,
    val tierLabel: String,
    val status: DeviceStatus,
    val statusLabel: String,
    val batteryLevelPct: Int?,
    val relativeDrainPctPerHour: Double?,
    val riskScore: Double,
    val isFavorite: Boolean,
    val hasDataIssues: Boolean,
    val headline: String,
)

sealed interface FleetUiState {
    data object Loading : FleetUiState

    @Immutable
    data class Success(
        val fleetId: String,
        val exportedAtEpochMs: Long?,
        val devices: List<DeviceUiModel>,
        val visibleStats: FleetStats,
        val fleetStats: FleetStats,
        val filter: FleetFilter,
        val query: String,
        val sourceWarnings: Int,
        val degradedRecords: Int,
    ) : FleetUiState

    data class Error(val failure: UiFailure) : FleetUiState
}

sealed interface FleetEvent {
    data class FavoriteToggleFailed(val deviceId: String) : FleetEvent
}
