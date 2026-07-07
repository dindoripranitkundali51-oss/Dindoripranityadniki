"use client";
import { useState } from "react";
import AdminShell from "@/components/AdminShell";
import { ActionNotice } from "@/components/ActionDialog";
import { Download, ShieldAlert, FileJson, Loader2 } from "lucide-react";
import { callAdminApi } from "@/lib/apiClient";

export default function BackupManagement() {
  const [loading, setLoading] = useState(false);
  const [notice, setNotice] = useState(null);

  const exportFullBackup = async () => {
    setLoading(true);
    try {
      const result = await callAdminApi("runAdminBackup", { schedule: "Manual" });
      const url = result.data?.downloadUrl;
      if (!url) throw new Error("Backup completed, but no download URL was returned.");
      const link = document.createElement("a");
      link.href = url;
      link.target = "_blank";
      link.rel = "noopener noreferrer";
      link.click();
      setNotice({ tone: "success", message: "Backup file generated successfully. Download opened in a new tab." });
    } catch (e) {
      setNotice({ tone: "danger", message: `Backup failed: ${e.message}` });
    } finally {
      setLoading(false);
    }
  };

  return (
    <AdminShell className="p-6">
      <ActionNotice tone={notice?.tone} message={notice?.message} onClose={() => setNotice(null)} />
      <header className="mb-6">
        <h1 className="text-2xl font-bold text-slate-800">Backup</h1>
        <p className="text-slate-500 text-sm">Data Security & Backups</p>
      </header>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <div className="rounded-lg border border-slate-200 bg-white p-8 shadow-sm">
          <div className="mb-4 flex h-12 w-12 items-center justify-center rounded bg-blue-50 text-blue-600">
            <Download size={24} />
          </div>
          <h3 className="mb-2 text-lg font-bold text-slate-800">Manual Data Export</h3>
          <p className="mb-6 text-sm text-slate-500">
            Download a complete snapshot of your database in JSON format for safe-keeping or manual recovery.
          </p>
          <button
            onClick={exportFullBackup}
            disabled={loading}
            className="flex w-full items-center justify-center gap-2 rounded bg-blue-600 py-3 font-medium text-white transition-colors hover:bg-blue-700 disabled:opacity-70"
          >
            {loading ? <Loader2 className="animate-spin" size={18} /> : <><FileJson size={18} /> Generate Full Backup</>}
          </button>
        </div>

        <div className="rounded-lg border border-amber-200 bg-amber-50 p-8 shadow-sm">
          <div className="mb-4 flex h-12 w-12 items-center justify-center rounded border border-amber-100 bg-white text-amber-600 shadow-sm">
            <ShieldAlert size={24} />
          </div>
          <h3 className="mb-2 text-lg font-bold text-amber-900">Restore & Recovery</h3>
          <p className="mb-4 text-sm text-amber-800 opacity-80">
            Restore functionality is restricted to Super Admins only. Please contact technical support for system
            restoration from a backup file.
          </p>
          <div className="rounded border border-amber-200 bg-white/60 p-4 text-xs text-amber-900">
            <span className="font-bold">Security Note:</span> Every backup export is logged in the Audit Trail for
            security purposes.
          </div>
        </div>
      </div>
    </AdminShell>
  );
}
