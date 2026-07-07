"use client";
import { useMemo, useState } from "react";
import AdminShell from "../../components/AdminShell";
import ActionDialog, { ActionNotice } from "@/components/ActionDialog";
import Image from "next/image";
import { useApiCollection } from "../../lib/useApiCollection";
import { CheckCircle2, X, Search, Eye, Calendar, AlertCircle, Loader2, FileText, ExternalLink, MapPin, Image as ImageIcon } from "lucide-react";
import { useRouter } from "next/navigation";
import { useLanguage } from "../../context/LanguageContext";
import { callAdminApi } from "@/lib/apiClient";

export default function GurujiManagement() {
  const router = useRouter();
  const { t } = useLanguage();
  const [searchTerm, setSearchTerm] = useState("");
  const [filterStatus, setFilterStatus] = useState("PENDING_VERIFICATION");
  const [selectedGuruji, setSelectedGuruji] = useState(null);
  const [kycData, setKycData] = useState(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const [loadingKyc, setLoadingKyc] = useState(false);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState(null);
  const [actionDialog, setActionDialog] = useState(null);

  const { rows: gurujis, loading } = useApiCollection(null, "admin-guruji");
  const { rows: bookings } = useApiCollection(null, "admin-guruji-bookings");
  const { rows: ledger } = useApiCollection(null, "admin-guruji-ledger");

  const loadKycDetails = async (guruji) => {
    setSelectedGuruji(guruji);
    setLoadingKyc(true);
    setKycData(null);
    setError(null);
    try {
      setKycData({});
    } catch {
      setError(t("error_kyc") || "Could not load KYC");
    } finally {
      setLoadingKyc(false);
    }
  };

  const handleStatusUpdate = async (gurujiId, newStatus) => {
    const needsReason = ["Rejected", "Blocked", "Deleted"].includes(newStatus);
    setError(null);
    setActionDialog({
      gurujiId,
      newStatus,
      title: `${newStatus} this Guruji?`,
      message: statusMessage(newStatus),
      confirmLabel: statusConfirmLabel(newStatus),
      tone: newStatus === "Approved" ? "success" : "danger",
      requireReason: needsReason,
      reasonLabel: "Admin note",
      reasonPlaceholder: needsReason
        ? reasonPrompt(newStatus)
        : "Optional approval note for audit trail",
    });
  };

  const confirmStatusUpdate = async (reason) => {
    if (!actionDialog) return;
    setIsProcessing(true);
    try {
      await callAdminApi("manageGurujiStatus", {
        gurujiId: actionDialog.gurujiId,
        status: actionDialog.newStatus,
        reason,
      });
      setSelectedGuruji(null);
      setNotice({
        tone: actionDialog.newStatus === "Approved" ? "success" : "info",
        message: `Guruji status updated to ${actionDialog.newStatus}.`,
      });
      setActionDialog(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setIsProcessing(false);
    }
  };

  const filtered = useMemo(() => {
    return gurujis.filter((g) => {
      const matchesSearch = `${g.fullName || ""} ${g.mobile || ""} ${g.email || ""}`.toLowerCase().includes(searchTerm.toLowerCase());
      const matchesStatus = filterStatus === "ALL" || (filterStatus === "PENDING_VERIFICATION" ? isPendingStatus(g.status) : g.status === filterStatus);
      return matchesSearch && matchesStatus;
    });
  }, [gurujis, searchTerm, filterStatus]);

  const pendingGurujis = useMemo(() => gurujis.filter((g) => isPendingStatus(g.status)), [gurujis]);

  return (
    <AdminShell className="p-6">
      <ActionNotice
        tone={notice?.tone}
        message={notice?.message}
        onClose={() => setNotice(null)}
      />
      <header className="mb-6 flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div><h1 className="text-2xl font-bold text-slate-800">{t("guruji_management")}</h1><p className="text-slate-500 text-sm">{t("manage_guruji_network")}</p></div>
        <button onClick={() => router.push("/availability")} className="flex items-center gap-2 px-3 py-2 bg-white border border-slate-300 rounded text-sm text-slate-600 hover:bg-slate-50 transition-colors"><Calendar size={16} /> {t("global_availability")}</button>
      </header>

      {error && <div className="mb-6 p-3 bg-red-50 border border-red-200 rounded text-red-700 text-xs flex items-center gap-2"><AlertCircle size={16} /> {error}</div>}

      <div className="flex flex-col xl:flex-row gap-4 mb-6">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
          <input type="text" placeholder={t("search")} className="w-full border border-slate-300 p-2 pl-10 rounded text-sm outline-none focus:border-blue-500" value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} />
        </div>
        <div className="flex bg-white border border-slate-300 p-1 rounded gap-1 overflow-x-auto">
          {[{ id: "PENDING_VERIFICATION", label: t("pending") }, { id: "Approved", label: t("approved") }, { id: "Blocked", label: "Blocked" }, { id: "ALL", label: t("all_status") }].map((s) => (
            <button key={s.id} onClick={() => setFilterStatus(s.id)} className={`px-4 py-1.5 rounded text-xs font-medium transition-colors ${filterStatus === s.id ? "bg-blue-600 text-white" : "text-slate-600 hover:bg-slate-50"}`}>{s.label}</button>
          ))}
        </div>
      </div>

      <div className="bg-white border rounded-lg overflow-x-auto shadow-sm">
        <table className="w-full text-left text-sm">
          <thead><tr className="bg-slate-50 border-b"><th className="p-4 font-semibold text-slate-600 uppercase text-xs">{t("guruji_profile")}</th><th className="p-4 font-semibold text-slate-600 uppercase text-xs">Seva Types</th><th className="p-4 font-semibold text-slate-600 uppercase text-xs">Wallet</th><th className="p-4 font-semibold text-slate-600 uppercase text-xs">{t("status")}</th><th className="p-4"></th></tr></thead>
          <tbody className="divide-y">
            {loading ? (
              <tr><td colSpan="5" className="p-10 text-center text-slate-500">{t("loading")}</td></tr>
            ) : filtered.length === 0 ? (
              <tr><td colSpan="5" className="p-10 text-center text-slate-500">{t("no_data")}</td></tr>
            ) : filtered.map((g) => (
              <tr key={g.id} className="hover:bg-slate-50 cursor-pointer" onClick={() => loadKycDetails(g)}>
                <td className="p-4"><div className="flex items-center gap-3"><div className="w-8 h-8 rounded bg-blue-50 text-blue-600 flex items-center justify-center font-bold text-xs">{g.fullName?.[0]}</div><div><div className="font-medium text-slate-800">{g.fullName}</div><div className="text-xs text-slate-500">{g.mobile}</div></div></div></td>
                <td className="p-4"><p className="text-xs text-slate-700">{stringList(g.expertises).slice(0, 2).join(", ") || "-"}</p></td>
                <td className="p-4"><p className="font-bold">₹{Number(g.walletBalance || 0).toLocaleString()}</p></td>
                <td className="p-4"><span className={`px-2 py-0.5 rounded text-[11px] font-medium ${statusClass(g.status)}`}>{g.status}</span></td>
                <td className="p-4 text-right"><Eye size={18} className="text-slate-400 inline" /></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {selectedGuruji && (
        <GurujiDrawer
          guruji={selectedGuruji}
          kycData={kycData}
          loadingKyc={loadingKyc}
          isProcessing={isProcessing}
          onClose={() => setSelectedGuruji(null)}
          onStatus={handleStatusUpdate}
          t={t}
        />
      )}
      {actionDialog && (
        <ActionDialog
          title={actionDialog.title}
          message={actionDialog.message}
          confirmLabel={actionDialog.confirmLabel}
          tone={actionDialog.tone}
          requireReason={actionDialog.requireReason}
          reasonLabel={actionDialog.reasonLabel}
          reasonPlaceholder={actionDialog.reasonPlaceholder}
          reasonMinLength={actionDialog.requireReason ? 3 : 0}
          loading={isProcessing}
          error={error || ""}
          onClose={() => {
            if (isProcessing) return;
            setActionDialog(null);
            setError(null);
          }}
          onConfirm={confirmStatusUpdate}
        />
      )}
    </AdminShell>
  );
}

function GurujiDrawer({ guruji, kycData, loadingKyc, isProcessing, onClose, onStatus, t }) {
  const docs = { ...(guruji.kycDocuments || {}), ...(kycData?.docs || {}) };
  return (
    <div className="fixed inset-0 bg-black/50 z-[100] flex justify-end">
      <div className="bg-white w-full max-w-3xl h-full p-6 shadow-xl overflow-y-auto">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-lg font-bold">{t("guruji_details")}</h2>
          <button onClick={onClose}><X size={20} className="text-slate-400 hover:text-slate-600" /></button>
        </div>
        <div className="space-y-6">
          <Section title="Basic Information">
            <div className="grid grid-cols-2 gap-4">
              <Field label="Full Name" value={guruji.fullName} />
              <Field label="Mobile" value={guruji.mobile} />
              <Field label="Email" value={guruji.email} />
              <Field label="District" value={guruji.district} />
            </div>
          </Section>
          
          <Section title="KYC Documents (Preview)">
            <DocumentGrid docs={docs} />
          </Section>

          <div className="sticky bottom-0 bg-white border-t py-4 grid grid-cols-3 gap-3">
            <button disabled={isProcessing} onClick={() => onStatus(guruji.id, "Approved")} className="bg-green-600 text-white py-2 rounded font-medium text-sm hover:bg-green-700">Approve</button>
            <button disabled={isProcessing} onClick={() => onStatus(guruji.id, "Rejected")} className="bg-red-600 text-white py-2 rounded font-medium text-sm hover:bg-red-700">Reject</button>
            <button disabled={isProcessing} onClick={() => onStatus(guruji.id, "Blocked")} className="bg-slate-800 text-white py-2 rounded font-medium text-sm hover:bg-slate-900">Block</button>
          </div>
        </div>
      </div>
    </div>
  );
}

function Section({ title, children }) {
  return <section className="border-b pb-4"><h3 className="text-sm font-bold text-slate-800 mb-3 uppercase tracking-wider">{title}</h3>{children}</section>;
}

function Field({ label, value }) {
  return <div><p className="text-xs text-slate-500 font-semibold">{label}</p><p className="text-slate-800">{value || "-"}</p></div>;
}

function DocumentGrid({ docs }) {
  const entries = Object.entries(docs).filter(([, url]) => url && typeof url === "string");
  if (!entries.length) return <p className="text-sm text-slate-400 italic">No documents uploaded.</p>;
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
      {entries.map(([key, url]) => (
        <div key={key} className="border rounded-lg overflow-hidden bg-slate-50">
          <div className="p-2 bg-slate-100 flex justify-between items-center border-b">
            <span className="text-xs font-bold uppercase">{key.replace("Url", "")}</span>
            <a href={url} target="_blank" rel="noreferrer" className="text-blue-600"><ExternalLink size={14} /></a>
          </div>
          <div className="aspect-video relative flex items-center justify-center">
            {url.match(/\.(jpeg|jpg|gif|png|webp)$/i) ? (
              <Image src={url} alt={key} fill unoptimized className="object-contain" />
            ) : (
              <div className="flex flex-col items-center text-slate-400">
                <FileText size={32} />
                <span className="text-[10px] mt-1">Non-image file</span>
              </div>
            )}
          </div>
        </div>
      ))}
    </div>
  );
}

function stringList(value) { return Array.isArray(value) ? value : []; }
function isPendingStatus(status) { return ["Pending", "PENDING_VERIFICATION", "Under Review"].includes(status); }
function statusClass(status) {
  if (["Approved", "Active"].includes(status)) return "bg-green-100 text-green-700";
  if (status === "Blocked") return "bg-slate-100 text-slate-700";
  return "bg-blue-100 text-blue-700";
}
function reasonPrompt(status) { return `Reason for ${status}:`; }
function statusConfirmLabel(status) {
  if (status === "Approved") return "Approve Guruji";
  if (status === "Rejected") return "Reject Guruji";
  if (status === "Blocked") return "Block Guruji";
  return "Confirm";
}
function statusMessage(status) {
  if (status === "Approved") {
    return "This Guruji will become available for live assignment in the app.";
  }
  if (status === "Rejected") {
    return "This profile will stay out of the live network until a corrected submission is reviewed again.";
  }
  if (status === "Blocked") {
    return "This Guruji will immediately stop receiving new assignment access.";
  }
  return "Please confirm this status change.";
}
