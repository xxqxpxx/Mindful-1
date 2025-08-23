import XCTest
@testable import Awaytime

class CoreFunctionalityTests: XCTestCase {
    
    var usageTracker: AwayTimeTracker!
    var appBlocker: AwayTimeBlocker!
    var goalTrackingService: GoalTrackingService!
    
    override func setUpWithError() throws {
        usageTracker = AwayTimeTracker()
        appBlocker = AwayTimeBlocker()
        goalTrackingService = GoalTrackingService()
    }
    
    override func tearDownWithError() throws {
        usageTracker = nil
        appBlocker = nil
        goalTrackingService = nil
    }
    
    // MARK: - Usage Tracking Tests
    
    func testUsageTrackingInitialization() throws {
        XCTAssertNotNil(usageTracker)
        XCTAssertEqual(usageTracker.getTodayUsage(), 0)
        XCTAssertFalse(usageTracker.isLimitReached())
    }
    
    func testUsageDataPersistence() throws {
        // Test that usage data persists across app launches
        let testUsage = 120 // 2 hours in minutes
        usageTracker.recordUsage(minutes: testUsage)
        
        XCTAssertEqual(usageTracker.getTodayUsage(), testUsage)
        
        // Simulate app restart
        usageTracker = AwayTimeTracker()
        XCTAssertEqual(usageTracker.getTodayUsage(), testUsage)
    }
    
    func testDailyUsageReset() throws {
        // Test that usage resets at midnight
        let testUsage = 180 // 3 hours
        usageTracker.recordUsage(minutes: testUsage)
        
        // Simulate midnight reset
        usageTracker.resetDailyUsage()
        
        XCTAssertEqual(usageTracker.getTodayUsage(), 0)
        XCTAssertFalse(usageTracker.isLimitReached())
    }
    
    func testUsageLimitDetection() throws {
        let dailyLimit = 120 // 2 hours
        usageTracker.setDailyLimit(minutes: dailyLimit)
        
        // Test below limit
        usageTracker.recordUsage(minutes: 60)
        XCTAssertFalse(usageTracker.isLimitReached())
        
        // Test at limit
        usageTracker.recordUsage(minutes: 60) // Total: 120
        XCTAssertTrue(usageTracker.isLimitReached())
        
        // Test above limit
        usageTracker.recordUsage(minutes: 30) // Total: 150
        XCTAssertTrue(usageTracker.isLimitReached())
    }
    
    // MARK: - App Blocking Tests
    
    func testAppBlockingInitialization() throws {
        XCTAssertNotNil(appBlocker)
        XCTAssertFalse(appBlocker.isBlocked())
    }
    
    func testAppBlockingActivation() throws {
        let testApps = ["com.instagram.app", "com.tiktok.app"]
        
        appBlocker.blockApps(testApps)
        XCTAssertTrue(appBlocker.isBlocked())
        
        appBlocker.unblockApps()
        XCTAssertFalse(appBlocker.isBlocked())
    }
    
    func testBlockingPersistence() throws {
        let testApps = ["com.instagram.app"]
        appBlocker.blockApps(testApps)
        
        // Simulate app restart
        appBlocker = AwayTimeBlocker()
        
        // Blocking should persist
        XCTAssertTrue(appBlocker.isBlocked())
    }
    
    // MARK: - Goal Tracking Tests
    
    func testGoalTrackingInitialization() throws {
        XCTAssertNotNil(goalTrackingService)
        XCTAssertEqual(goalTrackingService.currentStreak, 0)
        XCTAssertEqual(goalTrackingService.longestStreak, 0)
    }
    
    func testStreakCalculation() throws {
        // Test successful day
        goalTrackingService.recordDayResult(success: true)
        XCTAssertEqual(goalTrackingService.currentStreak, 1)
        XCTAssertEqual(goalTrackingService.longestStreak, 1)
        
        // Test another successful day
        goalTrackingService.recordDayResult(success: true)
        XCTAssertEqual(goalTrackingService.currentStreak, 2)
        XCTAssertEqual(goalTrackingService.longestStreak, 2)
        
        // Test failed day
        goalTrackingService.recordDayResult(success: false)
        XCTAssertEqual(goalTrackingService.currentStreak, 0)
        XCTAssertEqual(goalTrackingService.longestStreak, 2) // Should maintain longest
    }
    
    func testWeeklyProgressTracking() throws {
        // Test weekly progress calculation
        let weeklyData = [true, false, true, true, false, true, true]
        
        for (index, success) in weeklyData.enumerated() {
            goalTrackingService.recordDayResult(success: success, daysAgo: 6 - index)
        }
        
        let progress = goalTrackingService.weeklyProgress
        XCTAssertEqual(progress.count, 7)
        XCTAssertEqual(progress, weeklyData)
    }
    
