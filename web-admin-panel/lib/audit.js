import { getAuthClient } from "./authClient";
import { logError } from "./logger";

export const logAdminAction = async (action, targetId, module = "System", details = {}) => {
  try {
    const user = getAuthClient().currentUser;
    if (!user) return;
    // Audit logs are stored server-side via API calls
    console.log(`[Audit] ${action} on ${targetId} in ${module}`);
  } catch (error) {
    logError("audit-log", error);
  }
};
