package com.seldon.fleet.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.seldon.fleet.ui.theme.SeldonColors

@Composable
fun FleetEmptyState(
    filterActive: Boolean,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Canvas(modifier = Modifier.size(64.dp)) {
            val path = Path().apply {
                moveTo(size.width * 0.5f, size.height * 0.06f)
                lineTo(size.width * 0.9f, size.height * 0.30f)
                lineTo(size.width * 0.9f, size.height * 0.70f)
                lineTo(size.width * 0.5f, size.height * 0.94f)
                lineTo(size.width * 0.1f, size.height * 0.70f)
                lineTo(size.width * 0.1f, size.height * 0.30f)
                close()
            }
            drawPath(path, color = SeldonColors.Line, style = Stroke(width = 1.5.dp.toPx()))
            drawLine(
                color = SeldonColors.TextMuted.copy(alpha = 0.5f),
                start = Offset(size.width * 0.32f, size.height * 0.32f),
                end = Offset(size.width * 0.68f, size.height * 0.68f),
                strokeWidth = 1.5.dp.toPx(),
            )
        }
        Text(
            text = if (filterActive) "NENHUM DISPOSITIVO NO FILTRO" else "FROTA SEM DISPOSITIVOS",
            style = MaterialTheme.typography.headlineMedium,
            color = SeldonColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = if (filterActive) {
                "Ajuste a busca ou selecione outro status para ver registros."
            } else {
                "A fonte não retornou registros utilizáveis."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = SeldonColors.TextMuted,
            textAlign = TextAlign.Center,
        )
        if (filterActive) {
            ConsoleButton(
                label = "LIMPAR FILTROS",
                onClick = onClearFilters,
                accent = SeldonColors.Healthy,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
