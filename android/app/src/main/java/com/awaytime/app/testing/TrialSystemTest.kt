package com.awaytime.app.testing

import android.content.Context
import com.awaytime.app.service.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

/**
 * Comprehensive test for the premium trial system
 */
class TrialSystemTest(private val context: Context) {
    
    private val trialManager = PremiumTrialManager.getInstance(context)
    private val premiumFeatureManager = PremiumFeatureManager.getInstance(context)
    private val subscriptionService = SubscriptionService(context)
    private val trialExpirationManager = TrialExpirationManager.getInstance(context)
    
    companion object {
        private const val TAG = "TrialSystemTest"
    }
    
    /**
     * Run comprehensive trial system tests
     */
    suspend fun runAllTests(): TrialTestResults {
        println("🧪 Starting comprehensive trial system tests...")
        
        val results = TrialTestResults()
        
        try {
            // Test 1: New user detection and trial activation
            results.newUserDetectionTest = testNewUserDetection()
            
            // Test 2: Trial status and time calculations
            results.trialStatusTest = testTrialStatus()
            
            // Test 3: Premium feature access during trial
            results.premiumFeatureAccessTest = testPremiumFeatureAccess()
            
            // Test 4: Trial expiration handling
            results.trialExpirationTest = testTrialExpiration()
            
            // Test 5: Integration with subscription service
            results.subscriptionIntegrationTest = testSubscriptionIntegration()
            
            // Test 6: UI state management
            results.uiStateTest = testUIStateManagement()
            
            // Test 7: Notification system
            results.notificationTest = testNotificationSystem()
            
            // Test 8: Data persistence
            results.dataPersistenceTest = testDataPersistence()
            
            // Calculate overall success
            results.overallSuccess = results.getAllTests().all { it }
            
            println("🧪 Trial system tests completed. Overall success: ${results.overallSuccess}")
            
        } catch (e: Exception) {
            println("❌ Error during trial system tests: ${e.message}")
            results.overallSuccess = false
        }
        
        return results
    }
    
    /**
     * Test new user detection and automatic trial activation
     */
    private suspend fun testNewUserDetection(): Boolean {
        println("🧪 Testing new user detection...")
        
        return try {
            // Reset trial for testing
            trialManager.resetTrial()
            
            // Check that trial is not active initially
            if (trialManager.isTrialActive()) {
                println("❌ Trial should not be active initially")
                return false
            }
            
            // Simulate new user by activating trial
            trialManager.activateTrial()
            
            // Verify trial is now active
            if (!trialManager.isTrialActive()) {
                println("❌ Trial should be active after activation")
                return false
            }
            
            // Check trial duration
            val daysRemaining = trialManager.getDaysRemaining()
            if (daysRemaining != PremiumTrialManager.TRIAL_DURATION_DAYS) {
                println("❌ Days remaining should be ${PremiumTrialManager.TRIAL_DURATION_DAYS}, got $daysRemaining")
                return false
            }
            
            println("✅ New user detection test passed")
            true
            
        } catch (e: Exception) {
            println("❌ New user detection test failed: ${e.message}")
            false
        }
    }
    
    /**
     * Test trial status and time calculations
     */
    private suspend fun testTrialStatus(): Boolean {
        println("🧪 Testing trial status calculations...")
        
        return try {
            // Ensure trial is active
            if (!trialManager.isTrialActive()) {
                trialManager.activateTrial()
            }
            
            // Test status is ACTIVE
            val status = trialManager.trialStatus.first()
            if (status != TrialStatus.ACTIVE) {
                println("❌ Trial status should be ACTIVE, got $status")
                return false
            }
            
            // Test time calculations
            val daysRemaining = trialManager.getDaysRemaining()
            val hoursRemaining = trialManager.getHoursRemaining()
            val progress = trialManager.getTrialProgress()
            
            if (daysRemaining < 0 || daysRemaining > PremiumTrialManager.TRIAL_DURATION_DAYS) {
                println("❌ Invalid days remaining: $daysRemaining")
                return false
            }
            
            if (hoursRemaining < 0 || hoursRemaining > PremiumTrialManager.TRIAL_DURATION_DAYS * 24) {
                println("❌ Invalid hours remaining: $hoursRemaining")
                return false
            }
            
            if (progress < 0f || progress > 1f) {
                println("❌ Invalid progress: $progress")
                return false
            }
            
            // Test start and end dates
            val startDate = trialManager.getTrialStartDate()
            val endDate = trialManager.getTrialEndDate()
            
            if (startDate == null || endDate == null) {
                println("❌ Trial dates should not be null")
                return false
            }
            
            if (endDate.before(startDate)) {
                println("❌ End date should be after start date")
                return false
            }
            
            println("✅ Trial status test passed")
            true
            
        } catch (e: Exception) {
            println("❌ Trial status test failed: ${e.message}")
            false
        }
    }
    
