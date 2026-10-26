package com.seldon.fleet.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seldon.fleet.presentation.common.formatDecimal
import com.seldon.fleet.ui.theme.SeldonColors
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RiskGauge(
    score: Double,
    accent: Color,
    critical: Boolean,
    available: Boolean,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "gauge-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "gauge-pulse-alpha",
    )
    val pulseAlpha = if (critical) pulse else 1f
    val fraction = if (available) (score / 100.0).coerceIn(0.0, 1.0).toFloat() else 0f

    Box(
        modifier = modifier.size(132.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = 8.dp.toPx()
            val inset = 13.dp.toPx()
            val arcSize = androidx.compose.ui.geometry.Size(
                width = size.width - inset * 2,
                height = size.height - inset * 2,
            )
            val arcTopLeft = Offset(inset, inset)
            val startAngle = 135f
            val sweep = 270f

            if (critical) {
                drawArc(
                    color = accent.copy(alpha = 0.10f * pulseAlpha),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = stroke + 9.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            drawArc(
                color = SeldonColors.Line,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (fraction > 0f) {
                drawArc(
                    color = accent,
                    startAngle = startAngle,
                    sweepAngle = sweep * fraction,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }

            val center = Offset(size.width / 2f, size.height / 2f)
            val tickRadius = (size.minDimension / 2f) - 3.dp.toPx()
            repeat(11) { index ->
                val angle = Math.toRadians((startAngle + (sweep * index / 10f)).toDouble())
                val outer = Offset(
                    x = center.x + (tickRadius * cos(angle)).toFloat(),
                    y = center.y + (tickRadius * sin(angle)).toFloat(),
                )
                val inner = Offset(
                    x = center.x + ((tickRadius - 4.dp.toPx()) * cos(angle)).toFloat(),
                    y = center.y + ((tickRadius - 4.dp.toPx()) * sin(angle)).toFloat(),
                )
                drawLine(
                    color = if (available) accent.copy(alpha = 0.55f) else SeldonColors.Line,
                    start = inner,
                    end = outer,
                    strokeWidth = 1.dp.toPx(),
                )
            }

            if (available && fraction > 0f) {
                val endAngle = Math.toRadians((startAngle + sweep * fraction).toDouble())
                val dotRadius = (arcSize.width / 2f) - stroke / 2f
                drawCircle(
                    color = accent,
                    radius = 2.4.dp.toPx(),
                    center = Offset(
                        x = center.x + (dotRadius * cos(endAngle)).toFloat(),
                        y = center.y + (dotRadius * sin(endAngle)).toFloat(),
                    ),
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (available) formatDecimal(score) else "—",
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 30.sp),
                color = if (available) accent else SeldonColors.Unknown,
            )
            Text(
                text = if (available) "RISCO / 100" else "SEM BASE",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = SeldonColors.TextMuted,
            )
        }
    }
}
