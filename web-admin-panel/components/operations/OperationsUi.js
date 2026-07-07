"use client";

import { AlertTriangle } from "lucide-react";

export function OperationsPanel({ title, icon, children }) {
  return (
    <section className="bg-white border border-slate-200 rounded-lg p-4 shadow-sm">
      <h2 className="font-bold text-slate-800 mb-4 flex items-center gap-2">{icon}{title}</h2>
      <div className="space-y-3 max-h-[560px] overflow-y-auto">{children}</div>
    </section>
  );
}

export function OperationsStat({ label, value, tone }) {
  const colors = {
    red: "text-red-700 bg-red-50",
    amber: "text-amber-700 bg-amber-50",
    blue: "text-blue-700 bg-blue-50",
    slate: "text-slate-700 bg-slate-50",
  };
  return (
    <div className={`border rounded-lg p-4 ${colors[tone] || colors.slate}`}>
      <p className="text-xs font-semibold uppercase">{label}</p>
      <p className="text-2xl font-bold">{value}</p>
    </div>
  );
}

export function EmptyState() {
  return <p className="text-sm text-slate-400">No records found for this section.</p>;
}

export function StatusPill({ label, active }) {
  return (
    <div className="flex items-center justify-between rounded border border-slate-200 bg-slate-50 px-3 py-2">
      <span>{label}</span>
      <span className={`rounded px-2 py-0.5 text-xs font-semibold ${active ? "bg-red-100 text-red-700" : "bg-green-100 text-green-700"}`}>
        {active ? "ON" : "OFF"}
      </span>
    </div>
  );
}

export function RiskRow({ risk, formatDate }) {
  return (
    <div className="border rounded p-3 bg-slate-50 text-sm">
      <div className="flex justify-between gap-3">
        <b>{risk.type}</b>
        <span className={`text-xs px-2 py-0.5 rounded ${risk.severity === "high" ? "bg-red-100 text-red-700" : "bg-amber-100 text-amber-700"}`}>{risk.severity || "medium"}</span>
      </div>
      <p className="text-slate-700">{risk.message}</p>
      <p className="text-xs text-slate-500">Booking {risk.bookingId || "-"} | Guruji {risk.gurujiId || "-"} | {formatDate(risk.createdAt)}</p>
    </div>
  );
}

export function DeadLetterRow({ item, canReplayDeadLetter, loadingId, onReplay }) {
  return (
    <div className="border rounded p-3 bg-slate-50 text-sm">
      <div className="flex justify-between gap-3">
        <b>{item.type || "Dead letter"}</b>
        <span className="text-xs px-2 py-0.5 rounded bg-red-100 text-red-700">{item.status || "Open"}</span>
      </div>
      <p className="text-slate-500 text-xs mt-1">Entity: {item.entityId || "-"} | Severity: {item.severity || "-"}</p>
      <p className="mt-2 text-slate-700">{item.reason || item.error?.message || "-"}</p>
      {canReplayDeadLetter && item.status !== "Replayed" && (
        <button
          className="mt-3 px-3 py-1.5 bg-slate-800 text-white rounded text-xs font-semibold"
          onClick={() => onReplay(item.id)}
        >
          {loadingId === item.id ? "Working..." : "Replay / escalate"}
        </button>
      )}
    </div>
  );
}

export function DashboardHealthCard({ opsDashboard }) {
  return (
    <div className="border rounded p-3 bg-slate-50 text-sm">
      <div>Open dead letters: <b>{opsDashboard?.deadLetterSummary?.open || 0}</b></div>
      <div>Critical dead letters: <b>{opsDashboard?.deadLetterSummary?.critical || 0}</b></div>
      <div>Health score: <b>{opsDashboard?.liveHealth?.healthScore ?? "-"}</b></div>
      <div>Open risks: <b>{opsDashboard?.liveHealth?.openRisks ?? opsDashboard?.digest?.openRisks ?? 0}</b></div>
    </div>
  );
}

export function DefaultPanelIcon() {
  return <AlertTriangle size={17} />;
}
