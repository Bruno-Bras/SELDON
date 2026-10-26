package com.seldon.fleet.presentation.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seldon.fleet.presentation.fleet.FleetStats
import com.seldon.fleet.ui.theme.SeldonColors

@Composable
fun FleetMetricsStrip(
    stats: FleetStats,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SeldonColors.Panel.copy(alpha = 0.9f), shape)
            .border(1.dp, SeldonColors.Line, shape)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetricCounter("NA TELA", stats.total, SeldonColors.TextPrimary, Modifier.weight(1f))
        MetricDivider()
        MetricCounter("OK", stats.ok, SeldonColors.Healthy, Modifier.weight(1f))
        MetricDivider()
        MetricCounter("ATENÇÃO", stats.attention, SeldonColors.Warning, Modifier.weight(1f))
        MetricDivider()
        MetricCounter("CRÍTICO", stats.critical, SeldonColors.Critical, Modifier.weight(1f))
        MetricDivider()
        MetricCounter("SEM DADOS", stats.unknown, SeldonColors.Unknown, Modifier.weight(1f))
    }
}

@Composable
private fun MetricCounter(
    label: String,
    value: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val animatedValue by animateIntAsState(
        targetValue = value,
        animationSpec = tween(durationMillis = 420),
        label = "metric-$label",
    )
    Column(
        modifier = modifier.semantics { contentDescription = "$label: $value" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = animatedValue.toString().padStart(2, '0'),
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, letterSpacing = 0.5.sp),
            color = SeldonColors.TextMuted,
            maxLines = 1,
        )
    }
}

@Composable
private fun MetricDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(30.dp)
            .background(SeldonColors.Line.copy(alpha = 0.85f)),
    )
}
