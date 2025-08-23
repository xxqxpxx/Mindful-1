# Awaytime App - Test Execution Script

## Overview
This script provides step-by-step instructions for executing comprehensive tests of the Awaytime app. It covers automated tests, manual testing procedures, and validation criteria.

## Pre-Test Setup

### Environment Preparation
1. **iOS Testing Environment**
   ```bash
   # Ensure Xcode is updated
   xcode-select --install
   
   # Clean build folder
   cd ios/
   xcodebuild clean -workspace Awaytime.xcworkspace -scheme Awaytime
   
   # Install dependencies
   pod install
   ```

2. **Android Testing Environment**
   ```bash
   # Ensure Android SDK is updated
   cd android/
   ./gradlew clean
   
   # Install dependencies
   ./gradlew build
   ```

3. **Test Device Setup**
   - iOS: iPhone with iOS 15+ (physical device preferred)
   - Android: Android device with API 26+ (physical device preferred)
   - Both devices should have clean state (no existing Awaytime installation)

## Automated Test Execution

### 1. iOS Unit Tests
```bash
cd ios/
xcodebuild test -workspace Awaytime.xcworkspace -scheme Awaytime -destination 'platform=iOS Simulator,name=iPhone 14'
```

**Expected Results:**
- All unit tests pass (0 failures)
- Test coverage >80% for core functionality
- Performance tests complete within acceptable limits

### 2. iOS UI Tests
```bash
xcodebuild test -workspace Awaytime.xcworkspace -scheme AwayTimeUITests -destination 'platform=iOS Simulator,name=iPhone 14'
```

**Expected Results:**
- All UI tests pass
- No accessibility violations
- Navigation flows work correctly

### 3. Android Unit Tests
```bash
cd android/
./gradlew test
```

**Expected Results:**
- All unit tests pass
- Code coverage >80%
- No memory leaks detected

### 4. Android UI Tests
```bash
./gradlew connectedAndroidTest
```

**Expected Results:**
- All Espresso tests pass
- UI components render correctly
- Interactions work as expected

## Manual Testing Execution

### Phase 1: Core Functionality Testing (30 minutes)

#### Test 1.1: First Launch and Onboarding
**Time Limit:** 5 minutes

1. **Install and Launch**
   - [ ] Install app from TestFlight/Internal Testing
   - [ ] Launch app for first time
   - [ ] Verify app opens within 3 seconds
   - [ ] Check that Awaytime branding displays correctly

2. **Permission Flow**
   - [ ] Follow permission request prompts
   - [ ] Verify clear explanations for each permission
   - [ ] Test permission denial scenario
   - [ ] Confirm graceful handling of denied permissions

3. **Initial Setup**
   - [ ] Complete app selection process
   - [ ] Set initial daily limit
   - [ ] Verify setup completes successfully
   - [ ] Check that dashboard loads with initial data

**Pass Criteria:**
- Setup completes in <3 minutes
- No crashes or errors
- User understands each step

#### Test 1.2: Usage Tracking Accuracy
**Time Limit:** 10 minutes

1. **Basic Tracking**
   - [ ] Open a monitored app for exactly 5 minutes
   - [ ] Return to Awaytime dashboard
   - [ ] Verify usage time is approximately 5 minutes (±30 seconds)
   - [ ] Check progress circle updates correctly

2. **Multiple App Tracking**
   - [ ] Use 3 different monitored apps for 2 minutes each
   - [ ] Verify total usage shows ~6 minutes
   - [ ] Check individual app breakdowns (if available)

3. **Background Tracking**
   - [ ] Use monitored app while Awaytime is backgrounded
   - [ ] Return to Awaytime after 3 minutes
   - [ ] Verify usage was tracked in background

**Pass Criteria:**
- Usage tracking accuracy within ±10%
- Real-time updates work correctly
- Background tracking functions properly

#### Test 1.3: App Blocking Functionality
**Time Limit:** 10 minutes

1. **Limit Setup**
   - [ ] Set daily limit to 15 minutes for testing
   - [ ] Use monitored apps to reach 80% threshold (12 minutes)
   - [ ] Verify warning notification appears

2. **Blocking Activation**
   - [ ] Continue using monitored apps to reach 100% limit
   - [ ] Verify apps are blocked immediately
   - [ ] Check blocking screen/message displays correctly
   - [ ] Attempt to bypass blocking (should fail)

3. **Blocking Persistence**
   - [ ] Close and reopen Awaytime
   - [ ] Verify blocking remains active
   - [ ] Test device restart scenario (if time permits)

**Pass Criteria:**
- Blocking activates at exactly 100% of limit
- Blocking cannot be easily bypassed
- Blocking persists across app restarts

#### Test 1.4: Notifications and Alerts
**Time Limit:** 5 minutes

