import { useEffect, useState } from "react";
import { logError } from "./logger";

export function useApiCollection(initialData, collectionName) {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let active = true;
    let endpoint = "";
    const name = collectionName.toLowerCase();
    
    if (name.includes("user")) {
      endpoint = name.includes("booking") ? "bookings" : "users";
    } else if (name.includes("booking")) {
      endpoint = "bookings";
    } else if (name.includes("guruji") || name.includes("expert")) {
      endpoint = "experts";
    } else if (name.includes("ledger") || name.includes("payout")) {
      endpoint = "payouts";
    } else if (name.includes("audit")) {
      endpoint = "audit-logs";
    } else if (name.includes("pooja")) {
      endpoint = "poojas";
    } else if (name.includes("support")) {
      endpoint = "support/tickets";
    } else if (name.includes("faq") || name.includes("kb")) {
      endpoint = "faqs";
    }

    if (!endpoint) {
      setRows([]);
      setLoading(false);
      return;
    }

    const fetchCollection = async () => {
      try {
        const baseUrl = process.env.NEXT_PUBLIC_API_URL || "https://dindoripranitapi.somee.com/api/v1";
        const token = typeof window !== "undefined" ? localStorage.getItem("jwt_auth_token") || "" : "";
        
        const res = await fetch(`${baseUrl}/admin/${endpoint}`, {
          headers: {
            "Authorization": token ? `Bearer ${token}` : ""
          }
        });
        
        if (!res.ok) throw new Error(`HTTP error ${res.status}`);
        const data = await res.json();
        
        if (active) {
          // If the response is paginated (e.g. { rows: [], total: 0 }), extract the array
          const rowsData = data && Array.isArray(data) ? data : (data?.rows || []);
          setRows(rowsData);
          setError(null);
        }
      } catch (err) {
        logError(collectionName, err);
        if (active) {
          setError(err);
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    };

    fetchCollection();
    const interval = setInterval(fetchCollection, 10000);
    
    return () => {
      active = false;
      clearInterval(interval);
    };
  }, [collectionName]);

  return { rows, loading, error };
}
