package com.trustpass.agent;

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
@Table(name = "agents")
public class AgentEntity {
    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AgentType type;

    @Column(nullable = false, length = 120)
    private String owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AgentStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal authorityLimit;

    @Column(nullable = false)
    private int reputationScore;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected AgentEntity() {}

    public AgentEntity(String name, String description, AgentType type, String owner, BigDecimal authorityLimit) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.description = description;
        this.type = type;
        this.owner = owner;
        this.authorityLimit = authorityLimit;
        this.status = AgentStatus.ACTIVE;
        this.reputationScore = 80;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(String name, String description, AgentType type, String owner, BigDecimal authorityLimit, AgentStatus status) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.owner = owner;
        this.authorityLimit = authorityLimit;
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public AgentType getType() { return type; }
    public String getOwner() { return owner; }
    public AgentStatus getStatus() { return status; }
    public BigDecimal getAuthorityLimit() { return authorityLimit; }
    public int getReputationScore() { return reputationScore; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}

