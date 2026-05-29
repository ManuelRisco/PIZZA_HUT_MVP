package com.example.security;

import com.example.domain.model.Usuario;
import com.example.domain.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String token = getTokenFromRequest(request);

        if (token != null && tokenProvider.validateToken(token)) {
            String email = tokenProvider.getEmailFromToken(token);
            String roleFromToken = tokenProvider.getRoleFromToken(token);
            Long userId = tokenProvider.getIdFromToken(token);
            Integer tokenVersionFromToken = tokenProvider.getTokenVersionFromToken(token);

            // VALIDACIÓN CRÍTICA: Verificar que el rol y la versión del token coincidan con la BD
            Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                String roleActual = usuario.getRole().name();
                Integer tokenVersionActual = usuario.getTokenVersion();
                
                // Si el rol cambió O la versión del token no coincide, el token es inválido
                if (!roleActual.equals(roleFromToken) || 
                    (tokenVersionFromToken != null && !tokenVersionFromToken.equals(tokenVersionActual))) {
                    // No autenticar - el token tiene datos desactualizados
                    filterChain.doFilter(request, response);
                    return;
                }
                
                // Crear autoridades basadas en el rol ACTUAL de la BD
                List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + roleActual));

                // Crear autenticación con el email como principal
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(email, null, authorities);
                
                // Agregar detalles adicionales (opcional)
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