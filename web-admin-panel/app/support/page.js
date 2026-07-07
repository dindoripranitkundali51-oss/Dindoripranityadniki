"use client";
import { useState } from "react";
import AdminShell from "@/components/AdminShell";
import { ActionNotice } from "@/components/ActionDialog";
import { useApiCollection } from "@/lib/useApiCollection";
import { logAdminAction } from "@/lib/audit";
import { callAdminApi } from "@/lib/apiClient";
import { Clock, User, Loader2 } from "lucide-react";
import { useLanguage } from "@/context/LanguageContext";

export default function SupportManagement() {
  const { t } = useLanguage();
  const [filter, setFilter] = useState("Open");
  const [notice, setNotice] = useState(null);
  const { rows: tickets, loading } = useApiCollection(null, "support-module");

  const filtered = tickets.filter(t => filter === "All" || t.status === filter);

  const updateTicketStatus = async (id, status) => {
    try {
        await callAdminApi("updateSupportTicketStatus", { ticketId: id, status });
        setNotice({ tone: "success", message: `Ticket moved to ${status}.` });
        setTimeout(() => window.location.reload(), 1000);
    } catch (err) {
        setNotice({ tone: "danger", message: err.message || "Failed to update status." });
    }
  };

  return (
    <AdminShell className="p-6" requiredCapability="support:read">
        <ActionNotice
            tone={notice?.tone}
            message={notice?.message}
            onClose={() => setNotice(null)}
        />
        <header className="mb-6">
            <h1 className="text-2xl font-bold text-slate-800">{t("support_tickets") || "Support Tickets"}</h1>
            <p className="text-slate-500 text-sm">Review and resolve user support issues.</p>
        </header>

        <div className="flex bg-white border border-slate-300 p-1 rounded mb-6 w-fit gap-1 overflow-x-auto">
            {["Open", "In Progress", "Resolved", "All"].map((s) => (
                <button 
                    key={s} 
                    onClick={() => setFilter(s)}
                    className={`px-4 py-1.5 rounded text-xs font-medium transition-colors ${
                        filter === s ? "bg-blue-600 text-white" : "text-slate-600 hover:bg-slate-50"
                    }`}
                >
                    {s}
                </button>
            ))}
        </div>

        <div className="grid grid-cols-1 gap-3">
            {loading ? (
                <div className="py-20 text-center text-slate-400">
                    <Loader2 className="animate-spin mx-auto mb-2 text-blue-600" />
                    <p className="text-sm italic">Loading support tickets...</p>
                </div>
            ) : filtered.length === 0 ? (
                <div className="py-20 text-center border border-dashed rounded bg-slate-50 text-slate-500">
                    <p className="text-sm">No support tickets found.</p>
                </div>
            ) : (
                filtered.map((t) => (
                    <div key={t.id} className="bg-white p-5 rounded-lg border border-slate-200 shadow-sm flex flex-col md:flex-row justify-between gap-4 hover:border-blue-500 transition-colors">
                        <div className="flex-1">
                            <div className="flex items-center gap-3 mb-2">
                                <span className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase ${
                                    t.status === 'Open' ? 'bg-red-100 text-red-700' : 
                                    t.status === 'In Progress' ? 'bg-amber-100 text-amber-700' : 'bg-green-100 text-green-700'
                                }`}>{t.status}</span>
                                <span className="text-xs text-slate-400 font-mono">#{t.id.slice(-6).toUpperCase()}</span>
                            </div>
                            <h3 className="text-base font-bold text-slate-800">{t.subject || "No Subject"}</h3>
                            <p className="text-sm text-slate-600 mt-1 line-clamp-1">{t.description || "No message provided"}</p>
                            
                            <div className="flex items-center gap-4 mt-4">
                                <div className="flex items-center gap-1.5 text-xs text-slate-500"><User size={14} className="text-blue-500"/>{t.userName}</div>
                                <div className="flex items-center gap-1.5 text-xs text-slate-500"><Clock size={14} className="text-blue-500"/>{formatDate(t.createdAt || t.updatedAt)}</div>
                            </div>
                        </div>

                        <div className="flex gap-2 items-center">
                            {t.status !== "Resolved" && (
                                <button 
                                    onClick={() => updateTicketStatus(t.id, "Resolved")} 
                                    className="px-3 py-1.5 bg-green-600 text-white text-xs font-semibold rounded hover:bg-green-700 transition-colors"
                                >
                                    Resolve
                                </button>
                            )}
                            {t.status === "Open" && (
                                <button 
                                    onClick={() => updateTicketStatus(t.id, "In Progress")} 
                                    className="px-3 py-1.5 bg-white text-blue-600 border border-blue-200 text-xs font-semibold rounded hover:bg-blue-50 transition-colors"
                                >
                                    Attend
                                </button>
                            )}
                        </div>
                    </div>
                ))
            )}
        </div>
    </AdminShell>
  );
}

function formatDate(value) {
  if (!value) return "Recent";
  if (typeof value?.toDate === "function") return value.toDate().toLocaleString();
  if (typeof value?.seconds === "number") return new Date(value.seconds * 1000).toLocaleString();
  return String(value);
}
