package com.trustpass.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String aggregateType;

    @Column(nullable = false)
    private UUID aggregateId;

    @Column(nullable = false, length = 60)
    private String eventType;

    @Column(nullable = false, length = 120)
    private String actor;

    @Column(nullable = false, length = 4000)
    private String details;

    @Column(nullable = false, length = 64)
    private String previousHash;

    @Column(nullable = false, unique = true, length = 64)
    private String eventHash;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditEventEntity() {}

    public AuditEventEntity(String aggregateType, UUID aggregateId, String eventType, String actor,
                            String details, String previousHash, String eventHash, Instant createdAt) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.actor = actor;
        this.details = details;
        this.previousHash = previousHash;
        this.eventHash = eventHash;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getAggregateType() { return aggregateType; }
    public UUID getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getActor() { return actor; }
    public String getDetails() { return details; }
    public String getPreviousHash() { return previousHash; }
    public String getEventHash() { return eventHash; }
    public Instant getCreatedAt() { return createdAt; }
}

