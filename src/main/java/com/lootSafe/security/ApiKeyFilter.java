package com.lootSafe.security;

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

    @Value("${lootsafe.seguranca.lootSafe-admin-api-key}")
    private String adminApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {

        String url = request.getRequestURI();

        if (!url.startsWith("/api/moderacao")) {
            filterChain.doFilter(request, response);
            return;
        }

        String chaveEnviada = request.getHeader("X-API-KEY");

        if (chaveEnviada == null || !chaveEnviada.equals(adminApiKey)) {

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Acesso Negado: Chave de API inválida");

            return;
        }

        filterChain.doFilter(request, response);
    }
}