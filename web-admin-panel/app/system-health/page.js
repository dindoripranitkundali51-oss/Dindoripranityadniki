"use client";

import { useEffect, useState } from "react";
import { Activity, BrainCircuit, DatabaseBackup, IndianRupee, MapPinned, ShieldCheck, Siren, Bot } from "lucide-react";
import AdminShell from "@/components/AdminShell";
import { useApiCollection } from "@/lib/useApiCollection";

export default function SystemHealthPage() {
  const { rows: healthRows } = useApiCollection(null, "health-snapshots");
  const { rows: botRows } = useApiCollection(null, "bot-activity");
  const { rows: complianceRows } = useApiCollection(null, "compliance-runs");
  const { rows: costRows } = useApiCollection(null, "cost-runs");
  const { rows: disasterRows } = useApiCollection(null, "dr-runs");
  const { rows: sentimentRows } = useApiCollection(null, "sentiment-runs");
  const { rows: lifecycleRows } = useApiCollection(null, "lifecycle-runs");
  const { rows: geoRows } = useApiCollection(null, "geo-events");
  const { rows: auditStoryRows } = useApiCollection(null, "audit-story");
  const { rows: verificationRows } = useApiCollection(null, "verification-insights");
  const { rows: conflictRows } = useApiCollection(null, "conflict-insights");
  const { rows: anomalyRows } = useApiCollection(null, "operational-anomalies");

  const [liveHealth, setLiveHealth] = useState(null);
  const [opsDashboard, setOpsDashboard] = useState(null);
  const [livePolicy, setLivePolicy] = useState(null);
  useEffect(() => {
    // TODO: Replace with API-based real-time subscriptions
  }, []);

  const latestHealth = liveHealth || healthRows[0] || {};
  const latestCompliance = complianceRows[0] || {};
  const latestCost = costRows[0] || {};
  const latestDisaster = disasterRows[0] || {};
  const latestSentiment = sentimentRows[0] || {};
  const latestLifecycle = lifecycleRows[0] || {};

  return (
    <AdminShell className="p-6" requiredCapability="audit:write">
      <header className="mb-6">
        <h1 className="flex items-center gap-2 text-2xl font-bold text-slate-800"><Activity size={22} /> Live System Health</h1>
        <p className="text-sm text-slate-500">Unified health, compliance, bot activity, lifecycle exceptions, DR, and cost telemetry.</p>
      </header>

      <div className="mb-6 grid grid-cols-1 gap-4 md:grid-cols-4">
        <MetricCard icon={<Activity size={18} />} label="Health Score" value={latestHealth.healthScore ?? "-"} hint={latestHealth.state || "No snapshot"} />
        <MetricCard icon={<ShieldCheck size={18} />} label="Compliance" value={latestCompliance.gstConfigured ? "Ready" : "Watch"} hint={`TDS ${latestCompliance.tdsPercent ?? "-" }%`} />
        <MetricCard icon={<IndianRupee size={18} />} label="Estimated Ops Cost" value={latestCost.estimatedOpsCost != null ? `Rs. ${Number(latestCost.estimatedOpsCost).toLocaleString()}` : "-"} hint={`Failed notif ${latestCost.failedNotifications ?? 0}`} />
        <MetricCard icon={<DatabaseBackup size={18} />} label="DR Readiness" value={latestDisaster.readinessScore ?? "-"} hint={latestDisaster.state || "No snapshot"} />
        <MetricCard icon={<Siren size={18} />} label="Dead-Letter Queue" value={opsDashboard?.deadLetterSummary?.open ?? "-"} hint={`Critical ${opsDashboard?.deadLetterSummary?.critical ?? 0}`} />
      </div>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-3">
        <Panel title="Live Health Snapshots" icon={<Activity size={17} />} span="xl:col-span-2">
          {healthRows.map((row) => (
            <Item key={row.id}
              title={`Score ${row.healthScore ?? "-"}`}
              subtitle={`State: ${row.state || "-"} | Open risks: ${row.openRisks ?? 0} | Failed notifications: ${row.failedNotifications ?? 0}`}
              meta={`${row.backupAgeHours ?? "-"}h backup age | ${row.reconciliationAgeHours ?? "-"}h recon age`}
              date={formatDate(row.createdAt)}
            />
          ))}
        </Panel>

        <Panel title="Bot Activity Logs" icon={<Bot size={17} />}>
          {botRows.map((row) => (
            <Item
              key={row.id}
              title={row.action || "Bot Action"}
              subtitle={`Status: ${row.status || "unknown"}`}
              meta={Object.keys(row.details || {}).slice(0, 4).map((key) => `${key}: ${String(row.details[key])}`).join(" | ")}
              date={formatDate(row.createdAt)}
            />
          ))}
        </Panel>

        <Panel title="Compliance Monitor" icon={<ShieldCheck size={17} />}>
          {complianceRows.map((row) => (
            <Item
              key={row.id}
              title={row.gstConfigured ? "GST configured" : "GST needs review"}
              subtitle={`TDS ${row.tdsPercent ?? "-"}% | Mismatch ${row.paymentMismatchCount ?? 0}`}
              meta={`Backup ${row.latestBackupAgeHours ?? "-"}h | Recon ${row.latestReconciliationAgeHours ?? "-"}h`}
              date={formatDate(row.createdAt)}
            />
          ))}
        </Panel>

        <Panel title="Live Policy Snapshot" icon={<ShieldCheck size={17} />}>
          <Item
            title={`Risk threshold ${livePolicy?.operations?.riskOpenThresholdMinutes ?? "-"} min`}
            subtitle={`Failover ${livePolicy?.operations?.notificationFailoverThresholdMinutes ?? "-"} min | Withdrawal high-risk Rs. ${Number(livePolicy?.operations?.withdrawalHighRiskAmount || 0).toLocaleString()}`}
            meta={`Trust share ${livePolicy?.financial?.trustSharePercent ?? "-"}% | TDS ${livePolicy?.financial?.tdsPercent ?? "-"}%`}
            date={livePolicy?.generatedAt?.toDate ? livePolicy.generatedAt.toDate().toLocaleString() : "-"}
          />
        </Panel>

        <Panel title="Lifecycle Stuck Resolver" icon={<Siren size={17} />}>
          {lifecycleRows.map((row) => (
            <Item
              key={row.id}
              title={`Stuck items ${row.stuckCount ?? 0}`}
              subtitle={(row.items || []).slice(0, 2).map((item) => `${item.status} ${item.bookingId}`).join(" | ") || "No items"}
              meta="Stuck booking/payment chain detector"
              date={formatDate(row.createdAt)}
            />
          ))}
        </Panel>

        <Panel title="Feedback Sentiment" icon={<BrainCircuit size={17} />}>
          {sentimentRows.map((row) => (
            <Item
              key={row.id}
              title={`Positive ${row.positive ?? 0} | Negative ${row.negative ?? 0}`}
              subtitle={`Neutral ${row.neutral ?? 0} | Sample ${row.sampleSize ?? 0}`}
              meta={(row.topKeywords || []).slice(0, 3).map((item) => `${item.key} (${item.count})`).join(" | ")}
              date={formatDate(row.createdAt)}
            />
          ))}
        </Panel>

        <Panel title="Geo Attendance Alerts" icon={<MapPinned size={17} />}>
          {geoRows.map((row) => (
            <Item
              key={row.id}
              title={`Booking ${row.bookingId || "-"}`}
              subtitle={`Distance ${row.distanceKm ?? "-"} km | ${row.state || "-"}`}
              meta={`Guruji ${row.gurujiId || "-"} | ${row.status || "-"}`}
              date={formatDate(row.createdAt)}
            />
          ))}
        </Panel>

        <Panel title="Verification Insights" icon={<ShieldCheck size={17} />}>
          {verificationRows.map((row) => (
            <Item
              key={row.id}
              title={`Verification flags ${row.itemCount ?? 0}`}
              subtitle={(row.items || []).slice(0, 2).map((item) => `${item.reason} ${item.bookingId}`).join(" | ") || "No flagged items"}
              meta="Heuristic payment and receipt verification monitor"
              date={formatDate(row.createdAt)}
            />
          ))}
        </Panel>

        <Panel title="Conflict Resolution Insights" icon={<BrainCircuit size={17} />}>
          {conflictRows.map((row) => (
            <Item
              key={row.id}
              title={`Conflict items ${row.itemCount ?? 0}`}
              subtitle={(row.items || []).slice(0, 2).map((item) => `${item.bookingId} (${item.requestCount})`).join(" | ") || "No conflict pressure"}
              meta={(row.topSupportUsers || []).slice(0, 2).map((item) => `${item.userId} (${item.count})`).join(" | ")}
              date={formatDate(row.createdAt)}
            />
          ))}
        </Panel>

        <Panel title="Operational Anomalies" icon={<Siren size={17} />}>
          {anomalyRows.map((row) => (
            <Item
              key={row.id}
              title={`${row.type || "ANOMALY"} | ${row.severity || "unknown"}`}
              subtitle={row.message || "No message"}
              meta={(row.items || []).slice(0, 2).map((item) => item.bookingId || item.id || item.subject || item.target || "-").join(" | ")}
              date={formatDate(row.createdAt)}
            />
          ))}
        </Panel>

        <Panel title="Disaster Recovery" icon={<DatabaseBackup size={17} />}>
          {disasterRows.map((row) => (
            <Item
              key={row.id}
              title={`Readiness ${row.readinessScore ?? "-"}`}
              subtitle={`State: ${row.state || "-"} | Backup age ${row.latestBackupAgeHours ?? "-"}h`}
              meta={`Health score ${row.latestHealthScore ?? "-"} | Archive age ${row.latestArchiveRunAgeHours ?? "-"}h`}
              date={formatDate(row.createdAt)}
            />
          ))}
        </Panel>

        <Panel title="Audit Storytelling" icon={<BrainCircuit size={17} />} span="xl:col-span-2">
          {auditStoryRows.map((row) => (
            <Item
              key={row.id}
              title={row.headline || "Audit digest"}
              subtitle={(row.actionCounts || []).slice(0, 3).map((item) => `${item.key} (${item.count})`).join(" | ")}
              meta={(row.recentActors || []).slice(0, 3).map((item) => `${item.key} (${item.count})`).join(" | ")}
              date={formatDate(row.createdAt)}
            />
          ))}
        </Panel>
      </div>
    </AdminShell>
  );
}

