package com.seldon.fleet.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seldon.fleet.domain.model.DeviceStatus
import com.seldon.fleet.presentation.common.formatDecimal
import com.seldon.fleet.presentation.common.formatPercent
import com.seldon.fleet.presentation.fleet.DeviceUiModel
import com.seldon.fleet.ui.theme.SeldonColors
import com.seldon.fleet.ui.theme.visual

@Composable
fun DeviceCard(
    device: DeviceUiModel,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visual = device.status.visual()
    val shape = RoundedCornerShape(14.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "card-scale",
    )
    val isCritical = device.status == DeviceStatus.CRITICO
    val railAlpha = if (isCritical) criticalRailPulse() else 1f
    val borderColor = when (device.status) {
        DeviceStatus.CRITICO -> visual.color.copy(alpha = 0.42f)
        DeviceStatus.ATENCAO -> visual.color.copy(alpha = 0.28f)
        else -> SeldonColors.Line
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        SeldonColors.PanelRaised.copy(alpha = 0.92f),
                        SeldonColors.Panel.copy(alpha = 0.98f),
                    ),
                ),
                shape = shape,
            )
            .border(1.dp, borderColor, shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            visual.color.copy(alpha = 0.30f * railAlpha),
                            visual.color.copy(alpha = railAlpha),
                            visual.color.copy(alpha = 0.30f * railAlpha),
                        ),
                    ),
                    topLeft = Offset.Zero,
                    size = Size(3.dp.toPx(), size.height),
                )
            }
            .semantics { contentDescription = "Abrir detalhes de ${device.id}" }
            .padding(start = 16.dp, top = 13.dp, end = 8.dp, bottom = 13.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = device.id,
                    style = MaterialTheme.typography.labelLarge,
                    color = SeldonColors.TextSecondary,
                    maxLines = 1,
                )
                if (device.hasDataIssues) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Registro com ressalvas de dados",
                        tint = SeldonColors.Warning.copy(alpha = 0.8f),
                        modifier = Modifier.size(11.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                StatusPill(device.status)
                Spacer(Modifier.width(2.dp))
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(30.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = if (device.isFavorite) {
                            "Remover ${device.id} dos favoritos"
                        } else {
                            "Marcar ${device.id} como favorito"
                        },
                        tint = if (device.isFavorite) {
                            SeldonColors.Warning
                        } else {
                            SeldonColors.TextMuted.copy(alpha = 0.55f)
                        },
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = device.model,
                    style = MaterialTheme.typography.titleMedium,
                    color = SeldonColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${device.soc} · TIER ${device.tierLabel}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = SeldonColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                BatteryBlock(
                    level = device.batteryLevelPct,
                    accent = visual.color,
                )
                MiniMetric(
                    label = "DRENO",
                    value = formatPercent(device.relativeDrainPctPerHour),
                    valueColor = SeldonColors.TextSecondary,
                )
                Spacer(Modifier.weight(1f))
                MiniMetric(
                    label = "RISCO",
                    value = if (device.status == DeviceStatus.INDETERMINADO) {
                        "—"
                    } else {
                        formatDecimal(device.riskScore)
                    },
                    valueColor = visual.color,
                    progress = if (device.status == DeviceStatus.INDETERMINADO) {
                        null
                    } else {
                        (device.riskScore / 100.0).toFloat()
                    },
                    progressColor = visual.color,
                )
            }
        }
    }
}

@Composable
private fun criticalRailPulse(): Float {
    val transition = rememberInfiniteTransition(label = "critical-rail")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "critical-rail-alpha",
    )
    return alpha
}

@Composable
private fun BatteryBlock(level: Int?, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = "BATERIA",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = SeldonColors.TextMuted,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BatteryGlyph(level = level, color = accent)
            Text(
                text = level?.let { "$it%" } ?: "N/D",
                style = MaterialTheme.typography.labelLarge,
                color = SeldonColors.TextPrimary,
            )
        }
    }
}

@Composable
private fun MiniMetric(
    label: String,
    value: String,
    valueColor: Color,
    progress: Float? = null,
    progressColor: Color = SeldonColors.Healthy,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = SeldonColors.TextMuted,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = valueColor,
        )
        if (progress != null) {
            Box(
                modifier = Modifier
                    .width(58.dp)
                    .height(2.dp)
                    .background(SeldonColors.Line, RoundedCornerShape(50)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(2.dp)
                        .background(progressColor, RoundedCornerShape(50)),
                )
            }
        }
    }
}

@Composable
private fun BatteryGlyph(level: Int?, color: Color) {
    Canvas(modifier = Modifier.size(width = 23.dp, height = 11.dp)) {
        val stroke = 1.dp.toPx()
        val corner = CornerRadius(2.dp.toPx())
        drawRoundRect(
            color = SeldonColors.TextMuted.copy(alpha = 0.7f),
            style = Stroke(width = stroke),
            cornerRadius = corner,
        )
        drawRoundRect(
            color = SeldonColors.TextMuted.copy(alpha = 0.7f),
            topLeft = Offset(size.width - 2.dp.toPx(), size.height * 0.30f),
            size = Size(2.dp.toPx(), size.height * 0.40f),
            cornerRadius = CornerRadius(1.dp.toPx()),
        )
        if (level != null) {
            val innerWidth = (size.width - 6.dp.toPx()).coerceAtLeast(0f)
            val fillWidth = innerWidth * (level.coerceIn(0, 100) / 100f)
            if (fillWidth > 0f) {
                drawRoundRect(
                    color = color.copy(alpha = 0.85f),
                    topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                    size = Size(fillWidth, size.height - 4.dp.toPx()),
                    cornerRadius = CornerRadius(1.dp.toPx()),
                )
            }
        }
    }
}
