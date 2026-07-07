import { logError } from "./logger";

export const getAuthClient = () => {
  // Return a mock auth object that mimics the firebase Auth structure
  const token = typeof window !== "undefined" ? localStorage.getItem("jwt_auth_token") : null;
  return {
    currentUser: token ? {
      uid: "admin",
      email: "admin@dindori.org",
      displayName: "Admin Owner"
    } : null
  };
};

export const onAdminAuthStateChanged = (callback) => {
  const token = typeof window !== "undefined" ? localStorage.getItem("jwt_auth_token") : null;
  if (token) {
    callback({
      uid: "admin",
      email: "admin@dindori.org",
      displayName: "Admin Owner"
    });
  } else {
    callback(null);
  }
  return () => {};
};

export const resolveAdminLoginEmail = async (identifier) => {
  const normalized = String(identifier || "").trim().toLowerCase();
  if (!normalized || !normalized.includes("@")) {
    throw new Error("A valid administrator email is required.");
  }
  return normalized;
};

export const signInAdmin = async (email, password) => {
  const loginEmail = await resolveAdminLoginEmail(email);
  const baseUrl = process.env.NEXT_PUBLIC_API_URL || "//dindoripranitapi.somee.com/api/v1";
  
  const res = await fetch(`${baseUrl}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ mobile: loginEmail, password: password })
  });
  
  if (!res.ok) {
    const errData = await res.json().catch(() => ({}));
    throw new Error(errData.message || "Invalid credentials.");
  }
  
  const data = await res.json();
  if (data.token) {
    localStorage.setItem("jwt_auth_token", data.token);
  }
  
  return {
    user: {
      uid: data.profile?.Uid || "admin",
      email: loginEmail,
      displayName: "Administrator"
    }
  };
};

export const signOutAdmin = () => {
  localStorage.removeItem("jwt_auth_token");
  return Promise.resolve();
};

export const resetAdminPassword = async (email) => {
  // Mock password reset
  return Promise.resolve();
};
