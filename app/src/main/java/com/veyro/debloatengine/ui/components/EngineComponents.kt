package com.veyro.debloatengine.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared fluid-motion building blocks for the Veyro UI:
 * spring gauges, pulsing radar badge, shimmer skeleton, press-scale,
 * and the gradient sweep progress bar.
 */

// ---------------------------------------------------------------------------
// Animated circular gauge (spring sweep on launch)
// ---------------------------------------------------------------------------
@Composable
fun GaugeMeter(
    label: String,
    valueText: String,
    subText: String,
    progress: Float,
    ringColor: Color,
    modifier: Modifier = Modifier,
    diameter: Dp = 132.dp
) {
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        sweep.animateTo(
            progress.coerceIn(0f, 1f),
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessVeryLow
            )
        )
    }

    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(diameter)) {
            val stroke = diameter.toPx() * 0.085f
            val inset = stroke
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            drawArc(
                color = ringColor.copy(alpha = 0.15f),
                startAngle = -220f,
                sweepAngle = 260f,
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round),
                size = arcSize,
                topLeft = Offset(inset, inset)
            )
            drawArc(
                color = ringColor,
                startAngle = -220f,
                sweepAngle = 260f * sweep.value,
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round),
                size = arcSize,
                topLeft = Offset(inset, inset)
            )
        }
        Text(
            text = valueText,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "$label • $subText",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Pulsing radar-glow status badge
// ---------------------------------------------------------------------------
@Composable
fun RadarBadge(active: Boolean, text: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "radar")
    val pulse by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
        label = "pulse"
    )
    val glowColor = if (active) Color(0xFF4ADE80) else Color(0xFFFBBF24)

    Box(modifier = modifier.height(30.dp), contentAlignment = Alignment.CenterStart) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .drawBehind {
                    drawCircle(
                        glowColor.copy(alpha = 0.35f * (1f - pulse)),
                        radius = size.minDimension * pulse
                    )
                    drawCircle(
                        glowColor.copy(alpha = 0.18f * (1f - pulse)),
                        radius = size.minDimension * pulse * 0.6f
                    )
                }
        )
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(glowColor)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = glowColor,
            modifier = Modifier.padding(start = 22.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Shimmer skeleton (scanning state)
// ---------------------------------------------------------------------------
private fun Modifier.shimmerBrush(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -600f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "x"
    )
    background(
        Brush.linearGradient(
            colors = listOf(Color(0xFF1D1233), Color(0xFF31245A), Color(0xFF1D1233)),
            start = Offset(x, 0f),
            end = Offset(x + 500f, 220f)
        )
    )
}

@Composable
fun ShimmerSkeletonCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(18.dp))
            .shimmerBrush()
    )
}

@Composable
fun ShimmerSkeletonList(rows: Int = 5) {
    androidx.compose.foundation.layout.Column {
        repeat(rows) {
            ShimmerSkeletonCard(Modifier.padding(vertical = 6.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Spring press-scale wrapper (scale 0.96f on press)
// ---------------------------------------------------------------------------
@Composable
fun PressScale(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 800f),
        label = "pressScale"
    )
    Box(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
    ) {
        content()
    }
}

// ---------------------------------------------------------------------------
// Gradient sweep progress used during batch operations
// ---------------------------------------------------------------------------
@Composable
fun SweepProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "sweep")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "shift"
    )
    Box(
        modifier = modifier
            .height(10.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF31245A))
            .drawBehind {
                val filled = size.width * progress.coerceIn(0f, 1f)
                if (filled > 0f) {
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF8B5CF6), Color(0xFFD9C8FF), Color(0xFF8B5CF6)),
                            start = Offset(filled * shift - filled * 0.4f, 0f),
                            end = Offset(filled * shift + filled * 0.6f, size.height)
                        ),
                        size = Size(filled, size.height),
                        cornerRadius = CornerRadius(size.height / 2)
                    )
                }
            }
    )
}
