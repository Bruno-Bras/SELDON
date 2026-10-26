package com.seldon.fleet.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seldon.fleet.domain.model.DataQuality
import com.seldon.fleet.domain.model.DeviceStatus
import com.seldon.fleet.domain.model.RiskFactor
import com.seldon.fleet.presentation.common.formatDecimal
import com.seldon.fleet.presentation.common.formatInteger
import com.seldon.fleet.presentation.common.formatPercent
import com.seldon.fleet.presentation.common.formatUtcDate
import com.seldon.fleet.presentation.common.formatUtcTime
import com.seldon.fleet.presentation.components.ConsoleBackground
import com.seldon.fleet.presentation.components.ConsolePanel
import com.seldon.fleet.presentation.components.DetailSkeleton
import com.seldon.fleet.presentation.components.DeviceIdentityHeader
import com.seldon.fleet.presentation.components.FavoriteButton
import com.seldon.fleet.presentation.components.FleetErrorState
import com.seldon.fleet.presentation.components.MetricTile
import com.seldon.fleet.presentation.components.RiskFactorBar
import com.seldon.fleet.presentation.components.SectionLabel
import com.seldon.fleet.presentation.components.VerdictPanel
import com.seldon.fleet.ui.theme.SeldonColors
import com.seldon.fleet.ui.theme.visual
import com.seldon.fleet.ui.theme.visualLabel

@Composable
fun DeviceDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeviceDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                DetailEvent.FavoriteToggleFailed -> snackbarHostState.showSnackbar(
                    message = "Não foi possível atualizar o favorito. Tente novamente.",
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
            Column(modifier = Modifier.fillMaxSize()) {
                DetailTopBar(onBack = onBack)
                when (val state = uiState) {
                    is DeviceDetailUiState.Loading -> Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        DetailSkeleton()
                    }
                    is DeviceDetailUiState.Error -> Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        FleetErrorState(
                            failure = state.failure,
                            onRetry = viewModel::retry,
                        )
                    }
                    is DeviceDetailUiState.Success -> DetailContent(
                        device = state.device,
                        onToggleFavorite = viewModel::onToggleFavorite,
                        modifier = Modifier.weight(1f),
                    )
                }
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
internal fun DetailTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Voltar para a frota",
                tint = SeldonColors.TextSecondary,
                modifier = Modifier.size(21.dp),
            )
        }
        Text(
            text = "DETALHE DO DISPOSITIVO",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = SeldonColors.TextMuted,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "LAUDO TÉCNICO",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = SeldonColors.Healthy.copy(alpha = 0.75f),
        )
        Spacer(Modifier.size(8.dp))
    }
}