    /**
     * Test premium feature access during trial
     */
    private suspend fun testPremiumFeatureAccess(): Boolean {
        println("🧪 Testing premium feature access...")
        
        return try {
            // Ensure trial is active
            if (!trialManager.isTrialActive()) {
                trialManager.activateTrial()
            }
            
            // Test premium features are accessible
            val premiumFeatures = listOf(
                PremiumFeature.MULTIPLE_APP_GROUPS,
                PremiumFeature.ADVANCED_ANALYTICS,
                PremiumFeature.CUSTOM_THEMES,
                PremiumFeature.EXPORT_DATA,
                PremiumFeature.FOCUS_SESSIONS
            )
            
            for (feature in premiumFeatures) {
                if (!premiumFeatureManager.canUseFeature(feature)) {
                    println("❌ Should be able to use ${feature.displayName} during trial")
                    return false
                }
            }
            
            // Test free features are still accessible
            val freeFeatures = listOf(
                PremiumFeature.BASIC_USAGE_TRACKING,
                PremiumFeature.SINGLE_APP_GROUP,
                PremiumFeature.BASIC_NOTIFICATIONS
            )
            
            for (feature in freeFeatures) {
                if (!premiumFeatureManager.canUseFeature(feature)) {
                    println("❌ Should be able to use ${feature.displayName} (free feature)")
                    return false
                }
            }
            
            // Test subscription service integration
            if (!subscriptionService.isPremiumActive) {
                println("❌ Subscription service should report premium as active during trial")
                return false
            }
            
            println("✅ Premium feature access test passed")
            true
            
        } catch (e: Exception) {
            println("❌ Premium feature access test failed: ${e.message}")
            false
        }
    }
    
    /**
     * Test trial expiration handling
     */
    private suspend fun testTrialExpiration(): Boolean {
        println("🧪 Testing trial expiration...")
        
        return try {
            // Simulate trial expiration by ending it manually
            trialManager.endTrial()
            
            // Check trial status
            if (trialManager.isTrialActive()) {
                println("❌ Trial should not be active after ending")
                return false
            }
            
            if (!trialManager.isTrialEnded()) {
                println("❌ Trial should be marked as ended")
                return false
            }
            
            // Test premium features are now locked
            val premiumFeatures = listOf(
                PremiumFeature.MULTIPLE_APP_GROUPS,
                PremiumFeature.ADVANCED_ANALYTICS,
                PremiumFeature.CUSTOM_THEMES
            )
            
            for (feature in premiumFeatures) {
                if (premiumFeatureManager.canUseFeature(feature)) {
                    println("❌ Should not be able to use ${feature.displayName} after trial expiration")
                    return false
                }
            }
            
            // Test free features are still accessible
            if (!premiumFeatureManager.canUseFeature(PremiumFeature.BASIC_USAGE_TRACKING)) {
                println("❌ Should still be able to use basic features after trial expiration")
                return false
            }
            
            // Test upgrade prompts
            if (!trialManager.shouldShowUpgradePrompt()) {
                println("❌ Should show upgrade prompt after trial expiration")
                return false
            }
            
            println("✅ Trial expiration test passed")
            true
            
        } catch (e: Exception) {
            println("❌ Trial expiration test failed: ${e.message}")
            false
        }
    }
    
