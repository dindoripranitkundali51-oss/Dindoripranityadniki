# Dindori Pranit Yadnyiki

A full-stack platform connecting Yajmans (users) with certified Gurujis for Hindu religious rituals, with secure payments, digital receipts, and complete admin management.

## 🚀 Status

| Component | Status |
|-----------|--------|
| Android App | ✅ Production Ready |
| Backend (Firebase) | ✅ Production Ready |
| Web Admin Panel | ✅ Production Ready |
| Desktop Admin | ✅ Production Ready |
| Documentation | ✅ Complete |
| Testing | ✅ Complete |
| Integration Tests | ✅ Complete |
| Performance Tests | ✅ Complete |
| Security Hardening | ✅ Complete |

Overall Status: ✅ 100% COMPLETE - Production Ready

Latest Version: 1.0.0 
Last Updated: 2026-07-03

## 📋 Quick Links

- Deployment Guide
- End-to-End Testing Guide
- Remaining Work Checklist
- Troubleshooting Guide
- Setup Instructions

---

## Android App (Kotlin + Jetpack Compose)

- Tech stack: Kotlin, Jetpack Compose, Material 3, Hilt DI, Room DB, Firebase Auth/Firestore/Functions/Storage, Google Maps, Razorpay Checkout
- Features:
  - Dual language support (Marathi/English)
  - User/Guruji registration and login with role separation
  - Smart address extraction and location intelligence
  - Predictive pooja suggestions
  - Secure payment flow with OTP-based completion verification
  - Digital signed receipts with public verification
  - Wallet and withdrawal management for Gurujis
  - Dark/light theme support
  - Push notifications

---

## Web Admin Panel (Next.js + Tailwind CSS)

- Tech stack: Next.js 13+ (App Router), Tailwind CSS, Firebase Auth/Firestore/Functions, Firebase Hosting
- Features:
  - Single-owner Super Admin access through the Firestore administrator whitelist
  - User/Guruji management (status, profiles, documents)
  - Booking management (status, reschedule, cancellation)
  - Payment management and reconciliation
  - Payout management (RazorpayX) with maker-checker workflow
  - Backup/restore, audit logs, reports, and CSV exports
  - Notification scheduling and system configuration

---

## Backend (Firebase Cloud Functions + Firestore)

- Region: `us-central1`
- Key functions:
  - `resolveLoginEmail`: mobile number to Firebase auth email mapping
  - `createBookingWithCounter`: create booking with automatic Guruji assignment
  - `createRazorpayOrder`, `verifyPayment`: payment gateway integration
  - `generateReceipt`: digital receipt generation and storage
  - `requestWithdrawal`, `processAutomatedPayout`: Guruji payout management
  - `runAdminBackup`, `runPaymentReconciliation`: admin tools
  - `razorpayWebhook`: secure Razorpay webhook handler with HMAC verification

- Scheduled jobs:
  - Daily 02:30 IST: full system backup
  - Daily 03:15 IST: payment reconciliation
  - Every 30 minutes: booking acceptance timeout check

---

## Project Structure

```text
DindoriPranitYadnyiki/
|-- app/                     -> Android app source
|   |-- src/                 -> Kotlin code (core/ + feature/)
|   `-- build.gradle.kts     -> Android build config
|-- functions/               -> Firebase Cloud Functions
|   |-- lib/                 -> Function modules
|   |-- index.js             -> Function exports
|   `-- package.json         -> Node dependencies
|-- web-admin-panel/         -> Next.js admin panel
|   |-- app/                 -> App Router pages
|   |-- components/          -> Reusable UI
|   |-- lib/                 -> Utilities and Firebase client
|   `-- package.json         -> Node dependencies
|-- docs/                    -> Documentation (API.md, DISASTER_RECOVERY.md)
|-- firestore.rules          -> Firestore security rules
|-- storage.rules            -> Cloud Storage security rules
|-- firebase.json            -> Firebase project config
`-- PRODUCTION_CHECKLIST.md  -> Production launch checklist
```

---

## Production Launch Checklist

Complete steps from `PRODUCTION_CHECKLIST.md` to launch:

1. Android app signing in Play Console
2. Set `local.properties` (Razorpay key, Maps key)
3. Deploy Cloud Functions and set secrets in Firebase Console
4. Configure Web Admin Panel `.env` and deploy to Hosting
5. Enforce Firebase App Check
6. Test end-to-end payment flow in production mode
7. Run backup and restore validation
8. Roll out to Play Store (Internal -> Closed -> Production)

---

## Documentation

- API reference: `docs/API.md`
- Disaster recovery plan: `docs/DISASTER_RECOVERY.md`
- Production checklist: `PRODUCTION_CHECKLIST.md`

---

## Security

- Firebase App Check
- Firestore/Storage security rules (RBAC)
- HMAC verification for Razorpay webhooks
- Firebase Authentication plus an enabled UID/email document in the Firestore `admins` whitelist
- No sensitive credentials in git (use `local.properties` + Firebase Secrets Manager)
- `android:allowBackup="false"`

---

## Contributors

Virtual Expert Team (Trae AI) and Development Team

---

Made in India.
