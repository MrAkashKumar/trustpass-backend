package com.trustpass.config;

import com.trustpass.agent.AgentEntity;
import com.trustpass.agent.AgentRepository;
import com.trustpass.agent.AgentType;
import com.trustpass.approval.ApprovalRequestEntity;
import com.trustpass.approval.ApprovalService;
import com.trustpass.approval.ApprovalStatus;
import com.trustpass.policy.ActionType;
import com.trustpass.policy.ApprovalChannel;
import com.trustpass.policy.PolicyEntity;
import com.trustpass.policy.PolicyRepository;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DemoDataConfiguration {
    @Bean
    ApplicationRunner seedDemoData(AgentRepository agents, PolicyRepository policies, ApprovalService approvals,
                                   @Value("${trustpass.seed.enabled:true}") boolean enabled) {
        return arguments -> {
            if (!enabled || agents.count() > 0 || policies.count() > 0) return;

            AgentEntity travel = agents.save(new AgentEntity("Atlas Travel Agent",
                    "Finds policy-compliant travel and prepares bookings for consent.", AgentType.TRAVEL,
                    "Corporate Travel", new BigDecimal("10000")));
            AgentEntity finance = agents.save(new AgentEntity("Ledger Finance Agent",
                    "Reviews invoices and prepares governed vendor payments.", AgentType.FINANCE,
                    "Finance Operations", new BigDecimal("100000")));
            AgentEntity procurement = agents.save(new AgentEntity("Nova Procurement Agent",
                    "Negotiates software and supplier purchases within delegated authority.", AgentType.PROCUREMENT,
                    "Strategic Sourcing", new BigDecimal("50000")));

            policies.save(new PolicyEntity("Routine purchases", "Auto-approve small purchases; review larger ones.",
                    ActionType.PURCHASE, true, new BigDecimal("100"), new BigDecimal("5000"), 45,
                    false, ApprovalChannel.WEB));
            policies.save(new PolicyEntity("Corporate travel", "Human consent for material travel bookings.",
                    ActionType.TRAVEL_BOOKING, true, new BigDecimal("500"), new BigDecimal("10000"), 40,
                    true, ApprovalChannel.ANY));
            policies.save(new PolicyEntity("Vendor payments", "Identity-verified approval for vendor payments.",
                    ActionType.VENDOR_PAYMENT, true, BigDecimal.ZERO, new BigDecimal("50000"), 20,
                    true, ApprovalChannel.ANY));
            policies.save(new PolicyEntity("Contract authority", "Contracts above the delegated ceiling are denied.",
                    ActionType.CONTRACT_SIGNING, true, BigDecimal.ZERO, new BigDecimal("25000"), 10,
                    true, ApprovalChannel.WEB));

            ApprovalRequestEntity purchase = approvals.create(new ApprovalService.CreateCommand(procurement.getId(),
                    ActionType.PURCHASE, "Purchase design review tool", "Annual team plan for the design group.",
                    "Frameboard", new BigDecimal("49"), "USD", null), "system-seed");
            if (purchase.getStatus() == ApprovalStatus.AUTO_APPROVED) {
                approvals.execute(purchase.getId(), "system-seed");
            }
            approvals.create(new ApprovalService.CreateCommand(travel.getId(), ActionType.TRAVEL_BOOKING,
                    "Singapore to Tokyo flight", "Singapore Airlines return flight for customer workshop.",
                    "Singapore Airlines", new BigDecimal("850"), "SGD", null), "system-seed");
            approvals.create(new ApprovalService.CreateCommand(finance.getId(), ActionType.VENDOR_PAYMENT,
                    "Approve supplier milestone", "Second milestone payment after acceptance review.",
                    "Orbit Systems Pte Ltd", new BigDecimal("12500"), "SGD", null), "system-seed");
            approvals.create(new ApprovalService.CreateCommand(procurement.getId(), ActionType.CONTRACT_SIGNING,
                    "Sign enterprise data contract", "Three-year analytics platform agreement.",
                    "Northstar Analytics", new BigDecimal("30000"), "USD", null), "system-seed");
        };
    }
}