@Composable
internal fun DetailContent(
    device: DeviceDetailUiModel,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        DeviceIdentityHeader(device = device)

        VerdictPanel(
            device = device,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        FavoriteButton(
            isFavorite = device.isFavorite,
            onToggle = onToggleFavorite,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        SectionLabel(
            label = "matriz de métricas",
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        MetricGrid(
            device = device,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        SectionLabel(
            label = "fatores de risco",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
        )
        RiskFactorsPanel(
            device = device,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        SectionLabel(
            label = "integridade dos dados",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
        )
        IntegrityPanel(
            device = device,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        SectionLabel(
            label = "proveniência",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
        )
        ProvenancePanel(
            device = device,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Text(
            text = "ENGINE HEURÍSTICA LOCAL · REGRA OFICIAL OK / ATENÇÃO / CRÍTICO / INDETERMINADO\n" +
                "O índice de risco é complementar e não altera o status oficial.",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = SeldonColors.TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun MetricGrid(
    device: DeviceDetailUiModel,
    modifier: Modifier = Modifier,
) {
    val visual = device.status.visual()
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile(
                label = "consumo absoluto",
                value = formatDecimal(device.absoluteDrainMahPerHour),
                unit = "mAh/h",
                note = "consumo ÷ horas de tela",
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                label = "dreno relativo",
                value = formatPercent(device.relativeDrainPctPerHour),
                note = "limiar crítico 12 %/h",
                accent = drainAccent(device.relativeDrainPctPerHour),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile(
                label = "capacidade",
                value = formatDecimal(device.batteryCapacityMah, digits = 0),
                unit = "mAh",
                note = if (device.batteryCapacityMah == null) "inválida na origem" else "nominal",
                accent = if (device.batteryCapacityMah == null) SeldonColors.Unknown else SeldonColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                label = "tempo de tela",
                value = formatDecimal(device.screenOnMinutes, digits = 0),
                unit = "min",
                note = if (device.screenOnMinutes == null) "divisor inválido" else "janela observada",
                accent = if (device.screenOnMinutes == null) SeldonColors.Unknown else SeldonColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile(
                label = "bateria",
                value = formatInteger(device.batteryLevelPct),
                unit = "%",
                note = "nível no momento da leitura",
                accent = batteryAccent(device.batteryLevelPct),
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                label = "quadros perdidos",
                value = formatPercent(device.jankFramesPct),
                note = "limiar crítico 15%",
                accent = jankAccent(device.jankFramesPct),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile(
                label = "falhas em 7 dias",
                value = formatInteger(device.crashCount7d),
                note = "limiar crítico 3",
                accent = crashAccent(device.crashCount7d),
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                label = "nível de api",
                value = formatInteger(device.androidApi),
                unit = "API",
                note = "Android reportado",
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile(
                label = "memória ram",
                value = formatInteger(device.ramMb),
                unit = "MB",
                note = "hardware",
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                label = "tensão nominal",
                value = formatDecimal(device.nominalVoltage, digits = 2),
                unit = "V",
                note = "usada na normalização",
                modifier = Modifier.weight(1f),
            )
        }
        MetricTile(
            label = "último contato",
            value = formatUtcDate(device.lastSeenEpochMs),
            note = if (device.lastSeenEpochMs != null) {
                "${formatUtcTime(device.lastSeenEpochMs)} · fuso UTC"
            } else {
                "sem registro de contato"
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = "Veredito atual: ${device.statusLabel.uppercase()} · " +
                if (device.status == DeviceStatus.INDETERMINADO) {
                    "risco não calculável"
                } else {
                    "risco ${formatDecimal(device.riskScore)}/100"
                } +
                " · cor de referência ${visual.label.uppercase()}",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = SeldonColors.TextMuted,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RiskFactorsPanel(
    device: DeviceDetailUiModel,
    modifier: Modifier = Modifier,
) {
    ConsolePanel(modifier = modifier) {
        if (device.factors.isEmpty()) {
            Text(
                text = "Sem fatores calculáveis. O dreno relativo exige capacidade, tempo de tela e energia normalizada.",
                style = MaterialTheme.typography.bodySmall,
                color = SeldonColors.TextMuted,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                device.factors.forEach { factor ->
                    RiskFactorBar(
                        label = factor.label,
                        points = factor.points,
                        normalized = factor.normalized,
                        accent = riskFactorColor(factor.factor),
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "CONTRIBUIÇÕES PONDERADAS · DRENO 55% · FALHAS 30% · QUADROS 15%",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = SeldonColors.TextMuted,
        )
    }
}

@Composable
private fun IntegrityPanel(
    device: DeviceDetailUiModel,
    modifier: Modifier = Modifier,
) {
    val qualityColor = when (device.dataQuality) {
        DataQuality.HIGH -> SeldonColors.Healthy
        DataQuality.DEGRADED -> SeldonColors.Warning
        DataQuality.VOID -> SeldonColors.Unknown
    }
    ConsolePanel(
        modifier = modifier,
        borderColor = qualityColor.copy(alpha = 0.28f),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = device.dataQuality.visualLabel(),
                style = MaterialTheme.typography.labelLarge,
                color = qualityColor,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${device.issues.size.toString().padStart(2, '0')} RESSALVA(S)",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = SeldonColors.TextMuted,
            )
        }
        Spacer(Modifier.height(8.dp))
        if (device.issues.isEmpty()) {
            Text(
                text = "Nenhum campo precisou de fallback na ingestão. Energia, capacidade e divisores estão válidos.",
                style = MaterialTheme.typography.bodySmall,
                color = SeldonColors.TextSecondary,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                device.issues.forEach { issue ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = qualityColor.copy(alpha = 0.85f),
                            modifier = Modifier
                                .padding(top = 1.dp)
                                .size(11.dp),
                        )
                        Text(
                            text = issue.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = SeldonColors.TextSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProvenancePanel(
    device: DeviceDetailUiModel,
    modifier: Modifier = Modifier,
) {
    ConsolePanel(modifier = modifier) {
        Text(
            text = "NORMALIZAÇÃO NA CAMADA DE DADOS",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = SeldonColors.TextMuted,
        )
        Spacer(Modifier.height(6.dp))
        if (device.nominalVoltage != null) {
            Text(
                text = "Energia em mWh foi convertida para mAh usando mAh = mWh ÷ ${formatDecimal(device.nominalVoltage, digits = 2)} V. " +
                    "O domínio recebe apenas mAh e não conhece a unidade de origem.",
                style = MaterialTheme.typography.bodySmall,
                color = SeldonColors.TextSecondary,
            )
        } else {
            Text(
                text = "Sem tensão nominal válida. Se a origem estivesse em mWh, a conversão seria impossível e o registro seria INDETERMINADO.",
                style = MaterialTheme.typography.bodySmall,
                color = SeldonColors.Warning,
            )
        }
        Spacer(Modifier.height(9.dp))
        Text(
            text = "FONTE assets/frota.json · LEITURA LOCAL · SEM REDE · SOMENTE LEITURA",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = SeldonColors.TextMuted,
        )
    }
}

private fun drainAccent(value: Double?): Color = when {
    value == null -> SeldonColors.Unknown
    value >= 12.0 -> SeldonColors.Critical
    value >= 7.0 -> SeldonColors.Warning
    else -> SeldonColors.TextPrimary
}

private fun jankAccent(value: Double?): Color = when {
    value == null -> SeldonColors.Unknown
    value >= 15.0 -> SeldonColors.Critical
    value >= 8.0 -> SeldonColors.Warning
    else -> SeldonColors.TextPrimary
}

private fun crashAccent(value: Int?): Color = when {
    value == null -> SeldonColors.Unknown
    value >= 3 -> SeldonColors.Critical
    value >= 1 -> SeldonColors.Warning
    else -> SeldonColors.TextPrimary
}

private fun batteryAccent(value: Int?): Color = when {
    value == null -> SeldonColors.Unknown
    value <= 15 -> SeldonColors.Critical
    value <= 30 -> SeldonColors.Warning
    else -> SeldonColors.TextPrimary
}

private fun riskFactorColor(factor: RiskFactor): Color = when (factor) {
    RiskFactor.DRAIN -> SeldonColors.Warning
    RiskFactor.CRASHES -> SeldonColors.Critical
    RiskFactor.JANK -> SeldonColors.Cyan
    RiskFactor.DATA_INTEGRITY -> SeldonColors.Unknown
    RiskFactor.NONE -> SeldonColors.TextMuted
}