    /**
     * Test integration with subscription service
     */
    private suspend fun testSubscriptionIntegration(): Boolean {
        println("🧪 Testing subscription service integration...")
        
        return try {
            // Reset and activate trial
            trialManager.resetTrial()
            trialManager.activateTrial()
            
            // Test subscription service recognizes trial
            val accessInfo = subscriptionService.getPremiumStatusInfo()
            
            if (!accessInfo.hasActiveTrial) {
                println("❌ Subscription service should recognize active trial")
                return false
            }
            
            if (!accessInfo.hasAnyPremiumAccess) {
                println("❌ Should have premium access during trial")
                return false
            }
            
            if (accessInfo.trialDaysRemaining <= 0) {
                println("❌ Should have trial days remaining")
                return false
            }
            
            // Test feature access through subscription service
            if (!subscriptionService.canUseFeature(PremiumFeature.MULTIPLE_APP_GROUPS)) {
                println("❌ Subscription service should allow premium features during trial")
                return false
            }
            
            // End trial and test again
            trialManager.endTrial()
            
            val expiredAccessInfo = subscriptionService.getPremiumStatusInfo()
            if (expiredAccessInfo.hasActiveTrial) {
                println("❌ Should not have active trial after expiration")
                return false
            }
            
            if (expiredAccessInfo.shouldShowUpgradePrompt != true) {
                println("❌ Should show upgrade prompt after trial expiration")
                return false
            }
            
            println("✅ Subscription integration test passed")
            true
            
        } catch (e: Exception) {
            println("❌ Subscription integration test failed: ${e.message}")
            false
        }
    }
    
    /**
     * Test UI state management with Compose integration
     */
    private suspend fun testUIStateManagement(): Boolean {
        println("🧪 Testing UI state management...")
        
        return try {
            // Reset and activate trial
            trialManager.resetTrial()
            trialManager.activateTrial()
            
            // Test trial status flow
            val status = trialManager.trialStatus.first()
            if (status != TrialStatus.ACTIVE) {
                println("❌ Trial status flow should show ACTIVE")
                return false
            }
            
            // Test days remaining flow
            val daysRemaining = trialManager.daysRemaining.first()
            if (daysRemaining != PremiumTrialManager.TRIAL_DURATION_DAYS) {
                println("❌ Days remaining flow should show ${PremiumTrialManager.TRIAL_DURATION_DAYS}")
                return false
            }
            
            // Test premium status flow
            val premiumStatus = premiumFeatureManager.isPremiumActive.first()
            if (!premiumStatus) {
                println("❌ Premium status flow should show active during trial")
                return false
            }
            
            println("✅ UI state management test passed")
            true
            
        } catch (e: Exception) {
            println("❌ UI state management test failed: ${e.message}")
            false
        }
    }
    
    /**
     * Test notification system
     */
    private suspend fun testNotificationSystem(): Boolean {
        println("🧪 Testing notification system...")
        
        return try {
            // This test verifies notification methods don't crash
            // In a real test environment, you'd verify actual notifications
            
            val notificationService = NotificationService(context)
            
            // Test trial notifications don't crash
            notificationService.showTrialActivatedNotification()
            notificationService.showTrialReminderNotification("Test reminder", 24)
            notificationService.showTrialExpiredNotification()
            notificationService.showUpgradePromptNotification(1)
            
            // Test cancellation
            notificationService.cancelTrialNotifications()
            
            println("✅ Notification system test passed")
            true
            
        } catch (e: Exception) {
            println("❌ Notification system test failed: ${e.message}")
            false
        }
    }
    
    /**
     * Test data persistence across app restarts
     */
    private suspend fun testDataPersistence(): Boolean {
        println("🧪 Testing data persistence...")
        
        return try {
            // Reset trial
            trialManager.resetTrial()
            
            // Activate trial
            trialManager.activateTrial()
            val originalStartDate = trialManager.getTrialStartDate()
            
            // Create new trial manager instance (simulates app restart)
            val newTrialManager = PremiumTrialManager.getInstance(context)
            
            // Verify trial data persisted
            if (!newTrialManager.isTrialActive()) {
                println("❌ Trial activation should persist across restarts")
                return false
            }
            
            val persistedStartDate = newTrialManager.getTrialStartDate()
            if (originalStartDate != persistedStartDate) {
                println("❌ Trial start date should persist")
                return false
            }
            
            // Test trial ending persistence
            newTrialManager.endTrial()
            
            val anotherTrialManager = PremiumTrialManager.getInstance(context)
            if (!anotherTrialManager.isTrialEnded()) {
                println("❌ Trial ended status should persist")
                return false
            }
            
            println("✅ Data persistence test passed")
            true
            
        } catch (e: Exception) {
            println("❌ Data persistence test failed: ${e.message}")
            false
        }
    }
    
