package com.seldon.fleet.presentation.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.seldon.fleet.core.Resource
import com.seldon.fleet.domain.engine.FleetAnomalyEngine
import com.seldon.fleet.domain.model.DeviceStatus
import com.seldon.fleet.domain.usecase.ClassifyDeviceHealthUseCase
import com.seldon.fleet.domain.usecase.GetFleetTelemetryUseCase
import com.seldon.fleet.domain.usecase.ToggleFavoriteUseCase
import com.seldon.fleet.presentation.FakeFleetRepository
import com.seldon.fleet.presentation.MainDispatcherRule
import com.seldon.fleet.presentation.testDevice
import com.seldon.fleet.presentation.testSnapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `detail exposes verdict and metrics for routed device`() = runTest {
        val repository = FakeFleetRepository()
        val viewModel = viewModel(repository, "DEV-CRIT")

        viewModel.uiState.test {
            awaitItem()
            repository.fleet.value = Resource.Success(
                testSnapshot(
                    testDevice("DEV-OK"),
                    testDevice("DEV-CRIT", energyMah = 120.0, crashes = 4),
                ),
            )
            val success = awaitItem() as DeviceDetailUiState.Success
            assertThat(success.device.id).isEqualTo("DEV-CRIT")
            assertThat(success.device.status).isEqualTo(DeviceStatus.CRITICO)
            assertThat(success.device.statusLabel).isEqualTo("Crítico")
            assertThat(success.device.riskScore).isAtLeast(75.0)
            assertThat(success.device.relativeDrainPctPerHour).isWithin(0.001).of(12.0)
            assertThat(success.device.factors.first().factor)
                .isEqualTo(com.seldon.fleet.domain.model.RiskFactor.DRAIN)
            assertThat(success.device.isFavorite).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `detail favorite toggle updates the immutable state`() = runTest {
        val repository = FakeFleetRepository()
        val viewModel = viewModel(repository, "DEV-OK")

        viewModel.uiState.test {
            awaitItem()
            repository.fleet.value = Resource.Success(testSnapshot(testDevice("DEV-OK")))
            awaitItem()
            viewModel.onToggleFavorite()
            val updated = awaitItem() as DeviceDetailUiState.Success
            assertThat(updated.device.isFavorite).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `unknown routed device becomes explicit error`() = runTest {
        val repository = FakeFleetRepository()
        val viewModel = viewModel(repository, "DEV-MISSING")

        viewModel.uiState.test {
            awaitItem()
            repository.fleet.value = Resource.Success(testSnapshot(testDevice("DEV-OK")))
            val error = awaitItem() as DeviceDetailUiState.Error
            assertThat(error.failure.code).isEqualTo("DEVICE_NOT_FOUND")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `favorite failure emits one-shot detail event`() = runTest {
        val repository = FakeFleetRepository(failToggle = true)
        val viewModel = viewModel(repository, "DEV-OK")

        viewModel.uiState.test {
            awaitItem()
            repository.fleet.value = Resource.Success(testSnapshot(testDevice("DEV-OK")))
            awaitItem()
            viewModel.events.test {
                viewModel.onToggleFavorite()
                assertThat(awaitItem()).isEqualTo(DetailEvent.FavoriteToggleFailed)
                cancelAndIgnoreRemainingEvents()
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun viewModel(repository: FakeFleetRepository, deviceId: String) =
        DeviceDetailViewModel(
            getFleetTelemetry = GetFleetTelemetryUseCase(
                repository = repository,
                classifier = ClassifyDeviceHealthUseCase(
                    engine = FleetAnomalyEngine(),
                    dispatcher = UnconfinedTestDispatcher(),
                ),
            ),
            toggleFavorite = ToggleFavoriteUseCase(repository),
            savedStateHandle = SavedStateHandle(mapOf("deviceId" to deviceId)),
        )
}
