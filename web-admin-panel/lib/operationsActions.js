export function buildMakerCheckerApprovalDialog(requestId) {
  return {
    requestId,
    title: "Approve this maker-checker request?",
    message: "This will execute the pending high-risk action immediately.",
    confirmLabel: "Approve request",
    tone: "success",
  };
}

export function buildDeadLetterReplayRequest(deadLetterId) {
  return {
    deadLetterId,
    note: "Replay requested from Operations Brain",
  };
}
