"use client";

import { useState } from "react";
import AdminShell from "@/components/AdminShell";
import { useApiCollection } from "@/lib/useApiCollection";
import { callAdminApi } from "@/lib/apiClient";
import { AlertTriangle, RefreshCw } from "lucide-react";

export default function DeadLetterWorkbenchPage() {
  const [loadingId, setLoadingId] = useState("");
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const { rows } = useApiCollection(null, "dead-letter-workbench");

  const replay = async (row) => {
    setLoadingId(row.id);
    setError("");
    setNotice("");
    try {
      await callAdminApi("replayDeadLetter", {
        deadLetterId: row.id,
        note: "Replay requested from dead-letter workbench",
      });
      setNotice(`Replay/escalation triggered for ${row.type || row.id}`);
    } catch (e) {
      setError(e.message || "Replay failed");
    }
    setLoadingId("");
  };

  return (
    <AdminShell className="p-6" requiredCapability="audit:write">
      <header className="mb-6">
        <h1 className="text-2xl font-bold text-slate-800 flex items-center gap-2"><AlertTriangle size={22} /> Dead-Letter Workbench</h1>
        <p className="text-sm text-slate-500">Inspect failed operations, replay supported items, and escalate unresolved conflicts.</p>
      </header>

      {notice ? <div className="mb-4 rounded border border-green-200 bg-green-50 p-3 text-sm text-green-700">{notice}</div> : null}
      {error ? <div className="mb-4 rounded border border-red-200 bg-red-50 p-3 text-sm text-red-700">{error}</div> : null}

      <section className="rounded-lg border border-slate-200 bg-white shadow-sm">
        <div className="grid grid-cols-12 gap-3 border-b px-4 py-3 text-xs font-semibold uppercase tracking-wide text-slate-500">
          <div className="col-span-2">Type</div>
          <div className="col-span-2">Entity</div>
          <div className="col-span-2">Severity</div>
          <div className="col-span-2">Status</div>
          <div className="col-span-3">Reason</div>
          <div className="col-span-1">Action</div>
        </div>
        <div className="divide-y">
          {rows.length === 0 ? (
            <div className="px-4 py-8 text-sm text-slate-400">No dead-letter records found.</div>
          ) : rows.map((row) => (
            <div key={row.id} className="grid grid-cols-12 gap-3 px-4 py-4 text-sm text-slate-700">
              <div className="col-span-2 font-semibold">{row.type || "-"}</div>
              <div className="col-span-2">{row.entityId || "-"}</div>
              <div className="col-span-2">{row.severity || "-"}</div>
              <div className="col-span-2">{row.status || "Open"}</div>
              <div className="col-span-3">{row.reason || row.error?.message || "-"}</div>
              <div className="col-span-1">
                <button
                  disabled={loadingId === row.id || row.status === "Replayed"}
                  onClick={() => replay(row)}
                  className="inline-flex items-center gap-1 rounded bg-slate-800 px-2 py-1 text-xs font-semibold text-white disabled:opacity-50"
                >
                  <RefreshCw size={12} />
                  {loadingId === row.id ? "..." : "Replay"}
                </button>
              </div>
            </div>
          ))}
        </div>
      </section>
    </AdminShell>
  );
}
