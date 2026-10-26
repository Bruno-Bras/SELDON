package com.seldon.fleet.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.seldon.fleet.ui.theme.SeldonColors

@Composable
fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -500f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-x",
    )
    return remember(x) {
        Brush.linearGradient(
            colors = listOf(
                SeldonColors.Panel,
                SeldonColors.PanelRaised,
                SeldonColors.Panel,
            ),
            start = Offset(x, 0f),
            end = Offset(x + 420f, 260f),
        )
    }
}

@Composable
fun SkeletonBlock(
    brush: Brush,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(6.dp),
) {
    Box(modifier = modifier.background(brush, shape))
}

@Composable
fun FleetSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SkeletonBlock(brush, Modifier.fillMaxWidth().height(56.dp))
        SkeletonBlock(brush, Modifier.fillMaxWidth().height(46.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) {
                SkeletonBlock(
                    brush = brush,
                    modifier = Modifier.width(78.dp).height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                )
            }
        }
        repeat(5) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SeldonColors.Line, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row {
                    SkeletonBlock(brush, Modifier.width(92.dp).height(13.dp))
                    Spacer(Modifier.weight(1f))
                    SkeletonBlock(brush, Modifier.width(62.dp).height(20.dp))
                }
                SkeletonBlock(brush, Modifier.width(170.dp).height(17.dp))
                SkeletonBlock(brush, Modifier.width(120.dp).height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    SkeletonBlock(brush, Modifier.size(width = 58.dp, height = 24.dp))
                    SkeletonBlock(brush, Modifier.size(width = 58.dp, height = 24.dp))
                    Spacer(Modifier.weight(1f))
                    SkeletonBlock(brush, Modifier.size(width = 68.dp, height = 24.dp))
                }
            }
        }
    }
}

@Composable
fun DetailSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SkeletonBlock(brush, Modifier.width(130.dp).height(30.dp))
        SkeletonBlock(brush, Modifier.fillMaxWidth().height(110.dp))
        SkeletonBlock(brush, Modifier.fillMaxWidth().height(150.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonBlock(brush, Modifier.weight(1f).height(72.dp))
            SkeletonBlock(brush, Modifier.weight(1f).height(72.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonBlock(brush, Modifier.weight(1f).height(72.dp))
            SkeletonBlock(brush, Modifier.weight(1f).height(72.dp))
        }
        SkeletonBlock(brush, Modifier.fillMaxWidth().height(46.dp))
    }
}
