package com.lootsafe.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    @Value("${lootsafe.security.admin-api-key}")
    private String adminApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            filterChain.doFilter(request, response);
            return;
        }

        String url = request.getRequestURI();

        if (!url.startsWith("/api/mediation")) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedApiKey = request.getHeader("X-API-KEY");

        if (providedApiKey == null || !providedApiKey.equals(adminApiKey)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Access denied: invalid API key");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