1. **Warning Notifications**
   - [ ] Verify 80% warning notification content
   - [ ] Check notification timing and accuracy
   - [ ] Test notification interaction (tap to open app)

2. **Limit Reached Notifications**
   - [ ] Verify blocking notification appears
   - [ ] Check message tone is motivational, not punitive
   - [ ] Verify notification leads to appropriate screen

3. **Achievement Notifications**
   - [ ] Complete a successful day (stay under limit)
   - [ ] Check for streak/achievement notifications
   - [ ] Verify celebration animations (if applicable)

**Pass Criteria:**
- Notifications appear at correct thresholds
- Message content is appropriate and motivational
- Notifications function correctly

### Phase 2: User Experience Testing (20 minutes)

#### Test 2.1: Dashboard Usability
**Time Limit:** 5 minutes

1. **Information Clarity**
   - [ ] Verify usage progress is immediately understandable
   - [ ] Check time remaining is prominently displayed
   - [ ] Confirm purple theme is consistent and appealing
   - [ ] Test progress circle animation smoothness

2. **Navigation**
   - [ ] Test all main navigation buttons
   - [ ] Verify smooth transitions between screens
   - [ ] Check back navigation works correctly
   - [ ] Test deep linking (if applicable)

**Pass Criteria:**
- Dashboard information is clear at first glance
- Navigation is intuitive and responsive
- Visual design is polished and consistent

#### Test 2.2: Settings and Configuration
**Time Limit:** 5 minutes

1. **App Selection**
   - [ ] Test adding new apps to monitoring
   - [ ] Test removing apps from monitoring
   - [ ] Verify changes take effect immediately
   - [ ] Check app search/filter functionality

2. **Limit Adjustment**
   - [ ] Test changing daily limit
   - [ ] Verify new limit applies immediately
   - [ ] Test limit validation (minimum/maximum)
   - [ ] Check helpful guidance for limit setting

**Pass Criteria:**
- Settings changes are intuitive and immediate
- Validation prevents invalid configurations
- User guidance is helpful and clear

#### Test 2.3: Streak and Progress Features
**Time Limit:** 5 minutes

1. **Streak Display**
   - [ ] Verify current streak is displayed correctly
   - [ ] Check streak milestone indicators
   - [ ] Test streak reset on failed day
   - [ ] Verify longest streak tracking

2. **Weekly Progress**
   - [ ] Check weekly progress chart displays
   - [ ] Verify data accuracy for past week
   - [ ] Test chart interaction (if applicable)
   - [ ] Check visual indicators for successful days

**Pass Criteria:**
- Streak information is motivating and accurate
- Progress visualization is clear and helpful
- Data reflects actual user behavior

#### Test 2.4: Goal Recommendations
**Time Limit:** 5 minutes

1. **Recommendation Display**
   - [ ] Verify goal recommendations appear
   - [ ] Check recommendation relevance to current usage
   - [ ] Test different usage scenarios (low, medium, high)
   - [ ] Verify recommendation tone is supportive

2. **Recommendation Types**
   - [ ] Test "excellent" performance scenario
   - [ ] Test "warning" threshold scenario
   - [ ] Test "exceeded limit" scenario
   - [ ] Verify appropriate messaging for each type

**Pass Criteria:**
- Recommendations are relevant and helpful
- Messaging tone is supportive and motivational
- Different scenarios trigger appropriate recommendations

### Phase 3: Edge Case and Stress Testing (15 minutes)

#### Test 3.1: Rapid Interactions
**Time Limit:** 5 minutes

1. **Button Spam Testing**
   - [ ] Rapidly tap navigation buttons 10+ times
   - [ ] Verify no crashes or duplicate actions
   - [ ] Test rapid app switching during monitoring
   - [ ] Check system responsiveness after stress

2. **Data Input Stress**
   - [ ] Rapidly change limit settings multiple times
   - [ ] Test adding/removing many apps quickly
   - [ ] Verify data consistency after rapid changes

**Pass Criteria:**
- App handles rapid interactions gracefully
- No crashes or data corruption occurs
- System remains responsive

#### Test 3.2: Background and Foreground Transitions
**Time Limit:** 5 minutes

1. **App Backgrounding**
   - [ ] Background app during active monitoring
   - [ ] Use monitored apps while Awaytime is backgrounded
   - [ ] Return to Awaytime and verify data accuracy
   - [ ] Test multiple background/foreground cycles

2. **System Interruptions**
   - [ ] Test incoming call during app use
   - [ ] Test low battery warning interruption
   - [ ] Test notification interruptions
   - [ ] Verify app state is preserved

**Pass Criteria:**
- Background monitoring continues accurately
- App state is preserved during interruptions
- No data loss occurs during transitions

#### Test 3.3: Time-Based Edge Cases
**Time Limit:** 5 minutes

