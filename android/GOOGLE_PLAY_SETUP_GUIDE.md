# Google Play Console Setup Guide for Awaytime Premium

This guide walks you through setting up actual subscription products in Google Play Console to enable real payments in your Android app.

## Prerequisites

1. **Google Play Console Account**: You need a Google Play Console developer account ($25 one-time fee)
2. **App Published**: Your app must be published (can be in Internal Testing initially)
3. **Merchant Account**: Google Play Console merchant account for receiving payments

## Step 1: Create Subscription Products

### 1.1 Access Play Console
1. Go to [Google Play Console](https://play.google.com/console)
2. Select your app "Awaytime"
3. Navigate to **Monetize** → **Products** → **Subscriptions**

### 1.2 Create Monthly Subscription
1. Click **Create subscription**
2. Fill in the details:
   - **Product ID**: `com.awaytime.premium.monthly` (must match your code)
   - **Name**: "Awaytime Premium Monthly"
   - **Description**: "Monthly subscription to Awaytime Premium features"

3. **Pricing & Availability**:
   - Set your monthly price (e.g., $4.99/month)
   - Select countries where available
   - Choose billing period: **1 month**

4. **Free Trial** (Optional but recommended):
   - Enable free trial: **7 days** or **14 days**
   - This increases conversion rates significantly

### 1.3 Create Yearly Subscription
1. Click **Create subscription**
2. Fill in the details:
   - **Product ID**: `com.awaytime.premium.yearly` (must match your code)
   - **Name**: "Awaytime Premium Yearly"
   - **Description**: "Yearly subscription to Awaytime Premium features with savings"

3. **Pricing & Availability**:
   - Set yearly price (e.g., $39.99/year - 33% savings vs monthly)
   - Select same countries as monthly
   - Choose billing period: **1 year**

4. **Free Trial** (Optional):
   - Same trial period as monthly for consistency

### 1.4 Activate Products
1. Review both subscription products
2. Click **Activate** for each product
3. Products must be activated to work in production

## Step 2: Configure App Bundle

### 2.1 Upload Signed APK/Bundle
Your app needs to be uploaded to Play Console with the billing permission:

```xml
<!-- This should already be in your AndroidManifest.xml -->
<uses-permission android:name="com.android.vending.BILLING" />
```

### 2.2 Version Requirements
- **Target SDK**: Must be 33+ (you have 34 ✅)
- **Billing Library**: Must be 5.0+ (you have 6.1.0 ✅)

## Step 3: Testing Setup

### 3.1 License Testing
1. In Play Console, go to **Setup** → **License testing**
2. Add test accounts (Gmail addresses)
3. Set license test response to **RESPOND_NORMALLY**

### 3.2 Internal Testing Track
1. Go to **Testing** → **Internal testing**
2. Create a new release
3. Upload your signed APK/Bundle
4. Add testers (including yourself)
5. This allows testing real purchases without charges

### 3.3 Test Purchases
- Test accounts can make purchases without being charged
- Purchases are automatically cancelled after 5 minutes
- Perfect for testing the complete flow

## Step 4: Production Checklist

### 4.1 App Review Requirements
- **Privacy Policy**: Must be accessible from Play Store listing
- **Terms of Service**: Required for subscription apps
- **Subscription Details**: Clear description of what users get

### 4.2 Subscription Policies Compliance
- **Clear Pricing**: Display price and billing frequency clearly
- **Cancellation**: Easy cancellation process
- **Free Trial**: If offered, must be clearly disclosed
- **Auto-Renewal**: Must inform users about auto-renewal

### 4.3 Required App Store Assets
Update your Play Store listing:
- **Screenshots**: Show premium features
- **Description**: Mention premium features and pricing
- **What's New**: Highlight premium launch

## Step 5: Revenue & Analytics

### 5.1 Financial Reports
- Access revenue data in **Monetize** → **Financial reports**
- Track subscription metrics, churn, and revenue

### 5.2 Subscription Analytics
- Monitor subscriber lifecycle
- Track conversion rates from free to premium
- Analyze churn and retention

## Step 6: Post-Launch Monitoring

### 6.1 Key Metrics to Track
- **Conversion Rate**: Free to premium conversion
- **Churn Rate**: Monthly/yearly subscription cancellations
- **Revenue per User**: Average revenue per subscriber
- **Trial Conversion**: Free trial to paid conversion

### 6.2 Common Issues
- **Purchase Verification**: Ensure server-side verification if needed
- **Restore Purchases**: Test on multiple devices
- **Subscription Status**: Handle edge cases (expired, cancelled, etc.)

## Important Notes

### Security Considerations
- **Never store subscription status locally only**
- **Always verify with Google Play Billing**
- **Handle network failures gracefully**
- **Implement proper error handling**

### User Experience
- **Clear Value Proposition**: Make premium benefits obvious
- **Smooth Onboarding**: Guide users through premium features
- **Graceful Degradation**: Free users should still have good experience

### Legal Requirements
- **GDPR Compliance**: If serving EU users
- **CCPA Compliance**: If serving California users
- **Local Laws**: Check subscription laws in your target markets

## Testing Commands

Once set up, test these scenarios:
1. **Purchase Flow**: Complete purchase with test account
2. **Restore Purchases**: Test on different device
3. **Subscription Management**: Cancel and resubscribe
4. **Network Issues**: Test offline/poor connection scenarios
5. **Edge Cases**: Expired subscriptions, payment failures

## Support Resources

- [Google Play Billing Documentation](https://developer.android.com/google/play/billing)
- [Subscription Best Practices](https://developer.android.com/google/play/billing/subscriptions)
- [Play Console Help](https://support.google.com/googleplay/android-developer)

---

**Next Steps**: After completing Google Play Console setup, your app will be ready to accept real payments! The code is already implemented and ready to go.