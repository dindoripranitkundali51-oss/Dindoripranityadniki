"use client";
import { useState } from "react";
import AdminShell from "@/components/AdminShell";
import { ActionNotice } from "@/components/ActionDialog";
import { useApiCollection } from "@/lib/useApiCollection";
import { Download, Loader2, Search } from "lucide-react";
import { exportCsv } from "@/lib/csv";

export default function ReceiptManagement() {
  const [searchTerm, setSearchTerm] = useState("");
  const [notice, setNotice] = useState(null);
  const { rows: receipts, loading } = useApiCollection(null, "receipt-module");

  const filtered = receipts.filter((receipt) =>
    (receipt.id?.toLowerCase() || "").includes(searchTerm.toLowerCase()) ||
    (receipt.bookingId?.toLowerCase() || "").includes(searchTerm.toLowerCase()) ||
    (receipt.receiptNo?.toLowerCase() || "").includes(searchTerm.toLowerCase()) ||
    (receipt.poojaName?.toLowerCase() || "").includes(searchTerm.toLowerCase()) ||
    (receipt.contactName?.toLowerCase() || "").includes(searchTerm.toLowerCase())
  );

  return (
    <AdminShell className="p-6">
      <ActionNotice tone={notice?.tone} message={notice?.message} onClose={() => setNotice(null)} />
      <header className="mb-6 flex flex-col items-start justify-between gap-4 md:flex-row md:items-center">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">Receipts</h1>
          <p className="text-sm text-slate-500">Receipt snapshots and user receipt notifications are generated automatically after successful payment.</p>
        </div>
        <button
          onClick={() => exportCsv("receipts.csv", filtered)}
          className="flex items-center gap-2 rounded border border-slate-300 bg-white px-3 py-2 text-sm text-slate-600 transition-colors hover:bg-slate-50"
        >
          <Download size={16} /> Export CSV
        </button>
      </header>

      <div className="mb-6 rounded-lg border border-blue-100 bg-blue-50 px-4 py-3 text-sm text-blue-900">
        Manual regenerate/resend controls have been removed. Receipt freeze and receipt notification are backend-driven automation steps.
      </div>

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white shadow-sm">
        <div className="border-b bg-slate-50 p-4">
          <div className="relative max-w-xl">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
            <input
              type="text"
              placeholder="Search by receipt number, booking ID, yajman, or pooja..."
              className="w-full rounded border border-slate-300 bg-white p-2 pl-10 text-sm outline-none focus:border-blue-500"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
        </div>

        <table className="w-full text-left text-sm">
          <thead>
            <tr className="border-b bg-slate-50 text-xs font-semibold uppercase text-slate-600">
              <th className="p-4">Receipt</th>
              <th className="p-4">Yajman & Pooja</th>
              <th className="p-4">Amount</th>
              <th className="p-4">Automation State</th>
            </tr>
          </thead>
          <tbody className="divide-y">
            {loading ? (
              <tr>
                <td colSpan="4" className="p-10 text-center text-slate-400">
                  <Loader2 className="mx-auto mb-2 animate-spin text-blue-600" /> Loading receipts...
                </td>
              </tr>
            ) : filtered.length === 0 ? (
              <tr>
                <td colSpan="4" className="p-10 text-center text-slate-500">
                  No receipts found.
                </td>
              </tr>
            ) : (
              filtered.map((receipt) => (
                <tr key={receipt.id} className="hover:bg-slate-50">
                  <td className="p-4">
                    <p className="font-medium text-slate-800">{receipt.receiptNo || `#REC-${receipt.id.substring(0, 6)}`}</p>
                    <p className="text-xs text-slate-400">{receipt.date || formatDate(receipt.frozenAt || receipt.generatedAt)}</p>
                    <p className="mt-1 text-[11px] text-green-600">{receipt.status || "Valid"}</p>
                  </td>
                  <td className="p-4">
                    <p className="font-medium text-slate-700">{receipt.contactName || "-"}</p>
                    <p className="text-xs text-blue-600">{receipt.poojaName || "-"}</p>
                  </td>
                  <td className="p-4 font-bold text-slate-800">Rs. {Number(receipt.totalAmount || receipt.amount || 0).toLocaleString()}</td>
                  <td className="p-4">
                    <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
                      <p className="text-xs font-semibold uppercase text-slate-500">Auto Snapshot</p>
                      <p className="mt-1 text-sm font-semibold text-slate-800">
                        {receipt.generatedAt || receipt.frozenAt ? "Generated automatically" : "Awaiting payment freeze"}
                      </p>
                      <p className="mt-1 text-xs text-slate-500">
                        Receipt alert is sent by backend after payment success. This page is read-only supervision.
                      </p>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </AdminShell>
  );
}

function formatDate(value) {
  if (!value) return "-";
  const date = value?.toDate ? value.toDate() : value?.seconds ? new Date(value.seconds * 1000) : new Date(value);
  return date.toLocaleString();
}
