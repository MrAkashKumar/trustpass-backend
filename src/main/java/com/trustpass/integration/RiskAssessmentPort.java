package com.trustpass.integration;

import com.trustpass.approval.RiskAssessment;
import com.trustpass.policy.ActionType;
import java.math.BigDecimal;

public interface RiskAssessmentPort {
    RiskAssessment assess(Input input);

    record Input(ActionType actionType, String summary, String description, String target,
                 BigDecimal amount, String currency, int agentReputation) {}
}

