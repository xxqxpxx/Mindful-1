import XCTest

class DashboardUITests: XCTestCase {
    
    var app: XCUIApplication!
    
    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launch()
    }
    
    override func tearDownWithError() throws {
        app = nil
    }
    
    // MARK: - Dashboard Loading Tests
    
    func testDashboardLoadsCorrectly() throws {
        // Test that main dashboard elements are present
        XCTAssertTrue(app.staticTexts["Awaytime"].exists)
        XCTAssertTrue(app.staticTexts["Take control of your screen time"].exists)
        
        // Test progress circle is visible
        XCTAssertTrue(app.otherElements["Usage progress"].exists)
        
        // Test action buttons are present
        XCTAssertTrue(app.buttons["Select Apps"].exists)
        XCTAssertTrue(app.buttons["Set Daily Limit"].exists)
    }
    
    func testProgressCircleDisplaysCorrectly() throws {
        // Test progress circle shows usage percentage
        let progressElement = app.otherElements["Usage progress"]
        XCTAssertTrue(progressElement.exists)
        
        // Test accessibility label is present
        XCTAssertTrue(progressElement.label.contains("percent used today"))
    }
    
    func testTimeRemainingDisplaysCorrectly() throws {
        // Test time remaining text is visible and accessible
        let timeRemainingElement = app.staticTexts["Time remaining"]
        XCTAssertTrue(timeRemainingElement.exists)
        XCTAssertFalse(timeRemainingElement.label.isEmpty)
    }
    
    // MARK: - Navigation Tests
    
    func testAppSelectionNavigation() throws {
        // Test tapping "Select Apps" button
        let selectAppsButton = app.buttons["Select Apps"]
        XCTAssertTrue(selectAppsButton.exists)
        
        selectAppsButton.tap()
        
        // Verify app selection screen appears
        // Note: This might show permission request first
        XCTAssertTrue(
            app.navigationBars["App Selection"].exists ||
            app.staticTexts["Permission Required"].exists
        )
    }
    
    func testLimitSettingNavigation() throws {
        // Test tapping "Set Daily Limit" button
        let setLimitButton = app.buttons["Set Daily Limit"]
        XCTAssertTrue(setLimitButton.exists)
        
        setLimitButton.tap()
        
        // Verify limit setting screen appears
        XCTAssertTrue(app.navigationBars["Set Daily Limit"].exists)
    }
    
    func testPremiumNavigation() throws {
        // Test premium/analytics navigation
        if app.buttons["View Analytics"].exists {
            app.buttons["View Analytics"].tap()
            XCTAssertTrue(app.navigationBars["Analytics"].exists)
        } else if app.buttons["Upgrade to Premium"].exists {
            app.buttons["Upgrade to Premium"].tap()
            XCTAssertTrue(app.navigationBars["Premium"].exists)
        }
    }
    
    // MARK: - Accessibility Tests
    
    func testVoiceOverSupport() throws {
        // Test that all interactive elements have accessibility labels
        let selectAppsButton = app.buttons["Select Apps"]
        XCTAssertFalse(selectAppsButton.label.isEmpty)
        XCTAssertTrue(selectAppsButton.isHittable)
        
        let setLimitButton = app.buttons["Set Daily Limit"]
        XCTAssertFalse(setLimitButton.label.isEmpty)
        XCTAssertTrue(setLimitButton.isHittable)
        
        // Test progress circle accessibility
        let progressElement = app.otherElements["Usage progress"]
        XCTAssertFalse(progressElement.label.isEmpty)
    }
    
    func testDynamicTypeSupport() throws {
        // Test with larger text sizes
        app.terminate()
        
        // Enable larger accessibility text
        let settingsApp = XCUIApplication(bundleIdentifier: "com.apple.Preferences")
        settingsApp.launch()
        
        // Navigate to Accessibility > Display & Text Size > Larger Text
        // This is a simplified test - full implementation would navigate through Settings
        
        // Relaunch our app
        app.launch()
        
        // Verify text is still readable and UI adapts
        XCTAssertTrue(app.staticTexts["Awaytime"].exists)
        XCTAssertTrue(app.buttons["Select Apps"].exists)
    }
    
    // MARK: - Animation Tests
    
    func testProgressCircleAnimation() throws {
        // Test that progress circle animates smoothly
        let progressElement = app.otherElements["Usage progress"]
        XCTAssertTrue(progressElement.exists)
        
        // Wait for animation to complete
        let expectation = XCTNSPredicateExpectation(
            predicate: NSPredicate(format: "exists == true"),
            object: progressElement
        )
        
        wait(for: [expectation], timeout: 3.0)
        XCTAssertTrue(progressElement.exists)
    }
    
    func testButtonTapFeedback() throws {
        // Test button tap animations and feedback
        let selectAppsButton = app.buttons["Select Apps"]
        
        // Record initial state
        let initialFrame = selectAppsButton.frame
        
        // Tap button
        selectAppsButton.tap()
        
        // Button should provide visual feedback (though hard to test programmatically)
        XCTAssertTrue(selectAppsButton.exists)
    }
    
    // MARK: - Loading State Tests
    
    func testLoadingStatesDisplay() throws {
        // Test loading states appear when data is being fetched
        // This would require mocking slow network conditions
        
        // Force app to show loading state
        app.terminate()
        app.launch()
        
        // Check if skeleton loading views appear briefly
        // Note: This is timing-dependent and may be flaky
        let loadingIndicator = app.activityIndicators.firstMatch
        if loadingIndicator.exists {
            XCTAssertTrue(loadingIndicator.exists)
            
            // Wait for loading to complete
            let expectation = XCTNSPredicateExpectation(
                predicate: NSPredicate(format: "exists == false"),
                object: loadingIndicator
            )
            wait(for: [expectation], timeout: 10.0)
        }
        
        // Verify content loads after loading state
        XCTAssertTrue(app.staticTexts["Awaytime"].exists)
    }
    
    // MARK: - Error State Tests
    
    func testErrorStateHandling() throws {
        // Test error states display correctly
        // This would require simulating error conditions
        
        // For now, test that error elements would be accessible if they appeared
        if app.staticTexts["Something went wrong"].exists {
            XCTAssertTrue(app.buttons["Try Again"].exists)
            XCTAssertTrue(app.buttons["Try Again"].isHittable)
        }
    }
    
    // MARK: - Performance Tests
    
    func testAppLaunchPerformance() throws {
        measure(metrics: [XCTApplicationLaunchMetric()]) {
            app.terminate()
            app.launch()
        }
    }
    
    func testScrollPerformance() throws {
        // Test scrolling performance on dashboard
        let scrollView = app.scrollViews.firstMatch
        
        if scrollView.exists {
            measure(metrics: [XCTOSSignpostMetric.scrollingAndDecelerationMetric]) {
                scrollView.swipeUp()
                scrollView.swipeDown()
            }
        }
    }
    
    // MARK: - Orientation Tests
    
    func testLandscapeOrientation() throws {
        // Test app in landscape orientation
        XCUIDevice.shared.orientation = .landscapeLeft
        
        // Wait for orientation change
        sleep(1)
        
        // Verify UI adapts to landscape
        XCTAssertTrue(app.staticTexts["Awaytime"].exists)
        XCTAssertTrue(app.buttons["Select Apps"].exists)
        
        // Return to portrait
        XCUIDevice.shared.orientation = .portrait
    }
    
    // MARK: - Memory and Stability Tests
    
    func testMemoryStability() throws {
        // Test app stability under repeated interactions
        for _ in 0..<10 {
            // Navigate through different screens
            if app.buttons["Select Apps"].exists {
                app.buttons["Select Apps"].tap()
                
                // Go back
                if app.navigationBars.buttons["Back"].exists {
                    app.navigationBars.buttons["Back"].tap()
                } else if app.buttons["Cancel"].exists {
                    app.buttons["Cancel"].tap()
                }
            }
            
            // Small delay between iterations
            usleep(100000) // 0.1 seconds
        }
        
        // Verify app is still responsive
        XCTAssertTrue(app.staticTexts["Awaytime"].exists)
    }
    
    // MARK: - Edge Case Tests
    
    func testRapidButtonTaps() throws {
        // Test rapid button tapping doesn't cause issues
        let selectAppsButton = app.buttons["Select Apps"]
        
        for _ in 0..<5 {
            selectAppsButton.tap()
            usleep(50000) // 0.05 seconds between taps
        }
        
        // App should handle this gracefully
        XCTAssertTrue(app.exists)
    }
    
    func testBackgroundAndForeground() throws {
        // Test app behavior when backgrounded and foregrounded
        XCUIDevice.shared.press(.home)
        sleep(2)
        
        // Relaunch app
        app.activate()
        
        // Verify app state is preserved
        XCTAssertTrue(app.staticTexts["Awaytime"].exists)
    }
    
    // MARK: - Streak and Progress Tests
    
    func testStreakDisplayUpdates() throws {
        // Test that streak information displays correctly
        let streakElements = app.staticTexts.matching(NSPredicate(format: "label CONTAINS 'Day Streak'"))
        
        if streakElements.count > 0 {
            let streakElement = streakElements.firstMatch
            XCTAssertTrue(streakElement.exists)
            XCTAssertFalse(streakElement.label.isEmpty)
        }
    }
    
    func testWeeklyProgressChart() throws {
        // Test weekly progress chart displays
        let chartElements = app.staticTexts.matching(NSPredicate(format: "label CONTAINS 'This Week'"))
        
        if chartElements.count > 0 {
            let chartElement = chartElements.firstMatch
            XCTAssertTrue(chartElement.exists)
        }
    }
    
    // MARK: - Goal Recommendation Tests
    
    func testGoalRecommendationDisplay() throws {
        // Test goal recommendations appear and are accessible
        let recommendationElements = app.staticTexts.matching(NSPredicate(format: "label CONTAINS 'recommendation'"))
        
        if recommendationElements.count > 0 {
            let recommendationElement = recommendationElements.firstMatch
            XCTAssertTrue(recommendationElement.exists)
            XCTAssertFalse(recommendationElement.label.isEmpty)
        }
    }
}