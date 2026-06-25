package com.trustpass.audit;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
    private static final String GENESIS_HASH = "0".repeat(64);
    private final AuditEventRepository repository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public synchronized AuditEventEntity append(String aggregateType, UUID aggregateId, String eventType,
                                                 String actor, Map<String, ?> details) {
        String previousHash = repository.findTopByOrderByIdDesc()
                .map(AuditEventEntity::getEventHash)
                .orElse(GENESIS_HASH);
        Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        String detailJson = toJson(details);
        String canonical = String.join("|", previousHash, aggregateType, aggregateId.toString(), eventType,
                actor, detailJson, createdAt.toString());
        String eventHash = sha256(canonical);
        return repository.save(new AuditEventEntity(aggregateType, aggregateId, eventType, actor,
                detailJson, previousHash, eventHash, createdAt));
    }

    private String toJson(Map<String, ?> details) {
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize audit details", exception);
        }
    }

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    @Transactional(readOnly = true)
    public Verification verifyChain() {
        String expectedPrevious = GENESIS_HASH;
        long checked = 0;
        for (AuditEventEntity event : repository.findAllByOrderByIdAsc()) {
            checked++;
            String canonical = String.join("|", expectedPrevious, event.getAggregateType(),
                    event.getAggregateId().toString(), event.getEventType(), event.getActor(), event.getDetails(),
                    event.getCreatedAt().toString());
            String expectedHash = sha256(canonical);
            if (!expectedPrevious.equals(event.getPreviousHash()) || !expectedHash.equals(event.getEventHash())) {
                return new Verification(false, checked, event.getId());
            }
            expectedPrevious = event.getEventHash();
        }
        return new Verification(true, checked, null);
    }

    public record Verification(boolean valid, long checkedEvents, Long brokenAtEventId) {}
}
