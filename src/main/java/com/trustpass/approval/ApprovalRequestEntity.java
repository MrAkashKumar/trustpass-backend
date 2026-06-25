package com.trustpass.approval;

import com.trustpass.policy.ActionType;
import com.trustpass.policy.ApprovalChannel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "approval_requests")
public class ApprovalRequestEntity {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 30)
    private String reference;

    @Column(nullable = false)
    private UUID agentId;

    @Column(nullable = false, length = 120)
    private String agentName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ActionType actionType;

    @Column(nullable = false, length = 180)
    private String summary;

    @Column(nullable = false, length = 1500)
    private String description;

    @Column(nullable = false, length = 180)
    private String target;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Column(nullable = false, length = 2000)
    private String riskReasons;

    @Column(nullable = false, length = 40)
    private String riskProvider;

    @Column(length = 120)
    private String policyName;

    @Column(nullable = false, length = 500)
    private String policyRationale;

    @Column(nullable = false)
    private boolean identityVerificationRequired;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalChannel requiredChannel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApprovalStatus status;

    @Column(nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant decidedAt;
    private Instant executedAt;

    @Column(length = 120)
    private String decisionBy;

    @Column(length = 500)
    private String decisionComment;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ApprovalChannel decisionChannel;

    @Column(nullable = false)
    private boolean identityVerified;

    @Column(length = 64)
    private String consentProofHash;

    @Column(length = 180)
    private String notificationReference;

    @Column(length = 180)
    private String executionReference;

    @Column(length = 120)
    private String externalRequestId;

    @Column(length = 120)
    private String idempotencyKey;

    @Column(length = 64)
    private String actionPayloadHash;

    @Version
    private long version;

    protected ApprovalRequestEntity() {}

    public ApprovalRequestEntity(UUID agentId, String agentName, ActionType actionType, String summary,
                                 String description, String target, BigDecimal amount, String currency,
                                 RiskAssessment risk, String policyName, String policyRationale,
                                 boolean identityVerificationRequired, ApprovalChannel requiredChannel,
                                 PolicyDecision.Outcome outcome, String externalRequestId, String idempotencyKey,
                                 String actionPayloadHash) {
        this.id = UUID.randomUUID();
        this.reference = "TP-" + id.toString().substring(0, 8).toUpperCase();
        this.agentId = agentId;
        this.agentName = agentName;
        this.actionType = actionType;
        this.summary = summary;
        this.description = description;
        this.target = target;
        this.amount = amount;
        this.currency = currency.toUpperCase();
        this.riskScore = risk.score();
        this.riskLevel = risk.level();
        this.riskReasons = String.join("\n", risk.reasons());
        this.riskProvider = risk.provider();
        this.policyName = policyName;
        this.policyRationale = policyRationale;
        this.identityVerificationRequired = identityVerificationRequired;
        this.requiredChannel = requiredChannel;
        this.status = switch (outcome) {
            case AUTO_APPROVE -> ApprovalStatus.AUTO_APPROVED;
            case REQUIRE_APPROVAL -> ApprovalStatus.PENDING;
            case DENY -> ApprovalStatus.DENIED;
        };
        this.externalRequestId = externalRequestId;
        this.idempotencyKey = idempotencyKey;
        this.actionPayloadHash = actionPayloadHash;
        this.requestedAt = Instant.now();
        this.expiresAt = this.requestedAt.plusSeconds(24 * 60 * 60);
    }

    public void recordNotification(String reference) {
        this.notificationReference = reference;
    }

    public void approve(String actor, String comment, ApprovalChannel channel, boolean identityVerified,
                        String consentProofHash) {
        requirePending();
        if (identityVerificationRequired && !identityVerified) {
            throw new IllegalStateException("Identity verification is required by policy");
        }
        this.status = ApprovalStatus.APPROVED;
        this.decidedAt = Instant.now();
        this.decisionBy = actor;
        this.decisionComment = comment;
        this.decisionChannel = channel;
        this.identityVerified = identityVerified;
        this.consentProofHash = consentProofHash;
    }

    public void reject(String actor, String comment, ApprovalChannel channel, boolean identityVerified,
                       String consentProofHash) {
        requirePending();
        this.status = ApprovalStatus.REJECTED;
        this.decidedAt = Instant.now();
        this.decisionBy = actor;
        this.decisionComment = comment;
        this.decisionChannel = channel;
        this.identityVerified = identityVerified;
        this.consentProofHash = consentProofHash;
    }

    public void execute(String reference) {
        if (status != ApprovalStatus.APPROVED && status != ApprovalStatus.AUTO_APPROVED) {
            throw new IllegalStateException("Only approved actions can be executed");
        }
        this.status = ApprovalStatus.EXECUTED;
        this.executionReference = reference;
        this.executedAt = Instant.now();
    }

    private void requirePending() {
        if (status != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Only pending requests can receive a decision");
        }
        if (expiresAt.isBefore(Instant.now())) {
            this.status = ApprovalStatus.EXPIRED;
            throw new IllegalStateException("Approval request has expired");
        }
    }

    public UUID getId() { return id; }
    public String getReference() { return reference; }
    public UUID getAgentId() { return agentId; }
    public String getAgentName() { return agentName; }
    public ActionType getActionType() { return actionType; }
    public String getSummary() { return summary; }
    public String getDescription() { return description; }
    public String getTarget() { return target; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public int getRiskScore() { return riskScore; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public List<String> getRiskReasons() { return riskReasons.isBlank() ? List.of() : List.of(riskReasons.split("\n")); }
    public String getRiskProvider() { return riskProvider; }
    public String getPolicyName() { return policyName; }
    public String getPolicyRationale() { return policyRationale; }
    public boolean isIdentityVerificationRequired() { return identityVerificationRequired; }
    public ApprovalChannel getRequiredChannel() { return requiredChannel; }
    public ApprovalStatus getStatus() { return status; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getDecidedAt() { return decidedAt; }
    public Instant getExecutedAt() { return executedAt; }
    public String getDecisionBy() { return decisionBy; }
    public String getDecisionComment() { return decisionComment; }
    public ApprovalChannel getDecisionChannel() { return decisionChannel; }
    public boolean isIdentityVerified() { return identityVerified; }
    public String getConsentProofHash() { return consentProofHash; }
    public String getNotificationReference() { return notificationReference; }
    public String getExecutionReference() { return executionReference; }
    public String getExternalRequestId() { return externalRequestId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getActionPayloadHash() { return actionPayloadHash; }
    public long getVersion() { return version; }
}
