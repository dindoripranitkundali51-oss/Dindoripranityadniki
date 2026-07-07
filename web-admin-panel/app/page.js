"use client";
import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { useLanguage } from "../context/LanguageContext";
import AdminShell from "../components/AdminShell";
import Link from "next/link";
import {
  IndianRupee,
  Users as UsersIcon,
  CalendarCheck,
  Activity,
  TrendingUp,
  Wallet,
} from "lucide-react";

export default function Dashboard() {
  const { user, isAdmin, loading: authLoading } = useAuth();
  const { t, lang } = useLanguage();

  const [stats, setStats] = useState({
    bookingsToday: 0,
    totalBookings: 0,
    totalUsers: 0,
    totalGuruji: 0,
    revenueToday: 0,
    totalRevenue: 0,
  });
  const [latestBookings, setLatestBookings] = useState([]);
  const [pendingRequests, setPendingRequests] = useState([]);
  const [latestAlerts, setLatestAlerts] = useState([]);
  const [statsReady, setStatsReady] = useState(false);
  const [uiSettings, setUiSettings] = useState({
    dashboard_title: "",
    dashboard_subtitle: "",
    maintenance_banner_en: "",
    maintenance_banner_mr: "",
  });
  const [opsDashboard, setOpsDashboard] = useState(null);

  useEffect(() => {
    if (!user || !isAdmin) return;

    const fetchDashboardData = async () => {
      try {
        const baseUrl = process.env.NEXT_PUBLIC_API_URL || "https://dindoritrial.somee.com/api/v1";
        const token = localStorage.getItem("jwt_auth_token") || "";

        const res = await fetch(`${baseUrl}/admin/dashboard`, {
          headers: {
            "Authorization": token ? `Bearer ${token}` : ""
          }
        });
        if (!res.ok) throw new Error(`HTTP error ${res.status}`);
        
        const data = await res.json();
        if (data.success && data.stats) {
          setStats({
            bookingsToday: data.stats.bookingsToday || 0,
            totalBookings: data.stats.totalBookings || 0,
            totalUsers: data.stats.totalUsers || 0,
            totalGuruji: data.stats.totalExperts || 0,
            revenueToday: data.stats.revenueToday || 0,
            totalRevenue: data.stats.totalRevenue || 0,
          });
          setLatestBookings(data.latestBookings || []);
          setPendingRequests(data.pendingRequests || []);
          setStatsReady(true);
        }
      } catch (err) {
        console.error("Error fetching dashboard data:", err);
      }
    };

    fetchDashboardData();
    const interval = setInterval(fetchDashboardData, 15000); // refresh every 15 seconds
    return () => clearInterval(interval);
  }, [user, isAdmin]);

  if (authLoading) {
    return (
      <div className="h-screen flex items-center justify-center">
        <Activity className="animate-spin text-blue-600" />
      </div>
    );
  }

  if (!user || !isAdmin) return null;

  const maintenanceBanner =
    lang === "mr"
      ? (uiSettings.maintenance_banner_mr || uiSettings.maintenance_banner_en)
      : (uiSettings.maintenance_banner_en || uiSettings.maintenance_banner_mr);

  return (
    <AdminShell className="p-6">
      <header className="mb-6">
        <h1 className="text-2xl font-bold text-slate-800">{uiSettings.dashboard_title || t("trust_name")}</h1>
        <p className="text-slate-500 text-sm">{uiSettings.dashboard_subtitle || t("platform_overview")}</p>
      </header>

      {maintenanceBanner && (
        <div className="mb-6 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
          {maintenanceBanner}
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <StatBox
          icon={<CalendarCheck size={20} />}
          label={t("bookings")}
          value={stats.totalBookings}
          subtext={`${t("today")}: ${stats.bookingsToday}`}
          color="text-blue-600"
          bgColor="bg-blue-50"
        />
        <StatBox
          icon={<UsersIcon size={20} />}
          label={t("total_members")}
          value={stats.totalUsers + stats.totalGuruji}
          subtext={`${t("yajman")}: ${stats.totalUsers} | ${t("guruji")}: ${stats.totalGuruji}`}
          color="text-green-600"
          bgColor="bg-green-50"
        />
        <StatBox
          icon={<IndianRupee size={20} />}
          label={t("total_revenue")}
          value={`Rs. ${stats.totalRevenue.toLocaleString()}`}
          subtext={`${t("today")}: Rs. ${stats.revenueToday.toLocaleString()}`}
          color="text-indigo-600"
          bgColor="bg-indigo-50"
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 bg-white p-6 rounded-lg border border-slate-200 shadow-sm">
          <h3 className="font-semibold text-slate-800 flex items-center gap-2 mb-6">
            <TrendingUp size={18} className="text-blue-600" /> Revenue Trend
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <MiniMetric label="Today Revenue" value={`Rs. ${stats.revenueToday.toLocaleString()}`} />
            <MiniMetric label="Today Bookings" value={stats.bookingsToday} />
            <MiniMetric
              label="Average Order"
              value={`Rs. ${stats.totalBookings ? Math.round(stats.totalRevenue / stats.totalBookings).toLocaleString() : 0}`}
            />
          </div>
        </div>

        <div className="bg-white p-6 rounded-lg border border-slate-200 shadow-sm">
          <h3 className="font-semibold text-slate-800 mb-4">System Health</h3>
          <div className="space-y-4">
            <HealthRow
              label={t("database") || "Database"}
              value={statsReady ? (t("synced") || "Synced") : "Waiting data"}
              status={statsReady ? "bg-green-500" : "bg-amber-500"}
            />
            <HealthRow
              label={t("auth_service") || "Auth"}
              value={user?.email ? (t("live") || "Live") : "No session"}
              status={user?.email ? "bg-green-500" : "bg-red-500"}
            />
            <HealthRow
              label={t("cloud_functions") || "Functions"}
              value={(opsDashboard?.deadLetterSummary?.critical || latestAlerts.length) > 0 ? "Check alerts" : (t("operational") || "Operational")}
              status={(opsDashboard?.deadLetterSummary?.critical || latestAlerts.length) > 0 ? "bg-amber-500" : "bg-green-500"}
            />
            <HealthRow
              label="Ops Queue"
              value={`DLQ ${opsDashboard?.deadLetterSummary?.open || 0} | Risk ${opsDashboard?.liveHealth?.openRisks ?? 0}`}
              status={(opsDashboard?.deadLetterSummary?.open || 0) > 0 ? "bg-amber-500" : "bg-green-500"}
            />
          </div>
        </div>
      </div>

      <div className="mt-8 grid grid-cols-1 xl:grid-cols-3 gap-6">
        <InfoPanel title="Latest Bookings" href="/bookings" items={latestBookings} emptyText="No recent bookings yet.">
          {(booking) => (
            <>
              <div className="flex items-center justify-between gap-3">
                <div>
                  <p className="font-medium text-slate-800">{booking.poojaName || "Booking"}</p>
                  <p className="text-xs text-slate-500">{booking.contactName || booking.userId || "-"}</p>
                </div>
                <span className="rounded bg-blue-50 px-2 py-1 text-[11px] font-semibold text-blue-700">
                  {booking.status || "Pending"}
                </span>
              </div>
              <p className="mt-2 text-xs text-slate-400">#{booking.id}</p>
            </>
          )}
        </InfoPanel>

        <InfoPanel
          title="Pending Requests"
          href="/requests"
          items={pendingRequests.filter((entry) => (entry.status || "Pending") === "Pending")}
          emptyText="No pending requests."
        >
          {(request) => (
            <div className="flex items-center justify-between gap-3">
              <div>
                <p className="font-medium text-slate-800">{request.type || "Request"}</p>
                <p className="text-xs text-slate-500">{request.reason || request.bookingId || "-"}</p>
              </div>
              <span className="rounded bg-amber-50 px-2 py-1 text-[11px] font-semibold text-amber-700">
                {request.status || "Pending"}
              </span>
            </div>
          )}
        </InfoPanel>

        <InfoPanel title="Latest Alerts" href="/admin-alerts" items={latestAlerts} emptyText="No admin alerts right now.">
          {(alert) => (
            <>
              <p className="font-medium text-slate-800">{alert.type || "Alert"}</p>
              <p className="mt-1 text-xs text-slate-500">{alert.message || "-"}</p>
            </>
          )}
        </InfoPanel>
      </div>
    </AdminShell>
  );
}

function StatBox({ icon, label, value, subtext, color, bgColor }) {
  return (
    <div className="bg-white p-5 rounded-lg border border-slate-200 shadow-sm flex items-start gap-4">
      <div className={`${bgColor} ${color} p-3 rounded-md`}>{icon}</div>
      <div>
        <p className="text-slate-500 text-xs font-medium mb-1">{label}</p>
        <h3 className="text-xl font-bold text-slate-800">{value}</h3>
        <p className="text-slate-400 text-[11px] mt-1">{subtext}</p>
      </div>
    </div>
  );
}

function HealthRow({ label, value, status }) {
  return (
    <div className="flex justify-between items-center text-sm">
      <span className="text-slate-600">{label}</span>
      <div className="flex items-center gap-2">
        <span className="text-slate-800 font-medium">{value}</span>
        <div className={`w-2 h-2 rounded-full ${status}`} />
      </div>
    </div>
  );
}

function InfoPanel({ title, href, items, emptyText, children }) {
  return (
    <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
      <div className="mb-4 flex items-center justify-between gap-3">
        <h3 className="font-semibold text-slate-800">{title}</h3>
        <Link href={href} className="text-xs font-semibold text-blue-600 hover:underline">Open</Link>
      </div>
      <div className="space-y-3">
        {items.length === 0 ? (
          <p className="rounded border border-dashed border-slate-200 bg-slate-50 p-4 text-sm text-slate-400">{emptyText}</p>
        ) : (
          items.map((item) => (
            <div key={item.id} className="rounded border border-slate-200 p-3">
              {children(item)}
            </div>
          ))
        )}
      </div>
    </section>
  );
}

function MiniMetric({ label, value }) {
  return (
    <div className="rounded-lg border border-slate-200 bg-slate-50 p-4">
      <p className="text-xs font-medium text-slate-500">{label}</p>
      <p className="mt-1 text-xl font-bold text-slate-800">{value}</p>
    </div>
  );
}
