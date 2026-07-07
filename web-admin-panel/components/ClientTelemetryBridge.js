"use client";

import { useEffect } from "react";
import { bindGlobalAdminTelemetry } from "@/lib/telemetry";

export default function ClientTelemetryBridge() {
  useEffect(() => {
    bindGlobalAdminTelemetry();
  }, []);

  return null;
}
