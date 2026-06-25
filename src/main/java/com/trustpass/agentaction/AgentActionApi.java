package com.trustpass.agentaction;

import com.trustpass.approval.ApprovalApi;
import com.trustpass.approval.ApprovalRequestEntity;
import com.trustpass.approval.ApprovalService;
import com.trustpass.policy.ActionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.net.URI;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agent-actions")
@PreAuthorize("hasRole('AGENT_CLIENT')")
public class AgentActionApi {
    private final ApprovalService approvals;

    public AgentActionApi(ApprovalService approvals) {
        this.approvals = approvals;
    }

    @PostMapping
    public ResponseEntity<ApprovalApi.Response> create(@Valid @RequestBody CreateRequest request,
                                                       @RequestHeader(name = "Idempotency-Key", required = false)
                                                       String idempotencyKey,
                                                       Principal principal) {
        ApprovalRequestEntity saved = approvals.create(new ApprovalService.CreateCommand(request.agentId(),
                request.actionType(), request.summary(), request.description(), request.target(), request.amount(),
                request.currency(), request.approverPhone(), request.externalRequestId(), idempotencyKey,
                request.actionPayloadHash()), principal.getName());
        return ResponseEntity.created(URI.create("/api/v1/agent-actions/" + saved.getId()))
                .body(ApprovalApi.Response.from(saved));
    }

    @GetMapping("/{id}")
    public ApprovalApi.Response get(@PathVariable UUID id) {
        return ApprovalApi.Response.from(approvals.get(id));
    }

    @PostMapping("/{id}/execution-result")
    public ApprovalApi.Response executionResult(@PathVariable UUID id,
                                                @Valid @RequestBody ExecutionResultRequest request,
                                                Principal principal) {
        return ApprovalApi.Response.from(approvals.recordExternalExecution(id, request.executionReference(),
                principal.getName()));
    }

    public record CreateRequest(
            @NotBlank @Size(max = 120) @Pattern(regexp = "^[A-Za-z0-9._:-]+$") String externalRequestId,
            @NotBlank @Pattern(regexp = "^[A-Fa-f0-9]{64}$") String actionPayloadHash,
            @NotNull UUID agentId,
            @NotNull ActionType actionType,
            @NotBlank @Size(max = 180) String summary,
            @NotBlank @Size(max = 1500) String description,
            @NotBlank @Size(max = 180) String target,
            @NotNull @DecimalMin("0.00") BigDecimal amount,
            @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
            @Pattern(regexp = "^\\+[1-9][0-9]{7,14}$", message = "must be an E.164 phone number")
            String approverPhone
    ) {}

    public record ExecutionResultRequest(
            @NotBlank @Size(max = 180) String executionReference
    ) {}
}
