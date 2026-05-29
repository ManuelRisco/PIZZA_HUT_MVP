package com.example.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración adicional de CORS a nivel de aplicación
 * Esta configuración complementa la configuración de CORS en SecurityConfig
 * Permite que el frontend (Angular en localhost:4200) se comunique con el backend
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**")
                // Orígenes permitidos - Angular dev server
                .allowedOriginPatterns(
                    "http://localhost:4200",
                    "http://localhost:*",
                    "http://127.0.0.1:4200",
                    "http://127.0.0.1:*"
                )
                // Métodos HTTP permitidos
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD")
                // Headers permitidos
                .allowedHeaders(
                    "Authorization",
                    "Content-Type",
                    "Accept",
                    "Origin",
                    "X-Requested-With",
                    "Access-Control-Request-Method",
                    "Access-Control-Request-Headers"
                )
                // Headers expuestos al cliente
                .exposedHeaders(
                    "Authorization",
                    "Content-Type",
                    "X-Total-Count",
                    "X-Total-Pages"
                )
                // Permitir credenciales (cookies, authorization headers, etc.)
                .allowCredentials(true)
                // Cache de configuración CORS preflight (1 hora)
                .maxAge(3600);
    }
}
