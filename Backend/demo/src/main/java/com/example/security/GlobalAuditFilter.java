package com.example.security;

import com.example.services.AuditLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class GlobalAuditFilter extends OncePerRequestFilter {

    private final AuditLogService auditLogService;

    public GlobalAuditFilter(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        filterChain.doFilter(request, response);

        if (!isSuccessfulResponse(response.getStatus())) {
            return;
        }

        String uri = request.getRequestURI();
        String method = request.getMethod();

        if (isSafeMethod(method) || shouldIgnoreUri(uri)) {
            return;
        }

        String entityType = extractEntityType(uri);
        Integer userId = getCurrentUserId();

        logAuditAction(method, userId, entityType, uri);
    }

    private boolean isSuccessfulResponse(int status) {
        return status >= 200 && status < 300;
    }

    private boolean isSafeMethod(String method) {
        return method.equals("GET") || method.equals("OPTIONS") || method.equals("HEAD");
    }

    private boolean shouldIgnoreUri(String uri) {
        return uri.startsWith("/api/usuarios") ||
               uri.startsWith("/api/sessions") ||
               uri.startsWith("/api/audit-logs") ||
               uri.startsWith("/v3/api-docs") ||
               uri.startsWith("/swagger-ui");
    }

    private String extractEntityType(String uri) {
        String[] parts = uri.split("/");
        if (parts.length > 2 && "api".equals(parts[1])) {
            String entityType = parts[2];
            return entityType.substring(0, 1).toUpperCase() + entityType.substring(1);
        }
        return "Desconocido";
    }

    private void logAuditAction(String method, Integer userId, String entityType, String uri) {
        if ("POST".equals(method)) {
            auditLogService.registrarCreacion(userId, entityType, null, "Creación exitosa en módulo " + entityType);
        } else if ("PUT".equals(method) || "PATCH".equals(method)) {
            auditLogService.registrarActualizacion(userId, entityType, null, "{}", "{}", "Actualización exitosa en módulo " + entityType);
        } else if ("DELETE".equals(method)) {
            Integer entityId = extractEntityId(uri);
            auditLogService.registrarEliminacion(userId, entityType, entityId, "Eliminación exitosa en módulo " + entityType);
        }
    }

    private Integer extractEntityId(String uri) {
        String[] parts = uri.split("/");
        if (parts.length > 3) {
            try {
                return Integer.parseInt(parts[parts.length - 1]);
            } catch (NumberFormatException ignored) {
                // Not a valid ID in the URI, ignore
            }
        }
        return null;
    }

    private Integer getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() != null) {
            Object details = auth.getDetails();
            if (details instanceof Integer) {
                return (Integer) details;
            } else if (details instanceof Long) {
                return ((Long) details).intValue();
            } else if (details instanceof String) {
                try {
                    return Integer.parseInt((String) details);
                } catch (NumberFormatException ignored) {
                    // Not a valid user ID string, ignore
                }
            }
        }
        return null;
    }
}
