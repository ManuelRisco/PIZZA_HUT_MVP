package com.example.controller;

import com.example.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/debug")
@CrossOrigin(origins = "http://localhost:4200")
public class DebugController {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @GetMapping("/auth-info")
    public ResponseEntity<?> getAuthInfo(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> info = new HashMap<>();
        
        // Información del contexto de seguridad
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            info.put("authenticated", auth.isAuthenticated());
            info.put("principal", auth.getPrincipal());
            info.put("authorities", auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
            info.put("details", auth.getDetails());
        } else {
            info.put("authenticated", false);
            info.put("message", "No authentication found in security context");
        }
        
        // Información del token
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                info.put("tokenValid", jwtTokenProvider.validateToken(token));
                info.put("email", jwtTokenProvider.getEmailFromToken(token));
                info.put("role", jwtTokenProvider.getRoleFromToken(token));
                info.put("userId", jwtTokenProvider.getIdFromToken(token));
                info.put("tokenVersion", jwtTokenProvider.getTokenVersionFromToken(token));
            } catch (Exception e) {
                info.put("tokenError", e.getMessage());
            }
        } else {
            info.put("tokenPresent", false);
        }
        
        return ResponseEntity.ok(info);
    }

    @GetMapping("/test-admin")
    public ResponseEntity<?> testAdmin() {
        return ResponseEntity.ok(Map.of("message", "Tienes acceso ADMIN", "success", true));
    }

    @GetMapping("/test-customer")
    public ResponseEntity<?> testCustomer() {
        return ResponseEntity.ok(Map.of("message", "Tienes acceso CUSTOMER", "success", true));
    }
}
