package com.example.security;

import com.example.services.AuditLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
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
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // Continuar con la petici\u00f3n normal
        filterChain.doFilter(request, response);

        // SOLO logear si la petici\u00f3n fue exitosa (200-299)
        int status = response.getStatus();
        if (status < 200 || status >= 300) {
            return;
        }

        String uri = request.getRequestURI();
        String method = request.getMethod();

        // Ignorar GET y metodos seguros
        if (method.equals("GET") || method.equals("OPTIONS") || method.equals("HEAD")) {
            return;
        }

        // Ignorar m\u00f3dulos que ya tienen su propia auditor\u00eda o no requieren auditor\u00eda de
        // negocio
        if (uri.startsWith("/api/usuarios") ||
                uri.startsWith("/api/sessions") ||
                uri.startsWith("/api/audit-logs") ||
                uri.startsWith("/v3/api-docs") ||
                uri.startsWith("/swagger-ui")) {
            return;
        }

        // Extraer el M\u00f3dulo (Entidad) de la URL (ej. /api/pizzas/5 -> pizzas)
        String entityType = "Desconocido";
        String[] parts = uri.split("/");
        if (parts.length > 2 && parts[1].equals("api")) {
            entityType = parts[2]; // Ej: "pizzas"
            // Capitalizar primera letra
            entityType = entityType.substring(0, 1).toUpperCase() + entityType.substring(1);
        }

        Integer userId = getCurrentUserId();

        if (method.equals("POST")) {
            auditLogService.registrarCreacion(userId, entityType, null, "Creaci\u00f3n exitosa en m\u00f3dulo " + entityType);
        } else if (method.equals("PUT") || method.equals("PATCH")) {
            auditLogService.registrarActualizacion(userId, entityType, null, "{}", "{}",
                    "Actualizaci\u00f3n exitosa en m\u00f3dulo " + entityType);
        } else if (method.equals("DELETE")) {
            // Intentar extraer ID si la ruta termina en n\u00famero
            Integer entityId = null;
            try {
                if (parts.length > 3) {
                    entityId = Integer.parseInt(parts[parts.length - 1]);
                }
            } catch (NumberFormatException ignored) {
            }
            auditLogService.registrarEliminacion(userId, entityType, entityId,
                    "Eliminaci\u00f3n exitosa en m\u00f3dulo " + entityType);
        }
    }

    private Integer getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() != null) {
            if (auth.getDetails() instanceof Integer) {
                return (Integer) auth.getDetails();
            } else if (auth.getDetails() instanceof Long) {
                return ((Long) auth.getDetails()).intValue();
            } else if (auth.getDetails() instanceof String) {
                try {
                    return Integer.parseInt((String) auth.getDetails());
                } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }
}
