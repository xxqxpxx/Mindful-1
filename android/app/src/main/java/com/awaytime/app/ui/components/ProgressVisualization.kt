package com.awaytime.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awaytime.app.service.GoalRecommendation
import com.awaytime.app.ui.theme.AwayTimeColors
import com.awaytime.app.ui.theme.AwayTimeTheme

// MARK: - Enhanced Progress Circle

@Composable
fun EnhancedProgressCircle(
    progress: Float,
    size: androidx.compose.ui.unit.Dp = 200.dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 12.dp,
    showAnimation: Boolean = true,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (showAnimation) progress else progress,
        animationSpec = tween(durationMillis = 1500, easing = EaseInOutCubic),
        label = "progress"
    )

    val progressColors = remember(progress) {
        when {
            progress <= 0.5f -> listOf(AwayTimeColors.success, AwayTimeColors.primary)
            progress <= 0.8f -> listOf(AwayTimeColors.primary, AwayTimeColors.warning)
            else -> listOf(AwayTimeColors.warning, Color.Red)
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .semantics {
                contentDescription = "Progress circle showing ${(progress * 100).toInt()}% usage"
            },
        contentAlignment = Alignment.Center
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

            // Progress arc with gradient
            val sweepAngle = animatedProgress * 360f
            val gradient = Brush.sweepGradient(
                colors = progressColors,
                center = center
            )

            drawArc(
                brush = gradient,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )

            // Glow effect for high progress
            if (progress > 0.8f) {
                drawArc(
                    color = progressColors.last().copy(alpha = 0.6f),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidthPx * 0.3f, cap = StrokeCap.Round),
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )
            }
        }
    }
}

// MARK: - Weekly Progress Chart

