import test from "node:test";
import assert from "node:assert/strict";
import {
  bookingStatusLabel,
  buildPaymentStatusDialog,
  canUpdatePayment,
  paymentOptions,
  paymentStatusLabel,
} from "../lib/paymentAdmin.js";

const copy = {
  updateTitle: (status) => `Update ${status}`,
  updateMessage: "Live state change",
  setStatus: (status) => `Set ${status}`,
  makerChecker: (requestId) => `Approval ${requestId}`,
  statusUpdated: (status) => `Updated ${status}`,
};

test("canUpdatePayment only allows positive pending verification flows", () => {
  assert.equal(canUpdatePayment({ status: "Payment Pending", actualAmount: 1200, paymentStatus: "Pending" }), true);
  assert.equal(canUpdatePayment({ status: "Awaiting Verification", actualAmount: 1200, paymentStatus: "Submitted" }), true);
  assert.equal(canUpdatePayment({ status: "Awaiting Verification", actualAmount: 0, paymentStatus: "Submitted" }), false);
  assert.equal(canUpdatePayment({ status: "Completed", actualAmount: 1200, paymentStatus: "Pending" }), false);
  assert.equal(canUpdatePayment({ status: "Payment Pending", actualAmount: 1200, paymentStatus: "Paid" }), false);
});

test("paymentOptions narrows actions by booking stage", () => {
  assert.deepEqual(
    paymentOptions({ status: "Payment Pending", paymentStatus: "Pending", actualAmount: 100 }),
    ["Pending", "Submitted", "Paid"]
  );
  assert.deepEqual(
    paymentOptions({ status: "Awaiting Verification", paymentStatus: "Submitted", actualAmount: 100 }),
    ["Submitted", "Paid", "Rejected"]
  );
  assert.deepEqual(
    paymentOptions({ status: "Completed", paymentStatus: "Rejected", actualAmount: 100 }),
    ["Rejected"]
  );
});

test("buildPaymentStatusDialog derives tone and reusable notices", () => {
  const dialog = buildPaymentStatusDialog(copy, "Rejected", true);
  assert.equal(dialog.tone, "danger");
  assert.equal(dialog.title, "Update Rejected");
  assert.equal(dialog.confirmLabel, "Set Rejected");
  assert.equal(dialog.successMessage, "Updated Rejected");
  assert.equal(dialog.approvalRequiredMessage("req_1"), "Approval req_1");
  assert.equal(dialog.currentStatusLabel, "नाकारले");
});

test("status label helpers localize Marathi values", () => {
  assert.equal(paymentStatusLabel("Awaiting Verification", true), "पडताळणी प्रलंबित");
  assert.equal(bookingStatusLabel("Paid", true), "पूर्ण");
  assert.equal(bookingStatusLabel("Assigned", false), "Assigned");
});
