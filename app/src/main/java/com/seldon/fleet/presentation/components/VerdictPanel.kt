package com.seldon.fleet.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seldon.fleet.domain.model.DeviceStatus
import com.seldon.fleet.presentation.detail.DeviceDetailUiModel
import com.seldon.fleet.ui.theme.SeldonColors
import com.seldon.fleet.ui.theme.visual
import com.seldon.fleet.ui.theme.visualColor
import com.seldon.fleet.ui.theme.visualLabel

@Composable
fun VerdictPanel(
    device: DeviceDetailUiModel,
    modifier: Modifier = Modifier,
) {
    val visual = device.status.visual()
    val dataColor = device.dataQuality.visualColor()

    ConsolePanel(
        modifier = modifier,
        borderColor = visual.color.copy(alpha = 0.35f),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RiskGauge(
                score = device.riskScore,
                accent = visual.color,
                critical = device.status == DeviceStatus.CRITICO,
                available = device.status != DeviceStatus.INDETERMINADO,
            )
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = "VEREDITO DA ENGINE",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = SeldonColors.TextMuted,
                )
                Text(
                    text = device.headline,
                    style = MaterialTheme.typography.titleLarge,
                    color = SeldonColors.TextPrimary,
                )
                StatusPill(device.status)
                Text(
                    text = device.dataQuality.visualLabel(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = dataColor,
                )
            }
        }

        Spacer(Modifier.padding(top = 12.dp))

        Text(
            text = device.detail,
            style = MaterialTheme.typography.bodyMedium,
            color = SeldonColors.TextSecondary,
        )

        if (device.observedValue != null && device.thresholdValue != null) {
            Spacer(Modifier.padding(top = 12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ObservedBox(
                    label = "SINAL OBSERVADO",
                    value = device.observedValue,
                    accent = visual.color,
                    modifier = Modifier.weight(1f),
                )
                ObservedBox(
                    label = "LIMIAR",
                    value = device.thresholdValue,
                    accent = SeldonColors.TextSecondary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ObservedBox(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = modifier
            .background(SeldonColors.Void.copy(alpha = 0.5f), shape)
            .border(1.dp, SeldonColors.Line, shape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = SeldonColors.TextMuted,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = accent,
        )
    }
}
