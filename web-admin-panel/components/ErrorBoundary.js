"use client";

import { Component } from "react";
import { reportAdminClientIssue } from "@/lib/telemetry";

class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null, errorInfo: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error('ErrorBoundary caught an error:', error, errorInfo);
    reportAdminClientIssue({
      level: "error",
      message: error?.message || "React error boundary crash",
      page: typeof window !== "undefined" ? window.location.pathname : "",
      source: "react.error-boundary",
      stack: error?.stack || "",
      metadata: {
        componentStack: errorInfo?.componentStack || "",
      },
    });
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen bg-slate-50 flex flex-col items-center justify-center p-6 text-center">
          <h1 className="text-2xl font-bold text-slate-800 mb-4">Something went wrong.</h1>
          <p className="text-slate-500 mb-8">We apologize for the inconvenience. Please try refreshing the page.</p>
          {process.env.NODE_ENV === 'development' && (
            <details className="mt-6 text-left max-w-xl w-full">
              <summary className="cursor-pointer text-blue-600 font-semibold mb-2">Error Details</summary>
              <pre className="bg-slate-100 p-4 rounded-lg overflow-auto text-xs text-slate-700">
                {this.state.error && this.state.error.toString()}
                <br />
                {this.state.errorInfo && this.state.errorInfo.componentStack}
              </pre>
            </details>
          )}
          <button
            onClick={() => window.location.reload()}
            className="mt-8 bg-blue-600 hover:bg-blue-700 text-white font-medium py-3 px-6 rounded-lg transition-colors"
          >
            Refresh Page
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
