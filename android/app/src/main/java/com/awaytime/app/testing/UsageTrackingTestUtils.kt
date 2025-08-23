/*
package com.awaytime.app.testing

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.awaytime.app.MainActivity
import kotlinx.coroutines.launch

*/
/**
 * Utility functions and composables for testing usage tracking
 *//*

object UsageTrackingTestUtils {
    
    */
/**
     * Test usage tracking from any context
     *//*

    suspend fun testFromContext(context: Context): String {
        return DirectUsageTest.testUsageTracking(context)
    }
    
    */
/**
     * Quick check from any context
     *//*

    suspend fun quickCheckFromContext(context: Context): Boolean {
        return DirectUsageTest.quickCheck(context)
    }
}

*/
/**
 * Composable button that tests usage tracking when clicked
 * Add this to any screen for quick testing
 *//*

@Composable
fun UsageTrackingTestButton(
    modifier: Modifier = Modifier,
    text: String = "Test Usage Tracking"
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isLoading by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    
    Column(modifier = modifier) {
        Button(
            onClick = {
                isLoading = true
                lifecycleOwner.lifecycleScope.launch {
                    try {
                        val result = UsageTrackingTestUtils.testFromContext(context)
                        testResult = result
                        println("🧪 Usage Test Result:\n$result")
                    } catch (e: Exception) {
                        testResult = "❌ Test failed: ${e.message}"
                        println("❌ Usage test failed: ${e.message}")
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text)
        }
        
        testResult?.let { result ->
            Spacer(modifier = Modifier.height(8.dp))
            Card {
                Text(
                    text = if (result.contains("SUCCESS")) "✅ Usage tracking is working!" 
                           else if (result.contains("No usage data")) "⚠️ No usage data found today"
                           else "❌ Test failed",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

*/
/**
 * Quick test button that just shows working/not working
 *//*

@Composable
fun QuickUsageTestButton(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isLoading by remember { mutableStateOf(false) }
    var isWorking by remember { mutableStateOf<Boolean?>(null) }
    
    Row(modifier = modifier) {
        Button(
            onClick = {
                isLoading = true
                lifecycleOwner.lifecycleScope.launch {
                    try {
                        val working = UsageTrackingTestUtils.quickCheckFromContext(context)
                        isWorking = working
                        println("🔍 Quick usage check: ${if (working) "✅ WORKING" else "❌ NOT WORKING"}")
                    } catch (e: Exception) {
                        isWorking = false
                        println("❌ Quick check failed: ${e.message}")
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Quick Test")
            }
        }
        
        isWorking?.let { working ->
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (working) "✅ Working" else "❌ Not Working",
                color = if (working) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}

*/
/**
 * Debug panel with multiple test options
 *//*

@Composable
fun UsageTrackingDebugPanel(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Usage Tracking Debug",
                style = MaterialTheme.typography.headlineSmall
            )
            
            QuickUsageTestButton()
            
            UsageTrackingTestButton(
                text = "Full Test"
            )
            
            Button(
                onClick = {
                    // Open the dedicated test activity
                    val intent = android.content.Intent(context, com.awaytime.app.ui.debug.UsageTrackingTestActivity::class.java)
                    context.startActivity(intent)
                }
            ) {
                Text("Open Debug Screen")
            }
            
            Button(
                onClick = {
                    // Call MainActivity test method if available
                    val activity = context as? MainActivity
                    activity?.testUsageTrackingDirectly()
                }
            ) {
                Text("Run MainActivity Test")
            }
        }
    }
}*/
