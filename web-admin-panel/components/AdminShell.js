"use client";
import Sidebar from "@/components/Sidebar";
import { useAuth } from "@/context/AuthContext";
import { hasAdminCapability } from "@/lib/adminAccess";

export default function AdminShell({ children, className = "", requiredCapability = "" }) {
  const { loading, profile } = useAuth();
  const allowed = !requiredCapability || hasAdminCapability(profile, requiredCapability);

  if (loading) {
    return <div className="min-h-screen bg-slate-50" />;
  }

  if (!allowed) {
    return (
      <div className="min-h-screen bg-slate-50 font-sans">
        <Sidebar />
        <main className={`min-h-screen lg:ml-72 transition-all ${className}`}>
          <div className="mx-auto w-full px-4 py-10 sm:px-6">
            <div className="rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
              You do not have permission to access this page.
            </div>
          </div>
        </main>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-50 font-sans">
      {/* Sidebar Navigation */}
      <Sidebar />
      
      {/* Content Area */}
      <main className={`min-h-screen lg:ml-72 transition-all ${className}`}>
        <div className="mx-auto w-full px-4 py-6 sm:px-6 sm:py-8 pb-24 lg:pb-8">
          {children}
        </div>
      </main>
    </div>
  );
}
