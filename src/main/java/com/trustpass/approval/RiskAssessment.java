package com.trustpass.approval;

import java.util.List;

public record RiskAssessment(int score, RiskLevel level, List<String> reasons, String provider) {
    public RiskAssessment {
        score = Math.max(0, Math.min(100, score));
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
        provider = provider == null ? "unknown" : provider;
    }
}

