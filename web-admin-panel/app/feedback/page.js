"use client";
import { useState } from "react";
import AdminShell from "@/components/AdminShell";
import { useApiCollection } from "@/lib/useApiCollection";
import { AlertCircle, Loader2, Star, User } from "lucide-react";

export default function FeedbackReview() {
  const [ratingFilter, setRatingFilter] = useState("All");
  const { rows: feedbacks, loading } = useApiCollection(null, "feedback-module");

  const filtered = feedbacks.filter((entry) => ratingFilter === "All" || Math.floor(entry.rating || 0) === Number(ratingFilter));

  return (
    <AdminShell className="p-6" requiredCapability="support:read">
      <header className="mb-6">
        <h1 className="text-2xl font-bold text-slate-800">User Feedback</h1>
        <p className="text-sm text-slate-500">Review service quality, booking context, and guruji ratings.</p>
      </header>

      <div className="mb-8 flex w-fit gap-1 overflow-x-auto rounded border border-slate-300 bg-white p-1">
        {["All", "5", "4", "3", "2", "1"].map((rating) => (
          <button
            key={rating}
            onClick={() => setRatingFilter(rating)}
            className={`rounded px-4 py-1.5 text-xs font-medium transition-colors ${
              ratingFilter === rating ? "bg-blue-600 text-white" : "text-slate-600 hover:bg-slate-50"
            }`}
          >
            {rating === "All" ? "All ratings" : `${rating} star`}
          </button>
        ))}
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
        {loading ? (
          <div className="col-span-full py-20 text-center text-slate-400">
            <Loader2 className="mx-auto mb-2 animate-spin text-blue-600" />
            <p className="text-sm italic">Loading feedback...</p>
          </div>
        ) : filtered.length === 0 ? (
          <div className="col-span-full rounded border border-dashed bg-slate-50 py-20 text-center text-slate-500">
            <p className="text-sm">No feedback found for this filter.</p>
          </div>
        ) : (
          filtered.map((entry) => (
            <div
              key={entry.id}
              className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm transition-colors hover:border-blue-500"
            >
              <div className="mb-4 flex items-start justify-between">
                <div className="flex items-center gap-3">
                  <div className="flex h-9 w-9 items-center justify-center rounded border border-blue-100 bg-blue-50 text-xs font-bold text-blue-600">
                    {entry.userName?.[0] || <User size={14} />}
                  </div>
                  <div>
                    <h4 className="text-sm font-bold text-slate-800">{entry.userName || "Unknown user"}</h4>
                    <p className="text-xs text-slate-400">{formatDate(entry.createdAt || entry.date) || "Recent"}</p>
                  </div>
                </div>
                <div className="flex items-center gap-1 rounded border border-amber-100 bg-amber-50 px-2 py-0.5">
                  <Star size={12} className="fill-amber-500 text-amber-500" />
                  <span className="text-xs font-bold text-amber-700">{entry.rating ?? "-"}</span>
                </div>
              </div>

              <div className="mb-4 rounded border border-slate-100 bg-slate-50 p-3">
                <p className="text-sm italic text-slate-600">&ldquo;{entry.review || entry.comment || "No comment."}&rdquo;</p>
              </div>

              <div className="flex items-center justify-between border-t border-slate-100 pt-3">
                <div>
                  <span className="block text-xs font-semibold text-blue-600">{entry.gurujiName || "General service"}</span>
                  <span className="block text-[10px] text-slate-400">#{entry.bookingId?.slice(0, 8) || "No booking"}</span>
                </div>
                {Number(entry.rating || 0) <= 2 && (
                  <span className="flex items-center gap-1 text-[10px] font-bold uppercase text-red-600">
                    <AlertCircle size={12} /> Low rating
                  </span>
                )}
              </div>
            </div>
          ))
        )}
      </div>
    </AdminShell>
  );
}

function formatDate(value) {
  if (!value) return "";
  if (value?.toDate) return value.toDate().toLocaleString();
  if (value?.seconds) return new Date(value.seconds * 1000).toLocaleString();
  return String(value);
}
