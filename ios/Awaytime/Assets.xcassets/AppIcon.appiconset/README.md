# App Icon Setup Instructions

Your Awaytime app icon has been set up with the proper structure. To complete the setup:

## Required Icon Sizes

You need to create the following sizes from your icon image:

### iPhone Icons:
- AppIcon-20x20@2x.png (40x40 pixels)
- AppIcon-20x20@3x.png (60x60 pixels)
- AppIcon-29x29@2x.png (58x58 pixels)
- AppIcon-29x29@3x.png (87x87 pixels)
- AppIcon-40x40@2x.png (80x80 pixels)
- AppIcon-40x40@3x.png (120x120 pixels)
- AppIcon-60x60@2x.png (120x120 pixels)
- AppIcon-60x60@3x.png (180x180 pixels)

### iPad Icons:
- AppIcon-20x20@1x.png (20x20 pixels)
- AppIcon-29x29@1x.png (29x29 pixels)
- AppIcon-40x40@1x.png (40x40 pixels)
- AppIcon-76x76@1x.png (76x76 pixels)
- AppIcon-76x76@2x.png (152x152 pixels)
- AppIcon-83.5x83.5@2x.png (167x167 pixels)

### App Store:
- AppIcon-1024x1024@1x.png (1024x1024 pixels)

## How to Generate Icons:

1. **Using Xcode**: 
   - Open your project in Xcode
   - Navigate to Assets.xcassets > AppIcon
   - Drag your 1024x1024 icon to the App Store slot
   - Xcode can generate smaller sizes automatically

2. **Using Online Tools**:
   - Use tools like AppIcon.co or MakeAppIcon.com
   - Upload your high-resolution icon
   - Download the generated icon set
   - Copy the files to this folder

3. **Using Command Line** (if you have ImageMagick):
   ```bash
   # Example for creating 180x180 icon
   convert your-icon.png -resize 180x180 AppIcon-60x60@3x.png
   ```

## Notes:
- All icons must be PNG format
- Icons should not have transparency (solid background)
- Icons should not have rounded corners (iOS adds them automatically)
- Make sure your icon looks good at small sizes