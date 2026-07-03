package com.example.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuraci\u00f3n adicional de CORS a nivel de aplicaci\u00f3n
 * Esta configuraci\u00f3n complementa la configuraci\u00f3n de CORS en SecurityConfig
 * Permite que el frontend (Angular en localhost:4200) se comunique con el backend
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // Or\u00edgenes permitidos - Angular dev server
                .allowedOriginPatterns(
                    "http://localhost:4200",
                    "http://localhost:*",
                    "http://127.0.0.1:4200",
                    "http://127.0.0.1:*"
                )
                // M\u00e9todos HTTP permitidos
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
                // Cache de configuraci\u00f3n CORS preflight (1 hora)
                .maxAge(3600);
    }
}
