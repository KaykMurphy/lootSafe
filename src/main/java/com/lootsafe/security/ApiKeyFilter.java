package com.lootsafe.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
@Slf4j
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final String ADMIN_PATH_PREFIX = "/api/mediation";

    @Value("${lootsafe.security.admin-api-key}")
    private String adminApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            filterChain.doFilter(request, response);
            return;
        }

        String url = request.getRequestURI();

        if (!url.startsWith(ADMIN_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedApiKey = request.getHeader("X-API-KEY");

        if (!isValidAdminApiKey(providedApiKey)) {
            log.warn("Requisicao administrativa negada. metodo={} caminho={}", request.getMethod(), url);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Access denied: invalid API key");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isValidAdminApiKey(String providedApiKey) {
        if (providedApiKey == null || adminApiKey == null || adminApiKey.isBlank()) {
            return false;
        }

        byte[] provided = providedApiKey.trim().getBytes(StandardCharsets.UTF_8);
        byte[] expected = adminApiKey.trim().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(provided, expected);
    }
}
