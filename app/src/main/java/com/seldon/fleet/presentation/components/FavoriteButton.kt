package com.seldon.fleet.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.seldon.fleet.ui.theme.SeldonColors

@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ConsoleButton(
        label = if (isFavorite) "FAVORITO · PERSISTIDO" else "MARCAR COMO FAVORITO",
        onClick = onToggle,
        accent = if (isFavorite) SeldonColors.Warning else SeldonColors.TextSecondary,
        modifier = modifier.fillMaxWidth(),
        leading = {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = if (isFavorite) SeldonColors.Warning else SeldonColors.TextMuted,
                modifier = Modifier.size(15.dp),
            )
        },
    )
}
