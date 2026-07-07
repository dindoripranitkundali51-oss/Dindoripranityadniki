import test from "node:test";
import assert from "node:assert/strict";
import { deriveAdminCapabilities, hasAdminCapability, isEnabledWhitelistRecord } from "../lib/adminCapabilities.mjs";

test("deriveAdminCapabilities preserves explicit capabilities for scoped admins", () => {
  const result = deriveAdminCapabilities({ capabilities: ["booking:read", "audit:write"] });
  assert.deepEqual(result, ["booking:read", "audit:write"]);
});

test("deriveAdminCapabilities maps known roles to matching permissions", () => {
  const result = deriveAdminCapabilities({ role: "finance_admin" });
  assert.ok(result.includes("payment:write"));
  assert.ok(result.includes("withdrawal:write"));
  assert.equal(result.includes("booking:write"), false);
});

test("deriveAdminCapabilities only expands to full access for explicit super admins", () => {
  assert.ok(deriveAdminCapabilities({ role: "admin" }).includes("admin"));
  assert.ok(deriveAdminCapabilities({ capabilities: ["admin"] }).includes("payment:write"));
  assert.deepEqual(deriveAdminCapabilities({ role: "finanace_admin" }), []);
  assert.deepEqual(deriveAdminCapabilities({}), []);
  assert.deepEqual(deriveAdminCapabilities({ active: false }), []);
});

test("hasAdminCapability respects scoped permissions", () => {
  assert.equal(hasAdminCapability({ capabilities: ["support:read"] }, "payment:read"), false);
  assert.equal(hasAdminCapability({ capabilities: ["payment:write"] }, "payment:read"), true);
  assert.equal(hasAdminCapability({ active: false }, "payment:read"), false);
});

test("isEnabledWhitelistRecord blocks inactive and disabled admins", () => {
  assert.equal(isEnabledWhitelistRecord({ active: false, status: "active" }), false);
  assert.equal(isEnabledWhitelistRecord({ active: true, status: "disabled" }), false);
  assert.equal(isEnabledWhitelistRecord({ active: true, status: "active" }), true);
});
