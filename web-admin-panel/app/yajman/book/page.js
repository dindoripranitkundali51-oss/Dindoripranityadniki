"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { ArrowLeft, Loader2, Calendar, User, Phone, MapPin, CheckCircle } from "lucide-react";

export default function YajmanBookPooja() {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState("");
  const [poojas, setPoojas] = useState([
    { id: "pooja_ganesha", name: "गणपती पूजा (Ganapati Pooja)", price: 1500 },
    { id: "pooja_satyanarayan", name: "सत्यनारायण पूजा (Satyanarayan Pooja)", price: 2100 },
    { id: "pooja_vastu", name: "वास्तू शांत (Vastu Shanti Seva)", price: 5100 },
    { id: "pooja_rudrabhishek", name: "रुद्राभिषेक (Rudrabhishek)", price: 2500 }
  ]);

  const [form, setForm] = useState({
    poojaId: "pooja_ganesha",
    date: "",
    contactName: "",
    contactPhone: "",
    address: "",
    district: "Nashik",
    pincode: ""
  });

  useEffect(() => {
    const token = localStorage.getItem("jwt_auth_token");
    const cachedProfile = localStorage.getItem("yajman_profile");
    if (!token || !cachedProfile) {
      router.push("/yajman/login");
      return;
    }

    const profile = JSON.parse(cachedProfile);
    setForm(prev => ({
      ...prev,
      contactName: profile.FullName || "",
      contactPhone: profile.Mobile || "",
      address: profile.Address || "",
      district: profile.District || "Nashik",
      pincode: profile.Pincode || ""
    }));
  }, [router]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");

    try {
      const token = localStorage.getItem("jwt_auth_token");
      const baseUrl = process.env.NEXT_PUBLIC_API_URL || "https://dindoritrial.somee.com/api/v1";

      const payload = {
        clientRequestId: Guid(),
        poojaId: form.poojaId,
        date: new Date(form.date).toISOString(),
        contactName: form.contactName,
        contactPhone: form.contactPhone,
        address: form.address,
        district: form.district,
        pincode: form.pincode,
        userLat: 20.0017, // default Nashik coordinate
        userLng: 73.7898
      };

      const res = await fetch(`${baseUrl}/booking`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${token}`
        },
        body: JSON.stringify(payload)
      });

      if (!res.ok) {
        const errData = await res.json().catch(() => ({}));
        throw new Error(errData.message || "Failed to submit booking.");
      }

      setSuccess(true);
      setTimeout(() => {
        router.push("/yajman/bookings");
      }, 2000);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const Guid = () => {
    return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, function(c) {
      var r = (Math.random() * 16) | 0,
        v = c == "x" ? r : (r & 0x3) | 0x8;
      return v.toString(16);
    });
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-700 p-6 font-sans">
      <div className="max-w-2xl mx-auto">
        <Link href="/yajman/bookings" className="flex items-center gap-1.5 text-slate-500 hover:text-slate-800 text-xs font-bold uppercase tracking-wider mb-6">
          <ArrowLeft size={16} /> मागे जा (Back to Bookings)
        </Link>

        <div className="bg-white border border-slate-200 rounded-[35px] p-8 shadow-sm">
          <h2 className="text-2xl font-black text-slate-800 uppercase tracking-wide mb-2">नवीन सेवा बुक करा</h2>
          <p className="text-slate-500 text-sm mb-6">कृपया खालील सर्व माहिती अचूक भरा. सेवा मार्गाचे अधिकृत गुरुजी तुमच्या घरापर्यंत येतील.</p>

          {success ? (
            <div className="py-12 text-center text-green-600 space-y-4">
              <CheckCircle className="mx-auto" size={48} />
              <h3 className="text-xl font-bold">बुकिंग यशस्वी!</h3>
              <p className="text-sm text-slate-500">तुमच्या बुकिंगची विनंती यशस्वीरित्या नोंदवली गेली आहे.</p>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-5">
              {error && <div className="bg-red-50 text-red-600 text-xs p-3 rounded-xl">{error}</div>}

              <div>
                <label className="block text-xs font-bold uppercase text-slate-400 mb-2">पूजा / सेवा निवडा (Choose Seva)</label>
                <select 
                  className="w-full border border-slate-200 bg-slate-50 p-4 rounded-2xl text-sm outline-none focus:border-orange-500"
                  value={form.poojaId}
                  onChange={(e) => setForm({ ...form, poojaId: e.target.value })}
                >
                  {poojas.map(p => (
                    <option key={p.id} value={p.id}>{p.name} - ₹ {p.price}</option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold uppercase text-slate-400 mb-2">नाव (Name)</label>
                  <input 
                    type="text"
                    className="w-full border border-slate-200 bg-slate-50 p-4 rounded-2xl text-sm outline-none focus:border-orange-500"
                    value={form.contactName}
                    onChange={(e) => setForm({ ...form, contactName: e.target.value })}
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold uppercase text-slate-400 mb-2">मोबाईल (Mobile)</label>
                  <input 
                    type="text"
                    className="w-full border border-slate-200 bg-slate-50 p-4 rounded-2xl text-sm outline-none focus:border-orange-500"
                    value={form.contactPhone}
                    onChange={(e) => setForm({ ...form, contactPhone: e.target.value })}
                    required
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="md:col-span-2">
                  <label className="block text-xs font-bold uppercase text-slate-400 mb-2">दिनांक (Date)</label>
                  <input 
                    type="date"
                    className="w-full border border-slate-200 bg-slate-50 p-4 rounded-2xl text-sm outline-none focus:border-orange-500 text-slate-500"
                    value={form.date}
                    onChange={(e) => setForm({ ...form, date: e.target.value })}
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold uppercase text-slate-400 mb-2">पिनकोड (Pincode)</label>
                  <input 
                    type="text"
                    className="w-full border border-slate-200 bg-slate-50 p-4 rounded-2xl text-sm outline-none focus:border-orange-500"
                    value={form.pincode}
                    onChange={(e) => setForm({ ...form, pincode: e.target.value })}
                    required
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold uppercase text-slate-400 mb-2">पत्ता (Address)</label>
                <textarea 
                  className="w-full border border-slate-200 bg-slate-50 p-4 rounded-2xl text-sm outline-none focus:border-orange-500 h-24 resize-none"
                  value={form.address}
                  onChange={(e) => setForm({ ...form, address: e.target.value })}
                  required
                />
              </div>

              <button 
                type="submit" 
                disabled={loading}
                className="w-full bg-orange-600 hover:bg-orange-700 text-white py-4 rounded-2xl font-bold uppercase tracking-wider transition-all shadow-lg shadow-orange-600/10"
              >
                {loading ? <Loader2 className="animate-spin mx-auto text-white" size={20} /> : "बुकिंग निश्चित करा"}
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
