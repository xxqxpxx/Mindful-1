package com.awaytime.app

import com.awaytime.app.service.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CoreFunctionalityTest {
    
    private lateinit var usageTracker: AwayTimeTracker
    private lateinit var appBlocker: AwayTimeBlocker
    private lateinit var goalTrackingService: GoalTrackingService
    private lateinit var context: android.content.Context
    
    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        usageTracker = AwayTimeTracker(context)
        appBlocker = AwayTimeBlocker(context)
        goalTrackingService = GoalTrackingService(context)
    }
    
    @After
    fun tearDown() {
        // Clean up test data
        usageTracker.clearAllData()
        appBlocker.clearAllData()
        goalTrackingService.clearAllData()
    }
    
    // MARK: - Usage Tracking Tests
    
    @Test
    fun testUsageTrackingInitialization() {
        assertNotNull(usageTracker)
        assertEquals(0, usageTracker.getTodayUsage())
        assertFalse(usageTracker.isLimitReached())
    }
    
    @Test
    fun testUsageDataPersistence() = runTest {
        val testUsage = 120 // 2 hours in minutes
        usageTracker.recordUsage(testUsage)
        
        assertEquals(testUsage, usageTracker.getTodayUsage())
        
        // Simulate app restart
        val newTracker = AwayTimeTracker(context)
        assertEquals(testUsage, newTracker.getTodayUsage())
    }
    
    @Test
    fun testDailyUsageReset() = runTest {
        val testUsage = 180 // 3 hours
        usageTracker.recordUsage(testUsage)
        
        // Simulate midnight reset
        usageTracker.resetDailyUsage()
        
        assertEquals(0, usageTracker.getTodayUsage())
        assertFalse(usageTracker.isLimitReached())
    }
    
    @Test
    fun testUsageLimitDetection() = runTest {
        val dailyLimit = 120 // 2 hours
        usageTracker.setDailyLimit(dailyLimit)
        
        // Test below limit
        usageTracker.recordUsage(60)
        assertFalse(usageTracker.isLimitReached())
        
        // Test at limit
        usageTracker.recordUsage(60) // Total: 120
        assertTrue(usageTracker.isLimitReached())
        
        // Test above limit
        usageTracker.recordUsage(30) // Total: 150
        assertTrue(usageTracker.isLimitReached())
    }
    
    @Test
    fun testUsageProgressCalculation() = runTest {
        val dailyLimit = 120 // 2 hours
        usageTracker.setDailyLimit(dailyLimit)
        
        // Test 50% progress
        usageTracker.recordUsage(60)
        assertEquals(0.5f, usageTracker.getUsageProgress(), 0.01f)
        
        // Test 100% progress
        usageTracker.recordUsage(60) // Total: 120
        assertEquals(1.0f, usageTracker.getUsageProgress(), 0.01f)
        
        // Test over 100% progress
        usageTracker.recordUsage(30) // Total: 150
        assertEquals(1.25f, usageTracker.getUsageProgress(), 0.01f)
    }
    
    // MARK: - App Blocking Tests
    
    @Test
    fun testAppBlockingInitialization() {
        assertNotNull(appBlocker)
        assertFalse(appBlocker.isBlocked())
    }
    
    @Test
    fun testAppBlockingActivation() = runTest {
        val testApps = listOf("com.instagram.android", "com.zhiliaoapp.musically")
        
        appBlocker.blockApps(testApps)
        assertTrue(appBlocker.isBlocked())
        
        appBlocker.unblockApps()
        assertFalse(appBlocker.isBlocked())
    }
    
    @Test
    fun testBlockingPersistence() = runTest {
        val testApps = listOf("com.instagram.android")
        appBlocker.blockApps(testApps)
        
        // Simulate app restart
        val newBlocker = AwayTimeBlocker(context)
        
        // Blocking should persist
        assertTrue(newBlocker.isBlocked())
    }
    
    @Test
    fun testBlockedAppsRetrieval() = runTest {
        val testApps = listOf("com.instagram.android", "com.zhiliaoapp.musically")
        appBlocker.blockApps(testApps)
        
        val blockedApps = appBlocker.getBlockedApps()
        assertEquals(testApps.size, blockedApps.size)
        assertTrue(blockedApps.containsAll(testApps))
    }
    
    // MARK: - Goal Tracking Tests
    
    @Test
    fun testGoalTrackingInitialization() {
        assertNotNull(goalTrackingService)
        assertEquals(0, goalTrackingService.currentStreak)
        assertEquals(0, goalTrackingService.longestStreak)
    }
    
    @Test
    fun testStreakCalculation() = runTest {
        // Test successful day
        goalTrackingService.recordDayResult(true)
        assertEquals(1, goalTrackingService.currentStreak)
        assertEquals(1, goalTrackingService.longestStreak)
        
        // Test another successful day
        goalTrackingService.recordDayResult(true)
        assertEquals(2, goalTrackingService.currentStreak)
        assertEquals(2, goalTrackingService.longestStreak)
        
        // Test failed day
        goalTrackingService.recordDayResult(false)
        assertEquals(0, goalTrackingService.currentStreak)
        assertEquals(2, goalTrackingService.longestStreak) // Should maintain longest
    }
    
    @Test
    fun testWeeklyProgressTracking() = runTest {
        val weeklyData = listOf(true, false, true, true, false, true, true)
        
        weeklyData.forEachIndexed { index, success ->
            goalTrackingService.recordDayResult(success, daysAgo = 6 - index)
        }
        
        val progress = goalTrackingService.weeklyProgress
        assertEquals(7, progress.size)
        assertEquals(weeklyData, progress)
    }
    
    @Test
    fun testGoalRecommendations() = runTest {
        // Test excellent performance
        val excellentRecommendation = goalTrackingService.getGoalRecommendation(
            currentUsage = 60, // 1 hour
            currentLimit = 120 // 2 hours
        )
        assertEquals(GoalRecommendation.RecommendationType.EXCELLENT, excellentRecommendation.type)
        
        // Test warning threshold
        val warningRecommendation = goalTrackingService.getGoalRecommendation(
            currentUsage = 100, // 1h 40m
            currentLimit = 120 // 2 hours
        )
        assertEquals(GoalRecommendation.RecommendationType.WARNING, warningRecommendation.type)
        
        // Test exceeded limit
        val exceededRecommendation = goalTrackingService.getGoalRecommendation(
            currentUsage = 150, // 2.5 hours
            currentLimit = 120 // 2 hours
        )
        assertEquals(GoalRecommendation.RecommendationType.EXCEEDED, exceededRecommendation.type)
    }
    
    // MARK: - Data Validation Tests
    
    @Test
    fun testUsageDataValidation() = runTest {
        // Test negative usage handling
        usageTracker.recordUsage(-30)
        assertEquals(0, usageTracker.getTodayUsage())
        
        // Test extremely large usage values
        usageTracker.recordUsage(10000) // ~7 days
        assertTrue(usageTracker.getTodayUsage() <= 1440) // Max 24 hours
    }
    
    @Test
    fun testLimitValidation() = runTest {
        // Test minimum limit
        usageTracker.setDailyLimit(15) // Below minimum
        assertTrue(usageTracker.getDailyLimit() >= 30) // Should be clamped to 30min
        
        // Test maximum limit
        usageTracker.setDailyLimit(600) // 10 hours, above maximum
        assertTrue(usageTracker.getDailyLimit() <= 480) // Should be clamped to 8 hours
    }
    
    @Test
    fun testAppPackageValidation() = runTest {
        // Test invalid package names
        val invalidApps = listOf("", "invalid.package", "com.nonexistent.app")
        appBlocker.blockApps(invalidApps)
        
        // Should handle gracefully without crashing
        assertNotNull(appBlocker.getBlockedApps())
    }
    
    // MARK: - Performance Tests
    
    @Test
    fun testUsageTrackingPerformance() = runTest {
        val startTime = System.currentTimeMillis()
        
        repeat(1000) {
            usageTracker.recordUsage(1)
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        // Should complete within reasonable time (less than 1 second)
        assertTrue("Usage tracking took too long: ${duration}ms", duration < 1000)
    }
    
    @Test
    fun testGoalCalculationPerformance() = runTest {
        // Setup large dataset
        repeat(365) { i ->
            goalTrackingService.recordDayResult(i % 3 != 0, daysAgo = i)
        }
        
        val startTime = System.currentTimeMillis()
        
        // Perform calculations
        goalTrackingService.getGoalRecommendation(90, 120)
        goalTrackingService.weeklyProgress
        goalTrackingService.currentStreak
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        // Should complete quickly even with large dataset
        assertTrue("Goal calculations took too long: ${duration}ms", duration < 500)
    }
    
    // MARK: - Edge Case Tests
    
    @Test
    fun testMidnightTransition() = runTest {
        val testUsage = 100
        usageTracker.recordUsage(testUsage)
        usageTracker.setDailyLimit(120)
        
        // Block apps due to approaching limit
        val testApps = listOf("com.test.app")
        if (usageTracker.getTodayUsage() >= (usageTracker.getDailyLimit() * 0.8).toInt()) {
            appBlocker.blockApps(testApps)
        }
        
        assertTrue(appBlocker.isBlocked())
        
        // Simulate midnight reset
        usageTracker.resetDailyUsage()
        appBlocker.unblockApps()
        
        assertEquals(0, usageTracker.getTodayUsage())
        assertFalse(appBlocker.isBlocked())
    }
    
    @Test
    fun testRapidUsageUpdates() = runTest {
        val initialUsage = usageTracker.getTodayUsage()
        
        // Simulate rapid app switching
        repeat(100) {
            usageTracker.recordUsage(1)
        }
        
        val finalUsage = usageTracker.getTodayUsage()
        assertEquals(100, finalUsage - initialUsage)
    }
    
    @Test
    fun testConcurrentAccess() = runTest {
        // Test concurrent access to usage tracker
        val jobs = (1..10).map { i ->
            kotlinx.coroutines.async {
                usageTracker.recordUsage(i)
            }
        }
        
        // Wait for all jobs to complete
        jobs.forEach { it.await() }
        
        // Verify data integrity
        val totalUsage = usageTracker.getTodayUsage()
        assertTrue("Total usage should be greater than 0", totalUsage > 0)
        assertTrue("Total usage should not exceed sum", totalUsage <= 55) // Sum of 1+2+...+10 = 55
    }
    
    // MARK: - Notification Tests
    
    @Test
    fun testNotificationTriggers() = runTest {
        val notificationService = AwayTimeNotifications(context)
        usageTracker.setDailyLimit(120) // 2 hours
        
        // Test warning notification trigger (80% threshold)
        usageTracker.recordUsage(96) // 80% of 120 minutes
        val shouldTriggerWarning = usageTracker.getTodayUsage() >= (usageTracker.getDailyLimit() * 0.8).toInt()
        assertTrue("Warning notification should trigger at 80%", shouldTriggerWarning)
        
        // Test limit reached notification
        usageTracker.recordUsage(24) // Total: 120 minutes
        assertTrue("Limit reached notification should trigger", usageTracker.isLimitReached())
    }
    
    // MARK: - Data Persistence Tests
    
    @Test
    fun testDataPersistenceAcrossRestarts() = runTest {
        // Set up initial data
        usageTracker.recordUsage(90)
        usageTracker.setDailyLimit(120)
        goalTrackingService.recordDayResult(true)
        appBlocker.blockApps(listOf("com.test.app"))
        
        // Create new instances (simulating app restart)
        val newUsageTracker = AwayTimeTracker(context)
        val newGoalService = GoalTrackingService(context)
        val newAppBlocker = AwayTimeBlocker(context)
        
        // Verify data persisted
        assertEquals(90, newUsageTracker.getTodayUsage())
        assertEquals(120, newUsageTracker.getDailyLimit())
        assertEquals(1, newGoalService.currentStreak)
        assertTrue(newAppBlocker.isBlocked())
    }
    
    @Test
    fun testDataCleanup() = runTest {
        // Add test data
        usageTracker.recordUsage(100)
        goalTrackingService.recordDayResult(true)
        appBlocker.blockApps(listOf("com.test.app"))
        
        // Clear all data
        usageTracker.clearAllData()
        goalTrackingService.clearAllData()
        appBlocker.clearAllData()
        
        // Verify data is cleared
        assertEquals(0, usageTracker.getTodayUsage())
        assertEquals(0, goalTrackingService.currentStreak)
        assertFalse(appBlocker.isBlocked())
    }
}