package com.ecopizza.config;

import com.ecopizza.services.FileUploadService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuracion MVC para servir archivos estaticos de imagenes desde uploads/.
 * La configuracion de CORS se gestiona de forma centralizada en SecurityConfig.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadUri = FileUploadService.resolveUploadRoot(uploadDir).toUri().toString();
        if (!uploadUri.endsWith("/")) {
            uploadUri = uploadUri + "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadUri)
                .setCachePeriod(3600);
    }
}
