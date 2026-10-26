package com.seldon.fleet.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.seldon.fleet.core.Resource
import com.seldon.fleet.domain.model.FleetFailure
import com.seldon.fleet.domain.usecase.GetFleetTelemetryUseCase
import com.seldon.fleet.domain.usecase.ToggleFavoriteUseCase
import com.seldon.fleet.presentation.common.toUiFailure
import com.seldon.fleet.presentation.navigation.DeviceDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DeviceDetailViewModel @Inject constructor(
    private val getFleetTelemetry: GetFleetTelemetryUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val deviceId: String? = savedStateHandle.get<String>("deviceId")
        ?: runCatching { savedStateHandle.toRoute<DeviceDetailRoute>().deviceId }.getOrNull()

    private val refreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<DeviceDetailUiState> = refreshTrigger
        .flatMapLatest { getFleetTelemetry() }
        .map { resource ->
            when (resource) {
                is Resource.Loading -> DeviceDetailUiState.Loading
                is Resource.Failure -> DeviceDetailUiState.Error(resource.failure.toUiFailure())
                is Resource.Success -> {
                    val analysis = resource.data.devices.firstOrNull { it.device.id == deviceId }
                    if (analysis == null) {
                        val failure = FleetFailure.DeviceNotFound(deviceId ?: "desconhecido")
                        DeviceDetailUiState.Error(failure.toUiFailure())
                    } else {
                        DeviceDetailUiState.Success(
                            analysis.toDetailUiModel(
                                isFavorite = analysis.device.id in resource.data.favorites,
                            ),
                        )
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = DeviceDetailUiState.Loading,
        )

    private val _events = Channel<DetailEvent>(capacity = Channel.BUFFERED)
    val events: Flow<DetailEvent> = _events.receiveAsFlow()

    fun onToggleFavorite() {
        val device = (uiState.value as? DeviceDetailUiState.Success)?.device ?: return
        viewModelScope.launch {
            toggleFavorite(device.id).onFailure {
                _events.send(DetailEvent.FavoriteToggleFailed)
            }
        }
    }

    fun retry() {
        refreshTrigger.update { it + 1 }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
