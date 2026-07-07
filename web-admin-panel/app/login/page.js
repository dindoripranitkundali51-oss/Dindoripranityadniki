"use client";

import { useState, useEffect, useCallback } from "react";
import { AlertCircle, Loader2, ShieldCheck, CheckCircle2, Globe } from "lucide-react";
import { useRouter } from "next/navigation";
import { signInAdmin, signOutAdmin, resetAdminPassword } from "../../lib/authClient";
import { getAdminAccess } from "../../lib/adminAccess";
import { useAuth } from "../../context/AuthContext";
import { useLanguage } from "../../context/LanguageContext";
import InstallAdminAppButton from "../../components/InstallAdminAppButton";

export default function LoginPage() {
  const { t, lang, changeLang } = useLanguage();
  const credentialPlaceholder = t("email_placeholder") || "Admin email or username";
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loading, setLoading] = useState(false);
  const [sessionReady, setSessionReady] = useState(false);
  const [view, setView] = useState("login"); // login, forgot
  
  const router = useRouter();
  const { user, isAdmin, refreshAccess } = useAuth();

  useEffect(() => {
    if (sessionReady && user && isAdmin) {
      router.replace("/");
    }
  }, [sessionReady, user, isAdmin, router]);

  const clearAdminSession = useCallback(async () => {
    await signOutAdmin().catch(() => {});
    await refreshAccess().catch(() => {});
  }, [refreshAccess]);

  useEffect(() => {
    let ignore = false;

    const prepareSession = async () => {
      if (typeof window !== "undefined" && new URLSearchParams(window.location.search).get("logout") === "1") {
        await clearAdminSession();
      }
      if (!ignore) {
        setSessionReady(true);
      }
    };

    prepareSession();

    return () => {
      ignore = true;
    }
  }, [clearAdminSession]);

  const toggleLanguage = () => {
    changeLang(lang === "mr" ? "en" : "mr");
  };

  const handleLogin = async (e) => {
    e.preventDefault();
    if (!sessionReady) return;
    setLoading(true);
    setError("");
    setSuccess("");

    try {
      const userCredential = await signInAdmin(email.trim(), password);
      const access = await getAdminAccess(userCredential.user);

      if (access.isAdmin) {
        await refreshAccess();
        window.location.href = "/";
        return;
      }

      setError("Access Denied: Admin privileges required.");
      await clearAdminSession();

    } catch (err) {
      await clearAdminSession();
      setError(err?.message || "Login failed");
    } finally {
      setLoading(false);
    }
  };

  const handleForgotPassword = async (e) => {
    e.preventDefault();
    if (!email) return setError("Please enter your email first.");
    setLoading(true);
    try {
        await resetAdminPassword(email);
        setSuccess("Password reset link sent to your email.");
        setView("login");
    } catch (err) {
        setError(err.message);
    } finally {
        setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50 px-4 font-sans relative">
      
      {/* Top Language Toggle */}
      <div className="absolute top-6 right-8">
        <button 
          onClick={toggleLanguage}
          className="flex items-center gap-2 bg-white border border-slate-200 px-3 py-1.5 rounded shadow-sm hover:bg-slate-50 transition-colors text-slate-600"
        >
          <Globe size={16} className="text-blue-600" />
          <span className="text-xs font-bold uppercase tracking-wider">
            {lang === 'mr' ? 'MR' : 'EN'}
          </span>
        </button>
      </div>

      <div className="max-w-md w-full bg-white rounded-lg shadow-sm p-8 border border-slate-200">
        
        <div className="text-center mb-8">
          <div className="bg-blue-600 w-12 h-12 rounded flex items-center justify-center mx-auto mb-4">
            <ShieldCheck className="text-white" size={24} />
          </div>
          <h1 className="text-xl font-bold text-slate-800">
            {view === "forgot" ? t("reset_access") : t("login_title")}
          </h1>
          <p className="text-slate-500 text-sm mt-1">
            {t("trust_name")} Admin Portal
          </p>
          <div className="mt-4 flex justify-center">
            <InstallAdminAppButton />
          </div>
        </div>

        {error && (
          <div className="bg-red-50 border border-red-200 p-3 rounded mb-6 flex items-start gap-2 text-red-700 text-xs">
            <AlertCircle size={16} className="shrink-0" />
            <p>{error}</p>
          </div>
        )}

        {success && (
          <div className="bg-green-50 border border-green-200 p-3 rounded mb-6 flex items-start gap-2 text-green-700 text-xs">
            <CheckCircle2 size={16} className="shrink-0" />
            <p>{success}</p>
          </div>
        )}

        {!sessionReady ? (
            <div className="flex items-center justify-center py-10 text-slate-500">
                <Loader2 className="animate-spin" size={22} />
            </div>
        ) : view === "forgot" ? (
            <form onSubmit={handleForgotPassword} className="space-y-4">
                <input
                    type="text"
                    className="w-full border p-3 rounded text-sm outline-none focus:border-blue-500"
                    placeholder={credentialPlaceholder}
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    autoComplete="username"
                    required
                />
                <button type="submit" disabled={loading} className="w-full bg-blue-600 text-white py-3 rounded font-medium hover:bg-blue-700">
                    {loading ? <Loader2 className="animate-spin mx-auto" size={20} /> : "Send Link"}
                </button>
                <button type="button" onClick={() => setView("login")} className="w-full text-sm text-slate-500 hover:underline">{t("cancel")}</button>
            </form>
        ) : (
            <form onSubmit={handleLogin} className="space-y-4">
                <input
                    type="text"
                    className="w-full border p-3 rounded text-sm outline-none focus:border-blue-500"
                    placeholder={credentialPlaceholder}
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    autoComplete="username"
                    required
                />
                <input
                    type="password"
                    className="w-full border p-3 rounded text-sm outline-none focus:border-blue-500"
                    placeholder={t("password_placeholder")}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                />
                <div className="text-right">
                    <button type="button" onClick={() => setView("forgot")} className="text-xs text-blue-600 hover:underline">{t("forgot_password")}</button>
                </div>
                <button type="submit" disabled={loading} className="w-full bg-blue-600 text-white py-3 rounded font-medium hover:bg-blue-700 transition-colors">
                    {loading ? <Loader2 className="animate-spin mx-auto" size={20} /> : t("secure_login")}
                </button>
            </form>
        )}
      </div>
    </div>
  );
}
