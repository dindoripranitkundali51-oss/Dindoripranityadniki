"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Clock, Calendar, CheckCircle, IndianRupee, Plus, FileText, MessageSquare, LogOut, ArrowRight, Loader2, MapPin } from "lucide-react";

export default function YajmanBookings() {
  const [profile, setProfile] = useState(null);
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const router = useRouter();

  useEffect(() => {
    const token = localStorage.getItem("jwt_auth_token");
    const cachedProfile = localStorage.getItem("yajman_profile");

    if (!token || !cachedProfile) {
      router.push("/yajman/login");
      return;
    }

    setProfile(JSON.parse(cachedProfile));
    fetchBookings(token);
  }, [router]);

  const fetchBookings = async (token) => {
    try {
      const baseUrl = process.env.NEXT_PUBLIC_API_URL || "https://dindoritrial.somee.com/api/v1";
      const res = await fetch(`${baseUrl}/booking/user`, {
        headers: { "Authorization": `Bearer ${token}` }
      });
      if (!res.ok) throw new Error("Failed to fetch bookings.");
      const data = await res.json();
      setBookings(data.data || []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem("jwt_auth_token");
    localStorage.removeItem("yajman_profile");
    router.push("/yajman/login");
  };

  if (loading || !profile) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-orange-50/50 text-slate-600">
        <Loader2 className="animate-spin text-orange-600 mr-2" /> Loading Yajman Portal...
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-50 text-slate-700 p-6 font-sans">
      <header className="flex flex-col md:flex-row justify-between items-start md:items-center border-b border-slate-200 pb-6 mb-8 gap-4">
        <div>
          <span className="bg-orange-100 text-orange-700 text-xs font-bold px-3 py-1 rounded-full uppercase tracking-wider">
            Yajman Portal
          </span>
          <h1 className="text-3xl font-black text-slate-800 mt-2">हरि ॐ, {profile.FullName}</h1>
          <p className="text-slate-500 text-sm mt-1">{profile.Address}, {profile.District}</p>
        </div>

        <div className="flex items-center gap-3">
          <Link href="/yajman/book" className="bg-orange-600 hover:bg-orange-700 text-white px-5 py-3 rounded-2xl font-bold text-xs uppercase tracking-wider flex items-center gap-2 shadow-lg shadow-orange-600/10">
            <Plus size={16} /> नवीन सेवा बुक करा (Book Seva)
          </Link>

          <button 
            onClick={handleLogout}
            className="bg-white border border-slate-200 hover:bg-slate-50 text-slate-600 p-3 rounded-2xl"
          >
            <LogOut size={18} />
          </button>
        </div>
      </header>

      {/* Bookings Container */}
      <div className="max-w-5xl mx-auto bg-white border border-slate-200 rounded-[35px] p-8 shadow-sm">
        <h3 className="text-lg font-black text-slate-800 uppercase tracking-wider mb-6 flex items-center gap-2">
          <Clock className="text-orange-600" size={18} /> माझे बुकिंग डिटेल्स (My Bookings)
        </h3>

        {bookings.length === 0 ? (
          <div className="py-20 text-center text-slate-400">
            <p className="text-sm mb-4">तुम्ही अद्याप कोणतीही पूजा बुक केलेली नाही.</p>
            <Link href="/yajman/book" className="text-orange-600 font-bold hover:underline">
              नवीन सेवा बुक करण्यासाठी इथे क्लिक करा →
            </Link>
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-6">
            {bookings.map((booking) => (
              <div 
                key={booking.id} 
                className="bg-slate-50 border border-slate-200 p-6 rounded-3xl flex flex-col md:flex-row justify-between items-start md:items-center gap-6"
              >
                <div>
                  <div className="flex items-center gap-3 mb-2">
                    <span className="bg-white border border-slate-200 text-slate-600 text-[10px] font-mono px-2 py-0.5 rounded">
                      #{booking.displayId}
                    </span>
                    <span className={`text-[10px] font-bold px-2 py-0.5 rounded uppercase tracking-wider ${
                      booking.status === "Completed" ? "bg-green-100 text-green-700" :
                      booking.status === "PaymentPending" ? "bg-red-100 text-red-700" :
                      "bg-orange-100 text-orange-700"
                    }`}>
                      {booking.status}
                    </span>
                  </div>

                  <h4 className="text-base font-bold text-slate-800">{booking.poojaName || "Pooja/Seva"}</h4>
                  
                  <div className="flex flex-wrap gap-x-6 gap-y-2 mt-3 text-xs text-slate-500">
                    <div className="flex items-center gap-1.5"><Calendar size={14} className="text-orange-500" /> {booking.date}</div>
                    {booking.gurujiName && (
                      <div className="flex items-center gap-1.5"><MapPin size={14} className="text-orange-500" /> गुरुजी: {booking.gurujiName}</div>
                    )}
                  </div>
                </div>

                <div className="flex items-center gap-4 w-full md:w-auto justify-between md:justify-end border-t md:border-t-0 pt-4 md:pt-0 border-slate-200">
                  <div>
                    <p className="text-[10px] text-slate-400 uppercase tracking-wider">Amount (दक्षिणा)</p>
                    <p className="text-sm font-black text-slate-800">₹ {booking.amount}</p>
                  </div>

                  <div className="flex items-center gap-2">
                    {booking.status === "PaymentPending" && (
                      <Link 
                        href={`/yajman/checkout?bookingId=${booking.id}`}
                        className="bg-orange-600 hover:bg-orange-700 text-white px-4 py-2 rounded-xl text-xs font-bold uppercase tracking-wider flex items-center gap-1.5"
                      >
                        पेमेंट करा <ArrowRight size={14} />
                      </Link>
                    )}

                    {booking.status === "Completed" && (
                      <Link 
                        href={`/yajman/feedback?bookingId=${booking.id}`}
                        className="bg-white border border-slate-200 text-slate-700 px-4 py-2 rounded-xl text-xs font-bold hover:bg-slate-50 flex items-center gap-1.5"
                      >
                        <MessageSquare size={14} /> अभिप्राय (Feedback)
                      </Link>
                    )}

                    {booking.paymentStatus === "Paid" && (
                      <span className="text-green-600 text-xs font-bold flex items-center gap-1">
                        <CheckCircle size={14} /> Paid
                      </span>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
