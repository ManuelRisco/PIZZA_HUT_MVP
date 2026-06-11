package com.example.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    private final JwtTokenProvider tokenProvider;

    public SecurityUtils(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    /**
     * Obtiene el email del usuario autenticado
     */
    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * Obtiene el ID del usuario desde el contexto de autenticación
     */
    public Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() != null) {
            Object details = authentication.getDetails();
            if (details instanceof Long) {
                return ((Long) details).intValue();
            } else if (details instanceof Integer) {
                return (Integer) details;
            }
        }
        return null;
    }

    /**
     * Obtiene el ID del usuario desde el token JWT (alternativa)
     */
    public Integer getUserIdFromToken(String token) {
        if (token != null && tokenProvider.validateToken(token)) {
            Long id = tokenProvider.getIdFromToken(token);
            return id != null ? id.intValue() : null;
        }
        return null;
    }

    /**
     * Verifica si el usuario actual es ADMIN
     */
    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities() != null) {
            return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));
        }
        return false;
    }

    /**
     * Verifica si el usuario actual es CUSTOMER
     */
    public boolean isCustomer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities() != null) {
            return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_CUSTOMER"));
        }
        return false;
    }

    /**
     * Verifica si el usuario puede acceder al recurso
     * Los ADMIN pueden acceder a todo, los CUSTOMER solo a sus propios recursos
     */
    public boolean canAccessResource(Integer resourceUserId, Integer currentUserId) {
        if (isAdmin()) {
            return true; // Admin puede acceder a todo
        }
        
        if (currentUserId == null || resourceUserId == null) {
            return false;
        }
        
        return resourceUserId.equals(currentUserId); // Solo puede acceder si es su propio recurso
    }

    /**
     * Extrae el token del header Authorization
     */
    public String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
