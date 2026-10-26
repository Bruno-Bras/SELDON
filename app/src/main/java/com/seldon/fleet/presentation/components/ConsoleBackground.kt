package com.seldon.fleet.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.seldon.fleet.ui.theme.SeldonColors

@Composable
fun ConsoleBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SeldonColors.Void)
            .drawBehind {
                drawRect(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF07101A),
                            SeldonColors.Void,
                            Color(0xFF04080D),
                        ),
                        startY = 0f,
                        endY = size.height,
                    ),
                )

                val step = 32.dp.toPx()
                val grid = SeldonColors.Line.copy(alpha = 0.14f)
                var x = 0f
                while (x <= size.width) {
                    drawLine(grid, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                    x += step
                }
                var y = 0f
                while (y <= size.height) {
                    drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += step
                }

                val glowCenter = Offset(size.width * 0.9f, 0f)
                val glowRadius = size.width * 0.85f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            SeldonColors.Healthy.copy(alpha = 0.055f),
                            Color.Transparent,
                        ),
                        center = glowCenter,
                        radius = glowRadius,
                    ),
                    radius = glowRadius,
                    center = glowCenter,
                )
            },
        content = content,
    )
}
