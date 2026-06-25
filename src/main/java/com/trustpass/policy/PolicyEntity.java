package com.trustpass.policy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "policies")
public class PolicyEntity {
    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ActionType actionType;

    @Column(nullable = false)
    private boolean permitted;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal autoApproveLimit;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal hardLimit;

    @Column(nullable = false)
    private int humanApprovalRiskScore;

    @Column(nullable = false)
    private boolean identityVerificationRequired;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalChannel approvalChannel;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected PolicyEntity() {}

    public PolicyEntity(String name, String description, ActionType actionType, boolean permitted,
                        BigDecimal autoApproveLimit, BigDecimal hardLimit, int humanApprovalRiskScore,
                        boolean identityVerificationRequired, ApprovalChannel approvalChannel) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.description = description;
        this.actionType = actionType;
        this.permitted = permitted;
        this.autoApproveLimit = autoApproveLimit;
        this.hardLimit = hardLimit;
        this.humanApprovalRiskScore = humanApprovalRiskScore;
        this.identityVerificationRequired = identityVerificationRequired;
        this.approvalChannel = approvalChannel;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(String name, String description, boolean permitted, BigDecimal autoApproveLimit,
                       BigDecimal hardLimit, int humanApprovalRiskScore, boolean identityVerificationRequired,
                       ApprovalChannel approvalChannel, boolean active) {
        this.name = name;
        this.description = description;
        this.permitted = permitted;
        this.autoApproveLimit = autoApproveLimit;
        this.hardLimit = hardLimit;
        this.humanApprovalRiskScore = humanApprovalRiskScore;
        this.identityVerificationRequired = identityVerificationRequired;
        this.approvalChannel = approvalChannel;
        this.active = active;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public ActionType getActionType() { return actionType; }
    public boolean isPermitted() { return permitted; }
    public BigDecimal getAutoApproveLimit() { return autoApproveLimit; }
    public BigDecimal getHardLimit() { return hardLimit; }
    public int getHumanApprovalRiskScore() { return humanApprovalRiskScore; }
    public boolean isIdentityVerificationRequired() { return identityVerificationRequired; }
    public ApprovalChannel getApprovalChannel() { return approvalChannel; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}

