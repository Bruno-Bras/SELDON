package com.seldon.fleet.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.seldon.fleet.presentation.common.UiFailure
import com.seldon.fleet.ui.theme.SeldonColors

@Composable
fun FleetErrorState(
    failure: UiFailure,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Canvas(modifier = Modifier.size(66.dp)) {
            val path = Path().apply {
                moveTo(size.width * 0.5f, size.height * 0.06f)
                lineTo(size.width * 0.96f, size.height * 0.9f)
                lineTo(size.width * 0.04f, size.height * 0.9f)
                close()
            }
            drawPath(path, color = SeldonColors.Critical.copy(alpha = 0.25f))
            drawPath(path, color = SeldonColors.Critical, style = Stroke(width = 1.5.dp.toPx()))
            drawLine(
                color = SeldonColors.Critical,
                start = Offset(size.width * 0.5f, size.height * 0.34f),
                end = Offset(size.width * 0.5f, size.height * 0.64f),
                strokeWidth = 2.dp.toPx(),
            )
            drawCircle(
                color = SeldonColors.Critical,
                radius = 1.5.dp.toPx(),
                center = Offset(size.width * 0.5f, size.height * 0.76f),
            )
        }
        Text(
            text = failure.title,
            style = MaterialTheme.typography.headlineMedium,
            color = SeldonColors.Critical,
            textAlign = TextAlign.Center,
        )
        Text(
            text = failure.message,
            style = MaterialTheme.typography.bodyLarge,
            color = SeldonColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = failure.hint,
            style = MaterialTheme.typography.bodySmall,
            color = SeldonColors.TextMuted,
            textAlign = TextAlign.Center,
        )
        if (failure.retryable) {
            ConsoleButton(
                label = "TENTAR NOVAMENTE",
                onClick = onRetry,
                accent = SeldonColors.Critical,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SeldonColors.Panel.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                .border(1.dp, SeldonColors.Line, RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "DIAGNÓSTICO",
                    style = MaterialTheme.typography.labelSmall,
                    color = SeldonColors.TextMuted,
                )
                Text(
                    text = "// ${failure.code}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SeldonColors.Critical.copy(alpha = 0.8f),
                )
            }
            Text(
                text = failure.technicalDetail,
                style = MaterialTheme.typography.labelSmall,
                color = SeldonColors.TextSecondary,
            )
        }
    }
}