    func testGoalRecommendations() throws {
        // Test excellent performance
        let excellentRecommendation = goalTrackingService.getGoalRecommendation(
            currentUsage: 60, // 1 hour
            currentLimit: 120 // 2 hours
        )
        XCTAssertEqual(excellentRecommendation.type, .excellent)
        
        // Test warning threshold
        let warningRecommendation = goalTrackingService.getGoalRecommendation(
            currentUsage: 100, // 1h 40m
            currentLimit: 120 // 2 hours
        )
        XCTAssertEqual(warningRecommendation.type, .warning)
        
        // Test exceeded limit
        let exceededRecommendation = goalTrackingService.getGoalRecommendation(
            currentUsage: 150, // 2.5 hours
            currentLimit: 120 // 2 hours
        )
        XCTAssertEqual(exceededRecommendation.type, .exceeded)
    }
    
    // MARK: - Data Validation Tests
    
    func testUsageDataValidation() throws {
        // Test negative usage handling
        usageTracker.recordUsage(minutes: -30)
        XCTAssertEqual(usageTracker.getTodayUsage(), 0)
        
        // Test extremely large usage values
        usageTracker.recordUsage(minutes: 10000) // ~7 days
        XCTAssertLessThanOrEqual(usageTracker.getTodayUsage(), 1440) // Max 24 hours
    }
    
    func testLimitValidation() throws {
        // Test minimum limit
        usageTracker.setDailyLimit(minutes: 15) // Below minimum
        XCTAssertGreaterThanOrEqual(usageTracker.getDailyLimit(), 30) // Should be clamped to 30min
        
        // Test maximum limit
        usageTracker.setDailyLimit(minutes: 600) // 10 hours, above maximum
        XCTAssertLessThanOrEqual(usageTracker.getDailyLimit(), 480) // Should be clamped to 8 hours
    }
    
    // MARK: - Performance Tests
    
    func testUsageTrackingPerformance() throws {
        measure {
            for _ in 0..<1000 {
                usageTracker.recordUsage(minutes: 1)
            }
        }
    }
    
    func testGoalCalculationPerformance() throws {
        // Setup large dataset
        for i in 0..<365 {
            goalTrackingService.recordDayResult(success: i % 3 != 0, daysAgo: i)
        }
        
        measure {
            _ = goalTrackingService.getGoalRecommendation(currentUsage: 90, currentLimit: 120)
            _ = goalTrackingService.weeklyProgress
            _ = goalTrackingService.currentStreak
        }
    }
    
    // MARK: - Edge Case Tests
    
    func testMidnightTransition() throws {
        // Test behavior during midnight transition
        let testUsage = 100
        usageTracker.recordUsage(minutes: testUsage)
        usageTracker.setDailyLimit(minutes: 120)
        
        // Block apps due to approaching limit
        let testApps = ["com.test.app"]
        if usageTracker.getTodayUsage() >= Int(Double(usageTracker.getDailyLimit()) * 0.8) {
            appBlocker.blockApps(testApps)
        }
        
        XCTAssertTrue(appBlocker.isBlocked())
        
        // Simulate midnight reset
        usageTracker.resetDailyUsage()
        appBlocker.unblockApps()
        
        XCTAssertEqual(usageTracker.getTodayUsage(), 0)
        XCTAssertFalse(appBlocker.isBlocked())
    }
    
    func testRapidUsageUpdates() throws {
        // Test handling of rapid usage updates
        let initialUsage = usageTracker.getTodayUsage()
        
        // Simulate rapid app switching
        for _ in 0..<100 {
            usageTracker.recordUsage(minutes: 1)
        }
        
        let finalUsage = usageTracker.getTodayUsage()
        XCTAssertEqual(finalUsage - initialUsage, 100)
    }
    
    func testConcurrentAccess() throws {
        let expectation = XCTestExpectation(description: "Concurrent usage tracking")
        expectation.expectedFulfillmentCount = 10
        
        // Test concurrent access to usage tracker
        for i in 0..<10 {
            DispatchQueue.global().async {
                self.usageTracker.recordUsage(minutes: i + 1)
                expectation.fulfill()
            }
        }
        
        wait(for: [expectation], timeout: 5.0)
        
        // Verify data integrity
        let totalUsage = usageTracker.getTodayUsage()
        XCTAssertGreaterThan(totalUsage, 0)
        XCTAssertLessThanOrEqual(totalUsage, 55) // Sum of 1+2+...+10 = 55
    }
}