@Composable
fun WeeklyProgressChart(
    weeklyData: List<Boolean>,
    animated: Boolean = true,
    modifier: Modifier = Modifier
) {
    val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
    val barWidth = 24.dp
    val maxHeight = 40.dp

    val animatedHeights = weeklyData.mapIndexed { index, success ->
        val targetHeight = if (success) maxHeight else maxHeight * 0.2f

        val animatedHeight by animateFloatAsState(
            targetValue = targetHeight.value,
            animationSpec = tween(
                durationMillis = 800,
                delayMillis = if (animated) index * 100 else 0,
                easing = EaseOutCubic
            ),
            label = "height_$index"
        )

        animatedHeight.dp
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "This Week",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            weeklyData.forEachIndexed { index, success ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Progress bar
                    Box(
                        modifier = Modifier
                            .width(barWidth)
                            .height(animatedHeights[index])
                            .clip(RoundedCornerShape(4.dp))
                            .then(
                                if (success) {
                                    Modifier.background(AwayTimeColors.success)
                                } else {
                                    Modifier.background(AwayTimeColors.secondary.copy(alpha = 0.3f))
                                }
                            )
                    )

                    // Day label
                    Text(
                        text = dayLabels.getOrNull(index) ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// MARK: - Usage Trend Chart

@Composable
fun UsageTrendChart(
    usageData: List<Int>, // Usage in minutes for the last 7 days
    limit: Int,
    modifier: Modifier = Modifier
) {
    val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
    val chartHeight = 120.dp
    val maxUsage = maxOf(usageData.maxOrNull() ?: 0, limit)

    val animatedData = usageData.mapIndexed { index, usage ->
        val normalizedHeight = if (maxUsage > 0) {
            (usage.toFloat() / maxUsage.toFloat()) * chartHeight.value
        } else 0f

        val animatedHeight by animateFloatAsState(
            targetValue = maxOf(normalizedHeight, 4f), // Minimum height for visibility
            animationSpec = tween(
                durationMillis = 1000,
                delayMillis = index * 100,
                easing = EaseOutCubic
            ),
            label = "usage_$index"
        )

        animatedHeight.dp
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Usage Trend",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Limit: ${formatTime(limit)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chart area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
        ) {
            // Limit line
            val limitLinePosition = if (maxUsage > 0) {
                (limit.toFloat() / maxUsage.toFloat()) * chartHeight.value
            } else 0f

            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = AwayTimeColors.warning.copy(alpha = 0.5f),
                    start = Offset(0f, size.height - limitLinePosition),
                    end = Offset(size.width, size.height - limitLinePosition),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
            }

            // Usage bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                usageData.forEachIndexed { index, usage ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Usage bar
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(animatedData.getOrNull(index) ?: 4.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(getBarColor(usage, limit))
                        )

                        // Day label
                        Text(
                            text = dayLabels.getOrNull(index) ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun getBarColor(usage: Int, limit: Int): Color {
    return when {
        usage <= limit -> AwayTimeColors.success
        usage <= limit * 1.2 -> AwayTimeColors.warning
        else -> Color.Red
    }
}

// MARK: - Streak Visualization

@Composable
fun StreakVisualization(
    currentStreak: Int,
    longestStreak: Int,
    animated: Boolean = true,
    modifier: Modifier = Modifier
) {
    val animatedCurrentStreak by animateIntAsState(
        targetValue = if (animated) currentStreak else currentStreak,
        animationSpec = tween(durationMillis = 1500, easing = EaseOutCubic),
        label = "streak"
    )

    val streakColor = remember(currentStreak) {
        when {
            currentStreak >= 30 -> Color(0xFFFF9500) // Orange
            currentStreak >= 7 -> AwayTimeColors.success
            currentStreak >= 3 -> AwayTimeColors.primary
            else -> Color.Gray
        }
    }

    val nextMilestone = remember(currentStreak) {
        listOf(3, 7, 14, 30, 60, 100).firstOrNull { it > currentStreak }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Current streak
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = streakColor,
                modifier = Modifier.size(32.dp)
            )

            Text(
                text = "$animatedCurrentStreak",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = streakColor
            )
        }

        Text(
            text = "Day Streak",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Progress to next milestone
        nextMilestone?.let { milestone ->
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Next milestone: $milestone days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = currentStreak.toFloat() / milestone.toFloat(),
                    modifier = Modifier.width(120.dp),
                    color = AwayTimeColors.primary,
                    trackColor = AwayTimeColors.secondary.copy(alpha = 0.3f)
                )
            }
        }

        // Longest streak
        if (longestStreak > currentStreak) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Best: $longestStreak days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// MARK: - Goal Progress Card

@Composable
fun GoalProgressCard(
    title: String,
    current: Int,
    target: Int,
    unit: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    val progress = if (target > 0) (current.toFloat() / target.toFloat()).coerceAtMost(1f) else 0f

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "$current",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )

                    Text(
                        text = "of $target $unit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier.size(40.dp),
                    color = color,
                    trackColor = color.copy(alpha = 0.2f),
                    strokeWidth = 3.dp
                )
            }
        }
    }
}

// MARK: - Goal Recommendation Card

@Composable
fun GoalRecommendationCard(
    recommendation: GoalRecommendation,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = recommendation.type.color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = when (recommendation.type) {
                    GoalRecommendation.RecommendationType.EXCELLENT -> Icons.Default.Star
                    GoalRecommendation.RecommendationType.GOOD -> Icons.Default.CheckCircle
                    GoalRecommendation.RecommendationType.WARNING -> Icons.Default.Warning
                    GoalRecommendation.RecommendationType.EXCEEDED -> Icons.Default.Cancel
                },
                contentDescription = null,
                tint = recommendation.type.color,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = recommendation.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = recommendation.suggestion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// MARK: - Utility Functions

private fun formatTime(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60

    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
}

// MARK: - Previews

@Preview(showBackground = true)
@Composable
fun ProgressVisualizationPreview() {
    AwayTimeTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EnhancedProgressCircle(progress = 0.65f)

            WeeklyProgressChart(
                weeklyData = listOf(true, false, true, true, false, true, true)
            )

            StreakVisualization(
                currentStreak = 5,
                longestStreak = 12
            )
        }
    }
}