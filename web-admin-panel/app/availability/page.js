"use client";
import { useMemo, useState } from "react";
import AdminShell from "@/components/AdminShell";
import { useApiCollection } from "@/lib/useApiCollection";
import {
  Calendar as CalendarIcon,
  Search,
  Clock,
  Loader2,
  AlertCircle,
  UserCheck,
  Crosshair,
  MapPinned,
} from "lucide-react";

export default function AvailabilityCalendar() {
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split("T")[0]);

  const { rows: gurujis, loading: loadingG, error: errorG } = useApiCollection(null, "availability-guruji");
  const { rows: bookings, loading: loadingB, error: errorB } = useApiCollection(null, "availability-bookings");

  const activeBookingsByGuruji = useMemo(() => {
    const map = new Map();
    bookings
      .filter((booking) => !["Cancelled", "Rejected"].includes(String(booking.status || "")) && booking.gurujiId)
      .forEach((booking) => {
        map.set(booking.gurujiId, booking);
      });
    return map;
  }, [bookings]);

  const availabilityData = useMemo(() => {
    return gurujis
      .map((guruji) => {
        const activeBooking = activeBookingsByGuruji.get(guruji.id) || null;
        const lat = firstNumber(guruji.lastLat, guruji.lat);
        const lng = firstNumber(guruji.lastLng, guruji.lng);
        const bookingLat = firstNumber(activeBooking?.userLat, activeBooking?.lat);
        const bookingLng = firstNumber(activeBooking?.userLng, activeBooking?.lng);
        const distanceKm =
          isValidCoordinate(lat, lng) && isValidCoordinate(bookingLat, bookingLng)
            ? haversineKm(lat, lng, bookingLat, bookingLng)
            : null;

        return {
          ...guruji,
          lat,
          lng,
          activeBooking,
          isAvailable: !activeBooking,
          distanceKm,
        };
      })
      .filter((guruji) => {
        const haystack = `${guruji.fullName || ""} ${guruji.mobile || ""} ${guruji.email || ""}`.toLowerCase();
        return haystack.includes(searchTerm.toLowerCase());
      })
      .sort((a, b) => {
        if (a.isAvailable !== b.isAvailable) return a.isAvailable ? -1 : 1;
        if (a.distanceKm == null && b.distanceKm != null) return 1;
        if (a.distanceKm != null && b.distanceKm == null) return -1;
        if (a.distanceKm != null && b.distanceKm != null) return a.distanceKm - b.distanceKm;
        return String(a.fullName || "").localeCompare(String(b.fullName || ""), "en", { sensitivity: "base" });
      });
  }, [gurujis, activeBookingsByGuruji, searchTerm]);

  const stats = useMemo(() => {
    const total = availabilityData.length;
    const available = availabilityData.filter((guruji) => guruji.isAvailable).length;
    return { total, available, busy: total - available };
  }, [availabilityData]);

  return (
    <AdminShell className="p-6" requiredCapability="guruji:write">
      <header className="mb-6 flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">Availability Center</h1>
          <p className="text-slate-500 text-sm">Location-first guruji availability using live latitude/longitude records.</p>
        </div>
        <div className="flex items-center gap-3 bg-white p-2 px-4 rounded border border-slate-300 shadow-sm">
          <CalendarIcon className="text-blue-600" size={18} />
          <input
            type="date"
            className="text-sm font-semibold text-slate-700 outline-none bg-transparent cursor-pointer"
            value={selectedDate}
            onChange={(e) => setSelectedDate(e.target.value)}
          />
        </div>
      </header>

      {(errorG || errorB) && (
        <div className="mb-6 p-3 bg-red-50 border border-red-200 rounded text-red-700 text-xs flex items-center gap-2">
          <AlertCircle size={16} /> Error syncing availability data.
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
        <div className="relative md:col-span-2">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
          <input
            type="text"
            placeholder="Search by guruji name, mobile, or email..."
            className="w-full border border-slate-300 p-2 pl-10 rounded text-sm outline-none focus:border-blue-500 bg-white"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
        <div className="bg-green-50 border border-green-100 p-3 rounded flex items-center gap-3">
          <UserCheck className="text-green-600" size={18} />
          <div>
            <p className="text-[10px] font-bold text-green-600/70 uppercase">Available</p>
            <h3 className="text-lg font-bold text-green-700">{stats.available}</h3>
          </div>
        </div>
        <div className="bg-amber-50 border border-amber-100 p-3 rounded flex items-center gap-3">
          <Clock className="text-amber-600" size={18} />
          <div>
            <p className="text-[10px] font-bold text-amber-600/70 uppercase">Booked</p>
            <h3 className="text-lg font-bold text-amber-700">{stats.busy}</h3>
          </div>
        </div>
      </div>

      <div className="mb-6 rounded-lg border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700">
        District has been removed from this controller. Cards now show live coordinates and active booking distance wherever both sides have valid lat/long.
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
        {loadingG || loadingB ? (
          <div className="col-span-full py-20 text-center text-slate-400">
            <Loader2 className="animate-spin mx-auto mb-2 text-blue-600" />
            <p className="text-sm italic">Checking location-based availability...</p>
          </div>
        ) : availabilityData.length === 0 ? (
          <div className="col-span-full py-20 text-center border border-dashed rounded bg-slate-50 text-slate-500">
            <p className="text-sm">No matching guruji found for this search or date.</p>
          </div>
        ) : (
          availabilityData.map((guruji) => (
            <div key={guruji.id} className="bg-white p-5 rounded-lg border border-slate-200 shadow-sm hover:border-blue-500 transition-all flex flex-col">
              <div className="flex justify-between items-start mb-4">
                <div className="w-10 h-10 bg-slate-50 rounded text-blue-600 flex items-center justify-center font-bold border border-slate-100">
                  {guruji.fullName?.[0]}
                </div>
                <div className={`flex items-center gap-1.5 px-2 py-0.5 rounded text-[10px] font-bold uppercase border ${
                  guruji.isAvailable ? "bg-green-50 text-green-600 border-green-100" : "bg-red-50 text-red-600 border-red-100"
                }`}>
                  {guruji.isAvailable ? "Available" : "Booked"}
                </div>
              </div>

              <h4 className="font-bold text-slate-800 text-sm mb-1">{guruji.fullName || "Guruji"}</h4>
              <p className="text-xs text-slate-500">{guruji.mobile || guruji.email || "No contact info"}</p>

              <div className="space-y-3 mt-4 pt-3 border-t border-slate-100">
                <div className="flex items-start gap-2 text-xs text-slate-600">
                  <Crosshair size={14} className="text-slate-400 mt-0.5" />
                  <div>
                    <p className="font-semibold text-slate-700">Live Coordinates</p>
                    <p>{formatCoordinatePair(guruji.lat, guruji.lng)}</p>
                  </div>
                </div>

                <div className="flex items-start gap-2 text-xs text-slate-600">
                  <MapPinned size={14} className="text-slate-400 mt-0.5" />
                  <div>
                    <p className="font-semibold text-slate-700">Assignment Distance</p>
                    <p>{guruji.distanceKm != null ? `${guruji.distanceKm.toFixed(2)} km from yajman` : guruji.activeBooking ? "Waiting booking coordinates" : "No active booking"}</p>
                  </div>
                </div>

                {guruji.activeBooking ? (
                  <div className="bg-amber-50 p-2 rounded border border-amber-100">
                    <p className="text-[10px] font-bold uppercase text-amber-700">Active Booking</p>
                    <p className="mt-1 text-xs text-amber-800">{guruji.activeBooking.poojaName || "Booking"} | {guruji.activeBooking.bookingId || guruji.activeBooking.id || "-"}</p>
                  </div>
                ) : (
                  <div className="bg-green-50 p-2 rounded border border-green-100">
                    <p className="text-[10px] font-bold uppercase text-green-700">Open For Auto Assignment</p>
                    <p className="mt-1 text-xs text-green-800">No active booking is holding this guruji on {selectedDate}.</p>
                  </div>
                )}
              </div>
            </div>
          ))
        )}
      </div>
    </AdminShell>
  );
}

function firstNumber(...values) {
  for (const value of values) {
    const num = Number(value);
    if (Number.isFinite(num) && num !== 0) return num;
  }
  return null;
}

function isValidCoordinate(lat, lng) {
  return Number.isFinite(lat) && Number.isFinite(lng) && lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
}

function formatCoordinatePair(lat, lng) {
  if (!isValidCoordinate(lat, lng)) return "No valid lat/long synced";
  return `${lat.toFixed(6)}, ${lng.toFixed(6)}`;
}

function haversineKm(lat1, lng1, lat2, lng2) {
  const R = 6371;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLng = ((lng2 - lng1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLng / 2) *
      Math.sin(dLng / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}
