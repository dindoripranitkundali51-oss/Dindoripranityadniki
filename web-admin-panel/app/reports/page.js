"use client";
import { useMemo, useState } from "react";
import AdminShell from "@/components/AdminShell";
import { useApiCollection } from "@/lib/useApiCollection";
import { BarChart3, Download, TrendingUp, PieChart, Calendar, MapPinned, Filter } from "lucide-react";
import { exportCsv } from "@/lib/csv";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  LineChart,
  Line,
  PieChart as RePieChart,
  Pie,
  Cell,
} from "recharts";

export default function ReportsPage() {
  const [districtFilter, setDistrictFilter] = useState("All");
  const [statusFilter, setStatusFilter] = useState("All");
  const { rows: bookings, loading } = useApiCollection(null, "reports-module");

  const districts = useMemo(() => {
    return ["All", ...Array.from(new Set(bookings.map((item) => item.district).filter(Boolean))).sort()];
  }, [bookings]);

  const filteredBookings = useMemo(() => {
    return bookings.filter((item) => {
      const districtOk = districtFilter === "All" || (item.district || "") === districtFilter;
      const statusOk = statusFilter === "All" || (item.status || "Pending") === statusFilter;
      return districtOk && statusOk;
    });
  }, [bookings, districtFilter, statusFilter]);

  const stats = useMemo(() => {
    const bookingAmount = (b) => Number(b.actualAmount || 0);
    const revenue = filteredBookings.reduce((sum, b) => sum + bookingAmount(b), 0);
    const completed = filteredBookings.filter((b) => b.status === "Completed").length;

    const dailyData = filteredBookings.reduce((acc, b) => {
      const date = b.createdAt?.toDate ? b.createdAt.toDate().toLocaleDateString() : "Unknown";
      acc[date] = (acc[date] || 0) + bookingAmount(b);
      return acc;
    }, {});

    const chartData = Object.keys(dailyData).map((date) => ({ date, amount: dailyData[date] })).reverse().slice(-7);

    const typeData = filteredBookings.reduce((acc, b) => {
      const key = b.poojaName || "Unknown";
      acc[key] = (acc[key] || 0) + 1;
      return acc;
    }, {});

    const districtData = filteredBookings.reduce((acc, b) => {
      const key = b.district || "Unknown";
      acc[key] = (acc[key] || 0) + 1;
      return acc;
    }, {});

    const statusData = filteredBookings.reduce((acc, b) => {
      const key = b.status || "Pending";
      acc[key] = (acc[key] || 0) + 1;
      return acc;
    }, {});

    return {
      revenue,
      completed,
      chartData,
      pieData: Object.keys(typeData).map((name) => ({ name, value: typeData[name] })),
      districtData: Object.keys(districtData).map((name) => ({ name, value: districtData[name] })).sort((a, b) => b.value - a.value).slice(0, 8),
      statusData: Object.keys(statusData).map((name) => ({ name, value: statusData[name] })).sort((a, b) => b.value - a.value),
    };
  }, [filteredBookings]);

  const COLORS = ["#2563eb", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6", "#06b6d4", "#84cc16", "#f97316"];
  const hasRevenueData = stats.chartData.length > 0;
  const hasSevaData = stats.pieData.length > 0;
  const hasDistrictData = stats.districtData.length > 0;

  return (
    <AdminShell className="p-6">
      <header className="mb-6 flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">Reports & Analytics</h1>
          <p className="text-slate-500 text-sm">Advanced filters, exports, and geographical analytics.</p>
        </div>
        <button onClick={() => exportCsv("reports.csv", filteredBookings)} className="flex items-center gap-2 px-3 py-2 bg-white border border-slate-300 rounded text-sm text-slate-600 hover:bg-slate-50">
          <Download size={16} /> Export CSV
        </button>
      </header>

      <div className="mb-6 grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="rounded-lg border border-slate-200 bg-white p-4">
          <label className="text-xs font-semibold uppercase text-slate-500 flex items-center gap-2 mb-2"><Filter size={14} /> District</label>
          <select className="w-full border border-slate-300 rounded p-2 text-sm bg-slate-50" value={districtFilter} onChange={(e) => setDistrictFilter(e.target.value)}>
            {districts.map((district) => <option key={district} value={district}>{district}</option>)}
          </select>
        </div>
        <div className="rounded-lg border border-slate-200 bg-white p-4">
          <label className="text-xs font-semibold uppercase text-slate-500 flex items-center gap-2 mb-2"><Filter size={14} /> Status</label>
          <select className="w-full border border-slate-300 rounded p-2 text-sm bg-slate-50" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            {["All", "Pending", "Assigned", "Accepted", "In Progress", "Payment Pending", "Awaiting Verification", "Completed", "Cancelled"].map((status) => (
              <option key={status} value={status}>{status}</option>
            ))}
          </select>
        </div>
        <div className="rounded-lg border border-slate-200 bg-white p-4">
          <label className="text-xs font-semibold uppercase text-slate-500 mb-2 block">Filtered Bookings</label>
          <p className="text-2xl font-bold text-slate-800">{filteredBookings.length}</p>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4 mb-8">
        <ReportStat label="Total Revenue" value={`Rs. ${stats.revenue.toLocaleString()}`} icon={<TrendingUp size={18} />} color="text-green-600" bgColor="bg-green-50" />
        <ReportStat label="Completed Sevas" value={stats.completed} icon={<BarChart3 size={18} />} color="text-blue-600" bgColor="bg-blue-50" />
        <ReportStat label="Total Bookings" value={filteredBookings.length} icon={<PieChart size={18} />} color="text-amber-600" bgColor="bg-amber-50" />
        <ReportStat label="Avg. Order" value={`Rs. ${filteredBookings.length ? Math.round(stats.revenue / filteredBookings.length) : 0}`} icon={<Calendar size={18} />} color="text-indigo-600" bgColor="bg-indigo-50" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        <ChartPanel title="Revenue Trend (Last 7 Days)">
          {loading ? <ChartState label="Loading report data..." /> : !hasRevenueData ? <ChartState label="No revenue data available yet." /> : (
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={stats.chartData} margin={{ left: -16, right: 8, top: 8, bottom: 8 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                <XAxis dataKey="date" fontSize={10} tickLine={false} axisLine={false} minTickGap={12} />
                <YAxis fontSize={10} tickLine={false} axisLine={false} />
                <Tooltip contentStyle={{ borderRadius: "8px", border: "1px solid #e2e8f0" }} />
                <Line type="monotone" dataKey="amount" stroke="#2563eb" strokeWidth={2} dot={{ r: 4, fill: "#2563eb" }} activeDot={{ r: 6 }} />
              </LineChart>
            </ResponsiveContainer>
          )}
        </ChartPanel>

        <ChartPanel title="Seva Distribution">
          {loading ? <ChartState label="Loading distribution..." /> : !hasSevaData ? <ChartState label="No seva distribution yet." /> : (
            <ResponsiveContainer width="100%" height="100%">
              <RePieChart>
                <Pie data={stats.pieData} innerRadius="48%" outerRadius="70%" paddingAngle={5} dataKey="value">
                  {stats.pieData.map((entry, index) => <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />)}
                </Pie>
                <Tooltip />
              </RePieChart>
            </ResponsiveContainer>
          )}
        </ChartPanel>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <ChartPanel title="Geographical Analytics (Top Districts)">
          {loading ? <ChartState label="Loading district analytics..." /> : !hasDistrictData ? <ChartState label="No district data available yet." /> : (
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={stats.districtData} margin={{ left: -16, right: 8, top: 8, bottom: 8 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                <XAxis dataKey="name" fontSize={10} tickLine={false} axisLine={false} minTickGap={12} />
                <YAxis fontSize={10} tickLine={false} axisLine={false} />
                <Tooltip />
                <Bar dataKey="value" fill="#0f766e" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </ChartPanel>

        <div className="bg-white p-5 rounded-lg border border-slate-200 shadow-sm">
          <h3 className="text-sm font-semibold text-slate-800 mb-6 border-b pb-3 flex items-center gap-2">
            <MapPinned size={16} className="text-blue-600" /> Activity Summary
          </h3>
          <div className="space-y-3">
            {stats.statusData.length === 0 ? <ChartState label="No status summary yet." /> : stats.statusData.map((row) => (
              <div key={row.name} className="flex items-center justify-between rounded border border-slate-200 bg-slate-50 px-3 py-2">
                <span className="text-sm text-slate-700">{row.name}</span>
                <span className="text-sm font-bold text-slate-800">{row.value}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </AdminShell>
  );
}

function ChartPanel({ title, children }) {
  return (
    <div className="bg-white p-5 rounded-lg border border-slate-200 shadow-sm">
      <h3 className="text-sm font-semibold text-slate-800 mb-6 border-b pb-3">{title}</h3>
      <div className="h-[250px] w-full">{children}</div>
    </div>
  );
}

function ChartState({ label }) {
  return <div className="h-full w-full flex items-center justify-center text-sm text-slate-400 text-center px-4">{label}</div>;
}

function ReportStat({ label, value, icon, color, bgColor }) {
  return (
    <div className="bg-white p-4 rounded-lg border border-slate-200 shadow-sm">
      <div className={`w-9 h-9 ${bgColor} ${color} rounded flex items-center justify-center mb-3`}>{icon}</div>
      <p className="text-xs text-slate-500 font-medium">{label}</p>
      <h3 className="text-xl font-bold text-slate-800 mt-1">{value}</h3>
    </div>
  );
}
