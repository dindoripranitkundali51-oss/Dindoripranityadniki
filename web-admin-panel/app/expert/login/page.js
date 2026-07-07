"use client";

import { useState } from "react";
import { AlertCircle, Loader2, Award, Globe } from "lucide-react";
import { useRouter } from "next/navigation";
import { useLanguage } from "../../../context/LanguageContext";

export default function ExpertLoginPage() {
  const { t, lang, changeLang } = useLanguage();
  const [mobile, setMobile] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const router = useRouter();

  const handleLogin = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");

    try {
      const baseUrl = process.env.NEXT_PUBLIC_API_URL || "https://dindoritrial.somee.com/api/v1";
      const res = await fetch(`${baseUrl}/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ mobile: mobile.trim() })
      });

      if (!res.ok) {
        const errData = await res.json().catch(() => ({}));
        throw new Error(errData.message || "Login failed");
      }

      const data = await res.json();
      
      // Verify role is expert
      if (data.role === "Guruji" || data.role === "VastuExpert") {
        localStorage.setItem("jwt_auth_token", data.token);
        localStorage.setItem("expert_profile", JSON.stringify(data.profile));
        router.push("/expert/dashboard");
      } else {
        throw new Error("Access Denied: This portal is strictly for certified Gurujis/Vastu Experts.");
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-900 px-4 font-sans relative">
      <div className="absolute top-6 right-8">
        <button 
          onClick={() => changeLang(lang === "mr" ? "en" : "mr")}
          className="flex items-center gap-2 bg-slate-800 border border-slate-700 px-3 py-1.5 rounded shadow text-slate-300"
        >
          <Globe size={16} className="text-amber-500" />
          <span className="text-xs font-bold uppercase tracking-wider">
            {lang === 'mr' ? 'MR' : 'EN'}
          </span>
        </button>
      </div>

      <div className="max-w-md w-full bg-slate-800 rounded-[30px] shadow-2xl p-8 border border-slate-700 text-center">
        <div className="bg-amber-500/10 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-6">
          <Award className="text-amber-500" size={32} />
        </div>
        <h1 className="text-2xl font-black text-white uppercase tracking-wide">
          {lang === 'mr' ? 'गुरुजी लॉगिन' : 'Guruji Login'}
        </h1>
        <p className="text-slate-400 text-sm mt-1">
          {lang === 'mr' ? 'श्री स्वामी समर्थ सेवा मार्ग' : 'Shree Swami Samarth Seva Marg'}
        </p>

        {error && (
          <div className="bg-red-500/10 border border-red-500/20 p-3 rounded-xl mt-6 text-red-400 text-xs flex items-start gap-2 text-left">
            <AlertCircle size={16} className="shrink-0 mt-0.5" />
            <p>{error}</p>
          </div>
        )}

        <form onSubmit={handleLogin} className="space-y-4 mt-8">
          <input
            type="text"
            className="w-full border border-slate-700 bg-slate-900 text-white p-4 rounded-2xl text-sm outline-none focus:border-amber-500"
            placeholder={lang === 'mr' ? 'तुमचा नोंदणीकृत मोबाईल नंबर' : 'Registered Mobile Number'}
            value={mobile}
            onChange={(e) => setMobile(e.target.value)}
            required
          />
          <button type="submit" disabled={loading} className="w-full bg-amber-500 text-slate-950 py-4 rounded-2xl font-bold uppercase tracking-wider hover:bg-amber-600 transition-colors">
            {loading ? <Loader2 className="animate-spin mx-auto text-slate-950" size={20} /> : (lang === 'mr' ? 'प्रवेश करा' : 'Login')}
          </button>
        </form>
      </div>
    </div>
  );
}
