package com.trustpass.audit;

import com.trustpass.shared.PagedResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-events")
@PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
public class AuditApi {
    private final AuditEventRepository repository;
    private final AuditService auditService;

    public AuditApi(AuditEventRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @GetMapping
    public PagedResponse<Response> list(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "30") int size) {
        var pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
        return PagedResponse.from(repository.findAllByOrderByIdDesc(pageable), Response::from);
    }

    @GetMapping("/verify")
    public AuditService.Verification verify() {
        return auditService.verifyChain();
    }

    public record Response(Long id, String aggregateType, UUID aggregateId, String eventType, String actor,
                           String details, String previousHash, String eventHash, Instant createdAt) {
        static Response from(AuditEventEntity event) {
            return new Response(event.getId(), event.getAggregateType(), event.getAggregateId(),
                    event.getEventType(), event.getActor(), event.getDetails(), event.getPreviousHash(),
                    event.getEventHash(), event.getCreatedAt());
        }
    }
}
