package com.aviation.aviation_api.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Simple API key authentication filter.
 * Disabled when aviation.api-key is not set.
 */
@Component
public class ApiKeyFilter implements Filter {

    @Value("${aviation.api-key:}")
    private String apiKey;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        if (apiKey.isBlank()) {
            chain.doFilter(req, res);
            return;
        }

        HttpServletRequest request = (HttpServletRequest) req;
        String path = request.getRequestURI();

        // Allow actuator endpoints without auth
        if (path.startsWith("/actuator")) {
            chain.doFilter(req, res);
            return;
        }

        String provided = request.getHeader("X-API-Key");
        if (apiKey.equals(provided)) {
            chain.doFilter(req, res);
        } else {
            ((HttpServletResponse) res).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or missing API key");
        }
    }
}
