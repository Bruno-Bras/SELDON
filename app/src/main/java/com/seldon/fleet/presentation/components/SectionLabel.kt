package com.seldon.fleet.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.seldon.fleet.ui.theme.SeldonColors

@Composable
fun SectionLabel(
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label.uppercase(),
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = SeldonColors.TextMuted,
        )
        Row(
            modifier = Modifier.width(42.dp).height(1.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            androidx.compose.foundation.layout.Box(
                Modifier.weight(1f).height(1.dp).background(SeldonColors.Line),
            )
            androidx.compose.foundation.layout.Box(
                Modifier.width(4.dp).height(1.dp).background(SeldonColors.Healthy),
            )
        }
    }
}
