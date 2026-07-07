import test from "node:test";
import assert from "node:assert/strict";
import { buildDeadLetterReplayRequest, buildMakerCheckerApprovalDialog } from "../lib/operationsActions.js";

test("buildMakerCheckerApprovalDialog returns consistent confirmation content", () => {
  assert.deepEqual(buildMakerCheckerApprovalDialog("maker_1"), {
    requestId: "maker_1",
    title: "Approve this maker-checker request?",
    message: "This will execute the pending high-risk action immediately.",
    confirmLabel: "Approve request",
    tone: "success",
  });
});

test("buildDeadLetterReplayRequest preserves operations audit note", () => {
  assert.deepEqual(buildDeadLetterReplayRequest("dead_1"), {
    deadLetterId: "dead_1",
    note: "Replay requested from Operations Brain",
  });
});
