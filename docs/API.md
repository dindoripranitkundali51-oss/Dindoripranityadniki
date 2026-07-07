# Dindori Pranit Yadnyiki API Notes

Backend surface is Firebase Callable Functions (`us-central1`) plus the Razorpay webhook HTTP endpoint.

Set `REQUIRE_APP_CHECK=true` only after Firebase App Check is configured in the Android app and enforcement is enabled in Firebase Console.

Implementation source: `functions/index.js` and `functions/lib/*`.

## User / Guruji Callables

### `resolveLoginEmail`
- Auth: none (public callable)
- Purpose: map mobile number to Firebase auth email
- Fields: `mobile`, optional `role` (`any` | `user` | `guruji`)

### `createBookingWithCounter` / `createBooking`
- Auth: signed-in user
- Purpose: create seva booking and assign Guruji when available
- Idempotency: `clientRequestId`
- Key fields: `poojaId`, `poojaName`, `date`, `contactName`, `contactPhone`, `address`, `district`, `pincode`, `lat`, `lng`, `specialInstructions`, `totalAmount`

### `createBookingRequest`
- Auth: booking owner
- Purpose: submit reschedule/cancel request
- Fields: `bookingId`, `type` (`Reschedule` | `Cancel`), `requestedDate`, `reason`

### `updateBookingStatus`
- Auth: assigned Guruji, booking owner (limited), or admin
- Purpose: lifecycle transitions (`Accepted`, `Rejected`, `In Progress`, `Completed`, `Cancelled`)
- Completion fields: `otp`, `actualAmount`

### `requestCompletionOtp`
- Auth: assigned Guruji
- Purpose: generate OTP and notify Yajman

### `updateGurujiAvailability`
- Auth: Guruji
- Purpose: publish available dates to `global_availability`
- Fields: `dates: string[]`

### `createRazorpayOrder`
- Auth: booking owner
- Purpose: create Razorpay order for unpaid booking
- Secrets: `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`

### `verifyPayment`
- Auth: booking owner
- Purpose: verify Razorpay signature and capture payment + ledger + receipt
- Fields: `bookingId`, `paymentId`, `orderId`, `signature`

### `requestWithdrawal`
- Auth: Guruji
- Purpose: create `withdrawal_requests` entry
- Fields: `amount`, `accountHolder`, `accountNumber`, `ifsc`, `upiId`

### `submitFeedback`
- Auth: booking owner
- Fields: `bookingId`, `rating`, `review`

### `getRoutePreview`
- Auth: signed-in user
- Fields: `originLat`, `originLng`, `destinationLat`, `destinationLng`
- Env: `GOOGLE_MAPS_API_KEY` or `MAPS_KEY` (optional)

### `verifyReceiptPublic`
- Auth: none
- Fields: `receiptNo`

## Admin Callables

Admin callables require Firebase Authentication, App Check, and an enabled UID/email document in the Firestore `admins` whitelist. There is no separate admin OTP API or role hierarchy.

### `manageUserStatus`
- Fields: `userId`, `status`, optional `reason`

### `manageGurujiStatus`
- Fields: `gurujiId`, `status`, optional `reason`

### `updateBookingStatus`
- Fields: `bookingId`, `newStatus`

### `handleServiceRequest`
- Fields: `requestId`, `status` (`Approved` | `Rejected`), `note`

### `updatePaymentStatus`
- Fields: `bookingId`, `paymentStatus`
- Large amounts may return `{ approvalRequired: true, requestId }`

### `approveMakerCheckerRequest`
- Fields: `requestId`

### `markPayoutSettled`
- Fields: `bookingId`, `settlementRef`

### `settleWithdrawalRequest`
- Fields: `requestId`, `settlementRef`

### `processAutomatedPayout`
- Fields: `requestId`
- Secrets: RazorpayX account + API keys

### `generateReceipt` / `resendReceipt`
- Fields: `bookingId`

### `sendAdminNotification`
- Fields: `target` topic, `title`, `message`

### `scheduleNotification`
- Fields: `title`, `body`, `targetTopic`, `scheduledTime`

### `runAdminBackup`
- Returns: `downloadUrl`, `fileName`

### `runPaymentReconciliation`
- Returns: mismatch report in `payment_reconciliation_runs`

## Scheduled

### `scheduledDailyBackup`
- Daily 02:30 Asia/Kolkata

### `scheduledDailyPaymentReconciliation`
- Daily 03:15 Asia/Kolkata

## HTTP

### `razorpayWebhook`
- Events: `payment.authorized`, `payment.failed`, `order.paid`, `payment.dispute.created`, `payment.downtime.started`, `settlement.processed`
- Security: `RAZORPAY_WEBHOOK_SECRET` HMAC verification

## Local development

```bash
cd functions
npm install
firebase emulators:start --only auth,firestore,functions
npm run smoke:emulator
```
