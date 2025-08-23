#!/bin/bash

echo "🦊 Setting up Baby Fox Mascot for Awaytime..."

# Check if we're in the right directory
if [ ! -d "android" ] || [ ! -d "ios" ]; then
    echo "❌ Error: Please run this script from the project root directory"
    exit 1
fi

echo "✅ Project structure verified"

# Android setup
echo "📱 Setting up Android..."
if [ -d "android/app/src/main/assets/lottie" ]; then
    echo "✅ Android Lottie assets directory exists"
else
    echo "❌ Android Lottie assets directory missing"
fi

if [ -f "android/app/src/main/java/com/awaytime/app/ui/components/BabyFoxMascot.kt" ]; then
    echo "✅ Android BabyFoxMascot component exists"
else
    echo "❌ Android BabyFoxMascot component missing"
fi

# iOS setup
echo "🍎 Setting up iOS..."
if [ -d "ios/Awaytime/Assets.xcassets/Lottie" ]; then
    echo "✅ iOS Lottie assets directory exists"
else
    echo "❌ iOS Lottie assets directory missing"
fi

if [ -f "ios/Awaytime/Components/BabyFoxMascot.swift" ]; then
    echo "✅ iOS BabyFoxMascot component exists"
else
    echo "❌ iOS BabyFoxMascot component missing"
fi

# Check Lottie files
echo "🎬 Checking animation files..."
LOTTIE_FILES=(
    "baby_fox_happy.json"
    "baby_fox_sleepy.json"
    "baby_fox_sad.json"
    "baby_fox_exhausted.json"
    "transition_exhausted_to_happy.json"
    "transition_happy_to_sleepy.json"
    "transition_sleepy_to_sad.json"
    "transition_sad_to_exhausted.json"
)

for file in "${LOTTIE_FILES[@]}"; do
    if [ -f "android/app/src/main/assets/lottie/$file" ] && [ -f "ios/Awaytime/Assets.xcassets/Lottie/$file" ]; then
        echo "✅ $file exists on both platforms"
    else
        echo "❌ $file missing on one or both platforms"
    fi
done

echo ""
echo "🦊 Baby Fox Mascot Setup Complete!"
echo ""
echo "Next steps:"
echo "1. 📱 Android: Sync Gradle to install Lottie dependency"
echo "2. 🍎 iOS: Add Lottie package dependency in Xcode"
echo "3. 🎬 Add animation files to Xcode project"
echo "4. 🧪 Test the mascot on both platforms"
echo ""
echo "📖 See BABY_FOX_INTEGRATION_GUIDE.md for detailed instructions"