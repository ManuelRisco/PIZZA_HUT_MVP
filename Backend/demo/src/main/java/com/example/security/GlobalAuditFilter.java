package com.example.security;

import com.example.services.AuditLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class GlobalAuditFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(GlobalAuditFilter.class);

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

        try {
            String entityType = extractEntityType(uri);
            Integer userId = getCurrentUserId();
            logAuditAction(method, userId, entityType, uri);
        } catch (Exception ex) {
            logger.warn("No se pudo registrar auditoría para {} {}: {}", method, uri, ex.getMessage());
        }
    }

    private boolean isSuccessfulResponse(int status) {
        return status >= 200 && status < 300;
    }

    private boolean isSafeMethod(String method) {
        return "GET".equals(method) || "OPTIONS".equals(method) || "HEAD".equals(method);
    }

    private boolean shouldIgnoreUri(String uri) {
        return uri.startsWith("/api/usuarios") ||
               uri.startsWith("/api/ingresar") ||
               uri.startsWith("/api/registro") ||
               uri.startsWith("/api/refresh-token") ||
               uri.startsWith("/api/logout") ||
               uri.startsWith("/api/sessions") ||
               uri.startsWith("/api/audit-logs") ||
               uri.startsWith("/api/production/estimate") ||
               uri.startsWith("/api/pizzas/patrones") ||
               uri.startsWith("/api/promociones/validar") ||
               uri.startsWith("/v3/api-docs") ||
               uri.startsWith("/swagger-ui");
    }

    private String extractEntityType(String uri) {
        String[] parts = uri.split("/");
        if (parts.length > 2 && "api".equals(parts[1]) && !parts[2].isEmpty()) {
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
            if (details instanceof Integer idInt) {
                return idInt;
            } else if (details instanceof Long idLong) {
                return idLong.intValue();
            } else if (details instanceof String idStr) {
                try {
                    return Integer.parseInt(idStr);
                } catch (NumberFormatException ignored) {
                    // Not a valid user ID string, ignore
                }
            }
        }
        return null;
    }
}
