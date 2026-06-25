package com.trustpass.auth;

import com.trustpass.config.TrustPassProperties;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final JwtEncoder encoder;
    private final TrustPassProperties.Security properties;

    public JwtService(JwtEncoder encoder, TrustPassProperties root) {
        this.encoder = encoder;
        this.properties = root.security();
    }

    public Token issue(Authentication authentication) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.tokenMinutes(), ChronoUnit.MINUTES);
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(value -> value.replaceFirst("^ROLE_", ""))
                .toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("trustpass-backend")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(authentication.getName())
                .claim("roles", roles)
                .build();
        JwsHeader headers = JwsHeader.with(MacAlgorithm.HS256).build();
        String value = encoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
        return new Token(value, expiresAt, authentication.getName(), roles);
    }

    public record Token(String accessToken, Instant expiresAt, String username, List<String> roles) {}
}
