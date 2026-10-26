package com.seldon.fleet.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.seldon.fleet.domain.model.DataQuality
import com.seldon.fleet.domain.model.DeviceStatus
import com.seldon.fleet.domain.model.RiskFactor

@Immutable
data class StatusVisual(
    val label: String,
    val color: Color,
    val background: Color,
)

fun DeviceStatus.visual(): StatusVisual = when (this) {
    DeviceStatus.OK -> StatusVisual(
        label = "OK",
        color = SeldonColors.Healthy,
        background = SeldonColors.Healthy.copy(alpha = 0.12f),
    )
    DeviceStatus.ATENCAO -> StatusVisual(
        label = "Atenção",
        color = SeldonColors.Warning,
        background = SeldonColors.Warning.copy(alpha = 0.12f),
    )
    DeviceStatus.CRITICO -> StatusVisual(
        label = "Crítico",
        color = SeldonColors.Critical,
        background = SeldonColors.Critical.copy(alpha = 0.14f),
    )
    DeviceStatus.INDETERMINADO -> StatusVisual(
        label = "Sem dados",
        color = SeldonColors.Unknown,
        background = SeldonColors.Unknown.copy(alpha = 0.14f),
    )
}

fun DataQuality.visualLabel(): String = when (this) {
    DataQuality.HIGH -> "DADOS ÍNTEGROS"
    DataQuality.DEGRADED -> "DADOS DEGRADADOS"
    DataQuality.VOID -> "DADOS INSUFICIENTES"
}

fun DataQuality.visualColor(): Color = when (this) {
    DataQuality.HIGH -> SeldonColors.Healthy
    DataQuality.DEGRADED -> SeldonColors.Warning
    DataQuality.VOID -> SeldonColors.Unknown
}

fun RiskFactor.visualLabel(): String = when (this) {
    RiskFactor.DRAIN -> "Dreno relativo"
    RiskFactor.CRASHES -> "Falhas em 7 dias"
    RiskFactor.JANK -> "Quadros perdidos"
    RiskFactor.DATA_INTEGRITY -> "Integridade dos dados"
    RiskFactor.NONE -> "Nenhum fator"
}
