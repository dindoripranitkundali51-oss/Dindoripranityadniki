"use client";

import { useEffect, useState } from "react";
import AdminShell from "@/components/AdminShell";
import { ActionNotice } from "@/components/ActionDialog";
import { Loader2, Plus, Trash, Wifi, Key, Link2, ShieldCheck } from "lucide-react";

export default function WebhookSettingsPage() {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [testing, setTesting] = useState(false);
  const [webhooks, setWebhooks] = useState([]);
  const [notice, setNotice] = useState(null);
  
  const [form, setForm] = useState({
    url: "",
    secret: "",
    isActive: true
  });

  useEffect(() => {
    fetchWebhooks();
  }, []);

  const fetchWebhooks = async () => {
    try {
      const baseUrl = process.env.NEXT_PUBLIC_API_URL || "https://dindoritrial.somee.com/api/v1";
      const token = localStorage.getItem("jwt_auth_token") || "";

      const res = await fetch(`${baseUrl}/webhook`, {
        headers: { "Authorization": `Bearer ${token}` }
      });
      if (!res.ok) throw new Error("Failed to load webhook configuration.");
      const data = await res.json();
      setWebhooks(data.data || []);
    } catch (err) {
      setNotice({ tone: "danger", message: err.message });
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    setNotice(null);

    try {
      const baseUrl = process.env.NEXT_PUBLIC_API_URL || "https://dindoritrial.somee.com/api/v1";
      const token = localStorage.getItem("jwt_auth_token") || "";

      const res = await fetch(`${baseUrl}/webhook`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${token}`
        },
        body: JSON.stringify(form)
      });

      if (!res.ok) {
        const errData = await res.json().catch(() => ({}));
        throw new Error(errData.message || "Failed to save webhook settings.");
      }

      setNotice({ tone: "success", message: "Webhook saved successfully!" });
      setForm({ url: "", secret: "", isActive: true });
      fetchWebhooks();
    } catch (err) {
      setNotice({ tone: "danger", message: err.message });
    } finally {
      setSaving(false);
    }
  };

  const handleTest = async () => {
    setTesting(true);
    setNotice(null);

    try {
      const baseUrl = process.env.NEXT_PUBLIC_API_URL || "https://dindoritrial.somee.com/api/v1";
      const token = localStorage.getItem("jwt_auth_token") || "";

      const res = await fetch(`${baseUrl}/webhook/test`, {
        method: "POST",
        headers: { "Authorization": `Bearer ${token}` }
      });

      if (res.ok) {
        setNotice({ tone: "success", message: "Test trigger payload successfully dispatched to active webhooks." });
      } else {
        throw new Error("Webhook test invocation failed.");
      }
    } catch (err) {
      setNotice({ tone: "danger", message: err.message });
    } finally {
      setTesting(false);
    }
  };

  return (
    <AdminShell className="p-6">
      <ActionNotice
        tone={notice?.tone}
        message={notice?.message}
        onClose={() => setNotice(null)}
      />

      <header className="mb-8">
        <h1 className="text-2xl font-black text-slate-800 uppercase tracking-wide">Tally / Accounting Webhooks</h1>
        <p className="text-slate-500 text-sm">Automatically sync all financial ledger entries and payout transactions to external bookkeeping systems.</p>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        
        {/* Create Webhook Form */}
        <div className="bg-white border border-slate-200 rounded-[30px] p-6 shadow-sm">
          <h3 className="font-bold text-slate-800 text-sm uppercase tracking-wider mb-4 flex items-center gap-2">
            <Plus size={16} className="text-blue-600" /> Save Webhook EndPoint
          </h3>

          <form onSubmit={handleSave} className="space-y-4">
            <div>
              <label className="block text-[10px] font-bold text-slate-400 uppercase mb-2">Endpoint URL</label>
              <input 
                type="url"
                className="w-full border border-slate-200 bg-slate-50 p-3.5 rounded-xl text-xs outline-none focus:border-blue-500 text-slate-700"
                placeholder="https://tally.yoursite.com/webhook"
                value={form.url}
                onChange={(e) => setForm({ ...form, url: e.target.value })}
                required
              />
            </div>

            <div>
              <label className="block text-[10px] font-bold text-slate-400 uppercase mb-2">Secret Token (HMAC-SHA256 signature verification)</label>
              <input 
                type="text"
                className="w-full border border-slate-200 bg-slate-50 p-3.5 rounded-xl text-xs outline-none focus:border-blue-500 text-slate-700"
                placeholder="Secure Signing Key"
                value={form.secret}
                onChange={(e) => setForm({ ...form, secret: e.target.value })}
              />
            </div>

            <div className="flex items-center justify-between py-2">
              <span className="text-xs text-slate-500 font-bold uppercase">Activate on Sync Events</span>
              <input 
                type="checkbox"
                className="w-4 h-4 rounded text-blue-600 focus:ring-blue-500"
                checked={form.isActive}
                onChange={(e) => setForm({ ...form, isActive: e.target.checked })}
              />
            </div>

            <button 
              type="submit"
              disabled={saving}
              className="w-full bg-blue-600 hover:bg-blue-700 text-white py-3 rounded-xl text-xs font-bold uppercase tracking-wider transition-colors"
            >
              {saving ? <Loader2 className="animate-spin mx-auto" size={16} /> : "Save Settings"}
            </button>
          </form>
        </div>

        {/* Existing Webhooks List */}
        <div className="lg:col-span-2 bg-white border border-slate-200 rounded-[30px] p-6 shadow-sm flex flex-col justify-between">
          <div>
            <div className="flex justify-between items-center mb-6">
              <h3 className="font-bold text-slate-800 text-sm uppercase tracking-wider flex items-center gap-2">
                <Link2 size={16} className="text-blue-600" /> Configured Subscriptions
              </h3>

              <button 
                onClick={handleTest}
                disabled={testing || webhooks.length === 0}
                className="flex items-center gap-1 bg-slate-100 hover:bg-slate-200 text-slate-700 text-[10px] font-bold uppercase tracking-widest px-3 py-1.5 rounded-lg transition-colors"
              >
                {testing ? <Loader2 className="animate-spin" size={12} /> : <Wifi size={12} />} Test Sync
              </button>
            </div>

            {loading ? (
              <div className="py-12 text-center text-slate-400">
                <Loader2 className="animate-spin mx-auto mb-2 text-blue-600" />
                <p className="text-xs italic">Loading configured endpoints...</p>
              </div>
            ) : webhooks.length === 0 ? (
              <div className="py-16 text-center text-slate-400 border border-dashed rounded-2xl bg-slate-50">
                <p className="text-xs">No webhooks registered yet.</p>
              </div>
            ) : (
              <div className="space-y-4">
                {webhooks.map((w) => (
                  <div key={w.id} className="bg-slate-50 border p-4 rounded-xl flex items-center justify-between">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className={`w-2.5 h-2.5 rounded-full ${w.isActive ? "bg-green-500" : "bg-red-400"}`}></span>
                        <p className="text-xs font-bold text-slate-800 break-all">{w.url}</p>
                      </div>
                      
                      {w.secret && (
                        <div className="flex items-center gap-1 mt-2 text-[10px] text-slate-400 font-mono">
                          <Key size={12} /> Secret: {w.secret.slice(0, 4)}***
                        </div>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
          
          <div className="mt-8 pt-4 border-t border-slate-100 flex items-center gap-2 text-[10px] text-slate-400 font-bold uppercase">
            <ShieldCheck size={14} className="text-green-500" /> All payload transmissions are encrypted via SSL/TLS.
          </div>
        </div>

      </div>
    </AdminShell>
  );
}
