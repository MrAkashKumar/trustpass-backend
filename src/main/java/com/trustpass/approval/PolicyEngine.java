package com.trustpass.approval;

import com.trustpass.agent.AgentEntity;
import com.trustpass.agent.AgentStatus;
import com.trustpass.policy.PolicyEntity;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class PolicyEngine {
    public PolicyDecision evaluate(AgentEntity agent, Optional<PolicyEntity> policy, BigDecimal amount,
                                   RiskAssessment risk) {
        if (agent.getStatus() != AgentStatus.ACTIVE) {
            return deny("Agent authority is not active");
        }
        if (amount.compareTo(agent.getAuthorityLimit()) > 0) {
            return deny("Action exceeds the agent's delegated authority limit");
        }
        if (policy.isEmpty()) {
            return requireApproval("No active policy exists; fail-closed human review is required");
        }

        PolicyEntity rule = policy.get();
        if (!rule.isPermitted()) {
            return deny("The active enterprise policy forbids this action type");
        }
        if (amount.compareTo(rule.getHardLimit()) > 0) {
            return deny("Action exceeds the policy hard limit");
        }
        if (amount.compareTo(rule.getAutoApproveLimit()) <= 0
                && risk.score() < rule.getHumanApprovalRiskScore()) {
            return new PolicyDecision(PolicyDecision.Outcome.AUTO_APPROVE,
                    "Inside delegated amount and risk thresholds");
        }
        return requireApproval("Amount or risk threshold requires verified human consent");
    }

    private PolicyDecision deny(String rationale) {
        return new PolicyDecision(PolicyDecision.Outcome.DENY, rationale);
    }

    private PolicyDecision requireApproval(String rationale) {
        return new PolicyDecision(PolicyDecision.Outcome.REQUIRE_APPROVAL, rationale);
    }
}

