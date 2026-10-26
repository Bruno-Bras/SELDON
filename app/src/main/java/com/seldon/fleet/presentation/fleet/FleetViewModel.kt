package com.seldon.fleet.presentation.fleet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seldon.fleet.core.Resource
import com.seldon.fleet.domain.usecase.GetFleetTelemetryUseCase
import com.seldon.fleet.domain.usecase.ToggleFavoriteUseCase
import com.seldon.fleet.presentation.common.toUiFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class FleetViewModel @Inject constructor(
    private val getFleetTelemetry: GetFleetTelemetryUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val queryState = MutableStateFlow(savedStateHandle.get<String>(KEY_QUERY).orEmpty())
    private val filterState = MutableStateFlow(
        FleetFilter.fromStorage(savedStateHandle.get<String>(KEY_FILTER)),
    )
    private val refreshTrigger = MutableStateFlow(0)

    private val telemetry = refreshTrigger
        .flatMapLatest { getFleetTelemetry() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = Resource.Loading,
        )

    val uiState: StateFlow<FleetUiState> = combine(
        telemetry,
        queryState,
        filterState,
    ) { resource, query, filter ->
        when (resource) {
            is Resource.Loading -> FleetUiState.Loading
            is Resource.Failure -> FleetUiState.Error(resource.failure.toUiFailure())
            is Resource.Success -> {
                val allDevices = resource.data.toDeviceUiModels()
                val visibleDevices = allDevices.filtered(query = query, filter = filter)
                FleetUiState.Success(
                    fleetId = resource.data.fleetId ?: "SELDON-BR",
                    exportedAtEpochMs = resource.data.exportedAtEpochMs,
                    devices = visibleDevices,
                    visibleStats = visibleDevices.toStats(),
                    fleetStats = allDevices.toStats(),
                    filter = filter,
                    query = query,
                    sourceWarnings = resource.data.warnings.size,
                    degradedRecords = allDevices.count { it.hasDataIssues },
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = FleetUiState.Loading,
    )

    private val _events = Channel<FleetEvent>(capacity = Channel.BUFFERED)
    val events: Flow<FleetEvent> = _events.receiveAsFlow()

    fun onQueryChange(query: String) {
        queryState.value = query
        savedStateHandle[KEY_QUERY] = query
    }

    fun onFilterSelected(filter: FleetFilter) {
        filterState.value = filter
        savedStateHandle[KEY_FILTER] = filter.name
    }

    fun onClearFilters() {
        onQueryChange("")
        onFilterSelected(FleetFilter.ALL)
    }

    fun onToggleFavorite(deviceId: String) {
        viewModelScope.launch {
            toggleFavorite(deviceId).onFailure {
                _events.send(FleetEvent.FavoriteToggleFailed(deviceId))
            }
        }
    }

    fun refresh() {
        refreshTrigger.update { it + 1 }
    }

    fun retry() = refresh()

    private companion object {
        const val KEY_QUERY = "fleet_query"
        const val KEY_FILTER = "fleet_filter"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
