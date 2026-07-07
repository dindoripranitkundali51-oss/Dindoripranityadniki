export function canUpdatePayment(tx = {}) {
  if (tx.paymentStatus === "Paid") return false;
  const amount = Number(tx.actualAmount || 0);
  return amount > 0 && ["Payment Pending", "Awaiting Verification"].includes(tx.status || "");
}

export function paymentOptions(tx = {}) {
  const current = tx.paymentStatus || "Pending";
  if (!canUpdatePayment(tx)) return [current];
  if (tx.status === "Payment Pending") return unique([current, "Submitted", "Paid"]);
  if (tx.status === "Awaiting Verification") return unique([current, "Paid", "Rejected"]);
  return [current];
}

export function buildPaymentStatusDialog(copy, newStatus, isMarathi) {
  return {
    newStatus,
    title: copy.updateTitle(newStatus),
    message: copy.updateMessage,
    confirmLabel: copy.setStatus(newStatus),
    tone: dialogToneForPaymentStatus(newStatus),
    successMessage: copy.statusUpdated(newStatus),
    approvalRequiredMessage: copy.makerChecker,
    currentStatusLabel: paymentStatusLabel(newStatus, isMarathi),
  };
}

export function dialogToneForPaymentStatus(status) {
  if (status === "Paid") return "success";
  if (status === "Rejected") return "danger";
  return "info";
}

export function paymentStatusLabel(status, isMarathi) {
  if (!isMarathi) return status;
  switch (status) {
    case "Pending":
      return "प्रलंबित";
    case "Submitted":
      return "सबमिट";
    case "Awaiting Verification":
      return "पडताळणी प्रलंबित";
    case "Paid":
      return "पेड";
    case "Rejected":
      return "नाकारले";
    default:
      return status;
  }
}

export function bookingStatusLabel(status, isMarathi) {
  const normalizedStatus = status === "Paid" ? "Completed" : status;
  if (!isMarathi) return normalizedStatus;
  switch (normalizedStatus) {
    case "Pending":
      return "प्रलंबित";
    case "Assigned":
      return "नियुक्त";
    case "Accepted":
      return "स्वीकारले";
    case "In Progress":
      return "सुरू";
    case "Payment Pending":
      return "पेमेंट प्रलंबित";
    case "Awaiting Verification":
      return "पडताळणी प्रलंबित";
    case "Completed":
      return "पूर्ण";
    case "Cancelled":
      return "रद्द";
    default:
      return normalizedStatus;
  }
}

function unique(items) {
  return [...new Set(items.filter(Boolean))];
}
