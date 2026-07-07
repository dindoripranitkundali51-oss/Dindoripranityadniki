import test from "node:test";
import assert from "node:assert/strict";
import { DEFAULT_CONFIG, DEFAULT_UI_SETTINGS, validateSystemSettings } from "../lib/systemSettings.js";

test("validateSystemSettings returns parsed numeric values for valid payloads", () => {
  const result = validateSystemSettings(
    {
      ...DEFAULT_CONFIG,
      commission_pct: "65",
      withdrawal_high_risk_amount: "9000",
      risk_open_threshold_minutes: "25",
      notification_failover_threshold_minutes: "10",
      support_auto_attend_minutes: "5",
      support_auto_resolve_hours: "6",
      service_request_auto_approve_minutes: "3",
    },
    {
      ...DEFAULT_UI_SETTINGS,
      reports_auto_refresh_sec: "45",
    }
  );

  assert.equal(result.ok, true);
  assert.deepEqual(result.values, {
    commission: 65,
    refreshSeconds: 45,
    withdrawalHighRiskAmount: 9000,
    riskOpenThresholdMinutes: 25,
    notificationFailoverThresholdMinutes: 10,
    supportAutoAttendMinutes: 5,
    supportAutoResolveHours: 6,
    serviceRequestAutoApproveMinutes: 3,
  });
});

test("validateSystemSettings rejects out-of-range and too-small thresholds", () => {
  assert.equal(
    validateSystemSettings({ ...DEFAULT_CONFIG, commission_pct: 120 }, DEFAULT_UI_SETTINGS).error,
    "Trust share must be between 0 and 100."
  );
  assert.equal(
    validateSystemSettings(DEFAULT_CONFIG, { ...DEFAULT_UI_SETTINGS, reports_auto_refresh_sec: 10 }).error,
    "Reports auto refresh must be at least 15 seconds."
  );
  assert.equal(
    validateSystemSettings({ ...DEFAULT_CONFIG, risk_open_threshold_minutes: 1 }, DEFAULT_UI_SETTINGS).error,
    "Risk escalation threshold must be at least 5 minutes."
  );
});
