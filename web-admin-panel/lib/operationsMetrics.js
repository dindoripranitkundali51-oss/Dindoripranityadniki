export function riskRank(risk) {
  const sev = risk?.severity === "high" ? 100 : risk?.severity === "medium" ? 50 : 10;
  const age = risk?.createdAt?.seconds ? Math.min(30, (Date.now() / 1000 - risk.createdAt.seconds) / 3600) : 0;
  return sev + age;
}

export function formatDate(value) {
  if (!value) return "-";
  if (value?.toDate) return value.toDate().toLocaleString();
  if (value?.seconds) return new Date(value.seconds * 1000).toLocaleString();
  return String(value);
}

export function summarizeNotificationFailures(notificationLogs = []) {
  const failedNotifications = notificationLogs.filter((item) => item.status === "failed");
  const escalatedNotifications = failedNotifications.filter((item) => item.failoverState === "escalated");
  return {
    failedNotifications,
    escalatedNotifications,
  };
}
