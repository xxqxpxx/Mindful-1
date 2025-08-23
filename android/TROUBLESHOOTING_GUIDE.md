# Awaytime Troubleshooting Guide

This guide helps you resolve common issues with app selection and usage tracking.

## 🔍 **Issue: Apps Not Showing in Dashboard (-1 Values)**

### **Root Causes:**
1. **No App Group Created**: Apps selected but not saved as a group
2. **Permission Issues**: Usage stats permission not granted
3. **Database Issues**: App group not properly saved
4. **Usage Tracking Not Started**: Service not monitoring selected apps

### **Solutions:**

#### **Step 1: Check Permissions**
1. Go to **Settings** → **Apps** → **Awaytime** → **Permissions**
2. Enable **"Usage access"** permission
3. Enable **"Accessibility"** permission for app blocking
4. Restart the app

#### **Step 2: Use Debug Console**
1. Open Awaytime
2. Tap **"Debug Console"** on the dashboard
3. Tap **"Full Report"** to see detailed diagnostics
4. Look for issues in the output

#### **Step 3: Re-select Apps**
1. Go to **"Select Apps"** from dashboard
2. Choose your apps again
3. **Important**: Tap **"Save"** and give the group a name
4. Confirm the group is created

#### **Step 4: Auto-Fix Common Issues**
1. In Debug Console, tap **"Auto-Fix"**
2. This will:
   - Initialize user settings
   - Reload available apps
   - Start usage tracking
   - Clean up old data

## 🔄 **Issue: App Selection Screen Not Loading**

### **Symptoms:**
- Screen shows loading spinner indefinitely
- No apps appear in the list
- "Refresh" button doesn't work

### **Solutions:**

#### **Method 1: Permission Reset**
1. Go to Android **Settings** → **Apps** → **Awaytime**
2. Tap **"Permissions"**
3. **Disable** then **re-enable** "Usage access"
4. Open Awaytime and try again

#### **Method 2: Clear App Data**
1. Go to Android **Settings** → **Apps** → **Awaytime**
2. Tap **"Storage"**
3. Tap **"Clear Data"** (⚠️ This will reset all settings)
4. Open Awaytime and go through setup again

#### **Method 3: Debug Console Fix**
1. If you can access the dashboard, use **Debug Console**
2. Tap **"App Selection"** to see what's wrong
3. Tap **"Auto-Fix"** to resolve issues

## 📊 **Issue: Usage Data Shows 0 or Incorrect Values**

### **Possible Causes:**
1. **Apps not actually used**: Usage is genuinely 0
2. **Permission issues**: Can't access usage stats
3. **Wrong time zone**: Usage calculated for wrong day
4. **App group mismatch**: Tracking different apps than selected

### **Solutions:**

#### **Verify Real Usage:**
1. Use a test app (like Chrome) for 5-10 minutes
2. Check if usage updates in Awaytime
3. If not, it's a tracking issue

#### **Check Usage Stats Permission:**
1. Android **Settings** → **Special app access** → **Usage access**
2. Find **Awaytime** and ensure it's enabled
3. If disabled, enable it and restart Awaytime

#### **Debug Usage Tracking:**
1. Open **Debug Console**
2. Tap **"Usage Tracking"**
3. Look for:
   - "Monitoring Active: true"
   - App groups with selected apps
   - Non-zero usage values

## 🚫 **Issue: App Blocking Not Working**

### **Requirements for App Blocking:**
1. **Accessibility Service** must be enabled
2. **Usage limits** must be set and reached
3. **App group** must be active

### **Enable Accessibility Service:**
1. Android **Settings** → **Accessibility**
2. Find **"Awaytime"** in the list
3. Turn it **ON**
4. Confirm the permission dialog

### **Test App Blocking:**
1. Set a very low limit (e.g., 1 minute)
2. Use a selected app for more than the limit
3. The app should be blocked with a message

## 🔧 **Advanced Troubleshooting**

### **Database Issues:**
If the debug console shows database errors:

1. **Clear app data** (Settings → Apps → Awaytime → Storage → Clear Data)
2. **Reinstall the app** if clearing data doesn't work
3. **Check device storage** - ensure you have enough space

### **Performance Issues:**
If the app is slow or crashes:

1. **Restart your device**
2. **Close other apps** to free up memory
3. **Update Android** if possible
4. **Check for app updates** in Play Store

### **Permission Issues:**
If permissions keep getting revoked:

1. **Disable battery optimization** for Awaytime:
   - Settings → Battery → Battery optimization
   - Find Awaytime and set to "Don't optimize"

2. **Enable auto-start** (varies by device):
   - Look for "Auto-start manager" or similar in Settings
   - Enable Awaytime

## 📱 **Device-Specific Issues**

### **Samsung Devices:**
- **Disable "Put app to sleep"**: Settings → Device care → Battery → App power management
- **Add to "Never sleeping apps"** list

### **Xiaomi/MIUI:**
- **Disable MIUI Optimization**: Settings → Additional settings → Developer options
- **Enable "Autostart"**: Security → Autostart → Enable Awaytime

### **Huawei:**
- **Protected apps**: Settings → Advanced settings → Battery manager → Protected apps
- **Enable Awaytime** in the list

### **OnePlus/OxygenOS:**
- **Battery optimization**: Settings → Battery → Battery optimization → Awaytime → Don't optimize

## 🆘 **Still Having Issues?**

### **Collect Debug Information:**
1. Open **Debug Console**
2. Tap **"Full Report"**
3. Copy the entire output
4. Include this information when reporting the issue

### **Common Debug Output Meanings:**

**"No apps loaded! Try refreshing."**
- Permission issue or app loading failure
- Try the Auto-Fix option

**"App Groups in Database: 0"**
- No app groups created
- Re-select apps and save them

**"Monitoring Active: false"**
- Usage tracking not started
- Check permissions and try Auto-Fix

**"Usage: -1 minutes"**
- Database query returning null
- Usually fixed by creating a proper app group

### **Reset Everything (Last Resort):**
1. Android Settings → Apps → Awaytime
2. **Force Stop** the app
3. **Clear Storage** and **Clear Cache**
4. **Uninstall** and **reinstall** from Play Store
5. Go through the setup process again

## ✅ **Prevention Tips**

1. **Always tap "Save"** after selecting apps
2. **Give app groups meaningful names**
3. **Check permissions** after Android updates
4. **Use the Debug Console** regularly to catch issues early
5. **Don't force-close** Awaytime (let it run in background)

---

**Remember**: The Debug Console is your best friend for troubleshooting. It shows exactly what's happening inside the app and can auto-fix most common issues.