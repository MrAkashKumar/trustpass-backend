package com.trustpass.auth;

import com.trustpass.config.TrustPassProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AgentClientAuthenticationFilter extends OncePerRequestFilter {
    public static final String CLIENT_ID_HEADER = "X-TrustPass-Client-Id";
    public static final String API_KEY_HEADER = "X-TrustPass-Api-Key";
    private static final String AGENT_ACTIONS_PREFIX = "/api/v1/agent-actions";

    private final TrustPassProperties.AgentClient properties;

    public AgentClientAuthenticationFilter(TrustPassProperties root) {
        this.properties = root.security().agentClient();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(AGENT_ACTIONS_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (properties == null || !properties.enabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientId = request.getHeader(CLIENT_ID_HEADER);
        String apiKey = request.getHeader(API_KEY_HEADER);
        if (isBlank(clientId) || isBlank(apiKey)) {
            unauthorized(response, "Missing TrustPass agent client credentials");
            return;
        }
        if (!constantTimeEquals(clientId, properties.clientId())
                || !constantTimeEquals(apiKey, properties.apiKey())) {
            unauthorized(response, "Invalid TrustPass agent client credentials");
            return;
        }

        var authentication = new UsernamePasswordAuthenticationToken(
                "agent-client:" + clientId,
                "N/A",
                AuthorityUtils.createAuthorityList("ROLE_AGENT_CLIENT"));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean constantTimeEquals(String candidate, String expected) {
        if (candidate == null || expected == null || expected.isBlank()) return false;
        return MessageDigest.isEqual(candidate.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"" + message + "\"}");
    }
}
