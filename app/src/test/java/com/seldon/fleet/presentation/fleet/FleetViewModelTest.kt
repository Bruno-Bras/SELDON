package com.seldon.fleet.presentation.fleet

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import androidx.lifecycle.SavedStateHandle
import com.seldon.fleet.core.Resource
import com.seldon.fleet.domain.engine.FleetAnomalyEngine
import com.seldon.fleet.domain.model.DataIssue
import com.seldon.fleet.domain.model.DataIssueCode
import com.seldon.fleet.domain.usecase.ClassifyDeviceHealthUseCase
import com.seldon.fleet.domain.usecase.GetFleetTelemetryUseCase
import com.seldon.fleet.domain.usecase.ToggleFavoriteUseCase
import com.seldon.fleet.presentation.FakeFleetRepository
import com.seldon.fleet.presentation.MainDispatcherRule
import com.seldon.fleet.presentation.testDevice
import com.seldon.fleet.presentation.testFailure
import com.seldon.fleet.presentation.testSnapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FleetViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `success exposes fleet counters, filtering and query in one state`() = runTest {
        val repository = FakeFleetRepository()
        val viewModel = viewModel(repository)

        viewModel.uiState.test {
            assertThat(awaitItem()).isEqualTo(FleetUiState.Loading)
            repository.fleet.value = Resource.Success(
                testSnapshot(
                    testDevice("DEV-OK", model = "Pixel Ok"),
                    testDevice("DEV-ATN", model = "Moto Attention", energyMah = 70.0),
                    testDevice("DEV-CRIT", model = "Galaxy Critical", energyMah = 120.0),
                    testDevice(
                        id = "DEV-IND",
                        model = "Moto Unknown",
                        capacityMah = null,
                        issues = listOf(
                            DataIssue(
                                code = DataIssueCode.INVALID_CAPACITY,
                                field = "batteryCapacityMah",
                                rawValue = "0",
                                message = "Capacidade inválida.",
                            ),
                        ),
                    ),
                ),
            )

            val success = awaitItem() as FleetUiState.Success
            assertThat(success.fleetId).isEqualTo("TEST-FLEET")
            assertThat(success.fleetStats.total).isEqualTo(4)
            assertThat(success.fleetStats.ok).isEqualTo(1)
            assertThat(success.fleetStats.attention).isEqualTo(1)
            assertThat(success.fleetStats.critical).isEqualTo(1)
            assertThat(success.fleetStats.unknown).isEqualTo(1)
            assertThat(success.degradedRecords).isEqualTo(1)

            viewModel.onFilterSelected(FleetFilter.CRITICO)
            val criticalOnly = awaitItem() as FleetUiState.Success
            assertThat(criticalOnly.filter).isEqualTo(FleetFilter.CRITICO)
            assertThat(criticalOnly.devices.map { it.id }).containsExactly("DEV-CRIT")
            assertThat(criticalOnly.visibleStats.critical).isEqualTo(1)
            assertThat(criticalOnly.fleetStats.total).isEqualTo(4)

            viewModel.onFilterSelected(FleetFilter.ALL)
            awaitItem()
            viewModel.onQueryChange("pixel")
            val queried = awaitItem() as FleetUiState.Success
            assertThat(queried.devices.map { it.id }).containsExactly("DEV-OK")

            viewModel.onClearFilters()
            val cleared = awaitItem() as FleetUiState.Success
            assertThat(cleared.query).isEmpty()
            assertThat(cleared.devices).hasSize(4)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `favorite toggle updates list item and favorites filter`() = runTest {
        val repository = FakeFleetRepository()
        val viewModel = viewModel(repository)

        viewModel.uiState.test {
            awaitItem()
            repository.fleet.value = Resource.Success(
                testSnapshot(
                    testDevice("DEV-A"),
                    testDevice("DEV-B"),
                ),
            )
            val initial = awaitItem() as FleetUiState.Success
            assertThat(initial.devices.none { it.isFavorite }).isTrue()

            viewModel.onToggleFavorite("DEV-A")
            val afterToggle = awaitItem() as FleetUiState.Success
            assertThat(afterToggle.devices).hasSize(2)
            assertThat(afterToggle.devices.first { it.id == "DEV-A" }.isFavorite).isTrue()
            assertThat(afterToggle.fleetStats.favorites).isEqualTo(1)

            viewModel.onFilterSelected(FleetFilter.FAVORITES)
            val favoritesOnly = awaitItem() as FleetUiState.Success
            assertThat(favoritesOnly.devices.map { it.id }).containsExactly("DEV-A")

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `filter and query survive ViewModel recreation`() = runTest {
        val repository = FakeFleetRepository()
        val savedState = SavedStateHandle(
            mapOf(
                "fleet_filter" to FleetFilter.CRITICO.name,
                "fleet_query" to "DEV-CRIT",
            ),
        )
        val viewModel = viewModel(repository, savedState)

        viewModel.uiState.test {
            awaitItem()
            repository.fleet.value = Resource.Success(
                testSnapshot(
                    testDevice("DEV-OK"),
                    testDevice("DEV-CRIT", energyMah = 120.0),
                ),
            )
            val success = awaitItem() as FleetUiState.Success
            assertThat(success.filter).isEqualTo(FleetFilter.CRITICO)
            assertThat(success.query).isEqualTo("DEV-CRIT")
            assertThat(success.devices.map { it.id }).containsExactly("DEV-CRIT")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `failure is mapped to explicit error state`() = runTest {
        val repository = FakeFleetRepository()
        val viewModel = viewModel(repository)

        viewModel.uiState.test {
            awaitItem()
            repository.fleet.value = Resource.Failure(testFailure())
            val error = awaitItem() as FleetUiState.Error
            assertThat(error.failure.code).isEqualTo("MALFORMED_SOURCE")
            assertThat(error.failure.title).isEqualTo("TELEMETRIA CORROMPIDA")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `favorite persistence failure emits one-shot event`() = runTest {
        val repository = FakeFleetRepository(failToggle = true)
        val viewModel = viewModel(repository)

        viewModel.events.test {
            viewModel.onToggleFavorite("DEV-X")
            assertThat(awaitItem()).isEqualTo(FleetEvent.FavoriteToggleFailed("DEV-X"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun viewModel(
        repository: FakeFleetRepository,
        savedState: SavedStateHandle = SavedStateHandle(),
    ) = FleetViewModel(
        getFleetTelemetry = GetFleetTelemetryUseCase(
            repository = repository,
            classifier = ClassifyDeviceHealthUseCase(
                engine = FleetAnomalyEngine(),
                dispatcher = UnconfinedTestDispatcher(),
            ),
        ),
        toggleFavorite = ToggleFavoriteUseCase(repository),
        savedStateHandle = savedState,
    )
}