function MetricCard({ icon, label, value, hint }) {
  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
      <div className="mb-3 flex h-10 w-10 items-center justify-center rounded bg-blue-50 text-blue-600">{icon}</div>
      <p className="text-xs font-semibold uppercase text-slate-500">{label}</p>
      <p className="mt-1 text-2xl font-bold text-slate-800">{value}</p>
      <p className="mt-1 text-xs text-slate-400">{hint}</p>
    </div>
  );
}

function Panel({ title, icon, span = "", children }) {
  return (
    <section className={`rounded-lg border border-slate-200 bg-white p-5 shadow-sm ${span}`}>
      <h2 className="mb-4 flex items-center gap-2 font-semibold text-slate-800">{icon}{title}</h2>
      <div className="space-y-3 max-h-[520px] overflow-y-auto">{children.length ? children : <p className="text-sm text-slate-400">No records.</p>}</div>
    </section>
  );
}

function Item({ title, subtitle, meta, date }) {
  return (
    <div className="rounded border border-slate-200 bg-slate-50 p-3">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="font-semibold text-slate-800">{title}</p>
          <p className="mt-1 text-sm text-slate-600">{subtitle || "-"}</p>
        </div>
        <span className="text-[11px] text-slate-400">{date}</span>
      </div>
      <p className="mt-2 text-xs text-slate-500">{meta || "-"}</p>
    </div>
  );
}

function formatDate(value) {
  if (!value) return "-";
  if (value?.toDate) return value.toDate().toLocaleString();
  if (value?.seconds) return new Date(value.seconds * 1000).toLocaleString();
  return String(value);
}
