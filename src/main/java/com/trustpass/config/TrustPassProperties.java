package com.trustpass.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "trustpass")
public record TrustPassProperties(
        String corsOrigins,
        Security security,
        Integrations integrations
) {
    public record Security(
            String jwtSecret,
            long tokenMinutes,
            String adminPassword,
            String approverPassword,
            String auditorPassword
    ) {}

    public record Integrations(OpenAi openai, ElevenLabs elevenlabs) {
        public record OpenAi(boolean enabled, String apiKey, String model, String baseUrl) {}

        public record ElevenLabs(
                boolean enabled,
                String apiKey,
                String agentId,
                String phoneNumberId,
                String webhookSecret,
                String baseUrl
        ) {}
    }
}
