package com.trustpass.approval;

public record PolicyDecision(Outcome outcome, String rationale) {
    public enum Outcome {
        AUTO_APPROVE,
        REQUIRE_APPROVAL,
        DENY
    }
}

