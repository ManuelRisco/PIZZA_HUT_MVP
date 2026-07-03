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

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, com.example.services.UsuarioService usuarioService,
            SessionLogRepository sessionLogRepository) {
        this.tokenProvider = tokenProvider;
        this.usuarioService = usuarioService;
        this.sessionLogRepository = sessionLogRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = getTokenFromRequest(request);

        if (token != null && tokenProvider.validateToken(token)) {
            manageTokenCache();

            String roleFromToken = tokenProvider.getRoleFromToken(token);
            Long userId = tokenProvider.getIdFromToken(token);
            Integer tokenVersionFromToken = tokenProvider.getTokenVersionFromToken(token);

            if (userId != null && !processAuthentication(token, userId, roleFromToken, tokenVersionFromToken)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
    
    private boolean processAuthentication(String token, Long userId, String roleFromToken, Integer tokenVersionFromToken) {
        String roleActual = null;
        Integer tokenVersionActual = null;
        
        TokenCacheEntry cacheEntry = tokenCache.get(token);

        if (cacheEntry != null && !cacheEntry.isExpired()) {
            roleActual = cacheEntry.role;
            tokenVersionActual = cacheEntry.tokenVersion;
        } else {
            if (cacheEntry != null) {
                tokenCache.remove(token, cacheEntry);
            }

            if (isSessionInvalidated(token)) {
                return false;
            }

            Optional<Usuario> usuarioOpt = usuarioService.obtenerPorId(userId);
            if (usuarioOpt.isEmpty()) {
                return false;
            } 
            
            Usuario usuario = usuarioOpt.get();
            roleActual = usuario.getRole().name();
            tokenVersionActual = usuario.getTokenVersion();

            if (tokenCache.size() < MAX_CACHE_SIZE) {
                tokenCache.put(token, new TokenCacheEntry(roleActual, tokenVersionActual));
            }
        }

        if (isTokenRejected(roleActual, roleFromToken, tokenVersionActual, tokenVersionFromToken)) {
            tokenCache.remove(token); 
            return false;
        }

        authenticateUser(token, userId, roleActual);
        return true;
    }

    private boolean isSessionInvalidated(String token) {
        Optional<SessionLog> sessionLogOpt = sessionLogRepository.findBySessionToken(token);
        if (sessionLogOpt.isPresent() && !sessionLogOpt.get().getIsActive()) {
            tokenCache.remove(token);
            return true;
        }
        return false;
    }

    private boolean isTokenRejected(String roleActual, String roleFromToken, Integer tokenVersionActual, Integer tokenVersionFromToken) {
        return roleActual == null || !roleActual.equals(roleFromToken) ||
               (tokenVersionFromToken != null && !tokenVersionFromToken.equals(tokenVersionActual));
    }

    private void authenticateUser(String token, Long userId, String roleActual) {
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + roleActual));
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                tokenProvider.getEmailFromToken(token), null, authorities);
        authentication.setDetails(userId);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void manageTokenCache() {
        if (tokenCache.size() > MAX_CACHE_SIZE || Math.random() < 0.05) {
            tokenCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        }
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
