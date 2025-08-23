#!/bin/bash

# App Icon Generator Script for Awaytime
# Usage: ./generate_icons.sh path/to/your/source-icon.png

if [ $# -eq 0 ]; then
    echo "Usage: $0 <source-icon-path>"
    echo "Example: $0 awaytime-icon.png"
    exit 1
fi

SOURCE_ICON="$1"
ICON_DIR="Awaytime/Assets.xcassets/AppIcon.appiconset"

if [ ! -f "$SOURCE_ICON" ]; then
    echo "Error: Source icon file '$SOURCE_ICON' not found"
    exit 1
fi

# Check if ImageMagick is installed
if ! command -v convert &> /dev/null; then
    echo "ImageMagick is not installed. Please install it first:"
    echo "brew install imagemagick"
    exit 1
fi

echo "Generating app icons from $SOURCE_ICON..."

# iPhone Icons
convert "$SOURCE_ICON" -resize 40x40 "$ICON_DIR/AppIcon-20x20@2x.png"
convert "$SOURCE_ICON" -resize 60x60 "$ICON_DIR/AppIcon-20x20@3x.png"
convert "$SOURCE_ICON" -resize 58x58 "$ICON_DIR/AppIcon-29x29@2x.png"
convert "$SOURCE_ICON" -resize 87x87 "$ICON_DIR/AppIcon-29x29@3x.png"
convert "$SOURCE_ICON" -resize 80x80 "$ICON_DIR/AppIcon-40x40@2x.png"
convert "$SOURCE_ICON" -resize 120x120 "$ICON_DIR/AppIcon-40x40@3x.png"
convert "$SOURCE_ICON" -resize 120x120 "$ICON_DIR/AppIcon-60x60@2x.png"
convert "$SOURCE_ICON" -resize 180x180 "$ICON_DIR/AppIcon-60x60@3x.png"

# iPad Icons
convert "$SOURCE_ICON" -resize 20x20 "$ICON_DIR/AppIcon-20x20@1x.png"
convert "$SOURCE_ICON" -resize 29x29 "$ICON_DIR/AppIcon-29x29@1x.png"
convert "$SOURCE_ICON" -resize 40x40 "$ICON_DIR/AppIcon-40x40@1x.png"
convert "$SOURCE_ICON" -resize 76x76 "$ICON_DIR/AppIcon-76x76@1x.png"
convert "$SOURCE_ICON" -resize 152x152 "$ICON_DIR/AppIcon-76x76@2x.png"
convert "$SOURCE_ICON" -resize 167x167 "$ICON_DIR/AppIcon-83.5x83.5@2x.png"

# App Store Icon
convert "$SOURCE_ICON" -resize 1024x1024 "$ICON_DIR/AppIcon-1024x1024@1x.png"

echo "✅ All app icons generated successfully!"
echo "Icons saved to: $ICON_DIR"
echo ""
echo "Next steps:"
echo "1. Open your project in Xcode"
echo "2. The icons should automatically appear in Assets.xcassets > AppIcon"
echo "3. Build and run your app to see the new icon"