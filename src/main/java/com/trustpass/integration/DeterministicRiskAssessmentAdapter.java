package com.trustpass.integration;

import com.trustpass.approval.RiskAssessment;
import com.trustpass.approval.RiskLevel;
import com.trustpass.policy.ActionType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "trustpass.integrations.openai", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DeterministicRiskAssessmentAdapter implements RiskAssessmentPort {
    @Override
    public RiskAssessment assess(Input input) {
        int score = 10;
        List<String> reasons = new ArrayList<>();

        if (input.amount().compareTo(new BigDecimal("1000")) >= 0) {
            score += 25;
            reasons.add("Financial value is at least 1,000 " + input.currency());
        }
        if (input.amount().compareTo(new BigDecimal("5000")) >= 0) {
            score += 25;
            reasons.add("Financial value exceeds the high-value threshold");
        }
        if (List.of(ActionType.VENDOR_PAYMENT, ActionType.CONTRACT_SIGNING, ActionType.DATA_EXPORT)
                .contains(input.actionType())) {
            score += 25;
            reasons.add("Action type has elevated financial, legal, or data impact");
        }
        if (input.agentReputation() < 60) {
            score += 20;
            reasons.add("Agent reputation is below the trusted threshold");
        }
        if (reasons.isEmpty()) {
            reasons.add("No elevated deterministic risk signals detected");
        }
        score = Math.min(score, 100);
        return new RiskAssessment(score, RiskLevel.fromScore(score), reasons, "deterministic-policy-v1");
    }
}

