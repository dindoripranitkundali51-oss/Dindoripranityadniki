import { getAuthClient } from "./authClient";
import { logAdminAction } from "./audit";

export async function callAdminApi(functionName, params) {
  if (!getAuthClient().currentUser) {
    throw new Error("Admin session expired. Please login again.");
  }
  
  const baseUrl = process.env.NEXT_PUBLIC_API_URL || "//dindoripranitapi.somee.com/api/v1";
  const token = typeof window !== "undefined" ? localStorage.getItem("jwt_auth_token") || "" : "";
  
  let url = "";
  let method = "POST";
  let body = params;
  
  if (functionName === "manageUserStatus") {
    url = `${baseUrl}/admin/users/${params.userId}/status`;
    method = "PUT";
    body = { status: params.status };
  } else if (functionName === "manageExpertStatus") {
    url = `${baseUrl}/admin/experts/${params.gurujiId}/status`;
    method = "PUT";
    body = { status: params.status };
  } else if (functionName === "updateSettings") {
    url = `${baseUrl}/admin/settings`;
    method = "PUT";
    body = params;
  } else if (functionName === "createPooja") {
    url = `${baseUrl}/admin/poojas`;
    method = "POST";
    body = params;
  } else if (functionName === "updatePooja") {
    url = `${baseUrl}/admin/poojas/${params.id}`;
    method = "PUT";
    body = params;
  } else if (functionName === "deletePooja") {
    url = `${baseUrl}/admin/poojas/${params.id}`;
    method = "DELETE";
    body = null;
  } else if (functionName === "updateSupportTicketStatus") {
    url = `${baseUrl}/admin/support/tickets/${params.ticketId}/status`;
    method = "PUT";
    body = { status: params.status };
  } else if (functionName === "saveFaqEntry") {
    url = `${baseUrl}/admin/faqs`;
    method = "POST";
    body = params;
  } else if (functionName === "updateFaqStatus") {
    url = `${baseUrl}/admin/faqs/${params.id}`;
    method = "PUT";
    body = params;
  } else if (functionName === "deleteFaq") {
    url = `${baseUrl}/admin/faqs/${params.id}`;
    method = "DELETE";
    body = null;
  } else if (functionName === "saveTranslations") {
    url = `${baseUrl}/admin/languages/${params.lang}`;
    method = "PUT";
    body = params.strings;
  } else if (functionName === "saveCms") {
    url = `${baseUrl}/admin/cms`;
    method = "PUT";
    body = params.config;
  } else if (functionName === "saveLegal") {
    url = `${baseUrl}/admin/legal/${params.doc}`;
    method = "PUT";
    body = { content: params.content };
  } else {
    url = `${baseUrl}/admin/${functionName}`;
  }
  
  const targetId = params?.bookingId || params?.requestId || params?.gurujiId || params?.target || "system";
  
  try {
    const res = await fetch(url, {
      method: method,
      headers: {
        "Content-Type": "application/json",
        "Authorization": token ? `Bearer ${token}` : ""
      },
      body: JSON.stringify(body)
    });
    
    if (!res.ok) {
      const errData = await res.json().catch(() => ({}));
      throw new Error(errData.message || `Failed to call function ${functionName}`);
    }
    
    const data = await res.json();
    await logAdminAction(`CALL_${functionName}`, targetId, "Callable", { status: "Success" }).catch(() => {});
    return { data };
  } catch (error) {
    await logAdminAction(`CALL_${functionName}`, targetId, "Callable", { status: "Failed", message: error.message || "Callable request failed" }).catch(() => {});
    throw error;
  }
}
