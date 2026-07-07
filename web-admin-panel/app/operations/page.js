"use client";

import { useEffect, useState } from "react";
import { Activity, AlertTriangle, CalendarClock, MessageSquare, Power, ShieldCheck, Siren } from "lucide-react";
import ActionDialog, { ActionNotice } from "@/components/ActionDialog";
import AdminShell from "@/components/AdminShell";
import {
  DashboardHealthCard,
  DeadLetterRow,
  EmptyState,
  OperationsPanel,
  OperationsStat,
  RiskRow,
  StatusPill,
} from "@/components/operations/OperationsUi";
import { useAuth } from "@/context/AuthContext";
import { hasAdminCapability } from "@/lib/adminAccess";

import { callAdminApi } from "@/lib/apiClient";
import { buildDeadLetterReplayRequest, buildMakerCheckerApprovalDialog } from "@/lib/operationsActions";
import { formatDate, riskRank, summarizeNotificationFailures } from "@/lib/operationsMetrics";
import { useApiCollection } from "@/lib/useApiCollection";

export default function OperationsBrainPage() {
  const { profile } = useAuth();
  const [loadingId, setLoadingId] = useState("");
  const [notice, setNotice] = useState(null);
  const [error, setError] = useState("");
  const [actionDialog, setActionDialog] = useState(null);
  const [config, setConfig] = useState(null);
  const [opsDashboard, setOpsDashboard] = useState(null);

  const { rows: risks } = useApiCollection(null, "ops-risks");
  const { rows: makers } = useApiCollection(null, "ops-maker");
  const { rows: replies } = useApiCollection(null, "ops-replies");
  const { rows: digests } = useApiCollection(null, "ops-digests");
  const { rows: notificationLogs } = useApiCollection(null, "ops-notification-logs");
  const { rows: deadLetters } = useApiCollection(null, "ops-dead-letter");

  const { failedNotifications, escalatedNotifications } = summarizeNotificationFailures(notificationLogs);
  const canApproveMakerChecker = hasAdminCapability(profile, "maker_checker:approve");
  const canReplayDeadLetter = hasAdminCapability(profile, "audit:write");
  const riskSorted = [...risks].sort((a, b) => riskRank(b) - riskRank(a));

  useEffect(() => {
    const loadCmsAndConfig = async () => {
      try {
        const baseUrl = process.env.NEXT_PUBLIC_API_URL || "https://dindoripranitapi.somee.com/api/v1";
        const token = typeof window !== "undefined" ? localStorage.getItem("jwt_auth_token") || "" : "";
        const res = await fetch(`${baseUrl}/admin/settings`, {
          headers: {
            "Authorization": token ? `Bearer ${token}` : ""
          }
        });
        if (res.ok) {
          const data = await res.json();
          if (data.success && data.config) {
            setConfig(data.config);
          }
        }
      } catch (err) {
        console.error("Failed to load operations config:", err);
      }
    };
    loadCmsAndConfig();
  }, []);

  const approve = (id) => {
    setError("");
    setActionDialog(buildMakerCheckerApprovalDialog(id));
  };

  const confirmApprove = async (reason) => {
    if (!actionDialog) return;
    setLoadingId(actionDialog.requestId);
    try {
      await callAdminApi("approveMakerCheckerRequest", {
        requestId: actionDialog.requestId,
        note: reason,
      });
      setNotice({ tone: "success", message: "Maker-checker request approved successfully." });
      setActionDialog(null);
    } catch (e) {
      setError(e.message || "Approval failed");
    }
    setLoadingId("");
  };

  const handleReplayDeadLetter = async (deadLetterId) => {
    setLoadingId(deadLetterId);
    setError("");
    try {
      await callAdminApi("replayDeadLetter", {
        ...buildDeadLetterReplayRequest(deadLetterId),
      });
      setNotice({ tone: "success", message: "Dead-letter replay/escalation requested." });
    } catch (e) {
      setError(e.message || "Dead-letter replay failed");
    }
    setLoadingId("");
  };

  return (
    <AdminShell className="p-6" requiredCapability="audit:write">
      <ActionNotice tone={notice?.tone} message={notice?.message} onClose={() => setNotice(null)} />

      {error && !actionDialog && (
        <div className="mb-4 rounded border border-red-200 bg-red-50 p-3 text-sm text-red-700">
          {error}
        </div>
      )}

      <header className="mb-6">
        <h1 className="text-2xl font-bold text-slate-800 flex items-center gap-2"><Activity size={22} /> Operations Brain</h1>
        <p className="text-sm text-slate-500">Risk queue, maker-checker approvals, notification replies, and daily digest.</p>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
        <OperationsStat label="Open Risks" value={risks.filter((r) => (r.status || "Open") === "Open").length} tone="red" />
        <OperationsStat label="Maker Pending" value={makers.filter((m) => m.status === "Pending").length} tone="amber" />
        <OperationsStat label="Replies" value={replies.length} tone="blue" />
        <OperationsStat label="Latest Failed Push" value={digests[0]?.failedNotifications || 0} tone="slate" />
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-4 gap-6 mb-6">
        <section className="bg-white border border-slate-200 rounded-lg p-4 shadow-sm">
          <h2 className="font-bold text-slate-800 mb-4 flex items-center gap-2"><Power size={17} /> Kill Switch State</h2>
          <div className="space-y-2 text-sm">
            <StatusPill label="Global Kill Switch" active={Boolean(config?.kill_switch_enabled)} />
            <StatusPill label="Maintenance Mode" active={Boolean(config?.maintenance_mode)} />
            <StatusPill label="Bookings Blocked" active={Boolean(config?.disable_new_bookings)} />
            <StatusPill label="Payments Blocked" active={Boolean(config?.disable_payments)} />
            <StatusPill label="Broadcasts Blocked" active={Boolean(config?.disable_admin_broadcasts)} />
          </div>
        </section>

        <section className="bg-white border border-slate-200 rounded-lg p-4 shadow-sm">
          <h2 className="font-bold text-slate-800 mb-4 flex items-center gap-2"><Siren size={17} /> Escalation Controls</h2>
          <div className="space-y-2 text-sm text-slate-700">
            <div>Risk Threshold: <b>{config?.risk_open_threshold_minutes || 30} min</b></div>
            <div>Failover Threshold: <b>{config?.notification_failover_threshold_minutes || 15} min</b></div>
            <div>High-Risk Withdrawal: <b>Rs. {Number(config?.withdrawal_high_risk_amount || 5000).toLocaleString()}</b></div>
          </div>
        </section>

        <section className="bg-white border border-slate-200 rounded-lg p-4 shadow-sm">
          <h2 className="font-bold text-slate-800 mb-4 flex items-center gap-2"><AlertTriangle size={17} /> Notification Failover</h2>
          <div className="space-y-2 text-sm text-slate-700">
            <div>Failed Notifications: <b>{failedNotifications.length}</b></div>
            <div>Escalated Failures: <b>{escalatedNotifications.length}</b></div>
            <div>Last Digest Failures: <b>{digests[0]?.failedNotifications || 0}</b></div>
          </div>
        </section>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">
        <OperationsPanel title="Prioritized Risk Queue" icon={<AlertTriangle size={17} />}>
          {riskSorted.length === 0 ? <EmptyState /> : riskSorted.map((risk) => (
            <RiskRow key={risk.id} risk={risk} formatDate={formatDate} />
          ))}
        </OperationsPanel>

        <OperationsPanel title="Maker-Checker Approvals" icon={<ShieldCheck size={17} />}>
          {makers.length === 0 ? <EmptyState /> : makers.map((maker) => (
            <div key={maker.id} className="border rounded p-3 bg-slate-50 text-sm">
              <div className="flex justify-between gap-3">
                <b>{maker.action}</b>
                <span className={`text-xs px-2 py-0.5 rounded ${maker.status === "Approved" ? "bg-green-100 text-green-700" : "bg-amber-100 text-amber-700"}`}>
                  {maker.status}
                </span>
              </div>
              <p className="text-slate-500 text-xs mt-1">Target: {maker.targetId || "-"} | Maker: {maker.makerId || "-"}</p>
              {maker.status === "Pending" && canApproveMakerChecker && (
                <button
                  disabled={loadingId === maker.id}
                  onClick={() => approve(maker.id)}
                  className="mt-3 px-3 py-1.5 bg-blue-600 text-white rounded text-xs font-semibold disabled:opacity-60"
                >
                  {loadingId === maker.id ? "Approving..." : "Approve"}
                </button>
              )}
            </div>
          ))}
        </OperationsPanel>

        <OperationsPanel title="Notification Replies" icon={<MessageSquare size={17} />}>
          {replies.length === 0 ? <EmptyState /> : replies.map((reply) => (
            <div key={reply.id} className="border rounded p-3 bg-slate-50 text-sm">
              <b>{reply.bookingId || "No booking"}</b>
              <p className="text-slate-700">{reply.message}</p>
              <p className="text-xs text-slate-400">{formatDate(reply.createdAt)}</p>
            </div>
          ))}
        </OperationsPanel>

        <OperationsPanel title="Daily Digests" icon={<CalendarClock size={17} />}>
          {digests.length === 0 ? <EmptyState /> : digests.map((digest) => (
            <div key={digest.id} className="border rounded p-3 bg-slate-50 text-sm grid grid-cols-2 gap-2">
              <b className="col-span-2">{formatDate(digest.createdAt)}</b>
              <span>Risks: {digest.openRisks || 0}</span>
              <span>Withdrawals: {digest.pendingWithdrawals || 0}</span>
              <span>Approvals: {digest.pendingGurujiApprovals || 0}</span>
              <span>Failed Push: {digest.failedNotifications || 0}</span>
              <span>Breached Bookings: {digest.agingMetrics?.breachedBookings || 0}</span>
              <span>Breached Support: {digest.agingMetrics?.breachedSupportTickets || 0}</span>
            </div>
          ))}
        </OperationsPanel>

        <OperationsPanel title="Dead-Letter Replay Queue" icon={<AlertTriangle size={17} />}>
          {deadLetters.length === 0 ? <EmptyState /> : deadLetters.map((item) => (
            <DeadLetterRow
              key={item.id}
              item={item}
              canReplayDeadLetter={canReplayDeadLetter}
              loadingId={loadingId}
              onReplay={handleReplayDeadLetter}
            />
          ))}
        </OperationsPanel>

        <OperationsPanel title="Live Ops Dashboard" icon={<Activity size={17} />}>
          <DashboardHealthCard opsDashboard={opsDashboard} />
        </OperationsPanel>
      </div>

      {actionDialog && (
        <ActionDialog
          title={actionDialog.title}
          message={actionDialog.message}
          confirmLabel={actionDialog.confirmLabel}
          tone={actionDialog.tone}
          reasonLabel="Approval note"
          reasonPlaceholder="Optional note for operations audit trail"
          loading={loadingId === actionDialog.requestId}
          error={error}
          onClose={() => {
            if (loadingId) return;
            setActionDialog(null);
            setError("");
          }}
          onConfirm={confirmApprove}
        />
      )}
    </AdminShell>
  );
}
