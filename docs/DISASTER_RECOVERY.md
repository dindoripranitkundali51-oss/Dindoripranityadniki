# Disaster Recovery Plan

## Recovery Objectives

- RPO target: last daily scheduled backup, plus Razorpay/payment gateway records for reconciliation.
- RTO target: restore critical booking, user, Guruji, receipt, and ledger collections first.

## Backup Sources

- `admin_backups`: manual and scheduled backup metadata/payload.
- Firestore export or Firebase console export for full production recovery.
- Razorpay dashboard for payment/order/settlement reconciliation.
- Firebase Hosting/Admin Panel source from repository.

## Incident Steps

1. Freeze writes if data corruption is suspected.
2. Export current Firestore state before attempting restore.
3. Identify last valid `admin_backups` entry or full Firestore export.
4. Restore in this order: `users`, `guruji`, `poojas`, `bookings`, `financial_ledger`, `receipt_snapshots`, `withdrawal_requests`.
5. Reconcile payments with Razorpay order/payment IDs.
6. Verify admin panel dashboards, receipt verification, and user/guruji booking views.
7. Log incident summary in `audit_logs` and keep restore evidence.

## Manual Test Checklist

- Create booking in staging.
- Complete payment verification.
- Generate receipt.
- Run backup.
- Restore backup into a test project.
- Confirm booking, ledger, receipt, and wallet values match.
