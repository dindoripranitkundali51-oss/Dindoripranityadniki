"use client";
import { useMemo, useState } from "react";
import AdminShell from "@/components/AdminShell";
import ActionDialog, { ActionNotice } from "@/components/ActionDialog";
import { exportCsv } from "@/lib/csv";
import {
  bookingStatusLabel,
  buildPaymentStatusDialog,
  canUpdatePayment,
  paymentOptions,
  paymentStatusLabel,
} from "@/lib/paymentAdmin";
import { useApiCollection } from "@/lib/useApiCollection";
import { callAdminApi } from "@/lib/apiClient";
import Link from "next/link";
import { Search, Download, ArrowUpRight, Clock, Loader2, X } from "lucide-react";
import { useLanguage } from "@/context/LanguageContext";

export default function FinanceHub() {
  const { t, lang } = useLanguage();
  const isMarathi = lang === "mr";
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("All");
  const [actionLoading, setActionLoading] = useState(null);
  const [selectedPayment, setSelectedPayment] = useState(null);
  const [actionDialog, setActionDialog] = useState(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState(null);

  const copy = {
    subtitle: isMarathi
      ? "Razorpay verification, revenue split आणि payment exceptions सांभाळा"
      : "Manage Razorpay verification, revenue split, and payment exceptions",
    refRazorpay: isMarathi ? "Ref / Razorpay" : "Ref / Razorpay",
    amountSplit: isMarathi ? "रकमेचे वाटप" : "Amount Split",
    bookingState: isMarathi ? "बुकिंग" : "Booking",
    verifiedAt: isMarathi ? "पडताळले" : "Verified",
    trustGurujiSplit: isMarathi ? "ट्रस्ट {trust} / गुरुजी {guruji}" : "Trust {trust} / Guruji {guruji}",
    sevaPendingHint: isMarathi ? "सेवा पूर्ण किंवा पेमेंट प्रलंबित असणे आवश्यक" : "Seva complete/payment pending required",
    adminNote: isMarathi ? "अॅडमिन नोंद" : "Admin note",
    auditPlaceholder: isMarathi ? "पेमेंट audit trail साठी ऐच्छिक नोंद" : "Optional note for payment audit trail",
    paymentDetail: isMarathi ? "पेमेंट तपशील" : "Payment Detail",
    booking: isMarathi ? "बुकिंग" : "Booking",
    bookingId: isMarathi ? "बुकिंग आयडी" : "Booking ID",
    pooja: isMarathi ? "पूजा" : "Pooja",
    yajman: isMarathi ? "यजमान" : "Yajman",
    paymentId: isMarathi ? "पेमेंट आयडी" : "Payment ID",
    transactionId: isMarathi ? "ट्रान्झॅक्शन आयडी / UTR" : "Transaction ID / UTR",
    verifiedTime: isMarathi ? "पडताळणी वेळ" : "Verified At",
    failureReason: isMarathi ? "अयशस्वी कारण" : "Failure Reason",
    paymentMethod: isMarathi ? "पेमेंट पद्धत" : "Payment Method",
    settlementSplit: isMarathi ? "सेटलमेंट वाटप" : "Settlement Split",
    total: isMarathi ? "एकूण" : "Total",
    trust: isMarathi ? "ट्रस्ट" : "Trust",
    guruji: isMarathi ? "गुरुजी" : "Guruji",
    updateTitle: (status) =>
      isMarathi
        ? `पेमेंट स्थिती ${paymentStatusLabel(status, true)} वर बदलायची?`
        : `Update payment status to ${status}?`,
    updateMessage: isMarathi
      ? "हा निर्णय live payment state बदलतो आणि ledger किंवा maker-checker flow चालू करू शकतो."
      : "This decision changes the live payment state and may trigger ledger or maker-checker flow.",
    setStatus: (status) => (isMarathi ? `${paymentStatusLabel(status, true)} करा` : `Set ${status}`),
    makerChecker: (requestId) =>
      isMarathi
        ? `Maker-checker approval आवश्यक. Request ID: ${requestId}`
        : `Maker-checker approval required. Request ID: ${requestId}`,
    statusUpdated: (status) =>
      isMarathi
        ? `पेमेंट स्थिती ${paymentStatusLabel(status, true)} झाली.`
        : `Payment status updated to ${status}.`,
  };

  const { rows: transactions, loading } = useApiCollection(null, "payments-module");

  const filteredTransactions = useMemo(() => {
    return transactions.filter((tx) => {
      const text = `${tx.contactName || ""} ${tx.id || ""} ${tx.poojaName || ""} ${tx.gatewayPaymentId || ""} ${tx.transactionId || ""}`.toLowerCase();
      const matchesSearch = text.includes(searchTerm.toLowerCase());
      const matchesStatus = statusFilter === "All" || (tx.paymentStatus || "Pending") === statusFilter;
      return matchesSearch && matchesStatus;
    });
  }, [transactions, searchTerm, statusFilter]);

  const { totalCollected, totalPending } = useMemo(() => {
    let collected = 0;
    let pending = 0;
    transactions.forEach((tx) => {
      const amt = Number(tx.actualAmount || 0);
      if (tx.paymentStatus === "Paid") collected += amt;
      else if (canUpdatePayment(tx)) pending += amt;
    });
    return { totalCollected: collected, totalPending: pending };
  }, [transactions]);

  const handleStatusUpdate = async (id, newStatus) => {
    if (!id || !newStatus) return;
    setError("");
    setActionDialog({
      bookingId: id,
      ...buildPaymentStatusDialog(copy, newStatus, isMarathi),
    });
  };

  const confirmStatusUpdate = async (reason) => {
    if (!actionDialog) return;
    setActionLoading(actionDialog.bookingId);
    let succeeded = false;
    try {
      const result = await callAdminApi("updatePaymentStatus", {
        bookingId: actionDialog.bookingId,
        paymentStatus: actionDialog.newStatus,
        note: reason,
      });
      if (result?.data?.approvalRequired) {
        setNotice({
          tone: "info",
          message: actionDialog.approvalRequiredMessage(result.data.requestId),
        });
      } else {
        setNotice({
          tone: "success",
          message: actionDialog.successMessage,
        });
      }
      succeeded = true;
    } catch (actionError) {
      setError(actionError.message || "Update failed");
    } finally {
      setActionLoading(null);
      if (succeeded) {
        setActionDialog(null);
      }
    }
  };

  return (
    <AdminShell className="p-6" requiredCapability="payment:read">
      <ActionNotice tone={notice?.tone} message={notice?.message} onClose={() => setNotice(null)} />
      {error && !actionDialog && (
        <div className="mb-4 rounded border border-red-200 bg-red-50 p-3 text-sm text-red-700">
          {error}
        </div>
      )}

      <header className="mb-6 flex flex-col items-start justify-between gap-4 md:flex-row md:items-center">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">{t("finance_hub")}</h1>
          <p className="text-sm text-slate-500">{copy.subtitle}</p>
        </div>
        <button
          onClick={() => exportCsv("finance.csv", filteredTransactions)}
          className="flex items-center gap-2 rounded border border-slate-300 bg-white px-3 py-2 text-sm text-slate-600 hover:bg-slate-50"
        >
          <Download size={16} /> {t("export_csv")}
        </button>
      </header>

      <div className="mb-8 grid grid-cols-1 gap-6 md:grid-cols-2">
        <Stat icon={<ArrowUpRight size={20} />} label={t("total_collected")} value={money(totalCollected)} color="text-green-600" bg="bg-green-50" />
        <Stat icon={<Clock size={20} />} label={t("pending_dues")} value={money(totalPending)} color="text-amber-600" bg="bg-amber-50" />
      </div>

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white shadow-sm">
        <div className="flex flex-col gap-4 border-b bg-slate-50 p-4 md:flex-row">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
            <input
              type="text"
              placeholder={t("search")}
              className="w-full rounded border border-slate-300 p-2 pl-10 text-sm outline-none focus:border-blue-500"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
          <select
            className="rounded border border-slate-300 bg-white p-2 text-sm"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <option value="All">{t("all_status")}</option>
            {["Pending", "Submitted", "Awaiting Verification", "Paid", "Rejected"].map((status) => (
              <option key={status} value={status}>
                {paymentStatusLabel(status, isMarathi)}
              </option>
            ))}
          </select>
        </div>
        <table className="w-full text-left text-sm">
          <thead className="border-b bg-slate-50 text-xs font-semibold text-slate-600">
            <tr>
              <th className="p-4">{copy.refRazorpay}</th>
              <th className="p-4">{t("yajman")}</th>
              <th className="p-4">{copy.amountSplit}</th>
              <th className="p-4">{t("status")}</th>
              <th className="p-4">{copy.verifiedAt}</th>
              <th className="p-4"></th>
            </tr>
          </thead>
          <tbody className="divide-y">
            {loading ? (
              <tr>
                <td colSpan="6" className="p-10 text-center text-slate-400">
                  <Loader2 className="mx-auto animate-spin" />
                </td>
              </tr>
            ) : (
              filteredTransactions.map((tx) => {
                const updateAllowed = canUpdatePayment(tx);
                const options = paymentOptions(tx);
                return (
                  <tr
                    key={tx.id}
                    className="cursor-pointer hover:bg-slate-50"
                    onClick={() => setSelectedPayment(tx)}
                  >
                    <td className="p-4">
                      <p className="text-xs font-mono text-slate-500">#{tx.id?.substring(0, 8)}</p>
                      <p className="break-all text-[11px] font-mono text-blue-600">
                        {tx.gatewayPaymentId || tx.razorpayPaymentId || tx.paymentId || tx.transactionId || "-"}
                      </p>
                      <p className="mt-1 text-[11px] text-slate-400">
                        {copy.bookingState}: {bookingStatusLabel(tx.status || "Pending", isMarathi)}
                      </p>
                    </td>
                    <td className="p-4">
                      <div className="font-medium">{tx.contactName}</div>
                      <div className="text-xs text-blue-600">{tx.poojaName}</div>
                    </td>
                    <td className="p-4">
                      <p className="font-bold">{money(tx.actualAmount)}</p>
                      <p className="text-xs text-slate-500">
                        {interpolate(copy.trustGurujiSplit, {
                          trust: money(tx.trustShare),
                          guruji: money(tx.gurujiShare),
                        })}
                      </p>
                    </td>
                    <td className="p-4">
                      <span
                        className={`rounded px-2 py-0.5 text-[11px] font-medium ${
                          tx.paymentStatus === "Paid"
                            ? "bg-green-100 text-green-700"
                            : tx.paymentStatus === "Rejected"
                              ? "bg-red-100 text-red-700"
                              : "bg-amber-100 text-amber-700"
                        }`}
                      >
                        {paymentStatusLabel(tx.paymentStatus || "Pending", isMarathi)}
                      </span>
                    </td>
                    <td className="p-4 text-xs text-slate-500">
                      {formatDate(tx.paymentVerifiedAt || tx.verifiedAt || tx.paymentUpdatedAt || tx.paidAt)}
                    </td>
                    <td className="p-4 text-right" onClick={(e) => e.stopPropagation()}>
                      <select
                        disabled={Boolean(actionLoading) || !updateAllowed}
                        className="rounded border p-1 text-xs disabled:bg-slate-100 disabled:text-slate-400"
                        value={tx.paymentStatus || "Pending"}
                        onChange={(e) => handleStatusUpdate(tx.id, e.target.value)}
                      >
                        {options.map((status) => (
                          <option key={status} value={status}>
                            {paymentStatusLabel(status, isMarathi)}
                          </option>
                        ))}
                      </select>
                      {!updateAllowed && (
                        <p className="mt-1 text-[10px] text-slate-400">{copy.sevaPendingHint}</p>
                      )}
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {selectedPayment && (
        <PaymentDrawer
          payment={selectedPayment}
          onClose={() => setSelectedPayment(null)}
          copy={copy}
          isMarathi={isMarathi}
        />
      )}

      {actionDialog && (
        <ActionDialog
          title={actionDialog.title}
          message={actionDialog.message}
          confirmLabel={actionDialog.confirmLabel}
          tone={actionDialog.tone}
          reasonLabel={copy.adminNote}
          reasonPlaceholder={copy.auditPlaceholder}
          loading={actionLoading === actionDialog.bookingId}
          error={error}
          onClose={() => {
            if (actionLoading) return;
            setActionDialog(null);
            setError("");
          }}
          onConfirm={confirmStatusUpdate}
        />
      )}
    </AdminShell>
  );
}

function PaymentDrawer({ payment, onClose, copy, isMarathi }) {
  return (
    <div className="fixed inset-0 z-[100] flex justify-end bg-black/50">
      <div className="h-full w-full max-w-lg overflow-y-auto bg-white p-6 shadow-xl">
        <div className="mb-6 flex items-center justify-between">
          <h2 className="text-lg font-bold">{copy.paymentDetail}</h2>
          <button onClick={onClose}>
            <X size={20} className="text-slate-400 hover:text-slate-600" />
          </button>
        </div>
        <div className="space-y-5">
          <Section title={copy.booking}>
            <Field label={copy.bookingId} value={payment.id} mono />
            <Field label={copy.pooja} value={payment.poojaName} />
            <Field label={copy.yajman} value={`${payment.contactName || "-"} / ${payment.contactPhone || "-"}`} />
          </Section>
          <Section title="Razorpay">
            <Field label={copy.paymentId} value={payment.gatewayPaymentId || payment.razorpayPaymentId || payment.paymentId} mono />
            <Field label={copy.transactionId} value={payment.transactionId} mono />
            <Field label={copy.verifiedTime} value={formatDate(payment.paymentVerifiedAt || payment.verifiedAt || payment.paymentUpdatedAt || payment.paidAt)} />
            <Field label={copy.failureReason} value={payment.paymentFailureReason || payment.paymentRejectReason || payment.failureReason} />
            <Field label={copy.paymentMethod} value={payment.paymentMethod} />
          </Section>
          <Section title={copy.settlementSplit}>
            <div className="grid grid-cols-3 gap-3">
              <MoneyTile label={copy.total} value={payment.actualAmount} />
              <MoneyTile label={`${copy.trust} ${payment.trustSharePercent || 70}%`} value={payment.trustShare} />
              <MoneyTile label={`${copy.guruji} ${payment.gurujiSharePercent || 30}%`} value={payment.gurujiShare} />
            </div>
          </Section>
          <Section title={isMarathi ? "पेमेंट स्थिती" : "Payment Status"}>
            <Field label={isMarathi ? "सध्याची स्थिती" : "Current Status"} value={paymentStatusLabel(payment.paymentStatus || "Pending", isMarathi)} />
            <Field label={isMarathi ? "बुकिंग स्थिती" : "Booking Status"} value={bookingStatusLabel(payment.status || "Pending", isMarathi)} />
          </Section>
        </div>
      </div>
    </div>
  );
}

function Stat({ icon, label, value, color, bg }) {
  return (
    <div className="flex items-center gap-4 rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
      <div className={`${bg} ${color} rounded p-3`}>{icon}</div>
      <div>
        <p className="text-xs font-medium text-slate-500">{label}</p>
        <h3 className="text-xl font-bold text-slate-800">{value}</h3>
      </div>
    </div>
  );
}

function Section({ title, children }) {
  return (
    <section className="rounded-lg border p-4">
      <h3 className="mb-3 text-sm font-bold text-slate-800">{title}</h3>
      <div className="space-y-3">{children}</div>
    </section>
  );
}

function Field({ label, value, mono = false }) {
  return (
    <div>
      <p className="text-xs font-semibold uppercase text-slate-500">{label}</p>
      <p className={`text-slate-800 ${mono ? "break-all font-mono text-xs" : ""}`}>{value || "-"}</p>
    </div>
  );
}

function MoneyTile({ label, value }) {
  return (
    <div className="rounded border bg-slate-50 p-3">
      <p className="text-xs text-slate-500">{label}</p>
      <p className="font-bold">{money(value)}</p>
    </div>
  );
}

function money(value) {
  const amount = Number(value);
  return Number.isFinite(amount) && amount > 0 ? `Rs. ${amount.toLocaleString()}` : "-";
}

function formatDate(value) {
  if (!value) return "-";
  if (value?.toDate) return value.toDate().toLocaleString();
  if (value?.seconds) return new Date(value.seconds * 1000).toLocaleString();
  return String(value);
}

function interpolate(template, values) {
  return Object.entries(values).reduce(
    (text, [key, value]) => text.replace(`{${key}}`, value),
    template
  );
}

