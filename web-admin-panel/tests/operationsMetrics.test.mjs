import test from "node:test";
import assert from "node:assert/strict";
import { riskRank, formatDate, summarizeNotificationFailures } from "../lib/operationsMetrics.js";

test("riskRank prioritizes severity and age", () => {
  const low = riskRank({ severity: "low", createdAt: { seconds: Math.floor(Date.now() / 1000) } });
  const high = riskRank({ severity: "high", createdAt: { seconds: Math.floor(Date.now() / 1000) - 3600 } });
  assert.ok(high > low);
});

test("formatDate gracefully formats timestamps and fallbacks", () => {
  assert.equal(formatDate(null), "-");
  assert.equal(formatDate("raw"), "raw");
  assert.match(formatDate({ seconds: 1710000000 }), /\d/);
});

test("summarizeNotificationFailures returns failed and escalated subsets", () => {
  const summary = summarizeNotificationFailures([
    { id: "1", status: "failed", failoverState: "retry_pending" },
    { id: "2", status: "sent", failoverState: "none" },
    { id: "3", status: "failed", failoverState: "escalated" },
  ]);
  assert.equal(summary.failedNotifications.length, 2);
  assert.equal(summary.escalatedNotifications.length, 1);
});
