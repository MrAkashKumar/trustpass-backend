package com.trustpass.integration;

import com.trustpass.config.TrustPassProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class ElevenLabsWebhookVerifier {
    private static final Pattern TIMESTAMP = Pattern.compile("(?:^|,)t=([0-9]+)");
    private static final Pattern SIGNATURE = Pattern.compile("(?:^|,)v0=([a-fA-F0-9]+)");
    private static final long MAX_AGE_SECONDS = 300;
    private final String secret;

    public ElevenLabsWebhookVerifier(TrustPassProperties properties) {
        this.secret = properties.integrations().elevenlabs().webhookSecret();
    }

    public boolean isValid(String rawBody, String signatureHeader) {
        if (secret == null || secret.isBlank() || signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }
        Matcher timestampMatcher = TIMESTAMP.matcher(signatureHeader);
        Matcher signatureMatcher = SIGNATURE.matcher(signatureHeader);
        if (!timestampMatcher.find() || !signatureMatcher.find()) return false;

        long timestamp;
        try {
            timestamp = Long.parseLong(timestampMatcher.group(1));
        } catch (NumberFormatException exception) {
            return false;
        }
        if (Math.abs(Instant.now().getEpochSecond() - timestamp) > MAX_AGE_SECONDS) return false;

        String payload = timestamp + "." + rawBody;
        String expected = hmac(payload);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                signatureMatcher.group(1).toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

    private String hmac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not verify webhook signature", exception);
        }
    }
}

