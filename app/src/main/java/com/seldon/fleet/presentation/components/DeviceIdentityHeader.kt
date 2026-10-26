package com.seldon.fleet.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seldon.fleet.presentation.common.formatUtcLong
import com.seldon.fleet.presentation.detail.DeviceDetailUiModel
import com.seldon.fleet.ui.theme.SeldonColors
import com.seldon.fleet.ui.theme.SeldonFonts
import com.seldon.fleet.ui.theme.visual

@Composable
fun DeviceIdentityHeader(
    device: DeviceDetailUiModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "DISPOSITIVO / REGISTRO",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = SeldonColors.TextMuted,
            )
            Spacer(Modifier.weight(1f))
            if (device.isFavorite) {
                Text(
                    text = "FAVORITO",
                    style = MaterialTheme.typography.labelSmall,
                    color = SeldonColors.Warning,
                )
            }
        }
        Text(
            text = device.id,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = SeldonFonts.Mono,
                fontWeight = FontWeight.Bold,
                fontSize = 27.sp,
                letterSpacing = 0.4.sp,
            ),
            color = SeldonColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = device.model,
            style = MaterialTheme.typography.titleLarge,
            color = SeldonColors.TextPrimary,
        )
        Text(
            text = "${device.soc} · TIER ${device.tierLabel}",
            style = MaterialTheme.typography.labelSmall,
            color = SeldonColors.TextMuted,
        )
        Row(
            modifier = Modifier.padding(top = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            StatusPill(device.status)
            Text(
                text = "CONTATO ${formatUtcLong(device.lastSeenEpochMs)}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = SeldonColors.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
