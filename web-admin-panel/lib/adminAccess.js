import { deriveAdminCapabilities, hasAdminCapability, isEnabledWhitelistRecord } from "./adminCapabilities.mjs";

export { deriveAdminCapabilities, hasAdminCapability };

export async function getAdminAccess(user) {
  if (!user) return { isAdmin: false, profile: null };
  const token = typeof window !== "undefined" ? localStorage.getItem("jwt_auth_token") : null;
  if (!token) return { isAdmin: false, profile: null };
  return {
    isAdmin: true,
    profile: {
      id: user.uid || "admin",
      uid: user.uid || "admin",
      email: user.email || "admin@dindori.org",
      accessLevel: "owner",
      capabilities: deriveAdminCapabilities({ accessLevel: "owner", status: "Active" }),
    },
  };
}

export const checkAdminAccess = getAdminAccess;
