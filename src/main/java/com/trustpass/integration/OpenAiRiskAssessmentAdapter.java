package com.trustpass.integration;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.trustpass.approval.RiskAssessment;
import com.trustpass.approval.RiskLevel;
import com.trustpass.config.TrustPassProperties;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "trustpass.integrations.openai", name = "enabled", havingValue = "true")
public class OpenAiRiskAssessmentAdapter implements RiskAssessmentPort {
    private static final Logger log = LoggerFactory.getLogger(OpenAiRiskAssessmentAdapter.class);
    private final TrustPassProperties.Integrations.OpenAi properties;
    private final ObjectMapper objectMapper;
    private final RestClient client;

    public OpenAiRiskAssessmentAdapter(TrustPassProperties properties, ObjectMapper objectMapper,
                                       RestClient.Builder builder) {
        this.properties = properties.integrations().openai();
        this.objectMapper = objectMapper;
        if (this.properties.apiKey() == null || this.properties.apiKey().isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY is required when OpenAI integration is enabled");
        }
        RestClient.Builder clientBuilder = builder.baseUrl(this.properties.baseUrl())
                .defaultHeader("Authorization", "Bearer " + this.properties.apiKey());
        if (hasText(this.properties.organizationId())) {
            clientBuilder.defaultHeader("OpenAI-Organization", this.properties.organizationId());
        }
        if (hasText(this.properties.projectId())) {
            clientBuilder.defaultHeader("OpenAI-Project", this.properties.projectId());
        }
        this.client = clientBuilder.build();
    }

    @Override
    public RiskAssessment assess(Input input) {
        try {
            String actionJson = objectMapper.writeValueAsString(input);
            Map<String, Object> body = Map.of(
                    "model", properties.model(),
                    "instructions", "You are a conservative enterprise risk classifier. Return only JSON with "
                            + "integer score (0-100) and reasons (array of short strings). AI may increase review "
                            + "requirements but never grant authority. Consider financial, legal, privacy, "
                            + "irreversibility, destination, and agent reputation risk.",
                    "input", actionJson);
            JsonNode response = client.post().uri("/v1/responses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String output = extractOutputText(response);
            JsonNode result = objectMapper.readTree(stripCodeFence(output));
            int score = Math.max(0, Math.min(100, result.path("score").asInt(90)));
            List<String> reasons = result.path("reasons").isArray()
                    ? objectMapper.convertValue(result.path("reasons"), objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, String.class))
                    : List.of("AI risk response did not include structured reasons");
            return new RiskAssessment(score, RiskLevel.fromScore(score), reasons,
                    "openai:" + properties.model());
        } catch (Exception exception) {
            log.warn("OpenAI risk assessment failed; forcing human review: {}", exception.getMessage());
            return new RiskAssessment(90, RiskLevel.CRITICAL,
                    List.of("External risk service unavailable; fail-closed manual review required"),
                    "openai-fail-closed");
        }
    }

    private String extractOutputText(JsonNode response) {
        if (response == null) return "{}";
        for (JsonNode output : response.path("output")) {
            for (JsonNode content : output.path("content")) {
                if (content.hasNonNull("text")) return content.path("text").asText();
            }
        }
        return "{}";
    }

    private String stripCodeFence(String value) {
        return value.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
