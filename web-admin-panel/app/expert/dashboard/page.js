"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Clock, Calendar, CheckCircle2, DollarSign, Power, FileText, User, MapPin, Loader2, LogOut } from "lucide-react";

export default function ExpertDashboard() {
  const [profile, setProfile] = useState(null);
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState(false);
  const [isAvailable, setIsAvailable] = useState(false);
  const router = useRouter();

  useEffect(() => {
    const token = localStorage.getItem("jwt_auth_token");
    const cachedProfile = localStorage.getItem("expert_profile");

    if (!token || !cachedProfile) {
      router.push("/expert/login");
      return;
    }

    const parsedProfile = JSON.parse(cachedProfile);
    setProfile(parsedProfile);
    setIsAvailable(parsedProfile.IsAvailable || false);

    fetchBookings(token);
  }, [router]);

  const fetchBookings = async (token) => {
    try {
      const baseUrl = process.env.NEXT_PUBLIC_API_URL || "https://dindoritrial.somee.com/api/v1";
      const res = await fetch(`${baseUrl}/booking/expert`, {
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

  const toggleAvailability = async () => {
    setUpdating(true);
    try {
      const token = localStorage.getItem("jwt_auth_token");
      const baseUrl = process.env.NEXT_PUBLIC_API_URL || "https://dindoritrial.somee.com/api/v1";
      
      // Toggle availability state
      const nextState = !isAvailable;
      const res = await fetch(`${baseUrl}/booking/expert/availability`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${token}`
        },
        body: JSON.stringify({ isAvailable: nextState })
      });

      if (res.ok) {
        setIsAvailable(nextState);
        const updatedProfile = { ...profile, IsAvailable: nextState };
        localStorage.setItem("expert_profile", JSON.stringify(updatedProfile));
      }
    } catch (err) {
      console.error("Failed to update availability status", err);
    } finally {
      setUpdating(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem("jwt_auth_token");
    localStorage.removeItem("expert_profile");
    router.push("/expert/login");
  };

  if (loading || !profile) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-900 text-white">
        <Loader2 className="animate-spin text-amber-500 mr-2" /> Loading Expert Portal...
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 p-6 font-sans">
      <header className="flex flex-col md:flex-row justify-between items-start md:items-center border-b border-slate-800 pb-6 mb-8 gap-4">
        <div>
          <span className="bg-amber-500/10 text-amber-500 text-xs font-bold px-3 py-1 rounded-full uppercase tracking-wider">
            {profile.ExpertType || "Guruji"} Portal
          </span>
          <h1 className="text-3xl font-black text-white mt-2">नमस्कार, {profile.FullName}</h1>
          <p className="text-slate-400 text-sm mt-1">{profile.Address}, {profile.District}</p>
        </div>

        <div className="flex items-center gap-3">
          <button 
            onClick={toggleAvailability}
            disabled={updating}
            className={`flex items-center gap-2 px-5 py-3 rounded-2xl font-bold text-xs uppercase tracking-wider transition-all shadow-lg ${
              isAvailable 
                ? "bg-green-600/20 text-green-400 border border-green-500/30" 
                : "bg-red-600/20 text-red-400 border border-red-500/30"
            }`}
          >
            <Power size={16} />
            {isAvailable ? "Available (शुरू)" : "Offline (बंद)"}
          </button>

          <button 
            onClick={handleLogout}
            className="bg-slate-800 hover:bg-slate-700 text-slate-300 p-3 rounded-2xl"
          >
            <LogOut size={18} />
          </button>
        </div>
      </header>

      {/* Metrics Row */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 shadow-md flex items-center gap-4">
          <div className="bg-amber-500/10 p-4 rounded-2xl text-amber-500">
            <DollarSign size={24} />
          </div>
          <div>
            <p className="text-xs text-slate-400 font-bold uppercase tracking-wider">Wallet Balance</p>
            <h2 className="text-2xl font-black text-white mt-1">₹ {profile.WalletBalance?.toFixed(2) || "0.00"}</h2>
          </div>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 shadow-md flex items-center gap-4">
          <div className="bg-green-500/10 p-4 rounded-2xl text-green-500">
            <CheckCircle2 size={24} />
          </div>
          <div>
            <p className="text-xs text-slate-400 font-bold uppercase tracking-wider">Total Earnings</p>
            <h2 className="text-2xl font-black text-white mt-1">₹ {profile.TotalEarnings?.toFixed(2) || "0.00"}</h2>
          </div>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-3xl p-6 shadow-md flex items-center gap-4">
          <div className="bg-blue-500/10 p-4 rounded-2xl text-blue-500">
            <Calendar size={24} />
          </div>
          <div>
            <p className="text-xs text-slate-400 font-bold uppercase tracking-wider">Total Bookings</p>
            <h2 className="text-2xl font-black text-white mt-1">{bookings.length}</h2>
          </div>
        </div>
      </div>

      {/* Bookings Section */}
      <div className="bg-slate-900 border border-slate-800 rounded-[35px] p-8 shadow-xl">
        <h3 className="text-lg font-black text-white uppercase tracking-wider mb-6 flex items-center gap-2">
          <Clock className="text-amber-500" size={18} /> आगामी पूजा आणि विधी (Upcoming Bookings)
        </h3>

        {bookings.length === 0 ? (
          <div className="py-20 text-center text-slate-500">
            <p className="text-sm">सध्या कोणतेही सक्रिय बुकिंग नाही.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {bookings.map((booking) => (
              <div 
                key={booking.id} 
                className="bg-slate-950 border border-slate-800/80 p-6 rounded-3xl flex flex-col justify-between hover:border-amber-500/40 transition-all"
              >
                <div>
                  <div className="flex justify-between items-start mb-4">
                    <span className="bg-slate-800 text-slate-300 text-[10px] font-mono px-2 py-1 rounded">
                      #{booking.displayId}
                    </span>
                    <span className={`text-[10px] font-bold px-2 py-1 rounded uppercase tracking-wider ${
                      booking.status === "Accepted" ? "bg-green-500/10 text-green-400" :
                      booking.status === "InProgress" ? "bg-blue-500/10 text-blue-400" :
                      "bg-amber-500/10 text-amber-400"
                    }`}>
                      {booking.status}
                    </span>
                  </div>

                  <h4 className="text-base font-bold text-white mb-2">{booking.poojaName || "Pooja/Seva"}</h4>
                  
                  <div className="space-y-2 mt-4 text-xs text-slate-400">
                    <div className="flex items-center gap-2"><User size={14} className="text-amber-500" /> यजमान: {booking.contactName}</div>
                    <div className="flex items-center gap-2"><Clock size={14} className="text-amber-500" /> तारीख: {booking.date}</div>
                    <div className="flex items-center gap-2"><MapPin size={14} className="text-amber-500" /> पत्ता: {booking.address}</div>
                  </div>
                </div>

                <div className="mt-6 pt-4 border-t border-slate-800/60 flex justify-between items-center">
                  <div>
                    <p className="text-[10px] text-slate-500 uppercase tracking-wider">Dakshina</p>
                    <p className="text-sm font-bold text-white">₹ {booking.amount}</p>
                  </div>
                  
                  {booking.paymentStatus === "Paid" && (
                    <button className="flex items-center gap-1 text-xs font-bold text-amber-500 hover:text-amber-400">
                      <FileText size={14} /> Receipt
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
