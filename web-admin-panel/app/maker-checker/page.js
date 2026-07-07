"use client";
import { useMemo, useState } from "react";
import AdminShell from "@/components/AdminShell";
import ActionDialog, { ActionNotice } from "@/components/ActionDialog";
import Link from "next/link";
import { Shield, CheckCircle, XCircle, Loader2, AlertCircle } from "lucide-react";
import { useApiCollection } from "@/lib/useApiCollection";
import { callAdminApi } from "@/lib/apiClient";

export default function MakerChecker() {
  const [filter, setFilter] = useState("Pending");
  const [actionLoading, setActionLoading] = useState(null);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState(null);
  const [actionDialog, setActionDialog] = useState(null);
  
  const { rows: requests, loading } = useApiCollection(null, "maker-checker");

  const filtered = useMemo(() => {
    return requests.filter(r => filter === "All" || r.status === filter);
  }, [requests, filter]);

  const handleAction = async (request, newStatus) => {
    setError(null);
    setActionDialog({
      requestId: request.id,
      newStatus,
      title: `${newStatus} maker-checker request?`,
      message: "This decision will be written to the audit trail and can affect a live payment status change.",
      confirmLabel: newStatus === "Approved" ? "Approve request" : "Reject request",
      tone: newStatus === "Approved" ? "success" : "danger",
      reasonPlaceholder: `Reason for ${newStatus}:`,
    });
  };

  const confirmAction = async (reason) => {
    if (!actionDialog) return;
    setActionLoading(actionDialog.requestId);
    try {
      await callAdminApi("approveMakerCheckerRequest", {
        requestId: actionDialog.requestId,
        status: actionDialog.newStatus,
        note: reason,
      });
      setNotice({
        tone: "success",
        message: `Request ${actionDialog.newStatus.toLowerCase()} successfully.`,
      });
      setActionDialog(null);
    } catch (e) { 
      setError(e.message); 
    }
    finally { 
      setActionLoading(null); 
    }
  };

  const getTypeIcon = (type) => {
    switch (type) {
      case "PAYMENT_STATUS_UPDATE":
        return <Shield className="text-blue-600" size={18} />;
      default:
        return <AlertCircle className="text-slate-600" size={18} />;
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case "Pending":
        return "bg-yellow-50 border-yellow-200 text-yellow-700";
      case "Approved":
        return "bg-green-50 border-green-200 text-green-700";
      case "Rejected":
        return "bg-red-50 border-red-200 text-red-700";
      default:
        return "bg-slate-50 border-slate-200 text-slate-700";
    }
  };

  return (
    <AdminShell className="p-6">
      <ActionNotice
        tone={notice?.tone}
        message={notice?.message}
        onClose={() => setNotice(null)}
      />
      <header className="mb-6">
        <h1 className="text-2xl font-bold text-slate-800">Maker-Checker Approvals</h1>
        <p className="text-slate-500 text-sm">Review and approve high-value payment actions.</p>
      </header>

      <div className="flex bg-white border border-slate-300 p-1 rounded mb-6 w-fit gap-1">
        {["Pending", "Approved", "Rejected", "All"].map((status) => (
          <button 
            key={status} 
            onClick={() => setFilter(status)}
            className={`px-4 py-1.5 rounded text-xs font-medium transition-colors ${
              filter === status ? "bg-blue-600 text-white" : "text-slate-600 hover:bg-slate-50"
            }`}
          >
            {status}
          </button>
        ))}
      </div>

      {error && (
        <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded text-red-700 text-sm">
          {error}
        </div>
      )}

      <div className="space-y-4">
        {loading ? (
          <div className="py-20 text-center text-slate-400">
            <Loader2 className="animate-spin mx-auto mb-2 text-blue-600" />
          </div>
        ) : filtered.length === 0 ? (
          <div className="py-16 text-center text-slate-400 bg-white border rounded-lg">
            No maker-checker requests found for this filter.
          </div>
        ) : (
          filtered.map((request) => (
            <div key={request.id} className="bg-white p-5 rounded-lg border border-slate-200 shadow-sm">
              <div className="flex items-start gap-4">
                <div className="mt-1">{getTypeIcon(request.type)}</div>
                <div className="flex-1">
                  <div className="flex items-center justify-between mb-3">
                    <h3 className="font-bold text-sm text-slate-800">
                      {request.type.replace(/_/g, " ")}
                    </h3>
                    <span className={`px-2 py-1 rounded text-xs font-bold ${getStatusColor(request.status)}`}>
                      {request.status}
                    </span>
                  </div>
                  
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-3">
                    <div>
                      <p className="text-[10px] font-bold text-slate-400 uppercase">Booking ID</p>
                      <p className="text-sm text-slate-700">{request.bookingId || "-"}</p>
                    </div>
                    <div>
                      <p className="text-[10px] font-bold text-slate-400 uppercase">Amount</p>
                      <p className="text-sm text-slate-700 font-semibold">₹{request.amount || 0}</p>
                    </div>
                    <div>
                      <p className="text-[10px] font-bold text-slate-400 uppercase">Requested By</p>
                      <p className="text-sm text-slate-700">{request.requestedBy || "-"}</p>
                    </div>
                    <div>
                      <p className="text-[10px] font-bold text-slate-400 uppercase">Created At</p>
                      <p className="text-sm text-slate-700">{formatDate(request.createdAt)}</p>
                    </div>
                  </div>

                  {request.status === "Pending" && (
                    <div className="flex gap-2 mt-4">
                      <button 
                        disabled={actionLoading === request.id}
                        onClick={() => handleAction(request, "Approved")}
                        className="bg-green-600 text-white py-2 px-4 rounded text-xs font-semibold disabled:opacity-50 flex items-center gap-2"
                      >
                        {actionLoading === request.id ? <Loader2 className="animate-spin" size={14} /> : <CheckCircle size={14} />}
                        Approve
                      </button>
                      <button 
                        disabled={actionLoading === request.id}
                        onClick={() => handleAction(request, "Rejected")}
                        className="bg-white text-red-600 border border-red-200 py-2 px-4 rounded text-xs font-semibold disabled:opacity-50 flex items-center gap-2"
                      >
                        {actionLoading === request.id ? <Loader2 className="animate-spin" size={14} /> : <XCircle size={14} />}
                        Reject
                      </button>
                    </div>
                  )}

                  {request.status !== "Pending" && (
                    <div className="mt-4 p-3 bg-slate-50 rounded text-xs">
                      <p className="font-bold text-slate-600">Admin Note:</p>
                      <p className="text-slate-500">{request.adminReason || request.note || "Reviewed"}</p>
                    </div>
                  )}
                </div>
              </div>
            </div>
          ))
        )}
      </div>
      {actionDialog && (
        <ActionDialog
          title={actionDialog.title}
          message={actionDialog.message}
          confirmLabel={actionDialog.confirmLabel}
          tone={actionDialog.tone}
          requireReason
          reasonLabel="Admin note"
          reasonPlaceholder={actionDialog.reasonPlaceholder}
          reasonMinLength={3}
          loading={actionLoading === actionDialog.requestId}
          error={error || ""}
          onClose={() => {
            if (actionLoading) return;
            setActionDialog(null);
            setError(null);
          }}
          onConfirm={confirmAction}
        />
      )}
    </AdminShell>
  );
}

function formatDate(value) {
  if (!value) return "-";
  const date = value?.toDate ? value.toDate() : value?.seconds ? new Date(value.seconds * 1000) : new Date(value);
  return date.toLocaleString();
}
