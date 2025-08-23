# Baby Fox Mascot Integration Guide

## Overview
The Baby Fox mascot has been successfully integrated into both Android and iOS versions of Awaytime. The mascot changes its mood based on daily screen time usage and provides visual feedback to users about their screen time habits.

## Features
- **4 Mood States**: Happy (0-40%), Sleepy (40-70%), Sad (70-90%), Exhausted (90%+)
- **Smooth Transitions**: Animated transitions between mood states
- **Lottie Animations**: High-quality vector animations that scale perfectly
- **Cross-Platform**: Consistent behavior on both Android and iOS

## Android Implementation

### Dependencies Added
```gradle
// Lottie for animations
implementation 'com.airbnb.android:lottie-compose:6.1.0'
```

### Files Created
- `android/app/src/main/java/com/awaytime/app/ui/components/BabyFoxMascot.kt`
- `android/app/src/main/java/com/awaytime/app/service/FoxMoodService.kt`
- Lottie animation files in `android/app/src/main/assets/lottie/`

### Usage
```kotlin
BabyFoxMascot(
    usagePercent = usageProgress * 100f,
    modifier = Modifier.padding(bottom = 16.dp),
    size = 120
)
```

## iOS Implementation

### Dependencies Added
```swift
.package(url: "https://github.com/airbnb/lottie-ios.git", from: "4.3.4")
```

### Files Created
- `ios/Awaytime/Components/BabyFoxMascot.swift`
- `ios/Awaytime/Services/FoxMoodService.swift`
- Lottie animation files in `ios/Awaytime/Assets.xcassets/Lottie/`

### Usage
```swift
BabyFoxMascot(
    usagePercent: viewModel.usageProgress * 100,
    size: 120
)
```

## Animation Files

### Idle Animations (Loop Forever)
- `baby_fox_happy.json` - Happy fox bouncing gently
- `baby_fox_sleepy.json` - Sleepy fox with slow movements
- `baby_fox_sad.json` - Sad fox with drooping posture
- `baby_fox_exhausted.json` - Exhausted fox with fading opacity

### Transition Animations (Play Once)
- `transition_exhausted_to_happy.json`
- `transition_happy_to_sleepy.json`
- `transition_sleepy_to_sad.json`
- `transition_sad_to_exhausted.json`

## Mood Logic

The fox mood is determined by daily screen time usage percentage:

```javascript
function updateFoxMood(usagePercent) {
    let newMood;
    if (usagePercent <= 40) newMood = "happy";
    else if (usagePercent <= 70) newMood = "sleepy";
    else if (usagePercent <= 90) newMood = "sad";
    else newMood = "exhausted";
    
    if (newMood !== currentMood) {
        playTransition(currentMood, newMood);
        currentMood = newMood;
    }
}
```

## Integration Points

### Dashboard Integration
The Baby Fox mascot has been integrated at the top of both dashboard screens:

**Android**: `DashboardScreen.kt` - HeaderSection
**iOS**: `DashboardView.swift` - headerSection

### State Management
- **Android**: Uses `StateFlow` and `SharedPreferences` for persistence
- **iOS**: Uses `@Published` properties and `UserDefaults` for persistence

## Customization Options

### Size
Both components accept a size parameter:
- Default: 120dp/120pt
- Recommended range: 80-150dp/pt

### Animation Speed
Animations run at 30fps with these durations:
- Idle animations: Loop forever
- Transition animations: 1.5 seconds (45 frames)

## Performance Considerations

1. **Memory**: Lottie animations are vector-based and memory efficient
2. **CPU**: Animations pause when app is backgrounded
3. **Battery**: Minimal impact due to efficient Lottie rendering

## Testing

### Manual Testing
1. Change screen time usage in app
2. Verify fox mood changes at correct thresholds
3. Check transition animations play smoothly
4. Confirm persistence across app restarts

### Automated Testing
Consider adding unit tests for:
- Mood calculation logic
- State persistence
- Animation triggering

## Troubleshooting

### Common Issues
1. **Animations not loading**: Check file paths and asset inclusion
2. **Transitions not playing**: Verify transition logic and timing
3. **Performance issues**: Ensure animations are properly disposed

### Debug Tips
- Enable Lottie debug logging
- Check animation file validity
- Monitor memory usage during transitions

## Future Enhancements

### Potential Features
1. **Sound Effects**: Add audio feedback for mood changes
2. **Haptic Feedback**: Vibration on mood transitions
3. **Customization**: Allow users to choose different mascots
4. **Interactions**: Tap gestures for fox reactions
5. **Achievements**: Special animations for milestones

### Animation Improvements
1. **More Transitions**: Add all possible mood transitions
2. **Micro-interactions**: Subtle animations for user interactions
3. **Seasonal Themes**: Holiday-specific fox appearances
4. **Accessibility**: High contrast and reduced motion options

## Maintenance

### Regular Tasks
1. Update Lottie dependencies
2. Test on new OS versions
3. Monitor performance metrics
4. Gather user feedback

### Version Updates
When updating animations:
1. Test on both platforms
2. Verify file sizes remain reasonable
3. Check backward compatibility
4. Update documentation

## Support

For issues or questions about the Baby Fox mascot integration:
1. Check animation file formats and paths
2. Verify Lottie dependency versions
3. Test on physical devices
4. Review platform-specific implementation details