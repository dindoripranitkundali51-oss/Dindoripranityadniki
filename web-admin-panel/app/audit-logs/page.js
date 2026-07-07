"use client";
import { useState } from "react";
import AdminShell from "@/components/AdminShell";
import { useApiCollection } from "@/lib/useApiCollection";
import { Loader2, Search, User } from "lucide-react";

export default function AuditLogs() {
  const [searchTerm, setSearchTerm] = useState("");
  const { rows: logs, loading } = useApiCollection(null, "audit-logs-v2");

  const filteredLogs = logs.filter((log) =>
    `${log.adminEmail || ""} ${log.action || ""} ${log.module || ""} ${log.targetId || ""}`
      .toLowerCase()
      .includes(searchTerm.toLowerCase())
  );

  return (
    <AdminShell className="p-6">
      <header className="mb-6">
        <h1 className="text-2xl font-bold text-slate-800">Audit Logs</h1>
        <p className="text-sm text-slate-500">Security & Accountability Trail</p>
      </header>

      <div className="overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm">
        <div className="flex flex-col items-center justify-between gap-4 border-b bg-slate-50 p-4 md:flex-row">
          <h2 className="text-sm font-bold text-slate-700">Security Audit Trail</h2>
          <div className="relative w-full md:w-64">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
            <input
              type="text"
              placeholder="Search logs..."
              className="w-full rounded border border-slate-300 bg-white p-2 pl-9 text-sm outline-none focus:border-blue-500"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="border-b bg-slate-50 text-xs font-semibold text-slate-600">
              <tr>
                <th className="p-4">Administrator</th>
                <th className="p-4">Action</th>
                <th className="p-4">Client IP</th>
                <th className="p-4">Target / Details</th>
                <th className="p-4">Timestamp</th>
                <th className="p-4 text-center">Threat Level</th>
                <th className="p-4 text-right">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {loading ? (
                <tr>
                  <td colSpan="7" className="p-10 text-center text-slate-400">
                    <Loader2 className="mx-auto animate-spin text-blue-600" />
                  </td>
                </tr>
              ) : (
                filteredLogs.map((log) => (
                  <tr key={log.id} className={`hover:bg-slate-50 ${
                    log.threatLevel === "Critical" || log.threatLevel === "High" ? "bg-red-50/40" : ""
                  }`}>
                    <td className="p-4">
                      <div className="flex items-center gap-3">
                        <div className="flex h-8 w-8 items-center justify-center rounded bg-blue-50 text-blue-500">
                          <User size={16} />
                        </div>
                        <div>
                          <p className="font-medium text-slate-800">{log.adminName || "Admin"}</p>
                          <p className="text-[11px] text-slate-500">{log.adminEmail}</p>
                        </div>
                      </div>
                    </td>
                    <td className="p-4">
                      <p className="font-medium text-slate-700">{log.action}</p>
                      <p className="text-[10px] font-bold uppercase text-blue-600">{log.module || "System"}</p>
                    </td>
                    <td className="p-4 text-xs font-mono text-slate-600">
                      {log.clientIp || "127.0.0.1"}
                    </td>
                    <td className="p-4">
                      <p className="break-all font-mono text-xs text-slate-600">{log.targetId || log.target || "-"}</p>
                      {log.details?.message && <p className="mt-1 text-xs text-red-600">{log.details.message}</p>}
                    </td>
                    <td className="p-4 text-xs text-slate-500">{formatDate(log.timestamp || log.createdAt)}</td>
                    <td className="p-4 text-center">
                      <span className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase ${
                        log.threatLevel === "Critical" ? "bg-red-600 text-white" :
                        log.threatLevel === "High" ? "bg-red-100 text-red-700" :
                        log.threatLevel === "Medium" ? "bg-amber-100 text-amber-700" : "bg-slate-100 text-slate-600"
                      }`}>
                        {log.threatLevel || "Low"}
                      </span>
                    </td>
                    <td className="p-4 text-right">
                      <span
                        className={`rounded px-2 py-0.5 text-[10px] font-bold ${
                          log.status === "Failed" || log.details?.status === "Failed"
                            ? "bg-red-100 text-red-700"
                            : "bg-green-100 text-green-700"
                        }`}
                      >
                        {log.status || log.details?.status || "Success"}
                      </span>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </AdminShell>
  );
}

function formatDate(value) {
  if (!value) return "Recent";
  if (value?.toDate) return value.toDate().toLocaleString();
  if (value?.seconds) return new Date(value.seconds * 1000).toLocaleString();
  return String(value);
}
