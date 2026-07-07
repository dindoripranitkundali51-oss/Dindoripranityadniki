"use client";
import { useMemo, useState } from "react";
import AdminShell from "@/components/AdminShell";
import ActionDialog, { ActionNotice } from "@/components/ActionDialog";
import { useApiCollection } from "@/lib/useApiCollection";
import { Search, ChevronRight, X } from "lucide-react";
import { useLanguage } from "@/context/LanguageContext";
import { callAdminApi } from "@/lib/apiClient";

export default function YajmanManagement() {
  const { t } = useLanguage();
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedUser, setSelectedUser] = useState(null);
  const [actionDialog, setActionDialog] = useState(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [actionError, setActionError] = useState("");
  const [notice, setNotice] = useState(null);

  const { rows: users, loading } = useApiCollection(null, "admin-users-list");
  const { rows: bookings } = useApiCollection(null, "admin-user-bookings");

  const filteredUsers = useMemo(() => {
    return users.filter((u) => `${u.fullName || ""} ${u.mobile || ""} ${u.email || ""}`.toLowerCase().includes(searchTerm.toLowerCase()));
  }, [users, searchTerm]);

  const toggleUserStatus = async (user) => {
    const isBlocked = user.status === "Blocked";
    const nextStatus = isBlocked ? "Active" : "Blocked";
    setActionError("");
    setActionDialog({
      userId: user.id,
      nextStatus,
      title: isBlocked ? "Restore user access?" : "Block this user?",
      message: isBlocked
        ? "This yajman will regain access to the app immediately."
        : "This yajman will be blocked from app access until an admin restores the account.",
      confirmLabel: isBlocked ? "Unblock user" : "Block user",
      tone: isBlocked ? "success" : "danger",
      requireReason: !isBlocked,
      reasonLabel: "Admin note",
      reasonPlaceholder: isBlocked
        ? "Optional unblock note for audit trail"
        : "Why is this user being blocked?",
    });
  };

  const confirmUserStatusChange = async (reason) => {
    if (!actionDialog) return;
    setActionLoading(true);
    try {
      await callAdminApi("manageUserStatus", {
        userId: actionDialog.userId,
        status: actionDialog.nextStatus,
        reason: reason || "",
      });
      setNotice({
        tone: "success",
        message: `User status updated to ${actionDialog.nextStatus}.`,
      });
      setActionDialog(null);
    } catch (err) {
      setActionError(err.message || "Status update failed.");
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <AdminShell className="p-6" requiredCapability="user:write">
      <h1 className="text-2xl font-bold mb-6">{t("yajman_management")}</h1>
      <ActionNotice
        tone={notice?.tone}
        message={notice?.message}
        onClose={() => setNotice(null)}
      />
      <div className="relative mb-6">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
        <input type="text" placeholder={t("search")} className="w-full border p-2 pl-10 rounded outline-none" value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} />
      </div>

      <div className="bg-white border rounded-lg overflow-x-auto">
        <table className="w-full text-left">
          <thead className="bg-slate-50 border-b">
            <tr><th className="p-4 text-xs font-semibold uppercase">{t("yajman")}</th><th className="p-4 text-xs font-semibold uppercase">{t("mobile")}</th><th className="p-4 text-xs font-semibold uppercase">Bookings</th><th className="p-4 text-xs font-semibold uppercase">{t("status")}</th><th className="p-4"></th></tr>
          </thead>
          <tbody className="divide-y">
            {loading ? (
              <tr><td colSpan="5" className="p-10 text-center text-slate-500">Loading users...</td></tr>
            ) : filteredUsers.map((u) => {
              const userBookings = bookings.filter((b) => b.userId === u.id);
              return (
                <tr key={u.id} className="hover:bg-slate-50 cursor-pointer" onClick={() => setSelectedUser(u)}>
                  <td className="p-4 text-sm"><p className="font-medium">{u.fullName}</p><p className="text-xs text-slate-500">{u.email || "-"}</p></td>
                  <td className="p-4 text-sm">{u.mobile || u.phone}</td>
                  <td className="p-4 text-sm">{userBookings.length}</td>
                  <td className="p-4"><span className={`px-2 py-0.5 rounded text-[11px] ${u.status === "Blocked" ? "bg-red-100 text-red-700" : "bg-green-100 text-green-700"}`}>{u.status || "Active"}</span></td>
                  <td className="p-4 text-right"><ChevronRight size={16} /></td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {selectedUser && (
        <UserDrawer
          user={selectedUser}
          bookings={bookings.filter((b) => b.userId === selectedUser.id)}
          onClose={() => setSelectedUser(null)}
          onToggleStatus={toggleUserStatus}
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
          loading={actionLoading}
          error={actionError}
          onClose={() => {
            if (actionLoading) return;
            setActionDialog(null);
            setActionError("");
          }}
          onConfirm={confirmUserStatusChange}
        />
      )}
    </AdminShell>
  );
}

function UserDrawer({ user, bookings, onClose, onToggleStatus }) {
  const totalPaid = bookings.reduce((sum, b) => sum + (b.paymentStatus === "Paid" ? Number(b.totalAmount || b.amount || 0) : 0), 0);
  return (
    <div className="fixed inset-0 bg-black/50 z-[100] flex justify-end">
      <div className="bg-white w-full max-w-xl h-full p-6 shadow-xl overflow-y-auto">
        <div className="flex justify-between items-center mb-6"><h2 className="text-lg font-bold">Yajman Full Detail</h2><button onClick={onClose}><X size={20} className="text-slate-400 hover:text-slate-600" /></button></div>
        <div className="space-y-5">
          <Section title="Profile & Contact">
            <Field label="Name" value={user.fullName} />
            <Field label="Mobile" value={user.mobile || user.phone} />
            <Field label="Email" value={user.email} />
            <Field label="Address" value={user.address || user.fullAddress} />
            <Field label="District" value={user.district} />
            <Field label="Status" value={user.status || "Active"} />
            <button onClick={() => onToggleStatus(user)} className={`w-full py-2 rounded text-white text-sm font-medium ${user.status === "Blocked" ? "bg-green-600" : "bg-red-600"}`}>{user.status === "Blocked" ? "Unblock User" : "Block User"}</button>
          </Section>
          <Section title="Booking Summary">
            <div className="grid grid-cols-3 gap-3">
              <Tile label="Total" value={bookings.length} />
              <Tile label="Completed" value={bookings.filter((b) => ["Paid", "Completed"].includes(b.status)).length} />
              <Tile label="Paid" value={money(totalPaid)} />
            </div>
          </Section>
          <Section title="Booking History">
            {bookings.length === 0 ? <p className="text-sm text-slate-400">No bookings yet.</p> : (
              <div className="space-y-2">{bookings.map((b) => <div key={b.id} className="bg-slate-50 border rounded p-3 text-sm"><div className="flex justify-between gap-3"><b>{b.poojaName}</b><span className="text-xs">{b.status}</span></div><p className="text-slate-500">{b.date || formatDate(b.createdAt)} - {money(b.totalAmount || b.amount)}</p></div>)}</div>
            )}
          </Section>
        </div>
      </div>
    </div>
  );
}

function Section({ title, children }) {
  return <section className="border rounded-lg p-4"><h3 className="text-sm font-bold text-slate-800 mb-3">{title}</h3><div className="space-y-3">{children}</div></section>;
}

function Field({ label, value }) {
  return <div><p className="text-xs text-slate-500 uppercase font-semibold">{label}</p><p className="text-slate-800">{value || "-"}</p></div>;
}

function Tile({ label, value }) {
  return <div className="bg-slate-50 border rounded p-3"><p className="text-xs text-slate-500">{label}</p><p className="font-bold">{value}</p></div>;
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

