"use client";

import { useState } from "react";
import { AlertCircle, Loader2, Home, Globe } from "lucide-react";
import { useRouter } from "next/navigation";
import { useLanguage } from "../../../context/LanguageContext";

export default function YajmanLoginPage() {
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
      
      // Verify role is Yajman/User
      if (data.role === "Yajman") {
        localStorage.setItem("jwt_auth_token", data.token);
        localStorage.setItem("yajman_profile", JSON.stringify(data.profile));
        router.push("/yajman/bookings");
      } else {
        throw new Error("Access Denied: Please use the Admin or Expert portals for other roles.");
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-orange-50 to-orange-100 px-4 font-sans relative">
      <div className="absolute top-6 right-8">
        <button 
          onClick={() => changeLang(lang === "mr" ? "en" : "mr")}
          className="flex items-center gap-2 bg-white border border-orange-200 px-3 py-1.5 rounded shadow text-orange-700 font-bold"
        >
          <Globe size={16} className="text-orange-500" />
          <span className="text-xs font-bold uppercase tracking-wider">
            {lang === 'mr' ? 'MR' : 'EN'}
          </span>
        </button>
      </div>

      <div className="max-w-md w-full bg-white rounded-[35px] shadow-2xl p-8 border border-orange-100 text-center">
        <div className="bg-orange-500/10 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-6">
          <Home className="text-orange-600" size={32} />
        </div>
        
        <h1 className="text-2xl font-black text-slate-800 uppercase tracking-wide">
          {lang === 'mr' ? 'यजमान लॉगिन' : 'Yajman Login'}
        </h1>
        <p className="text-slate-500 text-sm mt-1">
          {lang === 'mr' ? 'यज्ञिकी आणि वास्तू सेवा बुकिंग' : 'Yadnyiki & Vastu Seva Booking'}
        </p>

        {error && (
          <div className="bg-red-50 border border-red-200 p-3 rounded-2xl mt-6 text-red-600 text-xs flex items-start gap-2 text-left">
            <AlertCircle size={16} className="shrink-0 mt-0.5" />
            <p>{error}</p>
          </div>
        )}

        <form onSubmit={handleLogin} className="space-y-4 mt-8">
          <input
            type="text"
            className="w-full border border-slate-200 bg-slate-50 text-slate-800 p-4 rounded-2xl text-sm outline-none focus:border-orange-500"
            placeholder={lang === 'mr' ? 'तुमचा नोंदणीकृत मोबाईल नंबर' : 'Your Registered Mobile'}
            value={mobile}
            onChange={(e) => setMobile(e.target.value)}
            required
          />
          <button type="submit" disabled={loading} className="w-full bg-orange-600 text-white py-4 rounded-2xl font-bold uppercase tracking-wider hover:bg-orange-700 transition-colors shadow-lg shadow-orange-600/20">
            {loading ? <Loader2 className="animate-spin mx-auto text-white" size={20} /> : (lang === 'mr' ? 'लॉगिन करा' : 'Sign In')}
          </button>
        </form>
      </div>
    </div>
  );
}
