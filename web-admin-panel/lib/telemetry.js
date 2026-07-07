"use client";

import { callAdminApi } from "@/lib/apiClient";
import { logError } from "@/lib/logger";

let listenersBound = false;

export async function reportAdminClientIssue({
  level = "error",
  message,
  page = "",
  source = "web-admin",
  stack = "",
  metadata = {},
} = {}) {
  if (!message) return false;
  try {
    await callAdminApi("logWebAdminClientError", {
      level,
      message,
      page,
      source,
      stack,
      metadata,
    });
    return true;
  } catch (error) {
    logError("telemetry-report", error);
    return false;
  }
}

export function bindGlobalAdminTelemetry() {
  if (listenersBound || typeof window === "undefined") return;
  listenersBound = true;

  window.addEventListener("error", (event) => {
    reportAdminClientIssue({
      level: "error",
      message: event.message || "Unhandled window error",
      page: window.location.pathname,
      source: "window.onerror",
      stack: event.error?.stack || "",
      metadata: {
        filename: event.filename || "",
        lineno: event.lineno || 0,
        colno: event.colno || 0,
      },
    });
  });

  window.addEventListener("unhandledrejection", (event) => {
    const reason = event.reason;
    reportAdminClientIssue({
      level: "error",
      message: reason?.message || String(reason || "Unhandled promise rejection"),
      page: window.location.pathname,
      source: "window.unhandledrejection",
      stack: reason?.stack || "",
    });
  });
}
