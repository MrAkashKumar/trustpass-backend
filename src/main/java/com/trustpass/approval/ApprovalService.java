package com.trustpass.approval;

import com.trustpass.agent.AgentEntity;
import com.trustpass.agent.AgentNotFoundException;
import com.trustpass.agent.AgentRepository;
import com.trustpass.audit.AuditService;
import com.trustpass.integration.ActionExecutionPort;
import com.trustpass.integration.ApprovalNotificationPort;
import com.trustpass.integration.RiskAssessmentPort;
import com.trustpass.policy.ActionType;
import com.trustpass.policy.ApprovalChannel;
import com.trustpass.policy.PolicyEntity;
import com.trustpass.policy.PolicyRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalService {
    private final ApprovalRepository approvalRepository;
    private final AgentRepository agentRepository;
    private final PolicyRepository policyRepository;
    private final PolicyEngine policyEngine;
    private final RiskAssessmentPort riskAssessment;
    private final ApprovalNotificationPort notification;
    private final ActionExecutionPort execution;
    private final AuditService auditService;

    public ApprovalService(ApprovalRepository approvalRepository, AgentRepository agentRepository,
                           PolicyRepository policyRepository, PolicyEngine policyEngine,
                           RiskAssessmentPort riskAssessment, ApprovalNotificationPort notification,
                           ActionExecutionPort execution, AuditService auditService) {
        this.approvalRepository = approvalRepository;
        this.agentRepository = agentRepository;
        this.policyRepository = policyRepository;
        this.policyEngine = policyEngine;
        this.riskAssessment = riskAssessment;
        this.notification = notification;
        this.execution = execution;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<ApprovalRequestEntity> list(Optional<ApprovalStatus> status, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
        return status.map(value -> approvalRepository.findByStatusOrderByRequestedAtDesc(value, pageable))
                .orElseGet(() -> approvalRepository.findAllByOrderByRequestedAtDesc(pageable));
    }

    @Transactional(readOnly = true)
    public ApprovalRequestEntity get(UUID id) {
        return find(id);
    }

    @Transactional
    public ApprovalRequestEntity create(CreateCommand command, String actor) {
        Optional<ApprovalRequestEntity> existing = existingRequest(command);
        if (existing.isPresent()) {
            return existing.get();
        }

        AgentEntity agent = agentRepository.findById(command.agentId())
                .orElseThrow(() -> new AgentNotFoundException(command.agentId()));
        RiskAssessment risk = riskAssessment.assess(new RiskAssessmentPort.Input(command.actionType(),
                command.summary(), command.description(), command.target(), command.amount(), command.currency(),
                agent.getReputationScore()));
        Optional<PolicyEntity> policy = policyRepository
                .findFirstByActionTypeAndActiveTrueOrderByUpdatedAtDesc(command.actionType());
        PolicyDecision decision = policyEngine.evaluate(agent, policy, command.amount(), risk);

        ApprovalRequestEntity approval = new ApprovalRequestEntity(agent.getId(), agent.getName(),
                command.actionType(), command.summary(), command.description(), command.target(), command.amount(),
                command.currency(), risk, policy.map(PolicyEntity::getName).orElse("Fail-closed default"),
                decision.rationale(), policy.map(PolicyEntity::isIdentityVerificationRequired).orElse(true),
                policy.map(PolicyEntity::getApprovalChannel).orElse(ApprovalChannel.WEB), decision.outcome(),
                normalize(command.externalRequestId()), normalize(command.idempotencyKey()),
                normalize(command.actionPayloadHash()));
        approval = approvalRepository.save(approval);

        Map<String, Object> details = new HashMap<>();
        details.put("reference", approval.getReference());
        details.put("agentId", approval.getAgentId());
        details.put("actionType", approval.getActionType());
        details.put("amount", approval.getAmount());
        details.put("currency", approval.getCurrency());
        details.put("riskScore", approval.getRiskScore());
        details.put("status", approval.getStatus());
        if (approval.getExternalRequestId() != null) details.put("externalRequestId", approval.getExternalRequestId());
        if (approval.getActionPayloadHash() != null) details.put("actionPayloadHash", approval.getActionPayloadHash());
        auditService.append("APPROVAL", approval.getId(), "APPROVAL_REQUESTED", actor, details);

        if (approval.getStatus() == ApprovalStatus.PENDING) {
            Optional<String> notificationReference = notification.notifyApprover(approval, command.approverPhone());
            if (notificationReference.isPresent()) {
                approval.recordNotification(notificationReference.get());
                approval = approvalRepository.save(approval);
            }
        }
        return approval;
    }

    @Transactional
    public ApprovalRequestEntity decide(UUID id, DecisionType decision, String comment, ApprovalChannel channel,
                                        boolean identityVerified, String actor) {
        ApprovalRequestEntity approval = find(id);
        String proof = consentProof(id, decision, actor, channel, identityVerified);
        if (decision == DecisionType.APPROVE) {
            approval.approve(actor, comment, channel, identityVerified, proof);
        } else {
            approval.reject(actor, comment, channel, identityVerified, proof);
        }
        ApprovalRequestEntity saved = approvalRepository.save(approval);
        auditService.append("APPROVAL", id, "CONSENT_" + decision.name(), actor, Map.of(
                "channel", channel,
                "identityVerified", identityVerified,
                "proofHash", proof,
                "status", saved.getStatus()));
        return saved;
    }

    @Transactional
    public ApprovalRequestEntity execute(UUID id, String actor) {
        ApprovalRequestEntity approval = find(id);
        String executionReference = execution.execute(approval);
        approval.execute(executionReference);
        ApprovalRequestEntity saved = approvalRepository.save(approval);
        auditService.append("APPROVAL", id, "ACTION_EXECUTED", actor, Map.of(
                "executionReference", executionReference,
                "status", saved.getStatus()));
        return saved;
    }

    @Transactional
    public ApprovalRequestEntity recordExternalExecution(UUID id, String executionReference, String actor) {
        ApprovalRequestEntity approval = find(id);
        if (approval.getExternalRequestId() == null) {
            throw new IllegalStateException("Only agent-originated approvals can receive external execution results");
        }
        approval.execute(executionReference);
        ApprovalRequestEntity saved = approvalRepository.save(approval);
        auditService.append("APPROVAL", id, "EXTERNAL_ACTION_EXECUTED", actor, Map.of(
                "executionReference", executionReference,
                "status", saved.getStatus()));
        return saved;
    }

    @Transactional
    public ApprovalRequestEntity decideFromVoice(UUID id, DecisionType decision, boolean identityVerified,
                                                 String conversationId) {
        return decide(id, decision, "Decision captured by verified voice workflow", ApprovalChannel.VOICE,
                identityVerified, "elevenlabs:" + conversationId);
    }

    private ApprovalRequestEntity find(UUID id) {
        return approvalRepository.findById(id).orElseThrow(() -> new ApprovalNotFoundException(id));
    }

    private Optional<ApprovalRequestEntity> existingRequest(CreateCommand command) {
        Optional<ApprovalRequestEntity> byIdempotency = Optional.ofNullable(normalize(command.idempotencyKey()))
                .flatMap(approvalRepository::findByIdempotencyKey);
        if (byIdempotency.isPresent()) {
            validateSamePayloadHash(byIdempotency.get(), command.actionPayloadHash());
            return byIdempotency;
        }
        Optional<ApprovalRequestEntity> byExternalRequest = Optional.ofNullable(normalize(command.externalRequestId()))
                .flatMap(approvalRepository::findByExternalRequestId);
        byExternalRequest.ifPresent(approval -> validateSamePayloadHash(approval, command.actionPayloadHash()));
        return byExternalRequest;
    }

    private void validateSamePayloadHash(ApprovalRequestEntity existing, String requestedPayloadHash) {
        String requested = normalize(requestedPayloadHash);
        String existingHash = normalize(existing.getActionPayloadHash());
        if (requested != null && existingHash != null && !existingHash.equalsIgnoreCase(requested)) {
            throw new IllegalStateException("Idempotent request payload hash does not match the existing approval");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String consentProof(UUID id, DecisionType decision, String actor, ApprovalChannel channel,
                                boolean identityVerified) {
        return AuditService.sha256(String.join("|", id.toString(), decision.name(), actor, channel.name(),
                Boolean.toString(identityVerified), Instant.now().toString()));
    }

    public record CreateCommand(
            UUID agentId,
            ActionType actionType,
            String summary,
            String description,
            String target,
            BigDecimal amount,
            String currency,
            String approverPhone,
            String externalRequestId,
            String idempotencyKey,
            String actionPayloadHash
    ) {}
}
