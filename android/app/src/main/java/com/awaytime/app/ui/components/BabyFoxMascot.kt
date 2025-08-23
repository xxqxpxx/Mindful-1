package com.awaytime.app.ui.components

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lottiefiles.dotlottie.core.compose.ui.DotLottieAnimation
import com.lottiefiles.dotlottie.core.util.DotLottieSource

enum class FoxMood {
    HAPPY, SLEEPY, SAD, EXHAUSTED
}

@Composable
fun BabyFoxMascot(
    usagePercent: Float,
    modifier: Modifier = Modifier,
    size: Int = 120
) {
    var currentMood by remember { mutableStateOf(FoxMood.HAPPY) }
    var isTransitioning by remember { mutableStateOf(false) }
    var currentAnimationPath by remember { mutableStateOf("baby_fox_happy.lottie") }
    var lastUsagePercent by remember { mutableStateOf(usagePercent) }
    
    // Determine new mood based on usage percentage
    val newMood = when {
        usagePercent <= 40f -> FoxMood.HAPPY
        usagePercent <= 70f -> FoxMood.SLEEPY
        usagePercent <= 90f -> FoxMood.SAD
        else -> FoxMood.EXHAUSTED
    }
    
    // Debounced mood transitions - only update if usage changed significantly (>5%)
    LaunchedEffect(usagePercent) {
        val usageChangeThreshold = 5f
        val hasSignificantChange = kotlin.math.abs(usagePercent - lastUsagePercent) >= usageChangeThreshold
        
        if (hasSignificantChange && newMood != currentMood && !isTransitioning) {
            isTransitioning = true
            
            // Debounce the update by waiting 1 second before applying mood change
            kotlinx.coroutines.delay(1000)
            
            // Check again if mood should still change (usage might have changed again)
            val recomputedMood = when {
                usagePercent <= 40f -> FoxMood.HAPPY
                usagePercent <= 70f -> FoxMood.SLEEPY
                usagePercent <= 90f -> FoxMood.SAD
                else -> FoxMood.EXHAUSTED
            }
            
            if (recomputedMood != currentMood) {
                currentMood = recomputedMood
                currentAnimationPath = getIdleAnimationPath(recomputedMood)
                lastUsagePercent = usagePercent
                
                // Only log when mood actually changes to reduce spam
                Log.d("BabyFoxMascot", "Mood changed to: $currentMood (usage: ${usagePercent.toInt()}%)")
            }
            
            // Brief delay for smooth animation transition
            kotlinx.coroutines.delay(300)
            isTransitioning = false
        }
    }
    
    Box(
        modifier = modifier.size(size.dp),
        contentAlignment = Alignment.Center
    ) {
        // Use DotLottie for .lottie files
        DotLottieAnimation(
            source = DotLottieSource.Asset("lottie/$currentAnimationPath"),
            autoplay = true,
            loop = true,
            speed = 1f,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun getIdleAnimationPath(mood: FoxMood): String {
    return when (mood) {
        FoxMood.HAPPY -> "baby_fox_happy.lottie"
        FoxMood.SLEEPY -> "baby_fox_sleepy.lottie"
        FoxMood.SAD -> "baby_fox_sad.lottie"
        FoxMood.EXHAUSTED -> "baby_fox_exhausted.lottie"
    }
}

@Composable
private fun SimpleFoxFallback(
    mood: FoxMood,
    size: Int
) {
    val foxColor = remember(mood) {
        when (mood) {
            FoxMood.HAPPY -> Color(0xFFF4A460) // Sandy brown
            FoxMood.SLEEPY -> Color(0xFFDEB887) // Burlywood
            FoxMood.SAD -> Color(0xFF8B7355) // Dark khaki
            FoxMood.EXHAUSTED -> Color(0xFF696969) // Dim gray
        }
    }
    
    val eyeColor = remember(mood) {
        when (mood) {
            FoxMood.HAPPY -> Color.Black
            FoxMood.SLEEPY -> Color(0xFF2F4F4F) // Dark slate gray
            FoxMood.SAD -> Color(0xFF4682B4) // Steel blue
            FoxMood.EXHAUSTED -> Color(0xFF800000) // Maroon
        }
    }
    
    Canvas(
        modifier = Modifier.size(size.dp)
    ) {
        val centerX = this.size.width / 2f
        val centerY = this.size.height / 2f
        val radius = this.size.width * 0.4f
        
        // Fox body (main circle)
        drawCircle(
            color = foxColor,
            radius = radius,
            center = androidx.compose.ui.geometry.Offset(centerX, centerY)
        )
        
        // Fox ears (triangular shapes)
        val earSize = radius * 0.3f
        val earOffset = radius * 0.7f
        
        // Left ear
        drawCircle(
            color = foxColor,
            radius = earSize,
            center = androidx.compose.ui.geometry.Offset(centerX - earOffset, centerY - earOffset)
        )
        
        // Right ear
        drawCircle(
            color = foxColor,
            radius = earSize,
            center = androidx.compose.ui.geometry.Offset(centerX + earOffset, centerY - earOffset)
        )
        
        // Eyes based on mood
        val eyeSize = radius * 0.15f
        val eyeOffsetX = radius * 0.3f
        val eyeOffsetY = radius * 0.2f
        
        when (mood) {
            FoxMood.HAPPY -> {
                // Happy eyes (circles)
                drawCircle(
                    color = eyeColor,
                    radius = eyeSize,
                    center = androidx.compose.ui.geometry.Offset(centerX - eyeOffsetX, centerY - eyeOffsetY)
                )
                drawCircle(
                    color = eyeColor,
                    radius = eyeSize,
                    center = androidx.compose.ui.geometry.Offset(centerX + eyeOffsetX, centerY - eyeOffsetY)
                )
            }
            FoxMood.SLEEPY -> {
                // Sleepy eyes (horizontal lines)
                val lineWidth = eyeSize * 2f
                val lineHeight = eyeSize * 0.3f
                drawRect(
                    color = eyeColor,
                    topLeft = androidx.compose.ui.geometry.Offset(
                        centerX - eyeOffsetX - lineWidth/2,
                        centerY - eyeOffsetY - lineHeight/2
                    ),
                    size = androidx.compose.ui.geometry.Size(lineWidth, lineHeight)
                )
                drawRect(
                    color = eyeColor,
                    topLeft = androidx.compose.ui.geometry.Offset(
                        centerX + eyeOffsetX - lineWidth/2,
                        centerY - eyeOffsetY - lineHeight/2
                    ),
                    size = androidx.compose.ui.geometry.Size(lineWidth, lineHeight)
                )
            }
            FoxMood.SAD -> {
                // Sad eyes (smaller circles)
                val sadEyeSize = eyeSize * 0.7f
                drawCircle(
                    color = eyeColor,
                    radius = sadEyeSize,
                    center = androidx.compose.ui.geometry.Offset(centerX - eyeOffsetX, centerY - eyeOffsetY)
                )
                drawCircle(
                    color = eyeColor,
                    radius = sadEyeSize,
                    center = androidx.compose.ui.geometry.Offset(centerX + eyeOffsetX, centerY - eyeOffsetY)
                )
            }
            FoxMood.EXHAUSTED -> {
                // Exhausted eyes (X marks)
                val crossSize = eyeSize * 1.5f
                val strokeWidth = eyeSize * 0.3f
                
                // Left eye X
                drawLine(
                    color = eyeColor,
                    start = androidx.compose.ui.geometry.Offset(
                        centerX - eyeOffsetX - crossSize/2,
                        centerY - eyeOffsetY - crossSize/2
                    ),
                    end = androidx.compose.ui.geometry.Offset(
                        centerX - eyeOffsetX + crossSize/2,
                        centerY - eyeOffsetY + crossSize/2
                    ),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = eyeColor,
                    start = androidx.compose.ui.geometry.Offset(
                        centerX - eyeOffsetX + crossSize/2,
                        centerY - eyeOffsetY - crossSize/2
                    ),
                    end = androidx.compose.ui.geometry.Offset(
                        centerX - eyeOffsetX - crossSize/2,
                        centerY - eyeOffsetY + crossSize/2
                    ),
                    strokeWidth = strokeWidth
                )
                
                // Right eye X
                drawLine(
                    color = eyeColor,
                    start = androidx.compose.ui.geometry.Offset(
                        centerX + eyeOffsetX - crossSize/2,
                        centerY - eyeOffsetY - crossSize/2
                    ),
                    end = androidx.compose.ui.geometry.Offset(
                        centerX + eyeOffsetX + crossSize/2,
                        centerY - eyeOffsetY + crossSize/2
                    ),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = eyeColor,
                    start = androidx.compose.ui.geometry.Offset(
                        centerX + eyeOffsetX + crossSize/2,
                        centerY - eyeOffsetY - crossSize/2
                    ),
                    end = androidx.compose.ui.geometry.Offset(
                        centerX + eyeOffsetX - crossSize/2,
                        centerY - eyeOffsetY + crossSize/2
                    ),
                    strokeWidth = strokeWidth
                )
            }
        }
        
        // Nose (small black triangle)
        val noseSize = radius * 0.1f
        drawCircle(
            color = Color.Black,
            radius = noseSize,
            center = androidx.compose.ui.geometry.Offset(centerX, centerY)
        )
        
        // Mouth based on mood
        val mouthRadius = radius * 0.5f
        val mouthStroke = radius * 0.05f
        
        when (mood) {
            FoxMood.HAPPY -> {
                // Happy mouth (smile arc)
                drawArc(
                    color = Color.Black,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(
                        centerX - mouthRadius/2,
                        centerY + radius * 0.1f
                    ),
                    size = androidx.compose.ui.geometry.Size(mouthRadius, mouthRadius/2),
                    style = Stroke(width = mouthStroke)
                )
            }
            FoxMood.SLEEPY -> {
                // Neutral mouth (small line)
                drawRect(
                    color = Color.Black,
                    topLeft = androidx.compose.ui.geometry.Offset(
                        centerX - mouthRadius/4,
                        centerY + radius * 0.25f
                    ),
                    size = androidx.compose.ui.geometry.Size(mouthRadius/2, mouthStroke)
                )
            }
            FoxMood.SAD -> {
                // Sad mouth (frown arc)
                drawArc(
                    color = Color.Black,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(
                        centerX - mouthRadius/2,
                        centerY + radius * 0.2f
                    ),
                    size = androidx.compose.ui.geometry.Size(mouthRadius, mouthRadius/2),
                    style = Stroke(width = mouthStroke)
                )
            }
            FoxMood.EXHAUSTED -> {
                // Exhausted mouth (wavy line)
                drawRect(
                    color = Color.Black,
                    topLeft = androidx.compose.ui.geometry.Offset(
                        centerX - mouthRadius/3,
                        centerY + radius * 0.3f
                    ),
                    size = androidx.compose.ui.geometry.Size(mouthRadius/1.5f, mouthStroke)
                )
            }
        }
    }
}
