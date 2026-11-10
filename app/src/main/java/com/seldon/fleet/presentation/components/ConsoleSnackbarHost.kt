package com.seldon.fleet.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.seldon.fleet.ui.theme.SeldonColors

@Composable
fun ConsoleSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier.padding(16.dp),
    ) { data ->
        Snackbar(
            snackbarData = data,
            shape = RoundedCornerShape(10.dp),
            containerColor = SeldonColors.PanelRaised,
            contentColor = SeldonColors.TextPrimary,
            actionColor = SeldonColors.Healthy,
            dismissActionContentColor = SeldonColors.TextMuted,
        )
    }
}
