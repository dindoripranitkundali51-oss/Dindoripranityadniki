"use client";
import { useEffect, useState } from "react";

import AdminShell from "@/components/AdminShell";
import {
  Save,
  AlertTriangle,
  ArrowRightLeft,
  Loader2,
  LayoutTemplate,
  ShieldAlert,
  RadioTower,
} from "lucide-react";
import { logAdminAction } from "@/lib/audit";
import { DEFAULT_CONFIG, DEFAULT_UI_SETTINGS, validateSystemSettings } from "@/lib/systemSettings";
import { callAdminApi } from "@/lib/apiClient";

export default function SystemSettings() {
  const [config, setConfig] = useState(DEFAULT_CONFIG);
  const [uiSettings, setUiSettings] = useState(DEFAULT_UI_SETTINGS);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  // For Password Change
  const [newPassword, setNewPassword] = useState("");
  const [pwSaving, setPwSaving] = useState(false);
  const [pwMessage, setPwMessage] = useState("");
  const [pwError, setPwError] = useState("");

  const handleChangePassword = async (e) => {
    e.preventDefault();
    setPwMessage("");
    setPwError("");
    if (!newPassword) {
      setPwError("New password cannot be empty.");
      return;
    }
    setPwSaving(true);
    try {
      const baseUrl = process.env.NEXT_PUBLIC_API_URL || "//dindoripranitapi.somee.com/api/v1";
      const token = typeof window !== "undefined" ? localStorage.getItem("jwt_auth_token") || "" : "";
      const res = await fetch(`${baseUrl}/admin/manage/change-password`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": token ? `Bearer ${token}` : ""
        },
        body: JSON.stringify({ newPassword })
      });
      const data = await res.json();
      if (res.ok && data.success) {
        setPwMessage("Password updated successfully.");
        setNewPassword("");
      } else {
        setPwError(data.message || "Failed to update password.");
      }
    } catch (err) {
      setPwError("Failed to connect to API.");
    } finally {
      setPwSaving(false);
    }
  };

  // For Adding Admin
  const [adminEmail, setAdminEmail] = useState("");
  const [adminMobile, setAdminMobile] = useState("");
  const [adminPassword, setAdminPassword] = useState("");
  const [adminSaving, setAdminSaving] = useState(false);
  const [adminMessage, setAdminMessage] = useState("");
  const [adminError, setAdminError] = useState("");

  const handleAddAdmin = async (e) => {
    e.preventDefault();
    setAdminMessage("");
    setAdminError("");
    if (!adminEmail || !adminMobile || !adminPassword) {
      setAdminError("Email, Mobile, and Password are all required.");
      return;
    }
    setAdminSaving(true);
    try {
      const baseUrl = process.env.NEXT_PUBLIC_API_URL || "//dindoripranitapi.somee.com/api/v1";
      const token = typeof window !== "undefined" ? localStorage.getItem("jwt_auth_token") || "" : "";
      const res = await fetch(`${baseUrl}/admin/manage/admins`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": token ? `Bearer ${token}` : ""
        },
        body: JSON.stringify({ email: adminEmail.trim(), mobile: adminMobile.trim(), password: adminPassword })
      });
      const data = await res.json();
      if (res.ok && data.success) {
        setAdminMessage("New administrator added successfully.");
        setAdminEmail("");
        setAdminMobile("");
        setAdminPassword("");
      } else {
        setAdminError(data.message || "Failed to add administrator.");
      }
    } catch (err) {
      setAdminError("Failed to connect to API.");
    } finally {
      setAdminSaving(false);
    }
  };

  useEffect(() => {
    const loadConfig = async () => {
      try {
        const baseUrl = process.env.NEXT_PUBLIC_API_URL || "https://dindoripranitapi.somee.com/api/v1";
        const token = typeof window !== "undefined" ? localStorage.getItem("jwt_auth_token") || "" : "";
        const res = await fetch(`${baseUrl}/admin/settings`, {
          headers: {
            "Authorization": token ? `Bearer ${token}` : ""
          }
        });
        if (res.ok) {
          const data = await res.json();
          if (data.success) {
            if (data.config) setConfig((prev) => ({ ...prev, ...data.config }));
            if (data.uiSettings) setUiSettings((prev) => ({ ...prev, ...data.uiSettings }));
          }
        }
      } catch (err) {
        console.error("Failed to load settings from API:", err);
        setError("Failed to load system settings.");
      } finally {
        setLoading(false);
      }
    };
    loadConfig();
  }, []);

  const patchConfig = (field, value) => setConfig((prev) => ({ ...prev, [field]: value }));
  const patchUiSettings = (field, value) => setUiSettings((prev) => ({ ...prev, [field]: value }));

  const handleSave = async () => {
    setMessage("");
    setError("");
    const validation = validateSystemSettings(config, uiSettings);
    if (!validation.ok) {
      setError(validation.error);
      return;
    }
    const {
      commission,
      refreshSeconds,
      withdrawalHighRiskAmount,
      riskOpenThresholdMinutes,
      headingText,
      subheadingText,
      maintenanceMessageMR,
      maintenanceMessageEN,
    } = validation.values;

    const finalConfig = { 
      ...config, 
      commission_pct: commission, 
      withdrawal_high_risk_amount: withdrawalHighRiskAmount,
      risk_open_threshold_minutes: riskOpenThresholdMinutes
    };
    const finalUiSettings = { 
      ...uiSettings, 
      reports_auto_refresh_sec: refreshSeconds 
    };

    setSaving(true);
    try {
      await callAdminApi("updateSettings", {
        config: finalConfig,
        uiSettings: finalUiSettings
      });
      setMessage("Settings saved successfully.");
    } catch (e) {
      setError(e.message || "Failed to save settings.");
    }
    setSaving(false);
  };

  if (loading) {
    return (
      <AdminShell className="p-10 text-center">
        <Loader2 className="mx-auto animate-spin text-blue-600" />
      </AdminShell>
    );
  }

  return (
    <AdminShell className="p-6" requiredCapability="config:write">
      <header className="mb-6 flex items-center justify-between gap-4">
        <h1 className="text-2xl font-bold text-slate-800">System Settings</h1>
        <button
          onClick={handleSave}
          disabled={saving}
          className="flex items-center gap-2 rounded bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-70"
        >
          {saving ? <Loader2 size={16} className="animate-spin" /> : <Save size={18} />}
          Save Settings
        </button>
      </header>

      {error && <div className="mb-4 rounded border border-red-200 bg-red-50 px-4 py-3 text-sm font-semibold text-red-700">{error}</div>}
      {message && <div className="mb-4 rounded border border-green-200 bg-green-50 px-4 py-3 text-sm font-semibold text-green-700">{message}</div>}

      <Section title="Trust Commission" icon={<ArrowRightLeft size={18} className="text-blue-600" />}>
        <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
          <Field label="Trust Share (%)">
            <input
              type="number"
              min="0"
              max="100"
              step="1"
              className="w-full rounded border bg-slate-50 p-2.5 font-bold text-blue-600"
              value={config.commission_pct}
              onChange={(e) => patchConfig("commission_pct", e.target.value)}
            />
          </Field>
          <Field label="Guruji Share (%)">
            <input
              type="number"
              disabled
              className="w-full rounded border bg-slate-100 p-2.5 font-bold text-slate-400"
              value={100 - Number(config.commission_pct || 0)}
            />
          </Field>
        </div>
      </Section>

      <Section title="Kill Switch And Escalation Logic" icon={<ShieldAlert size={18} className="text-red-600" />}>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <ToggleCard
            title="Global Kill Switch"
            description="Stops booking and payment entrypoints immediately."
            checked={Boolean(config.kill_switch_enabled)}
            onChange={(value) => patchConfig("kill_switch_enabled", value)}
            tone="red"
          />
          <ToggleCard
            title="Maintenance Mode"
            description="Shows system-down behavior for non-admin transactional flows."
            checked={Boolean(config.maintenance_mode)}
            onChange={(value) => patchConfig("maintenance_mode", value)}
            tone="amber"
          />
          <ToggleCard
            title="Disable New Bookings"
            description="Blocks fresh booking creation while keeping admin panel available."
            checked={Boolean(config.disable_new_bookings)}
            onChange={(value) => patchConfig("disable_new_bookings", value)}
            tone="amber"
          />
          <ToggleCard
            title="Disable Payments"
            description="Stops Razorpay order creation and payment verification."
            checked={Boolean(config.disable_payments)}
            onChange={(value) => patchConfig("disable_payments", value)}
            tone="amber"
          />
          <ToggleCard
            title="Disable Admin Broadcasts"
            description="Blocks manual and scheduled admin notification sends."
            checked={Boolean(config.disable_admin_broadcasts)}
            onChange={(value) => patchConfig("disable_admin_broadcasts", value)}
            tone="amber"
          />
          <ToggleCard
            title="Auto Approve Service Requests"
            description="Automatically clears reschedule and cancel requests."
            checked={Boolean(config.auto_approve_service_requests)}
            onChange={(value) => patchConfig("auto_approve_service_requests", value)}
            tone="amber"
          />
          <ToggleCard
            title="Auto Approve Maker-Checker"
            description="Automatically clears payment approval queue when enabled."
            checked={Boolean(config.auto_approve_maker_checker)}
            onChange={(value) => patchConfig("auto_approve_maker_checker", value)}
            tone="amber"
          />
          <ToggleCard
            title="Auto Close Support Tickets"
            description="Moves stale tickets forward without manual triage."
            checked={Boolean(config.auto_close_support_tickets)}
            onChange={(value) => patchConfig("auto_close_support_tickets", value)}
            tone="amber"
          />
          <ToggleCard
            title="Auto Process Low-Risk Withdrawals"
            description="Processes requests below the high-risk threshold automatically."
            checked={Boolean(config.auto_process_low_risk_withdrawals)}
            onChange={(value) => patchConfig("auto_process_low_risk_withdrawals", value)}
            tone="amber"
          />
          <ToggleCard
            title="Auto Process All Withdrawals"
            description="High-risk: processes every withdrawal automatically, regardless of amount."
            checked={Boolean(config.auto_process_all_withdrawals)}
            onChange={(value) => patchConfig("auto_process_all_withdrawals", value)}
            tone="red"
          />
          <ToggleCard
            title="Auto Resolve Risk Events"
            description="High-risk: auto-closes open risk events instead of keeping them for review."
            checked={Boolean(config.auto_resolve_risk_events)}
            onChange={(value) => patchConfig("auto_resolve_risk_events", value)}
            tone="red"
          />
        </div>

        <div className="mt-6 grid grid-cols-1 gap-6 md:grid-cols-3">
          <Field label="Withdrawal High Risk Amount">
            <input
              type="number"
              min="0"
              className="w-full rounded border bg-slate-50 p-2.5"
              value={config.withdrawal_high_risk_amount}
              onChange={(e) => patchConfig("withdrawal_high_risk_amount", e.target.value)}
            />
          </Field>
          <Field label="Risk Escalation Threshold (min)">
            <input
              type="number"
              min="5"
              className="w-full rounded border bg-slate-50 p-2.5"
              value={config.risk_open_threshold_minutes}
              onChange={(e) => patchConfig("risk_open_threshold_minutes", e.target.value)}
            />
          </Field>
          <Field label="Notification Failover Threshold (min)">
            <input
              type="number"
              min="5"
              className="w-full rounded border bg-slate-50 p-2.5"
              value={config.notification_failover_threshold_minutes}
              onChange={(e) => patchConfig("notification_failover_threshold_minutes", e.target.value)}
            />
          </Field>
          <Field label="Support Auto Attend (min)">
            <input
              type="number"
              min="1"
              className="w-full rounded border bg-slate-50 p-2.5"
              value={config.support_auto_attend_minutes}
              onChange={(e) => patchConfig("support_auto_attend_minutes", e.target.value)}
            />
          </Field>
          <Field label="Support Auto Resolve (hours)">
            <input
              type="number"
              min="1"
              className="w-full rounded border bg-slate-50 p-2.5"
              value={config.support_auto_resolve_hours}
              onChange={(e) => patchConfig("support_auto_resolve_hours", e.target.value)}
            />
          </Field>
          <Field label="Service Request Auto Approve (min)">
            <input
              type="number"
              min="1"
              className="w-full rounded border bg-slate-50 p-2.5"
              value={config.service_request_auto_approve_minutes}
              onChange={(e) => patchConfig("service_request_auto_approve_minutes", e.target.value)}
            />
          </Field>
        </div>
      </Section>

      <Section title="CMS Dynamic UI Settings" icon={<LayoutTemplate size={18} className="text-blue-600" />}>
        <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
          <Field label="Dashboard Title" className="md:col-span-2">
            <input
              className="w-full rounded border bg-slate-50 p-2.5"
              value={uiSettings.dashboard_title}
              onChange={(e) => patchUiSettings("dashboard_title", e.target.value)}
            />
          </Field>
          <Field label="Dashboard Subtitle" className="md:col-span-2">
            <input
              className="w-full rounded border bg-slate-50 p-2.5"
              value={uiSettings.dashboard_subtitle}
              onChange={(e) => patchUiSettings("dashboard_subtitle", e.target.value)}
            />
          </Field>
          <Field label="Maintenance Banner EN">
            <textarea
              className="min-h-[110px] w-full rounded border bg-slate-50 p-2.5"
              value={uiSettings.maintenance_banner_en}
              onChange={(e) => patchUiSettings("maintenance_banner_en", e.target.value)}
            />
          </Field>
          <Field label="Maintenance Banner MR">
            <textarea
              className="min-h-[110px] w-full rounded border bg-slate-50 p-2.5"
              value={uiSettings.maintenance_banner_mr}
              onChange={(e) => patchUiSettings("maintenance_banner_mr", e.target.value)}
            />
          </Field>
          <Field label="Reports Auto Refresh (sec)">
            <input
              type="number"
              min="15"
              className="w-full rounded border bg-slate-50 p-2.5"
              value={uiSettings.reports_auto_refresh_sec}
              onChange={(e) => patchUiSettings("reports_auto_refresh_sec", e.target.value)}
            />
          </Field>
        </div>
      </Section>

      <Section title="Admin Credentials & Security" icon={<ShieldAlert size={18} className="text-emerald-600" />}>
        <div className="grid grid-cols-1 gap-8 md:grid-cols-2">
          {/* Card 1: Change Password */}
          <div className="rounded-lg border border-slate-200 bg-slate-50 p-5">
            <h4 className="mb-4 text-sm font-semibold text-slate-800">Change Admin Password</h4>
            {pwError && <div className="mb-3 text-xs font-semibold text-red-600">{pwError}</div>}
            {pwMessage && <div className="mb-3 text-xs font-semibold text-green-600">{pwMessage}</div>}
            <form onSubmit={handleChangePassword} className="space-y-4">
              <Field label="New Password">
                <input
                  type="password"
                  required
                  placeholder="Enter new secure password"
                  className="w-full rounded border bg-white p-2 text-sm"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                />
              </Field>
              <button
                type="submit"
                disabled={pwSaving}
                className="w-full rounded bg-blue-600 py-2 text-xs font-semibold text-white hover:bg-blue-700 disabled:opacity-75"
              >
                {pwSaving ? "Updating..." : "Update Password"}
              </button>
            </form>
          </div>

          {/* Card 2: Create Admin */}
          <div className="rounded-lg border border-slate-200 bg-slate-50 p-5">
            <h4 className="mb-4 text-sm font-semibold text-slate-800">Add New Administrator</h4>
            {adminError && <div className="mb-3 text-xs font-semibold text-red-600">{adminError}</div>}
            {adminMessage && <div className="mb-3 text-xs font-semibold text-green-600">{adminMessage}</div>}
            <form onSubmit={handleAddAdmin} className="space-y-3">
              <Field label="Admin Email">
                <input
                  type="email"
                  required
                  placeholder="admin@example.com"
                  className="w-full rounded border bg-white p-2 text-sm"
                  value={adminEmail}
                  onChange={(e) => setAdminEmail(e.target.value)}
                />
              </Field>
              <Field label="Admin Mobile">
                <input
                  type="text"
                  required
                  placeholder="9022341138"
                  className="w-full rounded border bg-white p-2 text-sm"
                  value={adminMobile}
                  onChange={(e) => setAdminMobile(e.target.value)}
                />
              </Field>
              <Field label="Admin Password">
                <input
                  type="password"
                  required
                  placeholder="Enter temporary password"
                  className="w-full rounded border bg-white p-2 text-sm"
                  value={adminPassword}
                  onChange={(e) => setAdminPassword(e.target.value)}
                />
              </Field>
              <button
                type="submit"
                disabled={adminSaving}
                className="w-full rounded bg-emerald-600 py-2 text-xs font-semibold text-white hover:bg-emerald-700 disabled:opacity-75"
              >
                {adminSaving ? "Adding Admin..." : "Add Administrator"}
              </button>
            </form>
          </div>
        </div>
      </Section>

      <Section title="Monitoring Notes" icon={<RadioTower size={18} className="text-emerald-600" />}>
        <div className="rounded border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
          These controls now drive backend behavior for booking, payment, scheduled notifications, failover escalation,
          and automated digest generation. Live effect starts only after functions deploy.
        </div>
      </Section>

      <div className="rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
        <div className="flex gap-3">
          <AlertTriangle size={18} className="mt-0.5 shrink-0" />
          <p>
            Global kill switch and payment blocking are high-impact controls. Use them only for real incidents or
            planned maintenance windows.
          </p>
        </div>
      </div>
    </AdminShell>
  );
}

function Section({ title, icon, children }) {
  return (
    <div className="mb-6 rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
      <h3 className="mb-6 flex items-center gap-2 border-b pb-3 text-sm font-semibold text-slate-800">
        {icon}
        {title}
      </h3>
      {children}
    </div>
  );
}

function Field({ label, children, className = "" }) {
  return (
    <div className={`space-y-1 ${className}`}>
      <label className="text-xs font-semibold uppercase text-slate-500">{label}</label>
      {children}
    </div>
  );
}

function ToggleCard({ title, description, checked, onChange, tone = "amber" }) {
  const palette = tone === "red"
    ? "border-red-200 bg-red-50"
    : "border-amber-200 bg-amber-50";

  return (
    <label className={`flex cursor-pointer items-start justify-between gap-4 rounded border p-4 ${palette}`}>
      <div>
        <div className="text-sm font-semibold text-slate-800">{title}</div>
        <div className="mt-1 text-xs text-slate-600">{description}</div>
      </div>
      <input type="checkbox" className="mt-1 h-4 w-4" checked={checked} onChange={(e) => onChange(e.target.checked)} />
    </label>
  );
}
