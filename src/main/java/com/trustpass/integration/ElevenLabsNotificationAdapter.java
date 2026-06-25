package com.trustpass.integration;

import tools.jackson.databind.JsonNode;
import com.trustpass.approval.ApprovalRequestEntity;
import com.trustpass.config.TrustPassProperties;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "trustpass.integrations.elevenlabs", name = "enabled", havingValue = "true")
public class ElevenLabsNotificationAdapter implements ApprovalNotificationPort {
    private final TrustPassProperties.Integrations.ElevenLabs properties;
    private final RestClient client;

    public ElevenLabsNotificationAdapter(TrustPassProperties root, RestClient.Builder builder) {
        this.properties = root.integrations().elevenlabs();
        require(properties.apiKey(), "ELEVENLABS_API_KEY");
        require(properties.agentId(), "ELEVENLABS_AGENT_ID");
        require(properties.phoneNumberId(), "ELEVENLABS_PHONE_NUMBER_ID");
        this.client = builder.baseUrl(properties.baseUrl())
                .defaultHeader("xi-api-key", properties.apiKey())
                .build();
    }

    @Override
    public Optional<String> notifyApprover(ApprovalRequestEntity approval, String approverPhone) {
        require(approverPhone, "approverPhone");
        Map<String, Object> dynamicVariables = Map.of(
                "approval_id", approval.getId().toString(),
                "approval_reference", approval.getReference(),
                "action_summary", approval.getSummary(),
                "amount", approval.getAmount().toPlainString(),
                "currency", approval.getCurrency(),
                "target", approval.getTarget());
        Map<String, Object> body = Map.of(
                "agent_id", properties.agentId(),
                "agent_phone_number_id", properties.phoneNumberId(),
                "to_number", approverPhone,
                "conversation_initiation_client_data", Map.of("dynamic_variables", dynamicVariables),
                "call_recording_enabled", false);
        JsonNode response = client.post().uri("/v1/convai/twilio/outbound-call")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        return response != null && response.hasNonNull("conversation_id")
                ? Optional.of(response.path("conversation_id").asText())
                : Optional.empty();
    }

    private void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required when ElevenLabs integration is enabled");
        }
    }
}
