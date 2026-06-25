package com.trustpass.agent;

import com.trustpass.audit.AuditService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
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
@RequestMapping("/api/v1/agents")
public class AgentApi {
    private final AgentRepository repository;
    private final AuditService auditService;

    public AgentApi(AgentRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @GetMapping
    public List<Response> list() {
        return repository.findAll().stream().map(Response::from).toList();
    }

    @GetMapping("/{id}")
    public Response get(@PathVariable UUID id) {
        return Response.from(find(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Response> create(@Valid @RequestBody Request request, Principal principal) {
        AgentEntity saved = repository.save(new AgentEntity(
                request.name(), request.description(), request.type(), request.owner(), request.authorityLimit()));
        auditService.append("AGENT", saved.getId(), "AGENT_REGISTERED", principal.getName(),
                java.util.Map.of("name", saved.getName(), "type", saved.getType(),
                        "authorityLimit", saved.getAuthorityLimit()));
        return ResponseEntity.created(URI.create("/api/v1/agents/" + saved.getId())).body(Response.from(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Response update(@PathVariable UUID id, @Valid @RequestBody UpdateRequest request, Principal principal) {
        AgentEntity agent = find(id);
        agent.update(request.name(), request.description(), request.type(), request.owner(),
                request.authorityLimit(), request.status());
        AgentEntity saved = repository.save(agent);
        auditService.append("AGENT", saved.getId(), "AGENT_AUTHORITY_UPDATED", principal.getName(),
                java.util.Map.of("status", saved.getStatus(), "authorityLimit", saved.getAuthorityLimit()));
        return Response.from(saved);
    }

    private AgentEntity find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new AgentNotFoundException(id));
    }

    public record Request(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 500) String description,
            @NotNull AgentType type,
            @NotBlank @Size(max = 120) String owner,
            @NotNull @DecimalMin("0.00") BigDecimal authorityLimit
    ) {}

    public record UpdateRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 500) String description,
            @NotNull AgentType type,
            @NotBlank @Size(max = 120) String owner,
            @NotNull @DecimalMin("0.00") BigDecimal authorityLimit,
            @NotNull AgentStatus status
    ) {}

    public record Response(
            UUID id,
            String name,
            String description,
            AgentType type,
            String owner,
            AgentStatus status,
            BigDecimal authorityLimit,
            int reputationScore,
            Instant createdAt,
            Instant updatedAt
    ) {
        static Response from(AgentEntity agent) {
            return new Response(agent.getId(), agent.getName(), agent.getDescription(), agent.getType(),
                    agent.getOwner(), agent.getStatus(), agent.getAuthorityLimit(), agent.getReputationScore(),
                    agent.getCreatedAt(), agent.getUpdatedAt());
        }
    }
}