1. **Daily Reset Testing**
   - [ ] Test behavior near midnight (if possible)
   - [ ] Verify usage resets correctly
   - [ ] Check blocked apps are unblocked
   - [ ] Test streak counter updates

2. **Time Manipulation**
   - [ ] Change device time manually
   - [ ] Verify app handles time changes gracefully
   - [ ] Check for any exploitable behaviors
   - [ ] Restore correct time and verify recovery

**Pass Criteria:**
- Daily reset functions correctly
- Time manipulation doesn't break functionality
- App recovers gracefully from time changes

### Phase 4: Accessibility and Performance Testing (10 minutes)

#### Test 4.1: Accessibility Validation
**Time Limit:** 5 minutes

1. **VoiceOver/TalkBack Testing**
   - [ ] Enable screen reader
   - [ ] Navigate through main screens using only screen reader
   - [ ] Verify all elements have appropriate labels
   - [ ] Test critical user flows with screen reader

2. **Visual Accessibility**
   - [ ] Test with high contrast mode enabled
   - [ ] Verify text remains readable
   - [ ] Check color contrast ratios
   - [ ] Test with larger text sizes

**Pass Criteria:**
- All functionality accessible via screen reader
- Visual elements meet accessibility standards
- App works with accessibility features enabled

#### Test 4.2: Performance Validation
**Time Limit:** 5 minutes

1. **Response Time Testing**
   - [ ] Measure app launch time (should be <3 seconds)
   - [ ] Test navigation response times
   - [ ] Check data loading speeds
   - [ ] Verify smooth animations

2. **Resource Usage**
   - [ ] Monitor battery usage during testing
   - [ ] Check memory usage patterns
   - [ ] Verify no memory leaks
   - [ ] Test with low battery conditions

**Pass Criteria:**
- App launch time <3 seconds
- Navigation is responsive (<1 second)
- Battery usage is minimal
- No performance degradation over time

## Test Results Documentation

### Pass/Fail Criteria Summary

#### Critical (Must Pass)
- [ ] App launches successfully
- [ ] Core usage tracking works accurately
- [ ] App blocking functions correctly
- [ ] No crashes during normal usage
- [ ] Data persists across app restarts

#### Important (Should Pass)
- [ ] Notifications work correctly
- [ ] UI is intuitive and responsive
- [ ] Accessibility features function
- [ ] Performance meets targets
- [ ] Edge cases handled gracefully

#### Nice-to-Have (Could Pass)
- [ ] Advanced animations are smooth
- [ ] All visual polish is perfect
- [ ] Premium features work flawlessly
- [ ] All edge cases covered

### Bug Reporting Template

**Bug ID:** [Unique identifier]
**Severity:** [Critical/High/Medium/Low]
**Priority:** [P0/P1/P2/P3]
**Platform:** [iOS/Android/Both]
**Device:** [Specific device model and OS version]

**Summary:** [Brief description of the issue]

**Steps to Reproduce:**
1. [Step 1]
2. [Step 2]
3. [Step 3]

**Expected Result:** [What should happen]
**Actual Result:** [What actually happened]
**Screenshots/Videos:** [If applicable]

**Workaround:** [If any exists]
**Additional Notes:** [Any other relevant information]

### Test Completion Checklist

#### Pre-Release Validation
- [ ] All critical tests pass
- [ ] No P0 or P1 bugs remain
- [ ] Performance benchmarks met
- [ ] Accessibility requirements satisfied
- [ ] User acceptance criteria achieved

#### Documentation Complete
- [ ] Test results documented
- [ ] Bug reports filed
- [ ] Performance metrics recorded
- [ ] User feedback collected
- [ ] Recommendations prioritized

#### Sign-off Required
- [ ] QA Team approval
- [ ] Product Manager approval
- [ ] Development Team approval
- [ ] Accessibility Team approval (if applicable)
- [ ] Final release decision made

## Post-Test Actions

### Immediate Actions (Within 24 hours)
1. **Critical Bug Fixes**
   - Address any P0 bugs immediately
   - Verify fixes don't introduce new issues
   - Re-run affected test cases

2. **Results Communication**
   - Share test results with team
   - Prioritize identified issues
   - Plan fix timeline

### Short-term Actions (Within 1 week)
1. **High Priority Fixes**
   - Address P1 bugs
   - Implement critical UX improvements
   - Re-test affected functionality

2. **Performance Optimization**
   - Address performance issues
   - Optimize resource usage
   - Validate improvements

### Long-term Actions (Future releases)
1. **Enhancement Implementation**
   - Address P2/P3 issues
   - Implement user feedback
   - Add requested features

2. **Continuous Improvement**
   - Update test cases based on findings
   - Improve testing processes
   - Plan regular testing cycles

This comprehensive test execution script ensures thorough validation of the Awaytime app before release, with clear criteria for success and detailed procedures for identifying and addressing issues.