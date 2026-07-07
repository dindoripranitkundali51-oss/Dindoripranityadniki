"use client";

import { Suspense, useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { CheckCircle2, Loader2, ReceiptText, Search, XCircle } from "lucide-react";
import { callPublicFunction } from "@/lib/publicFunctions";

export default function VerifyReceiptPage() {
  return (
    <Suspense fallback={<VerifyShell loading />}>
      <VerifyReceiptContent />
    </Suspense>
  );
}

function VerifyReceiptContent() {
  const searchParams = useSearchParams();
  const [receiptNo, setReceiptNo] = useState(searchParams.get("id") || "");
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const verify = async (value = receiptNo) => {
    const clean = value.trim();
    if (!clean) return;
    setLoading(true);
    setError("");
    setResult(null);
    try {
      const response = await callPublicFunction("verifyReceiptPublic", { receiptNo: clean });
      setResult(response.data);
    } catch (err) {
      setError(err.message || "Verification failed.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const id = searchParams.get("id");
    if (id) verify(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams]);

  return (
    <VerifyShell loading={loading}>
      <form
        onSubmit={(event) => {
          event.preventDefault();
          verify();
        }}
        className="mx-auto mt-8 flex w-full max-w-xl gap-2"
      >
        <div className="relative flex-1">
          <ReceiptText className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
          <input
            value={receiptNo}
            onChange={(event) => setReceiptNo(event.target.value.toUpperCase())}
            placeholder="Enter receipt number"
            className="w-full rounded border border-slate-300 bg-white py-3 pl-10 pr-3 text-sm outline-none focus:border-orange-600"
          />
        </div>
        <button className="inline-flex items-center gap-2 rounded bg-orange-700 px-4 py-3 text-sm font-semibold text-white hover:bg-orange-800">
          <Search size={16} />
          Verify
        </button>
      </form>

      {error && <ResultCard valid={false} title="Unable to verify" body={error} />}
      {result && !result.valid && <ResultCard valid={false} title="Receipt not found" body={result.message || "This receipt could not be verified."} />}
      {result?.valid && (
        <ResultCard valid title="Receipt verified" body="This receipt exists in the official Dindori Pranit Yadnyiki receipt archive.">
          <dl className="mt-5 grid grid-cols-1 gap-3 text-sm sm:grid-cols-2">
            <Field label="Receipt No" value={result.receiptNo} />
            <Field label="Seva" value={result.poojaName} />
            <Field label="Date" value={result.date || "-"} />
            <Field label="Amount" value={`Rs. ${Number(result.totalAmount || 0).toFixed(2)}`} />
            <Field label="Status" value={result.status || "Valid"} />
          </dl>
        </ResultCard>
      )}
    </VerifyShell>
  );
}

function VerifyShell({ children, loading }) {
  return (
    <main className="min-h-screen bg-slate-50 px-4 py-10 text-slate-900">
      <section className="mx-auto max-w-3xl text-center">
        <p className="text-sm font-semibold uppercase tracking-wide text-orange-700">Official receipt verification</p>
        <h1 className="mt-2 text-3xl font-bold">Dindori Pranit Yadnyiki</h1>
        <p className="mt-3 text-sm text-slate-500">Scan the receipt QR code or enter the receipt number to confirm authenticity.</p>
        {loading && <Loader2 className="mx-auto mt-8 animate-spin text-orange-700" />}
        {children}
      </section>
    </main>
  );
}

function ResultCard({ valid, title, body, children }) {
  const Icon = valid ? CheckCircle2 : XCircle;
  return (
    <div className="mx-auto mt-8 max-w-xl rounded border border-slate-200 bg-white p-6 text-left shadow-sm">
      <div className="flex items-start gap-3">
        <Icon className={valid ? "text-green-600" : "text-red-600"} size={28} />
        <div>
          <h2 className="text-lg font-bold">{title}</h2>
          <p className="mt-1 text-sm text-slate-600">{body}</p>
        </div>
      </div>
      {children}
    </div>
  );
}

function Field({ label, value }) {
  return (
    <div className="rounded bg-slate-50 p-3">
      <dt className="text-xs font-semibold uppercase text-slate-400">{label}</dt>
      <dd className="mt-1 font-medium text-slate-800">{value || "-"}</dd>
    </div>
  );
}
