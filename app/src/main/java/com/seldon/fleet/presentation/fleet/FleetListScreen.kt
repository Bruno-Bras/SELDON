package com.seldon.fleet.presentation.fleet

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seldon.fleet.presentation.components.ConsoleBackground
import com.seldon.fleet.presentation.components.DeviceCard
import com.seldon.fleet.presentation.components.FleetEmptyState
import com.seldon.fleet.presentation.components.FleetErrorState
import com.seldon.fleet.presentation.components.FleetFilterRow
import com.seldon.fleet.presentation.components.FleetHeader
import com.seldon.fleet.presentation.components.FleetMetricsStrip
import com.seldon.fleet.presentation.components.FleetSearchField
import com.seldon.fleet.presentation.components.FleetSkeleton
import com.seldon.fleet.presentation.components.SourceWarningBanner
import com.seldon.fleet.ui.theme.SeldonColors

@Composable
fun FleetListScreen(
    onOpenDevice: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FleetViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is FleetEvent.FavoriteToggleFailed -> snackbarHostState.showSnackbar(
                    message = "Não foi possível salvar ${event.deviceId} nos favoritos.",
                    withDismissAction = true,
                )
            }
        }
    }

    ConsoleBackground(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            when (val state = uiState) {
                is FleetUiState.Loading -> LoadingContent(onRefresh = viewModel::refresh)
                is FleetUiState.Error -> ErrorContent(
                    failure = state.failure,
                    onRetry = viewModel::retry,
                    onRefresh = viewModel::refresh,
                )
                is FleetUiState.Success -> SuccessContent(
                    state = state,
                    onOpenDevice = onOpenDevice,
                    onToggleFavorite = viewModel::onToggleFavorite,
                    onQueryChange = viewModel::onQueryChange,
                    onFilterSelected = viewModel::onFilterSelected,
                    onClearFilters = viewModel::onClearFilters,
                    onRefresh = viewModel::refresh,
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(10.dp),
                    containerColor = SeldonColors.PanelRaised,
                    contentColor = SeldonColors.TextPrimary,
                    actionColor = SeldonColors.Healthy,
                    dismissActionContentColor = SeldonColors.TextMuted,
                )
            }
        }
    }
}

@Composable
private fun LoadingContent(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        FleetHeader(
            fleetId = null,
            deviceCount = null,
            degradedRecords = 0,
            exportedAtEpochMs = null,
            onRefresh = onRefresh,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            FleetSkeleton(modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun ErrorContent(
    failure: com.seldon.fleet.presentation.common.UiFailure,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        FleetHeader(
            fleetId = null,
            deviceCount = null,
            degradedRecords = 0,
            exportedAtEpochMs = null,
            onRefresh = onRefresh,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            FleetErrorState(
                failure = failure,
                onRetry = onRetry,
            )
        }
    }
}

@Composable
internal fun SuccessContent(
    state: FleetUiState.Success,
    onOpenDevice: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onFilterSelected: (FleetFilter) -> Unit,
    onClearFilters: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        FleetHeader(
            fleetId = state.fleetId,
            deviceCount = state.fleetStats.total,
            degradedRecords = state.degradedRecords,
            exportedAtEpochMs = state.exportedAtEpochMs,
            onRefresh = onRefresh,
        )

        Column(
            modifier = Modifier.padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FleetMetricsStrip(
                stats = state.visibleStats,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            SourceWarningBanner(
                warnings = state.sourceWarnings,
                degradedRecords = state.degradedRecords,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            FleetSearchField(
                query = state.query,
                onQueryChange = onQueryChange,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            FleetFilterRow(
                selected = state.filter,
                stats = state.fleetStats,
                onSelect = onFilterSelected,
            )
        }

        Spacer(Modifier.height(10.dp))

        if (state.devices.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                FleetEmptyState(
                    filterActive = state.query.isNotBlank() || state.filter != FleetFilter.ALL,
                    onClearFilters = onClearFilters,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 2.dp,
                    bottom = 26.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(
                    items = state.devices,
                    key = { _, device -> device.id },
                    contentType = { _, _ -> "device" },
                ) { index, device ->
                    DeviceCard(
                        device = device,
                        onClick = { onOpenDevice(device.id) },
                        onToggleFavorite = { onToggleFavorite(device.id) },
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(
                                durationMillis = 260,
                                delayMillis = index.coerceAtMost(7) * 22,
                            ),
                            placementSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            fadeOutSpec = tween(durationMillis = 140),
                        ),
                    )
                }
            }
        }
    }
}
