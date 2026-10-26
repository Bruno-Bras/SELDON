package com.seldon.fleet.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.seldon.fleet.ui.theme.SeldonColors

@Composable
fun ConsolePanel(
    modifier: Modifier = Modifier,
    borderColor: Color = SeldonColors.Line,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SeldonColors.Panel.copy(alpha = 0.86f), shape)
            .border(1.dp, borderColor, shape)
            .padding(14.dp),
        content = content,
    )
}
