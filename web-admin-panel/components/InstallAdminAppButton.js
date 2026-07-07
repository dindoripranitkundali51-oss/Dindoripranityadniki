"use client";

import { useEffect, useState } from "react";
import { Download } from "lucide-react";

export default function InstallAdminAppButton() {
  const [ready, setReady] = useState(false);
  const [installing, setInstalling] = useState(false);

  useEffect(() => {
    if (typeof window === "undefined") return undefined;

    const sync = () => setReady(Boolean(window.__dpAdminPwa?.deferredPrompt));
    sync();
    window.addEventListener("dp-admin-install-ready", sync);
    return () => window.removeEventListener("dp-admin-install-ready", sync);
  }, []);

  const handleInstall = async () => {
    const deferredPrompt = window.__dpAdminPwa?.deferredPrompt;
    if (!deferredPrompt) return;

    setInstalling(true);
    try {
      await deferredPrompt.prompt();
      await deferredPrompt.userChoice;
    } finally {
      if (window.__dpAdminPwa) {
        window.__dpAdminPwa.deferredPrompt = null;
        window.__dpAdminPwa.installReady = false;
      }
      setReady(false);
      setInstalling(false);
    }
  };

  if (!ready) return null;

  return (
    <button
      type="button"
      onClick={handleInstall}
      disabled={installing}
      className="inline-flex items-center gap-2 rounded border border-violet-200 bg-violet-50 px-3 py-2 text-xs font-semibold text-violet-700 transition hover:bg-violet-100 disabled:opacity-60"
    >
      <Download size={14} />
      {installing ? "Installing..." : "Install Admin App"}
    </button>
  );
}
