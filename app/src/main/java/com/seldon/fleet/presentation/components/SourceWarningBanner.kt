package com.seldon.fleet.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.seldon.fleet.ui.theme.SeldonColors

@Composable
fun SourceWarningBanner(
    warnings: Int,
    degradedRecords: Int,
    modifier: Modifier = Modifier,
) {
    if (warnings <= 0 && degradedRecords <= 0) return

    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SeldonColors.Warning.copy(alpha = 0.08f), shape)
            .border(1.dp, SeldonColors.Warning.copy(alpha = 0.28f), shape)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = SeldonColors.Warning,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = buildString {
                append("FONTE DEGRADADA")
                if (degradedRecords > 0) append(" · $degradedRecords REGISTRO(S) COM RESSALVAS")
                if (warnings > 0) append(" · $warnings AVISO(S) DE ESTRUTURA")
            },
            style = MaterialTheme.typography.labelSmall,
            color = SeldonColors.Warning,
            modifier = Modifier.weight(1f),
        )
    }
}
