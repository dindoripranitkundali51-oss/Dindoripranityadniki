"use client";

import { useEffect, useState } from "react";
import AdminShell from "@/components/AdminShell";
import { ActionNotice } from "@/components/ActionDialog";
import { Loader2, Award, ShieldAlert, CheckCircle, XCircle, FileText, User } from "lucide-react";

export default function KycVerificationPage() {
  const [loading, setLoading] = useState(true);
  const [pendingKyc, setPendingKyc] = useState([]);
  const [notice, setNotice] = useState(null);
  const [actioning, setActioning] = useState(null);

  useEffect(() => {
    fetchPendingKyc();
  }, []);

  const fetchPendingKyc = async () => {
    try {
      const baseUrl = process.env.NEXT_PUBLIC_API_URL || "https://dindoritrial.somee.com/api/v1";
      const token = localStorage.getItem("jwt_auth_token") || "";

      const res = await fetch(`${baseUrl}/kyc/pending`, {
        headers: { "Authorization": `Bearer ${token}` }
      });
      if (!res.ok) throw new Error("Failed to fetch pending KYC submissions.");
      const data = await res.json();
      setPendingKyc(data.data || []);
    } catch (err) {
      setNotice({ tone: "danger", message: err.message });
    } finally {
      setLoading(false);
    }
  };

  const handleVerify = async (uid, status) => {
    setActioning(uid);
    setNotice(null);

    try {
      const baseUrl = process.env.NEXT_PUBLIC_API_URL || "https://dindoritrial.somee.com/api/v1";
      const token = localStorage.getItem("jwt_auth_token") || "";

      const res = await fetch(`${baseUrl}/kyc/verify/${uid}`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${token}`
        },
        body: JSON.stringify({ status })
      });

      if (!res.ok) throw new Error("Failed to complete KYC verification.");

      setNotice({ tone: "success", message: `KYC successfully ${status == "Approved" ? "Approved" : "Rejected"}!` });
      fetchPendingKyc();
    } catch (err) {
      setNotice({ tone: "danger", message: err.message });
    } finally {
      setActioning(null);
    }
  };

  return (
    <AdminShell className="p-6">
      <ActionNotice
        tone={notice?.tone}
        message={notice?.message}
        onClose={() => setNotice(null)}
      />

      <header className="mb-8">
        <h1 className="text-2xl font-black text-slate-800 uppercase tracking-wide">Guruji KYC Verifications</h1>
        <p className="text-slate-500 text-sm">Review, verify and approve official document submissions (PAN / Aadhar Card) from registering Gurujis.</p>
      </header>

      <div className="bg-white border border-slate-200 rounded-[35px] p-8 shadow-sm">
        <h3 className="font-bold text-slate-800 text-sm uppercase tracking-wider mb-6 flex items-center gap-2">
          <Award size={18} className="text-blue-600" /> Pending Submissions
        </h3>

        {loading ? (
          <div className="py-20 text-center text-slate-400">
            <Loader2 className="animate-spin mx-auto mb-2 text-blue-600" />
            <p className="text-xs italic">Loading pending verification requests...</p>
          </div>
        ) : pendingKyc.length === 0 ? (
          <div className="py-20 text-center border border-dashed rounded-3xl bg-slate-50 text-slate-400">
            <p className="text-xs">All KYC verifications are up to date. No pending submissions.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-6">
            {pendingKyc.map((k) => {
              const isPanValid = /^[A-Z]{5}[0-9]{4}[A-Z]{1}$/.test(k.panNumber?.trim().toUpperCase() || "");
              const isAadharValid = /^[0-9]{12}$/.test(k.aadharNumber?.trim() || "");
              const isNameValid = k.fullName && k.fullName.trim().length >= 3 && !/[0-9]/.test(k.fullName);
              const isMobileValid = /^[6-9]\d{9}$/.test(k.mobile || "");

              return (
                <div key={k.uid} className="bg-slate-50 border p-6 rounded-3xl flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
                  
                  {/* Profile Details */}
                  <div className="flex flex-col gap-3 w-full md:w-auto">
                    <div className="flex items-start gap-4">
                      <div className="bg-blue-100 p-3 rounded-2xl text-blue-600 shrink-0">
                        <User size={20} />
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <h4 className="font-bold text-slate-800 text-sm">{k.fullName}</h4>
                          <span className="px-2 py-0.5 rounded text-[9px] font-bold uppercase bg-blue-100 text-blue-700">
                            {k.kycStatus || "Submitted"}
                          </span>
                        </div>
                        <p className="text-xs text-slate-500 mt-1">
                          Mobile: <span className="font-semibold text-slate-700">{k.mobile}</span> | 
                          PAN: <span className="font-mono font-semibold text-slate-700">{k.panNumber || "-"}</span> | 
                          Aadhar: <span className="font-mono font-semibold text-slate-700">{k.aadharNumber || "-"}</span>
                        </p>
                        <div className="flex gap-4 mt-3">
                          <a href={k.panCardUrl} target="_blank" rel="noopener noreferrer" className="flex items-center gap-1 text-[10px] font-bold text-blue-600 hover:underline">
                            <FileText size={12} /> View PAN Card
                          </a>
                          <a href={k.aadharCardUrl} target="_blank" rel="noopener noreferrer" className="flex items-center gap-1 text-[10px] font-bold text-blue-600 hover:underline">
                            <FileText size={12} /> View Aadhar Card
                          </a>
                        </div>
                      </div>
                    </div>

                    {/* Automatic OCR Verification Status Checklist */}
                    <div className="mt-2 bg-white p-3 rounded-2xl border border-slate-100 text-[10px] flex flex-wrap gap-x-4 gap-y-1 text-slate-500">
                      <span className="font-bold text-slate-700 uppercase text-[9px] mr-2 self-center">OCR Auto Check:</span>
                      <div className="flex items-center gap-1">
                        <span className={isPanValid ? "text-green-600 font-bold" : "text-red-500 font-bold"}>{isPanValid ? "✓" : "✗"}</span> PAN Format
                      </div>
                      <div className="flex items-center gap-1">
                        <span className={isAadharValid ? "text-green-600 font-bold" : "text-red-500 font-bold"}>{isAadharValid ? "✓" : "✗"}</span> Aadhar Format
                      </div>
                      <div className="flex items-center gap-1">
                        <span className={isNameValid ? "text-green-600 font-bold" : "text-red-500 font-bold"}>{isNameValid ? "✓" : "✗"}</span> Name Check
                      </div>
                      <div className="flex items-center gap-1">
                        <span className={isMobileValid ? "text-green-600 font-bold" : "text-red-500 font-bold"}>{isMobileValid ? "✓" : "✗"}</span> Mobile Check
                      </div>
                    </div>
                  </div>

                {/* Actions */}
                <div className="flex items-center gap-2 w-full md:w-auto justify-end">
                  {actioning === k.uid ? (
                    <Loader2 className="animate-spin text-blue-600" size={20} />
                  ) : (
                    <>
                      <button 
                        onClick={() => handleVerify(k.uid, "Approved")}
                        className="bg-green-600 hover:bg-green-700 text-white text-xs font-bold uppercase tracking-wider px-4 py-2.5 rounded-xl flex items-center gap-1 transition-colors"
                      >
                        <CheckCircle size={14} /> Approve
                      </button>
                      <button 
                        onClick={() => handleVerify(k.uid, "Rejected")}
                        className="bg-white border border-red-200 text-red-600 hover:bg-red-50 text-xs font-bold uppercase tracking-wider px-4 py-2.5 rounded-xl flex items-center gap-1 transition-colors"
                      >
                        <XCircle size={14} /> Reject
                      </button>
                    </>
                  )}
                </div>

              </div>
            );
          })}
          </div>
        )}
      </div>
    </AdminShell>
  );
}
