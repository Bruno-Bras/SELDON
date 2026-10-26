package com.seldon.fleet.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seldon.fleet.ui.theme.SeldonColors

@Composable
fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    accent: Color = SeldonColors.TextPrimary,
    note: String? = null,
    noteColor: Color = SeldonColors.TextMuted,
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier
            .background(SeldonColors.PanelSoft.copy(alpha = 0.8f), shape)
            .border(1.dp, SeldonColors.Line.copy(alpha = 0.85f), shape)
            .padding(horizontal = 11.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = SeldonColors.TextMuted,
            maxLines = 1,
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 21.sp),
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (unit != null) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = SeldonColors.TextMuted,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
        if (note != null) {
            Text(
                text = note,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = noteColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun MetricRow(
    left: @Composable () -> Unit,
    right: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        androidx.compose.foundation.layout.Box(Modifier.weight(1f)) { left() }
        androidx.compose.foundation.layout.Box(Modifier.weight(1f)) { right(Modifier.fillMaxWidth()) }
    }
}
