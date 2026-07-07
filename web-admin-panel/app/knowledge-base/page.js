"use client";

import { useState } from "react";
import { BookOpen, Loader2, PlusCircle, Save } from "lucide-react";
import AdminShell from "@/components/AdminShell";
import { useApiCollection } from "@/lib/useApiCollection";
import { callAdminApi } from "@/lib/apiClient";

const EMPTY_FORM = {
  question: "",
  answer: "",
  tags: "",
  status: "Published",
};

export default function KnowledgeBasePage() {
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState("");
  const { rows } = useApiCollection(null, "admin-kb");

  const saveEntry = async (event) => {
    event.preventDefault();
    setSaving(true);
    setNotice("");
    try {
      const tagsArr = form.tags.split(",").map(t => t.trim()).filter(Boolean);
      await callAdminApi("saveFaqEntry", {
        question: form.question,
        answer: form.answer,
        tags: tagsArr,
        status: form.status,
      });
      setForm(EMPTY_FORM);
      setNotice("Knowledge base entry saved.");
      setTimeout(() => window.location.reload(), 1000);
    } catch (err) {
      setNotice("Failed to save entry: " + err.message);
    } finally {
      setSaving(false);
    }
  };

  const toggleStatus = async (row) => {
    try {
      const newStatus = row.status === "Published" ? "Draft" : "Published";
      await callAdminApi("updateFaqStatus", {
        id: row.id,
        question: row.question,
        answer: row.answer,
        tags: row.tags,
        status: newStatus
      });
      window.location.reload();
    } catch (err) {
      alert("Failed to toggle status: " + err.message);
    }
  };

  return (
    <AdminShell className="p-6">
      <header className="mb-6">
        <h1 className="flex items-center gap-2 text-2xl font-bold text-slate-800"><BookOpen size={22} /> Auto Knowledge Base</h1>
        <p className="text-sm text-slate-500">FAQ, standard answers, and operator playbooks for support and admin actions.</p>
      </header>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-3">
        <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm xl:col-span-1">
          <h2 className="mb-4 flex items-center gap-2 font-semibold text-slate-800"><PlusCircle size={17} /> New FAQ</h2>
          {notice && <div className="mb-4 rounded border border-green-200 bg-green-50 px-3 py-2 text-sm text-green-700">{notice}</div>}
          <form className="space-y-4" onSubmit={saveEntry}>
            <Field label="Question">
              <input className="w-full rounded border p-2.5" value={form.question} onChange={(e) => setForm((prev) => ({ ...prev, question: e.target.value }))} required />
            </Field>
            <Field label="Answer">
              <textarea className="min-h-[160px] w-full rounded border p-2.5" value={form.answer} onChange={(e) => setForm((prev) => ({ ...prev, answer: e.target.value }))} required />
            </Field>
            <Field label="Tags (comma separated)">
              <input className="w-full rounded border p-2.5" value={form.tags} onChange={(e) => setForm((prev) => ({ ...prev, tags: e.target.value }))} />
            </Field>
            <Field label="Status">
              <select className="w-full rounded border p-2.5" value={form.status} onChange={(e) => setForm((prev) => ({ ...prev, status: e.target.value }))}>
                <option>Published</option>
                <option>Draft</option>
              </select>
            </Field>
            <button className="flex w-full items-center justify-center gap-2 rounded bg-blue-600 px-4 py-3 font-semibold text-white hover:bg-blue-700 disabled:opacity-70" disabled={saving}>
              {saving ? <Loader2 size={16} className="animate-spin" /> : <Save size={16} />}
              Save Entry
            </button>
          </form>
        </section>

        <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm xl:col-span-2">
          <h2 className="mb-4 font-semibold text-slate-800">Published And Draft Entries</h2>
          <div className="space-y-3">
            {rows.length === 0 ? (
              <p className="text-sm text-slate-400">No knowledge base entries yet.</p>
            ) : rows.map((row) => (
              <div key={row.id} className="rounded border border-slate-200 bg-slate-50 p-4">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="font-semibold text-slate-800">{row.question || "Untitled question"}</p>
                    <p className="mt-2 whitespace-pre-wrap text-sm text-slate-600">{row.answer || "-"}</p>
                  </div>
                  <button
                    onClick={() => toggleStatus(row)}
                    className={`rounded px-3 py-1 text-xs font-semibold ${row.status === "Published" ? "bg-green-100 text-green-700" : "bg-amber-100 text-amber-700"}`}
                  >
                    {row.status || "Draft"}
                  </button>
                </div>
                <div className="mt-3 flex flex-wrap gap-2">
                  {(row.tags || []).map((tag) => (
                    <span key={tag} className="rounded border border-slate-200 bg-white px-2 py-1 text-[11px] text-slate-500">{tag}</span>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </section>
      </div>
    </AdminShell>
  );
}

function Field({ label, children }) {
  return (
    <div className="space-y-1">
      <label className="text-xs font-semibold uppercase text-slate-500">{label}</label>
      {children}
    </div>
  );
}
