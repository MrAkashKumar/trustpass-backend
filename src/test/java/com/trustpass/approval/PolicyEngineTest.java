package com.trustpass.approval;

import static org.assertj.core.api.Assertions.assertThat;

import com.trustpass.agent.AgentEntity;
import com.trustpass.agent.AgentType;
import com.trustpass.policy.ActionType;
import com.trustpass.policy.ApprovalChannel;
import com.trustpass.policy.PolicyEntity;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PolicyEngineTest {
    private final PolicyEngine engine = new PolicyEngine();
    private final AgentEntity agent = new AgentEntity("Finance Agent", "Test agent", AgentType.FINANCE,
            "Finance", new BigDecimal("10000"));
    private final PolicyEntity policy = new PolicyEntity("Payments", "Test policy", ActionType.VENDOR_PAYMENT,
            true, new BigDecimal("100"), new BigDecimal("5000"), 50, true, ApprovalChannel.WEB);

    @Test
    void autoApprovesOnlyInsideAmountAndRiskThresholds() {
        PolicyDecision decision = engine.evaluate(agent, Optional.of(policy), new BigDecimal("50"),
                new RiskAssessment(20, RiskLevel.LOW, List.of("low"), "test"));
        assertThat(decision.outcome()).isEqualTo(PolicyDecision.Outcome.AUTO_APPROVE);
    }

    @Test
    void requiresHumanWhenRiskIsElevated() {
        PolicyDecision decision = engine.evaluate(agent, Optional.of(policy), new BigDecimal("50"),
                new RiskAssessment(70, RiskLevel.HIGH, List.of("high"), "test"));
        assertThat(decision.outcome()).isEqualTo(PolicyDecision.Outcome.REQUIRE_APPROVAL);
    }

    @Test
    void deniesBeyondHardLimit() {
        PolicyDecision decision = engine.evaluate(agent, Optional.of(policy), new BigDecimal("6000"),
                new RiskAssessment(20, RiskLevel.LOW, List.of("low"), "test"));
        assertThat(decision.outcome()).isEqualTo(PolicyDecision.Outcome.DENY);
    }

    @Test
    void failsClosedWhenNoPolicyExists() {
        PolicyDecision decision = engine.evaluate(agent, Optional.empty(), new BigDecimal("10"),
                new RiskAssessment(5, RiskLevel.LOW, List.of("low"), "test"));
        assertThat(decision.outcome()).isEqualTo(PolicyDecision.Outcome.REQUIRE_APPROVAL);
    }
}

