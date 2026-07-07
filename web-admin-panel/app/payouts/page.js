"use client";
import { useMemo, useState } from "react";
import AdminShell from "@/components/AdminShell";
import { exportCsv } from "@/lib/csv";
import { useApiCollection } from "@/lib/useApiCollection";
import { callAdminApi } from "@/lib/apiClient";
import Link from "next/link";
import { Search, Download, CheckCircle2, Clock, Wallet, Loader2, X } from "lucide-react";
import { useLanguage } from "@/context/LanguageContext";

export default function GurujiEarnings() {
  const { t, lang } = useLanguage();
  const isMarathi = lang === "mr";
  const [searchTerm, setSearchTerm] = useState("");
  const [payoutStatus, setPayoutStatus] = useState("All");
  const [actionLoading, setActionLoading] = useState(null);
  const [selectedPayout, setSelectedPayout] = useState(null);
  const [settlementDialog, setSettlementDialog] = useState(null);
  const [settlementRef, setSettlementRef] = useState("");
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");

  const copy = {
    gurujiEarnings: isMarathi ? "गुरुजी कमाई" : "Guruji Earnings",
    awaiting: isMarathi ? "प्रतीक्षेत" : "Awaiting",
    all: isMarathi ? "सर्व" : "All",
    booking: isMarathi ? "बुकिंग" : "Booking",
    gurujiEarning: isMarathi ? "गुरुजी कमाई" : "Guruji Earning",
    trustTotal: isMarathi ? "ट्रस्ट {trust} / एकूण {total}" : "Trust {trust} / Total {total}",
    paymentPaidRequired: isMarathi ? "पेमेंट paid असणे आवश्यक" : "Payment paid required",
    confirmSettlement: isMarathi ? "सेटलमेंट खात्री" : "Confirm settlement",
    settleHint: (type) =>
      isMarathi
        ? `${type === "payout" ? "हा payout" : "हे withdrawal"} settled करण्यापूर्वी bank/Razorpay reference टाका.`
        : `Enter the bank/Razorpay reference before marking this ${type === "payout" ? "payout" : "withdrawal"} as settled.`,
    settlementReference: isMarathi ? "सेटलमेंट रेफरन्स" : "Settlement reference",
    settlementPlaceholder: "UTR / transaction reference",
    saving: isMarathi ? "सेव्ह होत आहे..." : "Saving...",
    confirm: isMarathi ? "खात्री करा" : "Confirm",
    payoutDetail: isMarathi ? "पेआउट तपशील" : "Payout Detail",
    gurujiSection: isMarathi ? "गुरुजी" : "Guruji",
    mobile: isMarathi ? "मोबाइल" : "Mobile",
    note: isMarathi ? "नोंद" : "Note",
    bankNote: isMarathi
      ? "बँक/UPI माहिती नोंदणीवेळी नाही, withdrawal request मध्ये साठवली जाते."
      : "Bank/UPI is captured per withdrawal request, not at registration.",
    settlement: isMarathi ? "सेटलमेंट" : "Settlement",
    total: isMarathi ? "एकूण" : "Total",
    trust70: isMarathi ? "ट्रस्ट 70%" : "Trust 70%",
    guruji30: isMarathi ? "गुरुजी 30%" : "Guruji 30%",
    settlementRef: isMarathi ? "सेटलमेंट रेफ" : "Settlement Ref",
    settledAt: isMarathi ? "सेटल झाल्याची वेळ" : "Settled At",
    withdrawalRequests: isMarathi ? "withdrawal विनंत्या" : "Withdrawal Requests",
    ledgerHistory: isMarathi ? "लेजर इतिहास" : "Ledger History",
    noLedger: isMarathi ? "लेजर नोंदी नाहीत." : "No ledger entries.",
    noWithdrawal: isMarathi ? "withdrawal विनंत्या नाहीत." : "No withdrawal requests.",
    method: isMarathi ? "पद्धत" : "Method",
    holder: isMarathi ? "धारक" : "Holder",
    account: isMarathi ? "खाते" : "Account",
    ref: isMarathi ? "रेफ" : "Ref",
    settling: isMarathi ? "सेटल होत आहे..." : "Settling...",
    settleWithdrawal: isMarathi ? "Withdrawal settle करा" : "Settle Withdrawal",
    markSettled: isMarathi ? "सेटल झाले मार्क करा" : "Mark Settled",
  };

  const { rows: earnings, loading } = useApiCollection(null, "payouts-module");
  const { rows: ledger } = useApiCollection(null, "payout-ledger");
  const { rows: withdrawals } = useApiCollection(null, "withdrawal-requests");
  const { rows: gurujis } = useApiCollection(null, "payout-guruji");

  const filtered = earnings.filter((entry) => {
    const matchesSearch = `${entry.gurujiName || ""} ${entry.id || ""}`.toLowerCase().includes(searchTerm.toLowerCase());
    const status = entry.payoutStatus || "Pending";
    return matchesSearch && (payoutStatus === "All" || status === payoutStatus);
  });

  const totalEarned = earnings.reduce((sum, entry) => sum + Number(entry.gurujiShare || entry.totalAmount * 0.3), 0);
  const totalSettled = earnings
    .filter((entry) => entry.payoutStatus === "Settled")
    .reduce((sum, entry) => sum + Number(entry.gurujiShare || entry.totalAmount * 0.3), 0);

  const openSettlementDialog = (type, id) => {
    setSettlementDialog({ type, id });
    setSettlementRef("");
    setNotice("");
    setError("");
  };

  const closeSettlementDialog = () => {
    if (actionLoading) return;
    setSettlementDialog(null);
    setSettlementRef("");
  };

  const confirmSettlement = async () => {
    const ref = settlementRef.trim();
    if (!settlementDialog || ref.length < 4) {
      setError(isMarathi ? "वैध settlement reference टाका." : "Enter a valid settlement reference.");
      return;
    }
    setActionLoading(settlementDialog.id);
    setError("");
    try {
      if (settlementDialog.type === "payout") {
        await callAdminApi("markPayoutSettled", { bookingId: settlementDialog.id, settlementRef: ref });
        setNotice(isMarathi ? "Payout settled झाला." : "Payout settled.");
      } else {
        await callAdminApi("settleWithdrawalRequest", { requestId: settlementDialog.id, settlementRef: ref });
        setNotice(isMarathi ? "Withdrawal settled झाला." : "Withdrawal settled.");
      }
      setSettlementDialog(null);
      setSettlementRef("");
    } catch (actionError) {
      setError(actionError.message);
    } finally {
      setActionLoading(null);
    }
  };

  return (
    <AdminShell className="p-6" requiredCapability="withdrawal:write">
      <header className="mb-6 flex flex-col items-start justify-between gap-4 md:flex-row md:items-center">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">{t("payouts")}</h1>
          <p className="text-sm text-slate-500">{t("manage_settlements")}</p>
        </div>
        <button
          onClick={() => exportCsv("payouts.csv", filtered)}
          className="flex items-center gap-2 rounded border border-slate-300 bg-white px-3 py-2 text-sm text-slate-600 hover:bg-slate-50"
        >
          <Download size={16} /> {t("export_csv")}
        </button>
      </header>

      {error && <div className="mb-4 rounded border border-red-200 bg-red-50 px-4 py-3 text-sm font-semibold text-red-700">{error}</div>}
      {notice && <div className="mb-4 rounded border border-green-200 bg-green-50 px-4 py-3 text-sm font-semibold text-green-700">{notice}</div>}

      <div className="mb-8 grid grid-cols-1 gap-6 md:grid-cols-3">
        <Stat icon={<Wallet size={20} />} label={copy.gurujiEarnings} value={money(totalEarned)} color="text-blue-600" bg="bg-blue-50" />
        <Stat icon={<CheckCircle2 size={20} />} label={t("settled")} value={money(totalSettled)} color="text-green-600" bg="bg-green-50" />
        <Stat icon={<Clock size={20} />} label={copy.awaiting} value={money(totalEarned - totalSettled)} color="text-amber-600" bg="bg-amber-50" />
      </div>

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white shadow-sm">
        <div className="flex flex-col gap-3 border-b bg-slate-50 p-4 md:flex-row">
          <div className="relative max-w-md flex-1">
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
            value={payoutStatus}
            onChange={(e) => setPayoutStatus(e.target.value)}
          >
            <option value="All">{copy.all}</option>
            <option value="Pending">{t("pending")}</option>
            <option value="Settled">{t("settled")}</option>
          </select>
        </div>

        <table className="w-full text-left text-sm">
          <thead>
            <tr className="border-b bg-slate-50 text-xs font-semibold uppercase text-slate-600">
              <th className="p-4">{t("guruji")}</th>
              <th className="p-4">{copy.booking}</th>
              <th className="p-4">{copy.gurujiEarning}</th>
              <th className="p-4">{t("status")}</th>
              <th className="p-4 text-right"></th>
            </tr>
          </thead>
          <tbody className="divide-y">
            {loading ? (
              <tr>
                <td colSpan="5" className="p-10 text-center text-slate-400">
                  <Loader2 className="mx-auto animate-spin" />
                </td>
              </tr>
            ) : filtered.length === 0 ? (
              <tr>
                <td colSpan="5" className="p-10 text-center text-slate-400">
                  {t("no_data")}
                </td>
              </tr>
            ) : (
              filtered.map((entry) => {
                const settlementAllowed = canSettlePayout(entry);
                return (
                  <tr
                    key={entry.id}
                    className="cursor-pointer hover:bg-slate-50"
                    onClick={() => setSelectedPayout(entry)}
                  >
                    <td className="p-4">
                      <p className="font-medium text-slate-800">{entry.gurujiName}</p>
                      <p className="font-mono text-xs uppercase text-slate-400">ID: {entry.gurujiId || entry.id?.slice(-6)}</p>
                    </td>
                    <td className="p-4">
                      <p className="font-medium">{entry.poojaName}</p>
                      <p className="text-xs text-slate-500">{formatDate(entry.paidAt || entry.createdAt)}</p>
                    </td>
                    <td className="p-4">
                      <p className="font-bold text-blue-600">{money(entry.gurujiShare || entry.totalAmount * 0.3)}</p>
                      <p className="text-xs text-slate-500">
                        {interpolate(copy.trustTotal, {
                          trust: money(entry.trustShare),
                          total: money(entry.totalAmount),
                        })}
                      </p>
                    </td>
                    <td className="p-4">
                      <span
                        className={`rounded px-2 py-0.5 text-[11px] font-medium ${
                          entry.payoutStatus === "Settled"
                            ? "bg-green-100 text-green-700"
                            : "bg-blue-100 text-blue-700"
                        }`}
                      >
                        {payoutStatusLabel(entry.payoutStatus || "Pending", isMarathi)}
                      </span>
                    </td>
                    <td className="p-4 text-right" onClick={(event) => event.stopPropagation()}>
                      {entry.payoutStatus !== "Settled" ? (
                        <button
                          disabled={actionLoading === entry.id || !settlementAllowed}
                          onClick={() => openSettlementDialog("payout", entry.id)}
                          className="text-xs font-semibold text-blue-600 hover:underline disabled:text-slate-300 disabled:no-underline"
                        >
                          {actionLoading === entry.id ? <Loader2 size={12} className="animate-spin" /> : copy.markSettled}
                        </button>
                      ) : (
                        <span className="text-xs font-medium text-slate-400">{t("paid")}</span>
                      )}
                      {!settlementAllowed && entry.payoutStatus !== "Settled" && (
                        <p className="mt-1 text-[10px] text-slate-400">{copy.paymentPaidRequired}</p>
                      )}
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {selectedPayout && (
        <PayoutDrawer
          payout={selectedPayout}
          guruji={gurujis.find((guruji) => guruji.id === selectedPayout.gurujiId)}
          ledger={ledger.filter((row) => row.gurujiId === selectedPayout.gurujiId || row.bookingId === selectedPayout.id)}
          withdrawals={withdrawals.filter((row) => row.gurujiId === selectedPayout.gurujiId)}
          actionLoading={actionLoading}
          copy={copy}
          isMarathi={isMarathi}
          onSettleWithdrawal={(requestId) => openSettlementDialog("withdrawal", requestId)}
          onClose={() => setSelectedPayout(null)}
        />
      )}

      {settlementDialog && (
        <SettlementDialog
          type={settlementDialog.type}
          value={settlementRef}
          loading={actionLoading === settlementDialog.id}
          copy={copy}
          onChange={setSettlementRef}
          onCancel={closeSettlementDialog}
          onConfirm={confirmSettlement}
        />
      )}
    </AdminShell>
  );
}

function SettlementDialog({ type, value, loading, copy, onChange, onCancel, onConfirm }) {
  return (
    <div className="fixed inset-0 z-[120] flex items-center justify-center bg-black/50 p-4">
      <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl">
        <h2 className="text-lg font-bold text-slate-900">{copy.confirmSettlement}</h2>
        <p className="mt-2 text-sm text-slate-500">{copy.settleHint(type)}</p>
        <label className="mt-5 block text-xs font-bold uppercase text-slate-500">{copy.settlementReference}</label>
        <input
          autoFocus
          value={value}
          onChange={(event) => onChange(event.target.value)}
          className="mt-2 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm font-mono outline-none focus:border-blue-500"
          placeholder={copy.settlementPlaceholder}
        />
        <div className="mt-6 flex justify-end gap-3">
          <button
            onClick={onCancel}
            disabled={loading}
            className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-600 disabled:opacity-50"
          >
            Cancel
          </button>
          <button
            onClick={onConfirm}
            disabled={loading}
            className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {loading ? copy.saving : copy.confirm}
          </button>
        </div>
      </div>
    </div>
  );
}

function PayoutDrawer({ payout, guruji, ledger, withdrawals, actionLoading, copy, isMarathi, onSettleWithdrawal, onClose }) {
  return (
    <div className="fixed inset-0 z-[100] flex justify-end bg-black/50">
      <div className="h-full w-full max-w-xl overflow-y-auto bg-white p-6 shadow-xl">
        <div className="mb-6 flex items-center justify-between">
          <h2 className="text-lg font-bold">{copy.payoutDetail}</h2>
          <button onClick={onClose}>
            <X size={20} className="text-slate-400 hover:text-slate-600" />
          </button>
        </div>
        <div className="space-y-5">
          <Section title={copy.gurujiSection}>
            <Field label={copy.gurujiSection} value={guruji?.fullName || payout.gurujiName} />
            <Field label={copy.mobile} value={guruji?.mobile} />
            <Field label={copy.note} value={copy.bankNote} />
          </Section>
          <Section title={copy.settlement}>
            <div className="grid grid-cols-3 gap-3">
              <MoneyTile label={copy.total} value={payout.totalAmount} />
              <MoneyTile label={copy.trust70} value={payout.trustShare} />
              <MoneyTile label={copy.guruji30} value={payout.gurujiShare || payout.totalAmount * 0.3} />
            </div>
            <Field label={copy.settlementRef} value={payout.payoutTransactionRef || payout.settlementRef || payout.payoutSettledBy} mono />
            <Field label={copy.settledAt} value={formatDate(payout.payoutSettledAt)} />
          </Section>
          <Section title={copy.withdrawalRequests}>
            <WithdrawalList rows={withdrawals} actionLoading={actionLoading} onSettle={onSettleWithdrawal} copy={copy} isMarathi={isMarathi} />
          </Section>
          <Section title={copy.ledgerHistory}>
            <MiniList
              rows={ledger.slice(0, 12)}
              empty={copy.noLedger}
              render={(row) => (
                <>
                  <b>{row.type}</b>
                  <span>{money(row.amount)} - {payoutStatusLabel(row.status, isMarathi)}</span>
                  <small>{formatDate(row.createdAt || row.timestamp)}</small>
                </>
              )}
            />
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

function MiniList({ rows, empty, render }) {
  if (!rows.length) return <p className="text-sm text-slate-400">{empty}</p>;
  return (
    <div className="space-y-2">
      {rows.map((row) => (
        <div key={row.id} className="flex justify-between gap-3 rounded border bg-slate-50 p-3 text-sm">
          {render(row)}
        </div>
      ))}
    </div>
  );
}

function WithdrawalList({ rows, actionLoading, onSettle, copy, isMarathi }) {
  if (!rows.length) return <p className="text-sm text-slate-400">{copy.noWithdrawal}</p>;
  return (
    <div className="space-y-3">
      {rows.map((row) => {
        const bank = row.bankDetails || {};
        const isPending = (row.status || "Pending") === "Pending";
        return (
          <div key={row.id} className="space-y-2 rounded border bg-slate-50 p-3 text-sm">
            <div className="flex justify-between gap-3">
              <b>{money(row.amount)}</b>
              <span>{payoutStatusLabel(row.status || "Pending", isMarathi)}</span>
              <small>{formatDate(row.createdAt)}</small>
            </div>
            <div className="grid grid-cols-1 gap-2 text-xs text-slate-600 md:grid-cols-2">
              <span>{copy.method}: {row.payoutMethod || (row.upiId ? "UPI" : "BANK")}</span>
              <span>UPI: {row.upiId || "-"}</span>
              <span>{copy.holder}: {bank.accountHolder || "-"}</span>
              <span>{copy.account}: {bank.accountNumber || "-"}</span>
              <span>IFSC: {bank.ifsc || "-"}</span>
              <span>{copy.ref}: {row.settlementRef || "-"}</span>
            </div>
            {isPending && (
              <button
                disabled={actionLoading === row.id}
                onClick={() => onSettle(row.id)}
                className="text-xs font-semibold text-blue-600 hover:underline disabled:text-slate-300"
              >
                {actionLoading === row.id ? copy.settling : copy.settleWithdrawal}
              </button>
            )}
          </div>
        );
      })}
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
  return `₹${Number(value || 0).toLocaleString()}`;
}

function formatDate(value) {
  if (!value) return "-";
  if (value?.toDate) return value.toDate().toLocaleString();
  if (value?.seconds) return new Date(value.seconds * 1000).toLocaleString();
  return String(value);
}

function canSettlePayout(payout) {
  return payout.payoutStatus !== "Settled" && (payout.paymentStatus === "Paid" || ["Paid", "Completed"].includes(payout.status || ""));
}

function interpolate(template, values) {
  return Object.entries(values).reduce(
    (text, [key, value]) => text.replace(`{${key}}`, value),
    template
  );
}

function payoutStatusLabel(status, isMarathi) {
  if (!isMarathi) return status;
  switch (status) {
    case "Pending":
      return "प्रलंबित";
    case "Settled":
      return "सेटल";
    case "Paid":
      return "पेड";
    case "Completed":
      return "पूर्ण";
    case "Submitted":
      return "सादर";
    default:
      return status;
  }
}
