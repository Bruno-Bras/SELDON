package com.seldon.fleet.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.seldon.fleet.domain.model.DeviceStatus
import com.seldon.fleet.ui.theme.visual

@Composable
fun StatusPill(
    status: DeviceStatus,
    modifier: Modifier = Modifier,
) {
    val visual = status.visual()
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .background(visual.background, shape)
            .border(1.dp, visual.color.copy(alpha = 0.5f), shape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeartbeatDot(
            color = visual.color,
            size = 5.dp,
            active = status == DeviceStatus.CRITICO,
        )
        Text(
            text = visual.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = visual.color,
        )
    }
}
