package com.seldon.fleet.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seldon.fleet.presentation.fleet.FleetFilter
import com.seldon.fleet.presentation.fleet.FleetStats
import com.seldon.fleet.ui.theme.SeldonColors

@Composable
fun FleetFilterRow(
    selected: FleetFilter,
    stats: FleetStats,
    onSelect: (FleetFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SeldonFilterChip(
            label = "TODOS",
            count = stats.total,
            selected = selected == FleetFilter.ALL,
            accent = SeldonColors.TextPrimary,
            onClick = { onSelect(FleetFilter.ALL) },
        )
        SeldonFilterChip(
            label = "OK",
            count = stats.ok,
            selected = selected == FleetFilter.OK,
            accent = SeldonColors.Healthy,
            onClick = { onSelect(FleetFilter.OK) },
        )
        SeldonFilterChip(
            label = "ATENÇÃO",
            count = stats.attention,
            selected = selected == FleetFilter.ATENCAO,
            accent = SeldonColors.Warning,
            onClick = { onSelect(FleetFilter.ATENCAO) },
        )
        SeldonFilterChip(
            label = "CRÍTICO",
            count = stats.critical,
            selected = selected == FleetFilter.CRITICO,
            accent = SeldonColors.Critical,
            onClick = { onSelect(FleetFilter.CRITICO) },
        )
        SeldonFilterChip(
            label = "SEM DADOS",
            count = stats.unknown,
            selected = selected == FleetFilter.INDETERMINADO,
            accent = SeldonColors.Unknown,
            onClick = { onSelect(FleetFilter.INDETERMINADO) },
        )
        SeldonFilterChip(
            label = "FAVORITOS",
            count = stats.favorites,
            selected = selected == FleetFilter.FAVORITES,
            accent = SeldonColors.Warning,
            onClick = { onSelect(FleetFilter.FAVORITES) },
            showStar = true,
        )
    }
}

@Composable
private fun SeldonFilterChip(
    label: String,
    count: Int,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    showStar: Boolean = false,
) {
    val shape = RoundedCornerShape(8.dp)
    val contentColor = if (selected) accent else SeldonColors.TextSecondary
    Row(
        modifier = Modifier
            .height(34.dp)
            .background(
                color = if (selected) accent.copy(alpha = 0.13f) else SeldonColors.Panel.copy(alpha = 0.55f),
                shape = shape,
            )
            .border(
                width = 1.dp,
                color = if (selected) accent.copy(alpha = 0.55f) else SeldonColors.Line,
                shape = shape,
            )
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
            )
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showStar) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(12.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
        Text(
            text = count.toString().padStart(2, '0'),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = if (selected) accent else SeldonColors.TextMuted,
        )
    }
}
