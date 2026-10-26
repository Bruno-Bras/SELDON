package com.seldon.fleet.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.seldon.fleet.presentation.common.formatDecimal
import com.seldon.fleet.ui.theme.SeldonColors

@Composable
fun RiskFactorBar(
    label: String,
    points: Double,
    normalized: Double,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = SeldonColors.TextSecondary,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${formatDecimal(points)} PTS",
                style = MaterialTheme.typography.labelSmall,
                color = accent,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(SeldonColors.Line, RoundedCornerShape(50)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(normalized.coerceIn(0.0, 1.0).toFloat())
                    .height(4.dp)
                    .background(accent, RoundedCornerShape(50)),
            )
        }
    }
}
