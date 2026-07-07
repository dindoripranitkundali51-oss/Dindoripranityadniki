"use client";
import { useState, useEffect, useCallback } from "react";
import AdminShell from "@/components/AdminShell";
import { ActionNotice } from "@/components/ActionDialog";
import { FileText, Save, Shield, Info, Gavel, Loader2 } from "lucide-react";
import { callAdminApi } from "@/lib/apiClient";

export default function LegalManagement() {
  const [activeTab, setActiveTab] = useState("privacy");
  const [content, setContent] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState(null);

  const fetchContent = useCallback(async () => {
    setLoading(true);
    try {
      const baseUrl = process.env.NEXT_PUBLIC_API_URL || "https://dindoripranitapi.somee.com/api/v1";
      const token = typeof window !== "undefined" ? localStorage.getItem("jwt_auth_token") || "" : "";
      const res = await fetch(`${baseUrl}/admin/legal/${activeTab}`, {
        headers: {
          "Authorization": token ? `Bearer ${token}` : ""
        }
      });
      if (res.ok) {
        const data = await res.json();
        if (data.success) {
          setContent(data.content || "");
        }
      }
    } catch (e) { console.error(e); }
    setLoading(false);
  }, [activeTab]);

  useEffect(() => {
    fetchContent();
  }, [fetchContent]);

  const handleSave = async () => {
    setSaving(true);
    try {
      await callAdminApi("saveLegal", {
        doc: activeTab,
        content: content
      });
      setNotice({ tone: "success", message: "Document updated successfully." });
    } catch (e) {
      setNotice({ tone: "danger", message: e.message || "Document update failed." });
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
          <h1 className="text-2xl font-bold text-slate-800">Legal Management</h1>
          <p className="text-slate-500 text-sm">Compliance & Public Information</p>
        </div>
        <button 
          onClick={handleSave} 
          disabled={saving}
          className="bg-blue-600 text-white px-6 py-2 rounded text-sm font-medium hover:bg-blue-700 transition-colors flex items-center gap-2"
        >
          {saving ? <Loader2 className="animate-spin" size={18} /> : <Save size={18} />} Save Document
        </button>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        <div className="space-y-1">
          {[
            { id: "privacy", label: "Privacy Policy", icon: Shield },
            { id: "terms", label: "Terms & Conditions", icon: Gavel },
            { id: "about", label: "About Us", icon: Info }
          ].map((item) => (
            <button
              key={item.id}
              onClick={() => setActiveTab(item.id)}
              className={`w-full flex items-center gap-3 p-3 rounded text-sm font-medium transition-colors ${activeTab === item.id ? "bg-blue-50 text-blue-600 border border-blue-100" : "bg-white text-slate-600 border border-slate-200 hover:bg-slate-50"}`}
            >
              <item.icon size={18} /> {item.label}
            </button>
          ))}
        </div>

        <div className="lg:col-span-3">
          <div className="bg-white rounded-lg shadow-sm border border-slate-200 p-6 min-h-[500px] flex flex-col">
            <div className="flex items-center gap-2 mb-4 border-b pb-3 text-blue-600">
                <FileText size={18} />
                <h3 className="font-semibold text-slate-800 text-sm">Editing Content</h3>
            </div>
            {loading ? (
              <div className="flex-1 flex items-center justify-center text-slate-400">
                <Loader2 className="animate-spin mr-2 text-blue-600" /> Fetching content...
              </div>
            ) : (
              <textarea
                className="flex-1 w-full bg-slate-50 p-4 rounded border border-slate-200 text-sm outline-none focus:border-blue-500 focus:bg-white transition-all resize-none leading-relaxed text-slate-700"
                placeholder="Write the content here..."
                value={content}
                onChange={(e) => setContent(e.target.value)}
              />
            )}
            <p className="text-[10px] text-slate-400 mt-4 italic text-center">Note: Changes will reflect in the app immediately after saving.</p>
          </div>
        </div>
      </div>
    </AdminShell>
  );
}
