"use client";

import { Activity, AlertTriangle, MonitorSmartphone } from "lucide-react";
import AdminShell from "@/components/AdminShell";
import { useApiCollection } from "@/lib/useApiCollection";

export default function ActivityTimelinePage() {
  const { rows: timeline, loading: timelineLoading } = useApiCollection(null, "activity-timeline");
  const { rows: clientErrors } = useApiCollection(null, "client-errors");
  const { rows: risks } = useApiCollection(null, "activity-risks");

  return (
    <AdminShell className="p-6">
      <header className="mb-6">
        <h1 className="text-2xl font-bold text-slate-800 flex items-center gap-2">
          <Activity size={22} /> Activity Timeline
        </h1>
        <p className="text-sm text-slate-500">24/7 event-driven backend timeline, client errors, and risk escalation surfaces.</p>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <StatCard label="Timeline Events" value={timeline.length} tone="blue" />
        <StatCard label="Client Errors" value={clientErrors.length} tone="red" />
        <StatCard label="Open Risks" value={risks.filter((item) => (item.status || "Open") === "Open").length} tone="amber" />
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
        <Panel title="Live Activity Feed" icon={<Activity size={17} />} span="xl:col-span-2">
          {timelineLoading ? <Empty text="Loading timeline..." /> : timeline.length === 0 ? <Empty text="No activity captured yet." /> : timeline.map((item) => (
            <div key={item.id} className="rounded-lg border border-slate-200 bg-slate-50 p-4">
              <div className="flex items-center justify-between gap-3">
                <div>
                  <p className="font-semibold text-slate-800">{item.eventType || "Event"}</p>
                  <p className="text-xs text-slate-500">{item.entityType || "system"} • {item.entityId || "-"}</p>
                </div>
                <span className="text-xs text-slate-400">{formatDate(item.createdAt)}</span>
              </div>
              <p className="mt-2 text-sm text-slate-600">{item.message || "-"}</p>
              <div className="mt-2 flex flex-wrap gap-2 text-[11px] text-slate-500">
                {item.bookingId && <Tag>Booking {item.bookingId}</Tag>}
                {item.status && <Tag>{item.status}</Tag>}
                {item.district && <Tag>{item.district}</Tag>}
                {Array.isArray(item.changedKeys) && item.changedKeys.length > 0 && <Tag>{item.changedKeys.join(", ")}</Tag>}
              </div>
            </div>
          ))}
        </Panel>

        <Panel title="Client Error Stream" icon={<MonitorSmartphone size={17} />}>
          {clientErrors.length === 0 ? <Empty text="No web-admin client errors logged." /> : clientErrors.map((item) => (
            <div key={item.id} className="rounded-lg border border-slate-200 bg-slate-50 p-3">
              <div className="flex items-center justify-between gap-3">
                <b className="text-sm text-slate-800">{item.source || "web-admin"}</b>
                <span className="text-[11px] uppercase text-red-600 font-semibold">{item.level || "error"}</span>
              </div>
              <p className="mt-1 text-sm text-slate-600">{item.message}</p>
              <p className="mt-1 text-xs text-slate-400">{item.page || "-"} • {formatDate(item.createdAt)}</p>
            </div>
          ))}
        </Panel>

        <Panel title="Risk Escalations" icon={<AlertTriangle size={17} />}>
          {risks.length === 0 ? <Empty text="No risk escalations right now." /> : risks.map((item) => (
            <div key={item.id} className="rounded-lg border border-slate-200 bg-slate-50 p-3">
              <div className="flex items-center justify-between gap-3">
                <b className="text-sm text-slate-800">{item.type || "Risk"}</b>
                <span className={`text-[11px] font-semibold uppercase ${item.severity === "high" ? "text-red-600" : "text-amber-600"}`}>
                  {item.severity || "medium"}
                </span>
              </div>
              <p className="mt-1 text-sm text-slate-600">{item.message || "-"}</p>
              <p className="mt-1 text-xs text-slate-400">{formatDate(item.createdAt)}</p>
            </div>
          ))}
        </Panel>
      </div>
    </AdminShell>
  );
}

function Panel({ title, icon, span = "", children }) {
  return (
    <section className={`rounded-lg border border-slate-200 bg-white p-5 shadow-sm ${span}`}>
      <h2 className="mb-4 flex items-center gap-2 font-semibold text-slate-800">{icon}{title}</h2>
      <div className="space-y-3 max-h-[720px] overflow-y-auto">{children}</div>
    </section>
  );
}

function StatCard({ label, value, tone }) {
  const tones = {
    blue: "bg-blue-50 text-blue-700",
    red: "bg-red-50 text-red-700",
    amber: "bg-amber-50 text-amber-700",
  };
  return (
    <div className={`rounded-lg border border-slate-200 p-4 ${tones[tone]}`}>
      <p className="text-xs font-semibold uppercase">{label}</p>
      <p className="mt-1 text-2xl font-bold">{value}</p>
    </div>
  );
}

function Tag({ children }) {
  return <span className="rounded bg-white px-2 py-1 border border-slate-200">{children}</span>;
}

function Empty({ text }) {
  return <p className="text-sm text-slate-400">{text}</p>;
}

function formatDate(value) {
  if (!value) return "-";
  if (value?.toDate) return value.toDate().toLocaleString();
  if (value?.seconds) return new Date(value.seconds * 1000).toLocaleString();
  return String(value);
}
