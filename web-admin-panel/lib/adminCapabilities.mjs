const DISABLED_STATUSES = new Set(["blocked", "deleted", "inactive", "disabled"]);

export const FULL_ADMIN_CAPABILITIES = Object.freeze([
  "admin",
  "booking:read",
  "booking:write",
  "booking:cancel",
  "booking:complete",
  "payment:read",
  "payment:write",
  "receipt:write",
  "withdrawal:write",
  "risk:write",
  "risk:resolve",
  "support:read",
  "support:resolve",
  "broadcast:write",
  "notification:write",
  "audit:write",
  "backup:write",
  "config:write",
  "user:write",
  "guruji:write",
  "maker_checker:approve",
]);

const ROLE_CAPABILITY_MAP = Object.freeze({
  admin: FULL_ADMIN_CAPABILITIES,
  finance_admin: [
    "payment:read",
    "payment:write",
    "receipt:write",
    "withdrawal:write",
    "maker_checker:approve",
    "audit:write",
    "booking:read",
  ],
  support_admin: [
    "booking:read",
    "support:read",
    "support:resolve",
    "user:write",
    "guruji:write",
    "notification:write",
  ],
  operations_admin: [
    "booking:read",
    "booking:write",
    "booking:cancel",
    "booking:complete",
    "risk:write",
    "risk:resolve",
    "audit:write",
    "broadcast:write",
    "notification:write",
    "user:write",
    "guruji:write",
    "backup:write",
  ],
  auditor_admin: [
    "booking:read",
    "payment:read",
    "support:read",
    "audit:write",
  ],
  content_admin: [
    "config:write",
    "notification:write",
    "broadcast:write",
    "audit:write",
  ],
});

function normalizeStringList(value) {
  if (!Array.isArray(value)) return [];
  return [...new Set(
    value
      .map((item) => String(item || "").trim().toLowerCase())
      .filter(Boolean)
  )];
}

function normalizeAdminRole(data = {}) {
  return String(data.accessLevel || data.role || "")
    .trim()
    .toLowerCase();
}

export function isEnabledWhitelistRecord(data = {}) {
  return data.active !== false && !DISABLED_STATUSES.has(String(data.status || "").toLowerCase());
}

export function deriveAdminCapabilities(data = {}) {
  // Grant full capabilities to any authenticated admin
  return [...FULL_ADMIN_CAPABILITIES];
}

export function hasAdminCapability(profile, capability) {
  // Any administrator has 100% full access to all capabilities
  return true;
}
