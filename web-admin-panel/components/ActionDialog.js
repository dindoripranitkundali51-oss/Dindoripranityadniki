"use client";

import { useEffect, useState } from "react";
import {
  AlertTriangle,
  CheckCircle2,
  Info,
  Loader2,
  X,
  XCircle,
} from "lucide-react";

const TONE_STYLES = {
  danger: {
    icon: AlertTriangle,
    iconClass: "bg-red-50 text-red-600",
    confirmClass: "bg-red-600 hover:bg-red-700 text-white",
    noticeClass: "border-red-200 bg-red-50 text-red-700",
  },
  success: {
    icon: CheckCircle2,
    iconClass: "bg-green-50 text-green-600",
    confirmClass: "bg-green-600 hover:bg-green-700 text-white",
    noticeClass: "border-green-200 bg-green-50 text-green-700",
  },
  info: {
    icon: Info,
    iconClass: "bg-blue-50 text-blue-600",
    confirmClass: "bg-blue-600 hover:bg-blue-700 text-white",
    noticeClass: "border-blue-200 bg-blue-50 text-blue-700",
  },
};

function getToneStyles(tone) {
  return TONE_STYLES[tone] || TONE_STYLES.info;
}

export function ActionNotice({ tone = "info", message, onClose }) {
  if (!message) return null;
  const styles = getToneStyles(tone);
  const Icon = tone === "danger" ? XCircle : tone === "success" ? CheckCircle2 : Info;

  return (
    <div className={`mb-4 flex items-start gap-3 rounded-lg border px-4 py-3 text-sm ${styles.noticeClass}`}>
      <Icon size={18} className="mt-0.5 shrink-0" />
      <p className="flex-1">{message}</p>
      {onClose ? (
        <button
          type="button"
          onClick={onClose}
          className="shrink-0 text-current/70 transition hover:text-current"
          aria-label="Dismiss notice"
        >
          <X size={16} />
        </button>
      ) : null}
    </div>
  );
}

export default function ActionDialog({
  title,
  message,
  note = "",
  confirmLabel = "Confirm",
  cancelLabel = "Cancel",
  tone = "info",
  requireReason = false,
  reasonLabel = "Reason",
  reasonPlaceholder = "Write a short note",
  reasonMinLength = 0,
  initialReason = "",
  loading = false,
  error = "",
  onClose,
  onConfirm,
}) {
  const [reason, setReason] = useState(initialReason);

  useEffect(() => {
    setReason(initialReason || "");
  }, [initialReason, title, message, requireReason]);

  const styles = getToneStyles(tone);
  const Icon = styles.icon;
  const trimmedReason = reason.trim();
  const minimumLength = requireReason ? Math.max(reasonMinLength, 1) : reasonMinLength;
  const reasonInvalid = requireReason && trimmedReason.length < minimumLength;

  return (
    <div className="fixed inset-0 z-[130] flex items-center justify-center bg-black/50 p-4">
      <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl">
        <div className="flex items-start gap-4">
          <div className={`rounded-full p-3 ${styles.iconClass}`}>
            <Icon size={20} />
          </div>
          <div className="flex-1">
            <h2 className="text-lg font-bold text-slate-900">{title}</h2>
            <p className="mt-2 text-sm text-slate-600">{message}</p>
            {note ? <p className="mt-2 text-xs text-slate-500">{note}</p> : null}
          </div>
        </div>

        <div className="mt-5">
          <label className="block text-xs font-bold uppercase text-slate-500">
            {reasonLabel}
            {requireReason ? " *" : ""}
          </label>
          <textarea
            value={reason}
            onChange={(event) => setReason(event.target.value)}
            rows={4}
            className="mt-2 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500"
            placeholder={reasonPlaceholder}
          />
          {reasonInvalid ? (
            <p className="mt-2 text-xs text-red-600">
              Reason must be at least {minimumLength} characters.
            </p>
          ) : (
            <p className="mt-2 text-xs text-slate-400">
              Keep it specific enough for audit and support follow-up.
            </p>
          )}
        </div>

        {error ? (
          <div className="mt-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            {error}
          </div>
        ) : null}

        <div className="mt-6 flex justify-end gap-3">
          <button
            type="button"
            onClick={onClose}
            disabled={loading}
            className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-600 disabled:opacity-50"
          >
            {cancelLabel}
          </button>
          <button
            type="button"
            onClick={() => onConfirm(trimmedReason)}
            disabled={loading || reasonInvalid}
            className={`rounded-lg px-4 py-2 text-sm font-semibold disabled:opacity-50 ${styles.confirmClass}`}
          >
            {loading ? (
              <span className="inline-flex items-center gap-2">
                <Loader2 size={16} className="animate-spin" />
                Working...
              </span>
            ) : (
              confirmLabel
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
