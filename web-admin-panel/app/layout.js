import "./globals.css";
import AdminGate from "./AdminGate";
import { AuthProvider } from "../context/AuthContext";
import { LanguageProvider } from "../context/LanguageContext";
import ErrorBoundary from "../components/ErrorBoundary";
import ClientTelemetryBridge from "../components/ClientTelemetryBridge";
import PwaInstallBridge from "../components/PwaInstallBridge";

export const metadata = {
  title: "Dindori Pranit Admin",
  description: "Installable admin operations console for Dindori Pranit Yadnyiki.",
  robots: "noindex,nofollow",
  manifest: "/manifest.webmanifest",
  applicationName: "Dindori Pranit Admin",
  icons: {
    icon: "/admin-icon.png",
    shortcut: "/admin-icon.png",
    apple: "/admin-icon.png",
  },
  appleWebApp: {
    capable: true,
    statusBarStyle: "default",
    title: "DP Admin",
  },
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body className="bg-slate-50">
        <ErrorBoundary>
          <AuthProvider>
            <LanguageProvider>
              <ClientTelemetryBridge />
              <PwaInstallBridge />
              <AdminGate>{children}</AdminGate>
            </LanguageProvider>
          </AuthProvider>
        </ErrorBoundary>
      </body>
    </html>
  );
}
