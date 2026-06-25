package com.trustpass.policy;

import com.trustpass.audit.AuditService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.net.URI;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/v1/policies")
public class PolicyApi {
    private final PolicyRepository repository;
    private final AuditService auditService;

    public PolicyApi(PolicyRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @GetMapping
    public List<Response> list() {
        return repository.findAll().stream().map(Response::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Response> create(@Valid @RequestBody CreateRequest request, Principal principal) {
        validateLimits(request.autoApproveLimit(), request.hardLimit());
        PolicyEntity saved = repository.save(new PolicyEntity(request.name(), request.description(),
                request.actionType(), request.permitted(), request.autoApproveLimit(), request.hardLimit(),
                request.humanApprovalRiskScore(), request.identityVerificationRequired(), request.approvalChannel()));
        auditService.append("POLICY", saved.getId(), "POLICY_CREATED", principal.getName(),
                java.util.Map.of("name", saved.getName(), "actionType", saved.getActionType(),
                        "hardLimit", saved.getHardLimit()));
        return ResponseEntity.created(URI.create("/api/v1/policies/" + saved.getId())).body(Response.from(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Response update(@PathVariable UUID id, @Valid @RequestBody UpdateRequest request, Principal principal) {
        validateLimits(request.autoApproveLimit(), request.hardLimit());
        PolicyEntity policy = repository.findById(id).orElseThrow(() -> new PolicyNotFoundException(id));
        policy.update(request.name(), request.description(), request.permitted(), request.autoApproveLimit(),
                request.hardLimit(), request.humanApprovalRiskScore(), request.identityVerificationRequired(),
                request.approvalChannel(), request.active());
        PolicyEntity saved = repository.save(policy);
        auditService.append("POLICY", saved.getId(), "POLICY_UPDATED", principal.getName(),
                java.util.Map.of("active", saved.isActive(), "hardLimit", saved.getHardLimit(),
                        "riskThreshold", saved.getHumanApprovalRiskScore()));
        return Response.from(saved);
    }

    private void validateLimits(BigDecimal autoApproveLimit, BigDecimal hardLimit) {
        if (autoApproveLimit.compareTo(hardLimit) > 0) {
            throw new IllegalArgumentException("Auto-approval limit cannot exceed the hard limit");
        }
    }

    public record CreateRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 500) String description,
            @NotNull ActionType actionType,
            boolean permitted,
            @NotNull @DecimalMin("0.00") BigDecimal autoApproveLimit,
            @NotNull @DecimalMin("0.00") BigDecimal hardLimit,
            @Min(0) @Max(100) int humanApprovalRiskScore,
            boolean identityVerificationRequired,
            @NotNull ApprovalChannel approvalChannel
    ) {}

    public record UpdateRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 500) String description,
            boolean permitted,
            @NotNull @DecimalMin("0.00") BigDecimal autoApproveLimit,
            @NotNull @DecimalMin("0.00") BigDecimal hardLimit,
            @Min(0) @Max(100) int humanApprovalRiskScore,
            boolean identityVerificationRequired,
            @NotNull ApprovalChannel approvalChannel,
            boolean active
    ) {}

    public record Response(
            UUID id,
            String name,
            String description,
            ActionType actionType,
            boolean permitted,
            BigDecimal autoApproveLimit,
            BigDecimal hardLimit,
            int humanApprovalRiskScore,
            boolean identityVerificationRequired,
            ApprovalChannel approvalChannel,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        static Response from(PolicyEntity policy) {
            return new Response(policy.getId(), policy.getName(), policy.getDescription(), policy.getActionType(),
                    policy.isPermitted(), policy.getAutoApproveLimit(), policy.getHardLimit(),
                    policy.getHumanApprovalRiskScore(), policy.isIdentityVerificationRequired(),
                    policy.getApprovalChannel(), policy.isActive(), policy.getCreatedAt(), policy.getUpdatedAt());
        }
    }
}
