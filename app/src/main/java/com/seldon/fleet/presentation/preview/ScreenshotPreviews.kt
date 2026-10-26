package com.seldon.fleet.presentation.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.seldon.fleet.domain.model.DataQuality
import com.seldon.fleet.domain.model.DeviceStatus
import com.seldon.fleet.domain.model.RiskFactor
import com.seldon.fleet.presentation.components.ConsoleBackground
import com.seldon.fleet.presentation.detail.DetailContent
import com.seldon.fleet.presentation.detail.DetailTopBar
import com.seldon.fleet.presentation.detail.DeviceDetailUiModel
import com.seldon.fleet.presentation.detail.FactorUiModel
import com.seldon.fleet.presentation.detail.IssueUiModel
import com.seldon.fleet.presentation.fleet.DeviceUiModel
import com.seldon.fleet.presentation.fleet.FleetFilter
import com.seldon.fleet.presentation.fleet.FleetStats
import com.seldon.fleet.presentation.fleet.FleetUiState
import com.seldon.fleet.presentation.fleet.SuccessContent
import com.seldon.fleet.ui.theme.SeldonTheme

/**
 * Previews used by the Android CLI renderer to produce the two screenshots in
 * `docs/screenshots`. They are not part of the runtime navigation graph.
 */
@Preview(name = "Frota", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun FleetListPreview() {
    SeldonTheme {
        ConsoleBackground {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars),
            ) {
                SuccessContent(
                    state = previewFleetState(),
                    onOpenDevice = {},
                    onToggleFavorite = {},
                    onQueryChange = {},
                    onFilterSelected = {},
                    onClearFilters = {},
                    onRefresh = {},
                )
            }
        }
    }
}

@Preview(name = "Detalhe", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun DeviceDetailPreview() {
    SeldonTheme {
        ConsoleBackground {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    DetailTopBar(onBack = {})
                    DetailContent(
                        device = previewDetail,
                        onToggleFavorite = {},
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private fun previewFleetState(): FleetUiState.Success {
    val devices = listOf(
        DeviceUiModel(
            id = "DEV-1839",
            model = "Galaxy S23",
            soc = "Snapdragon 8 Gen 2",
            tierLabel = "HIGH",
            status = DeviceStatus.CRITICO,
            statusLabel = "CRÍTICO",
            batteryLevelPct = 38,
            relativeDrainPctPerHour = 19.4,
            riskScore = 91.2,
            isFavorite = false,
            hasDataIssues = false,
            headline = "Dreno crítico de bateria",
        ),
        DeviceUiModel(
            id = "DEV-0522",
            model = "Galaxy S23",
            soc = "Snapdragon 8 Gen 2",
            tierLabel = "HIGH",
            status = DeviceStatus.CRITICO,
            statusLabel = "CRÍTICO",
            batteryLevelPct = 42,
            relativeDrainPctPerHour = 13.4,
            riskScore = 86.0,
            isFavorite = true,
            hasDataIssues = false,
            headline = "Dreno crítico de bateria",
        ),
        DeviceUiModel(
            id = "DEV-0917",
            model = "Moto G84",
            soc = "Snapdragon 695",
            tierLabel = "MID",
            status = DeviceStatus.ATENCAO,
            statusLabel = "ATENÇÃO",
            batteryLevelPct = 61,
            relativeDrainPctPerHour = 8.7,
            riskScore = 63.5,
            isFavorite = false,
            hasDataIssues = false,
            headline = "Dreno acima do esperado",
        ),
        DeviceUiModel(
            id = "DEV-0745",
            model = "Redmi Note 12",
            soc = "Snapdragon 685",
            tierLabel = "MID",
            status = DeviceStatus.INDETERMINADO,
            statusLabel = "INDETERMINADO",
            batteryLevelPct = 77,
            relativeDrainPctPerHour = null,
            riskScore = 0.0,
            isFavorite = false,
            hasDataIssues = true,
            headline = "Dados insuficientes",
        ),
        DeviceUiModel(
            id = "DEV-0204",
            model = "Galaxy A54",
            soc = "Exynos 1380",
            tierLabel = "MID",
            status = DeviceStatus.OK,
            statusLabel = "OK",
            batteryLevelPct = 82,
            relativeDrainPctPerHour = 6.3,
            riskScore = 31.0,
            isFavorite = false,
            hasDataIssues = false,
            headline = "Saúde operacional",
        ),
    )
    val stats = FleetStats(
        total = 30,
        ok = 16,
        attention = 8,
        critical = 4,
        unknown = 2,
        favorites = 3,
    )
    return FleetUiState.Success(
        fleetId = "SELDON-BR",
        exportedAtEpochMs = 1_761_600_000_000,
        devices = devices,
        visibleStats = stats,
        fleetStats = stats,
        filter = FleetFilter.ALL,
        query = "",
        sourceWarnings = 2,
        degradedRecords = 2,
    )
}

private val previewDetail = DeviceDetailUiModel(
    id = "DEV-0522",
    model = "Galaxy S23",
    soc = "Snapdragon 8 Gen 2",
    tierLabel = "HIGH",
    status = DeviceStatus.CRITICO,
    statusLabel = "CRÍTICO",
    riskScore = 86.0,
    headline = "DRENO CRÍTICO",
    detail = "Consumo relativo acima do limite crítico de 12,0 %/h. O sinal " +
        "foi observado em 120 min de tela e deve ser tratado antes da próxima " +
        "distribuição da frota.",
    observedValue = "13,4 %/h",
    thresholdValue = "≥ 12,0 %/h",
    dataQuality = DataQuality.HIGH,
    batteryLevelPct = 42,
    absoluteDrainMahPerHour = 670.0,
    relativeDrainPctPerHour = 13.4,
    batteryCapacityMah = 5000.0,
    screenOnMinutes = 120.0,
    jankFramesPct = 12.0,
    crashCount7d = 3,
    androidApi = 34,
    ramMb = 8192,
    nominalVoltage = 3.85,
    lastSeenEpochMs = 1_761_600_000_000,
    isFavorite = true,
    factors = listOf(
        FactorUiModel(RiskFactor.DRAIN, "DRENO", 47.3, 1.12),
        FactorUiModel(RiskFactor.CRASHES, "FALHAS", 24.0, 1.0),
        FactorUiModel(RiskFactor.JANK, "QUADROS", 14.7, 0.8),
    ),
    issues = listOf(
        IssueUiModel("3 falhas em 7 dias: acima do limite de atenção."),
    ),
)
