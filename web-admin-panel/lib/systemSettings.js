export const DEFAULT_CONFIG = {
  trust_name: "Dindori Pranit Yadnyiki",
  app_version: "1.0.0",
  maintenance_mode: false,
  commission_pct: 70,
  kill_switch_enabled: false,
  disable_new_bookings: false,
  disable_payments: false,
  disable_admin_broadcasts: false,
  auto_approve_service_requests: true,
  auto_approve_maker_checker: true,
  auto_close_support_tickets: true,
  auto_process_low_risk_withdrawals: true,
  auto_process_all_withdrawals: false,
  auto_resolve_risk_events: false,
  withdrawal_high_risk_amount: 5000,
  risk_open_threshold_minutes: 30,
  notification_failover_threshold_minutes: 15,
  support_auto_attend_minutes: 15,
  support_auto_resolve_hours: 24,
  service_request_auto_approve_minutes: 5,
};

export const DEFAULT_UI_SETTINGS = {
  dashboard_title: "Dindori Pranit Yadnyiki",
  dashboard_subtitle: "Platform Performance Overview",
  maintenance_banner_en: "",
  maintenance_banner_mr: "",
  reports_auto_refresh_sec: 60,
};

export function validateSystemSettings(config, uiSettings) {
  const commission = Number(config.commission_pct);
  const refreshSeconds = Number(uiSettings.reports_auto_refresh_sec || 60);
  const withdrawalHighRiskAmount = Number(config.withdrawal_high_risk_amount || 0);
  const riskOpenThresholdMinutes = Number(config.risk_open_threshold_minutes || 0);
  const notificationFailoverThresholdMinutes = Number(config.notification_failover_threshold_minutes || 0);
  const supportAutoAttendMinutes = Number(config.support_auto_attend_minutes || 0);
  const supportAutoResolveHours = Number(config.support_auto_resolve_hours || 0);
  const serviceRequestAutoApproveMinutes = Number(config.service_request_auto_approve_minutes || 0);

  if (!Number.isFinite(commission) || commission < 0 || commission > 100) {
    return { ok: false, error: "Trust share must be between 0 and 100." };
  }
  if (!Number.isFinite(refreshSeconds) || refreshSeconds < 15) {
    return { ok: false, error: "Reports auto refresh must be at least 15 seconds." };
  }
  if (!Number.isFinite(withdrawalHighRiskAmount) || withdrawalHighRiskAmount < 0) {
    return { ok: false, error: "Withdrawal high risk amount must be 0 or more." };
  }
  if (!Number.isFinite(riskOpenThresholdMinutes) || riskOpenThresholdMinutes < 5) {
    return { ok: false, error: "Risk escalation threshold must be at least 5 minutes." };
  }
  if (!Number.isFinite(notificationFailoverThresholdMinutes) || notificationFailoverThresholdMinutes < 5) {
    return { ok: false, error: "Notification failover threshold must be at least 5 minutes." };
  }
  if (!Number.isFinite(supportAutoAttendMinutes) || supportAutoAttendMinutes < 1) {
    return { ok: false, error: "Support auto attend threshold must be at least 1 minute." };
  }
  if (!Number.isFinite(supportAutoResolveHours) || supportAutoResolveHours < 1) {
    return { ok: false, error: "Support auto resolve threshold must be at least 1 hour." };
  }
  if (!Number.isFinite(serviceRequestAutoApproveMinutes) || serviceRequestAutoApproveMinutes < 1) {
    return { ok: false, error: "Service request auto approve threshold must be at least 1 minute." };
  }

  return {
    ok: true,
    values: {
      commission,
      refreshSeconds,
      withdrawalHighRiskAmount,
      riskOpenThresholdMinutes,
      notificationFailoverThresholdMinutes,
      supportAutoAttendMinutes,
      supportAutoResolveHours,
      serviceRequestAutoApproveMinutes,
    },
  };
}
