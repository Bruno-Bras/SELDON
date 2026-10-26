package com.seldon.fleet.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seldon.fleet.presentation.common.formatUtcShort
import com.seldon.fleet.ui.theme.SeldonColors

@Composable
fun FleetHeader(
    fleetId: String?,
    deviceCount: Int?,
    degradedRecords: Int,
    exportedAtEpochMs: Long?,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandMark()
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SELDON FLEET",
                    style = MaterialTheme.typography.headlineLarge,
                    color = SeldonColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "FLEET OPERATIONS // ${fleetId ?: "SELDON-BR"}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = SeldonColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Recarregar frota",
                    tint = SeldonColors.TextSecondary,
                    modifier = Modifier.size(19.dp),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            HeartbeatDot(
                color = SeldonColors.Healthy,
                size = 5.dp,
                active = true,
            )
            Text(
                text = "FONTE LOCAL · ${deviceCount?.toString() ?: "--"} DISPOSITIVOS",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = SeldonColors.TextSecondary,
            )
            if (degradedRecords > 0) {
                Text(
                    text = "· $degradedRecords RESSALVAS",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = SeldonColors.Warning,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "EXPORT ${formatUtcShort(exportedAtEpochMs)}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = SeldonColors.TextMuted,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun BrandMark() {
    Canvas(modifier = Modifier.size(34.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.46f
        val path = Path().apply {
            for (i in 0 until 6) {
                val angle = Math.toRadians((60.0 * i - 90.0))
                val point = Offset(
                    x = center.x + (radius * kotlin.math.cos(angle)).toFloat(),
                    y = center.y + (radius * kotlin.math.sin(angle)).toFloat(),
                )
                if (i == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
            }
            close()
        }
        drawPath(path, color = SeldonColors.Healthy.copy(alpha = 0.10f))
        drawPath(path, color = SeldonColors.Healthy.copy(alpha = 0.8f), style = Stroke(width = 1.2.dp.toPx()))

        val bolt = Path().apply {
            moveTo(size.width * 0.55f, size.height * 0.24f)
            lineTo(size.width * 0.36f, size.height * 0.54f)
            lineTo(size.width * 0.48f, size.height * 0.54f)
            lineTo(size.width * 0.44f, size.height * 0.77f)
            lineTo(size.width * 0.66f, size.height * 0.45f)
            lineTo(size.width * 0.54f, size.height * 0.45f)
            close()
        }
        drawPath(bolt, color = SeldonColors.Healthy)
    }
}
