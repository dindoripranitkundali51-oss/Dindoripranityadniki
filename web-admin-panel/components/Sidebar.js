"use client";
import { useState } from "react";
import { usePathname } from "next/navigation";
import Link from "next/link";
import {
  LayoutDashboard, Users, UserSquare2, BookOpen, CalendarCheck, Wallet, Settings, LogOut,
  ShieldCheck, LifeBuoy, BarChart3, Globe, Megaphone, Scale, Activity, BellRing, ReceiptText,
  ClipboardList, FolderClock, Languages, AlertTriangle, BadgeIndianRupee,
  History, Menu, X,
} from "lucide-react";
import { signOutAdmin } from "../lib/authClient";
import { useLanguage } from "../context/LanguageContext";
import { useAuth } from "../context/AuthContext";
import { hasAdminCapability } from "../lib/adminAccess";

export default function Sidebar() {
  const pathname = usePathname();
  const { lang, changeLang, t } = useLanguage();
  const { profile } = useAuth();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const menuItems = [
    { name: t("dashboard") || "Dashboard", icon: LayoutDashboard, path: "/" },
    { name: t("booking_center") || "Booking Center", icon: CalendarCheck, path: "/bookings", capability: "booking:read" },
    { name: "Service Requests", icon: ClipboardList, path: "/requests", capability: "booking:read" },
    { name: "Admin Alerts", icon: AlertTriangle, path: "/admin-alerts", capability: "risk:resolve" },
    { name: "Availability", icon: FolderClock, path: "/availability", capability: "guruji:write" },
    { name: t("yajman_management") || "Yajman Management", icon: Users, path: "/users", capability: "user:write" },
    { name: t("guruji_management") || "Guruji Management", icon: UserSquare2, path: "/guruji", capability: "guruji:write" },
    { name: t("support_tickets") || "Support Tickets", icon: LifeBuoy, path: "/support", capability: "support:read" },
    { name: "Feedback", icon: BellRing, path: "/feedback", capability: "support:read" },
    { name: t("finance_hub") || "Finance Hub", icon: Wallet, path: "/payments", capability: "payment:read" },
    { name: t("payouts") || "Payouts", icon: BadgeIndianRupee, path: "/payouts", capability: "withdrawal:write" },
    { name: t("receipts") || "Receipts", icon: ReceiptText, path: "/receipts", capability: "receipt:write" },
    { name: "Maker Checker", icon: ShieldCheck, path: "/maker-checker", capability: "maker_checker:approve" },
    { name: "Dead Letter", icon: AlertTriangle, path: "/dead-letter", capability: "audit:write" },
    { name: t("pooja_master") || "Pooja Master", icon: BookOpen, path: "/pooja-master" },
    { name: t("communication") || "Communication", icon: Megaphone, path: "/notifications", capability: "notification:write" },
    { name: "Activity Timeline", icon: History, path: "/activity-timeline", capability: "audit:write" },
    { name: "System Health", icon: Activity, path: "/system-health", capability: "audit:write" },
    { name: "Knowledge Base", icon: BookOpen, path: "/knowledge-base", capability: "support:read" },
    { name: "Android CMS", icon: Globe, path: "/android-cms", capability: "config:write" },
    { name: t("reports") || "Reports & Analytics", icon: BarChart3, path: "/reports", capability: "audit:write" },
    { name: "Audit Logs", icon: ClipboardList, path: "/audit-logs", capability: "audit:write" },
    { name: "Language", icon: Languages, path: "/language" },
    { name: t("legal") || "Legal & Info", icon: Scale, path: "/legal" },
    { name: t("super_settings") || "Super Settings", icon: Settings, path: "/settings", capability: "config:write" },
  ].filter((item) => !item.capability || hasAdminCapability(profile, item.capability));

  const mobileItems = menuItems.slice(0, 4);
  const toggleLanguage = () => {
    changeLang(lang === "mr" ? "en" : "mr");
  };

  const handleLogout = () => {
    signOutAdmin().catch(() => {});
    window.location.replace("/login?logout=1");
  };

  const closeMobileMenu = () => setMobileMenuOpen(false);

  return (
    <>
      <div className="lg:hidden fixed bottom-0 left-0 right-0 bg-white border-t z-50 px-4 py-2 flex justify-between items-center shadow-lg">
        {mobileItems.map((item) => (
          <Link
            key={item.path}
            href={item.path}
            aria-label={item.name}
            title={item.name}
            onClick={closeMobileMenu}
            className={`flex min-w-0 flex-1 flex-col items-center gap-0.5 rounded p-2 ${pathname === item.path ? "text-blue-600 bg-blue-50" : "text-slate-500"}`}
          >
            <item.icon size={20} aria-hidden="true" />
            <span className="max-w-full truncate text-[10px] font-semibold leading-tight">{item.name}</span>
          </Link>
        ))}
        <button
          onClick={() => setMobileMenuOpen(true)}
          aria-label="Open full menu"
          title="Open full menu"
          className="flex flex-col items-center gap-0.5 p-2 text-slate-600"
        >
          <Menu size={18} aria-hidden="true" />
          <span className="text-[10px] font-semibold leading-tight">More</span>
        </button>
        <button onClick={toggleLanguage} aria-label="Change language" title="Change language" className="flex flex-col items-center gap-0.5 p-2 text-blue-600 font-bold text-[10px] uppercase">
          <Globe size={18} aria-hidden="true" /> {lang}
        </button>
      </div>

      {mobileMenuOpen ? (
        <div className="lg:hidden fixed inset-0 z-[60] bg-black/50">
          <div className="absolute inset-y-0 left-0 w-[86%] max-w-sm bg-white shadow-2xl flex flex-col">
            <div className="flex items-center justify-between border-b px-5 py-4">
              <div>
                <h2 className="text-base font-bold text-slate-800">Admin Menu</h2>
                <p className="text-xs text-slate-500">All panel controllers</p>
              </div>
              <button
                onClick={closeMobileMenu}
                className="rounded-full p-2 text-slate-500 hover:bg-slate-100"
                aria-label="Close menu"
              >
                <X size={18} />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto px-4 py-4 space-y-5">
              <div className="space-y-1">
                {menuItems.map((item) => {
                  const isActive = pathname === item.path || (item.path !== "/" && pathname.startsWith(item.path));
                  return (
                    <Link
                      key={item.path}
                      href={item.path}
                      onClick={closeMobileMenu}
                      className={`flex items-center gap-3 rounded-lg px-3 py-3 ${
                        isActive ? "bg-blue-50 text-blue-600 font-semibold" : "text-slate-600 hover:bg-slate-50"
                      }`}
                    >
                      <item.icon size={18} className={isActive ? "text-blue-600" : "text-blue-500/70"} />
                      <span className="text-sm">{item.name}</span>
                    </Link>
                  );
                })}
              </div>
            </div>

            <div className="border-t p-4 space-y-2">
              <button
                onClick={toggleLanguage}
                className="flex w-full items-center justify-center gap-2 rounded-lg border border-blue-100 bg-blue-50 px-4 py-3 text-sm font-semibold text-blue-700"
              >
                <Globe size={16} />
                Language: {lang === "mr" ? "MR" : "EN"}
              </button>
              <button
                onClick={handleLogout}
                className="flex w-full items-center justify-center gap-2 rounded-lg border border-red-100 bg-red-50 px-4 py-3 text-sm font-semibold text-red-600"
              >
                <LogOut size={16} />
                Logout
              </button>
            </div>
          </div>
          <button aria-label="Close overlay" className="absolute inset-0 -z-10 cursor-default" onClick={closeMobileMenu} />
        </div>
      ) : null}

      <aside className="hidden lg:flex w-72 bg-white h-screen flex-col fixed left-0 top-0 border-r z-30 shadow-sm overflow-hidden">
        <div className="p-6 border-b flex items-center gap-3">
          <ShieldCheck className="text-blue-600 shrink-0" size={28} />
          <div className="flex items-center gap-3">
            <h2 className="font-bold text-slate-800 text-xl whitespace-nowrap">Admin Panel</h2>
            <button
              onClick={toggleLanguage}
              className="flex items-center gap-1.5 px-2 py-1 bg-blue-50 border border-blue-100 rounded hover:bg-blue-100 transition-all shrink-0"
            >
              <Globe size={14} className="text-blue-600" />
              <span className="font-bold text-blue-600 uppercase text-xs">{lang === "mr" ? "MR" : "EN"}</span>
            </button>
          </div>
        </div>

        <nav className="flex-1 overflow-y-auto px-4 py-4 space-y-1 custom-scrollbar">
          {menuItems.map((item) => {
            const isActive = pathname === item.path || (item.path !== "/" && pathname.startsWith(item.path));
            return (
              <Link
                key={item.path}
                href={item.path}
                className={`flex items-center gap-3 px-4 py-3 rounded-lg transition-all ${
                  isActive ? "bg-blue-50 text-blue-600 font-semibold" : "text-slate-500 hover:bg-slate-50 hover:text-blue-600"
                }`}
              >
                <item.icon size={20} className={isActive ? "text-blue-600" : "text-blue-500/70"} />
                <span className="text-[15px]">{item.name}</span>
              </Link>
            );
          })}
        </nav>

        <div className="p-4 border-t">
          <button
            onClick={handleLogout}
            className="flex items-center gap-3 w-full px-4 py-3 text-sm text-red-500 hover:bg-red-50 rounded-lg transition-colors font-medium"
          >
            <LogOut size={20} /> <span className="text-[15px]">Logout</span>
          </button>
        </div>
      </aside>
    </>
  );
}
