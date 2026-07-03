package com.example.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class SecurityUtils {

    private final JwtTokenProvider tokenProvider;

    public SecurityUtils(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal();
        }
        return null;
    }

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

        return extractUserIdFromRequestContext();
    }

    private Integer extractUserIdFromRequestContext() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes != null) {
                String authHeader = attributes.getRequest().getHeader("Authorization");
                String token = extractTokenFromHeader(authHeader);
                if (token != null && tokenProvider.validateToken(token)) {
                    Long id = tokenProvider.getIdFromToken(token);
                    return id != null ? id.intValue() : null;
                }
            }
        } catch (Exception e) {
            // Ignorar y retornar null
        }
        return null;
    }

    public Integer getUserIdFromToken(String token) {
        if (token != null && tokenProvider.validateToken(token)) {
            Long id = tokenProvider.getIdFromToken(token);
            return id != null ? id.intValue() : null;
        }
        return null;
    }

    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        }
        return false;
    }

    public boolean isCustomer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"));
        }
        return false;
    }

    public boolean canAccessResource(Integer resourceUserId, Integer currentUserId) {
        if (isAdmin()) {
            return true;
        }
        if (currentUserId == null || resourceUserId == null) {
            return false;
        }
        return resourceUserId.equals(currentUserId);
    }

    public String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
