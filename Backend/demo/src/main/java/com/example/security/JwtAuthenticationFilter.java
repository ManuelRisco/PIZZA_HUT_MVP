package com.example.security;

import com.example.models.Usuario;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.example.repositories.SessionLogRepository;
import com.example.models.SessionLog;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    private final com.example.services.UsuarioService usuarioService;
    private final SessionLogRepository sessionLogRepository;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, com.example.services.UsuarioService usuarioService,
            SessionLogRepository sessionLogRepository) {
        this.tokenProvider = tokenProvider;
        this.usuarioService = usuarioService;
        this.sessionLogRepository = sessionLogRepository;
    }

    // Caché de validación de tokens (5 minutos de vida para evitar consultas
    // constantes)
    private static final long CACHE_DURATION_MS = TimeUnit.MINUTES.toMillis(5);
    private static final int MAX_CACHE_SIZE = 5000;
    private static final ConcurrentHashMap<String, TokenCacheEntry> tokenCache = new ConcurrentHashMap<>();

    private static class TokenCacheEntry {
        final String role;
        final Integer tokenVersion;
        final long expiresAt;

        TokenCacheEntry(String role, Integer tokenVersion) {
            this.role = role;
            this.tokenVersion = tokenVersion;
            this.expiresAt = System.currentTimeMillis() + CACHE_DURATION_MS;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String token = getTokenFromRequest(request);

        if (token != null && tokenProvider.validateToken(token)) {
            // Limpiar entradas expiradas periódicamente y controlar tamaño del caché
            if (tokenCache.size() > MAX_CACHE_SIZE || Math.random() < 0.05) {
                tokenCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
            }

            String roleFromToken = tokenProvider.getRoleFromToken(token);
            Long userId = tokenProvider.getIdFromToken(token);
            Integer tokenVersionFromToken = tokenProvider.getTokenVersionFromToken(token);

            if (userId != null) {
                String roleActual = null;
                Integer tokenVersionActual = null;
                boolean shouldBlock = false;

                // Verificar caché primero (ConcurrentHashMap es thread-safe, no requiere
                // synchronized)
                TokenCacheEntry cacheEntry = tokenCache.get(token);

                if (cacheEntry != null && !cacheEntry.isExpired()) {
                    roleActual = cacheEntry.role;
                    tokenVersionActual = cacheEntry.tokenVersion;
                } else {
                    // Limpiar entrada expirada si existe
                    if (cacheEntry != null) {
                        tokenCache.remove(token, cacheEntry);
                    }

                    // VALIDACIÓN DE SESIÓN: Si la sesión fue cerrada (manual o por admin), bloquear
                    // acceso.
                    Optional<SessionLog> sessionLogOpt = sessionLogRepository.findBySessionToken(token);
                    if (sessionLogOpt.isPresent() && !sessionLogOpt.get().getIsActive()) {
                        tokenCache.remove(token);
                        shouldBlock = true;
                    }

                    if (!shouldBlock) {
                        // VALIDACIÓN CRÍTICA: Verificar que el rol y la versión del token coincidan con
                        // la BD
                        Optional<Usuario> usuarioOpt = usuarioService.obtenerPorId(userId);
                        if (usuarioOpt.isEmpty()) {
                            shouldBlock = true;
                        } else {
                            Usuario usuario = usuarioOpt.get();
                            roleActual = usuario.getRole().name();
                            tokenVersionActual = usuario.getTokenVersion();

                            // Guardar en caché con control de tamaño máximo
                            if (tokenCache.size() < MAX_CACHE_SIZE) {
                                tokenCache.put(token, new TokenCacheEntry(roleActual, tokenVersionActual));
                            }
                        }
                    }
                }

                if (shouldBlock) {
                    filterChain.doFilter(request, response);
                    return;
                }

                // Si el rol cambió O la versión del token no coincide, el token es inválido
                if (roleActual == null || !roleActual.equals(roleFromToken) ||
                        (tokenVersionFromToken != null && !tokenVersionFromToken.equals(tokenVersionActual))) {
                    tokenCache.remove(token); // Invalidar caché
                    System.out.println(
                            "TOKEN REJECTED! Role match: " + (roleActual != null && roleActual.equals(roleFromToken)) +
                                    ", TokenVersion from token: " + tokenVersionFromToken +
                                    ", TokenVersion actual: " + tokenVersionActual);
                    filterChain.doFilter(request, response);
                    return;
                }

                // Crear autoridades basadas en el rol ACTUAL
                List<SimpleGrantedAuthority> authorities = List
                        .of(new SimpleGrantedAuthority("ROLE_" + roleActual));

                // Crear autenticación
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        tokenProvider.getEmailFromToken(token), null, authorities);

                // Agregar detalles adicionales
                authentication.setDetails(userId);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
