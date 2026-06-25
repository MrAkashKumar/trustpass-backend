package com.trustpass.approval;

import com.trustpass.policy.ActionType;
import com.trustpass.policy.ApprovalChannel;
import com.trustpass.shared.PagedResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.net.URI;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/approvals")
public class ApprovalApi {
    private final ApprovalService service;

    public ApprovalApi(ApprovalService service) {
        this.service = service;
    }

    @GetMapping
    public PagedResponse<Response> list(@RequestParam Optional<ApprovalStatus> status,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return PagedResponse.from(service.list(status, page, size), Response::from);
    }

    @GetMapping("/{id}")
    public Response get(@PathVariable UUID id) {
        return Response.from(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','APPROVER')")
    public ResponseEntity<Response> create(@Valid @RequestBody CreateRequest request, Principal principal) {
        ApprovalRequestEntity saved = service.create(new ApprovalService.CreateCommand(request.agentId(),
                request.actionType(), request.summary(), request.description(), request.target(), request.amount(),
                request.currency(), request.approverPhone(), null, null, null), principal.getName());
        return ResponseEntity.created(URI.create("/api/v1/approvals/" + saved.getId())).body(Response.from(saved));
    }

    @PostMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('ADMIN','APPROVER')")
    public Response decide(@PathVariable UUID id, @Valid @RequestBody DecisionRequest request,
                           Principal principal) {
        return Response.from(service.decide(id, request.decision(), request.comment(), ApprovalChannel.WEB,
                true, principal.getName()));
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("hasRole('ADMIN')")
    public Response execute(@PathVariable UUID id, Principal principal) {
        return Response.from(service.execute(id, principal.getName()));
    }

    public record CreateRequest(
            @NotNull UUID agentId,
            @NotNull ActionType actionType,
            @NotBlank @Size(max = 180) String summary,
            @NotBlank @Size(max = 1500) String description,
            @NotBlank @Size(max = 180) String target,
            @NotNull @DecimalMin("0.00") BigDecimal amount,
            @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
            @Pattern(regexp = "^\\+[1-9][0-9]{7,14}$", message = "must be an E.164 phone number") String approverPhone
    ) {}

    public record DecisionRequest(
            @NotNull DecisionType decision,
            @NotBlank @Size(max = 500) String comment
    ) {}

    public record Response(
            UUID id,
            String reference,
            UUID agentId,
            String agentName,
            ActionType actionType,
            String summary,
            String description,
            String target,
            BigDecimal amount,
            String currency,
            int riskScore,
            RiskLevel riskLevel,
            List<String> riskReasons,
            String riskProvider,
            String policyName,
            String policyRationale,
            boolean identityVerificationRequired,
            ApprovalChannel requiredChannel,
            ApprovalStatus status,
            Instant requestedAt,
            Instant expiresAt,
            Instant decidedAt,
            Instant executedAt,
            String decisionBy,
            String decisionComment,
            ApprovalChannel decisionChannel,
            boolean identityVerified,
            String consentProofHash,
            String notificationReference,
            String executionReference,
            String externalRequestId,
            String actionPayloadHash
    ) {
        public static Response from(ApprovalRequestEntity approval) {
            return new Response(approval.getId(), approval.getReference(), approval.getAgentId(),
                    approval.getAgentName(), approval.getActionType(), approval.getSummary(),
                    approval.getDescription(), approval.getTarget(), approval.getAmount(), approval.getCurrency(),
                    approval.getRiskScore(), approval.getRiskLevel(), approval.getRiskReasons(),
                    approval.getRiskProvider(), approval.getPolicyName(), approval.getPolicyRationale(),
                    approval.isIdentityVerificationRequired(), approval.getRequiredChannel(), approval.getStatus(),
                    approval.getRequestedAt(), approval.getExpiresAt(), approval.getDecidedAt(),
                    approval.getExecutedAt(), approval.getDecisionBy(), approval.getDecisionComment(),
                    approval.getDecisionChannel(), approval.isIdentityVerified(), approval.getConsentProofHash(),
                    approval.getNotificationReference(), approval.getExecutionReference(),
                    approval.getExternalRequestId(), approval.getActionPayloadHash());
        }
    }
}
