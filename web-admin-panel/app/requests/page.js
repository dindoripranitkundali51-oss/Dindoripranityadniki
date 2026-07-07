"use client";

import { useMemo, useState } from "react";
import AdminShell from "@/components/AdminShell";
import { useApiCollection } from "@/lib/useApiCollection";
import { Loader2 } from "lucide-react";

const DEFAULT_AUTO_APPROVE_MINUTES = 5;

export default function RescheduleCancelManagement() {
  const [filter, setFilter] = useState("Pending");
  const [autoApproveMinutes, setAutoApproveMinutes] = useState(DEFAULT_AUTO_APPROVE_MINUTES);

  const { rows: requests, loading } = useApiCollection(null, "booking-requests-v2");

  const filtered = useMemo(() => {
    return requests.filter((request) => filter === "All" || request.status === filter);
  }, [requests, filter]);

  return (
    <AdminShell className="p-6">
      <header className="mb-6">
        <h1 className="text-2xl font-bold text-slate-800">Service Requests</h1>
        <p className="text-slate-500 text-sm">Cancellation and reschedule requests are handled by backend autopilot. This page is supervision-only.</p>
      </header>

      <div className="mb-6 rounded-lg border border-blue-100 bg-blue-50 px-4 py-3 text-sm text-blue-900">
        Pending requests are auto-approved by scheduled autopilot after about {autoApproveMinutes} minute(s). Manual approve/reject controls have been removed from the admin panel.
      </div>

      <div className="mb-6 flex w-fit gap-1 rounded border border-slate-300 bg-white p-1">
        {["Pending", "Approved", "Rejected", "All"].map((status) => (
          <button
            key={status}
            onClick={() => setFilter(status)}
            className={`rounded px-4 py-1.5 text-xs font-medium transition-colors ${filter === status ? "bg-blue-600 text-white" : "text-slate-600 hover:bg-slate-50"}`}
          >
            {status}
          </button>
        ))}
      </div>

      <div className="space-y-4">
        {loading ? (
          <div className="py-20 text-center text-slate-400">
            <Loader2 className="animate-spin mx-auto mb-2 text-blue-600" />
          </div>
        ) : filtered.length === 0 ? (
          <div className="py-16 text-center text-slate-400 bg-white border rounded-lg">No service requests found for this filter.</div>
        ) : (
          filtered.map((request) => {
            const autoState = getAutoState(request, autoApproveMinutes);
            return (
              <div key={request.id} className="bg-white p-5 rounded-lg border border-slate-200 shadow-sm">
                <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
                  <div className="flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase ${request.type === "Cancel" ? "bg-red-100 text-red-700" : "bg-blue-100 text-blue-700"}`}>
                        {request.type || "Request"}
                      </span>
                      <span className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase ${statusClass(request.status)}`}>
                        {request.status || "Pending"}
                      </span>
                    </div>
                    <h3 className="text-base font-bold text-slate-800 mt-2">{request.bookingId || "Booking request"}</h3>
                    <p className="mt-3 text-sm text-slate-600 italic">&ldquo;{request.reason || "No reason provided."}&rdquo;</p>
                    <div className="mt-4 grid grid-cols-1 gap-3 text-xs text-slate-500 md:grid-cols-3">
                      <Meta label="Created At" value={formatDate(request.createdAt)} />
                      <Meta label="Last Update" value={formatDate(request.updatedAt || request.createdAt)} />
                      <Meta label="Automation Note" value={request.adminNote || request.autoResolutionReason || "Autopilot-managed"} />
                    </div>
                  </div>

                  <div className="w-full rounded-lg border border-slate-200 bg-slate-50 px-4 py-3 md:w-72">
                    <p className="text-xs font-semibold uppercase text-slate-500">Automation State</p>
                    <p className="mt-2 text-sm font-semibold text-slate-800">{autoState.title}</p>
                    <p className="mt-1 text-xs text-slate-500">{autoState.detail}</p>
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>
    </AdminShell>
  );
}

function getAutoState(request, autoApproveMinutes) {
  const status = String(request.status || "Pending");
  if (status === "Approved") {
    return {
      title: "Auto-approved",
      detail: request.adminNote || "Completed by backend autopilot.",
    };
  }
  if (status === "Rejected") {
    return {
      title: "Rejected",
      detail: request.adminNote || "Closed by business logic or previous review.",
    };
  }
  const createdMs = toMillis(request.createdAt);
  const cutoffMs = createdMs + autoApproveMinutes * 60 * 1000;
  const remainingMs = cutoffMs - Date.now();
  if (remainingMs <= 0) {
    return {
      title: "Queued for autopilot pickup",
      detail: "Scheduler window reached. Next scheduled run should process this request.",
    };
  }
  return {
    title: "Awaiting autopilot window",
    detail: `About ${Math.max(1, Math.ceil(remainingMs / 60000))} minute(s) left before auto-approval window.`,
  };
}

function Meta({ label, value }) {
  return (
    <div>
      <p className="font-semibold uppercase text-slate-400">{label}</p>
      <p className="mt-1 text-slate-600">{value || "-"}</p>
    </div>
  );
}

function statusClass(status) {
  if (status === "Approved") return "bg-green-100 text-green-700";
  if (status === "Rejected") return "bg-red-100 text-red-700";
  return "bg-amber-100 text-amber-700";
}

function formatDate(value) {
  if (!value) return "-";
  const date = value?.toDate ? value.toDate() : value?.seconds ? new Date(value.seconds * 1000) : new Date(value);
  return date.toLocaleString();
}

function toMillis(value) {
  if (!value) return 0;
  if (typeof value.toMillis === "function") return value.toMillis();
  if (typeof value?.seconds === "number") return value.seconds * 1000;
  const parsed = new Date(value).getTime();
  return Number.isFinite(parsed) ? parsed : 0;
}
