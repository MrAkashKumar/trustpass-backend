package com.trustpass.dashboard;

import com.trustpass.agent.AgentRepository;
import com.trustpass.agent.AgentStatus;
import com.trustpass.approval.ApprovalApi;
import com.trustpass.approval.ApprovalRepository;
import com.trustpass.approval.ApprovalStatus;
import com.trustpass.policy.PolicyRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardApi {
    private final ApprovalRepository approvals;
    private final AgentRepository agents;
    private final PolicyRepository policies;

    public DashboardApi(ApprovalRepository approvals, AgentRepository agents, PolicyRepository policies) {
        this.approvals = approvals;
        this.agents = agents;
        this.policies = policies;
    }

    @GetMapping
    public Response dashboard() {
        long activeAgents = agents.findAll().stream().filter(agent -> agent.getStatus() == AgentStatus.ACTIVE).count();
        List<ApprovalApi.Response> recent = approvals.findAllByOrderByRequestedAtDesc(PageRequest.of(0, 5))
                .getContent().stream().map(ApprovalApi.Response::from).toList();
        return new Response(approvals.countByStatus(ApprovalStatus.PENDING),
                approvals.countByStatus(ApprovalStatus.EXECUTED),
                approvals.countByStatus(ApprovalStatus.DENIED) + approvals.countByStatus(ApprovalStatus.REJECTED),
                approvals.count(), activeAgents, policies.count(), recent);
    }

    public record Response(long pendingApprovals, long executedActions, long preventedActions,
                           long totalRequests, long activeAgents, long policies, List<ApprovalApi.Response> recent) {}
}
