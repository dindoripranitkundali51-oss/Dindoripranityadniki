"use client";

import { useEffect, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import { useAuth } from "../context/AuthContext";
import { useLanguage } from "../context/LanguageContext";
import { Loader2, ShieldAlert } from "lucide-react";
import { signOutAdmin } from "../lib/authClient";

export default function AdminGate({ children }) {
  const { user, isAdmin, loading } = useAuth();
  const { t } = useLanguage();
  const [mounted, setMounted] = useState(false);
  const pathname = usePathname();
  const router = useRouter();
  
  const isLoginPage = pathname === "/login";

  useEffect(() => {
    setMounted(true);
  }, []);

  useEffect(() => {
    if (mounted && !loading && !isLoginPage) {
      if (!user) {
        router.replace("/login");
      }
    }
  }, [mounted, loading, user, isLoginPage, router]);

  const clearAndGoToLogin = async () => {
    await signOutAdmin().catch(() => {});
    window.location.replace("/login?logout=1");
  };

  if (isLoginPage) return <>{children}</>;

  if (!mounted) return null;

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-white">
        <Loader2 className="animate-spin text-blue-600" size={40} />
      </div>
    );
  }

  if (user && !isAdmin) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50 p-6">
        <div className="max-w-md w-full bg-white p-8 rounded-[40px] shadow-2xl text-center border border-red-100">
          <div className="bg-red-50 w-16 h-16 rounded-2xl flex items-center justify-center mx-auto mb-6">
            <ShieldAlert className="text-red-600" size={32} />
          </div>
          <h1 className="text-2xl font-black text-slate-800 uppercase mb-2">{t("access_denied") || "Access Denied"}</h1>
          <p className="text-slate-500 text-sm font-bold mb-6">{t("no_admin_privilege") || "Your account does not have administrator privileges."}</p>
          
          {process.env.NODE_ENV === "development" && (
            <div className="bg-slate-50 p-4 rounded-2xl mb-6 text-left border border-slate-100">
              <p className="text-[10px] font-black text-slate-400 uppercase mb-2">{t("debug_info") || "Debug Info"}</p>
              <p className="text-xs font-mono break-all text-slate-600">UID: {user.uid}</p>
              <p className="text-xs font-mono text-slate-600 mt-1">Whitelist: <span className="text-red-600 font-bold">No matching administrator document</span></p>
            </div>
          )}

          <button 
            onClick={clearAndGoToLogin}
            className="w-full bg-blue-600 text-white py-4 rounded-2xl font-black uppercase tracking-widest hover:bg-blue-700 transition-all"
          >
            {t("back_to_login") || "Back to Login"}
          </button>
        </div>
      </div>
    );
  }

  if (!user && !isLoginPage) return null;

  return <>{children}</>;
}
