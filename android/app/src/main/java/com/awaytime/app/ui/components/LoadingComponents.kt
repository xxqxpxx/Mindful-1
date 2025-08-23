package com.awaytime.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awaytime.app.ui.theme.AwayTimeColors
import com.awaytime.app.ui.theme.AwayTimeTheme
import kotlinx.coroutines.delay

// MARK: - Skeleton Loading Components

@Composable
fun SkeletonBox(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    cornerRadius: androidx.compose.ui.unit.Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    Box(
        modifier = modifier
            .size(width, height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        AwayTimeColors.skeletonBase,
                        AwayTimeColors.skeletonHighlight,
                        AwayTimeColors.skeletonBase
                    ),
                    start = Offset(shimmerOffset * width.value, 0f),
                    end = Offset((shimmerOffset + 1f) * width.value, 0f)
                )
            )
            .semantics {
                contentDescription = "Loading content"
            }
    )
}

@Composable
fun SkeletonText(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    SkeletonBox(
        width = width,
        height = height,
        cornerRadius = height / 2,
        modifier = modifier
    )
}

@Composable
fun SkeletonCircle(
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    SkeletonBox(
        width = size,
        height = size,
        cornerRadius = size / 2,
        modifier = modifier
    )
}

// MARK: - Loading Progress Circle

@Composable
fun LoadingProgressCircle(
    size: androidx.compose.ui.unit.Dp = 200.dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 12.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(size)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidthPx = strokeWidth.toPx()
                val radius = (size.toPx() - strokeWidthPx) / 2
                val center = Offset(size.toPx() / 2, size.toPx() / 2)

                // Background circle
                drawCircle(
                    color = AwayTimeColors.secondary.copy(alpha = 0.2f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidthPx)
                )

                // Animated loading arc
                drawArc(
                    color = AwayTimeColors.primary,
                    startAngle = rotation - 90f,
                    sweepAngle = 120f,
                    useCenter = false,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                )
            }

            // Loading text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Loading...",
                    style = MaterialTheme.typography.headlineSmall,
                    color = AwayTimeColors.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Fetching your data",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// MARK: - Smooth Number Transition

@Composable
fun SmoothNumberTransition(
    targetValue: Int,
    formatter: (Int) -> String = { it.toString() },
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    var currentValue by remember { mutableIntStateOf(0) }

    LaunchedEffect(targetValue) {
        val duration = 1000L
        val steps = maxOf(kotlin.math.abs(targetValue - currentValue), 1)
        val stepDuration = duration / steps
        val increment = if (targetValue > currentValue) 1 else -1

        repeat(steps) {
            delay(stepDuration)
            currentValue += increment
        }
    }

    Text(
        text = formatter(currentValue),
        style = style,
        color = color,
        modifier = modifier.semantics {
            contentDescription = "Value: ${formatter(currentValue)}"
        }
    )
}

// MARK: - Enhanced Button Components

@Composable
fun AwayTimeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = AwayTimeColors.primary,
        contentColor = Color.White
    ),
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
    content: @Composable RowScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        contentPadding = contentPadding,
        shape = RoundedCornerShape(12.dp),
        content = content
    )
}

@Composable
fun AwayTimeSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = AwayTimeColors.primary,
    content: @Composable RowScope.() -> Unit
) {
    AwayTimeButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.1f),
            contentColor = color
        ),
        content = content
    )
}

// MARK: - Loading States for Dashboard Components

@Composable
fun DashboardLoadingSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header skeleton
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SkeletonText(width = 150.dp, height = 32.dp)
            SkeletonText(width = 200.dp, height = 16.dp)
        }

        // Progress circle skeleton
        SkeletonCircle(size = 200.dp)

        // Stats skeleton
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(3) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SkeletonText(width = 60.dp, height = 12.dp)
                        SkeletonText(width = 40.dp, height = 24.dp)
                        SkeletonText(width = 50.dp, height = 12.dp)
                    }
                }
            }
        }

        // Action buttons skeleton
        repeat(3) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SkeletonCircle(size = 32.dp)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SkeletonText(width = 120.dp, height = 18.dp)
                        SkeletonText(width = 160.dp, height = 14.dp)
                    }
                    SkeletonBox(width = 16.dp, height = 16.dp, cornerRadius = 2.dp)
                }
            }
        }
    }
}

// MARK: - Error State Components

@Composable
fun ErrorStateView(
    title: String = "Something went wrong",
    message: String = "Please try again later",
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Error icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    AwayTimeColors.error.copy(alpha = 0.1f),
                    RoundedCornerShape(40.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⚠️",
                fontSize = 32.sp
            )
        }

        // Error text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Retry button
        onRetry?.let { retry ->
            AwayTimeButton(
                onClick = retry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AwayTimeColors.primary
                )
            ) {
                Text("Try Again")
            }
        }
    }
}

// MARK: - Previews

@Preview(showBackground = true)
@Composable
fun LoadingComponentsPreview() {
    AwayTimeTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LoadingProgressCircle()

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SkeletonBox(width = 100.dp, height = 60.dp)
                SkeletonCircle(size = 60.dp)
                SkeletonText(width = 120.dp)
            }

            SmoothNumberTransition(
                targetValue = 75,
                formatter = { "$it%" }
            )

            ErrorStateView(
                onRetry = { }
            )
        }
    }
}