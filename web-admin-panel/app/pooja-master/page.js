"use client";
import Image from "next/image";
import { useState } from "react";
import { uploadPublicPoojaImage } from "@/lib/storageClient";
import AdminShell from "@/components/AdminShell";
import { useApiCollection } from "@/lib/useApiCollection";
import { logAdminAction } from "@/lib/audit";
import { callAdminApi } from "@/lib/apiClient";
import { Plus, X, Search, Loader2, Edit2, Trash2 } from "lucide-react";
import { useLanguage } from "@/context/LanguageContext";

export default function PoojaMasterManagement() {
  const { t } = useLanguage();
  const { rows: poojas, loading } = useApiCollection(null, "admin-pooja-master");
  
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [editingPooja, setEditingPooja] = useState(null);
  const [error, setError] = useState(null);
  
  const [search, setSearch] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("All");

  const [formData, setFormData] = useState({
    name: "", nameEn: "", description: "", imageUrl: "", status: "Active", category: "General"
  });

  const filteredPoojas = poojas.filter((p) => {
    if (p.status === "Deleted") return false;
    const matchesSearch = (p.name?.toLowerCase() || "").includes(search.toLowerCase()) || 
                          (p.nameEn?.toLowerCase() || "").includes(search.toLowerCase());
    const matchesCat = categoryFilter === "All" || p.category === categoryFilter;
    return matchesSearch && matchesCat;
  });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setActionLoading(true);
    setError(null);
    try {
        if (editingPooja) {
          await callAdminApi("updatePooja", {
            id: editingPooja.id,
            name: formData.name,
            category: formData.category,
            basePrice: editingPooja.basePrice || 0,
            sevaType: editingPooja.sevaType || "Yadnyiki",
            status: formData.status
          });
        } else {
          const poojaId = formData.nameEn ? formData.nameEn.toLowerCase().replace(/[^a-z0-9]+/g, '_') : `pooja_${Date.now()}`;
          await callAdminApi("createPooja", {
            id: poojaId,
            name: formData.name,
            category: formData.category,
            basePrice: 0,
            sevaType: "Yadnyiki",
            status: formData.status
          });
        }
        closeModal();
        window.location.reload();
    } catch (err) { 
        setError(err.message || t("save_failed")); 
    } finally { 
        setActionLoading(false); 
    }
  };

  const handleDelete = async (poojaId) => {
    if (!window.confirm("Are you sure you want to delete this Pooja?")) return;
    try {
      await callAdminApi("deletePooja", { id: poojaId });
      window.location.reload();
    } catch (err) {
      alert("Failed to delete Pooja: " + err.message);
    }
  };

  const openModal = (pooja = null) => {
    if (pooja) {
        setEditingPooja(pooja);
        setFormData({
            name: pooja.name || "", nameEn: pooja.nameEn || "", description: pooja.description || "",
            imageUrl: pooja.imageUrl || "", status: pooja.status || "Active",
            category: pooja.category || "General"
        });
    } else {
        setEditingPooja(null);
        setFormData({ name: "", nameEn: "", description: "", imageUrl: "", status: "Active", category: "General" });
    }
    setIsModalOpen(true);
  };

  const closeModal = () => { setIsModalOpen(false); setEditingPooja(null); };

  const handleImageUpload = async (event) => {
    const file = event.target.files?.[0];
    if (!file) return;
    setUploading(true);
    setError(null);
    try {
      const imageUrl = await uploadPublicPoojaImage(file);
      setFormData((prev) => ({ ...prev, imageUrl }));
    } catch (err) {
      setError("Image upload failed.");
    } finally {
      setUploading(false);
      event.target.value = "";
    }
  };

  return (
    <AdminShell className="p-6" requiredCapability="config:write">
        <header className="flex flex-col md:flex-row justify-between items-start md:items-center mb-6 gap-4">
          <div>
             <h1 className="text-2xl font-bold text-slate-800">{t("pooja_master")}</h1>
             <p className="text-slate-500 text-sm">{t("manage_catalog")}</p>
          </div>
          <button onClick={() => openModal()} className="bg-blue-600 text-white px-4 py-2 rounded text-sm font-medium hover:bg-blue-700 flex items-center gap-2 transition-colors">
            <Plus size={18} /> {t("add_new_pooja")}
          </button>
        </header>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
            <div className="relative md:col-span-2">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
                <input className="w-full border border-slate-300 p-2 pl-10 rounded text-sm outline-none focus:border-blue-500" placeholder={t("search")} value={search} onChange={(e) => setSearch(e.target.value)} />
            </div>
            <select className="border border-slate-300 p-2 rounded text-sm bg-white" value={categoryFilter} onChange={(e) => setCategoryFilter(e.target.value)}>
                <option value="All">{t("all_categories")}</option>
            </select>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {loading ? (
              <div className="col-span-full p-20 text-center text-slate-500">{t("loading")}</div>
          ) : filteredPoojas.map((pooja) => (
            <div key={pooja.id} className="bg-white rounded-lg border border-slate-200 shadow-sm p-5 flex flex-col">
              <div className="flex items-start gap-3 mb-3">
                  <div className="h-16 w-16 shrink-0 overflow-hidden rounded-lg border border-slate-200 bg-slate-50">
                    {pooja.imageUrl ? (
                      <Image src={pooja.imageUrl} alt={pooja.name || "Pooja"} width={64} height={64} className="h-full w-full object-cover" unoptimized />
                    ) : (
                      <div className="flex h-full w-full items-center justify-center text-xs font-semibold text-slate-400">No photo</div>
                    )}
                  </div>
                  <div className="min-w-0 flex-1">
                    <h3 className="font-bold text-slate-800 text-base">{pooja.name}</h3>
                    <p className="mt-1 text-xs uppercase tracking-wide text-slate-400">{pooja.category || "General"}</p>
                    <p className="mt-1 text-xs font-semibold text-green-700">{pooja.status || "Active"}</p>
                  </div>
              </div>
              <p className="text-sm text-slate-500 line-clamp-2 mb-4 flex-1">{pooja.description || "No description"}</p>
              <div className="flex gap-4 border-t pt-3">
                  <button onClick={() => openModal(pooja)} className="text-sm text-blue-600 font-medium hover:underline flex items-center gap-1">
                      <Edit2 size={14}/> {t("edit")}
                  </button>
                  <button onClick={() => handleDelete(pooja.id)} className="text-sm text-red-600 font-medium hover:underline flex items-center gap-1">
                      <Trash2 size={14}/> {t("delete")}
                  </button>
              </div>
            </div>
          ))}
        </div>
        {isModalOpen && (
          <div className="fixed inset-0 bg-black/50 z-[100] flex items-center justify-center p-4">
            <div className="bg-white rounded-lg w-full max-w-md p-6 shadow-xl max-h-[90vh] overflow-y-auto">
                <div className="flex justify-between items-center mb-6">
                    <h2 className="text-lg font-bold">{editingPooja ? t("edit") : t("add_new_pooja")}</h2>
                    <button onClick={closeModal}><X size={20} className="text-slate-400"/></button>
                </div>
                <form onSubmit={handleSubmit} className="space-y-4">
                    <div className="space-y-1">
                        <label className="text-xs font-semibold text-slate-600 uppercase tracking-wider">{t("name")}</label>
                        <input type="text" required className="w-full p-2 border border-slate-300 rounded text-sm focus:border-blue-500 outline-none" value={formData.name} onChange={e => setFormData({...formData, name: e.target.value})} />
                    </div>
                    <div className="space-y-1">
                        <label className="text-xs font-semibold text-slate-600 uppercase tracking-wider">English Name</label>
                        <input type="text" className="w-full p-2 border border-slate-300 rounded text-sm focus:border-blue-500 outline-none" value={formData.nameEn} onChange={e => setFormData({...formData, nameEn: e.target.value})} />
                    </div>
                    <div className="space-y-1">
                        <label className="text-xs font-semibold text-slate-600 uppercase tracking-wider">Category</label>
                        <input type="text" className="w-full p-2 border border-slate-300 rounded text-sm focus:border-blue-500 outline-none" value={formData.category} onChange={e => setFormData({...formData, category: e.target.value})} />
                    </div>
                    <div className="space-y-1">
                        <label className="text-xs font-semibold text-slate-600 uppercase tracking-wider">Description</label>
                        <textarea className="w-full p-2 border border-slate-300 rounded text-sm focus:border-blue-500 outline-none min-h-[110px]" value={formData.description} onChange={e => setFormData({...formData, description: e.target.value})} />
                    </div>
                    <div className="space-y-2">
                        <label className="text-xs font-semibold text-slate-600 uppercase tracking-wider">Photo</label>
                        {formData.imageUrl ? (
                          <div className="overflow-hidden rounded-lg border border-slate-200">
                            <Image src={formData.imageUrl} alt={formData.name || "Pooja"} width={640} height={240} className="h-40 w-full object-cover" unoptimized />
                          </div>
                        ) : (
                          <div className="rounded-lg border border-dashed border-slate-300 bg-slate-50 px-3 py-6 text-center text-xs text-slate-400">
                            No photo uploaded yet.
                          </div>
                        )}
                        <input type="file" accept="image/*" onChange={handleImageUpload} className="w-full text-sm" />
                        {uploading && <p className="text-xs text-blue-600">Uploading image...</p>}
                    </div>
                    <div className="space-y-1">
                        <label className="text-xs font-semibold text-slate-600 uppercase tracking-wider">Status</label>
                        <select className="w-full p-2 border border-slate-300 rounded text-sm bg-white focus:border-blue-500 outline-none" value={formData.status} onChange={e => setFormData({...formData, status: e.target.value})}>
                          <option value="Active">Active</option>
                          <option value="Inactive">Inactive</option>
                        </select>
                    </div>
                    <div className="rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-800">
                      Base price field intentionally removed. Final payment amount is captured after seva completion.
                    </div>
                    <button type="submit" disabled={actionLoading} className="w-full bg-blue-600 text-white py-2.5 rounded font-medium hover:bg-blue-700 transition-colors mt-4">
                        {actionLoading ? t("loading") : t("save")}
                    </button>
                </form>
            </div>
          </div>
        )}
    </AdminShell>
  );
}

