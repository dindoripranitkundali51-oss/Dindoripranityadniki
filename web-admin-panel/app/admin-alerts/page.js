"use client";
import { useMemo, useState } from "react";
import AdminShell from "@/components/AdminShell";
import Link from "next/link";
import { AlertTriangle, CheckCircle, Clock, Loader2, XCircle } from "lucide-react";
import { useApiCollection } from "@/lib/useApiCollection";

export default function AdminAlerts() {
  const [filter, setFilter] = useState("All");
  const [statusFilter, setStatusFilter] = useState("Open");

  const { rows: notifications, loading } = useApiCollection(null, "admin-notifications");

  const filtered = useMemo(() => {
    return notifications.filter((item) => {
      const typeOk = filter === "All" || item.type === filter;
      const itemStatus = item.status || "Open";
      const statusOk = statusFilter === "All" || itemStatus === statusFilter;
      return typeOk && statusOk;
    });
  }, [notifications, filter, statusFilter]);

  return (
    <AdminShell className="p-6">
      <header className="mb-6">
        <h1 className="text-2xl font-bold text-slate-800">Admin Alerts</h1>
        <p className="text-sm text-slate-500">System notifications that need admin attention.</p>
      </header>

      <div className="mb-6 flex w-fit gap-1 rounded border border-slate-300 bg-white p-1">
        {["All", "REASSIGNMENT_FAILED", "ACCEPT_TIMEOUT_MAX_RETRIES", "ACCEPT_TIMEOUT_NO_GURUJI"].map((type) => (
          <button
            key={type}
            onClick={() => setFilter(type)}
            className={`rounded px-4 py-1.5 text-xs font-medium transition-colors ${
              filter === type ? "bg-blue-600 text-white" : "text-slate-600 hover:bg-slate-50"
            }`}
          >
            {typeLabel(type)}
          </button>
        ))}
      </div>

      <div className="mb-6 flex w-fit gap-1 rounded border border-slate-300 bg-white p-1">
        {["Open", "Resolved", "All"].map((status) => (
          <button
            key={status}
            onClick={() => setStatusFilter(status)}
            className={`rounded px-4 py-1.5 text-xs font-medium transition-colors ${
              statusFilter === status ? "bg-slate-800 text-white" : "text-slate-600 hover:bg-slate-50"
            }`}
          >
            {status}
          </button>
        ))}
      </div>

      <div className="space-y-4">
        {loading ? (
          <div className="py-20 text-center text-slate-400">
            <Loader2 className="mx-auto mb-2 animate-spin text-blue-600" />
            <p className="text-sm italic">Loading admin alerts...</p>
          </div>
        ) : filtered.length === 0 ? (
          <div className="rounded-lg border bg-white py-16 text-center text-slate-400">
            No admin alerts found for this filter.
          </div>
        ) : (
          filtered.map((alert) => (
            <div key={alert.id} className={`rounded-lg border p-5 shadow-sm ${typeColor(alert.type, alert.status)}`}>
              <div className="flex items-start gap-3">
                <div className="mt-1">{typeIcon(alert.type)}</div>
                <div className="flex-1">
                  <div className="mb-2 flex items-center justify-between gap-3">
                    <div>
                      <h3 className="text-sm font-bold text-slate-800">{typeLabel(alert.type)}</h3>
                      <p className="mt-1 text-[11px] uppercase tracking-wide text-slate-500">{alert.status || "Open"}</p>
                    </div>
                    <span className="text-xs text-slate-500">{formatDate(alert.createdAt)}</span>
                  </div>
                  <p className="mb-2 text-sm text-slate-700">{alert.message}</p>
                  {alert.resolutionNote ? <p className="mb-2 text-xs text-slate-500">Resolution: {alert.resolutionNote}</p> : null}
                  {alert.bookingId ? (
                    <Link href={`/bookings?search=${alert.bookingId}`} className="text-xs text-blue-600 hover:underline">
                      Open booking: {alert.bookingId}
                    </Link>
                  ) : null}
                </div>
              </div>
            </div>
          ))
        )}
      </div>
    </AdminShell>
  );
}

function typeIcon(type) {
  switch (type) {
    case "REASSIGNMENT_FAILED":
      return <AlertTriangle className="text-red-600" size={18} />;
    case "ACCEPT_TIMEOUT_MAX_RETRIES":
      return <Clock className="text-orange-600" size={18} />;
    case "ACCEPT_TIMEOUT_NO_GURUJI":
      return <XCircle className="text-red-600" size={18} />;
    default:
      return <CheckCircle className="text-green-600" size={18} />;
  }
}

function typeColor(type, status) {
  if (status === "Resolved") {
    return "border-green-200 bg-green-50";
  }
  switch (type) {
    case "REASSIGNMENT_FAILED":
      return "border-red-200 bg-red-50";
    case "ACCEPT_TIMEOUT_MAX_RETRIES":
      return "border-orange-200 bg-orange-50";
    case "ACCEPT_TIMEOUT_NO_GURUJI":
      return "border-red-200 bg-red-50";
    default:
      return "border-blue-200 bg-blue-50";
  }
}

function typeLabel(type) {
  switch (type) {
    case "All":
      return "All alerts";
    case "REASSIGNMENT_FAILED":
      return "Reassignment failed";
    case "ACCEPT_TIMEOUT_MAX_RETRIES":
      return "Accept timeout after max retries";
    case "ACCEPT_TIMEOUT_NO_GURUJI":
      return "No guruji accepted in time";
    default:
      return String(type || "System alert").replace(/_/g, " ");
  }
}

function formatDate(value) {
  if (!value) return "-";
  const date = value?.toDate ? value.toDate() : value?.seconds ? new Date(value.seconds * 1000) : new Date(value);
  return date.toLocaleString();
}
