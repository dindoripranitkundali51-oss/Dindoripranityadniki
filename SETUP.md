# Project Setup Instructions

## Required Files (Not in Git)

### 1. `google-services.json`
- Download from Firebase Console → Project Settings → "Add app" → Android
- Place in `app/` directory

### 2. `local.properties`
Create this file in the project root with the following content:
```properties
## This file must *NOT* be checked into Version Control Systems,
# as it contains information specific to your local configuration.

# API Keys (Replace with your actual keys)
RAZORPAY_KEY=your_razorpay_key_here
MAPS_KEY=your_google_maps_key_here
```

## Firebase Setup
1. Enable App Check in Firebase Console
2. Set up Cloud Functions secrets (RAZORPAY_KEY_ID, RAZORPAY_KEY_SECRET, RAZORPAY_WEBHOOK_SECRET)
3. Deploy Cloud Functions: `firebase deploy --only functions`

## Web Admin Panel Setup (if applicable)
- Create `.env` file in `web-admin-panel/` directory
- Add Firebase config and other secrets
