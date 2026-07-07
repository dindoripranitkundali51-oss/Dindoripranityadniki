"use client";
import { useState, useEffect, useCallback } from "react";
import AdminShell from "@/components/AdminShell";
import { ActionNotice } from "@/components/ActionDialog";
import { Save, BookOpen, Loader2 } from "lucide-react";
import { useLanguage } from "@/context/LanguageContext";
import { callAdminApi } from "@/lib/apiClient";

export default function AppLanguageManagement() {
  const { lang: adminLang, t } = useLanguage(); 
  const [activeAppLang, setActiveAppLang] = useState("mr"); 
  const [strings, setStrings] = useState({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState(null);

  const fetchStrings = useCallback(async () => {
    setLoading(true);
    try {
      const baseUrl = process.env.NEXT_PUBLIC_API_URL || "https://dindoripranitapi.somee.com/api/v1";
      const token = typeof window !== "undefined" ? localStorage.getItem("jwt_auth_token") || "" : "";
      const res = await fetch(`${baseUrl}/admin/languages/${activeAppLang}`, {
        headers: {
          "Authorization": token ? `Bearer ${token}` : ""
        }
      });
      if (res.ok) {
        const data = await res.json();
        if (data.success && data.strings) {
          // If strings dictionary is empty, seed it with default placeholders
          const loadedStrings = Object.keys(data.strings).length > 0 ? data.strings : {
            app_name: "दिंडोरी प्रणित याज्ञिकी",
            btn_book_now: "बुकिंग करा",
            welcome_msg: "नमस्कार"
          };
          setStrings(loadedStrings);
        } else {
          setStrings({});
        }
      }
    } catch (e) { 
      console.error(e); 
    }
    setLoading(false);
  }, [activeAppLang]);

  useEffect(() => {
    fetchStrings();
  }, [fetchStrings]);

  const handleSave = async () => {
    setSaving(true);
    try {
      await callAdminApi("saveTranslations", {
        lang: activeAppLang,
        strings: strings
      });
      setNotice({ tone: "success", message: "App translations updated successfully." });
    } catch (e) {
      setNotice({ tone: "danger", message: e.message || "Translation update failed." });
    }
    setSaving(false);
  };

  return (
    <AdminShell className="p-6">
      <ActionNotice
        tone={notice?.tone}
        message={notice?.message}
        onClose={() => setNotice(null)}
      />
      <header className="mb-6 flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">App Translation Manager</h1>
          <p className="text-slate-500 text-sm">Edit words that appear in the Mobile App</p>
        </div>
        <button onClick={handleSave} disabled={saving} className="bg-blue-600 text-white px-4 py-2 rounded text-sm font-medium hover:bg-blue-700 flex items-center gap-2 transition-colors">
          {saving ? <Loader2 className="animate-spin" size={16} /> : <Save size={18} />} Save Changes
        </button>
      </header>

      {/* Tabs */}
      <div className="flex bg-white border border-slate-300 p-1 rounded mb-8 w-fit gap-1 overflow-x-auto">
        {[
          { id: "mr", label: "मराठी (App)" },
          { id: "hi", label: "हिंदी (App)" },
          { id: "en", label: "English (App)" }
        ].map((l) => (
          <button 
            key={l.id} 
            onClick={() => setActiveAppLang(l.id)}
            className={`px-6 py-1.5 rounded text-xs font-medium transition-colors ${activeAppLang === l.id ? "bg-slate-100 text-blue-600" : "text-slate-500 hover:bg-slate-50"}`}
          >
            {l.label}
          </button>
        ))}
      </div>

      <div className="bg-white border border-slate-200 rounded-lg shadow-sm p-6">
        <div className="flex items-center gap-2 mb-6 border-b pb-3 text-blue-600">
            <BookOpen size={18} /> 
            <h3 className="font-semibold text-slate-800 text-sm">Editing {activeAppLang.toUpperCase()} Dictionary</h3>
        </div>
        
        {loading ? (
            <div className="py-20 text-center text-slate-400">
                <Loader2 className="animate-spin mx-auto mb-2 text-blue-600" />
                <p className="text-sm italic">Loading strings...</p>
            </div>
        ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {Object.entries(strings).map(([key, value]) => (
                <div key={key} className="space-y-1">
                <label className="text-[10px] font-semibold text-slate-400 uppercase tracking-wider block ml-1">{key}</label>
                <input 
                    type="text"
                    className="w-full border border-slate-200 p-2.5 rounded text-sm outline-none focus:border-blue-500 bg-slate-50 focus:bg-white transition-all text-slate-800"
                    value={value}
                    onChange={(e) => setStrings({ ...strings, [key]: e.target.value })}
                />
                </div>
            ))}
            </div>
        )}
      </div>
    </AdminShell>
  );
}
