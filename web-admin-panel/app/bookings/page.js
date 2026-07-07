"use client";
import { useMemo, useState } from "react";
import AdminShell from "@/components/AdminShell";
import { ActionNotice } from "@/components/ActionDialog";
import { exportCsv } from "@/lib/csv";
import { useApiCollection } from "@/lib/useApiCollection";
import Link from "next/link";
import { Search, Download, X, Clock, Bell, Star } from "lucide-react";
import { useLanguage } from "@/context/LanguageContext";

export default function BookingsManagement() {
  const { t, lang } = useLanguage();
  const isMarathi = lang === "mr";
  const copy = {
    service: isMarathi ? "सेवा" : "Service",
    currentAction: isMarathi ? "चालू कृती" : "Current Action",
    amountSplit: isMarathi ? "रकमेचे विभाजन" : "Amount Split",
    details: isMarathi ? "तपशील" : "Details",
    yajmanGuruji: isMarathi ? "यजमान / गुरुजी" : "Yajman / Guruji",
    gurujiNotAssigned: isMarathi ? "गुरुजी नियुक्त नाहीत" : "Guruji not assigned",
    trustGurujiSplit: isMarathi ? "ट्रस्ट {trust} / गुरुजी {guruji}" : "Trust {trust} / Guruji {guruji}",
  };
  const [filter, setFilter] = useState("All");
  const [searchTerm, setSearchTerm] = useState("");
  const [dateFilter, setDateFilter] = useState("");
  const [selectedBooking, setSelectedBooking] = useState(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState(null);
  const [receipt, setReceipt] = useState(null);

  const { rows: bookings, loading } = useApiCollection(null, "admin-bookings");
  const selectedBookingId = selectedBooking?.id || "";
  const { rows: notificationRows } = useApiCollection(null, "booking-notifications");
  const { rows: feedbackRows } = useApiCollection(null, "booking-feedbacks");
  const { rows: eventRows } = useApiCollection(null, "booking-events");
  const notifications = useMemo(
    () => [...notificationRows].sort((a, b) => toMillis(b.createdAt) - toMillis(a.createdAt)).slice(0, 8),
    [notificationRows]
  );
  const feedback = feedbackRows[0] || null;
  const events = useMemo(
    () => [...eventRows].sort((a, b) => toMillis(b.createdAt) - toMillis(a.createdAt)).slice(0, 12),
    [eventRows]
  );

  const filtered = useMemo(() => {
    return bookings.filter((b) => {
      const text = `${b.contactName || ""} ${b.id || ""} ${b.poojaName || ""} ${b.gurujiName || ""}`.toLowerCase();
      const matchesSearch = text.includes(searchTerm.toLowerCase());
      const derivedStatus = bookingStatusLabel(b);
      const matchesFilter = filter === "All" || derivedStatus === filter || b.status === filter;
      const matchesDate = !dateFilter || b.date === dateFilter;
      return matchesSearch && matchesFilter && matchesDate;
    });
  }, [bookings, searchTerm, filter, dateFilter]);

  return (
    <AdminShell className="p-6" requiredCapability="booking:read">
      <ActionNotice
        tone={notice?.tone}
        message={notice?.message}
        onClose={() => setNotice(null)}
      />
      {error && !actionDialog && (
        <div className="mb-4 rounded border border-red-200 bg-red-50 p-3 text-sm text-red-700">
          {error}
        </div>
      )}
      <header className="mb-6 flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">{t("booking_center")}</h1>
          <p className="text-slate-500 text-sm">{t("monitor_services")}</p>
        </div>
        <button onClick={() => exportCsv("bookings.csv", filtered)} className="flex items-center gap-2 px-3 py-2 bg-white border border-slate-300 rounded text-sm text-slate-600 hover:bg-slate-50">
          <Download size={16} /> {t("export_csv")}
        </button>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
        <div className="relative md:col-span-2">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
          <input type="text" placeholder={t("search")} className="w-full border border-slate-300 p-2 pl-10 rounded outline-none text-sm focus:border-blue-500" value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} />
        </div>
        <select className="border border-slate-300 p-2 rounded text-sm bg-white" value={filter} onChange={(e) => setFilter(e.target.value)}>
          <option value="All">{t("all_status")}</option>
          {["Pending", "Assigned", "Accepted", "In Progress", "Payment Pending", "Awaiting Verification", "Completed", "Cancelled"].map((status) => <option key={status} value={status}>{statusLabel(status, isMarathi)}</option>)}
        </select>
        <input type="date" className="border border-slate-300 p-2 rounded text-sm bg-white" value={dateFilter} onChange={(e) => setDateFilter(e.target.value)} />
      </div>

      <div className="bg-white border rounded-lg overflow-x-auto shadow-sm">
        {loading ? (
          <div className="p-10 text-center text-slate-500">{t("loading")}</div>
        ) : filtered.length === 0 ? (
          <div className="p-10 text-center text-slate-500">{t("no_data")}</div>
        ) : (
          <table className="w-full text-left">
            <thead className="bg-slate-50 border-b">
              <tr>
                <th className="p-4 font-semibold text-slate-600 text-sm">{copy.service}</th>
                <th className="p-4 font-semibold text-slate-600 text-sm">{copy.yajmanGuruji}</th>
                <th className="p-4 font-semibold text-slate-600 text-sm">{copy.currentAction}</th>
                <th className="p-4 font-semibold text-slate-600 text-sm">{copy.amountSplit}</th>
                <th className="p-4"></th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((b) => (
                <tr key={b.id} className="hover:bg-slate-50 border-b last:border-0 cursor-pointer" onClick={() => setSelectedBooking(b)}>
                  <td className="p-4 text-sm">
                    <p className="font-medium text-slate-800">{b.poojaName}</p>
                    <p className="text-xs text-slate-500">{b.date || formatDate(b.createdAt)}</p>
                  </td>
                  <td className="p-4 text-sm">
                    <p className="text-slate-700">{b.contactName || "-"}</p>
                    <p className="text-xs text-blue-600">{b.gurujiName || copy.gurujiNotAssigned}</p>
                  </td>
                  <td className="p-4">
                    <span className={`px-2 py-1 rounded text-[11px] font-medium ${statusClass(bookingStatusLabel(b))}`}>{b.currentAdminActionTitle || b.currentUserActionTitle || statusLabel(bookingStatusLabel(b), isMarathi) || "Pending"}</span>
                  </td>
                  <td className="p-4 text-sm">
                    <p className="font-bold">{money(b.actualAmount)}</p>
                    <p className="text-xs text-slate-500">{interpolate(copy.trustGurujiSplit, { trust: money(b.trustShare), guruji: money(b.gurujiShare) })}</p>
                  </td>
                  <td className="p-4 text-right"><button className="text-blue-600 text-sm hover:underline">{copy.details}</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {selectedBooking && (
        <BookingDetailsModal
          booking={selectedBooking}
          notifications={notifications}
          feedback={feedback}
          events={events}
          receipt={receipt}
          onClose={() => setSelectedBooking(null)}
        />
      )}
    </AdminShell>
  );
}

function BookingDetailsModal({ booking, notifications, feedback, events, receipt, onClose }) {
  const { t, lang } = useLanguage();
  const isMarathi = lang === "mr";
  const copy = {
    bookingDetails: isMarathi ? "बुकिंग तपशील" : "Booking Details",
    lifecycleTimeline: isMarathi ? "जीवनचक्र टाइमलाइन" : "Lifecycle Timeline",
    paymentAndRazorpay: isMarathi ? "पेमेंट आणि Razorpay" : "Payment and Razorpay",
    immutableBookingEvents: isMarathi ? "अपरिवर्तनीय बुकिंग इव्हेंट्स" : "Immutable Booking Events",
    receiptSnapshot: isMarathi ? "पावती स्नॅपशॉट" : "Receipt Snapshot",
    receiptSnapshotPending: isMarathi ? "पेमेंट आणि ledger posting नंतर पावती स्नॅपशॉट freeze होईल." : "Receipt snapshot will freeze after payment and ledger posting.",
    appNotificationLogs: isMarathi ? "अॅप नोटिफिकेशन लॉग्स" : "App Notification Logs",
    feedbackPending: isMarathi ? "अजून अभिप्राय सबमिट झालेला नाही." : "Feedback not submitted yet.",
    addressInstructions: isMarathi ? "पत्ता आणि सूचना" : "Address and Instructions",
    automationState: isMarathi ? "ऑटोमेशन स्थिती" : "Automation State",
    automationHint: isMarathi
      ? "हे पृष्ठ आता निरीक्षणासाठी आहे. Cancel, completion close, receipt freeze आणि feedback prompt हे backend flow वर चालतात; admin panel मधून status jump करण्याची गरज ठेवलेली नाही."
      : "This page is now supervision-only. Cancel, completion close, receipt freeze, and feedback prompt are driven by backend flow instead of admin-side status jumps.",
  };
  return (
    <div className="fixed inset-0 bg-black/50 z-[100] flex items-center justify-center p-4">
      <div className="bg-white rounded-lg w-full max-w-5xl max-h-[92vh] overflow-y-auto p-6 relative shadow-xl">
        <button onClick={onClose} className="absolute right-4 top-4 text-slate-400 hover:text-slate-600"><X size={20} /></button>
        <h2 className="text-lg font-bold mb-4">{copy.bookingDetails}</h2>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 mb-6">
          <DetailBox title={isMarathi ? "यजमान" : "Yajman"} lines={[booking.contactName, booking.contactPhone, booking.contactEmail]} />
          <DetailBox title={isMarathi ? "गुरुजी" : "Guruji"} lines={[booking.gurujiName || booking.gurujiId, booking.gurujiPhone]} />
          <DetailBox title={isMarathi ? "सेवा" : "Seva"} lines={[booking.poojaName, booking.date, booking.district]} />
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 mb-6">
          <MoneyBox label={isMarathi ? "अंतिम दक्षिणा" : "Final Dakshina"} value={booking.actualAmount} />
          <MoneyBox label={`${isMarathi ? "ट्रस्ट हिस्सा" : "Trust Share"} (${booking.trustSharePercent || 70}%)`} value={booking.trustShare} />
          <MoneyBox label={`${isMarathi ? "गुरुजी हिस्सा" : "Guruji Share"} (${booking.gurujiSharePercent || 30}%)`} value={booking.gurujiShare} />
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
          <section className="border rounded-lg p-4">
            <h3 className="text-sm font-bold text-slate-800 mb-4 flex items-center gap-2"><Clock size={16} /> {copy.lifecycleTimeline}</h3>
            <div className="space-y-3">{bookingTimeline(booking).map((step) => <TimelineRow key={step.label} {...step} />)}</div>
          </section>
          <section className="border rounded-lg p-4">
            <h3 className="text-sm font-bold text-slate-800 mb-4">{copy.paymentAndRazorpay}</h3>
            <div className="grid grid-cols-1 gap-3 text-sm">
              <Field label={isMarathi ? "पेमेंट स्थिती" : "Payment Status"} value={statusLabel(booking.paymentStatus || "Pending", isMarathi)} />
              <Field label="Razorpay Payment ID" value={booking.gatewayPaymentId || booking.razorpayPaymentId || booking.paymentId} mono />
              <Field label={isMarathi ? "ट्रान्झॅक्शन आयडी / UTR" : "Transaction ID / UTR"} value={booking.transactionId} mono />
              <Field label={isMarathi ? "पडताळणी वेळ" : "Verified At"} value={formatDate(booking.paymentVerifiedAt || booking.verifiedAt || booking.paymentUpdatedAt || booking.paidAt)} />
              <Field label={isMarathi ? "अयशस्वी / नकार कारण" : "Failure / Rejection Reason"} value={booking.paymentFailureReason || booking.paymentRejectReason || booking.failureReason} />
            </div>
          </section>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
          <section className="border rounded-lg p-4">
            <h3 className="text-sm font-bold text-slate-800 mb-4">{copy.immutableBookingEvents}</h3>
            <div className="space-y-2">
              {(events || []).slice(0, 12).map((e) => (
                <div key={e.id} className="bg-slate-50 rounded p-3 text-sm">
                  <div className="flex justify-between gap-3"><b>{e.type}</b><span className="text-xs text-slate-500">{formatDate(e.createdAt)}</span></div>
                  <p className="text-xs text-slate-500">Actor: {e.actorId || "system"}</p>
                </div>
              ))}
              {(!events || events.length === 0) && <p className="text-sm text-slate-400">{isMarathi ? "अजून immutable events नाहीत." : "No immutable events yet."}</p>}
            </div>
          </section>
          <section className="border rounded-lg p-4">
            <h3 className="text-sm font-bold text-slate-800 mb-4">{copy.receiptSnapshot}</h3>
            {receipt ? (
              <div className="grid grid-cols-1 gap-3 text-sm">
                <Field label="Generated At" value={formatDate(receipt.generatedAt || receipt.frozenAt)} />
                <Field label="Total" value={money(receipt.amount || booking.actualAmount)} />
                <Field label="Trust Share" value={money(receipt.trustShare || booking.trustShare)} />
                <Field label="Guruji Share" value={money(receipt.gurujiShare || booking.gurujiShare)} />
                <Field label="Gateway Payment ID" value={receipt.gatewayPaymentId || booking.gatewayPaymentId || booking.razorpayPaymentId || booking.paymentId} mono />
              </div>
            ) : <p className="text-sm text-slate-400">{copy.receiptSnapshotPending}</p>}
          </section>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
          <section className="border rounded-lg p-4">
            <h3 className="text-sm font-bold text-slate-800 mb-4 flex items-center gap-2"><Bell size={16} /> {copy.appNotificationLogs}</h3>
            <div className="space-y-2">
              {notifications.slice(0, 8).map((n) => (
                <div key={n.id} className="bg-slate-50 rounded p-3 text-sm">
                  <div className="flex justify-between gap-3"><b>{n.title || n.meta?.title || n.action || "Notification"}</b><span className="text-xs text-slate-500">{formatDate(n.createdAt)}</span></div>
                  <p className="text-slate-600">{n.body || n.meta?.body || n.failureReason || n.meta?.failureReason || "-"}</p>
                </div>
              ))}
              {notifications.length === 0 && <p className="text-sm text-slate-400">{isMarathi ? "या बुकिंगसाठी notification logs नाहीत." : "No notification logs for this booking."}</p>}
            </div>
          </section>
          <section className="border rounded-lg p-4">
            <h3 className="text-sm font-bold text-slate-800 mb-4 flex items-center gap-2"><Star size={16} /> {isMarathi ? "अभिप्राय" : "Feedback"}</h3>
            {feedback ? <FeedbackBox feedback={feedback} /> : <p className="text-sm text-slate-400">{copy.feedbackPending}</p>}
          </section>
        </div>

        <div className="border rounded-lg p-4 mb-6">
          <h3 className="text-sm font-bold text-slate-800 mb-3">{copy.addressInstructions}</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
            <Field label={isMarathi ? "पत्ता" : "Address"} value={booking.address || booking.contactAddress} />
            <Field label={isMarathi ? "विशेष सूचना" : "Special Instructions"} value={booking.specialInstructions} />
          </div>
        </div>

        <div className="rounded-lg border border-blue-100 bg-blue-50 px-4 py-3 text-sm text-blue-900">
          <p className="font-semibold">{copy.automationState}</p>
          <p className="mt-1 text-blue-800">{copy.automationHint}</p>
        </div>
      </div>
    </div>
  );
}

function money(value) {
  const amount = Number(value);
  return Number.isFinite(amount) && amount > 0 ? `₹${amount.toLocaleString()}` : "-";
}

function formatDate(value) {
  if (!value) return "-";
  if (value?.toDate) return value.toDate().toLocaleString();
  if (value?.seconds) return new Date(value.seconds * 1000).toLocaleString();
  return String(value);
}

function toMillis(value) {
  if (!value) return 0;
  if (typeof value.toMillis === "function") return value.toMillis();
  if (typeof value?.seconds === "number") return value.seconds * 1000;
  const parsed = new Date(value).getTime();
  return Number.isFinite(parsed) ? parsed : 0;
}

function bookingTimeline(b) {
  return [
    { label: "Created", at: b.createdAt, done: true },
    { label: "Assigned", at: b.assignedAt, done: !!b.gurujiId || ["Assigned", "Accepted", "In Progress", "Payment Pending", "Awaiting Verification", "Paid", "Completed"].includes(b.status) },
    { label: "Accepted", at: b.acceptedAt, done: ["Accepted", "In Progress", "Payment Pending", "Awaiting Verification", "Paid", "Completed"].includes(b.status) },
    { label: "Start Seva", at: b.startedAt || b.sevaStartedAt, done: ["In Progress", "Payment Pending", "Awaiting Verification", "Paid", "Completed"].includes(b.status) },
    { label: "OTP Requested", at: b.completionOtpRequestedAt, done: b.completionOtpAvailable || b.completionOtpUsed },
    { label: "Completed by Guruji", at: b.completedAt, done: ["Payment Pending", "Awaiting Verification", "Paid", "Completed"].includes(b.status) },
    { label: "Paid", at: b.paidAt, done: b.paymentStatus === "Paid" || ["Paid", "Completed"].includes(b.status) },
    { label: "Feedback", at: b.feedbackSubmittedAt, done: !!b.feedbackSubmittedAt || !!b.rating },
  ];
}

function TimelineRow({ label, at, done }) {
  return (
    <div className="flex gap-3">
      <div className={`mt-1 w-3 h-3 rounded-full ${done ? "bg-green-500" : "bg-slate-300"}`} />
      <div><p className="text-sm font-semibold text-slate-700">{label}</p><p className="text-xs text-slate-500">{formatDate(at)}</p></div>
    </div>
  );
}

function Field({ label, value, mono = false }) {
  return <div><p className="text-xs text-slate-500 uppercase font-semibold">{label}</p><p className={`text-slate-800 ${mono ? "font-mono text-xs break-all" : ""}`}>{value || "-"}</p></div>;
}

function DetailBox({ title, lines }) {
  return <div className="bg-slate-50 p-4 rounded border"><p className="text-xs text-slate-500 uppercase font-semibold mb-1">{title}</p>{lines.filter(Boolean).map((line, i) => <p key={i} className={i === 0 ? "font-bold text-slate-800" : "text-sm text-slate-600"}>{line}</p>)}</div>;
}

function MoneyBox({ label, value }) {
  return <div className="bg-slate-50 p-4 rounded border"><p className="text-xs text-slate-500 uppercase font-semibold mb-1">{label}</p><p className="text-2xl font-bold text-slate-800">{money(value)}</p></div>;
}

function FeedbackBox({ feedback }) {
  return <div className="bg-slate-50 rounded p-3 text-sm"><p className="font-bold text-amber-600">{feedback.rating || "-"} / 5</p><p className="text-slate-700">{feedback.review || feedback.comment || "-"}</p><p className="text-xs text-slate-500 mt-2">{formatDate(feedback.createdAt)}</p></div>;
}

function statusClass(status) {
  if (status === "Completed" || status === "Paid") return "bg-green-100 text-green-700";
  if (status === "Cancelled" || status === "Rejected") return "bg-red-100 text-red-700";
  if (status === "Payment Pending" || status === "Awaiting Verification") return "bg-amber-100 text-amber-700";
  return "bg-blue-100 text-blue-700";
}

function bookingStatusLabel(booking) {
  if ((booking.paymentStatus || "") === "Paid" && booking.status !== "Cancelled") return "Completed";
  return booking.status || "Pending";
}

function statusLabel(status, isMarathi) {
  if (!isMarathi) return status;
  switch (status) {
    case "Pending": return "प्रलंबित";
    case "Assigned": return "नियुक्त";
    case "Accepted": return "स्वीकारले";
    case "In Progress": return "चालू";
    case "Payment Pending": return "पेमेंट प्रलंबित";
    case "Awaiting Verification": return "पडताळणी प्रलंबित";
    case "Completed": return "पूर्ण";
    case "Paid": return "पूर्ण";
    case "Cancelled": return "रद्द";
    case "Rejected": return "नाकारले";
    default: return status;
  }
}

function interpolate(template, values) {
  return Object.entries(values).reduce(
    (text, [key, value]) => text.replace(`{${key}}`, value),
    template
  );
}

