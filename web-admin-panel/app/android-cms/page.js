"use client";

import { useEffect, useState } from "react";
import { Loader2, Save, Smartphone } from "lucide-react";
import AdminShell from "@/components/AdminShell";
import { callAdminApi } from "@/lib/apiClient";

const DEFAULTS = {
  app_banner_en: "",
  app_banner_mr: "",
  booking_primary_cta_en: "Start Seva",
  booking_primary_cta_mr: "सेवा सुरू करा",
  support_whatsapp: "",
  enable_feedback_prompt: true,
  force_refresh_after_hours: 12,
};

export default function AndroidCmsPage() {
  const [form, setForm] = useState(DEFAULTS);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState("");

  useEffect(() => {
    const loadCms = async () => {
      try {
        const baseUrl = process.env.NEXT_PUBLIC_API_URL || "https://dindoripranitapi.somee.com/api/v1";
        const token = typeof window !== "undefined" ? localStorage.getItem("jwt_auth_token") || "" : "";
        const res = await fetch(`${baseUrl}/admin/cms`, {
          headers: {
            "Authorization": token ? `Bearer ${token}` : ""
          }
        });
        if (res.ok) {
          const data = await res.json();
          if (data.success && data.config) {
            setForm((prev) => ({ ...prev, ...data.config }));
          }
        }
      } catch (err) {
        console.error("Failed to load CMS settings:", err);
      } finally {
        setLoading(false);
      }
    };
    loadCms();
  }, []);

  const save = async () => {
    setSaving(true);
    setNotice("");
    try {
      await callAdminApi("saveCms", { config: form });
      setNotice("Android CMS config saved.");
    } catch (err) {
      setNotice("Failed to save CMS config: " + err.message);
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <AdminShell className="p-10 text-center">
        <Loader2 className="mx-auto animate-spin text-blue-600" />
      </AdminShell>
    );
  }

  return (
    <AdminShell className="p-6">
      <header className="mb-6 flex items-center justify-between gap-4">
        <div>
          <h1 className="flex items-center gap-2 text-2xl font-bold text-slate-800"><Smartphone size={22} /> Dynamic Android CMS</h1>
          <p className="text-sm text-slate-500">Centralized Android-facing copy/config contract for app-side rollout controls.</p>
        </div>
        <button onClick={save} disabled={saving} className="flex items-center gap-2 rounded bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-70">
          {saving ? <Loader2 size={16} className="animate-spin" /> : <Save size={16} />}
          Save
        </button>
      </header>

      {notice && <div className="mb-4 rounded border border-green-200 bg-green-50 px-3 py-2 text-sm text-green-700">{notice}</div>}

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Field label="App Banner EN">
          <textarea className="min-h-[120px] w-full rounded border p-2.5" value={form.app_banner_en} onChange={(e) => setForm((prev) => ({ ...prev, app_banner_en: e.target.value }))} />
        </Field>
        <Field label="App Banner MR">
          <textarea className="min-h-[120px] w-full rounded border p-2.5" value={form.app_banner_mr} onChange={(e) => setForm((prev) => ({ ...prev, app_banner_mr: e.target.value }))} />
        </Field>
        <Field label="Primary Booking CTA EN">
          <input className="w-full rounded border p-2.5" value={form.booking_primary_cta_en} onChange={(e) => setForm((prev) => ({ ...prev, booking_primary_cta_en: e.target.value }))} />
        </Field>
        <Field label="Primary Booking CTA MR">
          <input className="w-full rounded border p-2.5" value={form.booking_primary_cta_mr} onChange={(e) => setForm((prev) => ({ ...prev, booking_primary_cta_mr: e.target.value }))} />
        </Field>
        <Field label="Support WhatsApp">
          <input className="w-full rounded border p-2.5" value={form.support_whatsapp} onChange={(e) => setForm((prev) => ({ ...prev, support_whatsapp: e.target.value }))} />
        </Field>
        <Field label="Force Refresh After Hours">
          <input type="number" min="1" className="w-full rounded border p-2.5" value={form.force_refresh_after_hours} onChange={(e) => setForm((prev) => ({ ...prev, force_refresh_after_hours: e.target.value }))} />
        </Field>
        <label className="flex items-center justify-between rounded border border-slate-200 bg-white px-4 py-3 lg:col-span-2">
          <div>
            <p className="font-semibold text-slate-800">Enable Feedback Prompt</p>
            <p className="text-sm text-slate-500">App-side flow can use this flag to open post-service feedback automatically.</p>
          </div>
          <input type="checkbox" checked={Boolean(form.enable_feedback_prompt)} onChange={(e) => setForm((prev) => ({ ...prev, enable_feedback_prompt: e.target.checked }))} />
        </label>
      </div>
    </AdminShell>
  );
}

function Field({ label, children }) {
  return (
    <div className="space-y-1">
      <label className="text-xs font-semibold uppercase text-slate-500">{label}</label>
      {children}
    </div>
  );
}
