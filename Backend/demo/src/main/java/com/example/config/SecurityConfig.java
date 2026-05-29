package com.example.config;

import com.example.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) 
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // ==========================================
                // ENDPOINTS PÚBLICOS (Sin autenticación)
                // ==========================================
                .requestMatchers("/api/registro", "/api/ingresar").permitAll()
                .requestMatchers("/api/verificar-email", "/api/verificar-nombre").permitAll()
                .requestMatchers("/api/refresh-token").permitAll()
                
                // Debug endpoints - temporal para verificar roles
                .requestMatchers("/api/debug/auth-info").permitAll()
                .requestMatchers("/api/debug/test-admin").hasRole("ADMIN")
                .requestMatchers("/api/debug/test-customer").hasAnyRole("CUSTOMER", "ADMIN")
                
                // Logout - Cualquier usuario autenticado
                .requestMatchers("/api/logout").authenticated()
                
                // ==========================================
                // PRODUCTOS Y CATÁLOGO (Lectura pública)
                // ==========================================
                // Pizzas y Categorías - Solo GET público, resto requiere ADMIN
                .requestMatchers(HttpMethod.GET, "/api/pizzas", "/api/pizzas/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/pizzas").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/pizzas/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/pizzas/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/pizzas/**").hasRole("ADMIN")
                
                .requestMatchers(HttpMethod.GET, "/api/categories", "/api/categories/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/categories").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")
                
                .requestMatchers(HttpMethod.GET, "/api/ingredients", "/api/ingredients/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/ingredients").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/ingredients/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/ingredients/*/availability").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/ingredients/**").hasRole("ADMIN")
                
                .requestMatchers(HttpMethod.GET, "/api/sizes", "/api/sizes/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/sizes").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/sizes/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/sizes/**").hasRole("ADMIN")
                
                // Pizza Ingredients - Solo ADMIN
                .requestMatchers("/api/pizza-ingredients/**").hasRole("ADMIN")
                
                // ==========================================
                // EXTRAS (Bebidas, Postres, Entradas, etc.)
                // ==========================================
                .requestMatchers(HttpMethod.GET, "/api/extras/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/extras").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/extras/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/extras/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/extras/**").hasRole("ADMIN")
                
                // ==========================================
                // PROMOCIONES
                // ==========================================
                .requestMatchers(HttpMethod.GET, "/api/promociones/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/promociones/validar").hasAnyRole("CUSTOMER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/promociones").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/promociones/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/promociones/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/promociones/**").hasRole("ADMIN")
                
                // ==========================================
                // PATRONES DE PIZZA (Cálculo de precios)
                // ==========================================
                .requestMatchers(HttpMethod.POST, "/api/pizzas/patrones/*/extras").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/pizzas/patrones/**").permitAll()
                
                // ==========================================
                // USUARIOS Y AUTENTICACIÓN
                // ==========================================
                // Patrones de Usuario - Info pública, creación pública, resto ADMIN
                .requestMatchers(HttpMethod.GET, "/api/usuarios/patrones/info").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/usuarios/patrones/crear-cliente").permitAll()
                .requestMatchers("/api/usuarios/patrones/**").hasRole("ADMIN")
                
                // Usuario Actual - Cualquier usuario autenticado
                .requestMatchers(HttpMethod.GET, "/api/usuarios/me").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/usuarios/me").authenticated()
                
                // Gestión de Usuarios - Solo ADMIN
                .requestMatchers("/api/usuarios/**").hasRole("ADMIN")
                
                // ==========================================
                // MÉTODOS DE PAGO
                // ==========================================
                .requestMatchers(HttpMethod.GET, "/api/payment-methods/active").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/payment-methods").hasAnyRole("ADMIN", "CUSTOMER")
                .requestMatchers(HttpMethod.POST, "/api/payment-methods").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/payment-methods/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/payment-methods/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/payment-methods/**").hasRole("ADMIN")
                
                // ==========================================
                // PEDIDOS (Orders)
                // ==========================================
                // GET /api/orders - ADMIN: todos, CUSTOMER: validado en controller
                .requestMatchers(HttpMethod.GET, "/api/orders", "/api/orders/complete").hasAnyRole("ADMIN", "CUSTOMER")
                .requestMatchers(HttpMethod.GET, "/api/orders/**").hasAnyRole("ADMIN", "CUSTOMER")
                .requestMatchers(HttpMethod.POST, "/api/orders").hasAnyRole("CUSTOMER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/orders/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/orders/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/orders/**").hasRole("ADMIN")
                
                // Order Items - CUSTOMER puede crear, resto ADMIN
                .requestMatchers(HttpMethod.POST, "/api/order-items").hasAnyRole("CUSTOMER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/order-items/**").hasAnyRole("CUSTOMER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/order-items/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/order-items/**").hasRole("ADMIN")
                
                // Order Item Extras - CUSTOMER puede crear, resto ADMIN
                .requestMatchers(HttpMethod.POST, "/api/order-item-extras").hasAnyRole("CUSTOMER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/order-item-extras/**").hasAnyRole("CUSTOMER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/order-item-extras/**").hasRole("ADMIN")
                
                // Order Tracking - Solo ADMIN
                .requestMatchers("/api/order-tracking/**").hasRole("ADMIN")
                
                // ==========================================
                // PAGOS (Payments)
                // ==========================================
                .requestMatchers(HttpMethod.POST, "/api/payments").hasAnyRole("CUSTOMER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/payments/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/payments/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/payments/**").hasRole("ADMIN")
                
                // ==========================================
                // RESEÑAS (Reviews)
                // ==========================================
                .requestMatchers(HttpMethod.GET, "/api/reviews", "/api/reviews/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/reviews").hasAnyRole("ADMIN", "CUSTOMER")
                .requestMatchers(HttpMethod.PUT, "/api/reviews/**").hasAnyRole("ADMIN", "CUSTOMER")
                .requestMatchers(HttpMethod.DELETE, "/api/reviews/**").hasAnyRole("ADMIN", "CUSTOMER")
                .requestMatchers(HttpMethod.PATCH, "/api/reviews/**").hasRole("ADMIN")
                
                // ==========================================
                // RECURSOS DE USUARIO AUTENTICADO
                // ==========================================
                // Direcciones - Solo usuario autenticado
                .requestMatchers("/api/addresses/**").authenticated()
                
                // Favoritos - Solo usuario autenticado
                .requestMatchers("/api/favorites/**").authenticated()
                
                // ==========================================
                // AUDITORÍA Y SESIONES (Solo ADMIN)
                // ==========================================
                .requestMatchers("/api/audit-logs/**").hasRole("ADMIN")
                .requestMatchers("/api/sessions/**").hasRole("ADMIN")
                
                // ==========================================
                // RESTO DE ENDPOINTS
                // ==========================================
                // Resto de endpoints requieren autenticación
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Permitir origen de Angular en desarrollo y localhost alternativo
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:4200",
            "http://localhost:*",
            "http://127.0.0.1:4200",
            "http://127.0.0.1:*"
        ));
        
        // Permitir todos los métodos HTTP necesarios
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"
        ));
        
        // Permitir headers específicos y wildcard para flexibilidad
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Requested-With",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        
        // Permitir credenciales (cookies, authorization headers, etc.)
        configuration.setAllowCredentials(true);
        
        // Exponer headers de respuesta que el frontend puede leer
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Total-Count",
            "X-Total-Pages"
        ));
        
        // Tiempo de cache para pre-flight requests (OPTIONS) - 1 hora
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Aplicar a todas las rutas
        return source;
    }
}
