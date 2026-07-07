"use client";

import { useEffect } from "react";

export default function PwaInstallBridge() {
  useEffect(() => {
    if (typeof window === "undefined" || !("serviceWorker" in navigator)) return undefined;

    let isMounted = true;
    const registerServiceWorker = async () => {
      try {
        const registration = await navigator.serviceWorker.register("/sw.js");
        if (!isMounted) return;
        window.__dpAdminPwa = {
          ...(window.__dpAdminPwa || {}),
          serviceWorkerRegistered: true,
          registrationScope: registration.scope,
        };
      } catch (error) {
        console.error("PWA service worker registration failed", error);
      }
    };

    registerServiceWorker();

    const handleBeforeInstallPrompt = (event) => {
      event.preventDefault();
      window.__dpAdminPwa = {
        ...(window.__dpAdminPwa || {}),
        deferredPrompt: event,
        installReady: true,
      };
      window.dispatchEvent(new CustomEvent("dp-admin-install-ready"));
    };

    window.addEventListener("beforeinstallprompt", handleBeforeInstallPrompt);

    return () => {
      isMounted = false;
      window.removeEventListener("beforeinstallprompt", handleBeforeInstallPrompt);
    };
  }, []);

  return null;
}
