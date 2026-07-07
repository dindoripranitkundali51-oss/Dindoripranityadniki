"use client";
import { useState } from "react";
import AdminShell from "@/components/AdminShell";
import { ActionNotice } from "@/components/ActionDialog";
import Link from "next/link";
import { Send, Bell, Loader2, Calendar } from "lucide-react";
import { callAdminApi } from "@/lib/apiClient";
import { useApiCollection } from "@/lib/useApiCollection";
import { useLanguage } from "@/context/LanguageContext";

export default function CommunicationHub() {
  const { t } = useLanguage();
  const [notif, setNotif] = useState({ title: "", body: "", topic: "all", scheduledTime: "" });
  const [mode, setMode] = useState("instant"); // instant or schedule
  const [sending, setSending] = useState(false);
  const [notice, setNotice] = useState(null);
  const { rows: logs, loading } = useApiCollection(null, "notification-logs");
  const { rows: scheduled } = useApiCollection(null, "scheduled-notifications");

  const sendNotification = async (e) => {
    e.preventDefault();
    setSending(true);
    try {
      if (mode === "instant") {
        await callAdminApi("sendAdminNotification", {
          target: notif.topic === "guruji" ? "Provider" : notif.topic === "yajman" ? "User" : "All",
          title: notif.title,
          message: notif.body,
        });
        setNotice({ tone: "success", message: "Broadcast sent successfully." });
      } else {
        if (!notif.scheduledTime) throw new Error("Please select a schedule time.");
        await callAdminApi("scheduleNotification", {
          title: notif.title,
          body: notif.body,
          targetTopic: notif.topic,
          scheduledTime: new Date(notif.scheduledTime).getTime(),
        });
        setNotice({ tone: "success", message: "Notification scheduled successfully." });
      }
      setNotif({ title: "", body: "", topic: "all", scheduledTime: "" });
    } catch (e) {
      setNotice({ tone: "danger", message: e.message || "Notification action failed." });
    }
    setSending(false);
  };

  return (
    <AdminShell className="p-6">
      <ActionNotice
        tone={notice?.tone}
        message={notice?.message}
        onClose={() => setNotice(null)}
      />
      <header className="mb-6">
        <h1 className="text-2xl font-bold text-slate-800">{t("communication") || "Communication Hub"}</h1>
        <p className="text-slate-500 text-sm">Send instant alerts or schedule them for later delivery.</p>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 bg-white p-6 rounded-lg border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between mb-6 border-b pb-3">
            <h3 className="font-semibold text-slate-800 flex items-center gap-2"><Send size={18} /> New Message</h3>
            <div className="flex bg-slate-100 p-1 rounded text-xs font-medium">
              <button onClick={() => setMode("instant")} className={`px-3 py-1 rounded ${mode === "instant" ? "bg-white shadow-sm text-blue-600" : "text-slate-500"}`}>Send Now</button>
              <button onClick={() => setMode("schedule")} className={`px-3 py-1 rounded ${mode === "schedule" ? "bg-white shadow-sm text-blue-600" : "text-slate-500"}`}>Schedule</button>
            </div>
          </div>
          
          <form onSubmit={sendNotification} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <Field label="Target Audience">
                <select className="w-full border p-2.5 rounded text-sm outline-none bg-slate-50" value={notif.topic} onChange={(e) => setNotif({ ...notif, topic: e.target.value })}>
                  <option value="all">Everyone</option>
                  <option value="yajman">Yajmans Only</option>
                  <option value="guruji">Gurujis Only</option>
                </select>
              </Field>
              {mode === "schedule" && (
                <Field label="Schedule Time">
                  <input type="datetime-local" className="w-full border p-2 rounded text-sm" value={notif.scheduledTime} onChange={(e) => setNotif({ ...notif, scheduledTime: e.target.value })} />
                </Field>
              )}
            </div>
            <Field label="Title">
              <input type="text" required className="w-full border p-2.5 rounded text-sm" placeholder="Notification title..." value={notif.title} onChange={(e) => setNotif({ ...notif, title: e.target.value })} />
            </Field>
            <Field label="Message Body">
              <textarea required className="w-full border p-2.5 rounded text-sm min-h-[100px]" placeholder="Type your message..." value={notif.body} onChange={(e) => setNotif({ ...notif, body: e.target.value })} />
            </Field>
            <button disabled={sending} className="w-full bg-blue-600 text-white py-3 rounded font-bold hover:bg-blue-700 flex items-center justify-center gap-2 transition-all">
              {sending ? <Loader2 className="animate-spin" /> : mode === "instant" ? <Send size={18} /> : <Calendar size={18} />}
              {mode === "instant" ? "Broadcast Now" : "Schedule Notification"}
            </button>
          </form>
        </div>

        <div className="space-y-6">
          <div className="bg-white p-5 rounded-lg border shadow-sm">
            <h4 className="font-bold text-sm mb-4 flex items-center gap-2 text-amber-600"><Calendar size={16} /> Scheduled</h4>
            <div className="space-y-3 max-h-[300px] overflow-auto">
              {scheduled.length === 0 ? <p className="text-xs text-slate-400">No scheduled notifications.</p> : scheduled.map(s => (
                <div key={s.id} className="p-2 border rounded bg-amber-50/50 text-xs">
                  <div className="flex justify-between font-bold"><span>{s.title}</span><span className="text-amber-700">{s.status}</span></div>
                  <p className="text-slate-500 mt-1">Target: {s.targetTopic}</p>
                  <p className="text-slate-600 font-medium mt-1">Time: {formatDate(s.scheduledTime)}</p>
                </div>
              ))}
            </div>
          </div>
          
          <div className="bg-white p-5 rounded-lg border shadow-sm">
            <h4 className="font-bold text-sm mb-4 flex items-center gap-2 text-blue-600"><Bell size={16} /> History</h4>
            <div className="space-y-3 max-h-[400px] overflow-auto">
              {logs.map(log => (
                <div key={log.id} className="p-2 border-b last:border-0 text-xs">
                  <div className="flex justify-between gap-3">
                    <b>{log.meta?.title || log.action || "Notification Log"}</b>
                    <span className="text-slate-400">{formatDate(log.createdAt)}</span>
                  </div>
                  <p className="truncate text-slate-500">{log.meta?.body || log.failureReason || log.target || "-"}</p>
                  <p className="mt-1 text-[11px] text-slate-400">
                    Status: {log.status || "logged"} | Channel: {log.channel || "system"}{log.failoverState ? ` | Failover: ${log.failoverState}` : ""}
                  </p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </AdminShell>
  );
}

function Field({ label, children }) {
  return <div className="space-y-1"><label className="text-[10px] font-bold text-slate-400 uppercase">{label}</label>{children}</div>;
}

function formatDate(value) {
  if (!value) return "-";
  const date = value?.toDate ? value.toDate() : value?.seconds ? new Date(value.seconds * 1000) : new Date(value);
  return date.toLocaleString();
}