    /**
     * Generate detailed test report
     */
    fun generateTestReport(results: TrialTestResults): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timestamp = formatter.format(Date())
        
        return buildString {
            appendLine("=".repeat(60))
            appendLine("PREMIUM TRIAL SYSTEM TEST REPORT")
            appendLine("Generated: $timestamp")
            appendLine("=".repeat(60))
            appendLine()
            
            appendLine("OVERALL RESULT: ${if (results.overallSuccess) "✅ PASSED" else "❌ FAILED"}")
            appendLine()
            
            appendLine("DETAILED RESULTS:")
            appendLine("1. New User Detection: ${if (results.newUserDetectionTest) "✅ PASSED" else "❌ FAILED"}")
            appendLine("2. Trial Status: ${if (results.trialStatusTest) "✅ PASSED" else "❌ FAILED"}")
            appendLine("3. Premium Feature Access: ${if (results.premiumFeatureAccessTest) "✅ PASSED" else "❌ FAILED"}")
            appendLine("4. Trial Expiration: ${if (results.trialExpirationTest) "✅ PASSED" else "❌ FAILED"}")
            appendLine("5. Subscription Integration: ${if (results.subscriptionIntegrationTest) "✅ PASSED" else "❌ FAILED"}")
            appendLine("6. UI State Management: ${if (results.uiStateTest) "✅ PASSED" else "❌ FAILED"}")
            appendLine("7. Notification System: ${if (results.notificationTest) "✅ PASSED" else "❌ FAILED"}")
            appendLine("8. Data Persistence: ${if (results.dataPersistenceTest) "✅ PASSED" else "❌ FAILED"}")
            appendLine()
            
            appendLine("SYSTEM INFORMATION:")
            appendLine("Trial Duration: ${PremiumTrialManager.TRIAL_DURATION_DAYS} days")
            
            try {
                val debugInfo = trialManager.getTrialDebugInfo()
                appendLine()
                appendLine("CURRENT TRIAL STATE:")
                appendLine(debugInfo)
            } catch (e: Exception) {
                appendLine("Could not retrieve trial debug info: ${e.message}")
            }
            
            appendLine("=".repeat(60))
        }
    }
    
    /**
     * Quick smoke test for basic functionality
     */
    suspend fun quickSmokeTest(): Boolean {
        println("🧪 Running quick smoke test...")
        
        return try {
            // Reset trial
            trialManager.resetTrial()
            
            // Test activation
            trialManager.activateTrial()
            if (!trialManager.isTrialActive()) return false
            
            // Test feature access
            if (!premiumFeatureManager.canUseFeature(PremiumFeature.MULTIPLE_APP_GROUPS)) return false
            
            // Test expiration
            trialManager.endTrial()
            if (premiumFeatureManager.canUseFeature(PremiumFeature.MULTIPLE_APP_GROUPS)) return false
            
            println("✅ Quick smoke test passed")
            true
            
        } catch (e: Exception) {
            println("❌ Quick smoke test failed: ${e.message}")
            false
        }
    }
}

/**
 * Results of trial system tests
 */
data class TrialTestResults(
    var newUserDetectionTest: Boolean = false,
    var trialStatusTest: Boolean = false,
    var premiumFeatureAccessTest: Boolean = false,
    var trialExpirationTest: Boolean = false,
    var subscriptionIntegrationTest: Boolean = false,
    var uiStateTest: Boolean = false,
    var notificationTest: Boolean = false,
    var dataPersistenceTest: Boolean = false,
    var overallSuccess: Boolean = false
) {
    fun getAllTests(): List<Boolean> {
        return listOf(
            newUserDetectionTest,
            trialStatusTest,
            premiumFeatureAccessTest,
            trialExpirationTest,
            subscriptionIntegrationTest,
            uiStateTest,
            notificationTest,
            dataPersistenceTest
        )
    }
    
    fun getPassedCount(): Int = getAllTests().count { it }
    fun getTotalCount(): Int = getAllTests().size
    fun getSuccessRate(): Float = getPassedCount().toFloat() / getTotalCount().toFloat()
}