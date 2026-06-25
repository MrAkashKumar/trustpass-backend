package com.trustpass.integration;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.trustpass.approval.ApprovalService;
import com.trustpass.approval.DecisionType;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/webhooks/elevenlabs")
public class ElevenLabsWebhookApi {
    private final ElevenLabsWebhookVerifier verifier;
    private final ObjectMapper objectMapper;
    private final ApprovalService approvalService;

    public ElevenLabsWebhookApi(ElevenLabsWebhookVerifier verifier, ObjectMapper objectMapper,
                                ApprovalService approvalService) {
        this.verifier = verifier;
        this.objectMapper = objectMapper;
        this.approvalService = approvalService;
    }

    @PostMapping
    public Map<String, String> receive(@RequestBody String rawBody,
                                       @RequestHeader(name = "ElevenLabs-Signature", required = false) String signature) {
        if (!verifier.isValid(rawBody, signature)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook signature");
        }
        try {
            JsonNode event = objectMapper.readTree(rawBody);
            if (!"post_call_transcription".equals(event.path("type").asText())) {
                return Map.of("status", "ignored");
            }
            JsonNode data = event.path("data");
            String conversationId = data.path("conversation_id").asText("unknown");
            String approvalId = firstText(
                    data.at("/conversation_initiation_client_data/dynamic_variables/approval_id"),
                    data.at("/metadata/approval_id"));
            String decisionValue = firstText(
                    data.at("/analysis/data_collection_results/approval_decision/value"),
                    data.at("/analysis/approval_decision"));
            String identityValue = firstText(
                    data.at("/analysis/data_collection_results/identity_verified/value"),
                    data.at("/analysis/identity_verified"));

            if (approvalId.isBlank() || decisionValue.isBlank()) {
                return Map.of("status", "accepted-no-decision");
            }
            DecisionType decision = switch (decisionValue.trim().toLowerCase(Locale.ROOT)) {
                case "approve", "approved", "yes" -> DecisionType.APPROVE;
                case "reject", "rejected", "no" -> DecisionType.REJECT;
                default -> null;
            };
            if (decision == null) return Map.of("status", "accepted-ambiguous");

            boolean identityVerified = "true".equalsIgnoreCase(identityValue)
                    || "verified".equalsIgnoreCase(identityValue)
                    || "yes".equalsIgnoreCase(identityValue);
            approvalService.decideFromVoice(UUID.fromString(approvalId), decision, identityVerified, conversationId);
            return Map.of("status", "processed");
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported webhook payload", exception);
        }
    }

    private String firstText(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            if (node != null && !node.isMissingNode() && !node.isNull()) {
                String value = node.isValueNode() ? node.asText() : node.path("value").asText();
                if (!value.isBlank()) return value;
            }
        }
        return "";
    }
}
